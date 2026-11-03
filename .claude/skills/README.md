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

# Connector Sensei skills

Agent skills for developing Tractus-X EDC. Each skill is a folder with a `SKILL.md` (the procedure, loaded on demand by
Claude Code and GitHub Copilot from `.claude/skills/`) and usually a `scripts/` folder with a portable, tested script
(bash 3.2+, macOS and Linux). The [Connector Sensei agent](../agents/connector-sensei.md) routes matching tasks to them.

Scripts run from anywhere inside the repository. They are read-only on the repository, except the explicit scaffolding
commands (`scaffold-extension.sh`, `migrations.sh new`, `e2e.sh new`, `new-adr.sh`, `draft-migration-guide.sh --write`),
which only create new files.

## Implemented

| Skill | Use it for | Script |
|---|---|---|
| [edc-upstream-sources](edc-upstream-sources/SKILL.md) | Upstream EDC code and SQL schemas at the exact pinned version: read, search, diff versions | `edc-src.sh` |
| [upstream-edc-bump](upstream-edc-bump/SKILL.md) | Upgrading the upstream `edc` version: impact report, procedure | `bump-impact.sh` |
| [setting-lifecycle](setting-lifecycle/SKILL.md) | A setting across `@Setting`, env var, Helm, READMEs, migration guides; add, rename, deprecate | `setting-trace.sh` |
| [breaking-change-guard](breaking-change-guard/SKILL.md) | Breaking-change review of a branch or PR, plus missing follow-ups | `bc-scan.sh` |
| [new-extension](new-extension/SKILL.md) | Placing (generic vs `cx-`) and scaffolding an extension that builds green | `scaffold-extension.sh` |
| [sql-migration](sql-migration/SKILL.md) | Flyway migrations: status, next version, new file, review, startup failures | `migrations.sh` |
| [e2e-test-author](e2e-test-author/SKILL.md) | E2E suites and fixtures: list, scaffold, compile, run like CI, de-flake | `e2e.sh` |
| [management-api-endpoint](management-api-endpoint/SKILL.md) | Tractus-X HTTP endpoints: inventory, anatomy, versioning, tests, OpenAPI, docs | `api-inventory.sh` |
| [migration-guide-and-adr](migration-guide-and-adr/SKILL.md) | Migration guides drafted from evidence; decision records in house format | `draft-migration-guide.sh`, `new-adr.sh` |
| [dependencies-and-dash](dependencies-and-dash/SKILL.md) | License check with Eclipse Dash exactly like CI, diff against published DEPENDENCIES | `dash-check.sh` |

## Planned (designs only, not yet implemented)

These are documented here to show how they could be built. They are intentionally **not** skill folders yet, so agents
do not load half-finished procedures. To implement one, create `.claude/skills/<name>/SKILL.md` and `scripts/` along
the design below, test the script against real history as the existing ones were, and add it to the table above.

### cx-policy-constraint

**Use for:** adding or changing a Catena-X ODRL constraint (CX-0152 `leftOperand`), or debugging "policy rejected" or
"negotiation TERMINATED".

**Procedure the SKILL.md would cover:**
1. Read the constraint in CX-0152 (Catena-X standards) and decide which policy scopes it applies to (access/catalog,
   contract negotiation, transfer). `PolicyScopes` and the `register*` methods in `CxPolicyExtension` show the current
   mapping.
2. Implement a constraint function following the existing ones in `edc-extensions/cx-policy` (e.g.
   `FrameworkAgreementConstraintFunction`, `AffiliatesRegionPermissionConstraintFunction`, and the
   `ValueValidatingConstraintFunction` base), and register it per scope and rule type (permission, prohibition,
   obligation).
3. Extend the JSON-schema policy validation (`CxJsonSchemaPolicyValidator` and its schemas) so invalid policies are
   rejected at creation.
4. Add the term to the cached context `core/json-ld-cx/src/main/resources/document/cx-odrl.jsonld` only if the standard
   defines it there.
5. Tests: function unit tests, validator cases, an E2E case in `policy-tests` (valid/invalid providers), and the
   policy walkthrough `docs/usage/management-api-walkthrough/02_policies.md`.

**Script idea:** `policy-map.sh`, a matrix of every registered `leftOperand` × scope × rule type, built from the
`registerFunction`/`bindings` calls, plus a `validate-policy.sh <file.json>` that posts a policy to a local
memory runtime and prints the validation result.

### connector-log-triage

**Use for:** diagnosing a failing negotiation, transfer, EDR refresh or DCP presentation from control-plane and
data-plane logs, for example the diagnostics bundles from umbrella CI runs.

**Procedure the SKILL.md would cover:**
1. Normalise the logs (log4j2 JSON or plain text) and group them by negotiation or transfer-process id and by
   participant.
2. Build the state timeline (`REQUESTED → … → FINALIZED/TERMINATED`, `STARTED → COMPLETED/TERMINATED`) and find the
   first error.
3. Map each error message back to the line in Tractus-X or upstream code that logs it (`git grep` and `edc-src.sh grep`
   on the message template), and explain the branch that produced it.
4. Check the usual suspects in order: DSP endpoint and version, DID resolution or BDRS, STS token or VP (trusted
   issuer, credential type), access/contract policy, data address, data plane selection, token refresh.

**Script idea:** `triage.sh <log files…>`, which extracts ids, state transitions, `WARNING`/`SEVERE` lines and the
source location of each message template, and prints a per-process timeline.

### multi-dataspace-split

**Use for:** the refactoring decided in ADR `2026-01-07_multi-dataspace-support`. Split `connector-discovery`,
`dataspace-protocol`, `dcp` and `migrations` into a dataspace-agnostic core and Catena-X-specific `cx-` modules, without
renaming published artifacts, images or charts.

**Procedure the SKILL.md would cover:**
1. Find Catena-X coupling in a module: imports of BPN, BDRS, CX policy and credential namespaces, `cx-` SPIs, Catena-X
   constants.
2. Introduce an interface or extension point in the generic module and move the Catena-X implementation into the `cx-`
   module, following `dcp/tx-dcp` + `dcp/cx-dcp`.
3. Keep `edc-controlplane-base`/`edc-dataplane-base` composition identical for Catena-X users. Verify with
   `breaking-change-guard` (runtime composition, settings, API paths) and the full E2E suites.

**Script idea:** `cx-coupling.sh <module>`, which lists the files and lines of a module that reference Catena-X-specific
types, namespaces or settings, ranked by coupling. It would serve as the work list for the split and a regression check
that `core` modules stay clean.
