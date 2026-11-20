# Tractus-X EDC

The Tractus-X EDC repository creates runnable applications out of EDC Connector extensions from the 
[EDC Connector](https://github.com/eclipse-edc/Connector) platform.

When running an EDC connector from the Tractus-X EDC repository there are three different setups to choose from. They 
only vary by using different extensions for

- Resolving of Connector-Identities
- Persistence of the Control-Plane-State
- Persistence of Secrets (Vault)

## Documentation Site

This folder is published as the documentation site at
<https://eclipse-tractusx.github.io/tractusx-edc/> (built with [MkDocs Material](https://squidfunk.github.io/mkdocs-material/),
configured in [`mkdocs.yml`](../mkdocs.yml)). To preview it locally:

```shell
python3 -m venv .venv && . .venv/bin/activate
pip install -r resources/docs/requirements.txt
mkdocs serve
```

Then open <http://127.0.0.1:8000/tractusx-edc/>. Pull requests that touch the documentation run
`mkdocs build --strict` in the `Publish documentation` workflow. New pages must be added to the `nav` in `mkdocs.yml`.

## Connector Setup

The three supported setups are.

- Setup 1: Pure in Memory **Not intended for production use!**
  - In Memory persistence
  - In Memory KeyVault with seedable secrets.
  - Planes:
    - [Control Plane](../edc-controlplane/edc-runtime-memory/README.md)
    - [Data Plane](../edc-dataplane/edc-dataplane-base/README.md)
- Setup 2: PostgreSQL & HashiCorp Vault
  - PostgreSQL persistence 
  - HashiCorp Vault
  - Planes:
    - [Control Plane](../edc-controlplane/edc-controlplane-postgresql-hashicorp-vault/README.md)
    - [Data Plane](../edc-dataplane/edc-dataplane-hashicorp-vault/README.md)

## Recommended Documentation

- [MXD: Minimum viable tractusX Dataspace](https://github.com/eclipse-tractusx/tutorial-resources/tree/main/mxd)
- [Migration guides](migration)
- [Development](development/README.md)
- [Application: Control Plane](../edc-controlplane)
- [Application: Data Plane](../edc-dataplane)
- [Extension: Business Partner Numbers validation](../edc-extensions/bpn-validation/README.md)
- [Eclipse Dataspace Components](https://eclipse-edc.github.io/docs/#/)

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2021,2022,2023 Contributors to the Eclipse Foundation
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
