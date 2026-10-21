# Postman — CollabFlow

## Import

1. Open Postman → **Import**
2. Add `CollabFlow-Auth.postman_collection.json`
3. Add `CollabFlow-Local.postman_environment.json`
4. Select environment **CollabFlow Local**

## Auth flow

1. **Health** — checks service + `X-API-Key`
2. **Register** or **Login** — saves `accessToken` / `refreshToken` into collection vars
3. **Me** — needs both `X-API-Key` and `Authorization: Bearer {{accessToken}}`
4. **Refresh** / **Logout** as needed

Swagger UI (same key via Authorize): http://localhost:8081/swagger-ui.html

## Headers every call needs

| Header | Value |
|--------|--------|
| `X-API-Key` | same as env `API_KEY` |
| `Authorization` | `Bearer <accessToken>` on `/me` (and later protected routes) |

Login is email + password only for now; organization will be added later.
