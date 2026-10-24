
package com.example.expense_manager;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GeminiService {

    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent";

    private static final int MAX_ATTEMPTS = 1;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        this.objectMapper = new ObjectMapper();
    }

    public String generateAdvice(String prompt)
            throws IOException, InterruptedException {

        String apiKey = System.getenv("GEMINI_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IOException(
                    "GEMINI_API_KEY environment variable is not configured.");
        }

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                ),
                "generationConfig", Map.of(
                        "maxOutputTokens", 500,
                        "temperature", 0.4
                )
        );

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

                int status = response.statusCode();

                if (status == 200) {
                    return extractResponseText(response.body());
                }

                String errorBody = response.body();

                if (errorBody == null || errorBody.isBlank()) {
                    errorBody = "No error details returned.";
                }

                if (errorBody.length() > 1000) {
                    errorBody = errorBody.substring(0, 1000);
                }

                boolean retryable =
                        status == 429
                        || status == 500
                        || status == 502
                        || status == 503
                        || status == 504;

                if (!retryable || attempt == MAX_ATTEMPTS) {
                    throw new IOException(
                            "Gemini API returned HTTP " + status
                                    + ": " + errorBody);
                }

                System.err.println(
                        "Gemini returned HTTP " + status
                                + ". Retrying attempt "
                                + (attempt + 1) + " of "
                                + MAX_ATTEMPTS + ".");

            } catch (HttpTimeoutException e) {
                if (attempt == MAX_ATTEMPTS) {
                    throw new IOException(
                            "Gemini request timed out after "
                                    + MAX_ATTEMPTS
                                    + " attempts. Please try again later.",
                            e);
                }

                System.err.println(
                        "Gemini request timed out. Retrying attempt "
                                + (attempt + 1) + " of "
                                + MAX_ATTEMPTS + ".");

            } catch (ConnectException e) {
                if (attempt == MAX_ATTEMPTS) {
                    throw new IOException(
                            "Could not connect to Gemini after "
                                    + MAX_ATTEMPTS
                                    + " attempts. Check your internet connection.",
                            e);
                }

                System.err.println(
                        "Connection failed. Retrying attempt "
                                + (attempt + 1) + " of "
                                + MAX_ATTEMPTS + ".");

            } catch (IOException e) {
                // Preserve API errors and other unexpected I/O failures.
                throw e;
            }

            if (attempt < MAX_ATTEMPTS) {
                // Wait 2 seconds before attempt 2, and 4 before attempt 3.
                Thread.sleep(2000L * attempt);
            }
        }

        throw new IOException(
                "Gemini could not respond after several attempts.");
    }

    private String extractResponseText(String responseBody)
            throws IOException {

        JsonNode root = objectMapper.readTree(responseBody);

        JsonNode textNode = root.path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text");

        if (textNode.isMissingNode()
                || textNode.isNull()
                || textNode.asText().isBlank()) {
            throw new IOException(
                    "Gemini returned no text. The response may have been blocked.");
        }

        return textNode.asText();
    }
}
