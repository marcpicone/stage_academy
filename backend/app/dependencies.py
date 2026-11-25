from typing import Annotated

from fastapi import Depends, HTTPException, Request, Security, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.orm import Session

from app.core.config import Settings
from app.core.security import access_token_user_id
from app.database import get_session
from app.models import User

bearer = HTTPBearer(auto_error=False, bearerFormat="JWT")
DatabaseSession = Annotated[Session, Depends(get_session)]


def get_settings(request: Request) -> Settings:
    return request.app.state.settings


AppSettings = Annotated[Settings, Depends(get_settings)]


def unauthorized(detail: str = "Jeton d’accès absent ou invalide.") -> HTTPException:
    return HTTPException(status.HTTP_401_UNAUTHORIZED, detail, headers={"WWW-Authenticate": "Bearer"})


def get_current_user(
    credentials: Annotated[HTTPAuthorizationCredentials | None, Security(bearer)],
    session: DatabaseSession,
    settings: AppSettings,
) -> User:
    if credentials is None:
        raise unauthorized()
    user_id = access_token_user_id(credentials.credentials, settings)
    user = session.get(User, user_id) if user_id is not None else None
    if user is None:
        raise unauthorized()
    return user


CurrentUser = Annotated[User, Depends(get_current_user)]


def require_admin(user: CurrentUser) -> User:
    if not user.is_admin:
        raise HTTPException(status.HTTP_403_FORBIDDEN, "Accès réservé aux administrateurs.")
    return user
