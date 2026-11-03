---
name: upstream-edc-bump
description: Upgrade Tractus-X EDC to a new upstream Eclipse EDC version (the `edc` entry in gradle/libs.versions.toml). Use for "bump EDC to 0.x", "adapt to upstream changes", failures after a dependabot EDC bump, or planning the effort of an upcoming upstream release. Covers impact analysis, compile fixes, DSP 0.8 backports, SQL migrations, settings, tests, migration guide and DEPENDENCIES.
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

# Upstream EDC bump

Upstream bumps are the largest recurring change in this repo. The 0.16.0 adaptation touched 59 files and the
0.17.0 one touched 25, followed by test and participant-context fixes. Work through the steps in order and keep a
running checklist in the PR description.

Use the `edc-upstream-sources` skill throughout (`.claude/skills/edc-upstream-sources/scripts/edc-src.sh`).

## 1. Analyse before touching anything

```bash
OLD=$(.claude/skills/edc-upstream-sources/scripts/edc-src.sh version); NEW=0.18.0   # target
.claude/skills/upstream-edc-bump/scripts/bump-impact.sh "$OLD" "$NEW" --main-only   # summary
.claude/skills/upstream-edc-bump/scripts/bump-impact.sh "$OLD" "$NEW" --diff > /tmp/edc-bump.diff  # full diffs incl. tests
```

The report lists:
- upstream types Tractus-X imports that were **removed**, **moved** or **changed**, ranked by how many Tractus-X files use them
- upstream **settings** removed or added, and removed ones Tractus-X still references
- upstream **SQL schema** changes. Each one needs a new Tractus-X Flyway migration.
- links to the release notes and the GitHub compare view

Read the upstream release notes and any breaking-change or deprecation notes there. Summarise the plan, with the
expected touchpoints, before you edit.

## 2. Bump the versions

Work on a branch cut from `origin/main` (for example `chore/upgrade-edc-$NEW`). In `gradle/libs.versions.toml`:
- set `edc` to the new version
- check whether `edc-build` (the Gradle plugin) needs a matching bump
- check whether `dsp-tck` or `dcp-tck` need a bump when the protocol or TCK changed
- fix renamed or split artifacts, including the BOMs `edc-bom-*`. `bump-impact` "NOT FOUND" lines and Gradle
  resolution errors point to them.

## 3. Make it compile

```bash
./gradlew compileJava compileTestJava compileTestFixturesJava --continue 2>&1 | grep -E 'error:|e: ' | sort | uniq -c
```

For every failing type, run `edc-src.sh diff <Type> $OLD $NEW` and adapt. Do not copy upstream code into Tractus-X
unless you are deliberately overriding it. Patterns seen in past bumps:
- Participant context is passed explicitly (`participantContextId`) into services and stores.
- Deprecated APIs or types are flagged with `@Deprecated(since = …)`; plan their replacement.
- Constructor or builder changes in `TransferProcess`, `ContractAgreement` and `DataFlowStartMessage`.

## 4. Behaviour, not just signatures

Review every **CHANGED** type that Tractus-X overrides or wraps, not only the ones that broke the build:
- **DSP 0.8 backports** (`edc-extensions/dsp/*-08`, `spi/dsp-spi-08`) mirror upstream DSP modules. Diff the
  upstream `dsp-*` counterparts and port fixes that still apply to 0.8. Wire format must not change within a DSP
  version (ADR `docs/development/decision-records/2025-02-12-dsp-downward-compatibility`).
- **DCP** (`edc-extensions/dcp/*`): scope handling, presentation verification, STS client.
- **Policy** (`cx-policy`): policy context classes, `PolicyEngine` registration, validator APIs.
- **EDR and token refresh** (`edr/*`, `dataplane-token-refresh`, `tokenrefresh-handler`).
- **Monitor/logging** (`log4j2-monitor`), when `Monitor` changed.

## 5. Database

For every upstream schema change in the report, add a new migration to `edc-extensions/migrations/connector-migration`
(`src/main/resources/migrations/connector/`). That module holds the shared control-plane and data-plane schema the
official chart uses; the 0.17 bump added `V1_9_0__Add_ClaimsColumn.sql` there for exactly the `claims` columns the
report shows. Check `git log` of `control-plane-migration` and `data-plane-migration` to see whether the
split-schema modules still need the same change. Use the next free `V…__Description.sql` and never edit a
released one (`breaking-change-guard` checks both). Keep migrations idempotent (`ADD COLUMN IF NOT EXISTS`). Run:
`./gradlew test -DincludeTags="PostgresqlIntegrationTest"`. The upgradeability-test workflow covers the rest.

## 6. Settings and Helm

For each upstream setting removed, added or renamed that operators touch, use the `setting-lifecycle` skill. It
covers the env var in `charts/*/templates`, `values.yaml`, `helm-docs` and the migration-guide row.

## 7. Tests

Run the suites CI runs (`.github/workflows/verify.yaml`), cheapest first:

```bash
./gradlew test                                            # unit tests
./gradlew test -DincludeTags="ComponentTest"
./gradlew test -DincludeTags="ApiTest"
./gradlew test -DincludeTags="PostgresqlIntegrationTest"
./gradlew -p edc-tests/e2e/<suite> test -DincludeTags="EndToEndTest"   # transfer, policy, dcp, edr-api, catalog, …
./gradlew test -DincludeTags="CompatibilityTest"          # old vs new connector
```

Also run the `dsp-tck-tests` and `dcp-tck-tests` e2e suites. Fix fixtures in `edc-tests/e2e-fixtures`, which
upstream changes break most often, rather than weakening assertions.

## 8. Docs and compliance

- Add a section to the upcoming `docs/migration/<date>-Version_<x>_<y>.md` covering operator-visible changes,
  setting and Helm renames, DB migrations and API changes.
- Update `DEPENDENCIES` via Eclipse Dash (`docs/development/release.md`) when third-party versions moved.
- Regenerate OpenAPI YAML in `resources/openapi/yaml` when Tractus-X API modules changed (swagger config in
  `build.gradle.kts`).
- Finish with `.claude/skills/breaking-change-guard/scripts/bc-scan.sh` and resolve or justify every finding.

Commit message convention: `chore: update to upstream EDC <version> and do necessary adaptations`.
