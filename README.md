# s25504_kafka — Real-time Stock Data (Kafka / SSE)

Two Spring Boot 3 microservices that consume a live stock-price SSE feed from `add.piotrkojalowicz.dev`.

| App | Folder | Port | Purpose |
|-----|--------|------|---------|
| Realtime Dashboard | `app1-realtime-dashboard/` | 8080 | Live price streaming via SSE + Chart.js |
| History Viewer | `app2-history-viewer/` | 8081 | Background polling + history export (CSV/JSON) |

## Prerequisites

- Java 21+
- Maven 3.9+
- An API key from `https://add.piotrkojalowicz.dev` (class password: `A@d-$01`)

## Quick Start

```bash
# 1. Create the API key file at the project root (one-time setup)
cp .env.example .env
# Edit .env and replace "your_api_key_here" with your real key.
# No `export` or sourcing is required — Spring Boot reads it automatically.

# 2. Start App 1 (terminal 1)
cd app1-realtime-dashboard
mvn spring-boot:run

# 3. Start App 2 (terminal 2)
cd app2-history-viewer
mvn spring-boot:run
```

Open **http://localhost:8080** for the Realtime Dashboard.  
Open **http://localhost:8081** for the History Viewer.

## Running with Docker

```bash
# App 1
cd app1-realtime-dashboard
docker build -t s25504-dashboard .
docker run -p 8080:8080 -e ADD_API_KEY=<your-key> s25504-dashboard

# App 2
cd app2-history-viewer
docker build -t s25504-history .
docker run -p 8081:8081 -e ADD_API_KEY=<your-key> s25504-history
```

## Full docs

- [App 1 README](app1-realtime-dashboard/README.md)
- [App 2 README](app2-history-viewer/README.md)
