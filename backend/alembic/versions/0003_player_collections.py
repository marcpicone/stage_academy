"""Persist shared collections and their synchronization receipts."""

import sqlalchemy as sa
from alembic import op

revision = "0003_player_collections"
down_revision = "0002_google_and_scores"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "player_collections",
        sa.Column("user_id", sa.Uuid(), sa.ForeignKey("users.id", ondelete="CASCADE"), primary_key=True),
        sa.Column("points", sa.Integer(), nullable=False),
        sa.Column("floats", sa.JSON(), nullable=False),
    )
    op.create_table(
        "collection_operations",
        sa.Column("user_id", sa.Uuid(), sa.ForeignKey("users.id", ondelete="CASCADE"), primary_key=True),
        sa.Column("id", sa.Uuid(), primary_key=True),
        sa.Column("reward", sa.String(48)),
    )


def downgrade() -> None:
    op.drop_table("collection_operations")
    op.drop_table("player_collections")
