package com.examradar.backend.collector;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class PdfTextExtractor {

    private static final int MAX_PAGES = 30;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public String extractTextFromUrl(String pdfUrl) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(pdfUrl))
                .header("User-Agent", "ExamRadarBot/1.0 (+https://examradar.in/bot; bot@examradar.in)")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to download PDF. HTTP status: " + response.statusCode());
        }

        try (InputStream in = response.body();
             PDDocument document = Loader.loadPDF(in.readAllBytes())) {

            if (document.isEncrypted()) {
                throw new RuntimeException("PDF document is encrypted / password protected.");
            }

            int numberOfPages = document.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(Math.min(numberOfPages, MAX_PAGES));

            String text = stripper.getText(document);
            if (text == null || text.trim().isEmpty()) {
                throw new RuntimeException("Extracted text is empty (document may be scanned image).");
            }

            return text;
        }
    }
}
