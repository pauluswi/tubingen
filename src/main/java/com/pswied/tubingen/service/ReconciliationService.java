package com.pswied.tubingen.service;

import com.pswied.tubingen.model.ReconciliationJob;
import com.pswied.tubingen.model.ReconciliationMatch;
import com.pswied.tubingen.model.TransactionRecord;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

@Service
public class ReconciliationService {

    public List<ReconciliationMatch> reconcile(List<TransactionRecord> listA, List<TransactionRecord> listB, long toleranceMillis, BigDecimal amountTolerance) {
        List<ReconciliationMatch> results = new ArrayList<>();

        // --- PRE-PROCESS: Detect Duplicates ---
        results.addAll(extractDuplicates(listA, "A"));
        results.addAll(extractDuplicates(listB, "B"));
        
        // Lists for the second pass (Fuzzy Match)
        List<TransactionRecord> unmatchedA = new ArrayList<>();
        List<TransactionRecord> unmatchedB = new ArrayList<>();

        // --- PASS 1: Exact ID Matching ---
        // Index listB by Transaction ID for O(1) lookup
        // Using a LinkedList to handle potential duplicate IDs in source B
        Map<String, Queue<TransactionRecord>> mapB = new HashMap<>();
        for (TransactionRecord recB : listB) {
            if (recB.getTransactionId() != null) {
                mapB.computeIfAbsent(recB.getTransactionId(), k -> new LinkedList<>()).add(recB);
            } else {
                unmatchedB.add(recB);
            }
        }

        for (TransactionRecord recA : listA) {
            String id = recA.getTransactionId();
            if (id != null && mapB.containsKey(id) && !mapB.get(id).isEmpty()) {
                // Exact ID match found
                TransactionRecord recB = mapB.get(id).poll();
                
                if (recB != null) {
                    if (isAmountMatching(recA.getAmount(), recB.getAmount(), amountTolerance)) {
                        results.add(new ReconciliationMatch(ReconciliationMatch.MatchType.MATCHED, recA, recB));
                    } else {
                        results.add(new ReconciliationMatch(ReconciliationMatch.MatchType.AMOUNT_MISMATCH, recA, recB));
                    }
                }
                
                // Clean up map if empty
                if (mapB.get(id).isEmpty()) {
                    mapB.remove(id);
                }
            } else {
                // No ID match, save for fuzzy matching
                unmatchedA.add(recA);
            }
        }

        // Collect remaining items from B for fuzzy matching
        for (Queue<TransactionRecord> queue : mapB.values()) {
            unmatchedB.addAll(queue);
        }

        // --- PASS 2: Fuzzy Time Matching (Two-Pointer) ---
        // Sort by timestamp
        unmatchedA.sort(Comparator.comparing(TransactionRecord::getTimestamp));
        unmatchedB.sort(Comparator.comparing(TransactionRecord::getTimestamp));

        int i = 0;
        int j = 0;

        while (i < unmatchedA.size() && j < unmatchedB.size()) {
            TransactionRecord recA = unmatchedA.get(i);
            TransactionRecord recB = unmatchedB.get(j);

            long timeDiff = Duration.between(recA.getTimestamp(), recB.getTimestamp()).toMillis();

            if (Math.abs(timeDiff) <= toleranceMillis && isAmountMatching(recA.getAmount(), recB.getAmount(), amountTolerance)) {
                 results.add(new ReconciliationMatch(ReconciliationMatch.MatchType.MATCHED, recA, recB));
                 i++;
                 j++;
            } else if (recA.getTimestamp().isBefore(recB.getTimestamp())) {
                // recA is earlier and not matched, so it's missing in B
                results.add(new ReconciliationMatch(ReconciliationMatch.MatchType.MISSING_IN_SOURCE_B, recA, null));
                i++;
            } else {
                // recB is earlier and not matched, so it's missing in A
                results.add(new ReconciliationMatch(ReconciliationMatch.MatchType.MISSING_IN_SOURCE_A, null, recB));
                j++;
            }
        }

        // Process remaining records
        while (i < unmatchedA.size()) {
            results.add(new ReconciliationMatch(ReconciliationMatch.MatchType.MISSING_IN_SOURCE_B, unmatchedA.get(i), null));
            i++;
        }

        while (j < unmatchedB.size()) {
            results.add(new ReconciliationMatch(ReconciliationMatch.MatchType.MISSING_IN_SOURCE_A, null, unmatchedB.get(j)));
            j++;
        }

        return results;
    }

    /**
     * Identifies and removes duplicates from the provided list.
     * Returns a list of ReconciliationMatch objects for the duplicates found.
     */
    private List<ReconciliationMatch> extractDuplicates(List<TransactionRecord> list, String sourceName) {
        List<ReconciliationMatch> duplicates = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        Iterator<TransactionRecord> iterator = list.iterator();

        while (iterator.hasNext()) {
            TransactionRecord record = iterator.next();
            String id = record.getTransactionId();

            // Skip null IDs for duplicate detection (handled in fuzzy match)
            if (id == null) {
                continue;
            }

            if (seenIds.contains(id)) {
                // Duplicate found!
                ReconciliationMatch match = new ReconciliationMatch();
                match.setType(ReconciliationMatch.MatchType.DUPLICATE);
                if ("A".equals(sourceName)) {
                    match.setTransactionA(record);
                } else {
                    match.setTransactionB(record);
                }
                duplicates.add(match);
                
                // Remove from the list so it's not processed further
                iterator.remove();
            } else {
                seenIds.add(id);
            }
        }
        return duplicates;
    }

    private boolean isAmountMatching(BigDecimal amount1, BigDecimal amount2, BigDecimal tolerance) {
        if (amount1 == null || amount2 == null) return false;
        return amount1.subtract(amount2).abs().compareTo(tolerance) <= 0;
    }
    
    public ReconciliationJob.JobSummary summarize(List<ReconciliationMatch> matches) {
        ReconciliationJob.JobSummary summary = new ReconciliationJob.JobSummary();
        for (ReconciliationMatch match : matches) {
            switch (match.getType()) {
                case MATCHED:
                    summary.setMatched(summary.getMatched() + 1);
                    break;
                case MISSING_IN_SOURCE_A:
                    summary.setMissingInSourceA(summary.getMissingInSourceA() + 1);
                    break;
                case MISSING_IN_SOURCE_B:
                    summary.setMissingInSourceB(summary.getMissingInSourceB() + 1);
                    break;
                case AMOUNT_MISMATCH:
                    summary.setAmountMismatch(summary.getAmountMismatch() + 1);
                    break;
                case DUPLICATE:
                    summary.setDuplicates(summary.getDuplicates() + 1);
                    break;
            }
        }
        return summary;
    }
}
