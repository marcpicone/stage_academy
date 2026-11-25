from uuid import UUID

from sqlalchemy import ForeignKey, Integer, JSON, String, Uuid
from sqlalchemy.orm import Mapped, mapped_column

from app.database import Base


class PlayerCollection(Base):
    __tablename__ = "player_collections"

    user_id: Mapped[UUID] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), primary_key=True)
    points: Mapped[int] = mapped_column(Integer)
    floats: Mapped[dict[str, int]] = mapped_column(JSON)


class CollectionOperation(Base):
    __tablename__ = "collection_operations"

    user_id: Mapped[UUID] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), primary_key=True)
    id: Mapped[UUID] = mapped_column(Uuid, primary_key=True)
    reward: Mapped[str | None] = mapped_column(String(48))
