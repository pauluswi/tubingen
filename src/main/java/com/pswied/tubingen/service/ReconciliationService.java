package com.pswied.tubingen.service;

import com.pswied.tubingen.model.ReconciliationJob;
import com.pswied.tubingen.model.ReconciliationMatch;
import com.pswied.tubingen.model.TransactionRecord;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ReconciliationService {

    public List<ReconciliationMatch> reconcile(List<TransactionRecord> listA, List<TransactionRecord> listB, long toleranceMillis, BigDecimal amountTolerance) {
        List<ReconciliationMatch> results = new ArrayList<>();
        
        // Sort both lists by timestamp
        listA.sort(Comparator.comparing(TransactionRecord::getTimestamp));
        listB.sort(Comparator.comparing(TransactionRecord::getTimestamp));

        int i = 0;
        int j = 0;

        while (i < listA.size() && j < listB.size()) {
            TransactionRecord recA = listA.get(i);
            TransactionRecord recB = listB.get(j);

            // Check for exact ID match first (if applicable)
            if (recA.getTransactionId().equals(recB.getTransactionId())) {
                if (isAmountMatching(recA.getAmount(), recB.getAmount(), amountTolerance)) {
                    results.add(new ReconciliationMatch(ReconciliationMatch.MatchType.MATCHED, recA, recB));
                } else {
                    results.add(new ReconciliationMatch(ReconciliationMatch.MatchType.AMOUNT_MISMATCH, recA, recB));
                }
                i++;
                j++;
                continue;
            }

            // Time-based comparison
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
        while (i < listA.size()) {
            results.add(new ReconciliationMatch(ReconciliationMatch.MatchType.MISSING_IN_SOURCE_B, listA.get(i), null));
            i++;
        }

        while (j < listB.size()) {
            results.add(new ReconciliationMatch(ReconciliationMatch.MatchType.MISSING_IN_SOURCE_A, null, listB.get(j)));
            j++;
        }

        return results;
    }

    private boolean isAmountMatching(BigDecimal amount1, BigDecimal amount2, BigDecimal tolerance) {
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
            }
        }
        return summary;
    }
}
