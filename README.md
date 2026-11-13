# Simple Chat Application Using Redis

A Java 21 / Spring Boot REST API for creating chat rooms, joining participants, sending messages, and reading recent history. Redis stores room metadata in hashes, participants in sets, and chat history in lists. New messages are also published to a room-specific Redis Pub/Sub channel.

## 1. What you need

- Java 21 or later
- Maven 3.9+
- Redis 7 (Docker Desktop is the easiest option on Windows)
- Git (only needed to push this project to GitHub)

Check Java and Maven in PowerShell:

```powershell
java -version
mvn -version
```

## 2. Start Redis

### Option A: Docker Desktop

Install and start Docker Desktop, then from this project directory run:

```powershell
docker compose up -d redis
docker compose ps
```

Redis is exposed on `localhost:6379` and uses an append-only file with a named Docker volume for persistence.

### Option B: Existing Redis

Start Redis separately and make sure it listens on port `6379`, or set `REDIS_HOST` and `REDIS_PORT` to match your Redis instance.

## 3. Run the API locally

Keep Redis running, open a PowerShell terminal in the project directory, and run:

```powershell
mvn spring-boot:run
```

The API starts at `http://localhost:8080`. To use another Redis host/port or API port:

```powershell
$env:REDIS_HOST = "localhost"
$env:REDIS_PORT = "6379"
$env:SERVER_PORT = "8080"
mvn spring-boot:run
```

## 4. Try the API with PowerShell

The common base URL is `http://localhost:8080/api/chatapp/chatrooms`.

### Create a room

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/chatapp/chatrooms" `
  -ContentType "application/json" `
  -Body '{"roomName":"general"}'
```

Expected: HTTP `201 Created`, with `status: success` and `roomId: general`.

### Join the room

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/chatapp/chatrooms/general/join" `
  -ContentType "application/json" `
  -Body '{"participant":"guest_user"}'
```

### Send messages

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/chatapp/chatrooms/general/messages" `
  -ContentType "application/json" `
  -Body '{"participant":"guest_user","message":"Hello, everyone!"}'

Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/chatapp/chatrooms/general/messages" `
  -ContentType "application/json" `
  -Body '{"participant":"another_user","message":"Hi!"}'
```

Sending a message does not require a prior join. Joining records membership; it is not an authentication check.

### Get the most recent messages

```powershell
Invoke-RestMethod -Method Get `
  -Uri "http://localhost:8080/api/chatapp/chatrooms/general/messages?limit=10"
```

The response contains up to the last `limit` messages, oldest to newest among that selected tail. The default is 50; allowed range is 0–500.

### List room participants

```powershell
Invoke-RestMethod -Method Get `
  -Uri "http://localhost:8080/api/chatapp/chatrooms/general/participants"
```

### Delete a room (extra endpoint)

```powershell
Invoke-RestMethod -Method Delete `
  -Uri "http://localhost:8080/api/chatapp/chatrooms/general"
```

This removes the metadata, participant, and message keys for that room.

## 5. API reference

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/chatapp/chatrooms` | Create a room using `{"roomName":"general"}` |
| `POST` | `/api/chatapp/chatrooms/{roomId}/join` | Join using `{"participant":"guest_user"}` |
| `POST` | `/api/chatapp/chatrooms/{roomId}/messages` | Send using `{"participant":"guest_user","message":"Hello"}` |
| `GET` | `/api/chatapp/chatrooms/{roomId}/messages?limit=10` | Get the last N messages |
| `GET` | `/api/chatapp/chatrooms/{roomId}/participants` | List participants |
| `DELETE` | `/api/chatapp/chatrooms/{roomId}` | Delete a room |

Room names and participant IDs are limited to 64 letters, numbers, underscores, and hyphens. Message text is limited to 4,000 characters.

### Errors

- Duplicate room: HTTP `409 Conflict`
- Unknown room: HTTP `404 Not Found`
- Invalid JSON or request fields: HTTP `400 Bad Request`

Error responses use `status: error` and a readable `message`; validation errors also include an `errors` map.

## 6. Redis keys and real-time events

- `chat:room:{roomId}:metadata` — hash containing `roomId` and `createdAt`
- `chat:room:{roomId}:participants` — set of participant IDs
- `chat:room:{roomId}:messages` — list of JSON-serialized messages
- `chat:messages:{roomId}` — Pub/Sub channel carrying each new message

The application subscribes to `chat:messages:*` and logs received message events. Redis Pub/Sub is transient: it broadcasts to active subscribers and does not retain events. The list is the durable chat history (subject to Redis persistence configuration).

For a manual Pub/Sub check, run `redis-cli` and subscribe before sending a message:

```text
SUBSCRIBE chat:messages:general
```

In another terminal, POST a message to the `general` room and observe the published JSON in the subscriber terminal.

## 7. Run with Docker Compose

To build and start both Redis and the API:

```powershell
docker compose up --build
```

Then use `http://localhost:8080`. Stop the containers with `Ctrl+C`, or run:

```powershell
docker compose down
```

The Redis volume remains after `docker compose down`; to also delete stored Redis data, run `docker compose down -v`.

## 8. Build and tests

```powershell
mvn test
mvn package
```

## 9. Project layout

```text
src/main/java/com/example/chatapp/
  config/       Redis connection, serializers, Pub/Sub listener setup
  controller/   REST endpoints
  dto/          Request and response records
  exception/    Domain errors and HTTP error mapping
  model/        ChatMessage model
  service/      Redis-backed chat room operations
src/main/resources/application.yml
```

## 10. Push to the GitHub repository

The remote repository URL supplied for this project is:
`https://github.com/imrajeevnayan/Simple-Chat-Application-Using-Redis.git`

From the repository folder, inspect the current branch and remotes before pushing:

```powershell
git status
git remote -v
```

If this folder has no Git metadata yet, initialize it and add the remote:

```powershell
git init
git branch -M main
git remote add origin https://github.com/imrajeevnayan/Simple-Chat-Application-Using-Redis.git
```

Stage and review the changes, then commit:

```powershell
git add .
git status
git commit -m "Build Redis-backed chat application"
```

Before the first push, check whether GitHub already has a `main` branch:

```powershell
git ls-remote --heads origin
```

If the output lists `refs/heads/main`, fetch and integrate that branch before pushing:

```powershell
git fetch origin
git pull --rebase origin main
```

Resolve any conflicts, then push:

```powershell
git push -u origin main
```

GitHub may prompt for authentication. Use Git Credential Manager or a personal access token when prompted; do not put a token in the remote URL or commit it to the project. If Git reports that `origin` already exists, inspect `git remote -v` and update it with `git remote set-url origin <repository-url>` only if it points somewhere else.
