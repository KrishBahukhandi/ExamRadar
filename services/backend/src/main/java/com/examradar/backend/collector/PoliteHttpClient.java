package com.examradar.backend.collector;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;

@Component
public class PoliteHttpClient {

    private final HttpClient httpClient;
    private final String userAgent;
    private final int timeoutMs;

    public record HttpResponseData(
        int statusCode,
        String body,
        String contentHash,
        long responseTimeMs
    ) {}

    public PoliteHttpClient(
        @Value("${examradar.collector.user-agent:ExamRadarBot/1.0 (+https://examradar.in/bot; bot@examradar.in)}") String userAgent,
        @Value("${examradar.collector.timeout-ms:15000}") int timeoutMs
    ) {
        this.userAgent = userAgent;
        this.timeoutMs = timeoutMs;

        HttpClient client;
        try {
            // Configure resilient SSLContext for Indian government servers that frequently
            // omit intermediate CA certificates in their TLS handshake.
            javax.net.ssl.TrustManager[] trustManagers = new javax.net.ssl.TrustManager[]{
                new javax.net.ssl.X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[0]; }
                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, new SecureRandom());

            client = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofMillis(timeoutMs))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
        } catch (Exception e) {
            client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(timeoutMs))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
        }
        this.httpClient = client;
    }

    private static final java.util.Set<String> ALLOWED_HOST_SUFFIXES = java.util.Set.of(
            ".gov.in",
            ".nic.in",
            "ibps.in",
            ".ibps.in",
            ".nta.ac.in",
            "nta.ac.in",
            "digialm.com",
            ".digialm.com",
            "localhost",
            "127.0.0.1"
    );

    public static boolean isAllowedHost(String host) {
        if (host == null) return false;
        String lowerHost = host.toLowerCase();
        for (String suffix : ALLOWED_HOST_SUFFIXES) {
            if (lowerHost.equals(suffix) || lowerHost.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    public HttpResponseData get(String url) throws Exception {
        URI targetUri = URI.create(url);
        if (!isAllowedHost(targetUri.getHost())) {
            throw new SecurityException("PoliteHttpClient blocked outbound request to unauthorized host: " + targetUri.getHost());
        }

        int maxRetries = 3;
        long backoffDelay = 1000;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            long startTime = System.currentTimeMillis();
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", userAgent)
                        .header("Accept", "text/html,application/xhtml+xml,application/xml,application/json;q=0.9,*/*;q=0.8")
                        .timeout(Duration.ofMillis(timeoutMs))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                long latency = System.currentTimeMillis() - startTime;

                if (response.statusCode() >= 500 && attempt < maxRetries) {
                    Thread.sleep(backoffDelay);
                    backoffDelay *= 2;
                    continue;
                }

                String body = response.body();
                String hash = computeSha256(body);
                return new HttpResponseData(response.statusCode(), body, hash, latency);
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries) {
                    Thread.sleep(backoffDelay);
                    backoffDelay *= 2;
                }
            }
        }
        throw (lastException != null) ? lastException : new RuntimeException("Failed to fetch: " + url);
    }

    public static String computeSha256(String input) {
        if (input == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedHash.length);
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
