package com.sigebox.central.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.service.ResultService;
import com.sigebox.central.service.ResultServiceTest;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.MotivoTerminacion;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

/** DSK-054: los encabezados de PDF, CSV y Excel dicen «Box Central». */
class BoxCentralExportTest {

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
    void elPdfLlevaBoxCentralEnMembreteYPie() throws Exception {
        var reader = new PdfReader(export.pdf(sheet));
        var text = new PdfTextExtractor(reader).getTextFromPage(1);
        reader.close();
        assertTrue(text.lines().anyMatch(l -> l.strip().equals("Box Central")), "membrete: " + text);
        assertTrue(text.contains("· Box Central"), "pie: " + text);
    }

    @Test
    void elCsvEmpiezaConElEventoBoxCentral() {
        var csv = export.csv(sheet);
        var first = csv.substring(1).split("\r\n")[0];
        assertEquals("evento,Box Central", first);
    }

    @Test
    void elExcelDiceBoxCentralEnElEvento() throws Exception {
        var rows = new ArrayList<List<String>>();
        try (var wb = new ReadableWorkbook(new ByteArrayInputStream(export.xlsx(sheet)));
                var stream = wb.findSheet("Resumen").orElseThrow().openStream()) {
            stream.forEach(r -> {
                var cells = new ArrayList<String>();
                for (int i = 0; i < r.getCellCount(); i++) {
                    cells.add(r.getCellText(i));
                }
                rows.add(cells);
            });
        }
        var evento = rows.stream().filter(r -> r.size() > 1 && "Evento".equals(r.get(0))).findFirst().orElseThrow();
        assertEquals("Box Central", evento.get(1));
    }
}
