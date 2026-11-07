package com.sigebox.central.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Código manual de 6 dígitos junto al token del QR (contrato AND-004). */
class SeatTokensTest {

    private static final String F = "K7Q2MX";

    @Test
    void invitationHasTokenAndSixDigitCode() {
        var t = new SeatTokens();
        var inv = t.issueInvitation(F, 1);
        assertTrue(inv.token().length() >= 22, inv.token());
        assertTrue(inv.code().matches("\\d{" + SeatTokens.CODE_LENGTH + "}"), inv.code());
        assertNotEquals(inv.token(), inv.code());
    }

    @Test
    void codeJoinsAndThenBothAreUsed() {
        var t = new SeatTokens();
        var inv = t.issueInvitation(F, 2);
        assertEquals(SeatTokens.Check.OK, t.consume(F, 2, inv.code()));
        assertEquals(SeatTokens.Check.USED, t.consume(F, 2, inv.code()));
        assertEquals(SeatTokens.Check.USED, t.consume(F, 2, inv.token()));
    }

    @Test
    void codeOfAnotherSeatDoesNotWork() {
        var t = new SeatTokens();
        var one = t.issueInvitation(F, 1);
        t.issueInvitation(F, 2);
        assertEquals(SeatTokens.Check.INVALID, t.consume(F, 2, one.code()));
        assertEquals(SeatTokens.Check.OK, t.consume(F, 1, one.code()));
    }

    @Test
    void failedCodesCountPerSeatAndDiscardTheInvitation() {
        var t = new SeatTokens();
        var inv = t.issueInvitation(F, 3);
        for (int i = 0; i < SeatTokens.MAX_FAILURES; i++) {
            assertEquals(SeatTokens.Check.INVALID, t.consume(F, 3, "00000" + i));
        }
        assertEquals(SeatTokens.Check.INVALID, t.consume(F, 3, inv.code())); // descartada
        assertEquals(SeatTokens.Check.INVALID, t.consume(F, 3, inv.token()));
    }

    /** DSK-040: al bajar los jueces, el lugar que sobra pierde su código; los demás lugares siguen igual. */
    @Test
    void forgetOneSeatKillsOnlyThatSeat() {
        var t = new SeatTokens();
        var kept = t.issueInvitation(F, 2);
        var gone = t.issueInvitation(F, 3);
        t.forget(F, 3);
        assertTrue(t.lookup(gone.code()).isEmpty());
        assertEquals(SeatTokens.Check.INVALID, t.consume(F, 3, gone.token()));
        assertEquals(SeatTokens.Check.INVALID, t.consume(F, 3, gone.code()));
        assertEquals(new SeatTokens.Seat(F, 2), t.lookup(kept.code()).orElseThrow());
        assertEquals(SeatTokens.Check.OK, t.consume(F, 2, kept.code()));
    }

    @Test
    void issueStillReturnsOnlyTheToken() {
        var t = new SeatTokens();
        var token = t.issue(F, 4);
        assertTrue(token.length() >= 22);
        assertEquals(SeatTokens.Check.OK, t.consume(F, 4, token));
    }
}
