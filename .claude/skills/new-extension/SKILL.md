---
name: new-extension
description: Create a new Tractus-X EDC extension or SPI module the way this repository does it. Covers the generic-vs-Catena-X placement decision, module scaffold with license headers, ServiceExtension with @Extension/@Inject/@Provider/@Setting, META-INF service registration, a DependencyInjectionExtension test, README, settings.gradle.kts include and wiring into the control-plane or data-plane runtime. Use for "add an extension", "new module", "where should this feature live", or when splitting an extension into core and cx- parts.
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

# New extension

## 1. Decide where it lives (before writing code)

Tractus-X EDC is becoming a midstream for several dataspaces (ADR
`docs/development/decision-records/2026-01-07_multi-dataspace-support`). Classify the feature first:

| Category | Belongs in | Naming | Examples |
|---|---|---|---|
| Generic, dataspace-agnostic | `edc-extensions/<feature>` or `<feature>/<feature>-core` | `tx-…` or no prefix | `agreements`, `dcp/tx-dcp`, `event-subscriber` |
| Catena-X specific (BPN, CX policies, CX credentials) | sibling `cx-…` module | `cx-…` | `cx-policy`, `dcp/cx-dcp`, `connector-discovery/cx-connector-discovery` |
| Contract other modules implement | `spi/<feature>-spi` or `edc-extensions/<feature>/<feature>-spi` | `…-spi` | `spi/edr-spi`, `bpn-validation/bpn-validation-spi` |
| Upstream behaviour that belongs upstream | contribute to `eclipse-edc` instead | | |

If a generic feature needs Catena-X behaviour, put an interface in the generic module and the Catena-X
implementation in the `cx-` module, as `dcp/tx-dcp` + `dcp/cx-dcp` do. New dataspace flavours are developed
outside this repo first, per the ADR.

Check for an existing extension you can extend: `ls edc-extensions spi`, and check upstream with the
`edc-upstream-sources` skill (`edc-src.sh grep 'interface <Concept>'`).

## 2. Scaffold

```bash
.claude/skills/new-extension/scripts/scaffold-extension.sh \
  edc-extensions/cx-foo  org.eclipse.tractusx.edc.foo  CxFooExtension  tx.edc.foo
```

It creates, and never overwrites:
- `build.gradle.kts`
- `<CxFoo>Extension.java`, with an `enabled` `@Setting` and an injected `Monitor`
- `META-INF/services/org.eclipse.edc.spi.system.ServiceExtension`
- `<CxFoo>ExtensionTest.java`, using `DependencyInjectionExtension`
- `README.md`, with a configuration table

All files carry license headers that pass Checkstyle (`resources/java.header`, `resources/hashtag.header`). The
generated module builds green as is: compile, Checkstyle on main and test, and the unit test.

Then do what the script prints:
1. Add `include(":edc-extensions:cx-foo")` to `settings.gradle.kts`, next to related modules.
2. Ship it with `implementation(project(":edc-extensions:cx-foo"))` in
   `edc-controlplane/edc-controlplane-base/build.gradle.kts` or `edc-dataplane/edc-dataplane-base/build.gradle.kts`.
   Leave it out if it is opt-in for adopters building their own runtime.
3. Replace `libs.edc.spi.core` with the SPIs you actually use (aliases in `gradle/libs.versions.toml`, e.g.
   `libs.edc.spi.policyengine`, `libs.edc.spi.controlplane`). Use `api(…)` only for types exposed in your public
   signatures.

## 3. Implement with EDC idioms

- **Inject** collaborators with `@Inject` (or `@Inject(required = false)` when optional). Never look services up
  manually.
- **Provide** services with `@Provider` methods. Use `@Provider(isDefault = true)` for defaults that other
  extensions may override, typically in-memory defaults that SQL extensions replace.
- **Configure** with `@Setting(key = CONST, description = …, defaultValue = …)` on fields, or a `@Settings`
  record. Follow the `setting-lifecycle` skill if operators need a Helm value.
- **Register, don't wire.** Use policy functions via `PolicyEngine.registerFunction(<Context>.class, …)`, rule
  bindings via `RuleBindingRegistry`, JSON-LD transformers via `TypeTransformerRegistry`, validators via
  `JsonObjectValidatorRegistry`, and API controllers via `WebService.registerResource(<ApiContext>, …)`.
- **JSON-LD.** Use terms from `spi/core-spi/.../CoreConstants` (`TX_NAMESPACE`, `CX_POLICY_2025_09_NS`, …) and
  cached contexts in `core/json-ld-*`. Never invent namespaces.
- **Results, not exceptions.** Return `ServiceResult`, `StoreResult` and `Result` across boundaries, as upstream
  does.
- **Logging.** Inject `Monitor` and never log secrets or tokens (ADR `2025-07-09-logging_and_metrics_strategy`).
- **Persistence.** For SQL stores, follow `edr-index-lock-sql` and `bpns-evaluation-store-sql`: a `…Statements`
  interface, a Postgres dialect class, `@Setting` for the datasource name, and a Flyway migration in
  `edc-extensions/migrations/connector-migration`.

## 4. Test like the repo does

- Unit tests next to the code with JUnit 5 + AssertJ + Mockito. Extension wiring uses
  `@ExtendWith(DependencyInjectionExtension.class)` (see the generated test).
- Tag slower tests: `@ComponentTest`, `@ApiTest`, `@PostgresqlIntegrationTest`, `@EndToEndTest`. CI runs them by
  tag (`.github/workflows/verify.yaml`).
- End-to-end: add a case to the matching suite in `edc-tests/e2e/*`, reusing participants and helpers from
  `edc-tests/e2e-fixtures` (`TractusxParticipantBase`, `Runtimes`, `PostgresExtension`, `PolicyHelperFunctions`,
  …).

## 5. Finish

- `./gradlew :edc-extensions:cx-foo:build` passes, which includes Checkstyle.
- README documents purpose, plane, dataspace category and every setting.
- New third-party dependencies are added to `gradle/libs.versions.toml` and DEPENDENCIES is regenerated (Eclipse
  Dash).
- Run the `breaking-change-guard` scan.
- Commit with Conventional Commits, e.g. `feat(cx-foo): add …`.
