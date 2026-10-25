package com.example.java26.budget;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Textmenyn. Pratar bara med användaren - all logik ligger i Repository, BudgetCalculator m.fl. */
public final class ConsoleUi {

    private static final Logger LOG = Logger.getLogger(ConsoleUi.class.getName());

    private final Scanner in;
    private final PrintStream out;
    private final Repository<Transaction> repository;
    private final TransactionCsvStorage storage;
    private final Path file;
    private boolean unsavedChanges;

    public ConsoleUi(Scanner in, PrintStream out, Repository<Transaction> repository,
                     TransactionCsvStorage storage, Path file) {
        this.in = in;
        this.out = out;
        this.repository = repository;
        this.storage = storage;
        this.file = file;
    }

    public void run() {
        LOG.info("Menyn startar med " + repository.size() + " transaktioner");
        boolean running = true;
        while (running) {
            printMenu();
            String choice = prompt("Val: ");
            if (choice == null) {
                LOG.info("Indata stangdes (EOF) - avslutar");
                break;
            }
            LOG.fine(() -> "Menyval: '" + choice + "'");
            switch (choice.strip().toLowerCase(Locale.ROOT)) {
                case "1" -> addTransaction();
                case "2" -> showAll();
                case "3" -> showSummary();
                case "4" -> filterTransactions();
                case "5" -> save();
                case "e" -> running = false;
                default -> {
                    out.println("Ogiltigt val, forsok igen.");
                    LOG.warning("Ogiltigt menyval: '" + choice + "'");
                }
            }
        }
        if (unsavedChanges) {
            out.println("Sparar andringar innan avslut...");
            save();
        }
        out.println("Hej då!");
        LOG.info("Programmet avslutas");
    }

    private void printMenu() {
        out.println();
        out.println("Budgethanteraren");
        out.println("================");
        out.println("1. Lägg till transaktion");
        out.println("2. Visa alla transaktioner");
        out.println("3. Visa saldo och sammanställning per kategori");
        out.println("4. Filtrera transaktioner");
        out.println("5. Spara till fil");
        out.println("e. Avsluta");
    }

    private void addTransaction() {
        try {
            TransactionType type = parseType(orEmpty(prompt("Typ (1 = inkomst, 2 = utgift): ")));
            String dateText = orEmpty(prompt("Datum (yyyy-MM-dd, tomt = idag): ")).strip();
            LocalDate date = dateText.isEmpty() ? LocalDate.now() : parseDate(dateText);
            String category = orEmpty(prompt("Kategori: "));
            double amount = parseAmount(orEmpty(prompt("Belopp: ")));

            Transaction transaction = new Transaction(date, category, amount, type);
            repository.add(transaction);
            unsavedChanges = true;
            out.println("Transaktionen lades till.");
            LOG.info("Transaktion tillagd: " + transaction);
        } catch (InvalidTransactionException e) {
            out.println("Ogiltig inmatning: " + e.getMessage());
            LOG.warning("Ogiltig transaktion nekades: " + e.getMessage());
        }
    }

    private void showAll() {
        printTable(repository.findAll().stream().sorted(Transaction.BY_DATE).toList());
    }

    private void showSummary() {
        List<Transaction> all = repository.findAll();
        if (all.isEmpty()) {
            out.println("Inga transaktioner ännu.");
            return;
        }
        out.printf(Locale.ROOT, "Inkomster totalt:  %12.2f%n", BudgetCalculator.totalIncome(all));
        out.printf(Locale.ROOT, "Utgifter totalt:   %12.2f%n", BudgetCalculator.totalExpenses(all));
        out.printf(Locale.ROOT, "Saldo:             %12.2f%n", BudgetCalculator.balance(all));
        out.println();
        out.println("Netto per kategori:");
        for (Map.Entry<String, Double> entry : BudgetCalculator.sumByCategory(all).entrySet()) {
            out.printf(Locale.ROOT, "  %-20s %12.2f%n", entry.getKey(), entry.getValue());
        }
    }

    private void filterTransactions() {
        out.println("Filtrera pa:");
        out.println("  1. Typ");
        out.println("  2. Datumintervall");
        out.println("  3. Kategori");
        String choice = orEmpty(prompt("Val: ")).strip();
        LOG.fine(() -> "Filterval: '" + choice + "'");
        try {
            Predicate<Transaction> condition;
            switch (choice) {
                case "1" -> condition = TransactionPredicates.ofType(
                        parseType(orEmpty(prompt("Typ (1 = inkomst, 2 = utgift): "))));
                case "2" -> {
                    LocalDate from = parseDate(orEmpty(prompt("Från och med (yyyy-MM-dd): ")).strip());
                    LocalDate to = parseDate(orEmpty(prompt("Till och med (yyyy-MM-dd): ")).strip());
                    condition = TransactionPredicates.betweenDates(from, to);
                }
                case "3" -> condition = TransactionPredicates.inCategory(orEmpty(prompt("Kategori: ")));
                default -> {
                    out.println("Ogiltigt val.");
                    LOG.warning("Ogiltigt filterval: '" + choice + "'");
                    return;
                }
            }
            List<Transaction> result = repository.findWhere(condition).stream().sorted(Transaction.BY_DATE).toList();
            printTable(result);
            if (!result.isEmpty()) {
                out.printf(Locale.ROOT, "Netto för urvalet: %.2f (%d transaktioner)%n",
                        BudgetCalculator.balance(result), result.size());
            }
        } catch (InvalidTransactionException | IllegalArgumentException e) {
            out.println("Ogiltig inmatning: " + e.getMessage());
            LOG.warning("Ogiltigt filter: " + e.getMessage());
        }
    }

    private void save() {
        try {
            storage.save(file, repository.findAll());
            unsavedChanges = false;
            out.println("Sparade " + repository.size() + " transaktioner till " + file + ".");
        } catch (IOException e) {
            out.println("Kunde inte spara till " + file + ": " + e.getMessage());
            LOG.log(Level.SEVERE, "Sparning misslyckades", e);
        }
    }

    private void printTable(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            out.println("Inga transaktioner att visa.");
            return;
        }
        out.printf(Locale.ROOT, "%-12s %-20s %12s  %s%n", "Datum", "Kategori", "Belopp", "Typ");
        out.println("-".repeat(54));
        for (Transaction t : transactions) {
            out.printf(Locale.ROOT, "%-12s %-20s %12.2f  %s%n", t.date(), t.category(), t.amount(), t.type().label());
        }
    }

    // --- Inläsning och tolkning av användarens indata ---

    private String prompt(String text) {
        out.print(text);
        out.flush();
        return in.hasNextLine() ? in.nextLine() : null;
    }

    private static String orEmpty(String text) {
        return text == null ? "" : text;
    }

    private static TransactionType parseType(String text) {
        return switch (text.strip()) {
            case "1" -> TransactionType.INKOMST;
            case "2" -> TransactionType.UTGIFT;
            default -> throw new InvalidTransactionException("Ogiltig typ '" + text.strip() + "' - ange 1 eller 2.");
        };
    }

    private static LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidTransactionException("Ogiltigt datum '" + text + "' - använd formatet yyyy-MM-dd.");
        }
    }

    private static double parseAmount(String text) {
        try {
            return Double.parseDouble(text.strip().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new InvalidTransactionException("Beloppet '" + text.strip() + "' är inte ett tal.");
        }
    }
}
