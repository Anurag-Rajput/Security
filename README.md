# Security
Spring Security with PostgreSQL-backed user authentication.

## Project structure

The application uses a layered package layout under `anurag.java.security`:

```text
anurag.java.security
├── SecurityApplication.java       # Application entry point
├── ServletInitializer.java        # External servlet-container deployment
├── config/                        # Security and application configuration
├── controller/                    # HTTP endpoints
├── entity/                        # JPA entities and embedded IDs
├── repository/                    # Spring Data persistence interfaces
└── service/                       # Authentication and business logic
```

Keep the application class at the root package. `@SpringBootApplication`
automatically scans its package and all subpackages for components, entities, and
Spring Data repositories. Explicit `@EntityScan` and `@EnableJpaRepositories`
annotations are therefore unnecessary for this layout; if packages are moved
outside the root package in a future project, configure scanning explicitly.

## PostgreSQL setup

The application uses Spring Data JPA for authentication persistence. `AppUser` maps
to `security.app_users`, and `AppUserAuthority` maps to
`security.app_user_authorities`; the JPA-backed `UserDetailsService` loads both
records when Spring Security authenticates a user. Hibernate uses
`ddl-auto=validate`, so the database schema must be created before startup and is
checked for compatibility without being modified. The existing PostgreSQL tables
below remain compatible.

The application connects to the `security` database on `localhost:5432` as the dedicated `security` database role. Create the database as a PostgreSQL administrator if it does not already exist:

```sql
CREATE DATABASE security;
```

Connect to the `security` database and run the following SQL as a database administrator. The `security` role must exist for the grants; create it first if needed, replacing the example database password:

```sql
CREATE ROLE security LOGIN PASSWORD 'replace-with-a-strong-database-password';

CREATE SCHEMA IF NOT EXISTS security;

CREATE TABLE IF NOT EXISTS security.app_users (
    username      VARCHAR(100) PRIMARY KEY,
    password_hash VARCHAR(100) NOT NULL,
    enabled       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS security.app_user_authorities (
    username  VARCHAR(100) NOT NULL
        REFERENCES security.app_users (username) ON DELETE CASCADE,
    authority VARCHAR(100) NOT NULL
        CHECK (authority ~ '^ROLE_[A-Z0-9_]+$'),
    PRIMARY KEY (username, authority)
);

GRANT CONNECT ON DATABASE security TO security;
GRANT USAGE ON SCHEMA security TO security;
GRANT SELECT ON ALL TABLES IN SCHEMA security TO security;
```

Install `pgcrypto` and create example accounts. Replace the password placeholders with strong, unique passwords; do not commit real passwords or leave them in shell history.

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO security.app_users (username, password_hash)
VALUES ('user1', crypt('replace-with-a-strong-user-password', gen_salt('bf', 12)));

INSERT INTO security.app_user_authorities (username, authority)
VALUES ('user1', 'ROLE_USER');

INSERT INTO security.app_users (username, password_hash)
VALUES ('admin1', crypt('replace-with-a-different-strong-admin-password', gen_salt('bf', 12)));

INSERT INTO security.app_user_authorities (username, authority)
VALUES ('admin1', 'ROLE_ADMIN');
```

Each account needs an authority. `ROLE_USER` can access `/user`, while `ROLE_ADMIN` can access `/admin` and `/user`. Passwords are stored as BCrypt hashes, not plaintext.

### Password hashing and encoders

In the seed SQL, `gen_salt('bf', 12)` generates a random salt for the `bf` (BCrypt) algorithm with a cost factor of 12. `crypt(password, salt)` uses that salt to produce a one-way password hash. The encoded result contains the algorithm, cost, salt, and hash, so PostgreSQL can verify a later login attempt without storing the original password. A salt makes identical passwords hash differently; the work factor makes guessing expensive. It is not encryption: the hash cannot be decrypted to recover the password.

The application currently exposes Spring Security's `BCryptPasswordEncoder` as its `PasswordEncoder` bean. During form login, Spring compares the submitted password with the stored hash. BCrypt hashes produced by PostgreSQL's `crypt(..., gen_salt('bf', 12))` are compatible with this encoder. For new accounts created by application code, use the injected `PasswordEncoder.encode(rawPassword)` and save that result in `password_hash`; never save the raw password.

Choose another encoder only when you have a concrete need, such as a security-policy requirement or a planned migration. Argon2, scrypt, and PBKDF2 are common adaptive alternatives; compare their security, operational cost, and library/deployment requirements. When changing algorithms, use one encoder consistently for both password creation and login verification, and migrate existing hashes (often by re-encoding after a successful login). Spring's `DelegatingPasswordEncoder` supports mixed formats by storing an algorithm id such as `{bcrypt}` before the hash. Do not add that prefix while using the current plain `BCryptPasswordEncoder` unless the application is also changed to use a delegating encoder.

### Database connection configuration

`src/main/resources/application.yaml` does not contain a database password. It reads `DB_PASSWORD` from the process environment; `DB_URL` and `DB_USERNAME` can also override the local defaults. Set `DB_PASSWORD` to the password for the PostgreSQL `security` role before starting the application. For example, in a PowerShell terminal:

```powershell
$env:DB_PASSWORD = "your-database-role-password"
```

Set this through your IDE's run configuration or a secret manager for other environments. Do not put database credentials in source control. If a password that was previously committed or shared is real, rotate it.
