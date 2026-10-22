package com.example.java26.budget;

public enum TransactionType {
    INKOMST("Inkomst"),
    UTGIFT("Utgift");

    private final String label;

    TransactionType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
