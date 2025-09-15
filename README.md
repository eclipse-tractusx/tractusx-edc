# Tractus-X EDC (Eclipse Dataspace Connector)

[![Contributors][contributors-shield]][contributors-url]
[![Stargazers][stars-shield]][stars-url]
[![Apache 2.0 License][license-shield]][license-url]
[![Latest Release][release-shield]][release-url]
![Latest Snapshot][snapshot-shield]

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=eclipse-tractusx_tractusx-edc&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=eclipse-tractusx_tractusx-edc)

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

Control-Plane distribution:

- [edc-controlplane-postgresql-hashicorp-vault](edc-controlplane/edc-controlplane-postgresql-hashicorp-vault) with
  dependency onto
  - [Hashicorp Vault](https://www.vaultproject.io/)
  - [PostgreSQL 8.2 or newer](https://www.postgresql.org/)

Data-Plane distribution:

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

## Known Incompatibilities

- Hashicorp Vault 1.18.1 is not compatible with the EDC due to a bug in the vault concerning path handling
  - [Internal Issue](https://github.com/eclipse-tractusx/tractusx-edc/issues/1772)
  - [Hashicorp Vault Issue](https://github.com/hashicorp/vault/issues/29357)

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

[snapshot-shield]: https://img.shields.io/badge/dynamic/regex?url=https%3A%2F%2Fraw.githubusercontent.com%2Feclipse-tractusx%2Ftractusx-edc%2Fmain%2Fgradle.properties&search=%5Eversion%3D%28.*%29%24&replace=%241&flags=m&label=latest-snapshot&style=for-the-badge

[fork]:https://img.shields.io/badge/latest--snapshot-error-red