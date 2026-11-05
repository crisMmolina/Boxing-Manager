package com.sigebox.central.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.DescuentoRepository;
import com.sigebox.central.service.ResultService;
import com.sigebox.central.service.ResultServiceTest;
import com.sigebox.core.model.Esquina;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.MotivoTerminacion;
import com.sigebox.core.model.Terminacion;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

class ExportServiceTest {

    @TempDir
    Path dir;

    private Database db;
    private ResultSheet sheet;
    private final ExportService export = new ExportService();

    @BeforeEach
    void setUp() throws Exception {
        db = Database.open(dir.resolve("sigebox.db"));
        var ref = ResultServiceTest.Reference.load();
        ref.into(db, EstadoPelea.TERMINADA);
        var service = new ResultService(db, new ResultService.Broadcaster() {
            @Override
            public void fightEnded(String fightId, MotivoTerminacion reason) {
            }

            @Override
            public void fightClosed(String fightId) {
            }
        });
        sheet = service.sheet(ref.pelea().id());
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void csvHasTheReferenceTotals() {
        var csv = export.csv(sheet);
        assertTrue(csv.startsWith("﻿"), "BOM para Excel");
        var lines = csv.substring(1).split("\r\n");
        assertTrue(csv.contains("resultado,Gana Roja por decisión dividida 2–1"), csv);
        assertTrue(csv.contains("juez,nombre,R1 rojo,R1 azul,"), csv);
        assertTrue(csv.contains("1,Ana Torres,") && csv.contains(",39,37,Roja\r\n"), csv);
        assertTrue(csv.contains("2,María López,") && csv.contains(",37,39,Azul\r\n"), csv);
        assertTrue(csv.contains("3,Luis Ramírez,"), csv);
        long totals = java.util.Arrays.stream(lines).filter(l -> l.matches("\\d,.*")).count();
        assertEquals(3, totals);
    }

    @Test
    void pdfContainsHeadlineAndTotals() throws Exception {
        byte[] pdf = export.pdf(sheet);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
        var reader = new PdfReader(pdf);
        var text = new PdfTextExtractor(reader).getTextFromPage(1);
        reader.close();
        assertTrue(text.contains("Gana Roja por decisión dividida 2-1"), text);
        assertTrue(text.contains("39-37"), text);
        assertTrue(text.contains("37-39"), text);
        assertTrue(text.contains("María López"), text);
    }

    @Test
    void csvEscapesQuotesCommasAndFormulas() {
        assertEquals("\"a,b\"", ExportService.escape("a,b"));
        assertEquals("\"di \"\"hola\"\"\"", ExportService.escape("di \"hola\""));
        assertEquals("'=1+1", ExportService.escape("=1+1"));
        assertEquals("-5", ExportService.escape("-5"));
        assertEquals("", ExportService.escape(null));
    }

    @Test
    void csvYPdfMuestranDecisionYTerminacionEnEspanol() throws Exception {
        var csv = export.csv(sheet);
        assertTrue(csv.contains("decision,Dividida\r\n"), csv);
        assertFalse(csv.contains("DIVIDIDA"), csv);

        var terminated = new ResultSheet(sheet.pelea(), sheet.resultado(), sheet.headline(), sheet.rows(),
                new Terminacion(sheet.pelea().id(), MotivoTerminacion.KOT, null, Esquina.ROJA, 3, "1:42", null));
        csv = export.csv(terminated);
        assertTrue(csv.contains("terminacion,KO técnico,round 3,1:42,"), csv);
        assertFalse(csv.contains("KOT"), csv);

        var reader = new PdfReader(export.pdf(terminated));
        var text = new PdfTextExtractor(reader).getTextFromPage(1);
        reader.close();
        assertTrue(text.contains("Terminación: KO técnico en el round 3"), text);
        assertFalse(text.contains("KOT"), text);
    }

    private ResultSheet discounted() {
        var fight = sheet.pelea().id();
        new DescuentoRepository(db).save(fight, 2, Esquina.ROJA, 1L);
        return new ResultService(db, new ResultService.Broadcaster() {
            @Override
            public void fightEnded(String fightId, MotivoTerminacion reason) {
            }

            @Override
            public void fightClosed(String fightId) {
            }
        }).sheet(fight);
    }

    @Test
    void csvKeepsWinnerAndDecisionAndAddsTheDiscounts() {
        var csv = export.csv(discounted());
        assertTrue(csv.contains("descuento,round 2,Roja,1\r\n"), csv);
        assertTrue(csv.contains("resultado,Gana "), csv);
        assertTrue(csv.contains("decision,"), csv);
        assertTrue(csv.contains(",38,37,Roja\r\n"), csv);
        assertFalse(export.csv(sheet).contains("descuento"));
    }

    @Test
    void pdfKeepsWinnerAndAddsTheDiscounts() throws Exception {
        var withDiscount = discounted();
        var reader = new PdfReader(export.pdf(withDiscount));
        var text = new PdfTextExtractor(reader).getTextFromPage(1);
        reader.close();
        assertTrue(text.contains("Descuentos: round 2 Roja -1"), text);
        assertTrue(text.contains(ExportService.pdfText(withDiscount.headline())), text);
        assertTrue(text.contains("Ganador"), text);
        assertTrue(text.contains("38-37"), text);
    }
}
