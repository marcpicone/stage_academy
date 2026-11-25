"""Activer un mot de passe local sur un ancien compte OAuth depuis le serveur."""

from argparse import ArgumentParser
from getpass import getpass
from sys import stdin

from sqlalchemy import select, update
from sqlalchemy.orm import Session

from app.core.config import Settings
from app.core.security import hash_password
from app.database import build_engine
from app.models import User


def main() -> None:
    parser = ArgumentParser(description="Définir le premier mot de passe local d'un ancien compte.")
    parser.add_argument("username", nargs="?", help="nom du compte, facultatif si un seul compte est éligible")
    args = parser.parse_args()
    if not stdin.isatty():
        parser.exit(1, "Lance cette commande dans un terminal interactif.\n")

    engine = build_engine(Settings().database_url)
    try:
        with Session(engine) as session:
            query = select(User.id, User.username).where(User.password_hash.is_(None))
            if args.username:
                query = query.where(User.username == args.username.strip().lower())
            matches = session.execute(query.limit(2)).all()
        if len(matches) != 1:
            parser.exit(1, "Indique le nom d'un compte sans mot de passe local.\n")

        user_id, username = matches[0]
        print(f"Nom de connexion : {username}")
        password = getpass("Nouveau mot de passe (12 à 128 caractères) : ")
        if not 12 <= len(password) <= 128 or password != getpass("Confirme le mot de passe : "):
            parser.exit(1, "Longueur incorrecte ou confirmation différente.\n")
        hashed = hash_password(password)
        with Session(engine) as session:
            result = session.execute(
                update(User)
                .where(User.id == user_id, User.password_hash.is_(None))
                .values(password_hash=hashed, google_sub=None)
            )
            if result.rowcount != 1:
                parser.exit(1, "Le compte a déjà été modifié.\n")
            session.commit()
        print(f"Mot de passe activé pour {username}. Les points restent liés au même compte.")
    finally:
        engine.dispose()


if __name__ == "__main__":
    main()
