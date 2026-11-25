from datetime import UTC, datetime
from typing import Annotated, Self
from uuid import UUID

from pydantic import AfterValidator, BaseModel, ConfigDict, EmailStr, Field, SecretStr, StringConstraints, field_validator, model_validator

Username = Annotated[
    str,
    StringConstraints(strip_whitespace=True, to_lower=True, min_length=3, max_length=32, pattern=r"^[a-zA-Z0-9_.-]+$"),
]
Email = Annotated[EmailStr, AfterValidator(str.lower)]


class RequestModel(BaseModel):
    model_config = ConfigDict(extra="forbid")


class UserProfile(RequestModel):
    username: Username
    email: Email


class UserRegistration(UserProfile):
    password: SecretStr = Field(min_length=12, max_length=128)


class UserPatch(RequestModel):
    username: Username | None = None
    email: Email | None = None

    @model_validator(mode="after")
    def validate_fields(self) -> Self:
        if not self.model_fields_set or any(getattr(self, name) is None for name in self.model_fields_set):
            raise ValueError("Indique au moins un champ avec une valeur.")
        return self


class UserResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    username: str
    email: EmailStr
    created_at: datetime
    is_admin: bool

    @field_validator("created_at")
    @classmethod
    def use_utc(cls, value: datetime) -> datetime:
        # SQLite restitue souvent un datetime sans fuseau, même si on enregistre l’heure UTC.
        return value.replace(tzinfo=UTC) if value.tzinfo is None else value.astimezone(UTC)
