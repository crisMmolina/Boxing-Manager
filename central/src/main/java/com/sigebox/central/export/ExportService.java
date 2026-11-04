package com.sigebox.central.export;

import com.sigebox.central.ui.Labels;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;

/**
 * Exporta el resultado de una pelea a CSV (UTF-8 con BOM, para Excel) y a PDF (OpenPDF). Estados y
 * decisiones salen con {@link Labels}, nunca como código.
 */
public final class ExportService {

    private static final String BOM = "﻿";

    /** CSV: datos de la pelea, una fila por juez con cada round y sus totales. */
    public String csv(ResultSheet sheet) {
        var p = sheet.pelea();
        var out = new StringBuilder(BOM);
        line(out, "pelea", p.id());
        line(out, "fecha", p.fecha());
        line(out, "esquina roja", p.esquinaRoja());
        line(out, "esquina azul", p.esquinaAzul());
        line(out, "resultado", sheet.headline());
        if (sheet.resultado().tipo() != null) {
            line(out, "decision", Labels.of(sheet.resultado().tipo()));
        }
        if (sheet.terminacion() != null) {
            var t = sheet.terminacion();
            line(out, "terminacion", Labels.of(t.motivo()), "round " + t.round(), t.tiempo(),
                    t.textoOtro() == null ? "" : t.textoOtro());
        }
        out.append("\r\n");

        var header = new java.util.ArrayList<String>();
        header.add("juez");
        header.add("nombre");
        for (int r = 1; r <= p.rounds(); r++) {
            header.add("R" + r + " rojo");
            header.add("R" + r + " azul");
        }
        header.add("total rojo");
        header.add("total azul");
        header.add("ganador");
        line(out, header.toArray(String[]::new));
        for (var row : sheet.rows()) {
            var cells = new java.util.ArrayList<String>();
            cells.add(String.valueOf(row.seat()));
            cells.add(row.name() == null ? "" : row.name());
            for (var c : row.cells()) {
                cells.add(c.red() == null ? "" : String.valueOf(c.red()));
                cells.add(c.blue() == null ? "" : String.valueOf(c.blue()));
            }
            cells.add(String.valueOf(row.totalRed()));
            cells.add(String.valueOf(row.totalBlue()));
            cells.add(row.winner());
            line(out, cells.toArray(String[]::new));
        }
        return out.toString();
    }

    public byte[] csvBytes(ResultSheet sheet) {
        return csv(sheet).getBytes(StandardCharsets.UTF_8);
    }

    /** PDF de una página: encabezado, resultado y la tarjeta de cada juez. */
    public byte[] pdf(ResultSheet sheet) {
        var p = sheet.pelea();
        var bytes = new ByteArrayOutputStream();
        var doc = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        PdfWriter.getInstance(doc, bytes);
        doc.addTitle("Resultado " + p.esquinaRoja() + " vs " + p.esquinaAzul());
        doc.addCreator("SIGEBOX Central");
        doc.open();
        var title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        var big = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22);
        var normal = FontFactory.getFont(FontFactory.HELVETICA, 11);
        var bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);

        doc.add(para(p.esquinaRoja() + " (roja) vs " + p.esquinaAzul() + " (azul)", title));
        doc.add(para("Pelea " + p.id() + " · " + p.fecha() + " · " + p.rounds() + " rounds · "
                + p.jueces() + " jueces", normal));
        var headline = para(sheet.headline(), big);
        headline.setSpacingBefore(12);
        headline.setSpacingAfter(12);
        doc.add(headline);
        if (sheet.terminacion() != null) {
            var t = sheet.terminacion();
            doc.add(para("Terminación: " + Labels.of(t.motivo()) + " en el round " + t.round() + " (" + t.tiempo() + ")"
                    + (t.textoOtro() == null ? "" : " · " + t.textoOtro())
                    + (t.observaciones() == null ? "" : " · " + t.observaciones()), normal));
        }

        var table = new PdfPTable(2 + p.rounds() + 2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(12);
        cell(table, "Juez", bold);
        cell(table, "Nombre", bold);
        for (int r = 1; r <= p.rounds(); r++) {
            cell(table, "R" + r, bold);
        }
        cell(table, "Total", bold);
        cell(table, "Ganador", bold);
        for (var row : sheet.rows()) {
            cell(table, String.valueOf(row.seat()), normal);
            cell(table, row.name() == null ? "" : row.name(), normal);
            for (var c : row.cells()) {
                cell(table, c.red() == null ? "—" : c.red() + "–" + c.blue(), normal);
            }
            cell(table, row.totalRed() + "–" + row.totalBlue(), bold);
            cell(table, row.winner(), normal);
        }
        doc.add(table);
        var c = sheet.resultado().conteo();
        if (c != null) {
            doc.add(para("Tarjetas: " + c.enviadas() + " enviadas, " + c.tardias() + " tardías, "
                    + c.capturadas() + " capturadas desde papel, " + c.corregidas() + " corregidas", normal));
        }
        doc.close();
        return bytes.toByteArray();
    }

    private static Paragraph para(String text, Font font) {
        return new Paragraph(pdfText(text), font);
    }

    /**
     * Las fuentes estándar del PDF (WinAnsi) no traen la raya ni el guion largo: se escriben como
     * guion normal para que "39-37" se lea y se pueda copiar.
     */
    static String pdfText(String text) {
        return text.replace('–', '-').replace('—', '-');
    }

    private static void cell(PdfPTable table, String text, Font font) {
        var cell = new PdfPCell(new Phrase(pdfText(text), font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private static void line(StringBuilder out, String... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(escape(values[i]));
        }
        out.append("\r\n");
    }

    /** Comillas si hace falta; un texto que empieza con = + - @ se antepone con ' (inyección de fórmulas). */
    static String escape(String v) {
        if (v == null) {
            return "";
        }
        var s = v;
        if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0 && !s.matches("-?\\d+")) {
            s = "'" + s;
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
