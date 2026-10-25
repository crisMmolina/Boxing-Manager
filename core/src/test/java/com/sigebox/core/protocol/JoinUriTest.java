package com.sigebox.core.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JoinUriTest {

    @Test
    void leeLaUriDelQr() {
        JoinUri uri = JoinUri.parse("sigebox://join?h=192.168.43.10&p=8765&s=pelea-ref-001&seat=2&t=k3J9xQ");
        assertEquals(new JoinUri("192.168.43.10", 8765, "pelea-ref-001", 2, "k3J9xQ", null), uri);
    }

    @Test
    void llevaLaHuellaDelCertificadoSiLaHay() {
        String f = "a".repeat(64);
        JoinUri uri = new JoinUri("h", 8765, "s", 1, "t", f);
        assertEquals("sigebox://join?h=h&p=8765&s=s&seat=1&t=t&f=" + f, uri.toUri());
        assertEquals(uri, JoinUri.parse(uri.toUri()));
    }

    @Test
    void idaYVueltaConCaracteresEspeciales() {
        JoinUri uri = new JoinUri("central.local", 8765, "pelea 1&2=ñ", 5, "a+b/c?d", null);
        assertEquals("sigebox://join?h=central.local&p=8765&s=pelea+1%262%3D%C3%B1&seat=5&t=a%2Bb%2Fc%3Fd",
                uri.toUri());
        assertEquals(uri, JoinUri.parse(uri.toUri()));
    }

    @Test
    void ignoraParesSinValor() {
        JoinUri uri = JoinUri.parse("sigebox://join?x&h=h&p=1&s=s&seat=1&t=t");
        assertEquals("h", uri.host());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://join?h=h&p=1&s=s&seat=1&t=t",
        "sigebox://leave?h=h&p=1&s=s&seat=1&t=t",
        "sigebox://join",
        "sigebox://join?p=1&s=s&seat=1&t=t",
        "sigebox://join?h=h&s=s&seat=1&t=t",
        "sigebox://join?h=h&p=uno&s=s&seat=1&t=t",
        "sigebox://join?h=h&p=0&s=s&seat=1&t=t",
        "sigebox://join?h=h&p=65536&s=s&seat=1&t=t",
        "sigebox://join?h=h&p=1&s=s&seat=0&t=t",
        "sigebox://join?h=h&p=1&s=s&seat=6&t=t",
        "sigebox://join?h=h&p=1&s=%20&seat=1&t=t",
        "sigebox://join?h=h&p=1&s=s&seat=1",
        "sigebox://join?h=h&p=1&s=s&seat=1&t=t&f=XYZ",
        "no es una uri ::"
    })
    void rechazaUrisInvalidas(String uri) {
        assertThrows(ProtocolException.class, () -> JoinUri.parse(uri));
    }
}
