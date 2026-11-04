package com.sigebox.core.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Forma nueva del QR, {@code sigebox://join?h=&p=&c=[&f=]} (ADR 0004). La forma anterior está en {@link JoinUriTest}. */
class JoinUriCodeTest {

    @Test
    void generaLaFormaConCodigo() {
        assertEquals("sigebox://join?h=192.168.43.10&p=8765&c=482915",
                new JoinUri("192.168.43.10", 8765, "482915", null).toUri());
    }

    @Test
    void generaLaHuellaDespuesDelCodigo() {
        String f = "a".repeat(64);
        assertEquals("sigebox://join?h=h&p=8765&c=000042&f=" + f, new JoinUri("h", 8765, "000042", f).toUri());
    }

    @Test
    void leeLaFormaConCodigo() {
        JoinUri uri = JoinUri.parse("sigebox://join?h=192.168.43.10&p=8765&c=482915");
        assertEquals(new JoinUri("192.168.43.10", 8765, "482915", null), uri);
        assertEquals("482915", uri.code());
        assertNull(uri.fightId());
        assertNull(uri.token());
        assertEquals(0, uri.seat());
    }

    @Test
    void idaYVueltaConHuella() {
        JoinUri uri = new JoinUri("central.local", 1, "123456", "b".repeat(64));
        assertEquals(uri, JoinUri.parse(uri.toUri()));
    }

    @Test
    void laFormaAnteriorNoTraeCodigo() {
        JoinUri uri = JoinUri.parse("sigebox://join?h=h&p=1&s=pelea&seat=2&t=token");
        assertNull(uri.code());
        assertEquals("pelea", uri.fightId());
        assertEquals(2, uri.seat());
        assertEquals("token", uri.token());
        assertEquals("sigebox://join?h=h&p=1&s=pelea&seat=2&t=token", uri.toUri());
    }

    @Test
    void siLleganLasDosFormasMandaElCodigo() {
        JoinUri uri = JoinUri.parse("sigebox://join?h=h&p=1&s=pelea&seat=2&t=token&c=482915");
        assertEquals(new JoinUri("h", 1, "482915", null), uri);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "sigebox://join?h=h&p=1&c=12345",
        "sigebox://join?h=h&p=1&c=1234567",
        "sigebox://join?h=h&p=1&c=12345a",
        "sigebox://join?h=h&p=1&c=abcdef",
        "sigebox://join?h=h&p=1&c=",
        "sigebox://join?h=h&p=1&c=123456%0A",
        "sigebox://join?h=h&p=1&c=%D9%A1%D9%A2%D9%A3%D9%A4%D9%A5%D9%A6",
        "sigebox://join?p=1&c=123456",
        "sigebox://join?h=h&c=123456",
        "sigebox://join?h=h&p=0&c=123456",
        "sigebox://join?h=h&p=65536&c=123456",
        "sigebox://join?h=h&p=1&c=123456&f=XYZ",
        "sigebox://join?h=h&p=1&s=pelea&seat=2&t=token&c=12345"
    })
    void rechazaUrisConCodigoInvalidas(String uri) {
        assertThrows(ProtocolException.class, () -> JoinUri.parse(uri));
    }

    @Test
    void elCodigoNoSeMezclaConLaFormaAnterior() {
        assertThrows(IllegalArgumentException.class, () -> new JoinUri("h", 1, "s", 0, null, "123456", null));
        assertThrows(IllegalArgumentException.class, () -> new JoinUri("h", 1, null, 1, null, "123456", null));
        assertThrows(IllegalArgumentException.class, () -> new JoinUri("h", 1, null, 0, "t", "123456", null));
    }
}
