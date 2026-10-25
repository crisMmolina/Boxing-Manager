package com.sigebox.juez.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** El lugar de juez de este teléfono (una sola fila): sirve para reconectar con {@code HELLO} tras cerrar la app. */
@Entity(tableName = "local_state")
public class SessionEntity {
    public static final int SINGLE_ROW = 1;

    @PrimaryKey
    public int id = SINGLE_ROW;

    @NonNull
    public String host = "";

    public int port;

    @NonNull
    @ColumnInfo(name = "fight_id")
    public String fightId = "";

    public int seat;

    @NonNull
    public String token = "";

    @NonNull
    @ColumnInfo(name = "judge_name")
    public String judgeName = "";

    /** SHA-256 del certificado de la central si el QR trae {@code f} ({@code wss://}); si no, {@code null}. */
    @ColumnInfo(name = "cert_fingerprint")
    public String certFingerprint;

    public boolean approved;
}
