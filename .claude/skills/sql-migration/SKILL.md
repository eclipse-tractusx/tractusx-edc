---
name: sql-migration
description: Create, review or debug Tractus-X EDC PostgreSQL Flyway migrations (edc-extensions/migrations). Use when a store's schema changes (new column, index, table, data fix), when upstream EDC changed a store schema, when a connector fails at startup with a Flyway or "column does not exist" error, or when asked which migration version comes next.
---

<!--
 Copyright (c) 2026 Catena-X Automotive Network e.V.
 Copyright (c) 2026 Contributors to the Eclipse Foundation

 See the NOTICE file(s) distributed with this work for additional
 information regarding copyright ownership.

 This program and the accompanying materials are made available under the
 terms of the Apache License, Version 2.0 which is available at
 https://www.apache.org/licenses/LICENSE-2.0.

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 License for the specific language governing permissions and limitations
 under the License.

 AI Disclosure: This file was entirely AI-generated.
 The AI-generated portions are made available under CC0-1.0 and not subject
 to the project's license. The human contributor has reviewed and verified
 that the code is correct.

 SPDX-License-Identifier: Apache-2.0 AND CC0-1.0
 Assisted-By: Claude Code (claude-opus-5-5)
-->

# SQL migration

Tractus-X EDC owns the database schema of the connector. Upstream EDC ships `*-schema.sql` files but no migrations, so
every upstream schema change needs a Tractus-X Flyway migration. Released migrations are immutable: Flyway checksums
them, and a modified file breaks every existing deployment on startup.

## Where migrations live

| Module | Folder | Used by |
|---|---|---|
| `connector-migration` | `src/main/resources/migrations/connector/` (`V1_<n>_0__…`) | **The official charts**: control plane and data plane share one schema. Put new work here. |
| `control-plane-migration` | `…/postgresql/migration/<store>/` (`V0_0_<n>__…`) | Split-schema deployments, one folder per store (asset, policy, contractnegotiation, transferprocess, …) |
| `data-plane-migration` | `…/postgresql/migration/<store>/` | Split-schema data plane (dataplane, accesstokendata) |
| `postgresql-migration-lib` | Java only | `AbstractPostgresqlMigrationExtension`, `FlywayManager`, `DatabaseMigrationConfiguration` |

Each store can be switched off with `tx.edc.postgresql.migration.<store>.enabled=false`, and the schema is set with
`tx.edc.postgresql.migration.schema`. See the module READMEs.

## 1. Look before you write

```bash
.claude/skills/sql-migration/scripts/migrations.sh status       # every folder: count, latest, released in <latest tag>?
.claude/skills/sql-migration/scripts/migrations.sh next connector
.claude/skills/edc-upstream-sources/scripts/edc-src.sh schema transfer-process   # upstream target schema
```

When the change comes from an upstream bump, `upstream-edc-bump/scripts/bump-impact.sh` lists the exact schema
deltas. In the 0.17 bump these were the `claims` columns on `edc_contract_agreement` and `edc_transfer_process`, which
became `V1_9_0__Add_ClaimsColumn.sql`.

## 2. Create the migration

```bash
.claude/skills/sql-migration/scripts/migrations.sh new connector "Add data address alias"
# -> .../migrations/connector/V1_10_0__Add_Data_Address_Alias.sql with license header and a TODO body
```

The script picks the next version per folder, bumping the minor (`V1_9_0` → `V1_10_0`) or the patch (`V0_0_6` →
`V0_0_7`), and refuses a version that is not newer than the latest. Use `--version` only to fill a deliberate gap.

Write the SQL so it is **idempotent and safe on large tables**:
- `ALTER TABLE … ADD COLUMN IF NOT EXISTS …`, `CREATE INDEX IF NOT EXISTS …`, `DROP … IF EXISTS`.
- Data migrations: guarded `UPDATE … WHERE <old shape>` so a rerun is a no-op. Cast JSON explicitly (`::json`)
  and keep the JSON structure the upstream store expects.
- No destructive change to data a still-running older connector reads during a rolling upgrade. Add first, remove in a
  later release.
- One concern per file, named by intent (`Add_…`, `Alter_…`, `Remove_…`).

If you also support split-schema deployments, add the equivalent change to the store folder in
`control-plane-migration` or `data-plane-migration`. Check `git log` on that folder to see whether recent upstream
changes were mirrored there.

## 3. Test

```bash
./gradlew :edc-extensions:migrations:connector-migration:test -DincludeTags="PostgresqlIntegrationTest"  # Docker
./gradlew test -DincludeTags="PostgresqlIntegrationTest"     # store tests against the migrated schema
```

`ConnectorPostgresqlMigrationTest` runs the full chain on Testcontainers Postgres. CI also runs the
upgradeability-test workflow, which deploys the last release, upgrades it, and checks the data survives.

## 4. Review checklist

- [ ] No released `V…__` file was modified or removed (`breaking-change-guard` reports this as a BLOCKER).
- [ ] Version is unique and newer than every file in the folder (`migrations.sh status` shows no duplicates).
- [ ] Statements are idempotent, and data migrations are guarded.
- [ ] Matches the upstream `*-schema.sql` of the pinned EDC version.
- [ ] Mentioned in the upcoming migration guide if operators must act: long-running migration, manual backup, or
  disabling a store migration (`migration-guide-and-adr` skill).

## Debugging startup failures

- `Validate failed: Migration checksum mismatch`: a released file was edited. Restore it from the release tag and move
  the change into a new file.
- `Detected resolved migration not applied` or `out of order`: a version lower than the latest applied one was added.
  Renumber it.
- `column … does not exist` after an upstream bump: a schema delta has no migration. Run `bump-impact.sh` and add it.
