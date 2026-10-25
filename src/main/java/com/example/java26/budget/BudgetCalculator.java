package com.example.java26.budget;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Sammanställningslogiken: saldo, summor och summa per kategori. Alla metoder är
 * statiska och utan sidoeffekter, vilket gör dem enkla att enhetstesta.
 * Resultaten avrundas till två decimaler så att flyttalsfel (0.1 + 0.2) inte syns.
 */
public final class BudgetCalculator {

    private static final Logger LOG = Logger.getLogger(BudgetCalculator.class.getName());

    private BudgetCalculator() {
    }

    public static double totalIncome(Collection<Transaction> transactions) {
        return sumOfType(transactions, TransactionType.INKOMST);
    }

    public static double totalExpenses(Collection<Transaction> transactions) {
        return sumOfType(transactions, TransactionType.UTGIFT);
    }

    /** Inkomster minus utgifter. Negativt om man spenderat mer än man tjänat. */
    public static double balance(Collection<Transaction> transactions) {
        Objects.requireNonNull(transactions, "transactions far inte vara null");
        double balance = Money.round2(transactions.stream().mapToDouble(Transaction::signedAmount).sum());
        LOG.fine(() -> "Saldo for " + transactions.size() + " transaktioner: " + balance);
        return balance;
    }

    /**
     * Nettosumma (inkomster minus utgifter) per kategori, sorterad på kategorinamn.
     * Kategorier som skiljer sig åt i versaler/gemener ("Mat" och "mat") slås ihop.
     */
    public static Map<String, Double> sumByCategory(Collection<Transaction> transactions) {
        Objects.requireNonNull(transactions, "transactions far inte vara null");
        TreeMap<String, Double> result = transactions.stream().collect(Collectors.groupingBy(
                Transaction::category,
                () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER),
                Collectors.summingDouble(Transaction::signedAmount)));
        result.replaceAll((category, sum) -> Money.round2(sum));
        LOG.fine(() -> "Summerade " + transactions.size() + " transaktioner i " + result.size() + " kategorier");
        return Collections.unmodifiableMap(result);
    }

    private static double sumOfType(Collection<Transaction> transactions, TransactionType type) {
        Objects.requireNonNull(transactions, "transactions far inte vara null");
        return Money.round2(transactions.stream()
                .filter(TransactionPredicates.ofType(type))
                .mapToDouble(Transaction::amount)
                .sum());
    }
}
