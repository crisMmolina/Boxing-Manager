package com.sigebox.central.export;

import com.sigebox.central.ui.Labels;
import com.sigebox.core.model.Pelea;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Rectangle;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
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
        for (var d : sheet.descuentos()) {
            line(out, "descuento", "round " + d.round(), Labels.of(d.esquina()), String.valueOf(d.puntos()));
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

    /** Guinda y azul de las esquinas (DESIGN.md §3.2, tema claro); siempre con su etiqueta escrita. */
    private static final Color RED = new Color(0x9F, 0x22, 0x41);
    private static final Color BLUE = new Color(0x26, 0x6C, 0xB4);
    private static final Color MUTED = new Color(0x5C, 0x5A, 0x55);
    private static final float MARGIN = 36;
    private static final float HEADER_H = 64;
    private static final float FOOTER_H = 92;
    private static final DateTimeFormatter GENERADO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * PDF con membrete SIGEBOX: logo de guantes (vector), pelea y fecha arriba; resultado con las
     * esquinas; la tarjeta de cada juez; y al pie las firmas, la fecha de generación y la página. Todos
     * los números salen de {@link ResultSheet}; la plantilla no calcula nada.
     */
    public byte[] pdf(ResultSheet sheet) {
        var p = sheet.pelea();
        var bytes = new ByteArrayOutputStream();
        var doc = new Document(PageSize.A4.rotate(), MARGIN, MARGIN, MARGIN + HEADER_H, MARGIN + FOOTER_H);
        var writer = PdfWriter.getInstance(doc, bytes);
        writer.setPageEvent(new Membrete(sheet, LocalDateTime.now().format(GENERADO)));
        doc.addTitle("Resultado " + p.esquinaRoja() + " vs " + p.esquinaAzul());
        doc.addCreator("SIGEBOX Central");
        doc.open();
        var big = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22);
        var normal = FontFactory.getFont(FontFactory.HELVETICA, 11);
        var bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);

        doc.add(esquinas(p));
        var headline = para(sheet.headline(), big);
        headline.setAlignment(Element.ALIGN_CENTER);
        headline.setSpacingBefore(10);
        headline.setSpacingAfter(4);
        doc.add(headline);
        var meta = para(p.rounds() + " rounds · " + p.jueces() + " jueces"
                + (sheet.resultado().tipo() == null ? "" : " · " + Labels.of(sheet.resultado().tipo())),
                FontFactory.getFont(FontFactory.HELVETICA, 11, MUTED));
        meta.setAlignment(Element.ALIGN_CENTER);
        meta.setSpacingAfter(8);
        doc.add(meta);
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
        if (!sheet.descuentos().isEmpty()) {
            doc.add(para("Descuentos: " + sheet.descuentos().stream()
                    .map(d -> "round " + d.round() + " " + Labels.of(d.esquina()) + " -" + d.puntos())
                    .collect(java.util.stream.Collectors.joining(", ")), normal));
        }
        var c = sheet.resultado().conteo();
        if (c != null) {
            doc.add(para("Tarjetas: " + c.enviadas() + " enviadas, " + c.tardias() + " tardías, "
                    + c.capturadas() + " capturadas desde papel, " + c.corregidas() + " corregidas", normal));
        }
        doc.close();
        return bytes.toByteArray();
    }

    /** Roja a la izquierda, azul a la derecha: color de esquina y la palabra, para leerse también en B/N. */
    private static PdfPTable esquinas(Pelea p) {
        var t = new PdfPTable(new float[] {5, 1, 5});
        t.setWidthPercentage(100);
        t.addCell(esquina("Roja", p.esquinaRoja(), RED, Element.ALIGN_LEFT));
        var vs = new PdfPCell(new Phrase("vs", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, MUTED)));
        vs.setBorder(Rectangle.NO_BORDER);
        vs.setHorizontalAlignment(Element.ALIGN_CENTER);
        vs.setVerticalAlignment(Element.ALIGN_MIDDLE);
        t.addCell(vs);
        t.addCell(esquina("Azul", p.esquinaAzul(), BLUE, Element.ALIGN_RIGHT));
        return t;
    }

    private static PdfPCell esquina(String etiqueta, String nombre, Color color, int align) {
        var label = new Paragraph("Esquina " + etiqueta,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, color));
        label.setAlignment(align);
        var name = para(nombre, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK));
        name.setAlignment(align);
        var cell = new PdfPCell();
        cell.addElement(label);
        cell.addElement(name);
        cell.setBorder(align == Element.ALIGN_LEFT ? Rectangle.LEFT : Rectangle.RIGHT);
        cell.setBorderWidth(6);
        cell.setBorderColor(color);
        cell.setPaddingLeft(10);
        cell.setPaddingRight(10);
        cell.setPaddingBottom(8);
        return cell;
    }

    /** Membrete y pie en cada página: logo, SIGEBOX, pelea y fecha; firmas, generación y página. */
    private static final class Membrete extends PdfPageEventHelper {
        private final ResultSheet sheet;
        private final String generado;
        private final SvgLogo logo = SvgLogo.load();

        Membrete(ResultSheet sheet, String generado) {
            this.sheet = sheet;
            this.generado = generado;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document doc) {
            var cb = writer.getDirectContent();
            var page = doc.getPageSize();
            float left = MARGIN;
            float right = page.getWidth() - MARGIN;
            float top = page.getHeight() - MARGIN;
            var p = sheet.pelea();

            float logoH = 46;
            logo.draw(cb, left, top - logoH, logoH, false);
            float textX = left + (float) logo.widthFor(logoH) + 10;
            text(cb, "SIGEBOX", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22), textX, top - 24, Element.ALIGN_LEFT);
            text(cb, "Hoja de resultado", FontFactory.getFont(FontFactory.HELVETICA, 10, MUTED), textX, top - 40,
                    Element.ALIGN_LEFT);
            text(cb, "Pelea " + p.id(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16), right, top - 22,
                    Element.ALIGN_RIGHT);
            text(cb, p.fecha(), FontFactory.getFont(FontFactory.HELVETICA, 11), right, top - 40, Element.ALIGN_RIGHT);
            cb.saveState();
            cb.setLineWidth(1.5f);
            cb.moveTo(left, top - HEADER_H + 8);
            cb.lineTo(right, top - HEADER_H + 8);
            cb.stroke();
            cb.restoreState();

            var firmas = new java.util.ArrayList<String>();
            for (var row : sheet.rows()) {
                firmas.add("Juez " + row.seat());
            }
            firmas.add("Supervisor");
            float gap = 18;
            float w = (right - left - gap * (firmas.size() - 1)) / firmas.size();
            float lineY = MARGIN + 44;
            var small = FontFactory.getFont(FontFactory.HELVETICA, 9, MUTED);
            cb.saveState();
            cb.setLineWidth(0.6f);
            for (int i = 0; i < firmas.size(); i++) {
                float x = left + i * (w + gap);
                cb.moveTo(x, lineY);
                cb.lineTo(x + w, lineY);
            }
            cb.stroke();
            cb.restoreState();
            for (int i = 0; i < firmas.size(); i++) {
                float x = left + i * (w + gap);
                text(cb, firmas.get(i), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9), x + w / 2, lineY - 12,
                        Element.ALIGN_CENTER);
            }
            text(cb, "Generado " + generado + " · SIGEBOX Central", small, left, MARGIN, Element.ALIGN_LEFT);
            text(cb, "Página " + writer.getPageNumber(), small, right, MARGIN, Element.ALIGN_RIGHT);
        }

        private static void text(PdfContentByte cb, String s, Font font, float x, float y, int align) {
            ColumnText.showTextAligned(cb, align, new Phrase(pdfText(s), font), x, y, 0);
        }
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
