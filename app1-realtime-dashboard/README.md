# App 1 — Realtime Stock Dashboard

A Spring Boot 3 / WebFlux application that proxies the instructor's SSE stock-price feed and renders a live dashboard in the browser.

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 21+ |
| Maven | 3.9+ |
| Docker | 24+ (optional) |

## Installation

```bash
git clone <repo-url>
cd app1-realtime-dashboard
```

## Configuration

The API key is read from the `ADD_API_KEY` environment variable — it is **never** hardcoded.

```bash
cp .env.example .env
# Edit .env and set your key:
#   ADD_API_KEY=<your-key-from-add.piotrkojalowicz.dev>
export $(cat .env | xargs)
```

## How to Run

### Local (Maven)

```bash
mvn spring-boot:run
```

Open **http://localhost:8080** in your browser.

### Docker

```bash
docker build -t s25504-dashboard .
docker run -p 8080:8080 -e ADD_API_KEY=<your-key> s25504-dashboard
```

## Endpoints Used

| Method | Upstream Endpoint | Purpose |
|--------|-------------------|---------|
| GET | `/api/tickers` | Fetch full list of 50 companies to populate the ticker selector |
| GET | `/api/stream?ticker=X` | Live SSE stream (~1 tick/s per ticker), proxied to the browser |

## How It Works

1. On page load the app fetches `/api/tickers` and renders a searchable checkbox list.
2. The user selects one or more tickers and clicks **Connect**.
3. The browser opens an `EventSource` to `/dashboard/stream?tickers=ACME&tickers=ALFA`.
4. The Spring Boot backend opens one reactive `WebClient` SSE subscription per ticker and merges all streams into a single `text/event-stream` response.
5. Each `tick` event updates the live price table and the Chart.js line chart (last 30 ticks per ticker).
6. If the connection drops, `EventSource` reconnects automatically; the backend retries upstream with `retry(5)`.

## Running Tests

```bash
mvn test
```

Tests use `@WebFluxTest` with a mocked `StockStreamService` — no live API calls are made.
