"""Persist private player profiles and revocable login sessions."""

import sqlalchemy as sa
from alembic import op

revision = "0004_profiles_and_sessions"
down_revision = "0003_player_collections"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "login_sessions",
        sa.Column("token_hash", sa.String(64), primary_key=True),
        sa.Column("user_id", sa.Uuid(), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("expires_at", sa.Integer(), nullable=False),
    )
    op.create_index("ix_login_sessions_user_id", "login_sessions", ["user_id"])
    op.create_index("ix_login_sessions_expires_at", "login_sessions", ["expires_at"])
    op.create_table(
        "player_profiles",
        sa.Column("user_id", sa.Uuid(), sa.ForeignKey("users.id", ondelete="CASCADE"), primary_key=True),
        sa.Column("display_name", sa.String(64), nullable=False),
        sa.Column("avatar", sa.LargeBinary()),
    )


def downgrade() -> None:
    op.drop_table("player_profiles")
    op.drop_table("login_sessions")
