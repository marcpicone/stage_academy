"""Copie des comptes et du classement SQLite vers une base PostgreSQL vide."""

from argparse import ArgumentParser
from getpass import getpass
from os import getenv
from pathlib import Path
from sys import stdin

from sqlalchemy import insert, select
from sqlalchemy.engine import Engine, make_url
from sqlalchemy.exc import SQLAlchemyError

from app.core.config import Settings
from app.database import build_engine
from app.models import ScoreRound, User


def copy_records(source: Engine, target: Engine) -> tuple[int, int]:
    metadata_tables = (User.__table__, ScoreRound.__table__)
    counts: list[int] = []
    with source.connect() as snapshot, target.begin() as output:
        snapshot.exec_driver_sql("BEGIN")
        if target.dialect.name == "postgresql":
            output.exec_driver_sql("LOCK TABLE users, score_rounds IN ACCESS EXCLUSIVE MODE")
        if any(output.scalar(select(table.c.id).limit(1)) is not None for table in metadata_tables):
            raise ValueError("La destination contient déjà des comptes ou des scores.")

        for table in metadata_tables:
            count = 0
            for batch in snapshot.execute(select(table)).mappings().partitions(250):
                rows = [dict(row) for row in batch]
                if table is User.__table__:
                    for row in rows:
                        # Les comptes locaux n'ont pas besoin de l'ancien identifiant Google.
                        row["google_sub"] = None
                output.execute(insert(table), rows)
                count += len(rows)
            counts.append(count)
    return counts[0], counts[1]


def main() -> None:
    parser = ArgumentParser(description=__doc__)
    parser.parse_args()
    source_url = make_url(Settings().database_url)
    if source_url.get_backend_name() != "sqlite" or not source_url.database or not Path(source_url.database).is_file():
        parser.exit(1, "La base SQLite source doit être un fichier existant.\n")

    destination_url = getenv("DESTINATION_DATABASE_URL")
    if not destination_url:
        if not stdin.isatty():
            parser.exit(1, "Indique DESTINATION_DATABASE_URL ou utilise un terminal interactif.\n")
        destination_url = getpass("URL PostgreSQL de destination (non affichée) : ")
    try:
        destination = make_url(destination_url)
        if destination.get_backend_name() not in {"postgresql", "postgres"}:
            raise ValueError("Une destination PostgreSQL est requise.")
        if destination.query.get("sslmode") not in {"require", "verify-ca", "verify-full"}:
            raise ValueError("La connexion à la destination doit utiliser TLS.")
        source = build_engine(source_url.render_as_string(hide_password=False))
        try:
            target = build_engine(destination.set(drivername="postgresql+psycopg").render_as_string(hide_password=False))
            try:
                users, rounds = copy_records(source, target)
            finally:
                target.dispose()
        finally:
            source.dispose()
    except ValueError as error:
        parser.exit(1, f"{error}\n")
    except SQLAlchemyError:
        parser.exit(1, "Import annulé. Vérifie l'adresse, les droits et les migrations PostgreSQL.\n")
    print(f"Import vérifié : {users} compte(s), {rounds} partie(s).")


if __name__ == "__main__":
    main()
