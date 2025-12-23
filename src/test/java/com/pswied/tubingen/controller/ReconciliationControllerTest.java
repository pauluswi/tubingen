package com.pswied.tubingen.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pswied.tubingen.model.TransactionRecord;
import com.pswied.tubingen.service.ReconciliationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(ReconciliationController.class)
@Import(ReconciliationService.class)
class ReconciliationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testIngestAndReconcile() throws Exception {
        // 1. Ingest Source A
        List<TransactionRecord> listA = List.of(
                new TransactionRecord("A", "TX1", new BigDecimal("100.00"), Instant.parse("2023-10-27T10:00:00Z"), "raw")
        );
        
        mockMvc.perform(post("/api/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(listA)))
                .andExpect(status().isOk());

        // 2. Ingest Source B
        List<TransactionRecord> listB = List.of(
                new TransactionRecord("B", "TX1", new BigDecimal("100.00"), Instant.parse("2023-10-27T10:00:00Z"), "raw")
        );

        mockMvc.perform(post("/api/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(listB)))
                .andExpect(status().isOk());

        // 3. Start Reconciliation
        ReconciliationController.ReconciliationRequest request = new ReconciliationController.ReconciliationRequest();
        request.setJobName("Test Job");
        request.setSourceA("A");
        request.setSourceB("B");
        request.setTimestampToleranceMillis(0);
        request.setAmountTolerance(BigDecimal.ZERO);

        String response = mockMvc.perform(post("/api/reconcile/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").exists())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andReturn().getResponse().getContentAsString();

        String jobId = objectMapper.readTree(response).get("jobId").asText();

        // 4. Check Status
        mockMvc.perform(get("/api/recon/" + jobId + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId));

        // 5. Check Results
        mockMvc.perform(get("/api/recon/" + jobId + "/results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("MATCHED"));
    }

    @Test
    void testIngestInvalid() throws Exception {
        mockMvc.perform(post("/api/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testIngestInvalidSource() throws Exception {
        List<TransactionRecord> listC = List.of(
                new TransactionRecord("C", "TX1", new BigDecimal("100.00"), Instant.parse("2023-10-27T10:00:00Z"), "raw")
        );

        mockMvc.perform(post("/api/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(listC)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid source. Must be A or B"));
    }
    
    @Test
    void testGetJobStatusNotFound() throws Exception {
        mockMvc.perform(get("/api/recon/invalid-id/status"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetJobResultsNotFound() throws Exception {
        mockMvc.perform(get("/api/recon/invalid-id/results"))
                .andExpect(status().isNotFound());
    }
}
