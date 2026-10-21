# Java services

Spring Boot **4.1.x**, JDK **21**, **Gradle** Wrapper. Versions: [docs/tech-stack.md](../docs/tech-stack.md).

| Service | Port | Role |
|---------|------|------|
| [api-gateway](api-gateway/) | 8080 | Spring Cloud Gateway |
| [auth-service](auth-service/) | 8081 | Users, JWT |
| [platform-service](platform-service/) | 8082 | Org + workspace + project |
| [task-service](task-service/) | 8083 | Tasks + workflows |

**Conventions:** see `.cursor/rules/java-spring.mdc`.  
**Build order:** [docs/implementation-guide.md](../docs/implementation-guide.md).

Skeletons (Gradle/`bootRun`) land in Week 2. Do not invent extra Java services — slim MVP only.
