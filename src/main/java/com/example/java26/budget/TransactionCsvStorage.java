package com.example.java26.budget;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Läser och skriver transaktioner som semikolonseparerad CSV (UTF-8):
 * <pre>
 * datum;kategori;belopp;typ
 * 2026-10-20;Mat;123.50;UTGIFT
 * </pre>
 *
 * <p>Felhantering:
 * <ul>
 *   <li>Saknas filen skapas en ny tom fil.</li>
 *   <li>Trasiga rader hoppas över (loggas som WARNING). Finns trasiga rader sparas en
 *       säkerhetskopia {@code <fil>.bak} så att inget data försvinner vid nästa sparning.</li>
 *   <li>Riktiga I/O-fel (t.ex. ogiltig teckenkodning, ingen skrivrättighet) kastas vidare som
 *       {@link IOException} så att anroparen kan informera användaren.</li>
 * </ul>
 */
public final class TransactionCsvStorage {

    public static final String HEADER = "datum;kategori;belopp;typ";

    private static final Logger LOG = Logger.getLogger(TransactionCsvStorage.class.getName());

    /** Resultatet av en inläsning: giltiga transaktioner och antal överhoppade rader. */
    public record LoadResult(List<Transaction> transactions, int skippedLines) {
    }

    /** Läser in filen. Skapar en ny tom fil om den saknas. */
    public LoadResult load(Path file) throws IOException {
        if (Files.notExists(file)) {
            LOG.info("Filen " + file + " finns inte - skapar en ny tom fil");
            save(file, List.of());
            return new LoadResult(List.of(), 0);
        }

        List<Transaction> transactions = new ArrayList<>();
        int skipped = 0;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1 && line.startsWith("\uFEFF")) {
                    line = line.substring(1); // BOM från t.ex. Notepad/Excel
                }
                if (line.isBlank() || (lineNumber == 1 && line.strip().equalsIgnoreCase(HEADER))) {
                    continue;
                }
                try {
                    transactions.add(parseLine(line, lineNumber));
                } catch (FileFormatException e) {
                    skipped++;
                    LOG.warning("Hoppar över trasig rad - " + e.getMessage());
                }
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Kunde inte läsa " + file, e);
            throw e;
        }

        if (skipped > 0) {
            Path backup = file.resolveSibling(file.getFileName() + ".bak");
            Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
            LOG.warning(skipped + " rader hoppades over - originalfilen kopierad till " + backup);
        }
        LOG.info("Läste " + transactions.size() + " transaktioner från " + file);
        return new LoadResult(List.copyOf(transactions), skipped);
    }

    /**
     * Sparar alla transaktioner. Skriver först till en temporär fil och byter sedan ut
     * originalet, så att en krasch mitt i sparningen inte lämnar en halv fil.
     */
    public void save(Path file, Collection<Transaction> transactions) throws IOException {
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                writer.write(HEADER);
                writer.newLine();
                for (Transaction transaction : transactions) {
                    writer.write(formatLine(transaction));
                    writer.newLine();
                }
            }
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            LOG.info("Sparade " + transactions.size() + " transaktioner till " + file);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Kunde inte spara till " + file, e);
            Files.deleteIfExists(temp);
            throw e;
        }
    }

    /** Tolkar en CSV-rad. @throws FileFormatException om raden är trasig */
    public static Transaction parseLine(String line, int lineNumber) throws FileFormatException {
        String[] parts = line.split(";", -1);
        if (parts.length != 4) {
            throw new FileFormatException(lineNumber, "förväntade 4 fält men hittade " + parts.length + ": '" + line + "'");
        }
        try {
            LocalDate date = LocalDate.parse(parts[0].strip());
            String category = parts[1];
            double amount = Double.parseDouble(parts[2].strip().replace(',', '.'));
            TransactionType type = TransactionType.valueOf(parts[3].strip().toUpperCase(Locale.ROOT));
            return new Transaction(date, category, amount, type);
        } catch (DateTimeParseException e) {
            throw new FileFormatException(lineNumber, "ogiltigt datum '" + parts[0] + "'", e);
        } catch (NumberFormatException e) {
            throw new FileFormatException(lineNumber, "ogiltigt belopp '" + parts[2] + "'", e);
        } catch (IllegalArgumentException e) {
            throw new FileFormatException(lineNumber, "ogiltig typ '" + parts[3] + "'", e);
        } catch (InvalidTransactionException e) {
            throw new FileFormatException(lineNumber, e.getMessage(), e);
        }
    }

    /** Gör om en transaktion till en CSV-rad (belopp alltid med punkt och två decimaler). */
    public static String formatLine(Transaction transaction) {
        return String.format(Locale.ROOT, "%s;%s;%.2f;%s",
                transaction.date(), transaction.category(), transaction.amount(), transaction.type());
    }
}
