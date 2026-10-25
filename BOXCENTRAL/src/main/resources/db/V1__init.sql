-- V1: esquema inicial de la central (plan.md §Data, IDEA §6). Solo cambios aditivos en v1.

CREATE TABLE fight (
    id          TEXT PRIMARY KEY,
    red_corner  TEXT NOT NULL,
    blue_corner TEXT NOT NULL,
    rounds      INTEGER NOT NULL CHECK (rounds BETWEEN 1 AND 12),
    judges      INTEGER NOT NULL CHECK (judges BETWEEN 1 AND 5),
    state       TEXT NOT NULL,
    date        TEXT NOT NULL,
    created_ms  INTEGER NOT NULL
);

CREATE TABLE judge_seat (
    fight_id        TEXT NOT NULL REFERENCES fight (id),
    number          INTEGER NOT NULL CHECK (number BETWEEN 1 AND 5),
    name            TEXT,
    device          TEXT,
    credential_hash TEXT,
    connection      TEXT NOT NULL,
    approved_by     TEXT,
    approved_ms     INTEGER,
    PRIMARY KEY (fight_id, number)
);

CREATE TABLE round (
    fight_id  TEXT NOT NULL REFERENCES fight (id),
    number    INTEGER NOT NULL CHECK (number BETWEEN 1 AND 12),
    state     TEXT NOT NULL,
    opened_ms INTEGER,
    closed_ms INTEGER,
    forced    INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (fight_id, number)
);

CREATE TABLE round_card (
    fight_id TEXT NOT NULL REFERENCES fight (id),
    judge    INTEGER NOT NULL,
    round    INTEGER NOT NULL,
    red      INTEGER NOT NULL CHECK (red BETWEEN 0 AND 10),
    blue     INTEGER NOT NULL CHECK (blue BETWEEN 0 AND 10),
    state    TEXT NOT NULL,
    origin   TEXT NOT NULL,
    time_ms  INTEGER NOT NULL,
    PRIMARY KEY (fight_id, judge, round)
);

CREATE TABLE card_version (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    fight_id    TEXT NOT NULL REFERENCES fight (id),
    judge       INTEGER NOT NULL,
    round       INTEGER NOT NULL,
    red_before  INTEGER,
    blue_before INTEGER,
    red_after   INTEGER NOT NULL,
    blue_after  INTEGER NOT NULL,
    who         TEXT NOT NULL,
    reason      TEXT,
    time_ms     INTEGER NOT NULL
);

CREATE TABLE final_card (
    fight_id TEXT NOT NULL REFERENCES fight (id),
    judge    INTEGER NOT NULL,
    hash     TEXT NOT NULL,
    time_ms  INTEGER NOT NULL,
    PRIMARY KEY (fight_id, judge)
);

CREATE TABLE termination (
    fight_id      TEXT PRIMARY KEY REFERENCES fight (id),
    reason        TEXT NOT NULL,
    other_text    TEXT,
    winner_corner TEXT,
    round         INTEGER NOT NULL,
    time          TEXT,
    notes         TEXT
);

CREATE TABLE result (
    fight_id      TEXT PRIMARY KEY REFERENCES fight (id),
    winner        TEXT NOT NULL,
    decision_type TEXT,
    reason        TEXT,
    totals_json   TEXT NOT NULL,
    count_json    TEXT NOT NULL,
    computed_ms   INTEGER NOT NULL
);

CREATE TABLE event_log (
    id       INTEGER PRIMARY KEY AUTOINCREMENT,
    fight_id TEXT,
    type     TEXT NOT NULL,
    detail   TEXT,
    time_ms  INTEGER NOT NULL
);

CREATE INDEX event_log_fight ON event_log (fight_id, id);

-- Idempotencia: cada mensaje WS (por su `id`) se procesa una sola vez.
CREATE TABLE processed_message (
    id           TEXT PRIMARY KEY,
    fight_id     TEXT,
    processed_ms INTEGER NOT NULL
);
