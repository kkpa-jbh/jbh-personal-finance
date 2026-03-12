---
name: SQL always goes in a new migration file
description: Every SQL statement must be added as a new migration file, never inline or in existing files
type: feedback
---

Always create a new Liquibase migration file for any SQL change (INSERTs, ALTERs, etc.). Never write SQL directly in chat or add it to an existing migration file.

**Why:** Liquibase tracks applied changesets by file; modifying existing files breaks idempotency and can corrupt migration history.

**How to apply:**
1. Create `NNN_descriptive_name.sql` in the appropriate `*-infra` module under `src/main/resources/db/changelog/`
2. Register it in the module's master XML (e.g., `finance-db-master.xml`)
3. If unsure which module the migration belongs to, ask the user before proceeding
