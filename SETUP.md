# Accounts, storage, and Railway setup

## Run locally

Use Java 17+ and Maven 3.6.3+:

```sh
mvn clean package
java -jar target/atelier-1.0.0.jar
```

Open http://localhost:8080 and choose **Create an account**. Each account receives its own 12 sample pieces and one sample look. Registration automatically signs in from the browser. Passwords require 12 characters and are limited to 72 UTF-8 bytes; email addresses are case-insensitive.

Local development uses an H2 database at `data/atelier.mv.db`. Accounts, closet snapshots, and uploaded image bytes persist in this database. Stop the app before copying the database for backup. Do not commit `data/` or credentials. The application does not load `.env` files automatically; set environment variables in the shell or hosting service.

## Database design

- `app_user`: unique normalized email address and BCrypt password hash.
- `user_closet`: one JSON snapshot per account, held in a database TEXT column. This preserves the existing wardrobe/outfit model without a full rewrite.
- `garment_image`: image bytes plus their owner's email. Image reads always check the signed-in owner.
- Flyway applies versioned migrations from `src/main/resources/db/migration` on startup.

Updates lock the account's closet row inside a transaction; image changes and closet changes commit together. Ownership comes from the authenticated session, never a browser-supplied user ID. Closet snapshots fit the current small personal-wardrobe application. Large catalogs may warrant separate garment/look tables and object storage later.

The former shared `data/closet.json` and `data/uploads/` are **not imported or deleted**. Back them up if they contain real clothes. They have no account ownership; importing them needs an explicit target account and a separate migration. A new account starts with samples only.

## Railway deployment (not provisioned by this change)

1. Add the GitHub repository as a Railway app service. Use the included Dockerfile and `railway.json`.
2. Add a PostgreSQL service in the same Railway project.
3. In the **app service**, set `SPRING_PROFILES_ACTIVE=production` and add reference variables for `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, and `PGPASSWORD` from the PostgreSQL service. For a service named `Postgres`, a reference looks like `${{Postgres.PGHOST}}`; use the actual service name for all five variables.
4. Deploy one app replica. Railway supplies `PORT`; the application reads it automatically. `/health` checks the database and is configured as the deployment health check.
5. Generate a public HTTPS domain for the app. Production session cookies require HTTPS. The frontend and API share this origin; no separate frontend deployment is needed.
6. Create two accounts and confirm their wardrobes, uploads, and saved looks stay separate. Restart/redeploy the app and verify that the data remains in PostgreSQL.
7. Enable database backups in the hosting setup. Back up PostgreSQL, including image bytes. No app filesystem volume is needed in production.

The production profile deliberately requires PostgreSQL variables instead of silently falling back to local storage. It builds a JDBC URL from Railway's PG variables; a raw `postgresql://` DATABASE_URL is not a JDBC URL. Local alternatives may use `DB_URL`, `DB_USER`, and `DB_PASSWORD` with a JDBC URL.

Official references: [Railway PostgreSQL](https://docs.railway.com/databases/postgresql), [Spring Security CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).

## Sessions and scope

Authentication uses HttpOnly, SameSite=Lax session cookies, CSRF tokens for writes, session-ID rotation at login, and logout invalidation. Credentials are not stored in browser localStorage. Sessions expire after 30 minutes of inactivity and are held in application memory; a restart signs users out without removing their data. Multiple replicas require a shared session store before enabling them.

This first account flow has no email verification, password reset, social login, or abuse-rate limiting. Email is an account identifier, not verified ownership of an email inbox. These can be added as a separate account-management phase.

## Validation

`mvn test` covers account validation, password hashing, incorrect passwords, session/CSRF behavior, cross-account wardrobe/image/look isolation, and the original outfit and upload rules. JavaScript syntax can be checked with `node --check src/main/resources/static/app.js`. The automated suite uses H2 in PostgreSQL compatibility mode; it does not replace a real Railway/PostgreSQL deployment smoke test.
