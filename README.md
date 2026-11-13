# Simple Chat Application Using Redis

A Java 21 and Spring Boot REST API for creating chat rooms, joining participants, sending messages, and retrieving recent chat history. Redis stores room metadata, participants, and messages; Redis Pub/Sub broadcasts new messages to active subscribers.

**The API has no user interface. This guide uses Postman for all API requests.**

## Features

- Create uniquely named chat rooms and join participants
- Send messages and retrieve the last N messages in chronological order
- Broadcast new messages through room-specific Redis Pub/Sub channels
- Validate requests and return useful errors for duplicates and missing rooms
- List room participants and delete a room
- Enable Redis AOF persistence when using the provided Docker Compose setup

## Requirements

- Java 21 or later
- Maven 3.9 or later
- Docker Desktop with Docker Compose, or a separately running Redis 7 instance
- Postman

## Start Redis and the application

### Option A: Run everything with Docker Compose

1. Start Docker Desktop.
2. Open a terminal in the project folder.
3. Run:

   ```bash
   docker compose up --build
   ```

4. Wait until both the `redis` and `app` services are running. The API is available at `http://localhost:8080`.
5. Keep this terminal open while testing. Stop the services with `Ctrl+C`.

Docker Compose enables Redis append-only file persistence and stores data in a named volume. To run the services in the background, use `docker compose up --build -d`; to stop them later, run `docker compose down`.

### Option B: Run Redis and the API separately

1. Start Redis on `localhost:6379` (or note your Redis host and port).
2. Open a terminal in the project folder and start the Spring Boot API:

   ```bash
   mvn spring-boot:run
   ```

3. The API is available at `http://localhost:8080`.

The application defaults to Redis at `localhost:6379` and API port `8080`. The Docker Compose setup configures the app container to connect to the `redis` service automatically.

## Test with Postman

### 1. Create a Postman collection

1. Open Postman and select **New → Collection**.
2. Name it **Redis Chat API**.
3. Select the collection’s **Variables** tab and add:
   - Variable: `baseUrl`; Initial value: `http://localhost:8080/api/chatapp/chatrooms`; Current value: `http://localhost:8080/api/chatapp/chatrooms`
   - Variable: `roomId`; Initial value: `general`; Current value: `general`
4. Save the collection.

Use `{{baseUrl}}` and `{{roomId}}` in the request URLs below. For each POST request, select **Body → raw → JSON**. Postman will set `Content-Type: application/json`.

### 2. Create a room

- Method: `POST`
- URL: `{{baseUrl}}`
- Body:

  ```json
  {
    "roomName": "{{roomId}}"
  }
  ```

Click **Send**. Expected: `201 Created`.

Example response:

```json
{
  "message": "Chatroom 'general' created successfully.",
  "status": "success",
  "roomId": "general"
}
```

### 3. Join the room

- Method: `POST`
- URL: `{{baseUrl}}/{{roomId}}/join`
- Body:

  ```json
  {
    "participant": "guest_user"
  }
  ```

Expected: `200 OK`.

### 4. Send a message

- Method: `POST`
- URL: `{{baseUrl}}/{{roomId}}/messages`
- Body:

  ```json
  {
    "participant": "guest_user",
    "message": "Hello, everyone!"
  }
  ```

Expected: `200 OK`. Send another message with a different message value so you can check history ordering. Joining records membership but is not an authentication check; this sample has no authentication.

### 5. Retrieve recent messages

- Method: `GET`
- URL: `{{baseUrl}}/{{roomId}}/messages?limit=10`
- No request body is needed.

Expected: `200 OK`. The response contains up to the latest 10 messages, oldest to newest within that selection. The default limit is 50; allowed values are 0–500.

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

### 6. List room participants

- Method: `GET`
- URL: `{{baseUrl}}/{{roomId}}/participants`
- No request body is needed.

Expected: `200 OK`, with the participant IDs that joined the room.

### 7. Check error handling

**Duplicate room:** Send the create-room request from Step 2 again without changing the room name. Expected: `409 Conflict`.

**Room does not exist:** Send a POST request to `{{baseUrl}}/missing-room/messages` with this JSON body. Expected: `404 Not Found`.

```json
{
  "participant": "guest_user",
  "message": "This room does not exist"
}
```

**Invalid history limit:** Send a GET request to `{{baseUrl}}/{{roomId}}/messages?limit=900`. Expected: `400 Bad Request`.

### 8. Delete a room (optional)

- Method: `DELETE`
- URL: `{{baseUrl}}/{{roomId}}`
- No request body is needed.

Expected: `200 OK`. This removes the room metadata, participant set, and message history. After deletion, room-specific operations return `404 Not Found`.

## API reference

| Method | Endpoint | Request body or query | Description |
|---|---|---|---|
| `POST` | `/api/chatapp/chatrooms` | `{"roomName":"general"}` | Create a room |
| `POST` | `/api/chatapp/chatrooms/{roomId}/join` | `{"participant":"guest_user"}` | Join a room |
| `POST` | `/api/chatapp/chatrooms/{roomId}/messages` | `{"participant":"guest_user","message":"Hello"}` | Store and publish a message |
| `GET` | `/api/chatapp/chatrooms/{roomId}/messages?limit=10` | Optional `limit` from 0 to 500; default 50 | Retrieve recent messages |
| `GET` | `/api/chatapp/chatrooms/{roomId}/participants` | — | List participants |
| `DELETE` | `/api/chatapp/chatrooms/{roomId}` | — | Delete a room and its data |

Room names and participant IDs are required, up to 64 characters, and may contain letters, numbers, underscores, and hyphens. Message text is required and limited to 4,000 characters.

## Redis data and real-time messages

| Redis key or channel | Type | Contents |
|---|---|---|
| `chat:room:{roomId}:metadata` | Hash | Room ID and creation timestamp |
| `chat:room:{roomId}:participants` | Set | Participant IDs |
| `chat:room:{roomId}:messages` | List | JSON messages in insertion order |
| `chat:messages:{roomId}` | Pub/Sub channel | New message events for that room |

Messages contain `participant`, `message`, and an ISO-8601 UTC `timestamp`. The application subscribes to `chat:messages:*` and logs received events. Redis Pub/Sub delivers events only to active subscribers; use the message list for stored history. To see the application's Pub/Sub logs while using Docker Compose, run `docker compose logs -f app` in a terminal.

## Build and tests

Run the test suite:

```bash
mvn test
```

Build the runnable JAR:

```bash
mvn package
```

The JAR is created in the `target/` directory.

## Troubleshooting

**Postman cannot connect to `localhost:8080`**

- Confirm the `app` service is running (`docker compose ps`) or that Spring Boot is still running in its terminal.
- If using Docker Compose, wait for the app to finish starting before sending the request.
- Check whether another application is already using port `8080`.

**The app cannot connect to Redis**

- Confirm Redis is running and healthy (`docker compose ps`).
- With Docker Compose, the app connects to the Redis service automatically.
- When running the app outside Compose, the default Redis address is `localhost:6379`.

**Room already exists**

- Room names must be unique. Change the `roomId` collection variable to use a different room, or delete the existing room using Step 8.

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
