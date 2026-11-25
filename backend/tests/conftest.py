from pathlib import Path

from alembic import command
from alembic.config import Config
from fastapi.testclient import TestClient
import pytest

from app.core.config import Settings
from app.main import create_app


@pytest.fixture
def client(tmp_path: Path):
    settings = Settings(
        _env_file=None,
        database_url=f"sqlite:///{(tmp_path / 'test.db').as_posix()}",
        jwt_secret_key="test-signing-key-long-enough-for-hs256",
    )
    migration = Config(Path(__file__).resolve().parents[1] / "alembic.ini")
    migration.attributes["database_url"] = settings.database_url
    command.upgrade(migration, "head")
    # Les tests passent par la vraie migration avec une base SQLite indépendante par test.
    with TestClient(create_app(settings)) as test_client:
        yield test_client
