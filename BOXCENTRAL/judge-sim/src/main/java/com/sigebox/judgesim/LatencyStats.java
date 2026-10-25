package com.sigebox.judgesim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Latencias de envío → {@code ACK} en milisegundos. */
public final class LatencyStats {

    private final List<Long> samples = new ArrayList<>();

    public synchronized void add(long millis) {
        samples.add(millis);
    }

    public synchronized int count() {
        return samples.size();
    }

    /** Percentil por rango más cercano; -1 sin muestras. */
    public synchronized long percentile(int p) {
        if (samples.isEmpty()) {
            return -1;
        }
        var sorted = new ArrayList<>(samples);
        Collections.sort(sorted);
        int rank = (int) Math.ceil(p / 100.0 * sorted.size());
        return sorted.get(Math.max(0, rank - 1));
    }

    public synchronized String report() {
        if (samples.isEmpty()) {
            return "latencia: sin ACK recibidos";
        }
        return "latencia envío→ACK: n=" + samples.size() + " p50=" + percentile(50) + " ms p95=" + percentile(95)
                + " ms máx=" + Collections.max(samples) + " ms";
    }
}
