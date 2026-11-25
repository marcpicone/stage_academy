import assert from 'node:assert/strict';
import { once } from 'node:events';
import { mkdtemp, rmdir, unlink } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { test } from 'node:test';
import { createLeaderboardServer } from './leaderboard.mjs';

async function start(database) {
    const server = createLeaderboardServer(database);
    server.listen(0, '127.0.0.1');
    await once(server, 'listening');
    const url = `http://127.0.0.1:${server.address().port}/api/leaderboard/sync`;
    return {
        async sync(...rounds) {
            const response = await fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ rounds }),
            });
            return { status: response.status, body: await response.json() };
        },
        close: () => new Promise((resolve) => server.close(resolve)),
    };
}

test('two devices share totals and retrying a round does not duplicate points', async (context) => {
    const client = await start(':memory:');
    context.after(client.close);
    const first = { id: 'phone-round', player: 'Camille', points: 5 };
    const second = { id: 'desktop-round', player: 'camille', points: 7 };

    assert.equal((await client.sync(first)).status, 200);
    assert.deepEqual((await client.sync(second)).body.players, [{ name: 'Camille', points: 12 }]);
    assert.deepEqual((await client.sync(first, second)).body.players, [{ name: 'Camille', points: 12 }]);
    assert.deepEqual((await client.sync()).body.players, [{ name: 'Camille', points: 12 }]);
});

test('scores survive a restart and invalid batches are rejected in full', async () => {
    const directory = await mkdtemp(join(tmpdir(), 'stage-leaderboard-'));
    const database = join(directory, 'rankings.sqlite');
    let client = await start(database);
    try {
        const valid = { id: 'first', player: 'Camille', points: 4 };
        const invalid = { id: 'invalid', player: 'Camille', points: -1 };
        assert.equal((await client.sync(valid, invalid)).status, 400);
        assert.deepEqual((await client.sync()).body.players, []);
        assert.equal((await client.sync(valid)).status, 200);
        await client.close();
        client = await start(database);
        assert.deepEqual((await client.sync()).body.players, [{ name: 'Camille', points: 4 }]);
    } finally {
        await client.close();
        await unlink(database);
        await rmdir(directory);
    }
});
