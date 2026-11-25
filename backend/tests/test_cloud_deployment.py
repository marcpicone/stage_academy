from pathlib import Path
from uuid import uuid4

import pytest
from alembic import command
from alembic.config import Config
from pydantic import ValidationError
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.config import Settings
from app.core.security import hash_password
from app.database import build_engine
from app.import_leaderboard import copy_records
from app.models import ScoreRound, User

from test_api import register


def test_hosted_requires_persistent_postgres_with_tls():
    base = {"_env_file": None, "jwt_secret_key": "a" * 40, "hosted_deployment": True}
    with pytest.raises(ValidationError, match="PostgreSQL"):
        Settings(**base)
    dsn = "postgresql+psycopg://test:example@localhost/quiz"
    with pytest.raises(ValidationError, match="TLS"):
        Settings(**base, database_url=dsn)
    assert Settings(**base, database_url=dsn + "?sslmode=require").hosted_deployment


def test_registration_without_invitation_and_health(client):
    response = register(client)
    assert response.status_code == 201
    assert client.get("/leaderboard").status_code == 401
    assert client.get("/health").json() == {"status": "ok"}


def test_copy_keeps_account_and_round_and_refuses_to_replace(client, tmp_path):
    source = client.app.state.session_factory.kw["bind"]
    with Session(source, expire_on_commit=False) as session:
        stored = User(
            username="legacy", email="legacy@example.com", password_hash=hash_password("test-password-33"),
            google_sub="historical-id",
        )
        session.add(stored)
        session.flush()
        round_id = uuid4()
        session.add(ScoreRound(id=round_id, user_id=stored.id, points=7))
        session.commit()
    destination = f"sqlite:///{(tmp_path / 'destination.db').as_posix()}"
    migration = Config(Path(__file__).resolve().parents[1] / "alembic.ini")
    migration.attributes["database_url"] = destination
    command.upgrade(migration, "head")
    target = build_engine(destination)
    try:
        assert copy_records(source, target) == (1, 1)
        with pytest.raises(ValueError, match="contient déjà"):
            copy_records(source, target)
        with Session(target) as session:
            copied = session.get(User, stored.id)
            assert copied.password_hash == stored.password_hash
            assert copied.google_sub is None
            assert [(item.id, item.user_id, item.points) for item in session.scalars(select(ScoreRound))] == [
                (round_id, stored.id, 7),
            ]
    finally:
        target.dispose()
