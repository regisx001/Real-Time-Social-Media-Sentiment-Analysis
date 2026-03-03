# Project Context: Real-Time Social Media Sentiment Analysis

## Table of Contents
1. [Project Overview](#1-project-overview)
2. [Architecture](#2-architecture)
3. [Technology Stack](#3-technology-stack)
4. [Repository Structure](#4-repository-structure)
5. [Data Pipeline](#5-data-pipeline)
6. [Backend (Spring Boot)](#6-backend-spring-boot)
7. [Frontend (SvelteKit)](#7-frontend-sveltekit)
8. [Spark Jobs](#8-spark-jobs)
9. [Infrastructure (Docker)](#9-infrastructure-docker)
10. [API Reference](#10-api-reference)
11. [Configuration & Environment](#11-configuration--environment)
12. [Scripts](#12-scripts)
13. [Known Issues & Gotchas](#13-known-issues--gotchas)
14. [Development Workflow](#14-development-workflow)

---

## 1. Project Overview

A full-stack real-time sentiment analysis platform that ingests tweets via a REST API, streams them through Apache Kafka, classifies them with a trained Apache Spark ML model (Logistic Regression), persists results in PostgreSQL, and displays live analytics on a SvelteKit dashboard.

**Core capabilities:**
- Ingest tweets via REST → Kafka → Spark Structured Streaming → PostgreSQL
- Live dashboard with real-time sentiment metrics via Server-Sent Events (SSE)
- Sentiment-over-time chart with 7 configurable time ranges (10 min → 30 days)
- Paginated tweet history table
- Health monitoring dashboard for all services
- Dark/light mode toggle

---

## 2. Architecture

```
┌─────────────┐   POST /api/tweets    ┌──────────────────────┐
│  REST Client │ ─────────────────────▶│  Spring Boot Backend │
│ (scripts /  │                        │  (port 8090)         │
│  Frontend)  │                        └──────────┬───────────┘
└─────────────┘                                   │
                                        TweetProducer
                                                   │ Kafka topic: tweets.raw
                                                   ▼
                                        ┌──────────────────┐
                                        │  Apache Kafka    │
                                        │  (KRaft, port    │
                                        │   9092)          │
                                        └──────┬───────────┘
                                               │ Spark reads tweets.raw
                                               ▼
                                        ┌──────────────────┐
                                        │  Spark Streaming │
                                        │  (PySpark ML     │
                                        │   LogisticReg.)  │
                                        └──────┬───────────┘
                                               │ Kafka topic: tweets.processed
                                               ▼
                                        ┌──────────────────────┐
                                        │  Spring Boot Backend │
                                        │  TweetConsumer       │
                                        │  (KafkaListener)     │
                                        └──────────┬───────────┘
                                                   │ saves to DB
                                                   ▼
                                        ┌──────────────────┐
                                        │  PostgreSQL 16   │
                                        │  (port 5435)     │
                                        │  table: raw_tweets│
                                        └──────────────────┘
                                                   │ SSE every 5s
                                                   ▼
                                        ┌──────────────────────┐
                                        │  SvelteKit Frontend  │
                                        │  (port 5173)         │
                                        └──────────────────────┘
```

---

## 3. Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Backend | Spring Boot | 4.0.3 |
| Backend | Spring Framework | 7.x |
| Backend | Java | 21 |
| Backend | Spring Data JPA + Hibernate 6 | — |
| Backend | Spring WebFlux (SSE) | — |
| Backend | Spring Kafka | — |
| Backend | Spring Actuator | — |
| Backend | Lombok | — |
| Backend | Jackson Databind | — |
| Backend | dotenv-java | 3.0.0 |
| Database | PostgreSQL | 16 |
| Message Broker | Apache Kafka | latest (KRaft mode) |
| ML / Streaming | Apache Spark | 3.5.1 |
| ML / Streaming | PySpark | 3.5.1 |
| Frontend | SvelteKit | latest |
| Frontend | Svelte | 5 (runes mode) |
| Frontend | TypeScript | — |
| Frontend | Tailwind CSS | v4 |
| Frontend | Shadcn-Svelte | — |
| Frontend | LayerChart / D3 | — |
| Frontend | mode-watcher | — |
| Infrastructure | Docker Compose | — |

---

## 4. Repository Structure

```
Real-Time-Social-Media-Sentiment-Analysis/
├── .env                          # Local environment variables (gitignored)
├── .env.example                  # Env variable template
├── docker-compose.yml            # All services: db, broker, spark-*
├── pom.xml                       # Spring Boot Maven config
├── ROADMAP.md                    # Feature roadmap
├── CONTEXT.md                    # This file
│
├── data/
│   └── twitter/
│       ├── twitter_training.csv  # Training dataset (75K rows)
│       └── twitter_validation.csv
│
├── scripts/
│   ├── kafka/                    # Kafka topic management scripts
│   ├── postgres/
│   │   ├── open-psql-console.sh
│   │   └── reset-db.sh
│   └── test/
│       ├── send_test_tweets.sh   # Send 30 sample tweets (one-shot)
│       ├── continuous_tweets.sh  # Send tweets continuously for 10 min
│       └── test_throughput.sh    # Throughput benchmarking tool
│
├── spark/
│   ├── training/
│   │   ├── DOCKERFILE
│   │   └── run_training_spark.py  # Trains Logistic Regression model
│   └── streaming/
│       ├── DOCKERFILE
│       └── run_streaming_spark.py # Structured Streaming inference job
│
├── src/
│   └── main/
│       ├── java/com/regisx001/core/
│       │   ├── CoreApplication.java
│       │   ├── config/
│       │   │   ├── CorsConfig.java
│       │   │   ├── KafkaAdminConfig.java
│       │   │   ├── KafkaConsumerConfig.java
│       │   │   ├── KafkaProducerConfig.java
│       │   │   └── KafkaTopicsConfig.java
│       │   ├── controllers/
│       │   │   ├── AnalyticsController.java  # SSE + REST analytics
│       │   │   ├── HealthController.java     # Service health SSE + REST
│       │   │   ├── KafkaController.java      # Direct Kafka publish
│       │   │   └── TweetController.java      # Tweet ingestion REST API
│       │   ├── domain/
│       │   │   ├── entities/
│       │   │   │   └── Tweet.java            # JPA entity (raw_tweets table)
│       │   │   └── dto/
│       │   │       ├── AnalyticsReport.java
│       │   │       ├── AnalyticsSummary.java
│       │   │       ├── DetailedHealthReport.java
│       │   │       ├── HealthReport.java
│       │   │       ├── KafkaMetrics.java
│       │   │       ├── LiveTweetDto.java
│       │   │       ├── PostgresMetrics.java
│       │   │       ├── ProcessedTweetEvent.java
│       │   │       ├── SentimentTimePoint.java
│       │   │       ├── ServiceHealth.java
│       │   │       ├── SparkMetrics.java
│       │   │       └── TweetEvent.java
│       │   ├── repository/
│       │   │   └── TweetRepository.java      # JPA repository + native queries
│       │   └── services/
│       │       ├── AnalyticsService.java     # Analytics aggregation logic
│       │       ├── HealthCheckService.java   # Service health probing
│       │       ├── TweetConsumer.java        # Kafka consumer (tweets.processed)
│       │       ├── TweetProducer.java        # Kafka producer (tweets.raw)
│       │       └── TweetService.java         # Tweet creation + persistence
│       └── resources/
│           └── application.yaml
│
└── web/                          # SvelteKit frontend
    └── src/
        ├── app.html
        ├── app.d.ts
        ├── lib/
        │   ├── components/
        │   │   ├── chart-area-sentiment.svelte    # Time-series area chart
        │   │   ├── sentiment-section-cards.svelte # KPI metric cards
        │   │   ├── sentiment-data-table.svelte    # Paginated tweet table
        │   │   ├── sentiment-site-header.svelte   # Header + dark mode toggle
        │   │   ├── sentiment-app-sidebar.svelte   # Nav sidebar
        │   │   └── ui/                            # Shadcn-Svelte components
        │   └── stores/
        │       ├── analytics.ts   # SSE + report store
        │       ├── liveFeed.ts    # Live tweet feed SSE store
        │       └── health.ts      # Health check SSE store
        └── routes/
            ├── +page.ts                    # Redirects / → /dashboard
            ├── +layout.svelte
            ├── (dashboard)/
            │   ├── +layout.svelte          # Sidebar layout wrapper
            │   └── dashboard/
            │       └── +page.svelte        # Main dashboard page
            └── health/                     # Health monitoring page
```

---

## 5. Data Pipeline

### Step-by-step flow

1. **Ingestion**: Client sends `POST /api/tweets` with `{ text, source }`.
2. **Storage**: `TweetService` creates a `Tweet` entity with `ingestedAt = now()`, `processedData = null`, saves to PostgreSQL.
3. **Kafka produce**: `TweetProducer` publishes a `TweetEvent` (`tweetId`, `text`, `timestamp`) to topic `tweets.raw`.
4. **Spark Streaming** reads from `tweets.raw`:
   - Parses JSON schema: `tweetId`, `text`, `timestamp`
   - Cleans text (lowercase, strip URLs/mentions/special chars)
   - Runs the loaded PipelineModel (Tokenizer → StopWordsRemover → CountVectorizer → IDF → LogisticRegression)
   - Maps prediction index back to label: `0→Negative`, `1→Neutral`, `2→Positive`
   - Publishes result as `ProcessedTweetEvent` to topic `tweets.processed`
5. **Kafka consume**: `TweetConsumer` listens to `tweets.processed`, finds the Tweet by ID, updates `processedData = { sentiment, score }` and `processedAt = now()`.
6. **Frontend SSE**: `AnalyticsController` streams aggregated reports every 5 seconds.

### Kafka Topics

| Topic | Producer | Consumer | Schema |
|-------|----------|----------|--------|
| `tweets.raw` | `TweetProducer` (Spring) | Spark Streaming | `TweetEvent` record |
| `tweets.processed` | Spark Streaming | `TweetConsumer` (Spring) | `ProcessedTweetEvent` record |

### Database Schema

Table: `raw_tweets`

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGSERIAL PK | Auto-generated |
| `raw_data` | JSONB | Original tweet payload |
| `processed_data` | JSONB | `{ sentiment, score }` from Spark |
| `ingested_at` | TIMESTAMP | Set on creation |
| `processed_at` | TIMESTAMP | Set after Spark processes it |

---

## 6. Backend (Spring Boot)

**Entry point:** `CoreApplication.java`  
**Port:** `8090`

### Controllers

#### `TweetController` — `POST /api/tweets`
Accepts `{ text, source }`, calls `TweetService.createTweet()`, returns the saved `Tweet`.

#### `KafkaController` — `POST /api/kafka/send`
Direct Kafka publish endpoint accepting a `TweetEvent` JSON body.

#### `AnalyticsController` — `/api/analytics/*`

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/analytics/stream` | SSE | Streams `AnalyticsReport` every 5 s |
| `/api/analytics/report` | GET | One-shot analytics snapshot |
| `/api/analytics/live-feed/stream` | SSE | Streams last 10 processed tweets every 5 s |
| `/api/analytics/tweets` | GET | Paginated tweet list (newest first) |

**Query parameters for stream/report:**
- `bucket` — `minute`, `hour`, or `day` (time series granularity)
- `minutes` — lookback window in minutes (takes precedence when `>= 0`)
- `hours` — fallback lookback in hours (default: `60`, used when `minutes = -1`)

**Effective lookback logic:**
```java
int effectiveMinutes = minutes >= 0 ? minutes : hours * 60;
```

#### `HealthController` — `/api/health/*`

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/health` | GET | Simple health snapshot |
| `/api/health/stream` | SSE | Simple health every 3 s |
| `/api/health/details/stream` | SSE | Detailed metrics (Postgres, Kafka, Spark) every 3 s |

### Services

#### `AnalyticsService`
- `getReport(bucket, lookbackMinutes)` → `AnalyticsReport`
- `buildSummary()` — counts by sentiment + 60 s rolling throughput
- `buildTimeSeries(bucket, lookbackMinutes)` — pivots native query rows into `SentimentTimePoint` list
  - **Hibernate 6 fix**: native query returns `LocalDateTime` (not `java.sql.Timestamp`); uses `instanceof` check

#### `TweetConsumer`
- `@KafkaListener(topics = "tweets.processed")`
- Maps Spark sentiment labels: `POSITIVE`, `NEGATIVE`, `NEUTRAL`, `UNKNOWN`
- Updates `processedData` and `processedAt` on the existing Tweet entity

### Important Spring Boot / Framework Notes

- **Spring Framework 7 + devtools classloader incompatibility**: `@RequiredArgsConstructor` (Lombok) fails at runtime with `BeanCreationException`. Use explicit constructors in `@RestController` / `@Service` classes that use devtools.
- **Hibernate 6 native queries**: `date_trunc()` result is returned as `LocalDateTime` (not `Timestamp`). The `buildTimeSeries` method handles both with an `instanceof` check.
- **WebFlux for SSE**: The project uses `spring-boot-starter-webflux` alongside `spring-boot-starter-webmvc`. SSE endpoints return `Flux<ServerSentEvent<T>>`.

---

## 7. Frontend (SvelteKit)

**Port:** `5173` (dev), `4173` (preview)  
**Mode:** Svelte 5 runes (`$state`, `$derived`, `$props`, `$effect`)

### Routing

| Route | Component | Description |
|-------|-----------|-------------|
| `/` | `+page.ts` | Redirects to `/dashboard` |
| `/dashboard` | `dashboard/+page.svelte` | Main analytics dashboard |
| `/health` | health page | Service health monitoring |

### Key Components

#### `sentiment-section-cards.svelte`
- Props: `summary: AnalyticsSummary | null | undefined`
- Displays: Total Processed, Processing Rate (msg/s), Positive %, Negative %
- Shows `—` when `summary` is null

#### `chart-area-sentiment.svelte`
- Props: `timeSeries: SentimentTimePoint[]`
- Time range selector: 7 options (`10m`, `30m`, `1h`, `12h`, `24h`, `7d`, `30d`)
- Calls `reconnectAnalytics(bucket, minutes)` on range change
- Uses LayerChart `AreaChart` with D3 `scaleUtc` and `curveNatural`
- Empty state: "Waiting for data…" shown when `timeSeries` is empty

#### `sentiment-data-table.svelte`
- Fetches `GET /api/analytics/tweets?page=0&size=20` on mount
- Paginated with Prev/Next buttons
- Maps `rawData.text`, `processedData.sentiment`, `processedData.score`, `processedAt`
- Shows skeleton while loading, empty state when no data

#### `sentiment-site-header.svelte`
- Shows live connection status (animated dot: green/amber/red)
- Dark/light mode toggle button using `toggleMode` from `mode-watcher`

### Stores

#### `analytics.ts`
- `analyticsReport` — derived store exposing latest `AnalyticsReport`
- `analyticsConnectionState` — `'connecting' | 'connected' | 'error' | 'closed'`
- `analyticsLastUpdated` — `Date | null`
- `reconnectAnalytics(bucket, minutes)` — exported function to change SSE params
- Auto-reconnects with exponential backoff (3 s → 30 s max)

#### `liveFeed.ts`
- `liveFeed` — derived store of last 10 `LiveTweet[]`
- Connects to `/api/analytics/live-feed/stream`

#### `health.ts`
- `connectionState` — overall connection state
- Stores `DetailedHealthReport` with Postgres, Kafka, Spark metrics

### Environment Variable

```
VITE_API_BASE_URL=http://localhost:8090
```
Falls back to `http://localhost:8090` if not set.

---

## 8. Spark Jobs

### Training Job (`spark/training/run_training_spark.py`)

Runs **once** at container startup. Reads CSV data from the shared Docker volume, trains a sentiment classifier, and saves the model.

**Pipeline:**
1. Load `twitter_training.csv` (columns: id, entity, sentiment, text)
2. Normalize: map `Irrelevant` → `Neutral`
3. Clean text: lowercase, strip URLs, @mentions, non-alpha chars, extra spaces
4. ML Pipeline: `Tokenizer` → `StopWordsRemover` → `CountVectorizer` → `IDF` → `StringIndexer` → `LogisticRegression` → `IndexToString`
5. Save model to `/opt/spark/work-dir/data/models/spark_sentiment_model`

**Label mapping:** `Negative=0`, `Neutral=1`, `Positive=2`

### Streaming Job (`spark/streaming/run_streaming_spark.py`)

Runs **continuously**. Loads the trained model and classifies incoming tweets in real-time.

**Flow:**
1. Load model from `/opt/spark/work-dir/data/spark_sentiment_model`
2. Read from Kafka `tweets.raw` (startingOffsets: latest)
3. Parse JSON schema: `tweetId`, `text`, `timestamp`
4. Apply same text cleaning as training
5. Run inference with loaded `PipelineModel`
6. Map prediction index → label string (`POSITIVE`, `NEGATIVE`, `NEUTRAL`)
7. Write result JSON to Kafka `tweets.processed`

---

## 9. Infrastructure (Docker)

### Services

| Service | Image | Port | Role |
|---------|-------|------|------|
| `db` | `postgres:16` | `5435:5432` | PostgreSQL database |
| `broker` | `apache/kafka:latest` | `9092:29092` | Kafka KRaft broker |
| `spark-master` | `apache/spark:3.5.1` | `8080:8080`, `7077:7077` | Spark master |
| `spark-worker` | `apache/spark:3.5.1` | `8081:8081` | Spark worker |
| `spark-training` | (custom build) | — | Runs training job once |
| `spark-streaming` | (custom build) | — | Runs streaming job continuously |

### Network
All services share `realtime-social-media-sentiments-net` (bridge driver).

### Volumes
- `pg_data` — PostgreSQL data persistence
- `./data` mounted into Spark containers at `/opt/spark/work-dir/data`

### ⚠️ Important
Running `docker compose up db -d` (or any command that recreates the `db` container) will **wipe the database** unless the named volume `pg_data` persists. After a DB reset, re-seed using the test scripts.

---

## 10. API Reference

### Tweet Ingestion

```http
POST /api/tweets
Content-Type: application/json

{ "text": "I love this product!", "source": "api" }
```

Response: `Tweet` entity JSON.

### Analytics Stream (SSE)

```
GET /api/analytics/stream?bucket=minute&minutes=10
GET /api/analytics/stream?bucket=hour&minutes=720
GET /api/analytics/stream?bucket=day&minutes=10080
```

Event name: `analytics`. Payload: `AnalyticsReport` JSON.

### Analytics Report (REST)

```
GET /api/analytics/report?bucket=minute&minutes=10
```

### Paginated Tweets

```
GET /api/analytics/tweets?page=0&size=20
```

Response: Spring Data `Page<Tweet>` with `content`, `number`, `totalPages`, `totalElements`.

### Live Feed Stream (SSE)

```
GET /api/analytics/live-feed/stream
```

Event name: `live-feed`. Payload: `LiveTweetDto[]` (last 10 tweets).

### Health

```
GET /api/health               → simple HealthReport
GET /api/health/stream        → SSE simple health (every 3s)
GET /api/health/details/stream → SSE detailed metrics (every 3s)
```

---

## 11. Configuration & Environment

### Backend (`src/main/resources/application.yaml`)

```yaml
spring:
  datasource:
    url: ${POSTGRES_URL}
    username: ${POSTGRES_USERNAME}
    password: ${POSTGRES_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: debug-consumer
server:
  port: 8090
cors:
  allowed-origins: ${CORS_ORIGINS:http://localhost:5173,http://localhost:4173}
health:
  spark:
    master-url: ${SPARK_MASTER_URL:http://localhost:8080}
```

### `.env` (required, not committed)

```env
POSTGRES_URL=jdbc:postgresql://localhost:5435/realtime_social_media_sentiments
POSTGRES_USERNAME=admin
POSTGRES_PASSWORD=adminpassword
```

### Frontend (`web/.env` or `web/.env.local`)

```env
VITE_API_BASE_URL=http://localhost:8090
```

---

## 12. Scripts

### `scripts/test/send_test_tweets.sh`
Sends 30 hardcoded tweets (10 negative, 10 positive, 10 neutral) to `POST /api/tweets`. Use to quickly seed the database after a reset.

```bash
./scripts/test/send_test_tweets.sh
```

### `scripts/test/continuous_tweets.sh`
Sends tweets continuously for ~10 minutes at a moderate rate. Shows elapsed time, total sent, and current rate.

```bash
./scripts/test/continuous_tweets.sh
```

### `scripts/test/test_throughput.sh`
Benchmarking tool with configurable flags:

| Flag | Default | Description |
|------|---------|-------------|
| `-n COUNT` | 100 | Total tweets to send |
| `-c WORKERS` | 5 | Parallel workers |
| `-r RATE` | 10 | Target requests/sec |
| `-u URL` | `http://localhost:8090` | Backend base URL |
| `-w WAIT` | 30 | Seconds to wait for pipeline |

Reports client RPS + pipeline throughput via 3 DB-polled samples.

```bash
./scripts/test/test_throughput.sh -n 200 -c 5
```

### `scripts/postgres/reset-db.sh`
Drops and recreates the database schema.

### `scripts/postgres/open-psql-console.sh`
Opens a psql console connected to the Docker PostgreSQL instance.

---

## 13. Known Issues & Gotchas

### 1. Spring Framework 7 + devtools + Lombok `@RequiredArgsConstructor`
`@RequiredArgsConstructor` on `@RestController` / `@Service` classes causes `BeanCreationException` at startup when Spring Boot devtools is active. The RestartClassLoader and SF7's injection mechanism conflict.  
**Fix:** Use explicit constructors instead of `@RequiredArgsConstructor`.

### 2. Hibernate 6 `date_trunc()` return type
In Hibernate 6 with native queries, `date_trunc()` returns `LocalDateTime` (not `java.sql.Timestamp`). Casting to `Timestamp` throws `ClassCastException`.  
**Fix in `AnalyticsService.buildTimeSeries`:**
```java
if (row[0] instanceof Timestamp ts) {
    ldt = ts.toLocalDateTime();
} else {
    ldt = (LocalDateTime) row[0];
}
```

### 3. Svelte 5 `$derived` syntax
`$derived(() => expression)` returns the arrow function reference, not the computed value.  
**Correct syntax:** `$derived(expression)` or `$derived((() => { ... })())` for multi-line.

### 4. DB wiped after `docker compose up db -d`
Recreating the `db` container without the named volume persisting empties all data.  
**Recovery:** Run `./scripts/test/send_test_tweets.sh` to re-seed.

### 5. Spark model path
The streaming job looks for the model at `/opt/spark/work-dir/data/spark_sentiment_model` (no `models/` subdirectory), while the training job saves to `data/models/spark_sentiment_model`. Verify paths match between the two scripts.

### 6. Kafka `bootstrap-servers` in `application.yaml`
Set to `localhost:9092`. The Kafka container's `EXTERNAL` listener maps `localhost:9092 → container:29092`. This works when the Spring Boot app runs on the host (not inside Docker). If containerized, change to `broker:9092`.

---

## 14. Development Workflow

### Start infrastructure

```bash
# Start DB, Kafka, Spark
sudo docker compose up db broker spark-master spark-worker -d

# Start Spark ML training + streaming (first time or after model changes)
sudo docker compose up spark-training spark-streaming -d
```

### Start backend

```bash
./mvnw spring-boot:run
# Backend available at http://localhost:8090
```

### Start frontend

```bash
cd web
bun install    # or npm install
bun run dev    # or npm run dev
# Frontend available at http://localhost:5173
```

### Seed test data

```bash
./scripts/test/send_test_tweets.sh
```

### Verify pipeline

```bash
# Check all services healthy
curl -s http://localhost:8090/api/health | jq

# Check analytics (should show data after seeding)
curl -s "http://localhost:8090/api/analytics/report?bucket=minute&minutes=60" | jq

# Check tweet count
curl -s "http://localhost:8090/api/analytics/tweets?page=0&size=5" | jq '.totalElements'
```

### Build for production

```bash
# Backend JAR
./mvnw clean package -DskipTests

# Frontend static build
cd web && bun run build
```
