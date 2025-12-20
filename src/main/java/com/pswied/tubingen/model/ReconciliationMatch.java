package com.pswied.tubingen.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReconciliationMatch {
    private MatchType type;
    private TransactionRecord transactionA;
    private TransactionRecord transactionB;

    public enum MatchType {
        MATCHED,
        MISSING_IN_SOURCE_A,
        MISSING_IN_SOURCE_B,
        AMOUNT_MISMATCH,
        DUPLICATE
    }
}
