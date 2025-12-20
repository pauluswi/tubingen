package com.pswied.tubingen.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionRecord {
    private String source;
    private String transactionId;
    private BigDecimal amount;
    private Instant timestamp;
    private String rawPayload;
}
