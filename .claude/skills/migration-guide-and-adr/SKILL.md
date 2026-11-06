---
name: migration-guide-and-adr
description: Write Tractus-X EDC migration guides (docs/migration/*.md) and architecture decision records (docs/development/decision-records/*/README.md) in the repository's exact format, drafted from evidence (breaking-change scan, upstream version change, new DB migrations, commits since the last release). Use when preparing a release, when a PR introduces an operator-visible change, or when a design decision needs to be recorded before implementation.
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

# Migration guide and ADR

Two kinds of documents carry most of this project's knowledge:
- **Migration guides** (`docs/migration/<yyyy>_<mm>-Version_<a.b>.x_<c.d>.x.md`), one per minor release. They tell
  operators and adopters what they must change. Most of their content is old→new tables.
- **Decision records** (`docs/development/decision-records/<yyyy-mm-dd>-<topic>/README.md`). They explain *why*
  before the code lands. Recent examples: multi-dataspace support, VP caching, DID service registration, refresh-token
  retirement.

## Migration guide

### 1. Draft from evidence

```bash
.claude/skills/migration-guide-and-adr/scripts/draft-migration-guide.sh --branch HEAD          # print the draft
.claude/skills/migration-guide-and-adr/scripts/draft-migration-guide.sh --branch HEAD --write  # create the file if absent
```

It compares the newest release tag (e.g. `0.13.0`) with the branch and collects:
- the breaking-change scan (`bc-scan.sh`): removed settings, Helm values and env vars, API paths, DSP/DCP and JSON-LD
  changes
- the upstream EDC version change, if any
- Flyway migrations added since the release
- feat/fix/refactor commits since the release, with breaking (`!`) commits called out

Everything lands in an `EVIDENCE` comment block. Turn the relevant items into sections, then delete the block.

### 2. Write it in the house style

Copy the structure of the latest guide (`docs/migration/2026_06-Version_0.12.x_0.13.x.md`):
- Title `# Migration Guide \`a.x -> b.x\``, the standard two-paragraph intro, and a `<!-- TOC -->` list.
- **One numbered `##` section per topic** (`## 3. Postgres Version`, `## 5. \`IATP\` renamed to \`DCP\``).
- **Tables for renames**, always including the env var:

  | Old value | New value | New environment variable |
  |---|---|---|
  | `tx.edc.iam.iatp.bdrs.server.url` | `tx.edc.iam.dcp.bdrs.server.url` | `TX_EDC_IAM_DCP_BDRS_SERVER_URL` |

  and for Helm: `| Old Helm value | New Helm value |`, plus API paths: `| Old API | New API |`.
- Say **what operators must do** and what happens if they don't. Mark mandatory steps with `> [!IMPORTANT]`.
- Link the extension README or decision record for details. Do not repeat them.
- Upstream changes appear only where operators or adopters notice them.

Use `setting-lifecycle/scripts/setting-trace.sh <old-key>` to fill rename rows accurately (key, env var, Helm
path).

## Decision record

```bash
.claude/skills/migration-guide-and-adr/scripts/new-adr.sh "Verifiable presentation cache eviction" --holder "Cofinity-X GmbH"
```

This creates `docs/development/decision-records/<today>-<slug>/README.md` with the structure the records use:

- `# Title`
- `## Decision`: what will be done, in one paragraph.
- `## Rationale`: why, with the constraints (CX standards, DSP/DCP specs, upstream behaviour) and rejected
  alternatives.
- `## Approach`: SPIs and extensions (generic vs `cx-`), configuration, API, persistence, compatibility. Optional
  `### …` subsections and diagrams (mermaid is used in existing records).
- `## NOTICE`: CC-BY-4.0, with `SPDX-FileCopyrightText: <year> <holder>`.

The script also lists existing records with overlapping words, so a decision can reference or supersede them.

Process: open the decision record as its own PR (`docs: …`) for review **before** the implementation PR. The
implementation PR links it, and the migration guide links it when the change is operator-visible.

## Checklist before a release

- [ ] `draft-migration-guide.sh` evidence is fully triaged: each item is in the guide or consciously dropped.
- [ ] Every removed or renamed setting, Helm value, env var and API path has a table row.
- [ ] DB migrations that need operator attention are explained.
- [ ] Upstream EDC bump consequences are covered.
- [ ] The TOC matches the sections, and links resolve.
