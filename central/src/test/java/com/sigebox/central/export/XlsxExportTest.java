package com.sigebox.central.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigebox.central.db.Database;
import com.sigebox.central.db.DescuentoRepository;
import com.sigebox.central.service.ResultService;
import com.sigebox.central.service.ResultServiceTest;
import com.sigebox.core.model.Esquina;
import com.sigebox.core.model.EstadoPelea;
import com.sigebox.core.model.MotivoTerminacion;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** DSK-052: el .xlsx de resultados (hojas Resumen y Tarjetas) sale de {@link ResultSheet}. */
class XlsxExportTest {

    @TempDir
    Path dir;

    private Database db;
    private ResultSheet sheet;
    private final ExportService export = new ExportService();

    private ResultService service() {
        return new ResultService(db, new ResultService.Broadcaster() {
            @Override
            public void fightEnded(String fightId, MotivoTerminacion reason) {
            }

            @Override
            public void fightClosed(String fightId) {
            }
        });
    }

    @BeforeEach
    void setUp() throws Exception {
        db = Database.open(dir.resolve("sigebox.db"));
        var ref = ResultServiceTest.Reference.load();
        ref.into(db, EstadoPelea.TERMINADA);
        sheet = service().sheet(ref.pelea().id());
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    /** Filas de una hoja como texto, columna por columna. */
    private static List<List<String>> rows(byte[] xlsx, String name) throws Exception {
        try (var wb = new ReadableWorkbook(new ByteArrayInputStream(xlsx))) {
            var found = wb.findSheet(name);
            assertTrue(found.isPresent(), "falta la hoja " + name);
            var out = new ArrayList<List<String>>();
            try (var stream = found.get().openStream()) {
                stream.forEach(r -> out.add(text(r)));
            }
            return out;
        }
    }

    private static List<String> text(Row r) {
        var cells = new ArrayList<String>();
        for (int i = 0; i < r.getCellCount(); i++) {
            cells.add(r.getCellText(i));
        }
        return cells;
    }

    /**
     * Las partes XML del zip, para revisar el formato que el lector no expone. Se lee por el directorio
     * central ({@link ZipFile}), como Excel: FastExcel escribe cabeceras locales ZIP64 que
     * {@code ZipInputStream} no sabe leer en streaming.
     */
    private Map<String, String> parts(byte[] xlsx) throws Exception {
        var file = Files.write(dir.resolve("partes.xlsx"), xlsx);
        var out = new HashMap<String, String>();
        try (var zip = new ZipFile(file.toFile())) {
            for (var e : java.util.Collections.list(zip.entries())) {
                try (var in = zip.getInputStream(e)) {
                    out.put(e.getName(), new String(in.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
        }
        return out;
    }

    private static String value(List<List<String>> rows, String label) {
        for (var r : rows) {
            if (!r.isEmpty() && r.get(0).equals(label)) {
                return r.size() > 1 ? r.get(1) : "";
            }
        }
        throw new AssertionError("falta la fila " + label + " en " + rows);
    }

    @Test
    void xlsxTraeResumenYTarjetas() throws Exception {
        byte[] xlsx = export.xlsx(sheet);

        var resumen = rows(xlsx, "Resumen");
        assertEquals("Box Central", value(resumen, "Evento"));
        assertEquals(sheet.pelea().id(), value(resumen, "Pelea"));
        assertEquals("Roja", value(resumen, "Ganador"));
        assertEquals("Dividida", value(resumen, "Método"));
        assertEquals("Gana Roja por decisión dividida 2–1", value(resumen, "Resultado"));

        var tarjetas = rows(xlsx, "Tarjetas");
        var header = tarjetas.get(0);
        assertEquals("Juez", header.get(0));
        assertEquals("R1 Roja", header.get(2));
        assertEquals("R1 Azul", header.get(3));
        assertTrue(header.contains("Total Roja") && header.contains("Total Azul") && header.contains("Ganador"),
                header.toString());
        assertEquals(1 + sheet.rows().size(), tarjetas.size(), "una fila por juez");
        assertEquals("María López", tarjetas.get(2).get(1));
    }

    @Test
    void xlsxTieneFormatoDePlantilla() throws Exception {
        var xml = parts(export.xlsx(sheet));
        var tarjetas = xml.get("xl/worksheets/sheet2.xml");
        assertTrue(tarjetas != null, xml.keySet().toString());
        assertTrue(tarjetas.contains("ySplit=\"1\"") && tarjetas.contains("state=\"frozen\""), "títulos congelados");
        assertTrue(tarjetas.contains("customWidth=\"true\"") || tarjetas.contains("customWidth=\"1\""),
                "anchos fijos");
        var styles = xml.get("xl/styles.xml");
        assertTrue(styles.contains("FF9F2241"), "esquina roja");
        assertTrue(styles.contains("FF266CB4"), "esquina azul");
        assertTrue(styles.contains("<b/>") || styles.contains("<b val=\"true\"/>"), "encabezados en negrita");
    }

    @Test
    void xlsxUsaLosNumerosDeResultSheet() throws Exception {
        new DescuentoRepository(db).save(sheet.pelea().id(), 2, Esquina.ROJA, 1L);
        var withDiscount = service().sheet(sheet.pelea().id());

        for (var s : List.of(sheet, withDiscount)) {
            var tarjetas = rows(export.xlsx(s), "Tarjetas");
            var header = tarjetas.get(0);
            int totalRed = header.indexOf("Total Roja");
            int totalBlue = header.indexOf("Total Azul");
            int winner = header.indexOf("Ganador");
            for (int i = 0; i < s.rows().size(); i++) {
                var expected = s.rows().get(i);
                var actual = tarjetas.get(i + 1);
                assertEquals(String.valueOf(expected.seat()), actual.get(0));
                for (var c : expected.cells()) {
                    int col = 2 + (c.round() - 1) * 2;
                    assertEquals(c.red() == null ? "" : String.valueOf(c.red()), actual.get(col));
                    assertEquals(c.blue() == null ? "" : String.valueOf(c.blue()), actual.get(col + 1));
                }
                assertEquals(String.valueOf(expected.totalRed()), actual.get(totalRed));
                assertEquals(String.valueOf(expected.totalBlue()), actual.get(totalBlue));
                assertEquals(expected.winner(), actual.get(winner));
            }
        }
        var discounted = rows(export.xlsx(withDiscount), "Tarjetas");
        assertEquals("38", discounted.get(1).get(discounted.get(0).indexOf("Total Roja")), "con el descuento");
    }
}
