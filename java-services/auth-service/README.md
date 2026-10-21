# auth-service

**Port:** 8081 · **Stack:** Java 21 / Spring Boot 4.1 / Gradle  
**Owns:** `users`, `refresh_tokens` ([docs/database.md](../../docs/database.md))

## Run

```bash
cd ../
gradlew.bat :auth-service:bootRun
curl http://localhost:8081/api/v1/auth/health
```

Status: Week 2 skeleton (health stub). Real auth = Week 3–4.
