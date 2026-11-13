# Simple Chat Application Using Redis

A Spring Boot REST API for creating chat rooms, joining as a participant, sending messages, and reading recent chat history. Redis stores room data and messages, and Redis Pub/Sub broadcasts each new message to subscribers of that room.

This project has no user interface. You can use the API with Postman, `curl`, or PowerShell.

## Features

- Create uniquely named chat rooms
- Join a room and keep track of its participants
- Send messages and retrieve the latest N messages in chronological order
- Publish new messages to a room-specific Redis Pub/Sub channel
- Return clear errors for duplicate rooms, missing rooms, and invalid requests
- Delete a room and its Redis data
- Persist Redis data with append-only file (AOF) enabled in Docker Compose

## Technology

- Java 21
- Spring Boot 3
- Spring Data Redis with Lettuce
- Redis 7
- Maven

## 1. Prerequisites

Install:

- JDK 21 or newer
- Maven 3.9 or newer
- Redis 7, or Docker Desktop to run Redis in a container

Confirm Java and Maven are available in PowerShell:

```powershell
java -version
mvn -version
```

## 2. Get Redis running

### Recommended: Docker Desktop

Start Docker Desktop. From the project folder, run:

```powershell
docker compose up -d redis
docker compose ps
```

Wait until Redis is shown as running/healthy. It listens on `localhost:6379`. The Compose setup enables AOF persistence and stores Redis data in the `redis-data` Docker volume.

### Use an existing Redis server

Start Redis separately and note its host and port. The application defaults to `localhost:6379`; configure different values in the next step if needed.

## 3. Start the application

Open a second PowerShell window in the project folder and run:

```powershell
mvn spring-boot:run
```

The API is available at `http://localhost:8080`. Keep this terminal open while using the API.

To specify custom ports, set environment variables before starting Spring Boot:

```powershell
$env:REDIS_HOST = "localhost"
$env:REDIS_PORT = "6379"
$env:SERVER_PORT = "8080"
mvn spring-boot:run
```

## 4. Make requests

The base URL is:

```text
http://localhost:8080/api/chatapp/chatrooms
```

The examples below use PowerShell. In Postman, use the same HTTP methods, URLs, and JSON bodies, and set `Content-Type` to `application/json` for POST requests.

### Step 1: Create a room

**POST** `/api/chatapp/chatrooms`

```json
{
  "roomName": "general"
}
```

PowerShell:

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/chatapp/chatrooms" `
  -ContentType "application/json" `
  -Body '{"roomName":"general"}'
```

The API returns HTTP `201 Created` with the room ID and a success message.

### Step 2: Join as a participant

**POST** `/api/chatapp/chatrooms/general/join`

```json
{
  "participant": "guest_user"
}
```

PowerShell:

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/chatapp/chatrooms/general/join" `
  -ContentType "application/json" `
  -Body '{"participant":"guest_user"}'
```

Joining records the participant in the room. This sample has no authentication, so joining is not an authorization check for sending messages.

### Step 3: Send messages

**POST** `/api/chatapp/chatrooms/general/messages`

```json
{
  "participant": "guest_user",
  "message": "Hello, everyone!"
}
```

PowerShell:

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/chatapp/chatrooms/general/messages" `
  -ContentType "application/json" `
  -Body '{"participant":"guest_user","message":"Hello, everyone!"}'
```

Send another message by changing the participant and message fields. A successful request returns HTTP `200 OK`.

### Step 4: Read recent messages

**GET** `/api/chatapp/chatrooms/general/messages?limit=10`

PowerShell:

```powershell
Invoke-RestMethod -Method Get `
  -Uri "http://localhost:8080/api/chatapp/chatrooms/general/messages?limit=10"
```

The response contains up to the latest 10 messages, ordered from oldest to newest within that selection. The default limit is 50; valid values are from 0 to 500.

Example response:

```json
{
  "messages": [
    {
      "participant": "guest_user",
      "message": "Hello, everyone!",
      "timestamp": "2026-11-13T10:30:00Z"
    }
  ]
}
```

### Step 5: List participants (additional endpoint)

**GET** `/api/chatapp/chatrooms/general/participants`

```powershell
Invoke-RestMethod -Method Get `
  -Uri "http://localhost:8080/api/chatapp/chatrooms/general/participants"
```

