package com.example.java26.budget;

import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Predicate;
public final class TransactionPredicates {

    private TransactionPredicates() {

    }

    public static Predicate<Transaction> ofType(TransactionType type) {
        Objects.requireNonNull(type, "type får ej vara null");
        return t -> t.type() == type;

    }

    public static Predicate<Transaction> inCategory(String category) {
        Objects.requireNonNull(category, "category får ej vara null");
        String wanted = category.strip();
        return t -> t.category().equalsIgnoreCase(wanted);
    }


    /**
     * Datum i intervallet, båda ändar inkluderade.
     * @throws IllegalArgumentException om {@code from} ligger efter {@code to}

    */
    public static Predicate<Transaction> betweenDates(LocalDate from, LocalDate to) {
        Objects.requireNonNull(from, "from får ej vara null");
        Objects.requireNonNull(to, "to får ej vara null");
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Startdatum (" + from + ") ligger efter slutdatum (" + to + ").");
        }
        return t -> !t.date().isBefore(from) && !t.date().isAfter(to);
    }


}
