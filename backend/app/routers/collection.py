from secrets import choice, randbelow
from typing import Annotated, Literal
from uuid import UUID

from fastapi import APIRouter, HTTPException
from pydantic import Field, StringConstraints
from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert as postgres_insert
from sqlalchemy.dialects.sqlite import insert as sqlite_insert

from app.dependencies import CurrentUser, DatabaseSession
from app.models import CollectionOperation, PlayerCollection
from app.schemas.user import RequestModel

router = APIRouter(prefix="/collection", tags=["Collection"])
MAX_POINTS = 2_147_483_647
FLOAT_SCALE = 10_000_000_000
Quality = Annotated[int, Field(strict=True, ge=0, lt=FLOAT_SCALE)]
SecretName = Annotated[str, StringConstraints(pattern=r"^[A-Za-z][A-Za-z0-9]{0,47}$")]
CRATES = {
    "Basic": (5, (80, 18, 2)),
    "Advanced": (12, (50, 37, 13)),
    "Prestige": (25, (20, 45, 35)),
}
REWARDS = (
    ("RubberDuck", "FloppyDisk"),
    ("NeonJellyfish", "ClockworkOwl"),
    ("GoldenCartridge", "PocketGalaxy"),
)


class CollectionResponse(RequestModel):
    points: int
    floats: dict[str, int]
    reward: str | None = None


class CollectionUpdate(RequestModel):
    id: UUID
    earned: int = Field(strict=True, ge=0, le=MAX_POINTS)
    initial_points: int = Field(default=0, strict=True, ge=0, le=MAX_POINTS)
    floats: dict[SecretName, Quality] = Field(max_length=100)


class CratePurchase(RequestModel):
    id: UUID
    crate: Literal["Basic", "Advanced", "Prestige"]


def result(state: PlayerCollection | None, reward: str | None = None) -> CollectionResponse:
    if state is None:
        return CollectionResponse(points=0, floats={})
    return CollectionResponse(points=state.points, floats=state.floats, reward=reward)


def lock_collection(session: DatabaseSession, user_id: UUID, initial_points: int = 0) -> PlayerCollection:
    # L'insertion prend le verrou d'écriture sur SQLite et SELECT FOR UPDATE protège PostgreSQL.
    insert = sqlite_insert if session.get_bind().dialect.name == "sqlite" else postgres_insert
    session.execute(
        insert(PlayerCollection)
        .values(user_id=user_id, points=initial_points, floats={})
        .on_conflict_do_nothing(index_elements=["user_id"])
    )
    return session.execute(
        select(PlayerCollection).where(PlayerCollection.user_id == user_id).with_for_update()
    ).scalar_one()


@router.get("", response_model=CollectionResponse)
def get_collection(session: DatabaseSession, user: CurrentUser) -> CollectionResponse:
    return result(session.get(PlayerCollection, user.id))


@router.post("/sync", response_model=CollectionResponse)
def sync_collection(data: CollectionUpdate, session: DatabaseSession, user: CurrentUser) -> CollectionResponse:
    state = lock_collection(session, user.id, data.initial_points)
    if session.get(CollectionOperation, (user.id, data.id)) is None:
        if len(state.floats.keys() | data.floats.keys()) > 100:
            raise HTTPException(422, "La collection contient trop de secrets.")
        state.points = min(MAX_POINTS, state.points + data.earned)
        state.floats = {
            **state.floats,
            **{name: max(value, state.floats.get(name, 0)) for name, value in data.floats.items()},
        }
        session.add(CollectionOperation(user_id=user.id, id=data.id))
    session.commit()
    return result(state)


@router.post("/crates", response_model=CollectionResponse)
def buy_crate(data: CratePurchase, session: DatabaseSession, user: CurrentUser) -> CollectionResponse:
    state = lock_collection(session, user.id)
    previous = session.get(CollectionOperation, (user.id, data.id))
    if previous is not None:
        return result(state, previous.reward)

    price, weights = CRATES[data.crate]
    available = [(weight, [name for name in pool if name not in state.floats])
                 for weight, pool in zip(weights, REWARDS, strict=True)]
    available = [(weight, pool) for weight, pool in available if pool]
    if state.points < price or not available:
        raise HTTPException(409, "Solde insuffisant ou collection de caisses complète.")

    total = sum(weight for weight, _ in available)
    ticket = randbelow(100)
    cumulative = 0
    for weight, pool in available:
        cumulative += weight
        if ticket < cumulative * 100 // total:
            reward = choice(pool)
            break

    state.points -= price
    state.floats = {**state.floats, reward: randbelow(FLOAT_SCALE)}
    session.add(CollectionOperation(user_id=user.id, id=data.id, reward=reward))
    session.commit()
    return result(state, reward)
