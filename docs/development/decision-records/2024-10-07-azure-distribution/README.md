# Refactor of the Release Workflow

## Decision

We will stop providing all variants of the official Tractus-X EDC distributions that come pre-packaged with Azure Vault as HSM.

## Rationale

Different reasons are pushing toward this decision:
- Tractus-X Connector is the reference implementation for Catena-X: this project is not meant to satisfy different needs
  but to show how an EDC Connector distribution for Catena-X can be assembled. Our focus should point toward maintaining
  a single distribution.
- Tractus-X is an open source software, and we should focus on a distribution that involves OSS services.
- For the past year there were no resources to stand up an integration test environment, i.e. an actual Azure Vault instance
- There is no reason for favoritism toward Azure Vault over other services like AWS SecretsManager or similar

## Approach

We will deprecate, 0.8.0:
- `tractusx-connector-azure-vault` helm chart
- `edc-controlplane-postgresql-azure-vault` docker image
- `edc-dataplane-postgresql-azure-vault` docker image

We will stop publishing them from version 0.9.0 on.
