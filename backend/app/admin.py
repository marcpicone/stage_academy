from argparse import ArgumentParser

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.config import Settings
from app.database import build_engine
from app.models import User


def main() -> None:
    parser = ArgumentParser(description="Donner le rôle administrateur à un utilisateur inscrit.")
    parser.add_argument("username", help="nom de l’utilisateur à promouvoir")
    args = parser.parse_args()
    engine = build_engine(Settings().database_url)
    try:
        with Session(engine) as session:
            user = session.scalar(select(User).where(User.username == args.username.strip().lower()))
            if user is None:
                parser.exit(status=1, message="Utilisateur introuvable. Inscris-le d’abord.\n")
            user.is_admin = True
            session.commit()
            print(f"{user.username} est administrateur.")
    finally:
        engine.dispose()


if __name__ == "__main__":
    main()
