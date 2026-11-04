package com.sigebox.juez.join;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.sigebox.core.protocol.JoinUri;

import org.junit.Test;

/** El QR nuevo con código ({@code c}) y el viejo ({@code s}/{@code seat}/{@code t}) (ADR 0004). */
public class QrParserCodeTest {

    @Test
    public void leeElQrNuevoConCodigo() {
        JoinUri uri = QrParser.parse("sigebox://join?h=192.168.43.10&p=8765&c=482913");
        assertEquals("192.168.43.10", uri.host());
        assertEquals(8765, uri.port());
        assertEquals("482913", uri.code());
        assertNull(uri.fightId());
    }

    @Test
    public void sigueLeyendoElQrViejo() {
        JoinUri uri = QrParser.parse("sigebox://join?h=192.168.43.10&p=8765&s=F1&seat=2&t=tok123");
        assertEquals("F1", uri.fightId());
        assertEquals(2, uri.seat());
        assertEquals("tok123", uri.token());
        assertNull(uri.code());
    }

    @Test
    public void rechazaCodigosMalFormados() {
        assertNull(QrParser.parse("sigebox://join?h=192.168.43.10&p=8765&c=48291"));
        assertNull(QrParser.parse("sigebox://join?h=192.168.43.10&p=8765&c=abcdef"));
        assertNull(QrParser.parse("https://example.com"));
    }
}
