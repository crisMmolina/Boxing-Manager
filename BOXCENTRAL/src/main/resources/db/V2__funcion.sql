-- V2: función con cartelera y configuración base (spec 002). Solo expand: no toca las tablas de V1.

CREATE TABLE funcion (
    id           TEXT PRIMARY KEY,
    date         TEXT NOT NULL,
    base_rounds  INTEGER NOT NULL CHECK (base_rounds BETWEEN 1 AND 12),
    base_judges  INTEGER NOT NULL CHECK (base_judges BETWEEN 1 AND 5),
    created_ms   INTEGER NOT NULL
);

-- Una fila por pelea de la cartelera. rounds/judges nulos = usa la base. El estado se deriva:
-- skipped → Saltada; sin fight_id → Programada; si no, el estado de la pelea creada.
CREATE TABLE funcion_card (
    funcion_id   TEXT NOT NULL REFERENCES funcion (id),
    position     INTEGER NOT NULL CHECK (position >= 1),
    red_corner   TEXT NOT NULL,
    blue_corner  TEXT NOT NULL,
    rounds       INTEGER CHECK (rounds IS NULL OR rounds BETWEEN 1 AND 12),
    judges       INTEGER CHECK (judges IS NULL OR judges BETWEEN 1 AND 5),
    skipped      INTEGER NOT NULL DEFAULT 0 CHECK (skipped IN (0, 1)),
    fight_id     TEXT REFERENCES fight (id),
    PRIMARY KEY (funcion_id, position)
);

-- Bitácora de la función (event_log exige una pelea existente).
CREATE TABLE funcion_log (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    funcion_id   TEXT NOT NULL REFERENCES funcion (id),
    type         TEXT NOT NULL,
    detail       TEXT NOT NULL,
    time_ms      INTEGER NOT NULL
);