### Step 6: Delete a room (additional endpoint)

**DELETE** `/api/chatapp/chatrooms/general`

```powershell
Invoke-RestMethod -Method Delete `
  -Uri "http://localhost:8080/api/chatapp/chatrooms/general"
```

Deleting a room removes its metadata, participants, and message history.

## API reference

| Method | Endpoint | Request body / query | Description |
|---|---|---|---|
| `POST` | `/api/chatapp/chatrooms` | `{"roomName":"general"}` | Create a room |
| `POST` | `/api/chatapp/chatrooms/{roomId}/join` | `{"participant":"guest_user"}` | Add a participant to a room |
| `POST` | `/api/chatapp/chatrooms/{roomId}/messages` | `{"participant":"guest_user","message":"Hello"}` | Store and publish a message |
| `GET` | `/api/chatapp/chatrooms/{roomId}/messages?limit=10` | Optional `limit` (0–500; default 50) | Read the latest messages |
| `GET` | `/api/chatapp/chatrooms/{roomId}/participants` | — | List room participants |
| `DELETE` | `/api/chatapp/chatrooms/{roomId}` | — | Delete room data |

### Input rules

- Room names and participant IDs are required and may contain letters, numbers, `_`, or `-` (up to 64 characters).
- Message text is required and may be up to 4,000 characters.
- The message-history limit must be between 0 and 500.

### Error responses

| Situation | HTTP status |
|---|---:|
| Duplicate room name | `409 Conflict` |
| Room does not exist | `404 Not Found` |
| Invalid JSON, fields, or query values | `400 Bad Request` |

Errors include `status: "error"` and a readable message. Validation errors also include an `errors` object with field-specific details.

## Redis data model and Pub/Sub

| Redis key/channel | Data type | Contents |
|---|---|---|
| `chat:room:{roomId}:metadata` | Hash | Room ID and creation timestamp |
| `chat:room:{roomId}:participants` | Set | Participant IDs |
| `chat:room:{roomId}:messages` | List | JSON messages in insertion order |
| `chat:messages:{roomId}` | Pub/Sub channel | New message events for that room |

Each stored message contains `participant`, `message`, and an ISO-8601 UTC `timestamp`. The application listens to `chat:messages:*` and logs received events. Redis Pub/Sub only delivers to active subscribers; message history is read from the Redis list.

To inspect a room channel manually, open a terminal with `redis-cli` and run:

```text
SUBSCRIBE chat:messages:general
```

Then send a message to `general` using the API. The subscriber terminal will display the published event.

## Run the whole stack with Docker Compose

If Docker Desktop is running, you can build and start both the API and Redis together:

```powershell
docker compose up --build
```

When both services are up, use `http://localhost:8080`. Stop the stack with `Ctrl+C`, or from another terminal run:

```powershell
docker compose down
```

This keeps the Redis volume. To remove the stored Redis data as well:

```powershell
docker compose down -v
```

## Build and run tests

```powershell
mvn test
```

Build the runnable JAR:

```powershell
mvn package
```

The JAR is created under `target/`.

## Troubleshooting

**The application cannot connect to Redis**

- Confirm Redis is running with `docker compose ps` or your Redis server's status command.
- Confirm `REDIS_HOST` and `REDIS_PORT` point to the Redis instance.
- When the API runs on your computer, use `localhost` for a local Docker Redis port mapping. When both services run in Compose, the app uses the `redis` service name automatically.

**Docker commands are not recognized**

- Install and start Docker Desktop, or run Redis outside Docker and configure `REDIS_HOST` / `REDIS_PORT`.

**A room name is already in use**

- Room names must be unique. Choose a different name, or delete the existing room before creating it again.

## Project structure

```text
src/main/java/com/example/chatapp/
├── config/       Redis connection, serializers, and Pub/Sub subscription
├── controller/   REST endpoints
├── dto/          Validated request and response models
├── exception/    Domain exceptions and HTTP error responses
├── model/        Chat message model
└── service/      Redis-backed room and message operations
src/main/resources/application.yml
```
