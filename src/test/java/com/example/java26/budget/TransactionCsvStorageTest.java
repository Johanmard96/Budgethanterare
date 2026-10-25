package com.example.java26.budget;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionCsvStorageTest {

    private static final String HEADER = TransactionCsvStorage.HEADER;

    private TransactionCsvStorage storage;

    @BeforeEach
    void setUp() {
        storage = new TransactionCsvStorage();
    }

    // --- formatLine / parseLine ---

    @Test
    @DisplayName("formatLine() skriver belopp med punkt och två decimaler")
    void formatLine_usesDotAndTwoDecimals() {
        // Arrange
        Transaction t = new Transaction(LocalDate.of(2026, 1, 15), "Mat", 12.5, TransactionType.UTGIFT);

        // Act
        String line = TransactionCsvStorage.formatLine(t);

        // Assert
        assertEquals("2026-01-15;Mat;12.50;UTGIFT", line);
    }

    @Test
    @DisplayName("parseLine() tolkar en giltig rad")
    void parseLine_validLine() throws FileFormatException {
        // Arrange + Act
        Transaction t = TransactionCsvStorage.parseLine("2026-01-15;Mat;12.50;UTGIFT", 2);

        // Assert
        assertEquals(new Transaction(LocalDate.of(2026, 1, 15), "Mat", 12.5, TransactionType.UTGIFT), t);
    }

    @Test
    @DisplayName("parseLine() accepterar decimalkomma och små bokstäver i typen")
    void parseLine_acceptsDecimalCommaAndLowercaseType() throws FileFormatException {
        Transaction t = TransactionCsvStorage.parseLine("2026-01-15;Lon;1234,50;inkomst", 1);

        assertEquals(1234.5, t.amount(), 0.0001);
        assertEquals(TransactionType.INKOMST, t.type());
    }

    @Test
    @DisplayName("parseLine() kastar FileFormatException med radnummer för trasiga rader")
    void parseLine_brokenLines_throwFileFormatException() {
        List<String> broken = List.of(
                "bara-en-kolumn",
                "2026-01-15;Mat;12.50",                 // för få fält
                "2026-01-15;Mat;12.50;UTGIFT;extra",    // för många fält
                "inte-ett-datum;Mat;12.50;UTGIFT",
                "2026-13-45;Mat;12.50;UTGIFT",          // omöjligt datum
                "2026-01-15;Mat;abc;UTGIFT",
                "2026-01-15;Mat;12.50;SPARANDE",        // okänd typ
                "2026-01-15;Mat;-5.00;UTGIFT",          // negativt belopp
                "2026-01-15;;12.50;UTGIFT");            // tom kategori

        for (String line : broken) {
            FileFormatException e = assertThrows(FileFormatException.class,
                    () -> TransactionCsvStorage.parseLine(line, 7), "Borde vara trasig: " + line);
            assertEquals(7, e.getLineNumber());
        }
    }

    // --- save / load ---

    @Test
    @DisplayName("Spara och läsa in ger tillbaka exakt samma transaktioner")
    void saveThenLoad_roundTrip(@TempDir Path dir) throws IOException {
        // Arrange
        Path file = dir.resolve("data.csv");
        List<Transaction> original = List.of(
                new Transaction(LocalDate.of(2026, 1, 1), "Lon", 25000, TransactionType.INKOMST),
                new Transaction(LocalDate.of(2026, 1, 2), "Mat & dryck", 1234.56, TransactionType.UTGIFT),
                new Transaction(LocalDate.of(2026, 1, 3), "Ränta", 0.01, TransactionType.INKOMST));

        // Act
        storage.save(file, original);
        TransactionCsvStorage.LoadResult result = storage.load(file);

        // Assert
        assertEquals(original, result.transactions());
        assertEquals(0, result.skippedLines());
    }

    @Test
    @DisplayName("Sparar en tom lista: filen får bara rubrikraden och läses in som tom")
    void saveEmpty_thenLoad_givesEmptyList(@TempDir Path dir) throws IOException {
        // Arrange
        Path file = dir.resolve("data.csv");

        // Act
        storage.save(file, List.of());
        TransactionCsvStorage.LoadResult result = storage.load(file);

        // Assert
        assertEquals(List.of(HEADER), Files.readAllLines(file));
        assertTrue(result.transactions().isEmpty());
    }

    @Test
    @DisplayName("Saknas filen skapas en ny tom fil och programmet kraschar inte")
    void load_missingFile_createsEmptyFile(@TempDir Path dir) throws IOException {
        // Arrange
        Path file = dir.resolve("finns-inte.csv");

        // Act
        TransactionCsvStorage.LoadResult result = storage.load(file);

        // Assert
        assertTrue(result.transactions().isEmpty());
        assertEquals(0, result.skippedLines());
        assertTrue(Files.exists(file));
    }

    @Test
    @DisplayName("En helt tom fil läses in som tom lista")
    void load_emptyFile_givesEmptyList(@TempDir Path dir) throws IOException {
        // Arrange
        Path file = dir.resolve("tom.csv");
        Files.writeString(file, "");

        // Act
        TransactionCsvStorage.LoadResult result = storage.load(file);

        // Assert
        assertTrue(result.transactions().isEmpty());
        assertEquals(0, result.skippedLines());
    }

    @Test
    @DisplayName("Trasiga rader hoppas över, de hela rader behålls och en .bak-fil skapas")
    void load_skipsBrokenLines_keepsGoodOnes_andCreatesBackup(@TempDir Path dir) throws IOException {
        // Arrange
        Path file = dir.resolve("data.csv");
        String content = String.join("\n",
                HEADER,
                "2026-01-01;Lon;100.00;INKOMST",
                "trasig rad",
                "",
                "2026-01-02;Mat;abc;UTGIFT",
                "2026-01-03;Mat;50.00;UTGIFT") + "\n";
        Files.writeString(file, content);

        // Act
        TransactionCsvStorage.LoadResult result = storage.load(file);

        // Assert
        assertEquals(2, result.transactions().size());
        assertEquals(2, result.skippedLines());
        assertEquals(content, Files.readString(dir.resolve("data.csv.bak")));
    }

    @Test
    @DisplayName("Fil utan rubrikrad och med BOM läses in korrekt")
    void load_handlesMissingHeaderAndBom(@TempDir Path dir) throws IOException {
        // Arrange
        Path file = dir.resolve("data.csv");
        Files.writeString(file, "\uFEFF2026-01-01;Lon;100.00;INKOMST\n2026-01-02;Mat;5.00;UTGIFT\n");

        // Act
        TransactionCsvStorage.LoadResult result = storage.load(file);

        // Assert
        assertEquals(2, result.transactions().size());
        assertEquals(0, result.skippedLines());
    }

    @Test
    @DisplayName("Ogiltig teckenkodning ger IOException (som programmet fångar och loggar)")
    void load_invalidEncoding_throwsIOException(@TempDir Path dir) throws IOException {
        // Arrange
        Path file = dir.resolve("data.csv");
        Files.write(file, new byte[]{(byte) 0xC3, (byte) 0x28, '\n'});

        // Act + Assert
        assertThrows(IOException.class, () -> storage.load(file));
    }

    @Test
    @DisplayName("save() skapar saknade mappar och ersätter gammalt innehåll")
    void save_createsParentDirectories_andOverwrites(@TempDir Path dir) throws IOException {
        // Arrange
        Path file = dir.resolve("under/mapp/data.csv");
        Transaction first = new Transaction(LocalDate.of(2026, 1, 1), "Lon", 100, TransactionType.INKOMST);
        Transaction second = new Transaction(LocalDate.of(2026, 1, 2), "Mat", 20, TransactionType.UTGIFT);

        // Act
        storage.save(file, List.of(first));
        storage.save(file, List.of(second));

        // Assert
        assertEquals(List.of(second), storage.load(file).transactions());
    }

    @Test
    @DisplayName("Efter lyckad sparning finns ingen kvarglömd .tmp-fil")
    void save_leavesNoTempFileBehind(@TempDir Path dir) throws IOException {
        // Arrange
        Path file = dir.resolve("data.csv");

        // Act
        storage.save(file, List.of());

        // Assert
        assertTrue(Files.notExists(dir.resolve("data.csv.tmp")));
    }
}
