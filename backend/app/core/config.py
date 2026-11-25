from pathlib import Path

from pydantic import Field, SecretStr, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict
from sqlalchemy.engine import make_url
from sqlalchemy.exc import ArgumentError

BACKEND_DIR = Path(__file__).resolve().parents[2]


class Settings(BaseSettings):
    database_url: str = "sqlite:///./local.db"
    jwt_secret_key: SecretStr = Field(min_length=32)
    jwt_access_token_minutes: int = Field(default=30, ge=1, le=1440)
    hosted_deployment: bool = False

    model_config = SettingsConfigDict(
        env_file=BACKEND_DIR / ".env",
        env_file_encoding="utf-8",
        extra="ignore",
        hide_input_in_errors=True,
    )

    @model_validator(mode="after")
    def validate_hosted_settings(self) -> "Settings":
        if self.hosted_deployment:
            if not self.database_url.startswith("postgresql+psycopg://"):
                raise ValueError("Le déploiement nécessite une base PostgreSQL persistante.")
            try:
                url = make_url(self.database_url)
            except (ArgumentError, ValueError):
                raise ValueError("L’adresse PostgreSQL est invalide.") from None
            if url.query.get("sslmode") not in {"require", "verify-ca", "verify-full"}:
                raise ValueError("La connexion PostgreSQL doit utiliser TLS.")
        return self
