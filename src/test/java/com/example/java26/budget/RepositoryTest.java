package com.example.java26.budget;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryTest {

    private Repository<String> repository;

    @BeforeEach
    void setUp() {
        repository = new Repository<>();
    }

    @Test
    void newRepository_isEmpty() {
        assertTrue(repository.isEmpty());
        assertEquals(0, repository.size());
    }
}