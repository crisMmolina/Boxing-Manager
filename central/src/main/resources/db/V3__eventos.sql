-- V3 (plan 004): solo agrega. El nombre del evento y los descuentos de puntos del réferi.

-- NULL = se muestra "Evento <fecha>".
ALTER TABLE funcion ADD COLUMN nombre TEXT;

CREATE TABLE descuento (
    id               INTEGER PRIMARY KEY,
    fight_id         TEXT    NOT NULL,
    round            INTEGER NOT NULL CHECK (round BETWEEN 1 AND 12),
    esquina          TEXT    NOT NULL CHECK (esquina IN ('ROJA', 'AZUL')),
    puntos           INTEGER NOT NULL DEFAULT 1 CHECK (puntos = 1),
    anulado          INTEGER NOT NULL DEFAULT 0,
    motivo_anulacion TEXT,
    created_ms       INTEGER NOT NULL,
    anulado_ms       INTEGER
);

CREATE INDEX idx_descuento_fight ON descuento (fight_id);
