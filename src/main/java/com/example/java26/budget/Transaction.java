package com.example.java26.budget;

import java.time.LocalDate;
import java.util.Comparator;

public record Transaction(LocalDate date, String category, double amount, TransactionType type) {

    //Maxbelopp på en miljard
    public static final double MAX_AMOUNT = 1_000_000_000.0;

    public static final Comparator<Transaction> BY_DATE =
            Comparator.comparing(Transaction::date)
                    .thenComparing(Transaction::category, String.CASE_INSENSITIVE_ORDER);
    public Transaction {
        if (date == null) {
            throw new InvalidTransactionException("Datum saknas.");
        }
        if (type == null) {
            throw new InvalidTransactionException("Typ saknas.");
        }
        if (category == null || category.isBlank()) {
            throw new InvalidTransactionException("Kategori får ej vara tom.");
        }
        category = category.strip();
        if (category.contains(";") || category.contains("\n") || category.contains("\r")) {
            throw new InvalidTransactionException("Kategorin får je innehålla semikolon eller radbrytning.");
        }
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            throw new InvalidTransactionException("Beloppet måste vara ett ändligt tal.");
        }
        double rounded = Money.round2(amount);
        if (rounded <= 0) {
            throw new InvalidTransactionException("Beloppet måste vara minst 0.01 (angavs: " + amount
                    + "). Utgifter anges som ett positivt belopp med typen UTGIFT.");
        }
        if (rounded > MAX_AMOUNT) {
            throw new InvalidTransactionException("Beloppet får ej överstiga " + (long) MAX_AMOUNT + ".");
        }
        amount = rounded;

    }
    public double signedAmount() {
        return type == TransactionType.INKOMST ? amount : -amount;
    }



}
