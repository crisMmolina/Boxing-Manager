package com.sigebox.juez.join;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class QrParserTest {

    @Test
    public void leeElQrDeSigebox() {
        assertEquals(2, QrParser.parse("sigebox://join?h=192.168.43.10&p=8765&s=F1&seat=2&t=tok123").seat());
    }

    @Test
    public void ignoraQrsAjenosOMalFormados() {
        assertNull(QrParser.parse("https://example.com"));
        assertNull(QrParser.parse("sigebox://join?h=192.168.43.10&p=8765&s=F1&seat=9&t=tok123"));
        assertNull(QrParser.parse(""));
        assertNull(QrParser.parse(null));
    }
}
