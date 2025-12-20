package com.pswied.tubingen.service;

import com.pswied.tubingen.model.ReconciliationMatch;
import com.pswied.tubingen.model.TransactionRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReconciliationServiceTest {

    private ReconciliationService service;

    @BeforeEach
    void setUp() {
        service = new ReconciliationService();
    }

    @Test
    void testExactMatch() {
        List<TransactionRecord> listA = new ArrayList<>();
        listA.add(new TransactionRecord("A", "TX1", new BigDecimal("100.00"), Instant.parse("2023-10-27T10:00:00Z"), "raw"));

        List<TransactionRecord> listB = new ArrayList<>();
        listB.add(new TransactionRecord("B", "TX1", new BigDecimal("100.00"), Instant.parse("2023-10-27T10:00:00Z"), "raw"));

        List<ReconciliationMatch> results = service.reconcile(listA, listB, 0, BigDecimal.ZERO);

        assertEquals(1, results.size());
        assertEquals(ReconciliationMatch.MatchType.MATCHED, results.get(0).getType());
    }

    @Test
    void testAmountMismatch() {
        List<TransactionRecord> listA = new ArrayList<>();
        listA.add(new TransactionRecord("A", "TX1", new BigDecimal("100.00"), Instant.parse("2023-10-27T10:00:00Z"), "raw"));

        List<TransactionRecord> listB = new ArrayList<>();
        listB.add(new TransactionRecord("B", "TX1", new BigDecimal("105.00"), Instant.parse("2023-10-27T10:00:00Z"), "raw"));

        List<ReconciliationMatch> results = service.reconcile(listA, listB, 0, BigDecimal.ZERO);

        assertEquals(1, results.size());
        assertEquals(ReconciliationMatch.MatchType.AMOUNT_MISMATCH, results.get(0).getType());
    }

    @Test
    void testMissingInB() {
        List<TransactionRecord> listA = new ArrayList<>();
        listA.add(new TransactionRecord("A", "TX1", new BigDecimal("100.00"), Instant.parse("2023-10-27T10:00:00Z"), "raw"));

        List<TransactionRecord> listB = new ArrayList<>();

        List<ReconciliationMatch> results = service.reconcile(listA, listB, 0, BigDecimal.ZERO);

        assertEquals(1, results.size());
        assertEquals(ReconciliationMatch.MatchType.MISSING_IN_SOURCE_B, results.get(0).getType());
    }

    @Test
    void testMissingInA() {
        List<TransactionRecord> listA = new ArrayList<>();
        List<TransactionRecord> listB = new ArrayList<>();
        listB.add(new TransactionRecord("B", "TX1", new BigDecimal("100.00"), Instant.parse("2023-10-27T10:00:00Z"), "raw"));

        List<ReconciliationMatch> results = service.reconcile(listA, listB, 0, BigDecimal.ZERO);

        assertEquals(1, results.size());
        assertEquals(ReconciliationMatch.MatchType.MISSING_IN_SOURCE_A, results.get(0).getType());
    }

    @Test
    void testFuzzyMatchTime() {
        List<TransactionRecord> listA = new ArrayList<>();
        listA.add(new TransactionRecord("A", "TX1", new BigDecimal("100.00"), Instant.parse("2023-10-27T10:00:00Z"), "raw"));

        List<TransactionRecord> listB = new ArrayList<>();
        // 5 seconds later, different ID
        listB.add(new TransactionRecord("B", "TX2", new BigDecimal("100.00"), Instant.parse("2023-10-27T10:00:05Z"), "raw"));

        // Tolerance of 10 seconds
        List<ReconciliationMatch> results = service.reconcile(listA, listB, 10000, BigDecimal.ZERO);

        assertEquals(1, results.size());
        assertEquals(ReconciliationMatch.MatchType.MATCHED, results.get(0).getType());
    }

    @Test
    void testFuzzyMatchAmount() {
        List<TransactionRecord> listA = new ArrayList<>();
        listA.add(new TransactionRecord("A", "TX1", new BigDecimal("100.00"), Instant.parse("2023-10-27T10:00:00Z"), "raw"));

        List<TransactionRecord> listB = new ArrayList<>();
        // Same time, slightly different amount
        listB.add(new TransactionRecord("B", "TX2", new BigDecimal("100.05"), Instant.parse("2023-10-27T10:00:00Z"), "raw"));

        // Tolerance of 0.10
        List<ReconciliationMatch> results = service.reconcile(listA, listB, 0, new BigDecimal("0.10"));

        assertEquals(1, results.size());
        assertEquals(ReconciliationMatch.MatchType.MATCHED, results.get(0).getType());
    }

    @Test
    void testComplexScenario() {
        List<TransactionRecord> listA = new ArrayList<>();
        listA.add(new TransactionRecord("A", "TX1", new BigDecimal("100.00"), Instant.parse("2023-10-27T10:00:00Z"), "raw"));
        listA.add(new TransactionRecord("A", "TX2", new BigDecimal("200.00"), Instant.parse("2023-10-27T10:05:00Z"), "raw"));
        listA.add(new TransactionRecord("A", "TX3", new BigDecimal("300.00"), Instant.parse("2023-10-27T10:10:00Z"), "raw"));

        List<TransactionRecord> listB = new ArrayList<>();
        listB.add(new TransactionRecord("B", "TX1", new BigDecimal("100.00"), Instant.parse("2023-10-27T10:00:00Z"), "raw")); // Match
        listB.add(new TransactionRecord("B", "TX4", new BigDecimal("400.00"), Instant.parse("2023-10-27T10:15:00Z"), "raw")); // Missing in A

        List<ReconciliationMatch> results = service.reconcile(listA, listB, 0, BigDecimal.ZERO);

        assertEquals(4, results.size());
        
        // Verify summary
        var summary = service.summarize(results);
        assertEquals(1, summary.getMatched());
        assertEquals(1, summary.getMissingInSourceA()); // TX4
        assertEquals(2, summary.getMissingInSourceB()); // TX2, TX3
    }
}
