from typing import Annotated

from fastapi import APIRouter, Depends, Query, Response, status

from app.dependencies import CurrentUser, DatabaseSession, require_admin
from app.models import User
from app.schemas.user import UserPatch, UserProfile, UserResponse
from app.services.users import delete_user, list_users, update_profile

router = APIRouter(prefix="/users", tags=["Utilisateurs"])


@router.get("/me", response_model=UserResponse)
def read_profile(user: CurrentUser) -> User:
    return user


@router.put("/me", response_model=UserResponse)
def replace_profile(data: UserProfile, user: CurrentUser, session: DatabaseSession) -> User:
    return update_profile(session, user, data)


@router.patch("/me", response_model=UserResponse)
def patch_profile(data: UserPatch, user: CurrentUser, session: DatabaseSession) -> User:
    return update_profile(session, user, data)


@router.delete("/me", status_code=status.HTTP_204_NO_CONTENT)
def remove_profile(user: CurrentUser, session: DatabaseSession) -> Response:
    delete_user(session, user)
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.get("", response_model=list[UserResponse], dependencies=[Depends(require_admin)])
def read_users(
    session: DatabaseSession,
    offset: Annotated[int, Query(ge=0)] = 0,
    limit: Annotated[int, Query(ge=1, le=100)] = 20,
) -> list[User]:
    return list_users(session, offset, limit)
