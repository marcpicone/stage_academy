from collections import Counter, deque
from contextlib import contextmanager
from hashlib import sha256
from secrets import token_urlsafe
from threading import BoundedSemaphore, Lock
from time import monotonic, time

from fastapi import APIRouter, HTTPException, Request, Response, status
from sqlalchemy import delete

from app.core.security import create_access_token
from app.dependencies import AppSettings, DatabaseSession, unauthorized
from app.models import LoginSession, User
from app.schemas.auth import LoginRequest, LoginResponse, SessionRequest, TokenResponse
from app.schemas.user import UserRegistration, UserResponse
from app.services.users import authenticate_user, register_user

router = APIRouter(prefix="/auth", tags=["Authentification"])
SESSION_SECONDS = 30 * 24 * 60 * 60


class AuthAttempts:
    def __init__(self) -> None:
        self.recent: deque[tuple[float, str]] = deque()
        self.by_account: Counter[str] = Counter()
        self.lock = Lock()
        self.workers = BoundedSemaphore(3)

    @contextmanager
    def allow(self, key: str):
        with self.lock:
            now = monotonic()
            while self.recent and self.recent[0][0] <= now - 60:
                _, expired_key = self.recent.popleft()
                self.by_account[expired_key] -= 1
                if not self.by_account[expired_key]:
                    del self.by_account[expired_key]
            if self.by_account[key] >= 15 or len(self.recent) >= 120:
                raise HTTPException(429, "Trop de demandes. Réessaie dans une minute.", headers={"Retry-After": "60"})
            if not self.workers.acquire(blocking=False):
                raise HTTPException(429, "Le serveur est occupé. Réessaie.", headers={"Retry-After": "3"})
            self.recent.append((now, key))
            self.by_account[key] += 1
        try:
            yield
        finally:
            self.workers.release()


@router.post("/register", response_model=UserResponse, status_code=status.HTTP_201_CREATED)
def register(data: UserRegistration, session: DatabaseSession, request: Request) -> User:
    with request.app.state.auth_attempts.allow("register"):
        return register_user(session, data)


@router.post("/login", response_model=LoginResponse)
def login(
    data: LoginRequest, response: Response, session: DatabaseSession, settings: AppSettings, request: Request,
) -> LoginResponse:
    with request.app.state.auth_attempts.allow(f"login:{data.username}"):
        user = authenticate_user(session, data)
    if user is None:
        raise unauthorized("Identifiant ou mot de passe incorrect.")

    refresh_token = token_urlsafe(32)
    now = int(time())
    session.execute(delete(LoginSession).where(LoginSession.expires_at <= now))
    session.add(LoginSession(token_hash=token_hash(refresh_token), user_id=user.id, expires_at=now + SESSION_SECONDS))
    session.commit()
    return LoginResponse(**issue_access_token(user, settings, response).model_dump(), refresh_token=refresh_token)


def token_hash(token: str) -> str:
    return sha256(token.encode()).hexdigest()


def issue_access_token(user: User, settings: AppSettings, response: Response) -> TokenResponse:
    response.headers["Cache-Control"] = "no-store"
    return TokenResponse(
        access_token=create_access_token(user.id, settings),
        expires_in=settings.jwt_access_token_minutes * 60,
        user_id=user.id,
        username=user.username,
    )


@router.post("/refresh", response_model=TokenResponse)
def refresh(data: SessionRequest, session: DatabaseSession, settings: AppSettings, response: Response) -> TokenResponse:
    remembered = session.get(LoginSession, token_hash(data.refresh_token.get_secret_value()))
    if remembered is None or remembered.expires_at <= int(time()):
        raise unauthorized()
    user = session.get(User, remembered.user_id)
    if user is None:
        raise unauthorized()
    return issue_access_token(user, settings, response)


@router.post("/logout", status_code=status.HTTP_204_NO_CONTENT)
def logout(data: SessionRequest, session: DatabaseSession) -> Response:
    session.execute(delete(LoginSession).where(
        LoginSession.token_hash == token_hash(data.refresh_token.get_secret_value())
    ))
    session.commit()
    return Response(status_code=status.HTTP_204_NO_CONTENT)
