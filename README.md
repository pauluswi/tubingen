# Tübingen – Reconciliation Engine

**Tübingen – Reconciliation Engine** is a backend-focused banking reconciliation service built with **Java Spring Boot**, designed to match and reconcile financial transactions between two independent sources (e.g. Core Banking vs Switch / Payment Provider).

The system demonstrates **real-world banking reconciliation logic**, using an efficient **Two-Pointer algorithm** to achieve linear-time matching on large, ordered transaction datasets.

This project is intentionally designed as a **clean, production-style backend service**, suitable for banking, fintech, and payment systems.

---

## 🎯 Purpose & Motivation

In real banking systems, transactions are processed by multiple parties:
- Core Banking System
- Payment Switch (ATM, EDC, QR, ISO8583)
- External Clearing or Settlement Providers

Due to network delays, retries, duplicates, or partial failures, transaction records between systems often **do not perfectly align**.

**Reconciliation** ensures:
- Financial consistency
- Accurate settlement
- Early detection of missing, duplicate, or mismatched transactions

This project simulates that exact scenario in a **simplified but realistic way**.

---

## 🧠 Key Features

- Reconcile transactions between **Source A** and **Source B**
- Efficient **Two-Pointer matching algorithm** (O(n))
- Tolerance-based matching (timestamp & amount)
- Detection of:
    - Matched transactions
    - Missing transactions
    - Amount mismatches
    - Duplicates
- Batch-style reconciliation jobs
- RESTful APIs for ingestion, execution, and reporting
- Backend-first design (frontend optional)

---

## 🏗 High-Level Architecture

                ┌────────────────────────┐
                │        Client          │
                │  (curl / Postman / UI) │
                └───────────┬────────────┘
                            │ REST
                            ▼
                ┌────────────────────────┐
                │   Spring Boot API      │
                │  Bremen Recon Engine   │
                └───────────┬────────────┘
                            │
    ┌───────────────────────┼───────────────────────┐
    │                       │                       │
    ▼                       ▼                       ▼
    ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
    │ Ingest       │ │ Ingest       │ │ Ingest       │
    │ Service      │ │ Reconcil     │ │ Reconcil     │
    │ (CSV / Logs) │ │ (Two Ptrs)   │ │ (CSV / Logs) │
    └──────────────┘ └──────────────┘ └──────────────┘
    │                       │                       │
    ▼                       ▼                       ▼
    ┌────────────────────────────────────────────────┐
    │ PostgreSQL / H2                                │
    │ transaction_record | recon_job |               │
    │ recon_match | audit                            │
    └────────────────────────────────────────────────┘


---

## 🧩 Core Domain Model

### TransactionRecord
Represents a normalized transaction from any source.

Key attributes:
- Source (A or B)
- Transaction ID / STAN
- Amount & currency
- Timestamp (UTC)
- Raw payload (for audit)

### Reconciliation Job
Represents a single reconciliation execution between two datasets.

### Reconciliation Match
Stores the outcome of matching:
- MATCHED
- MISSING_IN_SOURCE_A
- MISSING_IN_SOURCE_B
- AMOUNT_MISMATCH
- DUPLICATE

---

## ⚙️ Reconciliation Algorithm (Two-Pointer)

The engine uses a **Two-Pointer technique** on **sorted transaction lists**.

### Why Two Pointers?
- Transaction lists are naturally time-ordered
- Avoids expensive nested loops
- Scales linearly (O(n))
- Industry-standard approach for settlement systems

### Matching Logic (Simplified)
1. Sort Source A and Source B by timestamp
2. Maintain two pointers (A and B)
3. Compare records:
    - Exact key match → MATCHED
    - Timestamp & amount within tolerance → MATCHED
    - Earlier record outside tolerance → MISSING
    - Same timestamp but amount differs → AMOUNT_MISMATCH
4. Advance pointers accordingly
5. Remaining records are marked as missing

This mirrors how real reconciliation engines work in production banking systems.

---

## 🔌 API Overview

### Ingest Transactions
Upload transactions for a specific source.
- **Endpoint:** `POST /api/ingest`
- **Body:** `List<TransactionRecord>`

### Start Reconciliation
Creates and runs a reconciliation job with configurable tolerances.
- **Endpoint:** `POST /api/reconcile/start`
- **Body:** `ReconciliationRequest` (specifies job parameters)

