# RAG Hub

**RAG Hub** is a high-performance, enterprise-grade Spring Boot engine engineered for Retrieval-Augmented Generation (RAG). Built on Java 21, it provides asynchronous document parsing, vector database ingestion, k-NN similarity context retrieval, and low-latency Server-Sent Events (SSE) token streaming—delivering grounded, zero-hallucination AI responses with exact source attributions.

---

## Architecture Overview

```
                      ┌────────────────────────────────────────────────────────┐
                      │                   INGESTION PIPELINE                   │
                      │                                                        │
┌──────────────┐      │  ┌────────────┐    ┌───────────┐    ┌───────────────┐  │      ┌──────────────┐
│  Multi-Format│      │  │  Document  │    │ Token     │    │ Embedding     │  │      │ Vector Store │
│  Documents   ├─────►│  │  Parser    ├───►│ Chunking  ├───►│ Generation    ├──┼─────►│ (Pgvector /  │
│ (.pdf,.docx,.txt) │      │  │            │    │ & Overlap │    │ (Spring AI)   │  │      │  Qdrant)     │
└──────────────┘      │  └────────────┘    └───────────┘    └───────────────┘  │      └──────┬───────┘
                      └────────────────────────────────────────────────────────┘             │
                                                                                             │ k-NN Search
                      ┌────────────────────────────────────────────────────────┐             │
                      │                   RETRIEVAL & INFERENCE                │             │
┌──────────────┐      │  ┌────────────┐    ┌───────────┐    ┌───────────────┐  │             │
│ Client message │      │  │  Security  │    │ Redis     │    │ Context       │◄─┼─────────────┘
│  (HTTP/POST) ├─────►│  │  Filter    ├───►│ Cache     ├───►│ Augmentation  │  │
└──────────────┘      │  │  (JWT)     │    │ Layer     │    │ & Prompting   │  │
                      │  └────────────┘    └───────────┘    └──────┬────────┘  │
                      └────────────────────────────────────────────────────┼───┘
                                                                           │
                                                                           ▼
                                                                  ┌─────────────────┐
                                                                  │ LLM API         │
                                                                  │ (Gemini/OpenAI) │
                                                                  └────────┬────────┘
                                                                           │
                                                                           ▼
                                                                  ┌─────────────────┐
                                                                  │ SSE Stream      │
                                                                  │ (delta/sources) │
                                                                  └─────────────────┘

```

---

## Core Capabilities

* **Asynchronous Document Ingestion:** Multi-format file processing (`.pdf`, `.docx`, `.txt`) with configurable token-chunking windows, overlap thresholds, and automated embedding generation.
* **Vector Similarity Retrieval:** Fast k-Nearest Neighbors (k-NN) similarity searches over indexed embeddings with configurable distance metrics to filter out irrelevant context.
* **Real-time SSE Streaming:** Asynchronous token streaming over HTTP POST (`text/event-stream`), delivering chunked model responses and source attribution arrays (`sources` event) with minimal time-to-first-token (TTFT).
* **Multi-Tier Caching:** Redis-backed query caching and session state management to eliminate redundant model calls and reduce API overhead.
* **Hardened Dual-Token Security:** Spring Security integration featuring HttpOnly, `SameSite=Strict` JWT cookies (`accessToken`, `refreshToken`) with atomic token rotation and revocation.

---

## Tech Stack

| Domain | Technology |
| --- | --- |
| **Language & Runtime** | Java 21 (Virtual Threads / Loom ready) |
| **Framework** | Spring Boot 3.x, Spring AI, Spring Security, Spring WebFlux / Reactive Web |
| **Build System** | Gradle |
| **Caching & Messaging** | Redis |
| **Vector Database** | Pgvector / Qdrant / Pinecone |
| **Authentication** | JWT (Dual-Token in HttpOnly Cookies) |

---

## System Workflows

### 1. Document Ingestion & Indexing

1. **Validation:** Uploaded payloads are validated against max file size limits and MIME-type constraints.
2. **Parsing & Chunking:** Documents are converted into text streams and split into overlapping token chunks to preserve context boundaries.
3. **Embedding & Storage:** Chunks are vectorized using standard embedding models and written to the vector store along with metadata (`filename`, `userId`, `timestamp`).

### 2. Context-Augmented Query & Streaming

1. **Cache Check:** incoming query hashes are evaluated against Redis.
2. **Vector Search:** On cache miss, a k-NN search yields the top-$k$ relevant text blocks matching the user prompt.
3. **Prompt Assembly:** Retrieved blocks are wrapped into system instructions and sent to the LLM backend.
4. **SSE Event Emission:**
* `event: sources` — Dispatches array of source document metadata.
* `event: delta` — Streams word/token increments asynchronously.



