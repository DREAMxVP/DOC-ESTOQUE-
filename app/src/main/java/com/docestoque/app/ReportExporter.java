package com.docestoque.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public final class ReportExporter {
    private ReportExporter() {
    }

    public static void showExportDialog(Context context, List<Product> products) {
        String[] formats = {"PDF formatado", "Excel formatado (.xls)"};
        new AlertDialog.Builder(context)
                .setTitle("Exportar estoque")
                .setItems(formats, (dialog, which) -> {
                    try {
                        File file = which == 0 ? createPdfFile(context, products) : createExcelFile(context, products);
                        String mime = which == 0 ? "application/pdf" : "application/vnd.ms-excel";
                        shareFile(context, file, mime, which == 0 ? "Estoque + PDF" : "Estoque + Excel");
                    } catch (IOException exception) {
                        new AlertDialog.Builder(context)
                                .setTitle("Erro na exportação")
                                .setMessage("Não foi possível gerar o arquivo.")
                                .setPositiveButton("Fechar", null)
                                .show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    public static void showHistoryDialog(Context context, DatabaseHelper databaseHelper) {
        String[] periods = {"Últimos 7 dias", "Últimos 30 dias", "Últimos 90 dias", "Todo o histórico"};
        new AlertDialog.Builder(context)
                .setTitle("Histórico de movimentações")
                .setItems(periods, (dialog, which) -> {
                    long since = which == 3 ? 0 : System.currentTimeMillis()
                            - (which == 0 ? 7L : which == 1 ? 30L : 90L) * 24L * 60L * 60L * 1000L;
                    List<Movement> movements = databaseHelper.getMovementsSince(since, 100);
                    StringBuilder text = new StringBuilder();
                    if (movements.isEmpty()) {
                        text.append("Nenhuma movimentação no período selecionado.");
                    } else {
                        for (Movement movement : movements) {
                            text.append("IN".equals(movement.getType()) ? "Entrada +" : "Saída -")
                                    .append(movement.getQuantity()).append(" - ")
                                    .append(movement.getProductName()).append('\n');
                        }
                    }
                    new AlertDialog.Builder(context)
                            .setTitle(periods[which])
                            .setMessage(text.toString())
                            .setPositiveButton("Fechar", null)
                            .show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private static File createExcelFile(Context context, List<Product> products) throws IOException {
        File file = new File(context.getCacheDir(), "estoque_mais.xls");
        StringBuilder html = new StringBuilder();
        html.append("<html><head><meta charset=\"UTF-8\"></head><body>")
                .append("<h2 style=\"color:#1B6B5D;font-family:Arial\">Estoque +</h2>")
                .append("<table border=\"1\" cellspacing=\"0\" cellpadding=\"6\" style=\"border-collapse:collapse;font-family:Arial;font-size:10pt\">")
                .append("<tr style=\"background:#1B6B5D;color:white;font-weight:bold\">")
                .append("<th>NOME</th><th>DESCRIÇÃO</th><th>ESTOQUE</th><th>PREÇO CUSTO</th><th>PREÇO VENDA</th><th>LOCAL</th><th>STATUS</th></tr>");
        boolean alternate = false;
        for (Product product : products) {
            html.append("<tr style=\"background:").append(alternate ? "#F2F7F5" : "#FFFFFF").append("\">")
                    .append("<td>").append(htmlValue(product.getName())).append("</td>")
                    .append("<td>").append(htmlValue(product.getDescription())).append("</td>")
                    .append("<td style=\"text-align:right\">").append(product.getQuantity()).append("</td>")
                    .append("<td style=\"text-align:right\">").append(htmlValue(formatCurrency(product.getCostPrice()))).append("</td>")
                    .append("<td style=\"text-align:right\">").append(htmlValue(formatCurrency(product.getSalePrice()))).append("</td>")
                    .append("<td>").append(htmlValue(product.getLocation())).append("</td>")
                    .append("<td style=\"color:").append(product.isActive() ? "#168A55" : "#C62828")
                    .append("\">").append(product.isActive() ? "ATIVO" : "INATIVO").append("</td></tr>");
            alternate = !alternate;
        }
        html.append("</table></body></html>");
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(html.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        return file;
    }

    private static File createPdfFile(Context context, List<Product> products) throws IOException {
        File file = new File(context.getCacheDir(), "estoque_mais.pdf");
        PdfDocument document = new PdfDocument();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int pageNumber = 1;
        PdfDocument.Page page = document.startPage(new PdfDocument.PageInfo.Builder(842, 595, pageNumber).create());
        Canvas canvas = page.getCanvas();
        int y = drawPdfHeader(canvas, paint);
        for (Product product : products) {
            if (y > 545) {
                document.finishPage(page);
                page = document.startPage(new PdfDocument.PageInfo.Builder(842, 595, ++pageNumber).create());
                canvas = page.getCanvas();
                y = drawPdfHeader(canvas, paint);
            }
            drawPdfRow(canvas, paint, product, y);
            y += 30;
        }
        document.finishPage(page);
        try (FileOutputStream output = new FileOutputStream(file)) {
            document.writeTo(output);
        } finally {
            document.close();
        }
        return file;
    }

    private static int drawPdfHeader(Canvas canvas, Paint paint) {
        paint.setColor(0xFF1B6B5D);
        paint.setTextSize(18);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        canvas.drawText("Estoque +", 24, 32, paint);
        paint.setTextSize(8);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        paint.setColor(0xFFE8F2EF);
        canvas.drawRect(18, 42, 824, 65, paint);
        paint.setColor(0xFF1B6B5D);
        String[] headers = {"NOME", "DESCRIÇÃO", "ESTOQUE", "PREÇO CUSTO", "PREÇO VENDA", "LOCAL", "STATUS"};
        int[] positions = {24, 145, 330, 395, 490, 590, 710};
        for (int index = 0; index < headers.length; index++) {
            canvas.drawText(headers[index], positions[index], 57, paint);
        }
        return 78;
    }

    private static void drawPdfRow(Canvas canvas, Paint paint, Product product, int y) {
        paint.setColor((y / 30) % 2 == 0 ? 0xFFFFFFFF : 0xFFF2F7F5);
        canvas.drawRect(18, y - 14, 824, y + 12, paint);
        paint.setColor(0xFF202020);
        paint.setTextSize(8);
        canvas.drawText(limitPdf(product.getName(), 18), 24, y, paint);
        canvas.drawText(limitPdf(product.getDescription(), 27), 145, y, paint);
        canvas.drawText(String.valueOf(product.getQuantity()), 342, y, paint);
        canvas.drawText(formatCurrency(product.getCostPrice()), 395, y, paint);
        canvas.drawText(formatCurrency(product.getSalePrice()), 490, y, paint);
        canvas.drawText(limitPdf(product.getLocation(), 17), 590, y, paint);
        paint.setColor(product.isActive() ? 0xFF168A55 : 0xFFC62828);
        canvas.drawText(product.isActive() ? "ATIVO" : "INATIVO", 710, y, paint);
    }

    private static void shareFile(Context context, File file, String mimeType, String title) {
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType(mimeType);
        share.putExtra(Intent.EXTRA_SUBJECT, title);
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(share, title));
    }

    private static String htmlValue(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("\n", " ").replace("\r", " ");
    }

    private static String limitPdf(String value, int maxLength) {
        if (value == null) return "-";
        String clean = value.replace('\n', ' ').replace('\r', ' ');
        return clean.length() <= maxLength ? clean : clean.substring(0, maxLength - 1) + "…";
    }

    private static String formatCurrency(double value) {
        return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(value);
    }
}
