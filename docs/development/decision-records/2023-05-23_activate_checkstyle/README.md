# Activate Checkstyle to enforce code style

## Decision

From now on, Tractus-X EDC will use activate Checkstyle, i.e. change its reporting level from `WARNING` to `ERROR`.

## Rationale

We already have a checkstyle task in our Gradle setup, and our [PR Etiquette document](../../../../pr_etiquette.md) references
the styleguide and mandates its use.

Our CI pipeline already uses checkstyle, but only outputs warning at the moment.

## Approach

- in `resources/tx-checkstyle-config.xml`, Line 22, change `<property name="severity" value="warning"/>` to `<property name="severity" value="error"/>`.
- fix all checkstyle errors
