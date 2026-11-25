from typing import Literal
from uuid import UUID

from pydantic import BaseModel, Field, SecretStr

from app.schemas.user import RequestModel, Username


class LoginRequest(RequestModel):
    username: Username
    password: SecretStr = Field(min_length=1, max_length=128)


class TokenResponse(BaseModel):
    access_token: str
    token_type: Literal["bearer"] = "bearer"
    expires_in: int
    user_id: UUID
    username: str


class LoginResponse(TokenResponse):
    refresh_token: str


class SessionRequest(RequestModel):
    refresh_token: SecretStr = Field(min_length=43, max_length=43)
