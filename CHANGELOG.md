# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed

- Fixes [release 0.0.5](https://github.com/catenax-ng/product-edc/releases/tag/0.0.5), which introduced classpath issues due to usage of [net.jodah:failsafe:2.4.3](https://search.maven.org/artifact/net.jodah/failsafe/2.4.3/jar) library 


## [0.0.5] - 2022-07-28

### Added

-   EDC Health Checks for HashiCorp Vault 

### Changed

-   BusinessPartnerNumber constraint supports List structure
-   Helm: Confidential EDC settings can be set using k8s secrets
-   HashiCorp Vault API path configurable

## [0.0.4] - 2022-06-27

### Added

-   HashiCorp Vault Extension
-   Control Plane with HashiCorp Vault and PostgreSQL support

### Changed

-   Release Worklow now publishes Product EDC Extensions as Maven Artifacts

### Fixed

-   [#1515](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/1515) SQL: Connector sends out 50 contract offers max.

### Removed

-   CosmosDB Control Plane
-   Control API Extension for all Control Planes

## [0.0.3] - 2022-05-23

## [0.0.2] - 2022-05-20

## [0.0.1] - 2022-05-13

[Unreleased]: https://github.com/catenax-ng/product-edc/compare/0.0.5...HEAD

[0.0.5]: https://github.com/catenax-ng/product-edc/compare/0.0.4...0.0.5

[0.0.4]: https://github.com/catenax-ng/product-edc/compare/0.0.3...0.0.4

[0.0.3]: https://github.com/catenax-ng/product-edc/compare/0.0.2...0.0.3

[0.0.2]: https://github.com/catenax-ng/product-edc/compare/0.0.1...0.0.2

[0.0.1]: https://github.com/catenax-ng/product-edc/compare/a02601306fed39a88a3b3b18fae98b80791157b9...0.0.1
