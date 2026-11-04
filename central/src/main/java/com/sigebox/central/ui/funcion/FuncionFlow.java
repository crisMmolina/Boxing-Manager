package com.sigebox.central.ui.funcion;

import com.sigebox.central.service.FuncionService;
import com.sigebox.central.service.FuncionService.Fila;
import com.sigebox.core.model.Pelea;
import java.util.Optional;

/** El evento en curso y su siguiente pelea; lo comparten la pantalla Evento y el paso Resultado. */
public final class FuncionFlow {

    private final FuncionService service;
    private final FuncionQuery query;

    public FuncionFlow(FuncionService service, FuncionQuery query) {
        this.service = service;
        this.query = query;
    }

    public FuncionService service() {
        return service;
    }

    public Optional<String> funcionId() {
        return query.latest();
    }

    /** La siguiente pelea programada de la función en curso, si hay. */
    public Optional<Fila> peek() {
        return funcionId().flatMap(id -> FuncionView.siguiente(service.cartelera(id)));
    }

    /**
     * Inicia la siguiente pelea: crea la pelea, copia los lugares aprobados y avisa a los teléfonos
     * (el aviso lo conecta {@code App} con {@code FuncionService.alSiguiente}).
     */
    public Pelea start() {
        var id = funcionId().orElseThrow(() -> new IllegalStateException("no hay un evento"));
        return service.siguiente(id);
    }
}
