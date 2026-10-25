package com.example.java26.budget;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 20);

    @Test
    @DisplayName("En giltig transaktion skapas med givna värden")
    void validTransaction_keepsValues() {
        // Arrange + Act
        Transaction t = new Transaction(DATE, "Mat", 123.5, TransactionType.UTGIFT);

        // Assert
        assertEquals(DATE, t.date());
        assertEquals("Mat", t.category());
        assertEquals(123.5, t.amount(), 0.0001);
        assertEquals(TransactionType.UTGIFT, t.type());
    }

    @Test
    @DisplayName("Kategorin trimmas")
    void category_isStripped() {
        Transaction t = new Transaction(DATE, "  Mat  ", 10, TransactionType.UTGIFT);

        assertEquals("Mat", t.category());
    }

    @Test
    @DisplayName("Beloppet avrundas till två decimaler")
    void amount_isRoundedToTwoDecimals() {
        Transaction t = new Transaction(DATE, "Mat", 10.126, TransactionType.UTGIFT);

        assertEquals(10.13, t.amount(), 0.0001);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -0.01, -100.0, 0.004, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    @DisplayName("Ogiltiga belopp avvisas (noll, negativt, avrundas till noll, NaN, oändligt)")
    void invalidAmount_throwsInvalidTransactionException(double amount) {
        assertThrows(InvalidTransactionException.class,
                () -> new Transaction(DATE, "Mat", amount, TransactionType.UTGIFT));
    }

    @Test
    @DisplayName("Gränsvärden: minsta och största tillåtna belopp accepteras")
    void boundaryAmounts_areAccepted() {
        assertDoesNotThrow(() -> new Transaction(DATE, "Mat", 0.01, TransactionType.UTGIFT));
        assertDoesNotThrow(() -> new Transaction(DATE, "Mat", Transaction.MAX_AMOUNT, TransactionType.UTGIFT));
    }

    @Test
    @DisplayName("Gränsvärde: strax över största tillåtna belopp avvisas")
    void amountJustAboveMax_isRejected() {
        assertThrows(InvalidTransactionException.class,
                () -> new Transaction(DATE, "Mat", Transaction.MAX_AMOUNT + 0.01, TransactionType.UTGIFT));
    }

    @Test
    @DisplayName("Felmeddelandet för negativt belopp förklarar hur utgifter anges")
    void negativeAmount_messageExplainsHowToEnterExpenses() {
        InvalidTransactionException e = assertThrows(InvalidTransactionException.class,
                () -> new Transaction(DATE, "Mat", -5, TransactionType.UTGIFT));

        assertTrue(e.getMessage().contains("UTGIFT"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "Mat;Dryck", "Mat\nDryck"})
    @DisplayName("Ogiltiga kategorier avvisas (tom, blank, semikolon, radbrytning)")
    void invalidCategory_throws(String category) {
        assertThrows(InvalidTransactionException.class,
                () -> new Transaction(DATE, category, 10, TransactionType.UTGIFT));
    }

    @Test
    @DisplayName("null i datum, kategori eller typ avvisas")
    void nullFields_areRejected() {
        assertThrows(InvalidTransactionException.class,
                () -> new Transaction(null, "Mat", 10, TransactionType.UTGIFT));
        assertThrows(InvalidTransactionException.class,
                () -> new Transaction(DATE, null, 10, TransactionType.UTGIFT));
        assertThrows(InvalidTransactionException.class,
                () -> new Transaction(DATE, "Mat", 10, null));
    }

    @Test
    @DisplayName("signedAmount() är positivt för inkomst och negativt för utgift")
    void signedAmount_dependsOnType() {
        Transaction income = new Transaction(DATE, "Lon", 100, TransactionType.INKOMST);
        Transaction expense = new Transaction(DATE, "Mat", 40, TransactionType.UTGIFT);

        assertEquals(100.0, income.signedAmount(), 0.0001);
        assertEquals(-40.0, expense.signedAmount(), 0.0001);
    }

    @Test
    @DisplayName("BY_DATE sorterar på datum och därefter på kategori")
    void byDateComparator_sortsByDateThenCategory() {
        // Arrange
        Transaction late = new Transaction(LocalDate.of(2026, 3, 1), "A", 1, TransactionType.UTGIFT);
        Transaction earlyB = new Transaction(LocalDate.of(2026, 1, 1), "b", 1, TransactionType.UTGIFT);
        Transaction earlyA = new Transaction(LocalDate.of(2026, 1, 1), "A", 1, TransactionType.UTGIFT);

        // Act
        List<Transaction> sorted = List.of(late, earlyB, earlyA).stream().sorted(Transaction.BY_DATE).toList();

        // Assert
        assertEquals(List.of(earlyA, earlyB, late), sorted);
    }
}
