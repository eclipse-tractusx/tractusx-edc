# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

**Important Note**: Version 0.1.0 introduces multiple breaking changes. Before updating **always** consolidate the
corresponding [documentation](/docs/migration/Version_0.0.x_0.1.x.md).

### Added

- control plane extension ([data-plane-selector-client](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/tree/v0.0.1-milestone-5/extensions/data-plane-selector/selector-client))
  - run the EDC with multiple data planes at once
- control plane extension([dataplane-selector-configuration](edc-extensions/dataplane-selector-configuration))
  - add data plane instances to the control plane by configuration
- data plane extension ([s3-data-plane](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/tree/main/extensions/aws/data-plane-s3))
  - transfer from and to AWS S3 buckets

### Changed

- update setting name (`edc.dataplane.token.validation.endpoint` -> `edc.dataplane.token.validation.endpoint`)

## [0.0.6] - 2022-07-29

### Fixed

-   Fixes [release 0.0.5](https://github.com/catenax-ng/product-edc/releases/tag/0.0.5), which introduced classpath issues due to usage of [net.jodah:failsafe:2.4.3](https://search.maven.org/artifact/net.jodah/failsafe/2.4.3/jar) library 

## [0.0.5] - 2022-07-28

### Added

- EDC Health Checks for HashiCorp Vault

### Changed

- BusinessPartnerNumber constraint supports List structure
- Helm: Confidential EDC settings can be set using k8s secrets
- HashiCorp Vault API path configurable

## [0.0.4] - 2022-06-27

### Added

- HashiCorp Vault Extension
- Control Plane with HashiCorp Vault and PostgreSQL support

### Changed

- Release Worklow now publishes Product EDC Extensions as Maven Artifacts

### Fixed

- [#1515](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/1515) SQL: Connector sends out 50
  contract offers max.

### Removed

- CosmosDB Control Plane
- Control API Extension for all Control Planes

## [0.0.3] - 2022-05-23

## [0.0.2] - 2022-05-20

## [0.0.1] - 2022-05-13

[Unreleased]: https://github.com/catenax-ng/product-edc/compare/0.0.6...HEAD

[0.0.6]: https://github.com/catenax-ng/product-edc/compare/0.0.5...0.0.6

[0.0.5]: https://github.com/catenax-ng/product-edc/compare/0.0.4...0.0.5

[0.0.4]: https://github.com/catenax-ng/product-edc/compare/0.0.3...0.0.4

[0.0.3]: https://github.com/catenax-ng/product-edc/compare/0.0.2...0.0.3

[0.0.2]: https://github.com/catenax-ng/product-edc/compare/0.0.1...0.0.2

[0.0.1]: https://github.com/catenax-ng/product-edc/compare/a02601306fed39a88a3b3b18fae98b80791157b9...0.0.1
