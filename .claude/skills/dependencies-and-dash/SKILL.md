---
name: dependencies-and-dash
description: Check third-party dependency licenses for Tractus-X EDC with Eclipse Dash before merging. Reproduces CI's dependency list for the published control plane and data plane, diffs it against the DEPENDENCIES file CI publishes, runs Dash, and explains how to resolve restricted or rejected dependencies. Use when adding or bumping a library, when a dependabot PR or the release's dependency check fails, or when asked about DEPENDENCIES or Eclipse IP.
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

# Dependencies and Eclipse Dash

Every third-party artifact shipped in the Tractus-X EDC images must be license-vetted by the Eclipse Foundation (Eclipse
Dash License Tool, IP due diligence). In this repository:

- **`DEPENDENCIES` is generated, not hand-edited.** `.github/workflows/generate-and-publish-dependencies.yaml` runs on
  every push to `main` and `release/*`. It builds the list
  (`.github/actions/generate-and-check-dependencies`), runs Dash (`eclipse-tractusx/sig-infra` `run-dash`, Dash 1.1.0)
  and publishes the result to <https://eclipse-tractusx.github.io/tractusx-edc/DEPENDENCIES>. The committed file on
  `main` can be empty.
- **Releases are strict.** For official releases the check runs with `fail_on_restricted`. A restricted dependency
  that slipped in on `main` blocks the release, not the PR that introduced it, so check in the PR.

## 1. Check locally

```bash
.claude/skills/dependencies-and-dash/scripts/dash-check.sh --list-only   # ~10 s: list + diff vs published file
.claude/skills/dependencies-and-dash/scripts/dash-check.sh               # ~40 s: also runs Eclipse Dash 1.1.0
```

The script:
1. Builds the dependency list exactly like CI: `:edc-controlplane:allDependencies` and `:edc-dataplane:allDependencies`
   on `runtimeClasspath`, with the same coordinate extraction.
2. Diffs it against the published DEPENDENCIES: **new** artifacts or versions your branch introduces, and ones no longer
   used.
3. Lists dependencies that are already restricted upstream (pending IP review).
4. Runs Dash and reports approved, restricted and rejected counts. Outputs go to a temp dir; the repository is never
   modified.

## 2. Resolve findings

| Status | Meaning | What to do |
|---|---|---|
| `approved` | License vetted (ClearlyDefined or an IP issue) | Nothing |
| `restricted` | Not yet vetted, or the license is unclear | Prefer a version that is already approved. Otherwise get it reviewed: a committer runs Dash with `-review -project automotive.tractusx -token <gitlab-token>` to open an IP issue at gitlab.eclipse.org, or you harvest it on <https://clearlydefined.io/harvest> and wait for the curation. Track it in the PR. |
| `rejected` | License incompatible with EPL/Apache distribution | Do not ship it. Find an alternative, or remove the transitive path (`exclude(...)` in Gradle). |

Other levers:
- **Pin or align versions** in `gradle/libs.versions.toml`. A version bump can turn an approved artifact into a restricted
  one, because approval is per version. The repo already forces some versions (e.g. `com.networknt:json-schema-validator`
  via `resolutionStrategy`).
- **Find who pulls a transitive artifact**:
  `./gradlew :edc-controlplane:edc-controlplane-base:dependencyInsight --configuration runtimeClasspath --dependency <artifact>`.
- **Only shipped runtimes count.** Test-only dependencies (Testcontainers, WireMock, …) are not in the published list,
  but stay sensible.

## 3. In a PR

- [ ] `dash-check.sh` shows no new `restricted` or `rejected` entries, or each one has an IP review link.
- [ ] Do not commit a regenerated `DEPENDENCIES`; CI owns it.
- [ ] For dependabot PRs, the new version is listed as approved (or reviewed) before merge.

Background: `docs/development/release.md` (manual Dash procedure, which this script automates) and the Eclipse
handbook on IP due diligence (<https://www.eclipse.org/projects/handbook/#ip>).
