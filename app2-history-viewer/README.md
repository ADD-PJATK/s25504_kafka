# App 2 — Stock History Viewer

A Spring Boot 3 application that polls the instructor's `/api/latest` endpoint every 10 seconds, accumulates ticks in memory, and lets you view, chart, and export the recent history.

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 21+ |
| Maven | 3.9+ |
| Docker | 24+ (optional) |

## Installation

```bash
git clone <repo-url>
cd app2-history-viewer
```

## Configuration

The API key is read from the `ADD_API_KEY` variable — it is **never** hardcoded.

Create a `.env` file at the **project root** (one level up from this folder):

```bash
# from the project root
cp .env.example .env
# Edit .env and set your key:
#   ADD_API_KEY=<your-key-from-add.piotrkojalowicz.dev>
```

No `export` or sourcing is needed. Spring Boot loads the file automatically via `spring.config.import`.

### Changing tracked tickers

By default the app polls a set of 16 tickers defined in `application.properties`:

```properties
history.tracked-tickers=ACME,ALFA,BETA,CASH,CLOUD,DATA,DEVS,ECO,HEAL,NET,NOVA,FUEL,GAME,GRIN,INSR,JET
```

Override at runtime:

```bash
export HISTORY_TRACKED_TICKERS=ACME,ALFA,BETA
mvn spring-boot:run
```

## How to Run

### Local (Maven)

```bash
mvn spring-boot:run
```

Open **http://localhost:8081** in your browser.

> **Note:** The background poller starts immediately. Wait ~10–30 seconds for the first ticks to accumulate before fetching data.

### Docker

```bash
docker build -t s25504-history .
docker run -p 8081:8081 -e ADD_API_KEY=<your-key> s25504-history
```

## Endpoints Used

| Method | Upstream Endpoint | Purpose |
|--------|-------------------|---------|
| GET | `/api/latest?ticker=X&ticker=Y&…` | Polled every 10 s to accumulate ticks in memory |

## Internal REST API

| Method | Path | Description |
|--------|------|-------------|
| GET | `/history/tickers` | Returns the list of currently tracked tickers |
| GET | `/history/data?tickers=X&minutes=5` | Returns accumulated ticks for the last N minutes |
| GET | `/history/export?tickers=X&minutes=5&format=csv` | Downloads data as CSV or JSON |

## How It Works

1. On startup a `@Scheduled` task runs every 10 s and calls `/api/latest` for all tracked tickers in a single batched request.
2. Ticks are stored in a thread-safe in-memory `Deque` per ticker (up to 120 entries ≈ 20 minutes of data).
3. Duplicate ticks (same `seq` number) are discarded automatically.
4. The user opens the browser, selects tickers and a time range (1–10 min), and clicks **Fetch Data**.
5. The frontend calls `/history/data` which filters the in-memory buffer by timestamp.
6. A Chart.js line chart and a sortable table display the results.
7. **Download CSV** / **Download JSON** buttons call `/history/export` and trigger a file download.

## Running Tests

```bash
mvn test
```

Tests use Mockito to mock `RestClient` and inject ticks directly into the service — no live API calls.
