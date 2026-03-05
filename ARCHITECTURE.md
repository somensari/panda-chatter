# Panda Chatter — Architecture & Design

## System Overview

Panda Chatter is a **polyglot Spring Boot monorepo** demonstrating event-driven architecture using Kafka/RedPanda as the message backbone and SSE for real-time browser push.

---

## Component Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                          Browser Clients                             │
│                                                                      │
│  ┌──────────────────┐          ┌───────────────────────────────┐    │
│  │  Producer UI     │          │  Consumer UI (EventSource)    │    │
│  │  localhost:8080  │          │  localhost:8081               │    │
│  └────────┬─────────┘          └───────────────────────────────┘    │
│           │ HTTP POST /send              ▲ SSE / text-event-stream   │
└───────────┼──────────────────────────── │ ──────────────────────────┘
            │                             │
   ┌────────▼──────────────┐    ┌─────────┴──────────────┐
   │  panda-chatter-       │    │  panda-chatter-         │
   │  producer             │    │  consumer               │
   │                       │    │                         │
   │  ChatController       │    │  FeedController         │
   │  MessageProducerSvc   │    │  MessageConsumerSvc     │
   │  KafkaTemplate        │    │  @KafkaListener         │
   └──────────┬────────────┘    └────────────┬────────────┘
              │                              │
              │  Publish (key=messageId)     │ Consume (group: panda-feed-group)
              │                             │
              ▼                             │
   ┌──────────────────────────────────────────────────────┐
   │              Kafka / RedPanda Broker                 │
   │                                                      │
   │   Topic: chat-messages   Partitions: 3   RF: 1       │
   │                                                      │
   │   Partition 0 │ Partition 1 │ Partition 2           │
   └──────────────────────────────┬───────────────────────┘
                                  │
                                  │ Consume (group: panda-db-group)
                                  ▼
                     ┌────────────────────────┐
                     │  panda-chatter-        │
                     │  dbwriter              │
                     │                        │
                     │  MessageDbWriterSvc    │
                     │  @KafkaListener        │
                     │  ChatMessageRepository │
                     │                        │
                     │  H2 In-Memory DB       │
                     │  table: chat_messages  │
                     └────────────────────────┘
```

---

## Module Details

### panda-chatter-common

Shared library. Not a Spring Boot app — just a plain JAR.

- `ChatMessage` — Java record used as the Kafka value type
- Jackson annotations for consistent JSON serialization of `LocalDateTime`

```java
public record ChatMessage(
    String id,
    String username,
    String content,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime timestamp
) {}
```

### panda-chatter-producer (port 8080)

Spring MVC (Thymeleaf) + Spring Kafka producer.

**Key flows:**

```
Browser POST /send
  → ChatController.send()
  → MessageProducerService.sendMessage()
      → Creates ChatMessage(UUID, username, content, now())
      → KafkaTemplate.send("chat-messages", messageId, message)
  → Adds to in-memory sentMessages list
  → PRG redirect → GET /
  → Thymeleaf renders chat.html with sentMessages
```

- Messages keyed by UUID → consistent partition assignment per message
- `KafkaTemplate` uses Spring-configured `ObjectMapper` for serialization (JavaTimeModule included)
- Username persisted in browser `localStorage` for convenience

### panda-chatter-consumer (port 8081)

Spring MVC + Spring Kafka consumer + SSE.

**Key flows:**

```
Kafka offset: latest (only new messages since startup)
Consumer group: panda-feed-group

@KafkaListener → MessageConsumerService.consume()
  → FeedController.broadcast(message)
      → Serialize to JSON
      → Loop over CopyOnWriteArrayList<SseEmitter>
          → emitter.send(event "message", json)
          → Remove stale emitters on IOException

Browser GET / → Thymeleaf feed.html
  → JavaScript: new EventSource('/feed/stream')
  → GET /feed/stream → SseEmitter added to registry
  → addEventListener('message') → DOM append
