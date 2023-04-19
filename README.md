# Tractus-X EDC (Eclipse Dataspace Connector)

[![Contributors][contributors-shield]][contributors-url]
[![Stargazers][stars-shield]][stars-url]
[![Apache 2.0 License][license-shield]][license-url]
[![Latest Release][release-shield]][release-url]

Container images and deployments of the Eclipse Dataspace Components for the Tractus-X project.

Please also refer to:

- [Our docs](https://github.com/eclipse-tractusx/tractusx-edc/tree/main/docs)
- [Our Releases](https://github.com/eclipse-tractusx/tractusx-edc/releases)
- [Eclipse Dataspace Components](https://github.com/eclipse-edc/Connector)
- [Report Bug / Request Feature](https://github.com/eclipse-tractusx/tractusx-edc/issues)

## About The Project

The project provides pre-built control- and data-plane [docker](https://www.docker.com/) images
and [helm](https://helm.sh/) charts of
the [Eclipse DataSpaceConnector Project](https://github.com/eclipse-edc/Connector).

## Inventory

The eclipse data space connector is split up into Control-Plane and Data-Plane, whereas the Control-Plane functions as
administration layer and has responsibility of resource management, contract negotiation and administer data transfer.
The Data-Plane does the heavy lifting of transferring and receiving data streams.

Depending on your environment there are different derivatives of the control-plane prepared:

- [edc-controlplane-postgresql](edc-controlplane/edc-controlplane-postgresql) with dependency onto
  - [Azure Key Vault](https://azure.microsoft.com/en-us/services/key-vault/#product-overview)
  - [PostgreSQL 8.2 or newer](https://www.postgresql.org/)
- [edc-controlplane-postgresql-hashicorp-vault](edc-controlplane/edc-controlplane-postgresql-hashicorp-vault) with
  dependency onto
  - [Hashicorp Vault](https://www.vaultproject.io/)
  - [PostgreSQL 8.2 or newer](https://www.postgresql.org/)

Derivatives of the Data-Plane can be found here

- [edc-dataplane-azure-vault](edc-dataplane/edc-dataplane-azure-vault) with dependency onto
  - [Azure Key Vault](https://azure.microsoft.com/en-us/services/key-vault/#product-overview)
- [edc-dataplane-hashicorp-vault](edc-dataplane/edc-dataplane-hashicorp-vault) with dependency onto
  - [Hashicorp Vault](https://www.vaultproject.io/)

For testing/development purposes:

- [edc-runtime-memory](edc-controlplane/edc-runtime-memory)

## Getting Started

### Build

Build Tractus-X EDC together with its Container Images

```shell
./gradlew dockerize
```

## License

Distributed under the Apache 2.0 License.
See [LICENSE](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/LICENSE) for more information.

<!-- MARKDOWN LINKS & IMAGES -->
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->

[contributors-shield]: https://img.shields.io/github/contributors/eclipse-tractusx/tractusx-edc.svg?style=for-the-badge

[contributors-url]: https://github.com/eclipse-tractusx/tractusx-edc/graphs/contributors

[stars-shield]: https://img.shields.io/github/stars/eclipse-tractusx/tractusx-edc.svg?style=for-the-badge

[stars-url]: https://github.com/eclipse-tractusx/tractusx-edc/stargazers

[license-shield]: https://img.shields.io/github/license/eclipse-tractusx/tractusx-edc.svg?style=for-the-badge

[license-url]: https://github.com/eclipse-tractusx/tractusx-edc/blob/main/LICENSE

[release-shield]: https://img.shields.io/github/v/release/eclipse-tractusx/tractusx-edc.svg?style=for-the-badge

[release-url]: https://github.com/eclipse-tractusx/tractusx-edc/releases
