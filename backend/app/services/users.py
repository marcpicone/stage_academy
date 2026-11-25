from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.core.security import hash_password, verify_password
from app.models import User
from app.schemas.auth import LoginRequest
from app.schemas.user import UserPatch, UserProfile, UserRegistration


class UserConflict(Exception):
    pass


def register_user(session: Session, data: UserRegistration) -> User:
    user = User(
        username=data.username,
        email=data.email,
        password_hash=hash_password(data.password.get_secret_value()),
    )
    return persist_user(session, user)


def authenticate_user(session: Session, data: LoginRequest) -> User | None:
    user = session.scalar(select(User).where(User.username == data.username))
    hashed = user.password_hash if user else None
    return user if verify_password(data.password.get_secret_value(), hashed) else None


def update_profile(session: Session, user: User, data: UserProfile | UserPatch) -> User:
    if data.username is not None:
        user.username = data.username
    if data.email is not None:
        user.email = data.email
    return persist_user(session, user)


def list_users(session: Session, offset: int, limit: int) -> list[User]:
    query = select(User).order_by(User.created_at, User.id).offset(offset).limit(limit)
    return list(session.scalars(query))


def delete_user(session: Session, user: User) -> None:
    session.delete(user)
    session.commit()


def persist_user(session: Session, user: User) -> User:
    session.add(user)
    try:
        session.commit()
    except IntegrityError as error:
        # Les contraintes en base assurent l’unicité même avec des requêtes simultanées.
        session.rollback()
        raise UserConflict from error
    session.refresh(user)
    return user
