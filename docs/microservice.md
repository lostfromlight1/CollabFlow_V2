# CollabFlow Microservices Architecture

**Version**: 1.0.0
**Architecture**: Microservices with Event-Driven Communication
**Technologies**: Java (Spring Boot), Golang, Kafka, Redis, PostgreSQL

> **Scope update:** We build the **slim MVP** in [services-mvp.md](services-mvp.md) (**6 services**: 3 Java business + gateway + 2 Go).  
> This file remains the **endpoint / Kafka / Redis reference** and describes the older 16-service split as a **future backlog**. When owners conflict, **services-mvp.md wins** (e.g. org+workspace+project → `platform-service`).  
> Java controller patterns (`@CurrentUserId`, `@ApiId` / MDC): `.cursor/rules/java-spring.mdc` + catalog in [services-mvp.md](services-mvp.md).

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Service Assignment](#service-assignment)
3. [Technology Stack](#technology-stack)
4. [Inter-Service Communication](#inter-service-communication)
5. [Data Management](#data-management)
6. [Kafka Event Streaming](#kafka-event-streaming)
7. [Redis Usage](#redis-usage)
8. [Service Boundaries](#service-boundaries)
9. [API Gateway](#api-gateway)
10. [Infrastructure](#infrastructure)

---

## Architecture Overview

```
                    ┌─────────────────┐
                    │   API Gateway   │
                    │   (Kong/Java)   │
                    └────────┬────────┘
                             │
              ┌──────────────┴──────────────┐
              │                             │
      ┌───────▼────────┐          ┌────────▼────────┐
      │  Java Services │          │ Golang Services │
      │   (6 services) │          │  (10 services)  │
      └───────┬────────┘          └────────┬────────┘
              │                             │
    ┌─────────┼─────────┐         ┌────────┼────────┐
    │         │         │         │        │        │
┌───▼───┐ ┌─▼────┐ ┌─▼────┐ ┌───▼──┐ ┌─▼────┐ ┌─▼────┐
│  Auth │ │ Org  │ │ Work │ │ Notif│ │ Invit│ │ Real- │
│       │ │      │ │ space│ │      │ │      │ │ time │
└───┬───┘ └──┬───┘ └──┬───┘ └───┬──┘ └──┬───┘ └──┬───┘
    │        │        │        │        │        │
    └────────┴────────┴────────┴────────┴────────┘
                   │                   │
        ┌──────────▼──────────┐  ┌───▼────────────┐
        │     PostgreSQL      │  │  Redis         │
        │  (Shared Database)  │  │  (Sessions +   │
        └─────────────────────┘  │   Cache)       │
                                 └────────────────┘
                                      │
                             ┌────────▼────────┐
                             │  Kafka          │
                             │  (Core Events)  │
                             └─────────────────┘
```

---

## Service Assignment

### Java Services (6 services)

#### 1. auth-service
**Language**: Java (Spring Boot)
**Responsibility**: User authentication and authorization
**Database**: users, refresh_tokens
**Kafka Topics**: user.created, user.updated, user.deleted
**Redis**: JWT tokens, user sessions, rate limiting

**Endpoints**:
- POST /api/v1/auth/register - User registration
- POST /api/v1/auth/login - User login
- POST /api/v1/auth/logout - User logout
- POST /api/v1/auth/refresh - Refresh JWT token
- POST /api/v1/auth/verify-email - Email verification
- POST /api/v1/auth/forgot-password - Password reset request
- POST /api/v1/auth/reset-password - Password reset

**Tech Stack**:
- Spring Boot 4.1.x (see tech-stack.md)
- Spring Security 6.x
- Spring Data JPA
- PostgreSQL
- Redis
- Kafka

---

#### 2. organization-service
**Language**: Java (Spring Boot)
**Responsibility**: Organization management
**Database**: organizations, organization_members
**Kafka Topics**: organization.created, organization.updated, organization.deleted, organization.member_added, organization.member_removed
**Redis**: Organization cache, member lists

**Endpoints**:
- POST /api/v1/organizations - Create organization
- GET /api/v1/organizations - List my organizations (platform-service)
- GET /api/v1/organizations/{id} - Get organization
- PUT /api/v1/organizations/{id} - Update organization
- DELETE /api/v1/organizations/{id} - Soft-delete organization
- GET /api/v1/organizations/{id}/members - Get members
- POST /api/v1/organizations/{id}/members - Add member
- PUT /api/v1/organizations/{id}/members/{memberId} - Update member role
- DELETE /api/v1/organizations/{id}/members/{memberId} - Remove member
- GET /api/v1/roles?scope=ORGANIZATION - List organization system roles

**Tech Stack**:
- Spring Boot 4.1.x (see tech-stack.md)
- Spring Data JPA
- PostgreSQL
- Redis
- Kafka

---

#### 3. workspace-service
**Language**: Java (Spring Boot)
**Responsibility**: Workspace management
**Database**: workspaces, workspace_members
**Kafka Topics**: workspace.created, workspace.updated, workspace.deleted, workspace.member_added, workspace.member_removed
**Redis**: Workspace cache, member lists

**Endpoints**:
- POST /api/v1/workspaces - Create workspace
- GET /api/v1/workspaces/{id} - Get workspace
- PUT /api/v1/workspaces/{id} - Update workspace
- DELETE /api/v1/workspaces/{id} - Delete workspace
- GET /api/v1/workspaces/{id}/members - Get members
- POST /api/v1/workspaces/{id}/members - Add member
- PUT /api/v1/workspaces/{id}/members/{memberId} - Update member role
- DELETE /api/v1/workspaces/{id}/members/{memberId} - Remove member
- POST /api/v1/workspaces/{id}/join - Join via invite code

**Tech Stack**:
- Spring Boot 4.1.x (see tech-stack.md)
- Spring Data JPA
- PostgreSQL
- Redis
- Kafka

---

#### 4. project-service
**Language**: Java (Spring Boot)
**Responsibility**: Project management
**Database**: projects, project_members
**Kafka Topics**: project.created, project.updated, project.deleted, project.member_added, project.member_removed
**Redis**: Project cache, member lists

**Endpoints**:
- POST /api/v1/projects - Create project
- GET /api/v1/projects/{id} - Get project
- PUT /api/v1/projects/{id} - Update project
- DELETE /api/v1/projects/{id} - Delete project
- GET /api/v1/projects/{id}/members - Get members
- POST /api/v1/projects/{id}/members - Add member
- PUT /api/v1/projects/{id}/members/{memberId} - Update member role
- DELETE /api/v1/projects/{id}/members/{memberId} - Remove member

**Tech Stack**:
- Spring Boot 4.1.x (see tech-stack.md)
- Spring Data JPA
- PostgreSQL
- Redis
- Kafka

---

#### 5. task-service
**Language**: Java (Spring Boot)
**Responsibility**: Task management, custom fields, dependencies
**Database**: tasks, task_dependencies, task_custom_field_values, sprints
**Kafka Topics**: task.created, task.updated, task.deleted, task.assigned, task.status_changed
**Redis**: Task cache, sprint data

**Endpoints**:
- POST /api/v1/tasks - Create task
- GET /api/v1/tasks/{id} - Get task
- PUT /api/v1/tasks/{id} - Update task
- DELETE /api/v1/tasks/{id} - Delete task
- GET /api/v1/tasks - List tasks (with filters)
- POST /api/v1/tasks/bulk-update - Bulk update tasks
- POST /api/v1/tasks/{id}/dependencies - Add dependency
- DELETE /api/v1/tasks/{id}/dependencies/{depId} - Remove dependency
- POST /api/v1/sprints - Create sprint
- GET /api/v1/sprints/{id} - Get sprint
- PUT /api/v1/sprints/{id} - Update sprint
- DELETE /api/v1/sprints/{id} - Delete sprint

**Tech Stack**:
- Spring Boot 4.1.x (see tech-stack.md)
- Spring Data JPA
- PostgreSQL
- Redis
- Kafka

---

#### 6. workflow-service
**Language**: Java (Spring Boot)
**Responsibility**: Status workflow management
**Database**: status_workflows, workflow_statuses, workflow_transitions, task_types, custom_fields, custom_field_options, roles, permissions, role_permissions
**Kafka Topics**: None (read-only for other services)
**Redis**: Workflow cache, permissions cache

**Endpoints**:
- POST /api/v1/workflows - Create workflow
- GET /api/v1/workflows/{id} - Get workflow
- PUT /api/v1/workflows/{id} - Update workflow
- DELETE /api/v1/workflows/{id} - Delete workflow
- GET /api/v1/workflows/{id}/statuses - Get workflow statuses
- POST /api/v1/workflows/{id}/statuses - Add status
- PUT /api/v1/workflows/{id}/statuses/{statusId} - Update status
- DELETE /api/v1/workflows/{id}/statuses/{statusId} - Delete status
- POST /api/v1/workflows/{id}/transitions - Add transition
- DELETE /api/v1/workflows/{id}/transitions/{transitionId} - Delete transition
- POST /api/v1/task-types - Create task type
- GET /api/v1/task-types - List task types
- POST /api/v1/custom-fields - Create custom field
- GET /api/v1/custom-fields - List custom fields
- GET /api/v1/roles - List roles
- GET /api/v1/permissions - List permissions
- GET /api/v1/roles/{roleId}/permissions - Get role permissions

**Tech Stack**:
- Spring Boot 4.1.x (see tech-stack.md)
- Spring Data JPA
- PostgreSQL
- Redis

---

### Golang Services (10 services)

#### 1. notification-service (Priority 1 - Easy)
**Language**: Golang
**Responsibility**: Notification management
**Database**: notifications
**Kafka Topics**: Consumes task.assigned, comment.mention, project.invite, sprint.started
**Redis**: Notification cache

**Endpoints**:
- POST /api/v1/notifications - Create notification
- GET /api/v1/notifications - List user notifications
- PUT /api/v1/notifications/{id}/read - Mark as read
- PUT /api/v1/notifications/read-all - Mark all as read
- DELETE /api/v1/notifications/{id} - Delete notification

**Tech Stack**:
- Gin (see tech-stack.md)
- GORM (ORM)
- PostgreSQL
- Redis
- Kafka (segmentio/kafka-go)

---

#### 2. invitation-service (Priority 2 - Easy)
**Language**: Golang
**Responsibility**: Invitation management
**Database**: invitations
**Kafka Topics**: Consumes organization.invite, workspace.invite, project.invite
**Redis**: Invitation cache

**Endpoints**:
- POST /api/v1/invitations - Create invitation
- GET /api/v1/invitations/{token} - Get invitation by token
- POST /api/v1/invitations/{id}/accept - Accept invitation
- DELETE /api/v1/invitations/{id} - Decline invitation
- GET /api/v1/invitations - List user's invitations

**Tech Stack**:
- Gin
- GORM
- PostgreSQL
- Redis
- Kafka (segmentio/kafka-go)

---

#### 3. realtime-service (Priority 3 - Medium)
**Language**: Golang
**Responsibility**: Real-time WebSocket connections
**Database**: None
**Kafka Topics**: Consumes task.updated, comment.added, notification.created
**Redis**: Connection state, pub/sub

**Endpoints**:
- WS /api/v1/realtime - WebSocket endpoint
- POST /api/v1/realtime/broadcast - Broadcast message to room

**Tech Stack**:
- Gorilla WebSocket
- Redis (pub/sub)
- Kafka (segmentio/kafka-go)

---

#### 4. document-service (Priority 4 - Medium)
**Language**: Golang
**Responsibility**: Document management
**Database**: documents, document_folders, document_permissions, document_tags
**Kafka Topics**: document.uploaded, document.deleted
**Redis**: Document metadata cache

**Endpoints**:
- POST /api/v1/documents/upload - Upload document
- GET /api/v1/documents/{id} - Get document
- PUT /api/v1/documents/{id} - Update document
- DELETE /api/v1/documents/{id} - Delete document
- GET /api/v1/documents/{id}/download - Download document
- POST /api/v1/folders - Create folder
- GET /api/v1/folders/{id} - Get folder
- PUT /api/v1/folders/{id} - Update folder
- DELETE /api/v1/folders/{id} - Delete folder

**Tech Stack**:
- Gin
- GORM
- PostgreSQL
- Redis
- MinIO or AWS S3 (file storage)

---

#### 5. analytics-service (Priority 5 - Medium)
**Language**: Golang
**Responsibility**: Analytics and reporting
**Database**: Read-only access to all tables
**Kafka Topics**: None
**Redis**: Analytics cache

**Endpoints**:
- GET /api/v1/analytics/workspace/{id} - Workspace analytics
- GET /api/v1/analytics/project/{id} - Project analytics
- GET /api/v1/analytics/user/{id} - User analytics
- GET /api/v1/analytics/sprint/{id} - Sprint analytics

**Tech Stack**:
- Gin
- GORM
- PostgreSQL
- Redis

---

#### 6. comment-service (Priority 6 - Medium)
**Language**: Golang
**Responsibility**: Comment management
**Database**: comments, comment_attachments, comment_reactions, comment_mentions
**Kafka Topics**: comment.added, comment.updated, comment.deleted
**Redis**: Comment cache

**Endpoints**:
- POST /api/v1/comments - Create comment
- GET /api/v1/comments/{id} - Get comment
- PUT /api/v1/comments/{id} - Update comment
- DELETE /api/v1/comments/{id} - Delete comment
- GET /api/v1/comments - List comments (with filters)
- POST /api/v1/comments/{id}/reactions - Add reaction
- DELETE /api/v1/comments/{id}/reactions/{reactionId} - Remove reaction

**Tech Stack**:
- Gin
- GORM
- PostgreSQL
- Redis
- Kafka (segmentio/kafka-go)

---

#### 7. email-service (Priority 7 - Medium)
**Language**: Golang
**Responsibility**: Email queue processing
**Database**: email_queue (optional table)
**Kafka Topics**: email.send
**Redis**: Email queue, rate limiting

**Endpoints**:
- POST /api/v1/emails/queue - Queue email
- GET /api/v1/emails/status/{id} - Get email status

**Tech Stack**:
- Gin
- Redis (queue)
- Kafka (segmentio/kafka-go)
- SendGrid or Mailgun (email provider)

---

#### 8. activity-service (Priority 8 - Hard)
**Language**: Golang
**Responsibility**: Activity feed management
**Database**: activity_feed
**Kafka Topics**: Consumes all events
**Redis**: Activity cache

**Endpoints**:
- GET /api/v1/activity - List activity feed
- GET /api/v1/activity/entity/{type}/{id} - Get entity activity
- GET /api/v1/activity/user/{id} - Get user activity

**Tech Stack**:
- Gin
- GORM
- PostgreSQL
- Redis
- Kafka (segmentio/kafka-go)

---

#### 9. export-service (Priority 9 - Hard)
**Language**: Golang
**Responsibility**: Data export functionality
**Database**: Read-only access to all tables
**Kafka Topics**: None
**Redis**: Export job cache

**Endpoints**:
- POST /api/v1/export/tasks - Export tasks
- POST /api/v1/export/projects - Export projects
- POST /api/v1/export/analytics - Export analytics
- GET /api/v1/export/{id} - Get export status
- GET /api/v1/export/{id}/download - Download export

**Tech Stack**:
- Gin
- GORM
- PostgreSQL
- Redis

---

#### 10. search-service (Priority 10 - Hard)
**Language**: Golang
**Responsibility**: Search functionality
**Database**: Elasticsearch
**Kafka Topics**: Consumes task.created, task.updated, project.created, project.updated
**Redis**: Search results cache

**Endpoints**:
- GET /api/v1/search/tasks - Search tasks
- GET /api/v1/search/projects - Search projects
- GET /api/v1/search/users - Search users
- GET /api/v1/search/global - Global search

**Tech Stack**:
- Gin
- Elasticsearch (olivere/elastic)
- Redis
- Kafka (segmentio/kafka-go)

---

## Technology Stack

### Pinned stack (canonical)

See **[tech-stack.md](tech-stack.md)** — versions below are a summary only.

### Java Services
- **JDK**: 21 LTS
- **Framework**: Spring Boot **4.1.x**
- **Cloud / Gateway**: Spring Cloud **2025.1.x** (Oakwood)
- **Security**: Spring Security + JWT (managed by Boot)
- **Database**: Spring Data JPA + PostgreSQL 17
- **Kafka / Redis**: Spring Kafka, Spring Data Redis
- **API**: REST + springdoc-openapi
- **Build**: **Gradle 8.14+** (Wrapper)
- **Testing**: JUnit 5, Mockito

### Golang Services
- **Go**: **1.27.x**
- **Framework**: **Gin** only
- **Database**: GORM + PostgreSQL
- **Kafka**: segmentio/kafka-go
- **Redis**: go-redis/v9
- **WebSockets**: gorilla/websocket
- **Build**: Go modules
- **Testing**: testify

### Infrastructure
- **API Gateway**: Spring Cloud Gateway (not Kong for MVP)
- **Message Broker**: Kafka **3.9.x** + **ZooKeeper** (Confluent Platform **7.9.x**)
- **Cache**: Redis **8.x**
- **Database**: PostgreSQL **17**
- **Search / files**: deferred — [future-plan.md](future-plan.md)
- **Container**: Docker Compose v2


---

## Inter-Service Communication

### Synchronous Communication (HTTP/REST)

**Java to Java**: Direct HTTP calls via Feign or RestTemplate
**Java to Golang**: HTTP calls via RestTemplate or WebClient
**Golang to Java**: HTTP calls via net/http
**Golang to Golang**: HTTP calls via net/http

**Example**: task-service calls notification-service to send notification
```java
// Java calling Golang
@Service
public class TaskService {
    @Autowired
    private NotificationClient notificationClient;
    
    public void assignTask(UUID taskId, UUID assigneeId) {
        // ... assign task logic
        notificationClient.sendTaskAssignment(taskId, assigneeId);
    }
}
```

### Asynchronous Communication (Kafka)

**Event Publishing**: Services publish events to Kafka topics
**Event Consumption**: Services subscribe to relevant topics

**Example**: task-service publishes "task.assigned" event
```java
// Java publishing event
@Service
public class TaskService {
    @Autowired
    private KafkaTemplate<String, TaskAssignedEvent> kafkaTemplate;
    
    public void assignTask(UUID taskId, UUID assigneeId) {
        // ... assign task logic
        TaskAssignedEvent event = new TaskAssignedEvent(taskId, assigneeId);
        kafkaTemplate.send("task.assigned", event);
    }
}
```

```go
// Golang consuming event
func (s *NotificationService) consumeTaskAssigned() {
    consumer, _ := sarama.NewConsumer(...)
    consumer.Subscribe("task.assigned", nil)
    
    for {
        msg := <-consumer.Messages()
        event := parseTaskAssignedEvent(msg.Value)
        s.sendNotification(event)
    }
}
```

---

## Data Management

### Database Strategy

**Shared Database** (recommended for MVP):
- All services connect to the same PostgreSQL database
- Simpler to implement and debug
- Good for starting point
- Can migrate to database-per-service later

**Database Ownership** (slim MVP — see [database.md](database.md) and [services-mvp.md](services-mvp.md)):
- **auth-service**: users, refresh_tokens
- **platform-service**: organizations, workspaces, projects, members, roles, permissions, tags
- **task-service**: workflows, tasks, custom fields, sprints, task_tags, watchers
- **collab-service**: notifications, invitations, comments, activity_feed
- **Deferred (Phase 2)**: documents*
- **Shared database** for MVP (not database-per-service yet)

### Data Consistency

**Eventual Consistency**:
- Services publish events after data changes
- Other services consume events to update their data
- Accept slight delay in data propagation

**Example**: When task is assigned:
1. task-service updates task.assignee_id
2. task-service publishes "task.assigned" event
3. notification-service consumes event and creates notification
4. activity-service consumes event and adds to activity feed

---

## Kafka Event Streaming

### Core Events

#### User Events
- `user.created` - User account created  
  - **Producer:** `auth-service` (after commit)  
  - **Consumers (later):** `collab-service`  
  - **data:** `{ "userId": "uuid", "email": "string", "name": "string" }`
- `user.updated` - User profile updated
- `user.deleted` - User account deleted


#### Organization Events
- `organization.created` - Organization created
- `organization.updated` - Organization updated
- `organization.deleted` - Organization deleted
- `organization.member_added` - Member added to organization
- `organization.member_removed` - Member removed from organization

#### Workspace Events
- `workspace.created` - Workspace created
- `workspace.updated` - Workspace updated
- `workspace.deleted` - Workspace deleted
- `workspace.member_added` - Member added to workspace
- `workspace.member_removed` - Member removed from workspace

#### Project Events
- `project.created` - Project created
- `project.updated` - Project updated
- `project.deleted` - Project deleted
- `project.member_added` - Member added to project
- `project.member_removed` - Member removed from project

#### Task Events
- `task.created` - Task created
- `task.updated` - Task updated
- `task.deleted` - Task deleted
- `task.assigned` - Task assigned to user
- `task.status_changed` - Task status changed

### Event Schema

**Example Event Structure**:
```json
{
  "eventId": "uuid",
  "eventType": "task.assigned",
  "timestamp": "2026-10-21T10:00:00Z",
  "version": "1.0",
  "data": {
    "taskId": "uuid",
    "assigneeId": "uuid",
    "projectId": "uuid",
    "workspaceId": "uuid",
    "organizationId": "uuid",
    "assignerId": "uuid"
  }
}
```

---

## Redis Usage

### Session Management
- JWT refresh tokens storage
- User session data
- Rate limiting tokens

**Key Examples**:
```
session:{userId}:refresh_token → {token}
session:{userId}:data → {sessionData}
rate_limit:{userId}:{endpoint} → {count, expiry}
```

### Caching
- User permissions (roles + permissions)
- Organization/workspace/project data
- Workflow definitions
- Frequently accessed tasks

**Key Examples**:
```
user:{userId}:permissions → {role, permissions}
workspace:{workspaceId}:members → [member_ids]
workflow:{workflowId}:statuses → [status_ids]
task:{taskId}:details → {task_object}
```

### Cache Expiration
- Session data: 30 minutes
- Permissions: 1 hour
- Workspace/project data: 15 minutes
- Task data: 5 minutes

---

## Service Boundaries

### Java Service Boundaries

```
auth-service
├── User registration/login
├── JWT token management
└── Password reset

organization-service
├── Organization CRUD
├── Member management
└── Role assignment

workspace-service
├── Workspace CRUD
├── Member management
└── Invite code management

project-service
├── Project CRUD
├── Member management
└── Workflow assignment

task-service
├── Task CRUD
├── Custom fields
├── Dependencies
└── Sprint management

workflow-service
├── Workflow CRUD
├── Status management
├── Transition rules
├── Task types
├── Custom fields
├── Roles
└── Permissions
```

### Golang Service Boundaries

```
notification-service
├── Notification CRUD
├── Read status
└── Notification preferences

invitation-service
├── Invitation CRUD
├── Accept/decline
└── Token validation

realtime-service
├── WebSocket connections
├── Room management
└── Message broadcasting

document-service
├── Document upload/download
├── Folder management
├── Permissions
└── Version control

analytics-service
├── Workspace analytics
├── Project analytics
├── User analytics
└── Sprint analytics

comment-service
├── Comment CRUD
├── Reactions
├── Mentions
└── Attachments

email-service
├── Email queue
├── Email sending
└── Delivery tracking

activity-service
├── Activity feed
├── Entity activity
└── User activity

export-service
├── Task export
├── Project export
├── Analytics export
└── Download management

search-service
├── Task search
├── Project search
├── User search
└── Global search
```

---

## API Gateway

### Responsibilities
- Request routing to appropriate service
- Authentication and authorization
- Rate limiting
- Request/response transformation
- API versioning
- SSL termination

### Implementation Options
1. **Kong** - Feature-rich, industry standard
2. **Spring Cloud Gateway** - Java-native, good for Java services
3. **NGINX** - Lightweight, high performance

### Route Configuration

```
/api/v1/auth/* → auth-service:8081
/api/v1/organizations/* → organization-service:8082
/api/v1/workspaces/* → workspace-service:8083
/api/v1/projects/* → project-service:8084
/api/v1/tasks/* → task-service:8085
/api/v1/workflows/* → workflow-service:8086
/api/v1/notifications/* → notification-service:9081
/api/v1/invitations/* → invitation-service:9082
/api/v1/realtime/* → realtime-service:9083
/api/v1/documents/* → document-service:9084
/api/v1/analytics/* → analytics-service:9085
/api/v1/comments/* → comment-service:9086
/api/v1/emails/* → email-service:9087
/api/v1/activity/* → activity-service:9088
/api/v1/export/* → export-service:9089
/api/v1/search/* → search-service:9090
```

---

## Infrastructure

### Local Development

**Docker Compose Setup** (pins from [tech-stack.md](tech-stack.md); full compose file comes in Phase 1 infra):
```yaml
services:
  postgres:
    image: postgres:17-alpine
    ports:
      - "5432:5432"

  redis:
    image: redis:8-alpine
    ports:
      - "6379:6379"

  zookeeper:
    image: confluentinc/cp-zookeeper:7.9.2
    ports:
      - "2181:2181"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:7.9.2
    ports:
      - "9092:9092"
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      # plus listeners / advertised listeners when compose is finalized

  # Phase 2+: elasticsearch, minio — see future-plan.md
```

### Production

**Kubernetes Setup**:
- Each service in separate deployment
- Service discovery via Kubernetes DNS
- ConfigMaps for configuration
- Secrets for sensitive data
- Horizontal Pod Autoscaler
- Ingress controller for API Gateway

---

## Monitoring & Observability

### Metrics
- **Prometheus**: Collect metrics from all services
- **Grafana**: Visualize metrics and dashboards

### Logging
- **ELK Stack**: Elasticsearch, Logstash, Kibana
- **Loki**: Lightweight log aggregation

### Tracing
- **Jaeger**: Distributed tracing
- **Zipkin**: Alternative distributed tracing

### Health Checks
- Spring Boot Actuator for Java services
- Custom health endpoints for Golang services

---

## Security

### Authentication
- JWT tokens issued by auth-service
- Token validation at API Gateway
- Refresh token rotation

### Authorization
- Role-based access control (RBAC)
- Permission checks at service level
- Service-to-service authentication via API keys

### Network Security
- Service-to-service communication over internal network
- API Gateway as only public entry point
- TLS/SSL for all external communication

---

## Deployment

### CI/CD Pipeline
1. **Code Push**: Git push to repository
2. **Build**: Build Docker images for each service
3. **Test**: Run unit and integration tests
4. **Push**: Push images to container registry
5. **Deploy**: Deploy to Kubernetes cluster
6. **Verify**: Run smoke tests

### Blue-Green Deployment
- Deploy new version alongside old version
- Switch traffic gradually
- Rollback if issues detected

---

## Scalability

### Horizontal Scaling
- Each service can scale independently
- Load balancer distributes traffic
- Stateless services enable easy scaling

### Vertical Scaling
- Increase resources for CPU-intensive services
- Scale database with read replicas

---

## Disaster Recovery

### Database Backups
- Daily automated backups
- Point-in-time recovery via WAL
- Backups stored in cloud storage

### Service Redundancy
- Multiple instances of each service
- Auto-restart on failure
- Circuit breakers for external dependencies

---

**Document Version**: 1.0.0
**Last Updated**: 2026-10-21
