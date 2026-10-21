# api-gateway

**Port:** 8080 · **Stack:** Java 21 / Spring Cloud Gateway / Gradle  
**Role:** Route `/api/v1/**` to services; JWT validation at the edge (later).

## Run

```bash
cd ../
gradlew.bat :api-gateway:bootRun
# with auth-service also running:
curl http://localhost:8080/api/v1/auth/health
```

Routes: [docs/services-mvp.md](../../docs/services-mvp.md).
