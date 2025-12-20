package com.pswied.tubingen.controller;

import com.pswied.tubingen.model.ReconciliationJob;
import com.pswied.tubingen.model.ReconciliationMatch;
import com.pswied.tubingen.model.TransactionRecord;
import com.pswied.tubingen.service.ReconciliationService;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;
    
    // In-memory storage for demo purposes
    private final List<TransactionRecord> sourceA = new ArrayList<>();
    private final List<TransactionRecord> sourceB = new ArrayList<>();
    private final Map<String, ReconciliationJob> jobs = new ConcurrentHashMap<>();
    private final Map<String, List<ReconciliationMatch>> jobResults = new ConcurrentHashMap<>();

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<String> ingest(@RequestBody List<TransactionRecord> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return ResponseEntity.badRequest().body("No transactions provided");
        }
        
        String source = transactions.get(0).getSource();
        if ("A".equalsIgnoreCase(source)) {
            sourceA.addAll(transactions);
        } else if ("B".equalsIgnoreCase(source)) {
            sourceB.addAll(transactions);
        } else {
            return ResponseEntity.badRequest().body("Invalid source. Must be A or B");
        }
        
        return ResponseEntity.ok("Ingested " + transactions.size() + " transactions for source " + source);
    }

    @PostMapping("/reconcile/start")
    public ResponseEntity<ReconciliationJob> startReconciliation(@RequestBody ReconciliationRequest request) {
        ReconciliationJob job = new ReconciliationJob(request.getJobName());
        
        List<ReconciliationMatch> matches = reconciliationService.reconcile(
                new ArrayList<>(sourceA), 
                new ArrayList<>(sourceB), 
                request.getTimestampToleranceMillis(), 
                request.getAmountTolerance()
        );
        
        job.setSummary(reconciliationService.summarize(matches));
        job.setStatus("COMPLETED");
        
        jobs.put(job.getJobId(), job);
        jobResults.put(job.getJobId(), matches);
        
        return ResponseEntity.ok(job);
    }

    @GetMapping("/recon/{jobId}/status")
    public ResponseEntity<ReconciliationJob> getJobStatus(@PathVariable String jobId) {
        ReconciliationJob job = jobs.get(jobId);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(job);
    }

    @GetMapping("/recon/{jobId}/results")
    public ResponseEntity<List<ReconciliationMatch>> getJobResults(@PathVariable String jobId) {
        List<ReconciliationMatch> results = jobResults.get(jobId);
        if (results == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(results);
    }

    @Data
    public static class ReconciliationRequest {
        private String jobName;
        private String sourceA;
        private String sourceB;
        private long timestampToleranceMillis;
        private BigDecimal amountTolerance;
    }
}
