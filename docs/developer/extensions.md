# Writing extensions

New to extensions? Do [Tutorial 4](../tutorials/first-extension.md) first. This page is the reference for the idioms
used in this repository.

## Where does it belong?

| Kind | Location | Example |
|---|---|---|
| Generic, useful in any dataspace | `edc-extensions/<feature>` | `agreements`, `event-subscriber` |
| Catena-X specific | `edc-extensions/cx-<feature>`, or a `cx-` sibling module | `cx-policy`, `dcp/cx-dcp` |
| Contracts other modules use | `spi/<feature>-spi` or `edc-extensions/<feature>/<feature>-spi` | `spi/edr-spi`, `bpn-validation/bpn-validation-spi` |
| Useful to all EDC users | contribute to [upstream EDC](https://github.com/eclipse-edc/Connector) | – |

If a generic feature needs Catena-X behaviour, put the interface in the generic module and the Catena-X implementation
in a `cx-` module, as `dcp/tx-dcp` and `dcp/cx-dcp` do. Background:
[multi-dataspace support ADR](../development/decision-records/2026-01-07_multi-dataspace-support/README.md).

## Anatomy

```text
edc-extensions/my-feature/
├── build.gradle.kts
├── README.md                                      # purpose + settings table
└── src/main
    ├── java/org/eclipse/tractusx/edc/myfeature/MyFeatureExtension.java
    └── resources/META-INF/services/org.eclipse.edc.spi.system.ServiceExtension
```

Generate this with:

```bash
.claude/skills/new-extension/scripts/scaffold-extension.sh <dir> <java-package> <ClassName>Extension [setting.prefix]
```

## The annotations

```java
@Extension(value = SqlAgreementsRetirementStoreExtension.NAME)
public class SqlAgreementsRetirementStoreExtension implements ServiceExtension {

    protected static final String NAME = "SQL Agreement Retirement Store.";

    @Setting(key = "tx.edc.sql.store.agreementretirement.datasource",
             description = "Datasource name for the SQL AgreementsRetirement store",
             defaultValue = DataSourceRegistry.DEFAULT_DATASOURCE)
    private String dataSourceName;

    @Inject
    private DataSourceRegistry dataSourceRegistry;

    @Inject(required = false)
    private SqlAgreementsRetirementStatements statements;

    @Provider
    public AgreementsRetirementStore sqlStore() {
        return new SqlAgreementsRetirementStore(dataSourceRegistry, dataSourceName, /* … */);
    }
}
```

| Annotation | Purpose |
|---|---|
| `@Extension` | Names the extension (used in logs and the dependency graph). |
| `@Setting` | Injects a configuration value. Always give `description`, and `defaultValue` when there is one. |
| `@Settings` record + `@Configuration` field | Groups related settings into a typed record. |
| `@Inject` | Gets a service from another extension. `required = false` makes it optional. |
| `@Provider` | Offers a service. `isDefault = true` marks a fallback that another provider may replace. |

## Register, don't wire

Extensions contribute behaviour by registering it in upstream registries in `initialize()`:

| To add… | Register with |
|---|---|
| A policy function | `PolicyEngine.registerFunction(...)` and `RuleBindingRegistry.bind(...)` |
| A JSON-LD transformer | `TypeTransformerRegistry.register(...)` |
| A request validator | `JsonObjectValidatorRegistry.register(...)` |
| An HTTP controller | `WebService.registerResource(ApiContext.MANAGEMENT, controller)` |

Adding or versioning an HTTP endpoint is covered by the `management-api-endpoint` skill of the
[Connector Sensei](../development/connector-sensei/README.md).

## Rules of thumb

- **Fail fast.** Read and validate configuration in `initialize()`.
- **No checked exceptions.** Return `Result` / `ServiceResult` for expected failures; extend `EdcException` otherwise.
- **Secrets only in the `Vault`**, and never in logs. Log through the injected `Monitor`.
- **JSON via `TypeManager`**, not your own `ObjectMapper`.
- **Persistence:** a `…Statements` interface, a PostgreSQL dialect, a data source `@Setting`, and a Flyway migration in
  `connector-migration` (see the `sql-migration` skill).
- **Settings that operators need** go into the Helm chart too (see the `setting-lifecycle` skill and
  [Configuration](../deployment/configuration.md)).

More: [coding principles](../development/coding-principles.md).

## NOTICE

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- AI Disclosure: This file was entirely AI-generated. The AI-generated portions are made available under CC0-1.0 and
  not subject to the project's license. The human contributor has reviewed and verified that the content is correct.
- SPDX-License-Identifier: CC-BY-4.0 AND CC0-1.0
- SPDX-FileCopyrightText: 2026 Catena-X Automotive Network e.V.
- SPDX-FileCopyrightText: 2026 Contributors to the Eclipse Foundation
- Assisted-By: GitHub Copilot (claude-opus-5.5)
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
