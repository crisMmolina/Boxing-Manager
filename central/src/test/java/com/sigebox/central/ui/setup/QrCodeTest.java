package com.sigebox.central.ui.setup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.sigebox.central.db.Database;
import com.sigebox.central.server.SeatTokens;
import com.sigebox.central.service.FightService;
import com.sigebox.core.protocol.JoinUri;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** El QR que se muestra lleva el JoinUri con código (ADR 0004) y se lee de vuelta igual. */
class QrCodeTest {

    @TempDir
    Path dir;

    @Test
    void laImagenDelQrSeDecodificaAlMismoUriConCodigo() throws Exception {
        try (var db = Database.open(dir.resolve("sigebox.db"))) {
            var tokens = new SeatTokens();
            var service = new FightService(db, tokens, () -> LocalDate.of(2026, 11, 4));
            var p = service.create("Roja", "Azul", 3, 3);
            var inv = service.invite(p.id(), 2, "192.168.43.10", 8765);

            var image = QrCode.image(inv.qrUri(), QrCode.SIZE);
            var bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
            var text = new QRCodeReader().decode(bitmap).getText();

            assertEquals(inv.qrUri(), text);
            var uri = JoinUri.parse(text);
            assertEquals("192.168.43.10", uri.host());
            assertEquals(8765, uri.port());
            assertEquals(inv.code(), uri.code());
            assertNull(uri.fightId(), "solo el código, sin pelea");
            assertNull(uri.token());
            assertEquals(SeatTokens.Check.OK, tokens.consume(p.id(), 2, uri.code()));
        }
    }
}
