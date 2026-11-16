# Tutorials

Short, hands-on lessons. Each one takes 15–30 minutes and builds on the previous one.

<div class="grid cards" markdown>

-   :material-numeric-1-circle:{ .lg .middle } __Your first data exchange__

    ---

    Publish an asset as provider, find it in the catalog as consumer, negotiate a contract and fetch the data.

    [:octicons-arrow-right-24: Start](first-data-exchange.md)

-   :material-numeric-2-circle:{ .lg .middle } __Control access with BPN groups__

    ---

    Show an offer only to the partners you choose, without rewriting policies.

    [:octicons-arrow-right-24: Start](access-control-bpn-groups.md)

-   :material-numeric-3-circle:{ .lg .middle } __Consume data with EDRs__

    ---

    Negotiate and transfer in one call, then read data through the consumer proxy with automatic token refresh.

    [:octicons-arrow-right-24: Start](consume-with-edrs.md)

-   :material-numeric-4-circle:{ .lg .middle } __Write your first extension__

    ---

    Scaffold, configure, test and ship a new extension in the connector runtime.

    [:octicons-arrow-right-24: Start](first-extension.md)

</div>

## What you need

Tutorials 1–3 need **two connectors that trust each other**: a *provider* and a *consumer*, each registered with an
identity (DID, credentials) in the same dataspace, and an HTTP backend that the provider can reach. Options:

| Option | Good for |
|---|---|
| The [Tractus-X umbrella chart](https://github.com/eclipse-tractusx/tractus-x-umbrella) | A complete local dataspace with two connectors, identity wallet, BDRS and portal |
| Your company's test environment | Trying things with real partners |
| The end-to-end tests in [`edc-tests/e2e`](https://github.com/eclipse-tractusx/tractusx-edc/tree/main/edc-tests/e2e) | Developers: every tutorial step exists as a test |

A single connector from the [local quick start](../deployment/local-quickstart.md) is enough to try the provider half
of each tutorial.

Tutorial 4 needs only a JDK and this repository.

## Conventions

All examples use `curl` and these shell variables. Set them once:

```bash
export PROVIDER_MGMT=https://provider-control.plane/management   # provider Management API
export CONSUMER_MGMT=https://consumer-control.plane/management   # consumer Management API
export PROVIDER_DSP=https://provider-control.plane/api/v1/dsp/2025-1   # provider DSP endpoint
export PROVIDER_DID=did:web:provider.example.com
export PROVIDER_KEY=password     # X-Api-Key of the provider
export CONSUMER_KEY=password     # X-Api-Key of the consumer
```

!!! tip "Every request in detail"

    The tutorials keep each payload minimal. The [Management API walkthrough](../usage/README.md) explains every field.

## NOTICE

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- AI Disclosure: This file was entirely AI-generated. The AI-generated portions are made available under CC0-1.0 and
  not subject to the project's license. The human contributor has reviewed and verified that the content is correct.
- SPDX-License-Identifier: CC-BY-4.0 AND CC0-1.0
- SPDX-FileCopyrightText: 2026 Catena-X Automotive Network e.V.
- SPDX-FileCopyrightText: 2026 Contributors to the Eclipse Foundation
- Assisted-By: GitHub Copilot (claude-opus-5.5)
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
