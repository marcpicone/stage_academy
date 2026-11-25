from base64 import b64decode, b64encode
from io import BytesIO
from typing import Annotated

from fastapi import APIRouter, HTTPException, Response
from PIL import Image
from pydantic import Field, StringConstraints
from sqlalchemy.dialects.postgresql import insert as postgres_insert
from sqlalchemy.dialects.sqlite import insert as sqlite_insert

from app.dependencies import CurrentUser, DatabaseSession
from app.models import PlayerProfile
from app.schemas.user import RequestModel

router = APIRouter(prefix="/users/me/player-profile", tags=["Profil du joueur"])
MAX_AVATAR = 262_144


class ProfileData(RequestModel):
    display_name: Annotated[str, StringConstraints(strip_whitespace=True, min_length=1, max_length=64)]
    avatar: str | None = Field(default=None, max_length=4 * ((MAX_AVATAR + 2) // 3))


def clean_avatar(encoded: str | None) -> bytes | None:
    if encoded is None:
        return None
    try:
        data = b64decode(encoded, validate=True)
        if not data or len(data) > MAX_AVATAR:
            raise ValueError
        with Image.open(BytesIO(data), formats=["JPEG"]) as image:
            if not (1 <= image.width <= 1024 and 1 <= image.height <= 1024):
                raise ValueError
            with BytesIO() as output:
                # Réencoder enlève les métadonnées de prise de vue et valide les pixels.
                image.convert("RGB").save(output, format="JPEG", quality=85)
                avatar = output.getvalue()
                if len(avatar) > MAX_AVATAR:
                    raise ValueError
                return avatar
    except (ValueError, OSError, Image.DecompressionBombError):
        raise HTTPException(422, "La photo du profil est invalide.") from None


def response(profile: PlayerProfile) -> ProfileData:
    return ProfileData(
        display_name=profile.display_name,
        avatar=b64encode(profile.avatar).decode("ascii") if profile.avatar else None,
    )


@router.get("", response_model=ProfileData | None)
def load_profile(user: CurrentUser, session: DatabaseSession, reply: Response) -> ProfileData | None:
    reply.headers["Cache-Control"] = "no-store"
    profile = session.get(PlayerProfile, user.id)
    return response(profile) if profile is not None else None


@router.put("", response_model=ProfileData)
def save_profile(data: ProfileData, user: CurrentUser, session: DatabaseSession, reply: Response) -> ProfileData:
    insert = sqlite_insert if session.get_bind().dialect.name == "sqlite" else postgres_insert
    values = {"display_name": data.display_name}
    if "avatar" in data.model_fields_set:
        values["avatar"] = clean_avatar(data.avatar)
    statement = insert(PlayerProfile).values(user_id=user.id, **values)
    saved = session.execute(
        statement.on_conflict_do_update(index_elements=["user_id"], set_=values).returning(PlayerProfile)
    ).scalar_one()
    session.commit()
    reply.headers["Cache-Control"] = "no-store"
    return response(saved)
