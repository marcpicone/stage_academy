from types import SimpleNamespace
from uuid import uuid4

from app import activate_password
from app.models import ScoreRound, User

from test_api import PASSWORD, login, register


def bearer(result):
    return {"Authorization": f"Bearer {result['access_token']}"}


def test_password_sessions_share_scores_and_protect_standings(client):
    created = register(client).json()
    phone = login(client).json()
    desktop = login(client).json()
    assert phone["user_id"] == desktop["user_id"] == created["id"]
    assert phone["expires_in"] > 0
    first = {"id": str(uuid4()), "points": 4}
    second = {"id": str(uuid4()), "points": 3}
    route = "/leaderboard/rounds"
    assert client.get("/leaderboard").status_code == 401
    assert client.post(route, json={"rounds": [first]}).status_code == 401
    assert client.post(route, headers=bearer(phone), json={"rounds": [first]}).status_code == 200
    assert client.post(route, headers=bearer(desktop), json={"rounds": [first, second]}).status_code == 200
    assert client.get("/leaderboard", headers=bearer(phone)).json() == [{"name": created["username"], "points": 7}]
    assert client.delete("/users/me", headers=bearer(phone)).status_code == 204
    assert client.get("/leaderboard", headers=bearer(desktop)).status_code == 401


def test_google_entry_point_is_removed_and_password_attempts_are_limited(client):
    assert client.post("/auth/google/start").status_code == 404
    for _ in range(15):
        assert login(client).status_code == 401
    limited = login(client)
    assert limited.status_code == 429
    assert limited.headers["Retry-After"] == "60"


def test_local_password_activation_keeps_existing_points(client, monkeypatch):
    with client.app.state.session_factory() as session:
        legacy = User(username="legacy-player", email="legacy@example.com", google_sub="old-subject")
        session.add(legacy)
        session.flush()
        user_id = legacy.id
        session.add(ScoreRound(id=uuid4(), user_id=user_id, points=5))
        session.commit()

    assert login(client, username="legacy-player").status_code == 401
    monkeypatch.setattr(activate_password, "Settings", lambda: client.app.state.settings)
    monkeypatch.setattr(activate_password, "stdin", SimpleNamespace(isatty=lambda: True))
    monkeypatch.setattr(activate_password, "getpass", lambda _prompt: PASSWORD)
    monkeypatch.setattr("sys.argv", ["activate_password"])
    activate_password.main()

    logged_in = login(client, username="legacy-player").json()
    assert logged_in["user_id"] == str(user_id)
    assert client.get("/leaderboard", headers=bearer(logged_in)).json() == [{"name": "legacy-player", "points": 5}]
    with client.app.state.session_factory() as session:
        assert session.get(User, user_id).google_sub is None
