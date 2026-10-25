package com.sigebox.juez.net;

/** Espera exponencial entre reintentos de conexión, con tope. En la LAN conviene un tope corto. */
public final class Backoff {
    private final long initialMs;
    private final long maxMs;
    private long nextMs;

    public Backoff(long initialMs, long maxMs) {
        this.initialMs = initialMs;
        this.maxMs = maxMs;
        this.nextMs = initialMs;
    }

    public synchronized long next() {
        long current = nextMs;
        nextMs = Math.min(maxMs, nextMs * 2);
        return current;
    }

    public synchronized void reset() {
        nextMs = initialMs;
    }
}
