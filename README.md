# Project Manager

Self-hosted project manager with a Spring Boot API, PostgreSQL database, and Vite/React frontend.

## Structure

```text
backend/    Spring Boot API
frontend/   React frontend served by nginx
```

## Local Checks

```sh
cd backend
./mvnw test

cd ../frontend
npm run lint
npm run build
```

## Production

1. Copy `.env.example` to `.env`.
2. Replace all passwords.
3. Start the stack:

```sh
docker compose up -d --build
```

The frontend listens on `localhost:3001` and proxies `/api` plus `/actuator/health` to the backend container.

For your Cloudflare Tunnel, point `projectmanager.patreek.no` to:

```yaml
service: http://localhost:3001
```

Deploy updates with:

```sh
./deploy.sh
```