### Get Job Status
Retrieves the status and summary of a reconciliation job.
- **Endpoint:** `GET /api/recon/{jobId}/status`

### Get Job Results
Retrieves the detailed results (matches, mismatches, missing) for a job.
- **Endpoint:** `GET /api/recon/{jobId}/results`

---

## 🚀 How to Use

### 1. Ingest Transactions from Source A

```bash
curl -X POST http://localhost:8080/api/ingest \
-H "Content-Type: application/json" \
-d '[
  {
    "source": "A",
    "transactionId": "TXN1001",
    "amount": 150.00,
    "timestamp": "2023-10-27T10:00:00Z"
  },
  {
    "source": "A",
    "transactionId": "TXN1002",
    "amount": 200.50,
    "timestamp": "2023-10-27T10:05:00Z"
  }
]'
```

### 2. Ingest Transactions from Source B

```bash
curl -X POST http://localhost:8080/api/ingest \
-H "Content-Type: application/json" \
-d '[
  {
    "source": "B",
    "transactionId": "TXN1001",
    "amount": 150.00,
    "timestamp": "2025-10-27T10:00:05Z"
  },
  {
    "source": "B",
    "transactionId": "TXN1003",
    "amount": 300.00,
    "timestamp": "2025-10-27T10:10:00Z"
  }
]'
```

### 3. Start Reconciliation

This triggers the two-pointer algorithm.

```bash
curl -X POST http://localhost:8080/api/reconcile/start \
-H "Content-Type: application/json" \
-d '{
  "jobName": "Daily Reconciliation",
  "sourceA": "A",
  "sourceB": "B",
  "timestampToleranceMillis": 10000,
  "amountTolerance": 0.01
}'
```
> This will return a `jobId`. Let's assume it's `123e4567-e89b-12d3-a456-426614174000`.

### 4. Check Job Status

```bash
curl http://localhost:8080/api/recon/123e4567-e89b-12d3-a456-426614174000/status
```

**Expected Response:**
```json
{
  "jobId": "123e4567-e89b-12d3-a456-426614174000",
  "status": "COMPLETED",
  "summary": {
    "matched": 1,
    "missingInSourceB": 1,
    "missingInSourceA": 1,
    "amountMismatch": 0
  }
}
```

### 5. Get Detailed Results

```bash
curl http://localhost:8080/api/recon/123e4567-e89b-12d3-a456-426614174000/results
```

**Expected Response:**
```json
[
  {
    "type": "MATCHED",
    "transactionA": { "transactionId": "TXN1001", "amount": 150.00},
    "transactionB": { "transactionId": "TXN1001", "amount": 150.00}
  },
  {
    "type": "MISSING_IN_SOURCE_B",
    "transactionA": { "transactionId": "TXN1002"},
    "transactionB": null
  },
  {
    "type": "MISSING_IN_SOURCE_A",
    "transactionA": null,
    "transactionB": { "transactionId": "TXN1003"}
  }
]
```

## 🛠 Technology Stack

- Java 17+
- Spring Boot
- Spring Web
- Spring Data JPA
- H2 (local) / PostgreSQL (production)
- Maven
- Docker (optional)

## 📁 Project Structure
tubingen-reconciliation-engine/

    ├── src/main/java/com/pswied/tubingen/
        │   ├── controller/
        │   ├── service/
        │   ├── model/
        │   ├── repository/
        │   └── config/
        ├── src/test/
        ├── sample-data/
        ├── docs/
        ├── Dockerfile
        ├── pom.xml
        └── README.md

## 🚀 Running Locally

    ./mvnw spring-boot:run

### Access API at

    http://localhost:8080

## 🔐 Security & Production Considerations

- Designed to integrate with JWT / OAuth2
- Input validation & file size limits
- Audit logging for traceability
- Stateless API suitable for horizontal scaling

## 🧭 Roadmap

- CSV upload support (Source A / Source B)
- Async job execution
- React dashboard (optional)
- ISO8583 / ISO20022 mock adapters
- Kubernetes deployment example
- Metrics & monitoring (Actuator + Prometheus)

## 👤 Author

    Slamet Widodo (Wied)
    Software Engineer / Engineering Manager
    Banking · Middleware · Distributed Systems

## 📄 License

    This project is provided for educational and portfolio purposes.
