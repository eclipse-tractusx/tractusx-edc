# Migration from 0.3.0 to 0.3.1

## Configuration of Azure KeyVault

When using Helm Charts that use the Azure KeyVault (`edc-runtime-memory`, `edc-controlplane-postgres`)
it is now possible to select _either_ authentication via Client Secret (`azure.vault.secret`) or via
certificate (`azure.vault.certificate`).

If neither of the two is configured, the runtime will fail to start issuing an error.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2021,2022,2023 Contributors to the Eclipse Foundation
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
