package com.sigebox.juez.net;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BackoffTest {

    @Test
    public void duplicaHastaElTopeYSeReinicia() {
        Backoff backoff = new Backoff(500, 5_000);
        assertEquals(500, backoff.next());
        assertEquals(1_000, backoff.next());
        assertEquals(2_000, backoff.next());
        assertEquals(4_000, backoff.next());
        assertEquals(5_000, backoff.next());
        assertEquals(5_000, backoff.next());
        backoff.reset();
        assertEquals(500, backoff.next());
    }
}
