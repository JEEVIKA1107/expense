
package com.example.expense_manager;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AIController {

    private final GeminiService geminiService;

    public AIController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @GetMapping("/ai-advice")
    public String showAdvicePage() {
        return "ai-advice";
    }

    @PostMapping("/ai-advice")
    public String getAdvice(
            @RequestParam String prompt,
            Authentication authentication,
            Model model) {

        model.addAttribute("prompt", prompt);

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login";
        }

        if (prompt == null || prompt.isBlank()) {
            model.addAttribute("aiError", "Please enter a question.");
            return "ai-advice";
        }

        try {
            String expenseData = loadExpenseData(authentication.getName());

            String fullPrompt = """
                    You are a helpful personal finance assistant.
                    Analyze only the saved expense records provided below.
                    Answer the user's question using these records.
                    Give practical and realistic money-saving suggestions.
                    Do not invent transactions or amounts.
                    All amounts are in the currency used by the expense tracker.
                    If no expenses exist, explain that there are no saved expenses.

                    SAVED EXPENSE RECORDS:
                    %s

                    USER'S QUESTION:
                    %s
                    """.formatted(expenseData, prompt);

            String advice = geminiService.generateAdvice(fullPrompt);

            if (advice == null || advice.isBlank()) {
                model.addAttribute(
                        "aiError",
                        "Gemini returned an empty response. Please try again."
                );
            } else {
                model.addAttribute("advice", advice);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();

            model.addAttribute(
                    "aiError",
                    "The Gemini request was interrupted. Please try again."
            );

        } catch (IOException e) {
            e.printStackTrace();

            String details = e.getMessage();

            if (details == null || details.isBlank()) {
                details = e.getClass().getSimpleName()
                        + ". Check the terminal for details.";
            }

            model.addAttribute(
                    "aiError",
                    "Gemini request failed: " + details
            );

        } catch (SQLException e) {
            e.printStackTrace();

            model.addAttribute(
                    "aiError",
                    "Could not read saved expenses from SQLite. "
                            + "Check the terminal for details."
            );
        }

        return "ai-advice";
    }

    private String loadExpenseData(String email) throws SQLException {
        List<String> records = new ArrayList<>();

        String sql = """
                SELECT id, expense_name, amount, category, created_at
                FROM expenses
                WHERE user_id = (
                    SELECT id FROM users WHERE email = ?
                )
                ORDER BY id DESC
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String date = resultSet.getString("created_at");
                    String name = resultSet.getString("expense_name");
                    double amount = resultSet.getDouble("amount");
                    String category = resultSet.getString("category");

                    String record = String.format(
                            "Date: %s | Name: %s | Amount: %.2f | Category: %s",
                            date == null ? "Unknown" : date,
                            name == null ? "Unknown" : name,
                            amount,
                            category == null ? "Uncategorized" : category
                    );

                    records.add(record);
                }
            }
        }

        if (records.isEmpty()) {
            return "No saved expenses found for this account.";
        }

        return String.join("\n", records);
    }
}
