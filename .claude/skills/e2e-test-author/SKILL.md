---
name: e2e-test-author
description: Write, run and debug Tractus-X EDC end-to-end tests (edc-tests/e2e/*) that start real consumer and provider connector runtimes, with Postgres, DCP, Kafka or cloud storage via Testcontainers. Use when a feature needs an end-to-end proof (catalog, policy, negotiation, transfer, EDR, token refresh, discovery, DSP/DCP TCK), when an E2E test is flaky or failing in CI, or to find which suite covers a behaviour.
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

# E2E test author

E2E tests are the executable specification of this connector. They start two full Tractus-X runtimes in the test JVM
(consumer and provider) and drive them through the Management API, exactly as a participant would.

## 1. Find the right suite

```bash
.claude/skills/e2e-test-author/scripts/e2e.sh list
```

| Behaviour | Suite |
|---|---|
| Catalog filtering by access policy, BPN and groups, DSP 0.8 vs 2025-1 | `catalog-tests` |
| Policy validation and evaluation, policy monitor | `policy-tests` |
| PULL and PUSH transfers, retirement, token refresh in transfers, dataflow API | `transfer-tests` |
| EDR API (`/v3/edrs`), negotiate-and-cache | `edr-api-tests` |
| Identity: DCP, STS, DIV, credential spoofing | `dcp-tests`, `dcp-tck-tests` |
| Protocol compliance | `dsp-tck-tests` |
| Connector discovery (`/v3/connectordiscovery`) | `discovery-tests` |
| Management API validators, delegated auth | `management-tests` |
| S3 and Azure, Kafka | `cloud-transfer-tests`, `end2end-transfer-cloud`, `kafka-transfer-tests` |
| Data plane token refresh | `edc-dataplane-tokenrefresh-tests` |
| BPN events | `bpn-event-tests` |

Add to an existing suite whenever the runtime setup fits. A new suite (folder, `build.gradle.kts`,
`settings.gradle.kts` include) is only worth it for a new infrastructure dependency.

## 2. Use the fixtures, do not reinvent them

All shared code is in `edc-tests/e2e-fixtures/src/testFixtures/java/org/eclipse/tractusx/edc/tests/`:

| Fixture | Purpose |
|---|---|
| `participant/TransferParticipant`, `TractusxParticipantBase`, `DcpParticipant` | A participant: name, DID, BPN and protocol, plus Management API helpers (`createAsset`, `createPolicyDefinition`, `createContractDefinition`, `getCatalogDatasets`, `edrs()`, `waitForTransferProcess`, …) |
| `runtimes/Runtimes` | Runtime factories: `pgRuntime(participant, postgres)`, discovery runtimes, … |
| `runtimes/PostgresExtension`, `KafkaExtension`, `S3MockExtension`, `AzuriteExtension`, `FlociExtension` | Testcontainers-backed infrastructure, registered with `@RegisterExtension @Order(0)` |
| `TestRuntimeConfiguration` | `CONSUMER_*` and `PROVIDER_*` names, DIDs, BPNs; `DSP_2025`, `DSP_2025_PATH` |
| `helpers/PolicyHelperFunctions`, `CatalogHelperFunctions`, `EdrNegotiationHelperFunctions`, `QueryHelperFunctions` | Policies in Catena-X ODRL (`emptyPolicy`, `bpnPolicy`, `frameworkConstraint`, …), catalog accessors, EDR negotiation, queries |
| `MockBdrsClient`, `MockVcIdentityService`, `DataspaceIssuer` | Identity stubs and a test issuer for DCP |

When several tests need a helper, add it to the fixtures instead of copying it.

## 3. Scaffold, compile, run

```bash
.claude/skills/e2e-test-author/scripts/e2e.sh new catalog-tests CatalogVisibilityEndToEndTest
.claude/skills/e2e-test-author/scripts/e2e.sh compile catalog-tests          # compile + Checkstyle, no Docker
.claude/skills/e2e-test-author/scripts/e2e.sh run catalog-tests CatalogVisibility   # Docker required
```

The scaffold is a working provider/consumer test: Postgres runtimes, DSP 2025-1, asset → policy → contract
definition → catalog assertion. Replace the scenario and keep the structure. `run` uses the same flags as CI
(`-DincludeTags="EndToEndTest" -PverboseTest=true`) and fails fast when Docker is not reachable.

## 4. Write tests that stay green

- **One behaviour per test**, named `<action>_<condition>_<expectation>`, with a `@DisplayName` sentence.
- **Wait, never sleep.** Use `waitForTransferProcess`, negotiation-state helpers, or Awaitility (`await().untilAsserted`).
  Fixed `Thread.sleep` is the main cause of flaky tests.
- **Unique ids per test** (`UUID.randomUUID()`), or clean up with `DataWiperExtension`. Runtimes and databases are
  shared across the methods of a class.
- **Cover both protocol versions** when behaviour is protocol-facing: `TransferParticipant.Builder.protocol(...)` with
  DSP 2025-1 and the 0.8 default (see `CatalogTest` vs `CatalogTestDspV08`).
- **Parameterise over Management API versions** where both are served (see `PolicyDefinitionEndToEndTest`).
- **Assert on the API contract** (status codes, JSON-LD fields), not on internal store state, unless the test is about
  persistence.

## 5. Debugging failures

1. Re-run just the failing class locally: `e2e.sh run <suite> <Class>`. Reports are in
   `edc-tests/e2e/<suite>/build/reports/tests/test/index.html`. CI uploads Allure results per suite.
2. Read the runtime logs in the test output. Both runtimes run in the test JVM and log into the same output, so
   search by negotiation or transfer-process id to follow one flow across consumer and provider.
3. Typical root causes: a policy fixture out of date with CX-0152 validation, a missing setting in
   `TractusxParticipantBase` config after an upstream change, a port clash, a Docker image pull timeout, or async state
   asserted without waiting.
4. After an upstream bump, fix the fixtures (`TractusxParticipantBase`, `Runtimes`) once, rather than every test.
