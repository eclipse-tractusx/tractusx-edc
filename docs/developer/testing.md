# Testing

## Test types and tags

Tests are JUnit 5 and are selected by **tags**. A plain `./gradlew test` runs only unit tests; everything tagged
`IntegrationTest` (directly or through one of the annotations below) is skipped unless you ask for it.

| Annotation | Tag | What it is | Needs Docker |
|---|---|---|---|
| none | – | Unit test | no |
| `@ComponentTest` | `ComponentTest` | Several real components, no external systems | no |
| `@ApiTest` | `ApiTest` | HTTP controller tests with REST Assured | no |
| `@PostgresqlIntegrationTest` | `PostgresqlIntegrationTest` | SQL stores against PostgreSQL | yes |
| `@EndToEndTest` | `EndToEndTest` | Real connector runtimes talking to each other | usually |
| `@CompatibilityTest` | `CompatibilityTest` | Current code against the previous release's image | yes |

## Run what CI runs

| CI job | Command |
|---|---|
| Formatting | `./gradlew checkstyleMain checkstyleTest checkstyleTestFixtures` |
| Javadoc | `./gradlew javadoc` |
| Unit tests | `./gradlew test` |
| Component tests | `./gradlew test -DincludeTags="ComponentTest"` |
| API tests | `./gradlew test -DincludeTags="ApiTest"` |
| PostgreSQL tests | `./gradlew test -DincludeTags="PostgresqlIntegrationTest" -PverboseTest=true` |
| One end-to-end suite | `./gradlew -p edc-tests/e2e/<suite> test -DincludeTags="EndToEndTest" -PverboseTest=true` |
| Helm docs up to date | `helm-docs` in Docker, then `git diff` must be empty |

The definitive list is `.github/workflows/verify.yaml`.

## End-to-end tests

End-to-end tests start **real runtimes in the test JVM**: a provider and a consumer, each with control plane and data
plane, and, where needed, PostgreSQL, Kafka, S3 or Azurite in Testcontainers. Because everything runs in one JVM, you
can set breakpoints anywhere.

Run one class:

```bash
./gradlew -p edc-tests/e2e/transfer-tests test -DincludeTags="EndToEndTest" --tests "*TransferPullEndToEndTest*"
```

| Suite | Covers |
|---|---|
| `catalog-tests` | Catalog filtering by access policy, BPN and groups; DSP 0.8 vs 2025-1 |
| `policy-tests` | Policy validation and evaluation, policy monitor |
| `transfer-tests` | Pull and push, token refresh during transfers, data flow API, agreement retirement |
| `edr-api-tests` | `/v3/edrs` |
| `edc-dataplane-tokenrefresh-tests` | Data plane token refresh |
| `management-tests` | Management API validators, delegated authentication |
| `discovery-tests` | `/v3/connectordiscovery` |
| `dcp-tests` | DCP, STS, DIV, credential spoofing |
| `bpn-event-tests` | Business partner group events |
| `cloud-transfer-tests`, `end2end-transfer-cloud` | S3 and Azure Blob transfers |
| `kafka-transfer-tests` | Kafka pull |
| `dsp-tck-tests` | Dataspace Protocol TCK |
| `dcp-tck-tests` | Decentralized Claims Protocol TCK |

Shared fixtures are in `edc-tests/e2e-fixtures/src/testFixtures/java/org/eclipse/tractusx/edc/tests/`: participants,
runtime extensions, helpers for policies and catalogs, and base classes such as `ConsumerPullBaseTest`.

The `e2e-test-author` skill lists suites, scaffolds new tests and runs them like CI:

```bash
.claude/skills/e2e-test-author/scripts/e2e.sh run transfer-tests TransferPullEndToEndTest
```

## Compatibility tests

They check that the current code still talks to the **previous release**. First build the stable image, then run:

```bash
./gradlew :edc-tests:runtime:runtime-compatibility:stable:connector-stable:dockerize_stable
./gradlew test -DincludeTags="CompatibilityTest" -PverboseTest=true
```

## Deployment tests

CI also installs both Helm charts in kind on the three newest Kubernetes versions, runs `helm test`, and upgrades from
the latest release. You can run the same locally with the [local quick start](../deployment/local-quickstart.md).

## Writing good tests

- Prefer unit tests; use component and end-to-end tests for behaviour that crosses modules.
- Name tests after behaviour, for example `evaluate_whenGroupMissing_shouldDeny`.
- Use the existing fixtures and helpers instead of new ones.
- Pin new Testcontainers images in a `Dockerfile` under `e2e-fixtures/src/testFixtures/resources/`, so Dependabot can
  update them.

## NOTICE

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- AI Disclosure: This file was entirely AI-generated. The AI-generated portions are made available under CC0-1.0 and
  not subject to the project's license. The human contributor has reviewed and verified that the content is correct.
- SPDX-License-Identifier: CC-BY-4.0 AND CC0-1.0
- SPDX-FileCopyrightText: 2026 Catena-X Automotive Network e.V.
- SPDX-FileCopyrightText: 2026 Contributors to the Eclipse Foundation
- Assisted-By: GitHub Copilot (claude-opus-5.5)
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
