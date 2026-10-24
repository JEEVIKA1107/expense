
package com.example.expense_manager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AIService {

    private final String apiKey;

    public AIService(
            @Value("${OPENAI_API_KEY:}") String apiKey) {
        this.apiKey = apiKey;

        if (apiKey.isBlank()) {
            System.out.println("OpenAI API key is NOT configured.");
        } else {
            System.out.println("OpenAI API key is configured.");
        }
    }

    public String testConnection() throws Exception {

        if (apiKey.isBlank()) {
            throw new IllegalStateException(
                    "OPENAI_API_KEY is not configured.");
        }

        String requestBody = """
                {
                    "model": "gpt-4.1-mini",
                    "input": "Reply with exactly: Hello from OpenAI!"
                }
                """;

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "https://api.openai.com/v1/responses"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "OpenAI request failed. HTTP status: "
                            + response.statusCode()
                            + ". Details: " + response.body());
        }

        return response.body();
    }
}