```

**SSE Design:**
- `SseEmitter(Long.MAX_VALUE)` — never times out server-side
- `CopyOnWriteArrayList` — safe for concurrent read + rare writes
- Stale emitters (browser closed/navigated away) detected on `IOException` and removed

### panda-chatter-dbwriter (port 8082)

Spring Boot + Spring Data JPA + H2 + Spring Kafka consumer.

**Key flows:**

```
Kafka offset: earliest (replays full topic on restart)
Consumer group: panda-db-group

@KafkaListener → MessageDbWriterService.consume()
  → Build ChatMessageEntity(id, username, content, sentAt, receivedAt=now())
  → ChatMessageRepository.save(entity)  ← Spring Data JPA / H2
```

- H2 in-memory: data is lost on shutdown (appropriate for a demo)
- H2 Console available at `http://localhost:8082/h2-console`
- `receivedAt` field records when the message was processed by this service
- `auto-offset-reset: earliest` ensures no messages are missed after restarts (within topic retention)

---

## Kafka / RedPanda Configuration

### Topic: `chat-messages`

| Setting        | Value | Rationale                              |
|----------------|-------|----------------------------------------|
| Partitions     | 3     | Allows up to 3 parallel consumers     |
| Replication    | 1     | Single-node dev setup                  |
| Message key    | UUID  | Even distribution across partitions    |

### Consumer Groups

| Group              | Module      | Offset   | Behavior                       |
|--------------------|-------------|----------|--------------------------------|
| `panda-feed-group` | consumer    | latest   | Only shows messages sent while UI is open |
| `panda-db-group`   | dbwriter    | earliest | Replays all retained messages on restart  |

Two independent consumer groups ensure every message is received by **both** consumers, regardless of order.

### Spring Profiles

| Profile  | Bootstrap Server   | Docker Compose File        |
|----------|--------------------|----------------------------|
| `panda`  | localhost:19092    | `docker-compose.yml`       |
| `kafka`  | localhost:9092     | `docker-compose-kafka.yml` |

Default active profile: **`panda`**

---

## Serialization

All three apps use the **same Spring-auto-configured `ObjectMapper`** (injected into Kafka factories) to guarantee serialization consistency:

- `jackson-datatype-jsr310` registered automatically by Spring Boot
- `write-dates-as-timestamps: false` → ISO 8601 strings
- `JsonSerializer.setAddTypeInfo(false)` → no `__TypeId__` headers sent
- `JsonDeserializer.addTrustedPackages("*")` → accept all packages

---

## Data Model — H2 Schema

```sql
CREATE TABLE chat_messages (
    id           VARCHAR(36)  PRIMARY KEY,
    username     VARCHAR(50)  NOT NULL,
    content      VARCHAR(500) NOT NULL,
    sent_at      TIMESTAMP    NOT NULL,
    received_at  TIMESTAMP    NOT NULL
);
```

Schema generated automatically by Hibernate (`ddl-auto: create-drop`).

---

## Technology Stack

| Concern              | Technology                    |
|----------------------|-------------------------------|
| Language             | Java 21                       |
| Framework            | Spring Boot 3.4.3             |
| Messaging            | Spring Kafka + Kafka/RedPanda |
| Web UI               | Spring MVC + Thymeleaf        |
| Real-time push       | Server-Sent Events (SSE)      |
| Persistence          | Spring Data JPA + H2          |
| Observability        | Spring Boot Actuator          |
| Build                | Maven 3.9 (multi-module)      |
| Containerization     | Docker Compose                |

---

## Extending the Project

- **Add authentication** — Spring Security + session management
- **Persistent storage** — swap H2 for PostgreSQL (`spring.datasource.*`)
- **Multiple topics** — add topic per chat room
- **Scale consumers** — run multiple instances with same consumer group for load balancing
- **Schema Registry** — use Confluent Schema Registry with Avro instead of JSON
- **UI framework** — replace plain HTML/JS with HTMX or React