---

## Security Model

```
Browser / Client                          Spring Security Filter                        Auth Controller
   │                                                 │                                         │
   │─── POST /auth/refresh (Cookies Attached) ──────►│                                         │
   │                                                 │─── Validate Refresh Token ─────────────►│
   │                                                 │                                         │ Atomic Token
   │                                                 │                                         │ Rotation
   │◄── 200 OK (New HttpOnly Cookies Set) ───────────│◄────────────────────────────────────────│

```

* **Mitigated Vectors:** HttpOnly flags prevent client-side JavaScript access (XSS defense); `SameSite=Strict` mitigates CSRF.
* **Atomic Rotation:** Reissuing a refresh token invalidates the predecessor in Redis, revoking compromised sessions instantly.

---

## Getting Started

### Prerequisites

* **Java 21 JDK** or higher
* **Gradle 8.x**

### Local Setup

1. **Clone the repository:**
```bash
git clone https://github.com/totosborhany/Rag-Tag
cd rag-hub

```


2. **Configure application properties:**
   Create a `application.properties` file in the root directory or add these if it exists :
```env
# ===================================================================
# Server & Application Configuration
# ===================================================================
server.port=8080
spring.application.name=rag-hub
spring.docker.compose.enabled=false
spring.threads.virtual.enabled=true

# Error Handling
server.error.include-stacktrace=never
server.error.include-message=always
server.error.include-binding-errors=never

# ===================================================================
# Database Connection (PostgreSQL / Supabase)
# ===================================================================
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/postgres?prepareThreshold=0}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:your_db_password}

# JPA & Hibernate Settings
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# ===================================================================
# Redis Connection (Caching & Session Management)
# ===================================================================
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}

# ===================================================================
# Spring AI Configuration (Gemini API via OpenAI Compatibility)
# ===================================================================
spring.ai.openai.api-key=${GEMINI_API_KEY:your_gemini_api_key}
spring.ai.openai.base-url=https://generativelanguage.googleapis.com/v1beta/openai
spring.ai.openai.chat.options.model=gemini-3.6-flash // check if this model is still active
spring.ai.openai.chat.enabled=true

# Disable default OpenAI starter embedding auto-configuration
spring.ai.openai.embedding.enabled=false

# ===================================================================
# Spring AI Embedding Configuration (Google GenAI Native)
# ===================================================================
spring.ai.google.genai.api-key=${GEMINI_API_KEY:your_gemini_api_key}
spring.ai.google.genai.embedding.text.model=text-embedding-001

# ===================================================================
# Spring AI VectorStore (PgVector)
# ===================================================================
spring.ai.vectorstore.pgvector.table-name=vector_store
spring.ai.vectorstore.pgvector.dimensions=768
spring.ai.vectorstore.pgvector.initialize-schema=true

# ===================================================================
# Security & JWT Verification
# ===================================================================
# OAuth2 Resource Server (Supabase JWKS integration)
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=${JWKS_URI:https://your-project.supabase.co/auth/v1/.well-known/jwks.json}

# Local JWT Configuration
jwt.access-secret=${JWT_ACCESS_SECRET:your_base64_or_hex_access_secret_min_256_bits}
jwt.access-expiration=900000
jwt.refresh-secret=${JWT_REFRESH_SECRET:your_base64_or_hex_refresh_secret_min_256_bits}
jwt.refresh-expiration=604800000

# ===================================================================
# Resilience4j Rate Limiter
# ===================================================================
resilience4j.ratelimiter.instances.ragLlmLimiter.limitForPeriod=5
resilience4j.ratelimiter.instances.ragLlmLimiter.limitRefreshPeriod=60s
resilience4j.ratelimiter.instances.ragLlmLimiter.timeoutDuration=0s

# ===================================================================
# Mail Configuration
# ===================================================================
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${MAIL_USERNAME:your-email@gmail.com}
spring.mail.password=${MAIL_PASSWORD:your-app-password}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# ===================================================================
# File Upload Limits
# ===================================================================
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# ===================================================================
# Actuator & API Documentation
# ===================================================================
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always
springdoc.enable-spring-data-web-support=true

info.app.name=RAG Hub
info.app.version=1.0.0
info.app.description=Enterprise RAG Engine Service
```




3. **Build and run the application:**
```bash
./gradlew bootRun

```



