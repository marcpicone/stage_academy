from uuid import UUID

from fastapi import APIRouter
from pydantic import BaseModel, ConfigDict, Field
from sqlalchemy import func, select
from sqlalchemy.dialects.postgresql import insert as postgres_insert
from sqlalchemy.dialects.sqlite import insert as sqlite_insert

from app.dependencies import CurrentUser, DatabaseSession
from app.models import User
from app.models.competition import ScoreRound

router = APIRouter(prefix="/leaderboard", tags=["Classement"])


class RoundScore(BaseModel):
    model_config = ConfigDict(extra="forbid")
    id: UUID
    points: int = Field(strict=True, ge=0, le=100)


class RoundBatch(BaseModel):
    rounds: list[RoundScore] = Field(max_length=100)


class Ranking(BaseModel):
    name: str
    points: int


def standings(session: DatabaseSession) -> list[Ranking]:
    total = func.sum(ScoreRound.points)
    query = (
        select(User.username, total)
        .join(ScoreRound, ScoreRound.user_id == User.id)
        .group_by(User.id, User.username)
        .order_by(total.desc(), User.username, User.id)
        .limit(100)
    )
    return [Ranking(name=name, points=points) for name, points in session.execute(query)]


@router.get("", response_model=list[Ranking])
def read_leaderboard(session: DatabaseSession, _user: CurrentUser) -> list[Ranking]:
    return standings(session)


@router.post("/rounds", response_model=list[Ranking])
def record_rounds(data: RoundBatch, session: DatabaseSession, user: CurrentUser) -> list[Ranking]:
    if data.rounds:
        rows = [dict(id=game.id, user_id=user.id, points=game.points) for game in data.rounds]
        insert = sqlite_insert if session.get_bind().dialect.name == "sqlite" else postgres_insert
        # Le même identifiant est accepté une seule fois, y compris après une relance réseau.
        session.execute(insert(ScoreRound).values(rows).on_conflict_do_nothing(index_elements=["id"]))
        session.commit()
    return standings(session)
