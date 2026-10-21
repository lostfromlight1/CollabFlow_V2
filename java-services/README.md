# Java services (multi-module Gradle)

Spring Boot **4.1.1**, JDK **21**, Gradle **8.14.3** Wrapper.  
Versions: [docs/tech-stack.md](../docs/tech-stack.md).

## Modules

| Module | Port | Role |
|--------|------|------|
| `api-gateway` | 8080 | Spring Cloud Gateway |
| `auth-service` | 8081 | Users, JWT (Week 3+) |
| `platform-service` | 8082 | Org / workspace / project (Week 5+) |
| `task-service` | 8083 | Tasks / workflows (Week 8+) |

## Why one Gradle build?

All Java services share the same Boot/Java version. From `java-services/` you can build everything or run one module.

## Commands (from this folder)

```bash
# Build all
./gradlew build          # Linux/macOS
gradlew.bat build        # Windows

# Run one service
./gradlew :auth-service:bootRun
./gradlew :platform-service:bootRun
./gradlew :task-service:bootRun
./gradlew :api-gateway:bootRun
```

## Week 2 smoke test

1. Start infra (`infrastructure/`).
2. Terminal A: `gradlew.bat :auth-service:bootRun`
3. Terminal B: `gradlew.bat :api-gateway:bootRun`
4. Check:

```bash
curl http://localhost:8081/api/v1/auth/health
curl http://localhost:8080/api/v1/auth/health
```

Both should return JSON with `"status":"UP"`. The second call proves the **gateway** proxies to auth.

## Conventions

- Packages: `com.collabflow.<service>`
- REST under `/api/v1/...`
- Actuator: `/actuator/health`
- See `.cursor/rules/java-spring.mdc`
