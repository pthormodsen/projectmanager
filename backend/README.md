# Project Manager Backend

Spring Boot API for the Project Manager kanban app.

## Local Development

```sh
./mvnw spring-boot:run
```

The default profile uses an in-memory H2 database and disables application auth for local development.

## Production

Production deployment is handled from the parent `Projectmanager` folder:

```sh
cd ..
./deploy.sh
```

The parent `docker-compose.yml` builds this backend, the frontend, and PostgreSQL together.
