# Stage Academy API

Cette API FastAPI permet de créer un compte, de se connecter et de gérer son
profil. La première version utilise SQLite et se lance en local.

## 1. Installer et démarrer sous Windows

Le projet utilise `uv` et Python 3.15.2. Si `uv` manque, installe-le avec cette
commande, puis rouvre PowerShell. [Guide d’installation de uv](https://docs.astral.sh/uv/getting-started/installation/#winget)

```powershell
winget install --id astral-sh.uv -e
```

Si `uv` ne trouve pas Python 3.15.2, installe cette version depuis le site
officiel de [Python pour Windows](https://www.python.org/downloads/windows/).
Depuis la racine du projet, crée ensuite l’environnement virtuel et installe
les dépendances.

```powershell
cd backend
uv venv --python 3.15.2
uv sync --locked
```

Crée ton fichier `.env` à partir du modèle. Cette commande génère une clé
aléatoire pour signer les jetons JWT et conserve le fichier s’il existe déjà.

```powershell
if (!(Test-Path -LiteralPath '.env')) {
    uv run --locked python -c "from pathlib import Path; import secrets; example = Path('.env.example').read_text(encoding='utf-8'); Path('.env').open('x', encoding='utf-8').write(example.replace('JWT_SECRET_KEY=', 'JWT_SECRET_KEY=' + secrets.token_urlsafe(48)))"
}
```

Applique la migration. Alembic crée le fichier SQLite `local.db` et sa table
`users`.

```powershell
uv run --locked alembic upgrade head
```

Démarre Uvicorn et ouvre [Swagger](http://127.0.0.1:8000/docs).

```powershell
uv run --locked uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload
```

## 2. Tester l’API dans Swagger

1. Ouvre **POST /auth/register** et clique sur **Try it out**. Colle ce JSON et
   clique sur **Execute**. Le code **201** indique que le compte a été créé.

   ```json
   {
     "username": "camille",
     "email": "camille@example.com",
     "password": "Exemple-de-mot-de-passe-123"
   }
   ```

2. Ouvre **POST /auth/login** et clique sur **Try it out**. Envoie le même
   `username` et le même `password` dans le JSON. Copie la valeur
   d’`access_token` dans la réponse **200**.
3. Clique sur **Authorize** en haut de la page et colle le jeton seul dans
   **Value**. Swagger ajoutera le préfixe `Bearer` dans l’en-tête
   `Authorization`. Le jeton expire au bout de 30 minutes par défaut.
4. Lance **GET /users/me** pour voir le profil. Lance-le aussi après **Logout**
   dans Swagger pour observer une réponse **401**.

Pour tester une nouvelle inscription, utilise un autre nom et une autre
adresse. Un doublon donne une réponse **409**.

## 3. Routes disponibles

| Méthode et route | Action | Succès |
| --- | --- | --- |
| `POST /auth/register` | Créer un compte | 201 |
| `POST /auth/login` | Obtenir un JWT avec le nom et le mot de passe | 200 |
| `GET /users/me` | Lire le profil connecté | 200 |
| `PUT /users/me` | Modifier le nom et l’adresse e-mail ensemble | 200 |
| `PATCH /users/me` | Modifier un ou plusieurs champs du profil | 200 |
| `DELETE /users/me` | Supprimer le compte connecté | 204, corps vide |
| `GET /users?offset=0&limit=20` | Lister les comptes en tant qu’administrateur | 200 |

Pour tester le rôle administrateur, inscris le compte puis lance cette commande
dans un deuxième terminal, depuis `backend/`.

```powershell
uv run --locked python -m app.admin camille
```

L’API lit le rôle dans la base à chaque requête privée. L’inscription et
l’édition du profil donnent accès uniquement aux champs prévus.

## 4. Où se trouve le code

| Fichier ou dossier | Rôle |
| --- | --- |
| `app/main.py` | Crée l’API, monte les routes et prépare les réponses d’erreur. |
| `app/core/config.py` | Lit l’URL de la base, la clé JWT et la durée des jetons. |
| `app/core/security.py` | Hache les mots de passe avec Argon2id et signe et vérifie les JWT. |
| `app/database.py` | Crée le moteur SQLAlchemy et fournit une session par requête. |
| `app/models/user.py` | Décrit la table des utilisateurs et ses contraintes d’unicité. |
| `app/schemas/auth.py`, `app/schemas/user.py` | Définissent les données reçues et les champs retournés. |
| `app/routers/auth.py`, `app/routers/users.py` | Définissent les URL, les méthodes et les codes HTTP. |
| `app/services/users.py` | Contient les opérations sur les comptes et les accès à la base. |
| `app/dependencies.py` | Identifie l’utilisateur connecté et contrôle le rôle administrateur. |
| `app/admin.py` | Permet de promouvoir un compte depuis le terminal du serveur. |
| `alembic.ini`, `alembic/` | Configurent Alembic et conservent les migrations. |
| `tests/conftest.py`, `tests/test_api.py` | Préparent des bases isolées et testent l’API. |
| `pyproject.toml`, `uv.lock`, `.python-version` | Déclarent les dépendances, figent leurs versions et choisissent Python. |
| `.env.example`, `.gitignore` | Donnent le modèle de configuration locale et gardent les secrets et fichiers générés hors de Git. |

Quand tu t’inscris, Pydantic vérifie les champs, le service transforme le mot
de passe en hachage Argon2id et SQLAlchemy enregistre le compte. Quand tu te
connectes, le service vérifie ce hachage et renvoie un jeton signé. Le schéma
de réponse du profil contient uniquement les champs publics du compte.

## 5. Tests et migrations

```powershell
uv run --locked python -m pytest
uv run --locked alembic check
```

Après une modification des modèles, génère une nouvelle migration, relis le
fichier généré et applique-la.

```powershell
uv run --locked alembic revision --autogenerate -m "description du changement"
uv run --locked alembic upgrade head
```

## 6. Passer plus tard à PostgreSQL

Crée une base PostgreSQL, active le pilote et remplace `DATABASE_URL` dans
`.env` par l’adresse de cette base.

```powershell
uv sync --locked --extra postgres
```

```dotenv
DATABASE_URL=postgresql+psycopg://utilisateur:mot_de_passe@localhost:5432/stage_academy
```

```powershell
uv run --locked --extra postgres alembic upgrade head
uv run --locked --extra postgres uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload
```

Alembic crée les tables dans PostgreSQL. Pour retrouver des comptes existants,
il faut aussi importer les lignes de la base SQLite.

## 7. Classement sur le réseau privé

Laisse Uvicorn écouter sur `127.0.0.1:8000` et partage ce port avec
`tailscale serve --bg 8000`. Utilise l’URL HTTPS du réseau privé pour
compiler les deux versions du quiz. [Documentation Tailscale Serve](https://tailscale.com/docs/reference/tailscale-cli/serve)

Dans le jeu, ouvre **Profil**. Les comptes Dev Quiz sont
créés dans la base locale du serveur. L’API conserve un hachage Argon2id du mot
de passe et délivre un jeton de session de 30 minutes par défaut. Les tentatives
de connexion sont limitées par nom de joueur et pour l’ensemble du serveur.
Le serveur ne fait aucun appel à un fournisseur d’identité.

Le jeton de renouvellement est valable 30 jours et conservé sous forme
de hachage dans la base. `POST /auth/refresh` délivre un nouveau JWT
et `POST /auth/logout` révoque la session de cet appareil. L’application
protège le jeton localement avec Android Keystore ou Windows DPAPI.

`GET /users/me/player-profile` et `PUT /users/me/player-profile`
lisent et enregistrent le nom affiché et la photo du compte connecté.
L’API valide et réencode les photos JPEG pour retirer leurs métadonnées.

Pour récupérer les points d’un compte créé avec l’ancienne connexion Google,
lance la commande suivante dans un terminal interactif, depuis `backend/`.
Elle affiche le nom de connexion et te laisse définir le mot de passe
localement. S’il existe plusieurs anciens comptes, ajoute le nom du compte
comme argument.

```powershell
uv run --locked python -m app.activate_password
```

Les deux routes `GET /leaderboard` et `POST /leaderboard/rounds` demandent un
JWT valide. Le classement expose les pseudonymes et les points des 100
premiers comptes. Les identifiants de partie empêchent les doublons lors des
reprises réseau. Le serveur cumule le résultat envoyé par le jeu. Ce classement
convient à un usage entre joueurs de confiance.

La collection utilise le même JWT. `GET /collection` rend les secrets et
le solde du compte. `POST /collection/sync` fusionne les découvertes et
ajoute les points gagnés. `POST /collection/crates` tire la récompense et
débite les points dans une seule transaction. Une même opération rejouée
avec son identifiant ne crédite ni ne débite une deuxième fois.

## 8. Héberger sur Render gratuit avec Neon

Le fichier `render.yaml` configure le service Render gratuit. Il lance les
migrations avant de démarrer l’API. Neon conserve les comptes et les scores
dans PostgreSQL. Render peut mettre l’API en veille après 15 minutes sans
activité. La première connexion peut alors prendre environ une minute.
[Limites de l’offre Render](https://render.com/docs/free)

1. Crée une base Neon gratuite et récupère son URL PostgreSQL. Pour Render,
   utilise le préfixe `postgresql+psycopg://` et conserve les paramètres TLS
   fournis par Neon, par exemple `sslmode=require`.
2. Dans Render, crée un Blueprint à partir de la branche qui contient
   `render.yaml` et renseigne l’adresse de Neon dans `DATABASE_URL`.
   Render génère `JWT_SECRET_KEY`. Les joueurs peuvent créer un compte
   depuis l’application avec un nom de joueur, une adresse e-mail et
   un mot de passe.
3. Vérifie que `/health` sur l’URL Render répond `{"status":"ok"}`. Si
   ton ancien compte a été créé avec Google, active son mot de passe sur
   le PC avec la commande de la section précédente.
4. Si tu migres un classement existant, lance les commandes
   suivantes depuis `backend/` sur le PC. Colle l’URL de Neon lorsque le
   terminal la demande.

   ```powershell
   uv sync --locked --extra postgres
   uv run --locked --extra postgres python -m app.import_leaderboard
   ```

   L’import conserve les identifiants, les hachages et les scores.
   Il refuse de remplacer les comptes déjà présents. Suspends les parties
   sur l’ancien serveur pendant la bascule.
5. Recompile les deux versions avec `-PleaderboardUrl=https://<ton-service>.onrender.com`.
   Installe le nouvel APK et vérifie le même classement sur les deux
   appareils. Fais aussi un essai sur Android depuis un autre réseau
   après arrêt du backend local.
