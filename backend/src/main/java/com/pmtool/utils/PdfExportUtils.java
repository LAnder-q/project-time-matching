package com.pmtool.utils;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.OutputStream;
import java.util.List;

/**
 * PDF 导出工具类（基于 iText 7，支持中文字体）
 */
public class PdfExportUtils {

    /** 表头背景色 */
    private static final DeviceRgb HEADER_BG = new DeviceRgb(64, 158, 255);
    /** 表头文字颜色 */
    private static final DeviceRgb HEADER_FG = new DeviceRgb(255, 255, 255);

    /**
     * 生成 PDF 报表
     *
     * @param outputStream 输出流
     * @param title        报表标题
     * @param headers      表头列名
     * @param data         数据行（每行为字符串列表）
     */
    public static void writePdf(OutputStream outputStream, String title,
                                 List<String> headers, List<List<String>> data) {
        try {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // 使用亚洲字体支持中文
            PdfFont font = PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H");
            document.setFont(font);

            // 标题
            Paragraph titlePara = new Paragraph(title)
                    .setFont(font)
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(titlePara);

            // 构建表格（等宽列）
            Table table = new Table(headers.size())
                    .setWidth(UnitValue.createPercentValue(100))
                    .setHorizontalAlignment(HorizontalAlignment.CENTER);

            // 表头行
            for (String header : headers) {
                Cell cell = new Cell()
                        .add(new Paragraph(header).setFont(font).setFontSize(10))
                        .setBackgroundColor(HEADER_BG)
                        .setFontColor(HEADER_FG)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setPadding(6);
                table.addHeaderCell(cell);
            }

            // 数据行
            for (List<String> row : data) {
                for (String cellValue : row) {
                    Cell cell = new Cell()
                            .add(new Paragraph(cellValue != null ? cellValue : "").setFont(font).setFontSize(9))
                            .setTextAlignment(TextAlignment.CENTER)
                            .setPadding(5);
                    table.addCell(cell);
                }
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("PDF 导出失败: " + e.getMessage(), e);
        }
    }
}
