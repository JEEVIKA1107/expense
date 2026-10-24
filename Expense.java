
package com.example.expense_manager;

public class Expense {

    private int id;
    private String expenseName;
    private double amount;
    private String category;
    private String createdAt;

    public Expense(int id, String expenseName, double amount,
                   String category, String createdAt) {
        this.id = id;
        this.expenseName = expenseName;
        this.amount = amount;
        this.category = category;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public String getExpenseName() {
        return expenseName;
    }

    public double getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
