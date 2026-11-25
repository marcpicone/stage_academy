import { createServer } from 'node:http';
import { mkdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { DatabaseSync } from 'node:sqlite';
import { fileURLToPath, pathToFileURL } from 'node:url';

const maxRounds = 100;
const maxBodyBytes = 1024 * 1024;
const maxRoundPoints = 1000;

class RequestError extends Error {
    constructor(status, message) {
        super(message);
        this.status = status;
    }
}

function respond(response, status, body) {
    response.writeHead(status, {
        'Content-Type': 'application/json; charset=utf-8',
        'Cache-Control': 'no-store',
    });
    response.end(JSON.stringify(body));
}

async function readRounds(request) {
    const chunks = [];
    let received = 0;
    for await (const chunk of request.iterator({ destroyOnReturn: false })) {
        received += chunk.length;
        if (received > maxBodyBytes) {
            request.resume();
            throw new RequestError(413, 'Request too large');
        }
        chunks.push(chunk);
    }
    let input;
    try {
        input = JSON.parse(Buffer.concat(chunks).toString('utf8'));
    } catch {
        throw new RequestError(400, 'Invalid JSON');
    }
    if (!Array.isArray(input?.rounds) || input.rounds.length > maxRounds) {
        throw new RequestError(400, 'Invalid rounds');
    }
    return input.rounds.map((round) => {
        const player = typeof round?.player === 'string' ? round.player.trim() : '';
        if (typeof round?.id !== 'string' || !/^[a-zA-Z0-9_-]{1,80}$/.test(round.id) ||
            player.length === 0 || player.length > 1024 ||
            !Number.isInteger(round?.points) || round.points < 0 || round.points > maxRoundPoints) {
            throw new RequestError(400, 'Invalid round');
        }
        return { id: round.id, player, points: round.points };
    });
}

export function createLeaderboardServer(databaseFile) {
    if (databaseFile !== ':memory:') mkdirSync(dirname(databaseFile), { recursive: true });
    const db = new DatabaseSync(databaseFile);
    db.exec(`
        PRAGMA busy_timeout = 3000;
        PRAGMA journal_mode = WAL;
        CREATE TABLE IF NOT EXISTS rounds (
            id TEXT PRIMARY KEY,
            player TEXT NOT NULL,
            player_key TEXT NOT NULL,
            points INTEGER NOT NULL CHECK (points BETWEEN 0 AND ${maxRoundPoints})
        ) STRICT
    `);
    const insert = db.prepare(`
        INSERT INTO rounds (id, player, player_key, points) VALUES (?, ?, ?, ?)
        ON CONFLICT (id) DO NOTHING
    `);
    const standings = db.prepare(`
        SELECT MIN(player) AS name, SUM(points) AS points
        FROM rounds
        GROUP BY player_key
        ORDER BY points DESC, player_key
    `);
    const server = createServer({ requestTimeout: 10_000 }, async (request, response) => {
        if (request.method === 'GET' && request.url === '/health') {
            respond(response, 200, { status: 'ok' });
            return;
        }
        if (request.method !== 'POST' || request.url !== '/api/leaderboard/sync') {
            respond(response, 404, { error: 'Route not found' });
            return;
        }
        try {
            const rounds = await readRounds(request);
            db.exec('BEGIN IMMEDIATE');
            try {
                for (const { id, player, points } of rounds) {
                    insert.run(id, player, player.toLowerCase(), points);
                }
                db.exec('COMMIT');
            } catch (error) {
                db.exec('ROLLBACK');
                throw error;
            }
            respond(response, 200, { players: standings.all() });
        } catch (error) {
            if (error instanceof RequestError) {
                respond(response, error.status, { error: error.message });
            } else {
                console.error('Leaderboard request failed:', error.message);
                respond(response, 500, { error: 'Leaderboard unavailable' });
            }
        }
    });
    server.on('close', () => db.close());
    return server;
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
    const databaseFile = process.env.LEADERBOARD_DATABASE ??
        fileURLToPath(new URL('./data/leaderboard.sqlite', import.meta.url));
    const host = process.env.LEADERBOARD_HOST ?? '127.0.0.1';
    const port = Number(process.env.PORT ?? 8080);
    const server = createLeaderboardServer(databaseFile);
    server.listen(port, host, () => console.log(`Leaderboard listening on ${host}:${server.address().port}`));
}
