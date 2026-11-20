# Developer setup

## Toolchain

| Tool | Version | Notes |
|---|---|---|
| JDK | **21** | CI builds with JDK 21. The runtime images run on Temurin 25 JRE. |
| Gradle | wrapper (`./gradlew`) | Do not install Gradle yourself; the wrapper pins the version. |
| Docker | recent | Needed for images, PostgreSQL tests and end-to-end tests (Testcontainers). |
| kind, kubectl, Helm | optional | For the [local quick start](../deployment/local-quickstart.md). |

The build uses the [EDC build plugin](https://github.com/eclipse-edc/GradlePlugins) and a version catalog in
`gradle/libs.versions.toml`, which also pins the upstream EDC version (`edc = …`).

## Everyday commands

| Goal | Command |
|---|---|
| Build everything (compile, unit tests, Checkstyle) | `./gradlew build` |
| Unit tests only | `./gradlew test` |
| One module | `./gradlew :edc-extensions:cx-policy:test` |
| Formatting check, as CI runs it | `./gradlew checkstyleMain checkstyleTest checkstyleTestFixtures` |
| Javadoc | `./gradlew javadoc` |
| Fat jar of a runtime | `./gradlew :edc-controlplane:edc-controlplane-postgresql-hashicorp-vault:shadowJar` |
| Docker image of a runtime | `./gradlew :edc-controlplane:edc-controlplane-postgresql-hashicorp-vault:dockerize` |
| OpenAPI specs | `./gradlew resolve` (output in `resources/openapi/yaml/`) |

Integration-type tests are skipped by `test` unless you select them with `-DincludeTags=…`. See [Testing](testing.md).

## IDE

IntelliJ IDEA works out of the box: open the root `build.gradle.kts` as a project.

- Import the code style `resources/tx-codestyle.xml` (*Settings → Editor → Code Style → Import Scheme*), or use the
  EditorConfig file `resources/tx-codestyle.editorconfig`.
- Install the **CheckStyle-IDEA** plugin and point it at `resources/tx-checkstyle-config.xml`.
- Every source file needs the license header. Templates are in `resources/java.header` and `resources/hashtag.header`.

Details: [style guide](../../styleguide.md).

## Run a connector locally

The quickest option is the in-memory runtime, which contains a control plane and a data plane in one process:

```bash
./gradlew :edc-controlplane:edc-runtime-memory:dockerize
helm install tx-inmem charts/tractusx-connector-memory \
  -f edc-tests/deployment/src/main/resources/helm/tractusx-connector-memory-test.yaml
```

The full setup with PostgreSQL and Vault on kind is described in the
[local quick start](../deployment/local-quickstart.md).

For debugging in the IDE, most developers run an **end-to-end test**: it starts real runtimes in the same JVM, so
breakpoints in any extension work. See [Testing](testing.md#end-to-end-tests).

## Upstream EDC sources

Much of the behaviour comes from upstream Eclipse EDC. Read it **at the version this repository pins**, not from
`main` of the upstream repository. The `edc-upstream-sources` skill of the
[Connector Sensei](../development/connector-sensei/README.md) downloads and searches the exact sources:

```bash
.claude/skills/edc-upstream-sources/scripts/edc-src.sh --help
```

## NOTICE

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- AI Disclosure: This file was entirely AI-generated. The AI-generated portions are made available under CC0-1.0 and
  not subject to the project's license. The human contributor has reviewed and verified that the content is correct.
- SPDX-License-Identifier: CC-BY-4.0 AND CC0-1.0
- SPDX-FileCopyrightText: 2026 Catena-X Automotive Network e.V.
- SPDX-FileCopyrightText: 2026 Contributors to the Eclipse Foundation
- Assisted-By: GitHub Copilot (claude-opus-5.5)
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
