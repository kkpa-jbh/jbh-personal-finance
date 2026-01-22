# Share with Claude

When you want to share your schema with me in the future, just run:

`make export-all-schemas`

Then you can reference the file at `docs/database-schemas/all-schemas.sql` in our conversation, and I'll be able to see your complete database structure!

# Database Schema Export

This document explains how to export and maintain database schema documentation for the JBH Personal Finance application.

## Quick Start

### Export All Schemas

```bash
make export-all-schemas
```

This will create schema files in `docs/database-schemas/`:

- `acctmgmt-schema.sql` - Account management schema
- `notification-schema.sql` - Notification schema
- `all-schemas.sql` - Combined file with all schemas

### Export Individual Schema

```bash
# Export only account schema
make export-account-schema

# Export only notification schema
make export-notification-schema
```

## Manual Script Usage

You can also run the export script directly:

```bash
# Export all schemas
./scripts/export-schema.sh all

# Export specific schema
./scripts/export-schema.sh acctmgmt
./scripts/export-schema.sh notification

# Export to custom directory
./scripts/export-schema.sh all /path/to/output
```

## Environment Variables

The script uses these environment variables (with defaults):

| Variable            | Default       | Description       |
|---------------------|---------------|-------------------|
| `DATABASE_HOST`     | `localhost`   | PostgreSQL host   |
| `DATABASE_PORT`     | `5432`        | PostgreSQL port   |
| `DATABASE_NAME`     | `jbh_finance` | Database name     |
| `DATABASE_USERNAME` | `jbh_admin`   | Database username |
| `DATABASE_PASSWORD` | `raspukk`     | Database password |

## Keeping Schema Up-to-Date

### After Liquibase Migrations

After running Liquibase migrations (adding new changelog files), export the schema:

```bash
# Run your application (which runs Liquibase migrations)
mvn quarkus:dev

# Or apply migrations explicitly
mvn liquibase:update

# Then export the updated schema
make export-all-schemas
```

### Automated Export

To automatically export schemas after recreating them:

```bash
make recreate-all-schemas && make export-all-schemas
```

### Add to Git Workflow

You can add schema export to your git pre-commit hook to keep schemas in sync:

```bash
# .git/hooks/pre-commit
#!/bin/bash
make export-all-schemas
git add docs/database-schemas/
```

## Schema File Format

Each exported schema file includes:

- Header with export metadata (date, database name, schema name)
- All table definitions
- Sequences
- Indexes
- Constraints
- Views (if any)

The files are formatted with:

- No owner information (`--no-owner`)
- No privilege grants (`--no-privileges`)
- Schema-only structure (`--schema-only`)

## Troubleshooting

### pg_dump command not found

Install PostgreSQL client tools:

```bash
# macOS
brew install postgresql

# Ubuntu/Debian
sudo apt-get install postgresql-client
```

### Connection failed

Verify your database is running:

```bash
make check-all-connections
```

Check environment variables are set correctly.

### Permission denied

Ensure the export script is executable:

```bash
chmod +x scripts/export-schema.sh
```

## Sharing Schema

To share the schema with others (like Claude Code), use the combined file:

```bash
cat docs/database-schemas/all-schemas.sql
```

Or share individual schema files as needed.
