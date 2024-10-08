# Refactor of the Release Workflow

## Decision

We will stop to provide an azure flavoured distribution of the connector.

## Rationale

Two reasons are pushing toward this decision:
- Tractus-X Connector is the reference implementation for Catena-X: this project is not meant to satisfy different needs
  but to show how an EDC Connector distribution for Catena-X can be assembled. Our focus should point toward maintaining
  a single distribution.
- Tractus-X is an open source software, and we should focus on a distribution that involves OSS services.

## Approach

We will deprecate, 0.8.0:
- `tractusx-connector-azure-vault` helm chart
- `edc-controlplane-postgresql-azure-vault` docker image
- `edc-dataplane-postgresql-azure-vault` docker image

We will stop publishing them from version 0.9.0 on.
