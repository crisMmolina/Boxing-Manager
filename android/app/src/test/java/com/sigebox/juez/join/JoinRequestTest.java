package com.sigebox.juez.join;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.sigebox.core.protocol.JoinUri;

import org.junit.Test;

public class JoinRequestTest {

    private static final JoinUri QR =
            JoinUri.parse("sigebox://join?h=192.168.43.10&p=8765&s=F1&seat=2&t=abcdefghijklmnopqrstuv");

    @Test
    public void nombreVacioSeRechaza() {
        JoinRequest.Result result = JoinRequest.fromQr(QR, "   ");
        assertEquals(JoinRequest.Error.NAME_EMPTY, result.error);
        assertNull(result.request);
    }

    @Test
    public void sinQrNoHaySolicitud() {
        assertEquals(JoinRequest.Error.NO_SEAT, JoinRequest.fromQr(null, "María López").error);
    }

    @Test
    public void nombreSeLimpiaYSeUsaElLugarDelQr() {
        JoinRequest.Result result = JoinRequest.fromQr(QR, "  María   López ");
        assertNull(result.error);
        assertEquals("María López", result.request.judgeName);
        assertEquals(QR, result.request.uri);
    }

    @Test
    public void nombreDeMasDe80CaracteresSeRechaza() {
        String largo = "M".repeat(81);
        assertEquals(JoinRequest.Error.NAME_TOO_LONG, JoinRequest.fromQr(QR, largo).error);
    }

    @Test
    public void codigoManualDeSeisDigitos() {
        JoinRequest.Result result = JoinRequest.manual("192.168.43.10", "F1", "2", "482913", "Ana Ruiz");
        assertNull(result.error);
        JoinUri uri = result.request.uri;
        assertEquals("192.168.43.10", uri.host());
        assertEquals(JoinRequest.DEFAULT_PORT, uri.port());
        assertEquals("F1", uri.fightId());
        assertEquals(2, uri.seat());
        assertEquals("482913", uri.token());
    }

    @Test
    public void codigoManualAceptaEspaciosYPuertoExplicito() {
        JoinRequest.Result result = JoinRequest.manual(" 10.0.0.5:9000 ", " F1 ", "3", "482 913", "Ana");
        assertNull(result.error);
        assertEquals(9000, result.request.uri.port());
        assertEquals("482913", result.request.uri.token());
    }

    @Test
    public void codigoManualInvalido() {
        assertEquals(JoinRequest.Error.CODE_INVALID,
                JoinRequest.manual("10.0.0.5", "F1", "2", "48291", "Ana").error);
        assertEquals(JoinRequest.Error.CODE_INVALID,
                JoinRequest.manual("10.0.0.5", "F1", "2", "48a913", "Ana").error);
        assertEquals(JoinRequest.Error.HOST_INVALID,
                JoinRequest.manual("", "F1", "2", "482913", "Ana").error);
        assertEquals(JoinRequest.Error.HOST_INVALID,
                JoinRequest.manual("10.0.0.5:99999", "F1", "2", "482913", "Ana").error);
        assertEquals(JoinRequest.Error.SEAT_INVALID,
                JoinRequest.manual("10.0.0.5", "F1", "6", "482913", "Ana").error);
        assertEquals(JoinRequest.Error.FIGHT_INVALID,
                JoinRequest.manual("10.0.0.5", " ", "2", "482913", "Ana").error);
        assertEquals(JoinRequest.Error.NAME_EMPTY,
                JoinRequest.manual("10.0.0.5", "F1", "2", "482913", "").error);
    }
}
