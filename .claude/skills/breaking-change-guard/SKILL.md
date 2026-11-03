---
name: breaking-change-guard
description: Review a Tractus-X EDC change set (branch, PR or working tree) for changes that break operators, adopters or other dataspace participants, and for missing follow-ups. Covers released SQL migrations, settings, Helm values and env vars, Management API paths, JSON-LD contexts, DSP/DCP wire behaviour, public SPIs, runtime composition, migration guide, helm-docs and DEPENDENCIES. Use before opening or approving a PR, when reviewing someone's PR, or when asked "is this breaking?".
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

# Breaking-change guard

Catena-X participants must be able to keep exchanging data when the other side upgrades (CX operating model).
Within a DSP version the connector must stay strictly backward compatible (ADR
`docs/development/decision-records/2025-02-12-dsp-downward-compatibility`). Operators rely on the migration guides
to upgrade. This skill finds what a change breaks and what still has to be written.

## 1. Scan

```bash
.claude/skills/breaking-change-guard/scripts/bc-scan.sh                 # vs origin/main, incl. uncommitted work
.claude/skills/breaking-change-guard/scripts/bc-scan.sh <base-ref>      # e.g. a release tag or the PR base
.claude/skills/breaking-change-guard/scripts/bc-scan.sh --strict        # exit 1 on BLOCKER/BREAKING (CI-style)
```

To review a PR: `gh pr checkout <n>`, then run the scan against its base (`gh pr view <n> --json baseRefName`).

| Severity | Meaning | Examples the scanner detects |
|---|---|---|
| BLOCKER | Must not merge as is | A Flyway migration shipped in the latest release was modified or removed; duplicate or out-of-order migration version |
| BREAKING | Operators or clients break unless handled | Setting key removed or renamed; Helm value subtree removed; env var no longer set by the chart; `@Path` removed |
| HIGH | Wire or data compatibility risk; needs targeted tests | DSP or DCP implementation changed; JSON-LD context or namespace URL changed; new DB migration; upstream EDC bumped |
| WARN | Follow-up missing | No migration-guide change; chart README not regenerated; DEPENDENCIES not updated; new setting undocumented; SPI signature changed; feature removed from a runtime |

The scanner is a heuristic. Treat each finding as a question, confirm it by reading the diff, and look beyond it
(step 2).

## 2. Judge what the scanner cannot see

- **JSON-LD and DTO shape.** Did a Management API response or a DSP message gain, lose or rename a property, or
  change its `@type` or compaction? Additions are usually fine; removals and renames are breaking. For DSP,
  compare against the compatibility test expectations in `edc-tests/compatibility-tests`.
- **Semantics.** Changed defaults, policy evaluation (which scope, which `leftOperand` accepted), state-machine
  transitions, retry or timeouts, token lifetimes, and error codes (ADR
  `2025-07-21-return-http-status-codes-in-dataplane`).
- **Persistence.** Columns read by older connector versions during rolling upgrades, or JSON stored in columns
  (for example policies and data addresses) whose structure changed.
- **Security posture.** Auth on a newly exposed endpoint, secrets in logs, sender-constrained tokens.
- **Multi-dataspace boundary.** Catena-X specifics leaking into generic `core`/`tx-*` modules, or the reverse (ADR
  `2026-01-07_multi-dataspace-support`).

## 3. Required follow-ups for a breaking or high finding

1. **Avoid the break if possible.** Deprecate instead of removing (see the `setting-lifecycle` skill), keep the
   old API path alongside the new one for at least one release (as with `v4alpha` → `v3`), and add a new migration
   instead of editing a released one.
2. **Migration guide.** Add to the upcoming `docs/migration/<date>-Version_<x>_<y>.md`. Keep its table of
   contents, one numbered section per topic, and old→new tables with the env var column. Look at the latest guide
   for the exact style.
3. **Tests that prove compatibility:**
   - DSP: `./gradlew test -DincludeTags="CompatibilityTest"` and the `edc-tests/e2e/dsp-tck-tests` suite
   - DCP: `edc-tests/e2e/dcp-tests`, `dcp-tck-tests`
   - DB: `-DincludeTags="PostgresqlIntegrationTest"`, plus the upgradeability-test workflow
   - API: `-DincludeTags="ApiTest"` and `management-tests`
4. **Housekeeping.** Run helm-docs for chart changes, regenerate DEPENDENCIES for new third-party artifacts, update
   the OpenAPI YAML for API changes, and add a decision record for architectural changes
   (`docs/development/decision-records/<yyyy-mm-dd>-<topic>/README.md`).

## 4. Report

Summarise as a table: finding, severity, evidence (`path:line`), and resolution (fixed, justified, or follow-up
issue). In a PR review, post BLOCKER and BREAKING items as review comments on the exact lines.
