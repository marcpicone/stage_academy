from datetime import UTC, datetime, timedelta
from uuid import UUID

import jwt
from pwdlib import PasswordHash
from pwdlib.hashers.argon2 import Argon2Hasher

from app.core.config import Settings

ALGORITHM = "HS256"
ISSUER = "stage-academy"
AUDIENCE = "stage-academy-api"

password_hasher = PasswordHash((Argon2Hasher(),))
# Vérifier aussi un hachage quand le compte est inconnu réduit les écarts de temps de réponse.
dummy_hash = password_hasher.hash("dummy-verification-value")


def hash_password(password: str) -> str:
    return password_hasher.hash(password)


def verify_password(password: str, password_hash: str | None) -> bool:
    valid = password_hasher.verify(password, password_hash or dummy_hash)
    return valid and bool(password_hash)


def create_access_token(user_id: UUID, settings: Settings) -> str:
    now = datetime.now(UTC)
    return jwt.encode(
        {
            "sub": str(user_id),
            "iat": now,
            "nbf": now,
            "exp": now + timedelta(minutes=settings.jwt_access_token_minutes),
            "iss": ISSUER,
            "aud": AUDIENCE,
        },
        settings.jwt_secret_key.get_secret_value(),
        algorithm=ALGORITHM,
    )


def access_token_user_id(token: str, settings: Settings) -> UUID | None:
    try:
        claims = jwt.decode(
            token,
            settings.jwt_secret_key.get_secret_value(),
            algorithms=[ALGORITHM],
            audience=AUDIENCE,
            issuer=ISSUER,
            options={"require": ["sub", "iat", "nbf", "exp", "iss", "aud"]},
        )
        return UUID(claims["sub"])
    except (jwt.InvalidTokenError, ValueError):
        return None
