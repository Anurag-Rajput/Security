# Security

Spring Security authenticates users against LDAP. Login names are resolved as
`uid={username},ou=people`; group membership is read from `ou=groups` using the
group's `member` attribute. The group's `cn` becomes its role, prefixed with
`ROLE_`. For example, a member of the `USER` group can access `/user`, while a
member of the `ADMIN` group can access both `/admin` and `/user`.

## LDAP configuration

The application reads these settings from the environment, with local defaults
for an LDAP directory listening on port 8389:

| Environment variable | Default | Description |
| --- | --- | --- |
| `LDAP_URL` | `ldap://localhost:8389` | LDAP server URL |
| `LDAP_BASE` | `dc=springframework,dc=org` | Directory base DN |
| `LDAP_BIND_DN` | empty | Optional service account DN for directory searches |
| `LDAP_BIND_PASSWORD` | empty | Password for the optional service account |

Set the environment for a local run in PowerShell:

```powershell
$env:LDAP_URL = "ldaps://ldap.example.com:636"
$env:LDAP_BASE = "dc=example,dc=com"
$env:LDAP_BIND_DN = "uid=security-app,ou=service-accounts,dc=example,dc=com"
$env:LDAP_BIND_PASSWORD = "retrieve-this-from-a-secret-manager"
```

This setup supports LDAPS URLs; ensure the server certificate's issuing CA is
trusted by the JVM. StartTLS is not enabled by this configuration. Do not send
user credentials over an unencrypted connection, and keep bind credentials out
of source control.

The directory should contain users with a `uid`, `cn`, `sn`, and
`userPassword`, located below `ou=people`. Groups should be below `ou=groups`,
have a `cn`, and list member user DNs in `member`. A sample directory export
matching the defaults is provided in
[`src/main/resources/ldap-data.ldif`](./src/main/resources/ldap-data.ldif).
It creates `ldap-user` in the `USER` group and `ldap-admin` in the `ADMIN`
group. Both sample accounts use `change-me-now`; these are development-only
credentials. Replace them before importing anywhere outside a disposable local
environment.

### Import the sample entries

Start an LDAP server configured with the `dc=springframework,dc=org` base
(suffix) on port `8389`. Import from the repository root using the directory
administrator DN and password configured for your LDAP server; the example
administrator DN below is common for OpenLDAP but is not universal:

```powershell
ldapadd -x -H ldap://localhost:8389 `
  -D "cn=admin,dc=springframework,dc=org" -W `
  -f .\src\main\resources\ldap-data.ldif
```

The base DN must already exist in the LDAP server. If you use another base DN,
change the entry DNs in the LDIF and set `LDAP_BASE` to match. The application
bind account (`LDAP_BIND_DN`) is for directory searches; it is separate from
the administrator credentials used by `ldapadd`.

### Start the application

With LDAP running and the entries imported, use the matching defaults or set
the connection explicitly, then start Spring Boot:

```powershell
$env:LDAP_URL = "ldap://localhost:8389"
$env:LDAP_BASE = "dc=springframework,dc=org"
.\mvnw.cmd spring-boot:run
```

Open `http://localhost:8080/login` and sign in with `ldap-user` or `ldap-admin`
and the sample password above. Use LDAPS and directory-managed credentials
outside local development.

## Tests

The tests start an in-memory LDAP server, so no external LDAP server or
credentials are required:

```powershell
.\mvnw.cmd test
```
