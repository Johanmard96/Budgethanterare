package com.example.java26.budget;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionPredicatesTest {

    private Repository<Transaction> repository;
    private Transaction salary;
    private Transaction foodJan;
    private Transaction foodFeb;

    @BeforeEach
    void setUp() {
        salary = new Transaction(LocalDate.of(2026, 1, 25), "Lön", 30000, TransactionType.INKOMST);
        foodJan = new Transaction(LocalDate.of(2026, 1, 31), "Mat", 500, TransactionType.UTGIFT);
        foodFeb = new Transaction(LocalDate.of(2026, 2, 1), "Mat", 450, TransactionType.UTGIFT);
        repository = new Repository<>();
        repository.addAll(List.of(salary, foodJan, foodFeb));
    }

    @Test
    @DisplayName("ofType() väljer rätt typ")
    void ofType_selectsMatchingType() {
        assertEquals(List.of(salary), repository.findWhere(TransactionPredicates.ofType(TransactionType.INKOMST)));
        assertEquals(List.of(foodJan, foodFeb),
                repository.findWhere(TransactionPredicates.ofType(TransactionType.UTGIFT)));
    }

    @Test
    @DisplayName("inCategory() ignorerar versaler/gemener och blanksteg")
    void inCategory_ignoresCaseAndWhitespace() {
        assertEquals(List.of(foodJan, foodFeb), repository.findWhere(TransactionPredicates.inCategory("  mAt ")));
    }

    @Test
    @DisplayName("inCategory() för en okänd kategori ger tom lista")
    void inCategory_unknownCategory_returnsEmpty() {
        assertTrue(repository.findWhere(TransactionPredicates.inCategory("Resor")).isEmpty());
    }

    @Test
    @DisplayName("betweenDates() inkluderar både start- och slutdatum")
    void betweenDates_includesBothEnds() {
        // Arrange
        var predicate = TransactionPredicates.betweenDates(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 1));

        // Act
        List<Transaction> result = repository.findWhere(predicate);

        // Assert
        assertEquals(List.of(foodJan, foodFeb), result);
    }

    @Test
    @DisplayName("betweenDates() med samma start och slut ger just den dagen")
    void betweenDates_singleDay() {
        var predicate = TransactionPredicates.betweenDates(LocalDate.of(2026, 1, 25), LocalDate.of(2026, 1, 25));

        assertEquals(List.of(salary), repository.findWhere(predicate));
    }

    @Test
    @DisplayName("betweenDates() med start efter slut kastar IllegalArgumentException")
    void betweenDates_startAfterEnd_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> TransactionPredicates.betweenDates(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1)));
    }

    @Test
    @DisplayName("Filter kan kombineras: utgifter i kategorin Mat före 1 februari")
    void predicates_canBeCombined() {
        // Arrange
        var combined = TransactionPredicates.ofType(TransactionType.UTGIFT)
                .and(TransactionPredicates.inCategory("Mat"))
                .and(TransactionPredicates.betweenDates(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)));

        // Act
        List<Transaction> result = repository.findWhere(combined);

        // Assert
        assertEquals(List.of(foodJan), result);
    }
}
