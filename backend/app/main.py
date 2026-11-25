from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from sqlalchemy import text
from sqlalchemy.exc import SQLAlchemyError
from sqlalchemy.orm import sessionmaker

from app.core.config import Settings
from app.database import build_engine
from app.dependencies import DatabaseSession
from app.routers import auth, collection, leaderboard, profile, users
from app.services.users import UserConflict


def create_app(settings: Settings | None = None) -> FastAPI:
    @asynccontextmanager
    async def lifespan(api: FastAPI) -> AsyncIterator[None]:
        api.state.settings = settings if settings is not None else Settings()
        api.state.auth_attempts = auth.AuthAttempts()
        engine = build_engine(api.state.settings.database_url)
        api.state.session_factory = sessionmaker(bind=engine, expire_on_commit=False)
        try:
            yield
        finally:
            engine.dispose()

    api = FastAPI(title="Stage Academy API", version="0.1.0", lifespan=lifespan)
    api.include_router(auth.router)
    api.include_router(users.router)
    api.include_router(leaderboard.router)
    api.include_router(collection.router)
    api.include_router(profile.router)

    @api.get("/health", include_in_schema=False)
    def health(session: DatabaseSession) -> JSONResponse:
        try:
            session.execute(text("SELECT 1"))
        except SQLAlchemyError:
            return JSONResponse({"status": "unavailable"}, status_code=503)
        return JSONResponse({"status": "ok"})

    @api.exception_handler(RequestValidationError)
    async def invalid_data(_: Request, error: RequestValidationError) -> JSONResponse:
        # FastAPI inclut normalement la valeur saisie dans ses erreurs de validation.
        issues = [
            {"loc": item["loc"], "type": item["type"], "msg": item["msg"]}
            for item in error.errors()
        ]
        return JSONResponse(status_code=status.HTTP_422_UNPROCESSABLE_CONTENT, content={"detail": issues})

    @api.exception_handler(UserConflict)
    async def user_conflict(_: Request, __: UserConflict) -> JSONResponse:
        return JSONResponse(
            status_code=status.HTTP_409_CONFLICT,
            content={"detail": "Ce nom de joueur ou cet e-mail est déjà utilisé."},
        )

    return api


app = create_app()
