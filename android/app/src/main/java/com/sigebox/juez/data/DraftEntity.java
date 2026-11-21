package com.sigebox.juez.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * AND-035 (spec 009): el puntaje a medio poner del round abierto (una sola fila). Se escribe en cada toque de − o +
 * y se borra al enviar; tras un cierre forzado la pantalla vuelve al mismo round con los mismos números. Una esquina
 * sin tocar queda en {@code null} («—»), nunca con un valor por defecto.
 */
@Entity(tableName = "draft")
public class DraftEntity {
    public static final int SINGLE_ROW = 1;

    @PrimaryKey
    public int id = SINGLE_ROW;

    @NonNull
    @ColumnInfo(name = "fight_id")
    public String fightId = "";

    public int seat;

    public int round;

    public Integer red;

    public Integer blue;
}
