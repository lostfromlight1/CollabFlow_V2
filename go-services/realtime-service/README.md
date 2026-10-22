# realtime-service

**Port:** 9082 · Go **1.27** / Gin / gorilla/websocket / go-redis / kafka-go  
**Owns:** no Postgres tables (Kafka + Redis only)

## What / why

Pushes live updates to browsers. Kafka events are published to Redis channels; each realtime instance listens and fans out to local WebSocket rooms. Redis keeps multi-instance ready even when you run one process.

## Protocol

Connect:

```
ws://localhost:9082/api/v1/realtime/ws?token=<JWT>
# or via gateway:
ws://localhost:8080/api/v1/realtime/ws?token=<JWT>&apiKey=<API_KEY>
```

Then send:

```json
{"action":"subscribe","room":"project:<projectId>"}
{"action":"subscribe","room":"task:<taskId>"}
```

You receive:

```json
{"type":"task.updated","room":"project:...","data":{...}}
{"type":"comment.added","room":"task:...","data":{...}}
```

## Kafka → rooms

| Topic | Rooms |
|-------|--------|
| `task.updated` | `project:{projectId}`, `task:{taskId}` |
| `comment.added` | `task:{entityId}` or `project:{entityId}` |

## Run

Redis + Kafka up.

```powershell
cd D:\Codes\CollabFlow_V2\go-services\realtime-service
go run ./cmd/server
```

Smoke: open a WS client → subscribe `project:{{projectId}}` → update a task name in Postman → see `task.updated`.

Manual publish (JWT + API key via gateway):

`POST /api/v1/realtime/broadcast` `{"room":"project:...","type":"test","data":{"ok":true}}`

## Env

| Variable | Default |
|----------|---------|
| `REALTIME_HTTP_PORT` | `9082` |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` |
| `KAFKA_GROUP_ID` | `realtime-service` |
| `JWT_SECRET` | same as auth |
