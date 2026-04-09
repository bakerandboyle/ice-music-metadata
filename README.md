# ICE Music Metadata Service

A production-ready music metadata service built to SOLID principles with a hexagonal architecture, designed for horizontal scalability and operational transparency.

**Author:** Steve Baker  
**Stack:** Java 25 LTS · Spring Boot 4.0.4 · Spring Framework 7.0.6 · PostgreSQL · Redis

---

## Table of Contents

- [Quick Start](#quick-start)
- [API Contract](#api-contract)
- [Architecture](#architecture)
- [Design Decisions](#design-decisions)
- [Data Model](#data-model)
- [Running the Tests](#running-the-tests)
- [Production Deployment Notes](#production-deployment-notes)

---

## Quick Start

**Prerequisites:** Java 25, Docker & Docker Compose (for PostgreSQL and Redis)

```bash
git clone <repo-url>
cd ice-music-metadata
cp .env.example .env          # Create local secrets (gitignored)
docker compose up -d
./mvnw spring-boot:run
```

The service starts on `http://localhost:8080`. Docker Compose provisions PostgreSQL and Redis — this is the real stack, not an in-memory substitute. Credentials live in `.env` (never committed), not in application config.

---

## API Contract

All endpoints return [RFC 7807 Problem Details](https://www.rfc-editor.org/rfc/rfc7807) on error.

### Versioning

API versioning uses the `X-ICE-Version` header with a date-based scheme. This avoids the friction of URL path versioning (`/v1/`, `/v2/`) which pollutes resource URIs with non-resource concerns.

| Header | Behaviour |
|--------|-----------|
| `X-ICE-Version: 2026-04-09` | Current version (default if header omitted) |
| `X-ICE-Version: 2024-01-01` | Legacy version (future: when breaking changes are introduced) |
| *(omitted)* | Falls through to current version |

Clients opt in to a specific contract date. New versions are introduced only on breaking changes; non-breaking additions are always backwards-compatible within a version.

### Tracks

| Method | Path                          | Description                        | Success | Key Headers              |
|--------|-------------------------------|------------------------------------|---------|--------------------------|
| POST   | `/api/artists/{id}/tracks`    | Add a new track to an artist       | 201     | `X-Idempotency-Key`      |
| GET    | `/api/artists/{id}/tracks`    | Fetch all tracks for an artist     | 200     |                          |

#### POST `/api/artists/{id}/tracks`

**Request Body:**
```json
{
  "title": "Bohemian Rhapsody",
  "isrc": "GBAYE7500101",
  "genre": "Rock",
  "durationSeconds": 354
}
```

**Response (201 Created):**
```json
{
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "artistId": "a1b2c3d4-...",
  "title": "Bohemian Rhapsody",
  "isrc": "GBAYE7500101",
  "genre": "Rock",
  "durationSeconds": 354,
  "createdAt": "2026-04-09T10:30:00Z"
}
```

**ISRC (International Standard Recording Code):** The industry-standard 12-character unique identifier for a recording (ISO 3901). Used as the natural key constraint — this allows an artist to have multiple tracks with the same title (e.g. studio, live, remastered) as long as each is a distinct recording with its own ISRC.

**Idempotency:** Supplying an `X-Idempotency-Key` header prevents duplicate track creation from network retries. The same key returns the original response without side effects.

#### GET `/api/artists/{id}/tracks`

**Response (200 OK):**
```json
[
  {
    "id": "f47ac10b-...",
    "artistId": "a1b2c3d4-...",
    "title": "Bohemian Rhapsody",
    "isrc": "GBAYE7500101",
    "genre": "Rock",
    "durationSeconds": 354,
    "createdAt": "2026-04-09T10:30:00Z"
  }
]
```

### Artists

| Method | Path                           | Description                        | Success |
|--------|--------------------------------|------------------------------------|---------|
| PATCH  | `/api/artists/{id}/name`       | Rename an artist (canonical name)  | 200     |
| POST   | `/api/artists/{id}/aliases`    | Add an alias to an artist          | 201     |
| GET    | `/api/artists?name={name}`     | Look up artist by name or alias    | 200     |

#### PATCH `/api/artists/{id}/name`

**Request Body:**
```json
{
  "name": "Freddie Mercury"
}
```

**Response (200 OK):** Returns the updated artist resource. Uses optimistic locking - returns `409 Conflict` if a concurrent modification is detected.

#### POST `/api/artists/{id}/aliases`

**Request Body:**
```json
{
  "alias": "Farrokh Bulsara"
}
```

**Response (201 Created):** Returns the created alias resource.

#### GET `/api/artists?name={name}`

Searches by canonical name **and** aliases. Returns matching artist(s).

### Artist of the Day

| Method | Path                       | Description                        | Success |
|--------|----------------------------|------------------------------------|---------|
| GET    | `/api/artist-of-the-day`   | Today's featured artist            | 200     |

**Response (200 OK):**
```json
{
  "artist": {
    "id": "a1b2c3d4-...",
    "name": "Queen"
  },
  "date": "2026-04-09"
}
```

### Operations

| Method | Path                 | Description              |
|--------|----------------------|--------------------------|
| GET    | `/actuator/health`   | Liveness / readiness     |
| GET    | `/actuator/metrics`  | Prometheus metrics       |
| GET    | `/actuator/info`     | Build and git metadata   |

---

## Architecture

### Hexagonal (Ports & Adapters)

```
┌─────────────────────────────────────────────────────────┐
│                     ADAPTERS (IN)                       │
│  REST Controllers · Request/Response DTOs               │
├─────────────────────────────────────────────────────────┤
│                     PORTS (IN)                          │
│  AddTrackUseCase · EditArtistNameUseCase                │
│  FetchArtistTracksUseCase · ArtistOfTheDayUseCase       │
├─────────────────────────────────────────────────────────┤
│                     DOMAIN                              │
│  Artist · Track · ArtistAlias · AuditEvent              │
│  ArtistOfTheDayService                                  │
│  Pure Java records. No framework annotations.           │
├─────────────────────────────────────────────────────────┤
│                     PORTS (OUT)                         │
│  ArtistRepository · TrackRepository                     │
│  CachePort · AuditPublisher                             │
├─────────────────────────────────────────────────────────┤
│                     ADAPTERS (OUT)                      │
│  JPA Entities · Spring Data Repos · Redis               │
│  SnsAuditPublisher (logs → SNS in production)           │
└─────────────────────────────────────────────────────────┘
```

**The Sanctuary Rule:** The `domain` package contains zero imports from Spring, Hibernate, or Jakarta. Domain objects are pure Java records. All mapping between JPA entities and domain records happens at the adapter boundary.

**Why:** If the persistence layer changes (Postgres → NoSQL, JPA → jOOQ), the domain and use case logic requires zero modification.

---

## Design Decisions

### 1. Java 25 LTS on Spring Boot 4.0.4

| Factor | Reasoning |
|--------|-----------|
| **LTS lifecycle** | Java 25 (Sept 2025) is the current Long Term Support release. Feature releases (24, 26) carry only six months of support. An enterprise streaming platform requires multi-year JDK stability. |
| **Virtual thread pinning solved** | Java 24 introduced the fundamental fix: `synchronized` blocks no longer pin the carrier thread. Java 25 is the first LTS to inherit this. This is the first LTS where Virtual Threads + Hibernate is safe at scale without manual workarounds. |
| **Project Loom maturity** | Java 25 ships the most stable, finalised versions of Structured Concurrency and Scoped Values — the full Loom suite, not just virtual threads as a buzzword. |
| **Spring Boot 4.0.4** | Current GA on Spring Framework 7.0.6. Compatible with Java 17–26. GraalVM native image support included. |

**Configuration:**

```properties
spring.threads.virtual.enabled=true
```

**JVM flags (production):**

```
-XX:+UseZGC -XX:+ZGenerational
```

ZGC keeps p99 GC pause times under 1ms, establishing a consistent latency floor even under heavy load.

### 2. Artist of the Day — The Rank-Pointer Pattern

**The Problem:** Artist IDs are UUIDs — non-sequential. A naive `epochDay % n` selecting the "n-th row" requires a table scan. SQL `OFFSET` on a large table without sequential keys degrades to O(N).

**Algorithm:** `targetIndex = (epochDay % totalArtistCount)`

**The Rank-Pointer Query:**

```sql
SELECT id FROM artist
ORDER BY created_at ASC, id ASC
LIMIT 1 OFFSET :targetIndex;
```

A **covering index** on `(created_at, id)` allows the database to satisfy this entirely from the index without touching heap pages. For the POC catalogue size, this is performant and correct.

**Why not random?** The specification requires cyclical rotation with fair exposure. Deterministic modular arithmetic guarantees every artist is featured exactly once per cycle of *n* days, with zero state beyond the artist count.

**Edge Cases — Rotation Robustness:**

| Event | Behaviour | Fairness Impact |
|-------|-----------|-----------------|
| **Deletion** | Modulo naturally shifts all subsequent positions up. | Fair over the long term — the cycle is based on current catalogue state. |
| **Insertion** | New artists append to the end of the sort order (by `created_at`). | Existing rotation is undisrupted until the next cycle begins. |
| **Empty catalogue** | `totalArtistCount == 0` → return `404` with Problem Details. | No division by zero. |

**Scaling Path — The Materialised Rank Table:**

If the catalogue reaches hundreds of millions of records, even a covering-index `OFFSET` hits a wall. The production-ready evolution:

```
┌──────────────────┐
│  artist_rank     │
│──────────────────│
│  artist_id (FK)  │
│  rank (PK, int)  │  ← Sequential 1, 2, 3...
└──────────────────┘
```

A nightly job (out of scope for POC) re-indexes the `rank` column. The AOD query becomes a primary key lookup: `SELECT artist_id FROM artist_rank WHERE rank = :targetIndex`. Complexity drops from O(N) to O(1).

**Artist Count — Redis Counter, Not `COUNT(*)`:**

`COUNT(*)` performs a table scan — unacceptable at streaming platform scale. Instead, the artist count is maintained as an atomic Redis counter:

```
Artist created → INCR artist:count     (O(1), atomic)
AOD compute   → GET artist:count       (O(1), no DB)
Cold start    → seed from DB COUNT(*)  (one-time only)
```

The counter is incremented on every artist create. The AOD engine reads from Redis, never from the database. On a Redis cold start (e.g. after a flush or failover), the count is seeded once from `repository.count()` and cached with no TTL — this is the only time the table scan runs.

**Single-Flight Cache with Redis SETNX + Pub/Sub:**

```
1. GET cache:aod → HIT? Return immediately.
2. SETNX lock:aod → Won the lock?
   YES → Compute AOD → SET cache:aod → PUBLISH aod:notifications "ready" → DELETE lock.
   NO  → SUBSCRIBE aod:notifications → CompletableFuture.get(2s) → GET cache:aod.
```

No polling, no `Thread.sleep`. Lock losers subscribe to a Redis pub/sub channel and complete as soon as the winner publishes. The 2-second timeout is a safety valve — on expiry, the node falls back to direct computation. The lock is explicitly deleted in a `finally` block; the 30-second TTL is a secondary safety valve if the winner crashes.

**Temporal Locality — The "Fragmentation of Truth" Problem:**

When does "today" flip? There are 26+ functional time offsets globally. If the AOD is timezone-aware, there is no single "Artist of the Day" — there are 24+ simultaneous versions of the truth.

*The conflict:* A user in London shares the "Artist of the Day" link with a friend in New York. If the AOD is zoned, the friend sees a different artist. This breaks the social consistency of the platform — the feature becomes unshareable.

**POC Decision: Global UTC Anchor.**

The AOD flips at midnight UTC for all users globally. This gives us:

| Factor | UTC Anchor | Zoned Transitions |
|--------|-----------|-------------------|
| Cache efficiency | 100% hit rate — one key, all users | Fragmented into 24+ keys, 24x more cache misses |
| Social consistency | Shareable — same artist worldwide | Broken — links show different artists by timezone |
| Marketing coordination | "Today's Artist is Radiohead" is globally true | Only true for 1/24th of the world at any moment |
| Travelling users | Consistent experience | Artist "travels back in time" crossing zones |
| DB pressure at midnight | Single thundering-herd event, mitigated by SETNX | 24 thundering-herd events across 24 hours |

**If the business requires Zoned Transitions — the architecture supports it:**

The calculation is inherently offset-aware:

```
LocalDay = floor((UnixTimestamp + OffsetSeconds) / 86400)
targetIndex = LocalDay % totalArtistCount
cacheKey = aod:offset:{offsetSeconds}:day:{LocalDay}
```

To mitigate the 24x cache fragmentation and the "thundering herd times 24" problem, a background projection pre-calculates all 24 offset results into a single Redis Hash:

```
HSET aod:projections +0000 {artist_id_utc}
HSET aod:projections +0900 {artist_id_tokyo}
HSET aod:projections -0500 {artist_id_nyc}
...
```

Client requests become `HGET aod:projections {userOffset}` — O(1), zero database pressure during request spikes. The projection job runs hourly (not at midnight) so that offset transitions are pre-warmed before they occur.

This is documented, not built. The UTC anchor is the correct default for a streaming platform prioritising social shareability. The zoned path is a configuration and business decision, not an architectural one — the system already supports it.

### 3. Cache Strategy — Redis via CachePort

Redis serves as the distributed cache layer behind a `CachePort` interface. There is no in-memory fallback — the POC runs the same infrastructure as production.

**Redis serves four distinct roles:**

| Role | Mechanism | Key Pattern |
|------|-----------|-------------|
| **AOD result cache** | `GET`/`SET` with TTL to midnight UTC | `aod:artist` |
| **AOD single-flight** | `SETNX` lock + pub/sub signal | `aod:lock`, `aod:notifications` |
| **Artist count** | Atomic `INCR` on create, `GET` on read (no TTL) | `artist:count` |
| **Idempotency keys** | `SETNX` with short TTL | `idempotency:{key}` |

**Why not Caffeine as a "dev mode" shortcut?** A senior choice would be to show the hexagonal port with two adapters. A staff choice is to ship the correct one. An in-process cache cannot provide the distributed locking (`SETNX`), pub/sub coordination, atomic counters, or cross-node consistency that a horizontally scaled service requires. Docker Compose makes Redis a single-command dependency — there is no friction to justify an inferior substitute.

The `CachePort` interface remains. If a future adapter is needed (Memcached, Hazelcast), it plugs in without domain changes. The hexagonal benefit is proven by the `AuditPublisher` port, not by shipping a toy cache.

### 4. Data Integrity & Concurrency

**Two-Tiered Auditing:**

The system separates *housekeeping* auditing from *compliance* auditing:

- **Housekeeping (Spring Data JPA Auditing):** Every entity uses `@EntityListeners(AuditingEntityListener.class)` with `@CreatedDate`, `@LastModifiedDate`, and `@Version`. These are populated automatically by the persistence layer at zero overhead — no extra tables, no triggers, no Envers. This was chosen deliberately over Hibernate Envers: the compliance audit trail is externalised (see Decision #8), so Envers' shadow tables would be redundant infrastructure.
- **Compliance (AuditPublisher port):** State-change events are emitted asynchronously after successful commits (see Decision #8). The before/after payload provides the full revision history that Envers would have stored locally, but externalised to a dedicated audit service.

**Optimistic Locking — Mitigating the Lost Update:**

The `@Version` field on Artist entities prevents the "Lost Update" problem in a distributed system. When two editors simultaneously read Artist v3, the first write succeeds (v3 → v4). The second write fails with `409 Conflict` because its expected version (v3) no longer matches. The client must re-read and retry. This is the correct strategy for a read-heavy metadata service where write contention is low — pessimistic locking would serialise reads unnecessarily.

**Idempotency (X-Idempotency-Key):** Cached in the `CachePort` with a short TTL. Prevents duplicate track creation from network retries.

**Natural Key — ISRC:** The database enforces a unique constraint on `isrc` (International Standard Recording Code, ISO 3901) rather than `(artist_id, title)`. This is the music industry's canonical recording identifier, and correctly handles the real-world case of an artist having multiple recordings with the same title (studio, live, remaster). The ISRC constraint acts as a safety net beneath the idempotency layer.

### 5. CQRS-Lite (Infrastructure Level)

The command (write) and query (read) paths are separated at the Spring configuration level. In the POC, both point to the same datasource. In production, writes target the Postgres primary; reads target a load-balanced read replica pool.

The wiring is visible in configuration — the architectural intent is implemented, and the production cutover is a configuration change, not a code change.

### 6. The Alias Identity Model

```
┌──────────────┐       ┌───────────────────┐
│   Artist     │       │   ArtistAlias     │
│──────────────│       │───────────────────│
│ id (PK, UUID)│◄──────│ artist_id (FK)    │
│ name         │       │ alias_name (IDX)  │
│ version      │       │ created_at        │
│ created_at   │       └───────────────────┘
│ updated_at   │
└──────┬───────┘
       │ 1:N
┌──────┴───────┐
│    Track     │
│──────────────│
│ id (PK, UUID)│
│ artist_id(FK)│
│ title        │
│ isrc (UQ)    │
│ genre        │
│ duration_secs│
│ created_at   │
│ updated_at   │
└──────────────┘
```

The specification says "edit an artist's name." The PATCH endpoint does exactly that — a simple canonical rename. The alias system is an **enhancement**: real-world artists operate under multiple names, and a streaming platform must resolve any of them.

**ISRC as the Natural Key:** The unique constraint is on `isrc` (International Standard Recording Code, ISO 3901), not `(artist_id, title)`. A real streaming platform must handle the same title appearing multiple times in an artist's catalogue — studio version, live recording, remaster, acoustic version — each a distinct recording with its own ISRC. The 12-character ISRC (e.g. `GBAYE7500101`) is the music industry's canonical identifier for this purpose.

The `Track` table remains thin — it points to a single artist UUID. Alias resolution is an O(log n) index lookup that returns the canonical artist ID, keeping downstream queries simple.

### 7. Observability — "Day 2" Readiness

A service is not production-ready if it is a black box.

- **Spring Boot Actuator:** `/health` (liveness + readiness probes for Kubernetes), `/metrics` (Prometheus-compatible), `/info` (build + git metadata).
- **Structured Logging:** JSON-formatted logs for ingestion into ELK, Splunk, or CloudWatch.
- **RFC 7807 Problem Details:** All error responses return machine-readable JSON structures, not stack traces. Downstream consumers can programmatically handle errors.

### 8. Auditing — Asynchronous Event-Driven Pipeline

To maintain high throughput and separation of concerns, auditing is handled via an asynchronous event-driven pattern. The service publishes state-change events after successful transaction commits. This decouples metadata logic from audit persistence, ensuring that slow compliance writes or downstream search-index updates never block the critical path of music metadata ingestion.

**The Pipeline:**

```
┌──────────────┐    ┌───────────┐    ┌───────────┐    ┌─────────────────┐
│  Metadata    │───▶│  SNS      │───▶│  SQS      │───▶│  Audit Service  │
│  Service     │    │  Topic    │    │  Queue    │    │  (out of scope) │
│  (emit only) │    └───────────┘    └───────────┘    │  → S3/Glacier   │
└──────────────┘                                      │  → OpenSearch   │
                                                      └─────────────────┘
```

**Event Payload:**

```json
{
  "eventType": "ARTIST_NAME_CHANGED",
  "entityId": "a1b2c3d4-...",
  "before": { "name": "Queen" },
  "after": { "name": "Queen (Legacy)" },
  "actorId": "user-xyz",
  "idempotencyKey": "abc-123",
  "timestamp": "2026-04-09T10:30:00Z"
}
```

**POC Implementation:** The `AuditPublisher` port is implemented by a `LoggingAuditPublisher` adapter that emits structured log entries: `[AUDIT] ARTIST_NAME_CHANGED {...}`. This proves the emission contract. The SNS adapter is a configuration swap — the domain and use case layers are unchanged.

**Why SNS/SQS over synchronous writes?** Writing audit records in the same transaction as the metadata update would increase write latency, couple the service to an audit datastore, and create a failure dependency — a slow audit table could degrade the user-facing service. The "fire and forget" pattern preserves the latency floor.

### 9. API Versioning — Header-Based Date Scheme

API versioning uses the `X-ICE-Version` header rather than URL path segments (`/v1/`, `/v2/`).

**Why not path versioning?** Path-based versions pollute resource URIs with non-resource concerns. A track's identity is `/api/artists/{id}/tracks/{trackId}` — the version of the *contract* is orthogonal to the *resource*. Path versioning also creates friction: client libraries hard-code paths, proxies need rewrite rules, and every new version doubles the route table.

**The date scheme:** Version identifiers are ISO dates (e.g. `2026-04-09`) representing the contract date. This provides natural ordering, clear provenance, and avoids the "what changed between v3 and v7?" ambiguity of sequential numbering.

**Behaviour:**

| Header | Result |
|--------|--------|
| `X-ICE-Version: 2026-04-09` | Current contract |
| *(omitted)* | Default to current contract |
| `X-ICE-Version: 2024-01-01` | Legacy contract (when breaking changes exist) |
| `X-ICE-Version: 9999-01-01` | `400 Bad Request` — unknown version |

### 10. Environment & Secret Isolation

No secret is committed to source control. Ever.

**The Pattern — Environment Variable Injection:**

```
┌─────────────┐     ┌──────────────────┐     ┌──────────────────────┐
│ .env.example│     │  .env            │     │  application.yml     │
│ (committed) │────▶│  (gitignored)    │────▶│  ${DB_PASSWORD}      │
│ shape only  │     │  real values     │     │  placeholder only    │
└─────────────┘     └──────────────────┘     └──────────────────────┘
```

| Layer | Secret Source | Scope |
|-------|-------------|-------|
| **Local dev** | `.env` file (gitignored), loaded by Docker Compose and Spring Boot | Developer workstation |
| **CI/CD** | Pipeline secrets (GitHub Actions secrets, GitLab CI variables) | Build environment |
| **Production** | AWS Secrets Manager / HashiCorp Vault, injected as env vars by the orchestrator (ECS task definition, K8s `ExternalSecret`) | Runtime environment |

**What lives where:**

| File | Committed? | Contains |
|------|-----------|----------|
| `application.yml` | Yes | `${DB_PASSWORD}` placeholders with safe defaults for host/port only |
| `.env.example` | Yes | Variable names and shape — no real values |
| `.env` | **No** (gitignored) | Real credentials for local development |

**Why not Spring profiles for environment separation?** Profiles (`application-dev.yml`, `application-prod.yml`) embed environment assumptions into the codebase. A 12-Factor application treats config as environment — the same artifact runs in every environment, differentiated only by injected variables. This also eliminates the risk of a `prod` profile being accidentally committed with real credentials.

---

## Data Model

### Database Migrations (Flyway)

Schema evolution is managed by Flyway. Migration scripts live in `src/main/resources/db/migration/`. The database is never modified by hand or by `ddl-auto`.

### Indexes

| Table | Index | Purpose |
|-------|-------|---------|
| `artist` | PK on `id` | Primary lookup |
| `artist` | Covering on `(created_at, id)` | AOD Rank-Pointer query — OFFSET satisfied from index without heap access |
| `artist_alias` | B-Tree on `alias_name` | Alias resolution |
| `artist_alias` | FK on `artist_id` | Join performance |
| `track` | FK on `artist_id` | Fetch tracks by artist |
| `track` | Unique on `isrc` | ISRC natural key — industry-standard recording identifier (ISO 3901) |

---

## Running the Tests

**Prerequisites:** Docker must be running (Testcontainers spins up real Postgres and Redis containers).

```bash
./mvnw test
```

### Test Philosophy — No Mocks for Infrastructure

Repository tests and integration tests run against **real PostgreSQL and Redis** via Testcontainers. There are no embedded substitutes (H2, embedded Redis). The test infrastructure matches production infrastructure. If a query works in tests, it works in production.

Testcontainers lifecycle is managed by Spring Boot's `@ServiceConnection` — containers are started once per test suite, shared across test classes, and torn down on JVM exit.

### Test Pyramid

| Test Class | Layer | Infrastructure | Description |
|------------|-------|----------------|-------------|
| `*DomainTest` | Domain | None | Pure unit tests. No Spring context. No I/O. |
| `*UseCaseTest` | Ports | Mocked outbound ports | Use case logic verified against port interfaces. |
| `*ControllerTest` | Adapters (In) | `@WebMvcTest` | HTTP contract: status codes, headers, request/response shapes. |
| `*RepositoryTest` | Adapters (Out) | `@DataJpaTest` + Testcontainers (Postgres) | JPA mapping, query correctness, constraints — against real Postgres. |
| `*CacheTest` | Adapters (Out) | Testcontainers (Redis) | Cache operations, TTL behaviour, `SETNX` stampede protection — against real Redis. |
| `*IntegrationTest` | End-to-End | `@SpringBootTest` + Testcontainers (Postgres, Redis) | Full request lifecycle through all layers against real infrastructure. |

---

## Production Deployment Notes

### What this POC demonstrates

- Hexagonal architecture with clean domain isolation
- Virtual thread readiness (Java 25 LTS, pinning-safe)
- Redis-backed distributed caching with stampede protection
- Asynchronous audit event emission via `AuditPublisher` port
- Header-based API versioning (date scheme, no URL pollution)
- CQRS wiring ready for read replica separation
- Idempotent writes, optimistic locking, deterministic AOD rotation
- Full observability stack
- Real infrastructure testing via Testcontainers (Postgres + Redis) — no embedded substitutes

### What a production deployment would add

- **SNS audit publisher adapter** replacing the logging adapter, wiring to the SNS → SQS → Audit Service pipeline
- **Audit Service** consuming SQS: cold storage (S3/Glacier) for compliance, warm storage (OpenSearch) for CS queries
- **Zoned AOD projection** (if business requires localised transitions): hourly background job pre-warming a Redis Hash mapping all 24 offsets to their artist selections
- **Materialised rank table** for AOD at 10M+ catalogue scale, with nightly re-index job
- **Read replica datasource** configured behind a load balancer, wired to the existing CQRS query path
- **API authentication** (OAuth2 / API keys) — excluded from the POC to keep the focus on architecture and domain logic
- **Rate limiting** at the API gateway layer
- **OpenAPI spec** generated from controller annotations via Springdoc, published to a developer portal
- **GraalVM native image** build for sub-second cold starts in serverless / Kubernetes environments

---

## Trade-off Summary

| Decision | I chose                                               | Over | Reasoning |
|----------|-------------------------------------------------------|------|-----------|
| Architecture | Hexagonal (Ports & Adapters)                          | Layered / Framework-coupled | Persistence and framework changes don't touch domain logic |
| Search | Postgres B-Tree / GIN indexes                         | OpenSearch / Elasticsearch | Operational simplicity for POC; CQRS makes search migration a plug-in task |
| AOD algorithm | Rank-Pointer (`epochDay % n` + covering index OFFSET) | Random / stateful pointer / naive UUID scan | Guarantees fair rotation; covering index avoids table scan; materialised rank table documented as O(1) scaling path |
| AOD temporal anchor | Global UTC midnight | Timezone-localised transitions | Social consistency + single cache key; zoned transitions supported via offset-partitioned Redis Hash projection if business requires it |
| AOD artist count | Redis `INCR` counter (DB seed on cold start) | `COUNT(*)` table scan | O(1) reads on hot path; table scan runs once per Redis cold start, never during request handling |
| AOD coordination | Redis pub/sub signal (`SETNX` + `SUBSCRIBE`) | `Thread.sleep` polling loop | Event-driven: waiters complete immediately on signal; no wasted CPU cycles or latency from arbitrary sleep intervals |
| Cache | Redis (distributed) | Caffeine (in-process) | Ship the production infrastructure; Docker Compose eliminates the friction argument for in-memory substitutes |
| Auditing | Async event emission (SNS/SQS) | Synchronous audit table writes | Preserves latency floor; decouples compliance writes from critical path |
| API versioning | Header-based date scheme (`X-ICE-Version`) | URL path versioning (`/v1/`) | Resource URIs stay clean; avoids route duplication and client-library friction |
| Mapping | Manual Entity ↔ Domain Record | `@Entity` on domain objects | Staff trade-off: more code today, zero framework migration pain tomorrow |
| Application state | 100% stateless nodes | Session affinity | Infinite horizontal scaling on Kubernetes; all state lives in Postgres + Redis |
| JDK | Java 25 LTS | Java 24/26 (feature releases) | Enterprise lifecycle stability; first LTS with virtual thread pinning fix |
| Testing infra | Testcontainers (real Postgres + Redis) | H2 / embedded Redis | Tests prove production behaviour, not substitute behaviour; eliminates "works in H2, fails in Postgres" class of bugs |
| Auditing | JPA `@CreatedDate`/`@LastModifiedDate` + async event emission | Hibernate Envers | Envers shadow tables redundant when compliance trail is externalised; JPA auditing is zero-overhead housekeeping |
| Secrets | Environment variable injection (`.env` / Secrets Manager) | Hardcoded in config or Spring profiles | 12-Factor compliant; same artifact in every environment; zero risk of credential commit |

---

## Development Process

This project was developed using TDD (Red → Green → Refactor) with feature branches and atomic commits. Each iteration follows:

1. **Architectural decision** documented in this README
2. **Failing test** written against the contract
3. **Minimal implementation** to pass the test
4. **Refactor** with all tests green
5. **Commit** on feature branch with descriptive message
6. **PR** to main branch
7. **Merge** to main branch
8. **Pull** from main branch
9. **Feature branch** for next iteration

---

