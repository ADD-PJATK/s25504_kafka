# PLAN.md — s25504_kafka: Real-time Stock Data (Kafka / SSE)

**Student ID:** s25504  
**Date:** 2026-05-07  
**Estimated implementation time:** ~60 minutes  

---

## 1. System Architecture

### Overview

Two independent Spring Boot microservices consume a live stock-price feed exposed by an instructor-run Kafka broker via a simple HTTP/SSE gateway (`https://add.piotrkojalowicz.dev`). No Kafka client library is required — all communication happens over standard HTTP using Spring WebFlux's reactive `WebClient`.

```
┌──────────────────────────────────────────────────────┐
│              Instructor's Kafka Broker                │
│  (SSE Gateway: add.piotrkojalowicz.dev)               │
│  GET /api/tickers  │  /api/latest  │  /api/stream    │
└────────────┬───────────────────────┬─────────────────┘
             │ SSE / HTTP             │ HTTP polling
             ▼                        ▼
┌─────────────────────┐   ┌───────────────────────────┐
│  App #1             │   │  App #2                   │
│  Realtime Dashboard │   │  History Downloader/Viewer│
│  Spring Boot 3      │   │  Spring Boot 3            │
│  + WebFlux (SSE)    │   │  + WebClient (polling)    │
│  + Thymeleaf + JS   │   │  + Thymeleaf + JS         │
│  Port: 8080         │   │  Port: 8081               │
└──────────┬──────────┘   └──────────────┬────────────┘
           │ EventSource (browser→server) │
           ▼                             ▼
┌──────────────────────────────────────────────────────┐
│            Browser (HTML + Chart.js)                 │
│  App #1: live price chart, ticker selector           │
│  App #2: table/chart, CSV/JSON download              │
└──────────────────────────────────────────────────────┘
```

### App #1 — Realtime Dashboard

| Layer | Technology | Role |
|---|---|---|
| Backend | Spring Boot 3 + WebFlux | Proxies SSE stream from upstream API to browser |
| Frontend | Thymeleaf + plain JS (`EventSource`) | Ticker selector, live table, Chart.js price chart |
| API consumed | `GET /api/stream?ticker=X` | One SSE connection per selected ticker |

**Flow:**
1. User opens browser, selects one or more tickers via a checkbox list.
2. Browser opens an `EventSource` to the Spring Boot backend endpoint `/dashboard/stream?tickers=ACME,ALFA`.
3. Spring Boot opens one `WebClient` SSE subscription per ticker to the upstream API, merges the streams, and re-emits them as a single SSE to the browser.
4. Browser appends each tick to a rolling table (last 30 rows) and updates a Chart.js line chart.
5. On connection drop, `EventSource` auto-reconnects; the backend logs the retry.

### App #2 — History Downloader / Viewer

| Layer | Technology | Role |
|---|---|---|
| Backend | Spring Boot 3 + WebClient | Polls `/api/latest` every 10 s, exposes REST endpoints |
| Frontend | Thymeleaf + plain JS | Ticker + time-range selector, table/chart, download button |
| API consumed | `GET /api/latest?ticker=X` | Polling with ≥10 s interval |

**Flow:**
1. User selects tickers and a time range (1–10 minutes).
2. Browser calls `POST /history/fetch` with tickers and minutes.
3. Backend polls `/api/latest` in a loop (10 s delay), collects ticks for the requested window, returns aggregated JSON.
4. Frontend renders a sortable table and a Chart.js line chart.
5. User clicks "Download CSV" or "Download JSON"; backend streams the file from `/history/export?format=csv`.

---

## 2. Project Structure

```
s25504_kafka/
├── app1-realtime-dashboard/
│   ├── src/main/java/com/s25504/dashboard/
│   │   ├── DashboardApplication.java
│   │   ├── config/WebClientConfig.java
│   │   ├── controller/DashboardController.java
│   │   └── service/StockStreamService.java
│   ├── src/main/resources/
│   │   ├── templates/index.html
│   │   └── application.properties
│   ├── src/test/java/com/s25504/dashboard/
│   │   └── DashboardControllerTest.java
│   ├── pom.xml
│   ├── Dockerfile
│   ├── .env.example
│   └── README.md
│
├── app2-history-viewer/
│   ├── src/main/java/com/s25504/history/
│   │   ├── HistoryApplication.java
│   │   ├── config/WebClientConfig.java
│   │   ├── controller/HistoryController.java
│   │   ├── service/StockHistoryService.java
│   │   └── model/StockTick.java
│   ├── src/main/resources/
│   │   ├── templates/index.html
│   │   └── application.properties
│   ├── src/test/java/com/s25504/history/
│   │   └── HistoryServiceTest.java
│   ├── pom.xml
│   ├── Dockerfile
│   ├── .env.example
│   └── README.md
│
├── k8s/
│   ├── app1-deployment.yaml
│   ├── app1-service.yaml
│   ├── app2-deployment.yaml
│   ├── app2-service.yaml
│   └── secrets.yaml          # template only — actual secret via Sealed Secrets / Vault
│
├── .gitlab-ci.yml
├── .gitignore
├── PLAN.md
└── README.md                 # root-level overview + links to each app
```

