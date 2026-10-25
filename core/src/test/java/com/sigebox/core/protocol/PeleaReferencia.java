package com.sigebox.core.protocol;

import com.google.gson.Gson;
import com.sigebox.core.model.LugarJuez;
import com.sigebox.core.model.Pelea;
import com.sigebox.core.model.Resultado;
import com.sigebox.core.model.Round;
import com.sigebox.core.model.TarjetaRound;
import java.nio.file.Path;
import java.util.List;

/** {@code demo_data/pelea_referencia.json} leída con los records del modelo. Las pruebas de scoring la reusan. */
public record PeleaReferencia(
        boolean demo_data,
        String descripcion,
        Pelea pelea,
        List<LugarJuez> lugares,
        List<Round> rounds,
        List<TarjetaRound> tarjetas,
        Resultado esperado) {

    public static final Path ARCHIVO = Path.of("..", "demo_data", "pelea_referencia.json");

    public static PeleaReferencia cargar() {
        return new Gson().fromJson(ProtocolSchemaTest.read(ARCHIVO), PeleaReferencia.class);
    }
}
