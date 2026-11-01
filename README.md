# Security
Spring Security with PostgreSQL-backed user authentication.

## PostgreSQL setup

The application expects a PostgreSQL database and a dedicated, read-only connection account. In a PostgreSQL console, create the account and database (replace the example database password):

```sql
CREATE ROLE security_app LOGIN PASSWORD 'replace-with-a-strong-database-password';
CREATE DATABASE security_db;
```

Connect to `security_db` as a database administrator, then create the application schema and tables:

```sql
CREATE SCHEMA security;

CREATE TABLE security.app_users (
    username VARCHAR(100) PRIMARY KEY,
    password_hash VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE security.app_user_authorities (
    username VARCHAR(100) NOT NULL REFERENCES security.app_users (username) ON DELETE CASCADE,
    authority VARCHAR(100) NOT NULL CHECK (authority ~ '^ROLE_[A-Z0-9_]+$'),
    PRIMARY KEY (username, authority)
);

GRANT CONNECT ON DATABASE security_db TO security_app;
GRANT USAGE ON SCHEMA security TO security_app;
GRANT SELECT ON ALL TABLES IN SCHEMA security TO security_app;
```

To add an initial account, `pgcrypto` can generate a BCrypt-compatible hash. Replace the example password before running this and do not use it as a real credential:

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO security.app_users (username, password_hash)
VALUES ('admin', crypt('replace-with-a-strong-user-password', gen_salt('bf', 12)));

INSERT INTO security.app_user_authorities (username, authority)
VALUES ('admin', 'ROLE_ADMIN');
```

Run the seed statements as the database administrator (the application account is read-only). Use `ROLE_USER` for a regular user. The application stores password hashes, not plaintext passwords; each account needs at least one authority. `ROLE_USER` can access `/user`, while `ROLE_ADMIN` can access both `/user` and `/admin`.

Configure the application's PostgreSQL connection through environment variables (these are database login details, separate from application user accounts):

```text
DB_URL=jdbc:postgresql://localhost:5432/security_db
DB_USERNAME=security_app
DB_PASSWORD=<the database account password>
```
