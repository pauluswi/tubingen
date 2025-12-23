package com.pswied.tubingen.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ModelTest {

    @Test
    void testTransactionRecord() {
        TransactionRecord record = new TransactionRecord();
        record.setSource("A");
        record.setTransactionId("123");
        record.setAmount(BigDecimal.TEN);
        record.setTimestamp(Instant.now());
        record.setRawPayload("payload");

        assertEquals("A", record.getSource());
        assertEquals("123", record.getTransactionId());
        assertEquals(BigDecimal.TEN, record.getAmount());
        assertNotNull(record.getTimestamp());
        assertEquals("payload", record.getRawPayload());

        TransactionRecord record2 = new TransactionRecord("A", "123", BigDecimal.TEN, record.getTimestamp(), "payload");
        assertEquals(record, record2);
        assertEquals(record.hashCode(), record2.hashCode());
        assertNotNull(record.toString());
    }

    @Test
    void testReconciliationMatch() {
        ReconciliationMatch match = new ReconciliationMatch();
        match.setType(ReconciliationMatch.MatchType.MATCHED);
        match.setTransactionA(new TransactionRecord());
        match.setTransactionB(new TransactionRecord());

        assertEquals(ReconciliationMatch.MatchType.MATCHED, match.getType());
        assertNotNull(match.getTransactionA());
        assertNotNull(match.getTransactionB());

        ReconciliationMatch match2 = new ReconciliationMatch(ReconciliationMatch.MatchType.MATCHED, match.getTransactionA(), match.getTransactionB());
        assertEquals(match, match2);
        assertEquals(match.hashCode(), match2.hashCode());
        assertNotNull(match.toString());
    }

    @Test
    void testMatchTypeEnum() {
        for (ReconciliationMatch.MatchType type : ReconciliationMatch.MatchType.values()) {
            assertNotNull(ReconciliationMatch.MatchType.valueOf(type.name()));
        }
    }

    @Test
    void testReconciliationJob() {
        ReconciliationJob job = new ReconciliationJob();
        job.setJobId("1");
        job.setJobName("Test");
        job.setStatus("CREATED");
        job.setSummary(new ReconciliationJob.JobSummary());

        assertEquals("1", job.getJobId());
        assertEquals("Test", job.getJobName());
        assertEquals("CREATED", job.getStatus());
        assertNotNull(job.getSummary());

        ReconciliationJob job2 = new ReconciliationJob("1", "Test", "CREATED", new ReconciliationJob.JobSummary());
        assertEquals(job, job2);
        assertEquals(job.hashCode(), job2.hashCode());

        ReconciliationJob job3 = new ReconciliationJob("Test Job");
        assertNotNull(job3.getJobId());
        assertEquals("Test Job", job3.getJobName());
        assertEquals("CREATED", job3.getStatus());
        assertNotNull(job3.getSummary());
        
        assertNotNull(job.toString());
        assertNotEquals(job, job3);
    }

    @Test
    void testJobSummary() {
        ReconciliationJob.JobSummary summary = new ReconciliationJob.JobSummary();
        summary.setMatched(1);
        summary.setMissingInSourceA(2);
        summary.setMissingInSourceB(3);
        summary.setAmountMismatch(4);
        summary.setDuplicates(5);

        assertEquals(1, summary.getMatched());
        assertEquals(2, summary.getMissingInSourceA());
        assertEquals(3, summary.getMissingInSourceB());
        assertEquals(4, summary.getAmountMismatch());
        assertEquals(5, summary.getDuplicates());

        ReconciliationJob.JobSummary summary2 = new ReconciliationJob.JobSummary(1, 2, 3, 4, 5);
        assertEquals(summary, summary2);
        assertEquals(summary.hashCode(), summary2.hashCode());
        assertNotNull(summary.toString());
    }
}
