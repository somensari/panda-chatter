# Panda Chatter

A real-time, event-driven chat application built with Spring Boot 3, Apache Kafka / RedPanda, and Server-Sent Events.

```
         ( )  ( )
          \ \/ /
          (o  o)
          /|  |\
         (_|  |_)   Panda Chatter
```

---

## Architecture Overview

```
 ┌─────────────────────┐        ┌───────────────────────────────┐
 │   Producer (8080)   │──────▶ │   Kafka / RedPanda Broker     │
 │   Thymeleaf UI      │        │   Topic: chat-messages        │
 │   POST /send        │        │   3 partitions                │
 └─────────────────────┘        └────────────┬──────────────────┘
                                             │
                         ┌───────────────────┴───────────────────┐
                         │                                       │
              ┌──────────▼──────────┐             ┌─────────────▼─────────────┐
              │  Consumer (8081)    │             │   DB Writer (8082)        │
              │  Group: panda-feed  │             │   Group: panda-db         │
              │  SSE live feed UI   │             │   H2 persistence          │
              │  offset: latest     │             │   H2 Console: /h2-console │
              └─────────────────────┘             └───────────────────────────┘
```

## Modules

| Module                    | Port | Description                                      |
|---------------------------|------|--------------------------------------------------|
| `panda-chatter-common`    | —    | Shared `ChatMessage` record, serialization       |
| `panda-chatter-producer`  | 8080 | Web UI to compose and send messages to Kafka     |
| `panda-chatter-consumer`  | 8081 | Live feed UI — receives messages via SSE         |
| `panda-chatter-dbwriter`  | 8082 | Persists every message to H2 in-memory database  |

## Prerequisites

- **Java 21**
- **Maven 3.9+**
- **Docker & Docker Compose**

## Quick Start

### 1. Start the message broker

**RedPanda** (default — recommended):
```bash
docker compose up -d
```

**Apache Kafka** (alternative):
```bash
docker compose -f docker-compose-kafka.yml up -d
```

Both broker UIs are available at **http://localhost:8090** once healthy.

### 2. Build all modules

```bash
mvn clean install
```

### 3. Run the services

Open **three terminals** and run each service:

```bash
# Terminal 1 — Producer UI (http://localhost:8080)
cd panda-chatter-producer
mvn spring-boot:run

# Terminal 2 — Consumer Live Feed (http://localhost:8081)
cd panda-chatter-consumer
mvn spring-boot:run

# Terminal 3 — DB Writer (http://localhost:8082/h2-console)
cd panda-chatter-dbwriter
mvn spring-boot:run
```

> By default all services activate the **`panda`** profile (RedPanda on `localhost:19092`).

### 4. Switch to Kafka profile

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=kafka
```

Or set the environment variable:
```bash
SPRING_PROFILES_ACTIVE=kafka mvn spring-boot:run
```

## Running as Fat JARs

```bash
mvn clean package -DskipTests

java -jar panda-chatter-producer/target/panda-chatter-producer-1.0.0-SNAPSHOT.jar
java -jar panda-chatter-consumer/target/panda-chatter-consumer-1.0.0-SNAPSHOT.jar
java -jar panda-chatter-dbwriter/target/panda-chatter-dbwriter-1.0.0-SNAPSHOT.jar
```

## Endpoints

| Service    | URL                                  | Description              |
|------------|--------------------------------------|--------------------------|
| Producer   | http://localhost:8080/               | Chat compose UI          |
| Consumer   | http://localhost:8081/               | Live feed UI             |
| DB Writer  | http://localhost:8082/h2-console     | H2 database browser      |
| DB Writer  | http://localhost:8082/actuator/health| Health check             |
| Broker UI  | http://localhost:8090/               | RedPanda Console / Kafka UI |

### H2 Console settings
- **JDBC URL**: `jdbc:h2:mem:pandachat`
- **Username**: `sa`
- **Password**: _(leave blank)_

## Actuator Endpoints

All services expose:

```
/actuator/health
/actuator/info
/actuator/metrics
/actuator/env
```

## Kafka Topic

| Property      | Value           |
|---------------|-----------------|
| Name          | `chat-messages` |
| Partitions    | 3               |
| Replication   | 1               |

## Consumer Groups

| Group             | Service    | Offset  | Purpose             |
|-------------------|------------|---------|---------------------|
| `panda-feed-group`| consumer   | latest  | Real-time SSE feed  |
| `panda-db-group`  | dbwriter   | earliest| Full message history|

## ChatMessage Schema

```json
{
  "id":        "550e8400-e29b-41d4-a716-446655440000",
  "username":  "bamboo-panda",
  "content":   "Hello from the panda stream!",
  "timestamp": "2025-01-15T10:30:00"
}
```

## Project Structure

```
panda-chatter/
├── pom.xml                          Parent POM
├── docker-compose.yml               RedPanda + Console
├── docker-compose-kafka.yml         Kafka (KRaft) + Kafka UI
├── README.md
├── ARCHITECTURE.md
├── panda-chatter-common/            Shared model
├── panda-chatter-producer/          Producer app (port 8080)
├── panda-chatter-consumer/          Consumer live feed (port 8081)
└── panda-chatter-dbwriter/          DB persistence consumer (port 8082)
```

## GitHub

Repository: https://github.com/somensari/panda-chatter
