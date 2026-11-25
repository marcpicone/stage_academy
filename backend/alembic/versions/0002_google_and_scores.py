"""Store Google identities, pending sign-ins and ranked scores."""

import sqlalchemy as sa
from alembic import op

revision = "0002_google_and_scores"
down_revision = "0001_create_users"
branch_labels = None
depends_on = None


def upgrade() -> None:
    with op.batch_alter_table("users") as batch:
        batch.alter_column("password_hash", existing_type=sa.String(255), nullable=True)
        batch.add_column(sa.Column("google_sub", sa.String(255), nullable=True))
        batch.create_unique_constraint("uq_users_google_sub", ["google_sub"])
    op.create_table(
        "google_logins",
        sa.Column("id", sa.Uuid(), primary_key=True),
        sa.Column("poll_hash", sa.String(64), nullable=False),
        sa.Column("expires_at", sa.Integer(), nullable=False),
        sa.Column("status", sa.String(16), nullable=False),
        sa.Column("user_id", sa.Uuid(), sa.ForeignKey("users.id", ondelete="CASCADE")),
    )
    op.create_index("ix_google_logins_expires_at", "google_logins", ["expires_at"])
    op.create_table(
        "score_rounds",
        sa.Column("id", sa.Uuid(), primary_key=True),
        sa.Column("user_id", sa.Uuid(), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("points", sa.Integer(), nullable=False),
    )
    op.create_index("ix_score_rounds_user_id", "score_rounds", ["user_id"])


def downgrade() -> None:
    op.drop_table("score_rounds")
    op.drop_table("google_logins")
    # Les comptes Google ne possèdent aucun mot de passe local valide.
    op.execute("UPDATE users SET password_hash = '' WHERE password_hash IS NULL")
    with op.batch_alter_table("users") as batch:
        batch.drop_constraint("uq_users_google_sub", type_="unique")
        batch.drop_column("google_sub")
        batch.alter_column("password_hash", existing_type=sa.String(255), nullable=False)
