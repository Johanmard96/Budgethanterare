package com.example.java26.budget;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BudgetCalculatorTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 20);

    private static Transaction income(String category, double amount) {
        return new Transaction(DATE, category, amount, TransactionType.INKOMST);
    }

    private static Transaction expense(String category, double amount) {
        return new Transaction(DATE, category, amount, TransactionType.UTGIFT);
    }

    // --- Gränsfall: inga transaktioner ---

    @Test
    @DisplayName("Tom lista ger saldo 0, summor 0 och tom kategorikarta")
    void emptyList_givesZeroEverywhere() {
        // Arrange
        List<Transaction> none = List.of();

        // Act + Assert
        assertEquals(0.0, BudgetCalculator.balance(none), 0.0001);
        assertEquals(0.0, BudgetCalculator.totalIncome(none), 0.0001);
        assertEquals(0.0, BudgetCalculator.totalExpenses(none), 0.0001);
        assertTrue(BudgetCalculator.sumByCategory(none).isEmpty());
    }

    // --- Normalfall ---

    @Test
    @DisplayName("Saldo = inkomster minus utgifter")
    void balance_isIncomeMinusExpenses() {
        // Arrange
        List<Transaction> transactions = List.of(
                income("Lon", 25000), expense("Hyra", 9000), expense("Mat", 3500.50));

        // Act
        double balance = BudgetCalculator.balance(transactions);

        // Assert
        assertEquals(12499.50, balance, 0.0001);
    }

    @Test
    @DisplayName("Total inkomst och total utgift summeras var för sig")
    void totals_areSummedPerType() {
        // Arrange
        List<Transaction> transactions = List.of(
                income("Lon", 1000), income("Bidrag", 250), expense("Mat", 400));

        // Act + Assert
        assertEquals(1250.0, BudgetCalculator.totalIncome(transactions), 0.0001);
        assertEquals(400.0, BudgetCalculator.totalExpenses(transactions), 0.0001);
    }

    @Test
    @DisplayName("Saldo blir negativt när utgifterna överstiger inkomsterna")
    void balance_canBeNegative() {
        // Arrange
        List<Transaction> transactions = List.of(income("Lon", 100), expense("Hyra", 350));

        // Act
        double balance = BudgetCalculator.balance(transactions);

        // Assert
        assertEquals(-250.0, balance, 0.0001);
    }

    @Test
    @DisplayName("Bara utgifter: total inkomst är 0 och saldot är negativt")
    void onlyExpenses_incomeIsZero() {
        // Arrange
        List<Transaction> transactions = List.of(expense("Mat", 50), expense("Mat", 25));

        // Act + Assert
        assertEquals(0.0, BudgetCalculator.totalIncome(transactions), 0.0001);
        assertEquals(-75.0, BudgetCalculator.balance(transactions), 0.0001);
    }

    @Test
    @DisplayName("Saldot är alltid inkomster minus utgifter")
    void balance_equalsTotalIncomeMinusTotalExpenses() {
        // Arrange
        List<Transaction> transactions = List.of(
                income("Lon", 1234.56), expense("Mat", 78.9), expense("Nojen", 0.01), income("Bidrag", 5));

        // Act
        double expected = BudgetCalculator.totalIncome(transactions) - BudgetCalculator.totalExpenses(transactions);

        // Assert
        assertEquals(expected, BudgetCalculator.balance(transactions), 0.0001);
    }

    // --- Summa per kategori ---

    @Test
    @DisplayName("sumByCategory() ger nettosumma per kategori, sorterat på namn")
    void sumByCategory_groupsAndNetsPerCategory() {
        // Arrange
        List<Transaction> transactions = List.of(
                expense("Mat", 100), expense("Mat", 50.25), income("Lon", 3000),
                expense("Hyra", 900), income("Mat", 20));

        // Act
        Map<String, Double> result = BudgetCalculator.sumByCategory(transactions);

        // Assert
        assertEquals(List.of("Hyra", "Lon", "Mat"), List.copyOf(result.keySet()));
        assertEquals(-900.0, result.get("Hyra"), 0.0001);
        assertEquals(3000.0, result.get("Lon"), 0.0001);
        assertEquals(-130.25, result.get("Mat"), 0.0001);
    }

    @Test
    @DisplayName("Kategorier med olika versaler/gemener slås ihop")
    void sumByCategory_isCaseInsensitive() {
        // Arrange
        List<Transaction> transactions = List.of(expense("Mat", 10), expense("mat", 5), expense("MAT", 1));

        // Act
        Map<String, Double> result = BudgetCalculator.sumByCategory(transactions);

        // Assert
        assertEquals(1, result.size());
        assertEquals(-16.0, result.get("mat"), 0.0001);
    }

    @Test
    @DisplayName("Kategorikartan går inte att ändra utifrån")
    void sumByCategory_resultIsUnmodifiable() {
        // Arrange
        Map<String, Double> result = BudgetCalculator.sumByCategory(List.of(expense("Mat", 10)));

        // Act + Assert
        assertThrows(UnsupportedOperationException.class, () -> result.put("Hack", 1.0));
    }

    // --- Gränsvärden och flyttal ---

    @Test
    @DisplayName("Flyttalsfel syns inte: tio gånger 0.10 blir exakt 1.00")
    void balance_hasNoFloatingPointDrift() {
        // Arrange
        List<Transaction> tenDimes = Collections.nCopies(10, income("Smapengar", 0.10));

        // Act
        double total = BudgetCalculator.balance(tenDimes);

        // Assert
        assertEquals(1.0, total, 0.0);
    }

    @Test
    @DisplayName("Minsta möjliga belopp (0.01) summeras korrekt")
    void smallestAmount_isHandled() {
        // Arrange
        List<Transaction> transactions = List.of(income("Ranta", 0.01), expense("Avgift", 0.01));

        // Act
        double balance = BudgetCalculator.balance(transactions);

        // Assert
        assertEquals(0.0, balance, 0.0);
    }

    @Test
    @DisplayName("Största möjliga belopp hanteras utan överflöd")
    void largestAmount_isHandled() {
        // Arrange
        List<Transaction> transactions = List.of(
                income("Arv", Transaction.MAX_AMOUNT), income("Arv", Transaction.MAX_AMOUNT), expense("Hus", 1));

        // Act
        double balance = BudgetCalculator.balance(transactions);

        // Assert
        assertEquals(2 * Transaction.MAX_AMOUNT - 1, balance, 0.0001);
    }

    // --- Felfall ---

    @Test
    @DisplayName("null som indata ger NullPointerException")
    void nullInput_throws() {
        assertThrows(NullPointerException.class, () -> BudgetCalculator.balance(null));
        assertThrows(NullPointerException.class, () -> BudgetCalculator.totalIncome(null));
        assertThrows(NullPointerException.class, () -> BudgetCalculator.totalExpenses(null));
        assertThrows(NullPointerException.class, () -> BudgetCalculator.sumByCategory(null));
    }
}
