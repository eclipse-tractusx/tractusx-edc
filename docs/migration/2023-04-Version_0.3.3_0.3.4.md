# Migration from 0.3.3 to 0.3.4

## Refactoring of Helm Charts

In issue [#136](https://github.com/eclipse-tractusx/tractusx-edc/issues/136) work has begun to split the Helm charts up
into several technology-focused charts:

- In-memory: for testing and development
- PostgreSQL+Hashicorp: this is the **recommended** distribution of Tractus-X EDC
- (Azure KeyVault: uses Azure KeyVault instead of Hashicorp Vault.) - Work in Progress

Unfortunately, due to time constraints, we had to release 0.3.4 **without** the Azure KeyVault chart, it will be
included in one of the subsequent releases in the future.

**Please note that the Azure KeyVault variant is not included in the 0.3.4 Release! If you rely on AZKV please do NOT
upgrade to 0.3.4 yet!**

## Change in Docker image publishing

Starting with the 0.3.3 release we switched over to publish our Docker images
to [Docker Hub](https://hub.docker.com/search?q=tractusx) instead of GHCR.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2021,2022,2023 Contributors to the Eclipse Foundation
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
