# Deployment

## 1. Local (Docker Compose)

```bash
cp .env.example .env              # edit: passwords + JWT_SECRET (openssl rand -base64 48)
docker compose up --build -d
docker compose ps                 # mysql "healthy", backend + frontend running
```

- App: http://localhost:8082
- Swagger: http://localhost:8082/swagger-ui.html
- Health: `docker compose exec backend bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080' && echo up`

`SPRING_PROFILES_ACTIVE=dev` loads the DEMO data and enables the simulated inbox. With `NOTIFICATION_MODE=email`, OTP e-mails go to **Mailpit** at http://localhost:8025 (a free local mail catcher). Flyway runs automatically at start-up, applying V1 (schema) and V2 (demo data, dev only).

## 2. Without Docker (development)

```bash
# MySQL 8 running locally with database "milansetu" and a user
cd backend && DB_USERNAME=... DB_PASSWORD=... mvn spring-boot:run -Dspring-boot.run.profiles=dev
cd frontend && npm ci && npm run dev      # http://localhost:5173 (proxies /api to :8080)
```

## 3. Production (free/low-cost single VM)

**Ask before spending money.** No paid service is required by the code.

1. Get a small Linux VM (e.g. a free-tier instance) and install Docker.
2. Clone the repo. Create `.env` with strong secrets and `SPRING_PROFILES_ACTIVE=prod`.
3. HTTPS: put **Caddy** in front. It gets Let's Encrypt certificates automatically:
   ```
   your-domain.example {
       reverse_proxy localhost:8081
   }
   ```
   Then set `CORS_ALLOWED_ORIGINS=https://your-domain.example`.
4. Never add `ports:` to the `mysql` service.
5. Create the first admin. The prod profile has no demo users:
   ```sql
   INSERT INTO users(full_name,email,password_hash,role)
   VALUES ('Event Admin','you@example.com','<BCrypt hash of a strong password>','ADMIN');
   ```
6. Set `NOTIFICATION_MODE=email` and the SMTP variables (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`). Any free-tier SMTP provider works. **Ask before using a paid one.**
7. HTTPS is **mandatory**: browsers only allow camera QR scanning on HTTPS.
8. Label the site clearly as a **demonstration**, not an official child-safety system.

**Deployment URL:** none yet. Nothing has been deployed or verified, so no live link is claimed.

## 4. Backup and restore

```bash
# backup (daily via cron)
docker compose exec -T mysql sh -c 'mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" --single-transaction milansetu' > backup-$(date +%F).sql
# restore
docker compose exec -T mysql sh -c 'mysql -u root -p"$MYSQL_ROOT_PASSWORD" milansetu' < backup-2026-11-09.sql
```

Keep backups off the server, and test a restore at least once.

## 5. Migrations

- Never edit an applied migration. Add `V3__...sql`.
- Flyway records applied versions in `flyway_schema_history`.
- Hibernate is set to `ddl-auto: none`; Flyway owns the schema.
