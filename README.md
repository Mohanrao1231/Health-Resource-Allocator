<<<<<<< HEAD
# 🏥 PulseTriage™ — Emergency Department Triage Priority Service

A clinical decision support, real-time patient prioritization, and physician resource dispatching engine for Emergency Departments (ED). Built using **Spring Boot 3**, **Spring Data JPA**, and fundamental **Computer Science Data Structures & Algorithms (DSA)** to prioritize emergency patients based on clinical severity, vital sign derangements, wait time, and available hospital resources.

---

## 🌐 Live Application & Web Dashboard

When running locally, access the interactive dashboard, metrics, and database consoles:

- **🖥️ Interactive Clinical Web Dashboard:** [http://localhost:8080](http://localhost:8080)
- **❤️ Actuator Health Endpoint:** [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- **📊 Micrometer Prometheus Metrics:** [http://localhost:8080/actuator/metrics](http://localhost:8080/actuator/metrics)
- **🗄️ H2 In-Memory Database Console:** [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
  - **JDBC URL:** `jdbc:h2:mem:triage`
  - **User:** `sa`
  - **Password:** *(empty)*

---

## ⚡ Interactive Web Console Features

The built-in web frontend (`src/main/resources/static/index.html`) serves a modern, dark-themed clinical monitoring console:

1. **🚨 Real-Time Priority Queue (`PriorityQueue<TriageRecord>` Visualizer):**
   - Live ranked list of waiting patients ordered by clinical urgency and arrival time.
   - Dynamic ESI color badges (`ESI 1 - Red`, `ESI 2 - Orange`, `ESI 3 - Yellow`, `ESI 4 - Blue`, `ESI 5 - Green`).
   - Vital sign pill monitors with automated alerts for hypoxia ($SpO_2 < 92\%$) and severe tachycardia ($HR > 120$).
   - Live elapsed waiting time clock counters.

2. **🩺 Clinical Intake Assessment Simulator:**
   - Pre-loaded patient selector or walk-in patient form.
   - Sliders and numeric inputs for Heart Rate, Blood Pressure (Sys/Dia), Respiratory Rate, $SpO_2$, Pain Score (0–10), and GCS Score (3–15).
   - Live ESI calculation and 1-click enqueue into the priority queue.

3. **👨‍⚕️ Physician Dispatching Console (`Doctor FIFO Queue` Visualizer):**
   - Displays real-time on-duty physician cards with status indicators (`● Ready for patient` / `● In Consultation`).
   - **"⚡ Dispatch Next Priority Patient"** action button pairs the top-priority patient with the next available doctor.

4. **↺ Transactional Undo Controller (`Stack<AuditActionMemento>` Visualizer):**
   - **"↺ Undo Last Decision"** button allows emergency clinical staff to immediately revert misassigned patients, restore them back into the priority queue, and free up physicians.

5. **📜 Clinical Decision Audit Trail:**
   - Chronological table logging state transitions, actions, timestamps, and operators.

---

## 🎯 Clinical Problem & Triage Model

Emergency departments face unpredictable surges in acute patient volume. Manual triage creates bottlenecks, delays care for critical patients, and increases clinical morbidity.

This service implements the **Emergency Severity Index (ESI 1–5)** clinical algorithm:
1. **Level 1 — Resuscitation (Immediate):** Immediate life-saving intervention required (cardiac/respiratory arrest, severe hypoxia $SpO_2 < 85\%$, profound shock $SBP < 70$, unresponsive $GCS \le 8$, severe bradycardia/tachycardia).
2. **Level 2 — Emergent (High Risk):** High-risk clinical complaints (chest pain, stroke, major trauma), danger-zone vitals ($SpO_2 < 92\%$, $HR > 130$ or $< 50$, $RR > 30$), severe acute pain ($\ge 8/10$), or confusion/lethargy ($GCS < 14$).
3. **Level 3 — Urgent:** Clinically stable vitals, projected need for **two or more** emergency diagnostic/therapeutic resources (e.g. labs, IV fluids, ECG, imaging).
4. **Level 4 — Less Urgent:** Clinically stable vitals, projected need for **one** simple resource (e.g. plain X-ray, oral medication, simple laceration).
5. **Level 5 — Non-Urgent:** Clinically stable vitals, **zero** projected diagnostic resources (e.g. prescription refill, suture check, mild rash).

---

## 🧩 Data Structures & Algorithms (DSA) Mapping

| Component | Data Structure | Time Complexity | Purpose |
|---|---|---|---|
| **Patient Prioritization** | `PriorityQueue<TriageRecord>` | $O(\log N)$ Insert / Poll | Orders waiting patients by **Severity Score DESC**, tie-breaking on **Arrival Time ASC** ($FIFO$). |
| **Instant Lookup Cache** | `ConcurrentHashMap<Long, TriageRecord>` | $O(1)$ | Provides constant-time active triage encounter lookup by Patient ID & Record ID. |
| **Physician Availability Pool** | `ConcurrentLinkedQueue<Doctor>` | $O(1)$ | Thread-safe FIFO queue for dispatching on-duty emergency physicians. |
| **Audit Trail Undo Stack** | `ArrayDeque<AuditActionMemento>` | $O(1)$ Push / Pop | LIFO Undo stack preserving previous clinical state snapshots to safely revert decisions. |

---

## 🛠️ Tech Stack

- **Framework:** Spring Boot 3.3.4 (Spring Web, Spring Data JPA, Spring Validation, Actuator)
- **Language & Runtime:** Java 17 / 21 / 25
- **Database:** In-memory H2 Database (`jdbc:h2:mem:triage`) with PostgreSQL production dialect readiness
- **Metrics & Observability:** Spring Boot Actuator + Micrometer Prometheus registry
- **Frontend:** Glassmorphic Single-Page Application (HTML5, Vanilla CSS3, JavaScript ES6)
- **Testing:** JUnit 5, Mockito, AssertJ, MockMvc
- **Build Tool:** Apache Maven 3.9+
- **Containerization:** Multi-stage `Dockerfile` & `docker-compose.yml`

---

## 🚀 REST API Reference

### Triage Endpoints (`/triage`)

#### 1. Assess Patient (`POST /triage/assess`)
Evaluates vital signs and chief complaint, computes the ESI level, persists the record, and optionally enqueues into the priority queue.

```bash
curl -X POST http://localhost:8080/triage/assess \
  -H "Content-Type: application/json" \
  -d '{
    "patientMrn": "MRN-89210",
    "chiefComplaint": "Crushing chest pain and cold sweats",
    "vitals": {
      "heartRate": 115,
      "systolicBp": 145,
      "diastolicBp": 92,
      "respiratoryRate": 22,
      "oxygenSaturation": 95.0,
      "temperature": 37.0,
      "painScore": 9,
      "gcsScore": 15
    },
    "estimatedResources": 3,
    "autoEnqueue": true
  }'
```

#### 2. View Priority Waiting Queue (`GET /triage/queue`)
Returns an ordered list of waiting patients ranked strictly by clinical urgency and arrival time.

```bash
curl -X GET http://localhost:8080/triage/queue
```

#### 3. Dispatch Next Priority Patient (`POST /triage/next`)
Dequeues the top-priority patient and assigns them to the next available emergency physician from the FIFO doctor pool.

```bash
curl -X POST http://localhost:8080/triage/next
```

#### 4. Update Triage Status (`PUT /triage/{id}/status`)
Updates encounter status (e.g. `IN_CONSULTATION`, `COMPLETED`, `CANCELLED`). When completed, automatically frees the physician back to the available doctor queue.

```bash
curl -X PUT http://localhost:8080/triage/1/status \
  -H "Content-Type: application/json" \
  -d '{
    "status": "COMPLETED",
    "clinicalNotes": "Discharged with oral prescription"
  }'
```

#### 5. Undo Last Triage Action (`POST /triage/undo`)
Reverts the last state mutation using the LIFO undo stack.

```bash
curl -X POST http://localhost:8080/triage/undo
```

#### 6. Patient O(1) Lookup (`GET /triage/lookup/patient/{patientId}`)
Fast in-memory lookup of active triage encounter.

```bash
curl -X GET http://localhost:8080/triage/lookup/patient/1
```

#### 7. Audit Log History (`GET /triage/audit-logs`)
Retrieves chronological regulatory audit trail of all clinical decisions.

```bash
curl -X GET http://localhost:8080/triage/audit-logs
```

---

## 👨‍⚕️ Staff & Patient Management Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/doctors` | List all physicians |
| `GET` | `/doctors/available` | View active FIFO available physician queue |
| `POST` | `/doctors` | Register new physician |
| `PUT` | `/doctors/{id}/availability` | Toggle doctor on-duty/off-duty status |
| `GET` | `/patients` | List registered patients |
| `POST` | `/patients` | Register new patient |
| `GET` | `/patients/mrn/{mrn}` | Lookup patient by Medical Record Number |

---

## 🧪 Testing & Verification

Run the comprehensive unit and integration test suite:

```bash
mvn clean test
```

### Test Coverage Summary:
- **`TriageScoringServiceTest`** (11 tests): Validates ESI 1 through 5 classifications, danger-zone triggers, resuscitation criteria, and input validation.
- **`TriageQueueServiceTest`** (6 tests): Validates PriorityQueue sorting (Severity DESC, Arrival ASC), $O(1)$ HashMap lookups, FIFO physician queue dispatching, and Stack undo state rollbacks.
- **`TriageControllerTest`** (2 tests): Validates end-to-end HTTP REST flows with JSON payloads and error statuses.
- **`TriageApplicationTests`** (1 test): Validates Spring Boot context initialization.

```
[INFO] Results:
[INFO] Tests run: 20, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## ☁️ Cloud Deployment Guide

This service is containerized and ready for 1-click cloud deployment.

### Option 1: Deploy to Render (Free)
1. Go to [Render.com](https://render.com) and click **New +** -> **Web Service**.
2. Connect your GitHub repository: `DHANUSH141495/emergency-triage-service`.
3. Configuration:
   - **Environment:** `Docker`
   - **Branch:** `main`
   - **Plan:** `Free`
4. Click **Create Web Service**. Render will automatically build the `Dockerfile` and provide a public HTTPS URL (e.g. `https://emergency-triage-service.onrender.com`).

### Option 2: Deploy to Railway
1. Go to [Railway.app](https://railway.app) and click **New Project** -> **Deploy from GitHub repo**.
2. Select `DHANUSH141495/emergency-triage-service`.
3. Railway detects the `Dockerfile` and deploys automatically. Under service settings, click **Generate Domain** to get a public URL.

### Option 3: Run with Docker Locally
```bash
docker build -t emergency-triage-service .
docker run -p 8080:8080 emergency-triage-service
```
or with Docker Compose:
```bash
docker compose up --build
```

---

## 🧑‍💻 About the Project

Designed and developed as an Emergency Department prioritization and queue management service. Incorporates clean architecture, domain-driven design, and thread-safe data structures suitable for high-throughput clinical healthcare systems.
=======
# Health-Resource-Allocator
>>>>>>> 55646633a3fab08946294f4f4e214b74daa2de06
