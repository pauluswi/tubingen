package com.pswied.tubingen.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReconciliationJob {
    private String jobId;
    private String jobName;
    private String status;
    private JobSummary summary;

    public ReconciliationJob(String jobName) {
        this.jobId = UUID.randomUUID().toString();
        this.jobName = jobName;
        this.status = "CREATED";
        this.summary = new JobSummary();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class JobSummary {
        private int matched;
        private int missingInSourceA;
        private int missingInSourceB;
        private int amountMismatch;
    }
}