---

## 3. DevOps & Deployment Strategy

### 3.1 Containerisation (Docker)

Each app has its own multi-stage `Dockerfile`:

```
Stage 1 (builder): maven:3.9-eclipse-temurin-21 → mvn package -DskipTests
Stage 2 (runtime): eclipse-temurin:21-jre-alpine → COPY jar → ENTRYPOINT
```

Environment variables (e.g. `ADD_API_KEY`) are injected at runtime — never baked into the image.

### 3.2 Kubernetes Manifests (GitOps)

```
k8s/
  app1-deployment.yaml   Deployment (replicas: 1), resources limits, liveness probe on /actuator/health
  app1-service.yaml      ClusterIP service, port 8080
  app2-deployment.yaml   Deployment (replicas: 1), same pattern, port 8081
  app2-service.yaml      ClusterIP service
  secrets.yaml           Kubernetes Secret template (ADD_API_KEY via envFrom)
```

The `ADD_API_KEY` is mounted from a `Secret` object — never stored in the Git repository.

### 3.3 GitLab CI/CD Pipeline (`.gitlab-ci.yml`)

```
stages:
  - build        # mvn verify (compile + unit tests)
  - docker       # docker build & push to registry
  - deploy       # kubectl apply -f k8s/ (GitOps: auto-deploy on main)

Rules:
  - build: runs on every branch
  - docker: runs on main & tags
  - deploy: runs on main (after docker succeeds)
```

Branch strategy: `main` → production. Feature branches trigger only `build`.

---

## 4. Testing Approach

### Unit Tests (JUnit 5 + Mockito)

| App | Class under test | What is tested |
|---|---|---|
| App #1 | `StockStreamService` | SSE event parsing, ticker filter logic |
| App #1 | `DashboardController` | `/dashboard/stream` returns `text/event-stream` content type |
| App #2 | `StockHistoryService` | Polling loop, time-window filtering, deduplication by `seq` |
| App #2 | `HistoryController` | `/history/fetch` request validation, `/history/export` CSV format |

Mocking strategy: `WebClient` is mocked via `MockWebServer` (OkHttp) to avoid live API calls in unit tests.

### Integration Tests (Spring Boot Test)

- `@SpringBootTest` with `WebTestClient` for App #1 SSE endpoint.
- `@SpringBootTest` with `MockMvc` for App #2 REST endpoints.
- Both use `application-test.properties` pointing to a `MockWebServer` instance that replays canned tick payloads.

### Manual / Smoke Tests

- Run each app locally with a real `ADD_API_KEY`, verify live data appears.
- Take screenshots for the deliverable `screenshots/` folder.

---

## 5. Task Breakdown

### Phase 2 Implementation Steps

| # | Task | Deliverable |
|---|---|---|
| 1 | Repository scaffold | `.gitignore`, root `README.md`, folder structure |
| 2 | App #1 — Spring Boot project setup | `pom.xml`, `DashboardApplication.java`, `application.properties` |
| 3 | App #1 — `WebClientConfig` + `StockStreamService` | SSE proxy to upstream API |
| 4 | App #1 — `DashboardController` | `/dashboard/stream`, `/dashboard/tickers` endpoints |
| 5 | App #1 — Frontend (`index.html`) | Ticker selector, live table, Chart.js chart |
| 6 | App #1 — Unit tests | `DashboardControllerTest` |
| 7 | App #1 — `Dockerfile` + `README.md` | Container build instructions |
| 8 | App #2 — Spring Boot project setup | `pom.xml`, `HistoryApplication.java`, `StockTick` model |
| 9 | App #2 — `StockHistoryService` | Polling logic, time-window filter, CSV/JSON serialisation |
| 10 | App #2 — `HistoryController` | `/history/fetch`, `/history/export` endpoints |
| 11 | App #2 — Frontend (`index.html`) | Ticker + time-range selector, table/chart, download buttons |
| 12 | App #2 — Unit tests | `HistoryServiceTest` |
| 13 | App #2 — `Dockerfile` + `README.md` | Container build instructions |
| 14 | Kubernetes manifests | `k8s/*.yaml` |
| 15 | GitLab CI/CD pipeline | `.gitlab-ci.yml` |
| 16 | Git hygiene | ≥3 meaningful commits, verify `.gitignore` excludes `.env` |

---

## 6. Key Decisions & Rationale

| Decision | Choice | Reason |
|---|---|---|
| Language | Java 21 + Spring Boot 3 | Mandated by DB Systel CX tech stack |
| Reactive HTTP | Spring WebFlux + `WebClient` | Non-blocking SSE consumption; no Kafka client needed |
| Frontend | Plain JS + Thymeleaf | Zero build-tool overhead; serves from Spring Boot |
| Charting | Chart.js (CDN) | Lightweight, no npm required |
| Data retention | No local DB | Upstream retains only ~10 min; polling window designed accordingly |
| API key | `ADD_API_KEY` env var | Never hardcoded; loaded from `.env` locally, K8s Secret in cluster |

---

**Awaiting your approval to proceed to Phase 2 (implementation).**
