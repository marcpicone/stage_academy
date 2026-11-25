from concurrent.futures import ThreadPoolExecutor
from uuid import uuid4

from test_api import login, register


def headers(client, username="camille"):
    return {"Authorization": f"Bearer {login(client, username=username).json()['access_token']}"}


def test_two_devices_merge_secrets_and_retry_credits_only_once(client):
    assert register(client).status_code == 201
    phone, desktop = headers(client), headers(client)
    assert client.get("/collection").status_code == 401
    update = {"id": str(uuid4()), "initial_points": 8, "earned": 2, "floats": {"Bug": 7_000_000_000}}
    assert client.post("/collection/sync", headers=phone, json=update).json()["points"] == 10
    assert client.post("/collection/sync", headers=phone, json=update).json()["points"] == 10
    request = {"id": str(uuid4()), "initial_points": 90, "earned": 1, "floats": {"Bug": 9_100_000_000, "Chameleon": 3}}
    merged = client.post("/collection/sync", headers=desktop, json=request)
    assert merged.status_code == 200
    assert client.get("/collection", headers=phone).json() == {
        "points": 11, "floats": {"Bug": 9_100_000_000, "Chameleon": 3}, "reward": None,
    }
    assert register(client, username="another", email="another@example.com").status_code == 201
    assert client.get("/collection", headers=headers(client, "another")).json()["floats"] == {}


def test_simultaneous_purchases_cannot_spend_the_same_points(client):
    assert register(client).status_code == 201
    phone, desktop = headers(client), headers(client)
    seed = {"id": str(uuid4()), "earned": 5, "floats": {}}
    assert client.post("/collection/sync", headers=phone, json=seed).status_code == 200
    purchases = [{"id": str(uuid4()), "crate": "Basic"} for _ in range(2)]

    def purchase(index):
        authorization = (phone, desktop)[index]
        return client.post("/collection/crates", headers=authorization, json=purchases[index])

    with ThreadPoolExecutor(max_workers=2) as executor:
        responses = list(executor.map(purchase, (0, 1)))
    assert sorted(response.status_code for response in responses) == [200, 409]
    winner = next(index for index, response in enumerate(responses) if response.status_code == 200)
    reward = responses[winner].json()
    assert reward["points"] == 0 and reward["reward"] in reward["floats"]
    assert purchase(winner).json() == reward
    stored = client.get("/collection", headers=desktop).json()
    assert stored["points"] == 0 and stored["floats"] == reward["floats"]


def test_collection_is_removed_with_account(client):
    assert register(client).status_code == 201
    user = headers(client)
    update = {"id": str(uuid4()), "earned": 0, "floats": {"Bug": 12}}
    assert client.post("/collection/sync", headers=user, json=update).status_code == 200
    assert client.delete("/users/me", headers=user).status_code == 204
    assert client.get("/collection", headers=user).status_code == 401
    assert register(client).status_code == 201
    assert client.get("/collection", headers=headers(client)).json()["floats"] == {}
