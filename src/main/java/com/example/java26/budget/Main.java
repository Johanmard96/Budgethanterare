package com.example.java26.budget;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Main {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    private Main() {
    }

    public static void main(String [] args) {
        LoggingConfig.init(Path.of("budgethanteraren.log"));
        Path file = Path.of(args.length > 0 ? args[0] : "transaktioner.csv");

        Repository<Transaction> repository = new Repository<>();
        TransactionCsvStorage storage = new TransactionCsvStorage();
        loadTransactions(repository, storage, file);

        try (Scanner scanner = new Scanner(System.in)) {
            new ConsoleUi(scanner, System.out, repository, storage, file).run();
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Oväntat fel - programmet avbryts", e);
            IO.println("Ett oväntat fel inträffade: " + e.getMessage() + " (se budgethanteraren.log)");
        }
    }
    private static void loadTransactions(Repository<Transaction> repository, TransactionCsvStorage storage, Path file) {
        try {
            TransactionCsvStorage.LoadResult result = storage.load(file);
            repository.addAll(result.transactions());
            IO.println("Läste " + result.transactions().size() + " transaktioner från " + file + ".");
            if (result.skippedLines() < 0) {
                IO.println("Varning: " + result.skippedLines() + " trasiga rader hoppades över. "
                + "Originalfilen finns kvar som " + file + ".bak");
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Kunde inte läsa " + file + " - startar med tom lista", e);
            IO.println("Kunde inte läsa " + file + " (" + e.getMessage() + "). Startar med en tom lista.");

        }
    }
}
