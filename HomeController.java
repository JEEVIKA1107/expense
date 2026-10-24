
package com.example.expense_manager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Authentication authentication, Model model) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login";
        }

        loadExpenses(authentication.getName(), model);
        return "home";
    }

    @PostMapping("/add-expense")
    public String addExpense(
            @RequestParam String expenseName,
            @RequestParam double amount,
            @RequestParam String category,
            Authentication authentication,
            Model model) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login";
        }

        if (expenseName == null || expenseName.isBlank()
                || category == null || category.isBlank()
                || !Double.isFinite(amount) || amount <= 0) {

            model.addAttribute("message",
                    "Please enter a valid expense name, category, and amount.");
            loadExpenses(authentication.getName(), model);
            return "home";
        }

        String sql = """
            INSERT INTO expenses
                (expense_name, amount, category, user_id)
            SELECT ?, ?, ?, id
            FROM users
            WHERE email = ?
            """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, expenseName.trim());
            statement.setDouble(2, amount);
            statement.setString(3, category.trim());
            statement.setString(4, authentication.getName());

            int rowsInserted = statement.executeUpdate();

            if (rowsInserted != 1) {
                model.addAttribute("message",
                        "Could not identify your account. Please log in again.");
                loadExpenses(authentication.getName(), model);
                return "home";
            }

        } catch (SQLException e) {
            e.printStackTrace();
            model.addAttribute("message",
                    "Could not save expense. Check the terminal.");
            loadExpenses(authentication.getName(), model);
            return "home";
        }

        return "redirect:/";
    }

    private void loadExpenses(String email, Model model) {
        List<Expense> expenses = new ArrayList<>();

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
                    Expense expense = new Expense(
                            resultSet.getInt("id"),
                            resultSet.getString("expense_name"),
                            resultSet.getDouble("amount"),
                            resultSet.getString("category"),
                            resultSet.getString("created_at")
                    );

                    expenses.add(expense);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            model.addAttribute("message",
                    "Could not load expenses. Check the terminal.");
        }

        double totalSpending = 0;
        Map<String, Double> categoryTotals = new HashMap<>();

        for (Expense expense : expenses) {
            totalSpending += expense.getAmount();

            String category = expense.getCategory();
            double expenseAmount = expense.getAmount();

            categoryTotals.put(
                    category,
                    categoryTotals.getOrDefault(category, 0.0)
                            + expenseAmount
            );
        }

        model.addAttribute("expenses", expenses);
        model.addAttribute("totalSpending", totalSpending);
        model.addAttribute("categoryTotals", categoryTotals);
    }
}
