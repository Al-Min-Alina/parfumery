package parfumery;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class WordExporter {

    public static void exportSaleToWord(SaleFX sale, String exportedBy, File file) throws IOException {
        String xml = buildSaleXml(sale, exportedBy);
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
            pw.print(xml);
        }
    }

    public static void exportSupplyToWord(SupplyFX supply, String exportedBy, File file) throws IOException {
        String xml = buildSupplyXml(supply, exportedBy);
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
            pw.print(xml);
        }
    }

    private static String buildSaleXml(SaleFX sale, String exportedBy) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        String dateTime = LocalDateTime.now().format(dtf);

        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<?mso-application progid=\"Word.Document\"?>\n" +
                "<w:wordDocument xmlns:w=\"http://schemas.microsoft.com/office/word/2003/wordml\">\n" +
                "  <w:body>\n" +
                "    <w:p>\n" +
                "      <w:pPr><w:jc w:val=\"center\"/><w:rPr><w:b/><w:sz w:val=\"32\"/></w:rPr></w:pPr>\n" +
                "      <w:r><w:rPr><w:b/><w:sz w:val=\"32\"/></w:rPr><w:t>Документ о продаже №" + sale.getId() + "</w:t></w:r>\n" +
                "    </w:p>\n" +
                "    <w:p><w:r><w:t> </w:t></w:r></w:p>\n" +
                "    <w:p>\n" +
                "      <w:r><w:rPr><w:b/><w:sz w:val=\"24\"/></w:rPr><w:t>Парфюмерный магазин</w:t></w:r>\n" +
                "    </w:p>\n" +
                "    <w:p><w:r><w:t> </w:t></w:r></w:p>\n" +
                "    <w:tbl>\n" +
                "      <w:tblPr><w:tblBorders><w:top w:val=\"single\" w:sz=\"4\"/><w:left w:val=\"single\" w:sz=\"4\"/><w:bottom w:val=\"single\" w:sz=\"4\"/><w:right w:val=\"single\" w:sz=\"4\"/></w:tblBorders></w:tblPr>\n" +
                "      " + tblRow("Номер продажи", String.valueOf(sale.getId()), true) +
                "      " + tblRow("Дата продажи", sale.getDate(), false) +
                "      " + tblRow("Клиент", sale.getClient().isEmpty() ? "Без клиента" : sale.getClient(), false) +
                "      " + tblRow("Продавец", sale.getSeller(), false) +
                "      " + tblRow("Товары", sale.getItems(), false) +
                "      " + tblRow("Общая сумма", String.format("%.2f руб.", sale.getTotalAmount()).replace(".", ","), true) +
                "    </w:tbl>\n" +
                "    <w:p><w:r><w:t> </w:t></w:r></w:p>\n" +
                "    <w:p>\n" +
        "      <w:r><w:rPr><w:i/><w:sz w:val=\"20\"/></w:rPr><w:t>Документ создан: " + dateTime + "</w:t></w:r>\n" +
                "    </w:p>\n" +
                "    <w:p>\n" +
                "      <w:r><w:rPr><w:i/><w:sz w:val=\"20\"/></w:rPr><w:t>Экспортировал: " + escapeXml(exportedBy) + "</w:t></w:r>\n" +
                "    </w:p>\n" +
                "    <w:p>\n" +
                "      <w:r><w:rPr><w:i/><w:sz w:val=\"20\"/></w:rPr><w:t>Подпись: ___________________</w:t></w:r>\n" +
                "    </w:p>\n" +
                "  </w:body>\n" +
                "</w:wordDocument>";
    }

    private static String buildSupplyXml(SupplyFX supply, String exportedBy) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        String dateTime = LocalDateTime.now().format(dtf);

        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<?mso-application progid=\"Word.Document\"?>\n" +
                "<w:wordDocument xmlns:w=\"http://schemas.microsoft.com/office/word/2003/wordml\">\n" +
                "  <w:body>\n" +
                "    <w:p>\n" +
                "      <w:pPr><w:jc w:val=\"center\"/><w:rPr><w:b/><w:sz w:val=\"32\"/></w:rPr></w:pPr>\n" +
                "      <w:r><w:rPr><w:b/><w:sz w:val=\"32\"/></w:rPr><w:t>Документ о поставке №" + supply.getId() + "</w:t></w:r>\n" +
                "    </w:p>\n" +
                "    <w:p><w:r><w:t> </w:t></w:r></w:p>\n" +
                "    <w:p>\n" +
                "      <w:r><w:rPr><w:b/><w:sz w:val=\"24\"/></w:rPr><w:t>Парфюмерный магазин</w:t></w:r>\n" +
                "    </w:p>\n" +
                "    <w:p><w:r><w:t> </w:t></w:r></w:p>\n" +
                "    <w:tbl>\n" +
                "      <w:tblPr><w:tblBorders><w:top w:val=\"single\" w:sz=\"4\"/><w:left w:val=\"single\" w:sz=\"4\"/><w:bottom w:val=\"single\" w:sz=\"4\"/><w:right w:val=\"single\" w:sz=\"4\"/></w:tblBorders></w:tblPr>\n" +
                "      " + tblRow("Номер поставки", String.valueOf(supply.getId()), true) +
                "      " + tblRow("Дата поставки", supply.getDate(), false) +
                "      " + tblRow("Поставщик", supply.getSupplier(), false) +
                "      " + tblRow("Принял", supply.getReceiver(), false) +
                "      " + tblRow("Товары", supply.getItems(), false) +
                "      " + tblRow("Общая сумма", String.format("%.2f руб.", supply.getTotalAmount()).replace(".", ","), true) +
                "    </w:tbl>\n" +
                "    <w:p><w:r><w:t> </w:t></w:r></w:p>\n" +
                "    <w:p>\n" +
                "      <w:r><w:rPr><w:i/><w:sz w:val=\"20\"/></w:rPr><w:t>Документ создан: " + dateTime + "</w:t></w:r>\n" +
                "    </w:p>\n" +
                "    <w:p>\n" +
                "      <w:r><w:rPr><w:i/><w:sz w:val=\"20\"/></w:rPr><w:t>Экспортировал: " + escapeXml(exportedBy) + "</w:t></w:r>\n" +
                "    </w:p>\n" +
                "    <w:p>\n" +
                "      <w:r><w:rPr><w:i/><w:sz w:val=\"20\"/></w:rPr><w:t>Подпись: ___________________</w:t></w:r>\n" +
                "    </w:p>\n" +
                "  </w:body>\n" +
                "</w:wordDocument>";
    }

    private static String tblRow(String label, String value, boolean isHeader) {
        String bgColor = isHeader ? "<w:shd w:val=\"clear\" w:color=\"auto\" w:fill=\"8B6C5B\"/>" : "";
        String fontColor = isHeader ? "<w:color w:val=\"FFFFFF\"/>" : "";
        String bold = isHeader ? "<w:b/>" : "";

        return "      <w:tr>\n" +
                "        <w:tc>\n" +
                "          <w:tcPr><w:tcW w:w=\"2500\" w:type=\"dxa\"/>" + bgColor + "</w:tcPr>\n" +
                "          <w:p><w:r><w:rPr>" + bold + fontColor + "<w:sz w:val=\"22\"/></w:rPr><w:t>" + escapeXml(label) + "</w:t></w:r></w:p>\n" +
                "        </w:tc>\n" +
                "        <w:tc>\n" +
                "          <w:tcPr><w:tcW w:w=\"5500\" w:type=\"dxa\"/></w:tcPr>\n" +
                "          <w:p><w:r><w:rPr><w:sz w:val=\"22\"/></w:rPr><w:t>" + escapeXml(value) + "</w:t></w:r></w:p>\n" +
                "        </w:tc>\n" +
                "      </w:tr>\n";
    }

    private static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}