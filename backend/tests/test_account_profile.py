from base64 import b64decode, b64encode
from hashlib import sha256
from io import BytesIO

from PIL import Image

from app.models import LoginSession

from test_api import login, register


def auth(token):
    return {"Authorization": f"Bearer {token}"}


def test_remembered_session_renews_access_until_device_logout(client):
    assert register(client).status_code == 201
    phone = login(client).json()
    desktop = login(client).json()
    assert phone["refresh_token"] != desktop["refresh_token"]
    with client.app.state.session_factory() as session:
        digest = sha256(phone["refresh_token"].encode()).hexdigest()
        assert session.get(LoginSession, digest).user_id.hex == phone["user_id"].replace("-", "")
        assert session.get(LoginSession, phone["refresh_token"]) is None

    credentials = {"refresh_token": phone["refresh_token"]}
    renewed = client.post("/auth/refresh", json=credentials)
    assert renewed.status_code == 200
    assert renewed.headers["Cache-Control"] == "no-store"
    assert renewed.json()["user_id"] == phone["user_id"]
    assert client.post("/auth/logout", json=credentials).status_code == 204
    assert client.post("/auth/refresh", json=credentials).status_code == 401
    assert client.post("/auth/refresh", json={"refresh_token": desktop["refresh_token"]}).status_code == 200


def test_profile_name_and_photo_are_shared_and_private(client):
    assert register(client).status_code == 201
    first = auth(login(client).json()["access_token"])
    second = auth(login(client).json()["access_token"])
    route = "/users/me/player-profile"
    assert client.get(route).status_code == 401
    assert client.get(route, headers=first).json() is None

    image = Image.new("RGB", (20, 20), "green")
    metadata = Image.Exif()
    metadata[270] = "test metadata"
    encoded = BytesIO()
    image.save(encoded, format="JPEG", exif=metadata)
    payload = {"display_name": "  Camille du quiz  ", "avatar": b64encode(encoded.getvalue()).decode()}
    saved = client.put(route, json=payload, headers=first)
    assert saved.status_code == 200
    assert saved.json()["display_name"] == "Camille du quiz"
    assert client.get(route, headers=second).json() == saved.json()
    assert client.get("/users/me", headers=second).json()["username"] == "camille"
    with Image.open(BytesIO(b64decode(saved.json()["avatar"]))) as avatar:
        assert avatar.size == (20, 20)
        assert not avatar.getexif()

    assert register(client, username="other", email="other@example.com").status_code == 201
    other = auth(login(client, username="other").json()["access_token"])
    assert client.get(route, headers=other).json() is None
    assert client.put(route, json={"display_name": "   ", "avatar": None}, headers=first).status_code == 422
    assert client.put(route, json={"display_name": "Camille", "avatar": "not an image"}, headers=first).status_code == 422
    renamed = client.put(route, json={"display_name": "Camille"}, headers=second)
    assert renamed.json()["avatar"] == saved.json()["avatar"]
    cleared = client.put(route, json={"display_name": "Camille", "avatar": None}, headers=second)
    assert cleared.status_code == 200
    assert client.get(route, headers=first).json() == {"display_name": "Camille", "avatar": None}
