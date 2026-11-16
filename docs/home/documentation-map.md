# Documentation map

Every part of the documentation, grouped by what you want to do. Not sure where to begin? Find your role first.

## Find your path

<div class="grid cards" markdown>

-   :material-lightbulb-on-outline: **I am new to dataspaces**

    ---

    [What is Tractus-X EDC?](overview.md) · [Glossary](glossary.md) · [FAQ](faq.md) ·
    [Architecture overview](../architecture/index.md)

-   :material-kubernetes: **I operate a connector**

    ---

    [Deployment overview](../deployment/index.md) · [Helm chart](../deployment/helm-chart.md) ·
    [Production checklist](../deployment/production-checklist.md) · [Upgrades](../deployment/upgrades.md) ·
    [Troubleshooting](../operations/troubleshooting.md)

-   :material-school-outline: **I integrate an application**

    ---

    [Tutorials](../tutorials/index.md) · [Management API walkthrough](../usage/management-api-walkthrough/README.md) ·
    [APIs](../reference/apis.md) · [JSON-LD](../reference/json-ld.md)

-   :material-code-braces: **I develop or contribute**

    ---

    [Development setup](../developer/setup.md) · [Code architecture](../developer/code-architecture.md) ·
    [Writing extensions](../developer/extensions.md) · [Testing](../developer/testing.md) ·
    [Contribution workflow](../developer/contributing.md)

</div>

## All sections

### Home

| Page | What you will find |
|---|---|
| [What is Tractus-X EDC?](overview.md) | The problem, the four core ideas, control vs data plane, what Tractus-X adds |
| [Glossary](glossary.md) | Every term and abbreviation |
| [FAQ](faq.md) | Short answers to common questions |

### Architecture

| Page | What you will find |
|---|---|
| [Overview](../architecture/index.md) | System context, deployment view, protocols |
| [Components](../architecture/components.md) | Control plane, data plane, their APIs and dependencies |
| [Identity and trust](../architecture/identity-and-trust.md) | DIDs, DCP, STS, credentials, BDRS |
| [Catena-X policies](../architecture/policies.md) | Access vs contract policies, CX-0152 constraints, BPN groups |
| [Data exchange flows](../architecture/flows.md) | Catalog, negotiation, transfer, discovery as sequence diagrams |
| [EDRs and token refresh](../architecture/edr-and-token-refresh.md) | EDR cache, refresh, renewal |
| [Persistence and state](../architecture/persistence.md) | Stores, PostgreSQL, Flyway migrations, vault |
| [Data plane signaling](../development/dataplane-signaling/tx-signaling.extensions.md) | Deep dive into the Tractus-X signaling extensions |
| [Bidirectional data transfer](../development/bidirectional-transfer/BidirectionalDataTransfer.md) | Deep dive into response channels |

### Deployment

| Page | What you will find |
|---|---|
| [Overview](../deployment/index.md) | Distributions, topologies, prerequisites |
| [Local quick start](../deployment/local-quickstart.md) | Run a connector on kind in minutes |
| [Helm chart](../deployment/helm-chart.md) | The values that matter, explained |
| [Configuration](../deployment/configuration.md) | How settings, environment variables and Helm values relate |
| [Production checklist](../deployment/production-checklist.md) | Security, availability and operations before go-live |
| [Upgrades and migrations](../deployment/upgrades.md) | How to upgrade safely; [all migration guides](../migration/index.md) |

### Tutorials

| Page | What you will find |
|---|---|
| [Learning path](../tutorials/index.md) | Which tutorial to take when |
| [1. Your first data exchange](../tutorials/first-data-exchange.md) | Asset → policy → offer → catalog → negotiation → transfer → data |
| [2. Restrict access with BPN groups](../tutorials/access-control-bpn-groups.md) | Show an offer only to selected partners |
| [3. Consume data with EDRs](../tutorials/consume-with-edrs.md) | One-call negotiation, EDR cache, token refresh |
| [4. Write your first extension](../tutorials/first-extension.md) | A working `ServiceExtension` with a setting and a test |
| [Management API walkthrough](../usage/management-api-walkthrough/README.md) | Every Management API resource in depth |

### Developers

| Page | What you will find |
|---|---|
| [Overview](../developer/index.md) | Where to start as a contributor |
| [Development setup](../developer/setup.md) | Toolchain, build, run, IDE |
| [Code architecture](../developer/code-architecture.md) | Module layers, runtimes, dependency rules |
| [Writing extensions](../developer/extensions.md) | The extension model as used in this repository |
| [Testing](../developer/testing.md) | Test types, tags, end-to-end tests |
| [Contribution workflow](../developer/contributing.md) | Issues, PRs, commits, reviews, decision records |
| [Connector Sensei](../development/connector-sensei/README.md) | The AI agent and its skills |
| [Coding principles](../development/coding-principles.md), [Project structure](../development/project-structure.md), [Repository structure](../development/repository-structure.md), [Release process](../development/release.md) | Project guidelines |
| [Decision records](../development/decision-records/index.md) | All architecture decisions |

### Operations and reference

| Page | What you will find |
|---|---|
| [Operations overview](../operations/index.md) | Day-2 operations |
| [Observability](../operations/observability.md) | Health checks, logs, metrics |
| [Troubleshooting](../operations/troubleshooting.md) | Symptoms, causes, fixes |
| [APIs](../reference/apis.md) | Management, DSP, data plane APIs; OpenAPI; Tractus-X endpoints |
| [Extension catalogue](../reference/extensions.md) | Every Tractus-X module and what it does |
| [JSON-LD and namespaces](../reference/json-ld.md) | Contexts, prefixes and vocabularies |
| [Migration guides](../migration/index.md) | Release-by-release upgrade notes |

## NOTICE

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- AI Disclosure: This file was entirely AI-generated. The AI-generated portions are made available under CC0-1.0 and
  not subject to the project's license. The human contributor has reviewed and verified that the content is correct.
- SPDX-License-Identifier: CC-BY-4.0 AND CC0-1.0
- SPDX-FileCopyrightText: 2026 Catena-X Automotive Network e.V.
- SPDX-FileCopyrightText: 2026 Contributors to the Eclipse Foundation
- Assisted-By: GitHub Copilot (claude-opus-5.5)
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
