from datetime import UTC, datetime, timedelta

import jwt
import pytest
from sqlalchemy import select

from app.core.security import ALGORITHM, AUDIENCE, ISSUER
from app.models import User

PASSWORD = "Fake-example-password-123"
USER = {"username": "Camille", "email": "camille@example.com", "password": PASSWORD}


def register(client, **changes):
    return client.post("/auth/register", json=USER | changes)


def login(client, username="camille", password=PASSWORD):
    return client.post("/auth/login", json={"username": username, "password": password})


def auth_headers(client):
    token = login(client).json()["access_token"]
    return {"Authorization": f"Bearer {token}"}


def test_register_stores_an_argon2id_hash_and_returns_public_fields(client):
    response = register(client)
    assert response.status_code == 201
    body = response.json()
    assert set(body) == {"id", "username", "email", "created_at", "is_admin"}
    assert body["username"] == "camille"
    assert body["email"] == "camille@example.com"
    assert body["is_admin"] is False
    assert datetime.fromisoformat(body["created_at"]).utcoffset() == timedelta(0)
    with client.app.state.session_factory() as session:
        stored = session.scalar(select(User))
        assert stored.password_hash.startswith("$argon2id$")
        assert PASSWORD not in stored.password_hash


@pytest.mark.parametrize("changes", [{"email": "autre@example.com"}, {"username": "autre"}])
def test_duplicate_username_or_email_returns_conflict(client, changes):
    assert register(client).status_code == 201
    response = register(client, **changes)
    assert response.status_code == 409
    assert PASSWORD not in response.text


def test_login_returns_a_bearer_token_that_opens_the_profile(client):
    created = register(client).json()
    response = login(client, username="Camille")
    assert response.status_code == 200
    assert response.json()["token_type"] == "bearer"
    profile = client.get("/users/me", headers={"Authorization": f"Bearer {response.json()['access_token']}"})
    assert profile.status_code == 200
    assert profile.json() == created


def test_wrong_credentials_return_the_same_error_for_unknown_users(client):
    assert register(client).status_code == 201
    bad = login(client, password="incorrect")
    unknown = login(client, username="inconnu", password="incorrect")
    assert bad.status_code == unknown.status_code == 401
    assert bad.json() == unknown.json()


@pytest.mark.parametrize("path", ["/users/me", "/users"])
def test_private_routes_require_a_bearer_token(client, path):
    response = client.get(path)
    assert response.status_code == 401
    assert response.headers["www-authenticate"] == "Bearer"
    assert client.get(path, headers={"Authorization": "Bearer invalid"}).status_code == 401


@pytest.mark.parametrize(
    "changes",
    [{"email": "not-an-email"}, {"username": "x"}, {"password": "secret-test"}, {"is_admin": True}],
)
def test_invalid_registration_does_not_echo_credentials(client, changes):
    response = register(client, **changes)
    assert response.status_code == 422
    assert PASSWORD not in response.text
    assert "secret-test" not in response.text
    assert all("input" not in issue for issue in response.json()["detail"])


def test_put_patch_delete_and_no_transferred_access_after_deletion(client):
    old_id = register(client).json()["id"]
    headers = auth_headers(client)
    put = client.put("/users/me", headers=headers, json={"username": "nouveau", "email": "new@example.com"})
    assert put.status_code == 200
    assert put.json()["id"] == old_id
    patch = client.patch("/users/me", headers=headers, json={"email": "better@example.com"})
    assert patch.status_code == 200
    assert patch.json()["username"] == "nouveau"
    assert patch.json()["email"] == "better@example.com"

    for invalid in ({}, {"email": None}, {"is_admin": True}):
        assert client.patch("/users/me", headers=headers, json=invalid).status_code == 422

    deleted = client.delete("/users/me", headers=headers)
    assert deleted.status_code == 204
    assert deleted.content == b""
    assert register(client).json()["id"] != old_id
    assert client.get("/users/me", headers=headers).status_code == 401


def test_users_list_is_admin_only_and_paginated(client):
    assert register(client).status_code == 201
    assert register(client, username="autre", email="autre@example.com").status_code == 201
    headers = auth_headers(client)
    assert client.get("/users", headers=headers).status_code == 403
    with client.app.state.session_factory() as session:
        user = session.scalar(select(User).where(User.username == "camille"))
        user.is_admin = True
        session.commit()
    response = client.get("/users?limit=1&offset=1", headers=headers)
    assert response.status_code == 200
    assert len(response.json()) == 1
    assert "password_hash" not in response.json()[0]


def test_an_expired_token_is_rejected(client):
    user = register(client).json()
    issued = datetime.now(UTC) - timedelta(hours=1)
    expired_token = jwt.encode(
        {"sub": user["id"], "iat": issued, "nbf": issued, "exp": issued + timedelta(minutes=1),
         "iss": ISSUER, "aud": AUDIENCE},
        client.app.state.settings.jwt_secret_key.get_secret_value(),
        algorithm=ALGORITHM,
    )
    assert client.get("/users/me", headers={"Authorization": f"Bearer {expired_token}"}).status_code == 401
