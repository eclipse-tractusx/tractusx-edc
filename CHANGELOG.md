# Changelog

## Current Releases 
See https://github.com/eclipse-tractusx/tractusx-edc/releases for the releases and the changes (bugfixes, new features, improvements, documentation and other changes) introduced with each release. 


--- 
## Historic Releases
All notable changes to this project historically were documented in this file. 

## [0.5.3] - 2023-11-10

What's Changed

- chore: backport [autovacuum fix](https://github.com/eclipse-edc/Connector/pull/3479) by @wolf4ood  in[#871]( https://github.com/eclipse-tractusx/tractusx-edc/pull/871)

## [0.5.2] - 2023-11-08

What's Changed

- chore: backport [autovacuum fix](https://github.com/eclipse-edc/Connector/pull/3479) by @wolf4ood  in [#866](https://github.com/eclipse-tractusx/tractusx-edc/pull/866)

## [0.5.1] - 2023-08-22

What's Changed

- chore: Improve Helm Chart documentation by @tuncaytunc-zf in [#607](https://github.com/eclipse-tractusx/tractusx-edc/pull/607)
- refactor(CPA): obliviates the control plane adapter term and module refactor by @wolf4ood in [#606](https://github.com/eclipse-tractusx/tractusx-edc/pull/606)
- Chore: synchronize histories of main and releases by @paullatzelsperger in [#604](https://github.com/eclipse-tractusx/tractusx-edc/pull/604)
- fix(helm charts): Replace all "ids" occurrences in helm charts by @florianrusch-zf in [#586](https://github.com/eclipse-tractusx/tractusx-edc/pull/586)
- feat(ci): use dependency check from upstream EDC by @paullatzelsperger in [#615](https://github.com/eclipse-tractusx/tractusx-edc/pull/615)
- fix(helm chart): Set participant ID in data plane by @tuncaytunc-zf in [#627](https://github.com/eclipse-tractusx/tractusx-edc/pull/627)
- chore: update curl version to 8.2.0-r0 by @paullatzelsperger in [#633](https://github.com/eclipse-tractusx/tractusx-edc/pull/633)
- build: decrease dependabot frequency to 'weekly' by @paullatzelsperger in [#634](https://github.com/eclipse-tractusx/tractusx-edc/pull/634)
- feat(EDR): adds EDR state machine for handling EDR renewal by @wolf4ood in [#620](https://github.com/eclipse-tractusx/tractusx-edc/pull/620)
- feat: upgrade to EDC 0.2.0 by @paullatzelsperger in [#674](https://github.com/eclipse-tractusx/tractusx-edc/pull/674)
- feat: simplify data encryptor by @paullatzelsperger in [#678](https://github.com/eclipse-tractusx/tractusx-edc/pull/678)
- chore(helm): add securityContext to Helm tests by @fty4 in [#637](https://github.com/eclipse-tractusx/tractusx-edc/pull/637)
- feat(DataPlaneConsumerProxy): adds support for data plane provider url by @wolf4ood in [#643](https://github.com/eclipse-tractusx/tractusx-edc/pull/643)
- chore(helm): remove tractusx-connector-legacy chart by @fty4 in [#684](https://github.com/eclipse-tractusx/tractusx-edc/pull/684)
- feat: improve bpn validation by @paullatzelsperger in [#687](https://github.com/eclipse-tractusx/tractusx-edc/pull/687)
- feat: add API for the BPN validation extension by @paullatzelsperger in [#688](https://github.com/eclipse-tractusx/tractusx-edc/pull/688)
- chore: suppress noisy kics warning on miw tests docker-compose by @wolf4ood in [#689](https://github.com/eclipse-tractusx/tractusx-edc/pull/689)
- feat: use new BPN Policy in artefacts and E2E tests by @paullatzelsperger in [#690](https://github.com/eclipse-tractusx/tractusx-edc/pull/690)
- docs: provide a multi-tenancy sample by @ndr-brt in [#691](https://github.com/eclipse-tractusx/tractusx-edc/pull/691)
- feat: generate, merge and publish OpenAPI spec 2 by @bcronin90 in [#619](https://github.com/eclipse-tractusx/tractusx-edc/pull/619)
- feat: download opentelemetry jar outside of Dockerfile by @paullatzelsperger in [#697](https://github.com/eclipse-tractusx/tractusx-edc/pull/697)
- feat(edrs): add init edr request api validator by @wolf4ood in [#703](https://github.com/eclipse-tractusx/tractusx-edc/pull/703)
- feat(edrs): add EDR api schema and example by @wolf4ood in [#705](https://github.com/eclipse-tractusx/tractusx-edc/pull/705)
- Fix: Add missing OpenAPI parameter by @bcronin90 in [#712](https://github.com/eclipse-tractusx/tractusx-edc/pull/712)
- feat(helm): add customCaCerts value by @fty4 in [#707](https://github.com/eclipse-tractusx/tractusx-edc/pull/707)
- chore(helm): fix typo for controlplane.endpoints.management.authKey by @fty4 in [#706](https://github.com/eclipse-tractusx/tractusx-edc/pull/706)
- chore: update to EDC 0.2.1 by @paullatzelsperger in [#716](https://github.com/eclipse-tractusx/tractusx-edc/pull/716)
- chore: add LICENSE file to all charts by @paullatzelsperger in [#722](https://github.com/eclipse-tractusx/tractusx-edc/pull/722)
- fix: add binding to tx namespace for credential policy by @wolf4ood in [#713](https://github.com/eclipse-tractusx/tractusx-edc/pull/713)
- Release version 0.5.1 by @github-actions in [#725](https://github.com/eclipse-tractusx/tractusx-edc/pull/725)

Full Changelog: [0.5.0_rn...0.5.1](https://github.com/eclipse-tractusx/tractusx-edc/compare/0.5.0_rn...0.5.1)

## [0.5.0] - 2023-06-11

0.5.0
What's Changed

- Release version 0.5.0-rc5 by @github-actions in [#570](https://github.com/eclipse-tractusx/tractusx-edc/pull/570)
- chore: update DEPENDENCIES, add approvals by @paullatzelsperger in [#572](https://github.com/eclipse-tractusx/tractusx-edc/pull/572)
- feat(docs): adds more information on MIW credential module by @wolf4ood in [#576](https://github.com/eclipse-tractusx/tractusx-edc/pull/576)
- feat(MIWClient): adds response body in case errors in MIW response by @wolf4ood in [#574](https://github.com/eclipse-tractusx/tractusx-edc/pull/574)
- chore: reformat workflow yaml files by @paullatzelsperger in [#578](https://github.com/eclipse-tractusx/tractusx-edc/pull/578)
- docs: add decision record about removing the CHANGELOG.md by @paullatzelsperger in [#579](https://github.com/eclipse-tractusx/tractusx-edc/pull/579)
- feat(SSI): adds tolerance to eventual trailing slash in MIW or token URLs by @wolf4ood in [#581](https://github.com/eclipse-tractusx/tractusx-edc/pull/581)
- fix(test): converted some MockWebServer to long-running instances by @paullatzelsperger in [#588](https://github.com/eclipse-tractusx/tractusx-edc/pull/588)
- feat(docs): updates postman collection by @wolf4ood in [#587](https://github.com/eclipse-tractusx/tractusx-edc/pull/587)
- feat(docs): add changes in consumer pull flow in the migration guide by @wolf4ood in [#591](https://github.com/eclipse-tractusx/tractusx-edc/pull/591)
- feat: remove CHANGELOG.md, generate automatically by @paullatzelsperger in [#583](https://github.com/eclipse-tractusx/tractusx-edc/pull/583)
- chore(SSI): add security context in cache by @wolf4ood in [#595](https://github.com/eclipse-tractusx/tractusx-edc/pull/595)
- chore(docs): adds a note about the cid removal by @wolf4ood in [#596](https://github.com/eclipse-tractusx/tractusx-edc/pull/596)
- feat(test): add integration tests for MIW by @paullatzelsperger in [#585](https://github.com/eclipse-tractusx/tractusx-edc/pull/585)
- feat(Gateway): forward EDR + refactor by @wolf4ood in [#597](https://github.com/eclipse-tractusx/tractusx-edc/pull/597)
- chore(deps): bump net.minidev:json-smart from 2.4.11 to 2.5.0 by @dependabot in [#593](https://github.com/eclipse-tractusx/tractusx-edc/pull/593)
- docs: update migration guide for 0.5.0 by @paullatzelsperger in [#601](https://github.com/eclipse-tractusx/tractusx-edc/pull/601)
- docs: add link to MIW to readme by @tmberthold in [#592](https://github.com/eclipse-tractusx/tractusx-edc/pull/592)
- Release version 0.5.0 by @github-actions in [#602](https://github.com/eclipse-tractusx/tractusx-edc/pull/602)

New Contributors
@tmberthold made their first contribution in [#592](https://github.com/eclipse-tractusx/tractusx-edc/pull/592)

Full Changelog: [0.5.0-rc5_rn...0.5.0_rn](https://github.com/eclipse-tractusx/tractusx-edc/compare/0.5.0-rc5_rn...0.5.0_rn)

## [0.5.0-rc5] - 2023-07-05

### 0.5.0-rc5 Changed

- Upgraded to EDC 0.1.3

## [0.5.0-rc4] - 2023-07-04

### 0.5.0-rc4 Removed

- Removed the interim solution `observability-api-customization` and use the upstream extension `Observability API` instead

## [0.5.0-rc3] - 2023-06-30

### 0.5.0-rc3 Fixed

- Replace '\_\_' with '--' in the Sql EDR Store (#538)
- Adapt Postman Collection for RC1/RC2 (#535)

## [0.5.0-rc2] - 2023-06-23

### 0.5.0-rc2 Changed

- Upgraded to EDC 0.1.2

## [0.5.0-rc1] - 2023-06-21

### 0.5.0-rc1 Fixed

- Various fixes and improvements to our helm charts

### 0.5.0-rc1 Added

- Support for SSI (centralized MiW) (#459, #510)
- Support for the JsonWebSignature2020 Crypto Suite (#483)

### 0.5.0-rc1 Changed

- All Helm charts now use SSI instead of DAPS (#511)

### 0.5.0-rc1 Removed

- Support for DAPS as identity provider (#511)

## [0.4.1] - 2023-05-31

### 0.4.1 Added

- SQL implementation for the EDR Cache
- E2E test variant using PostgreSQL
- Documentation

### 0.4.1 Changed

- Moved to Java 17
- Switched to Eclipse Dataspace Components `0.1.0`

### 0.4.1 Removed

- Lombok

## [0.4.0] - 2023-05-18

### 0.4.0 Added

- Support for the new Dataspace Protocol
- GitHub Workflow to check for missing license headers

### 0.4.0 Changed

- Switched to Eclipse Dataspace Components `0.0.1-milestone-9`

### 0.4.0 Removed

- Business tests. All tests are covered by other means.
- Control-Plane-Adapter. Replaced by a DSP-compatible implementation

## [0.3.4] - 2023-05-17

### 0.3.4 Fixed

- Added license headers to several files in the code base
- Refactoring of Helm charts - multiple charts instead of one dynamically assembled chart

## [0.3.3] - 2023-04-19

### 0.3.3 Fixed

- Config values for the data plane part of the helm chart
- Contract Validity

### 0.3.3 Added

- A log line whenever a policy evaluation of the BPN number was performed

## [0.3.2] - 2023-03-30

### 0.3.2 Fixed

- Fixed mutually-exclusive config values for Azure KeyVault

## [0.3.1] - 2023-03-27

### 0.3.1 Added

### 0.3.1 Changed

- Support unauthenticated access to the ObservabilityAPI (#126)

### 0.3.1 Fixed

## [0.3.0] - 2023-02-20

**Important Note**: This version introduces multiple breaking changes. Before updating **always** consolidate the
corresponding [documentation](/docs/migration/Version_0.1.x_0.3.x.md).

### 0.3.0 Added

- Add contract id to data source http call (#732)
- Support also support releases in ci pipeline
- Introduce typed object for oauth2 provisioning
- Add documentation
- Add test case
- Add client to omejdn
- add hydra deployment
- Configure dynamically HTTP Receiver callback endpoints. (#685)
- cp-adapter : code review, rollbacke name change (#664)
- Feature/cp adapter task 355 356 357 (#621)
- Add Validity Mapping in ContractDefinitionStepDefs class
- Add feature and create SendAnOfferwithoutConstraints method in class negotiationSteps
- Add validity attribute in class ContractDefinition
- Add Validity Mapping in ContractDefinitionStepDefs class
- Add feature and create SendAnOfferwithoutConstraints method in class negotiationSteps
- Add validity attribute in class ContractDefinition
- Local TXDC Setup Documentation (#618)
- Feature: Sftp Provisioner and Client (#554)

### 0.3.0 Changed

- Support horizontal edc scaling in cp adapter extension (#678)
- Use upstream jackson version (#741)
- Replace provision-oauth2 with data-plane-http-oauth2
- docs: Update sample documentation (#671)
- chore: Disable build ci pipeline if just docu was updated (#705)
- Increase trivy timeout
- Remove not useful anymore custom-jsonld extension (#683)
- update setup docu (#654)
- remove trailing slash (#652)
- update alpine from 3.17.0 to 3.17.1 for controlplane-memory-hashicorp-vault (#665)
- Feature/set charts deprecated (#628)
- update setup docu (#627)
- Feature/update txdc deployment downward capabilities (#625)
- remove git submodule (#619)
- Feature/update postman (#624)
- update control plane docu (#623)
- update postgresql version in Chart.yaml supporting-infrastructure (#622)
- update link to edc logo in README.md (#612)
- update description of supporting infrastructure deployment (#616)

### 0.3.0 Fixed

- bugfix: Fix slow AES encryption (#746)
- Fix typo in tractusx-connector values.yaml comment
- Fix not working docu link in README.md
- Fix typo in control-plane adapter README

### 0.3.0 Dependency updates

- Bump EDC to 20220220 (#767)
- Bump alpine (#749)
- Bump alpine (#750)
- Bump alpine (#752)
- Bump alpine in /edc-controlplane/edc-runtime-memory/src/main/docker (#753)
- Bump maven-deploy-plugin from 3.0.0 to 3.1.0 (#735)
- Bump actions/setup-java from 3.9.0 to 3.10.0 (#730)
- Bump s3 from 2.19.33 to 2.20.0
- Bump s3 from 2.19.27 to 2.19.33
- Bump jaxb-runtime from 4.0.1 to 4.0.2
- Bump spotless-maven-plugin from 2.31.0 to 2.32.0
- Bump postgresql from 42.5.1 to 42.5.3
- Bump nimbus-jose-jwt from 9.30 to 9.30.1
- Bump lombok from 1.18.24 to 1.18.26
- Bump flyway-core from 9.12.0 to 9.14.1
- Bump jackson-bom from 2.14.0-rc2 to 2.14.2
- Bump cucumber.version from 7.11.0 to 7.11.1
- Bump azure-sdk-bom from 1.2.8 to 1.2.9
- Bump mockito-bom from 5.0.0 to 5.1.1
- Bump edc version to 0.0.1-20230131-SNAPSHOT
- Bump s3 from 2.19.18 to 2.19.27
- Bump docker/build-push-action from 3 to 4
- Bump nimbus-jose-jwt from 9.29 to 9.30
- Bump spotless-maven-plugin from 2.30.0 to 2.31.0
- Bump nimbus-jose-jwt from 9.28 to 9.29
- Bump mockito-bom from 4.11.0 to 5.0.0
- Bump edc version to 0.0.1-20230125-SNAPSHOT
- Bump flyway-core from 9.11.0 to 9.12.0
- Bump s3 from 2.19.15 to 2.19.18 (#684)
- Bump mikefarah/yq from 4.30.6 to 4.30.8 (#682)
- Bump spotless-maven-plugin from 2.29.0 to 2.30.0
- Bump edc version to 0.0.1-20230115-SNAPSHOT
- Bump cucumber.version from 7.10.1 to 7.11.0 (#672)
- Bump maven-dependency-plugin from 3.4.0 to 3.5.0 (#669)
- Bump s3 from 2.19.11 to 2.19.15 (#668)
- Bump maven-surefire-plugin from 3.0.0-M7 to 3.0.0-M8 (#670)
- Bump edc version to 0.0.1-20230109-SNAPSHOT (#666)
- Bump alpine in /edc-controlplane/edc-runtime-memory/src/main/docker (#659)
- Bump alpine in /edc-dataplane/edc-dataplane-azure-vault/src/main/docker (#660)
- Bump alpine (#658)
- Bump alpine (#661)
- Bump alpine (#662)
- Bump azure/setup-kubectl from 3.1 to 3.2 (#655)
- Bump junit-bom from 5.9.1 to 5.9.2 (#657)
- Bump s3 from 2.19.2 to 2.19.11 (#648)
- Bump actions/checkout from 3.2.0 to 3.3.0 (#647)
- Bump flyway-core from 9.10.2 to 9.11.0 (#646)
- Bump spotless-maven-plugin from 2.28.0 to 2.29.0 (#641)
- Bump mockito-bom from 4.10.0 to 4.11.0 (#637)
- Bump flyway-core from 9.10.1 to 9.10.2 (#632)
- Bump s3 from 2.19.1 to 2.19.2 (#631)
- Bump s3 from 2.18.41 to 2.19.1 (#626)
- Bump mikefarah/yq from 4.30.5 to 4.30.6 (#613)
- Bump cucumber.version from 7.10.0 to 7.10.1 (#614)
- Bump s3 from 2.18.40 to 2.18.41 (#615)
- Bump azure/setup-helm from 3.4 to 3.5 (#596)
- Bump actions/checkout from 3.1.0 to 3.2.0 (#598)
- Bump mockito-bom from 4.9.0 to 4.10.0 (#607)
- Bump s3 from 2.18.39 to 2.18.40 (#609)
- Bump flyway-core from 9.10.0 to 9.10.1 (#610)
- Bump actions/setup-java from 3.8.0 to 3.9.0 (#605)
- Bump s3 from 2.18.35 to 2.18.39 (#606)

## [0.1.6] - 2023-02-20

### 0.1.6 Fixed

- SQL leakage issue
- Catalog pagination

## [0.1.5] - 2023-02-13

### 0.1.5 Fixed

- Use patched EDC version: 0.0.1-20220922.2-SNAPSHOT to fix catalog pagination bug
- Data Encryption extension: fixed usage of a blocking algorithm

## [0.1.2] - 2022-09-30

### 0.1.2 Added

- Introduced DEPENDENCIES file

### 0.1.2 Changed

- Moved helm charts from `deployment/helm` to `charts`
- Replaced distroless image with alpine in all docker images
- Update EDC commit to `740c100ac162bc41b1968c232ad81f7d739aefa9`

## [0.1.1] - 2022-09-04

**Important Note**: Please consolidate the migration documentation before updating your
connector. [documentation](/docs/migration/Version_0.1.0_0.1.1.md).

### 0.1.1 Added

- Control-Plane Extension ([cx-oauth2](/edc-extensions/cx-oauth2/README.md))

### 0.1.1 Changed

- Introduced git submodule to import EDC dependencies (instead of snapshot- or milestone artifact)
- Helm Charts: TLS secret name is now configurable

### 0.1.1 Fixed

- Connectors with Azure Vault extension are now starting
  again [link](https://github.com/eclipse-edc/Connector/issues/1892)

## [0.1.0] - 2022-08-19

**Important Note**: Version 0.1.0 introduces multiple breaking changes. Before updating **always** consolidate the
corresponding [documentation](/docs/migration/Version_0.0.x_0.1.x.md).

### 0.1.0 Added

- Control-Plane extension ([data-plane-selector-client](https://github.com/eclipse-edc/Connector/tree/v0.0.1-milestone-5/extensions/data-plane-selector/selector-client))
- run the EDC with multiple data planes at once
- Control-Plane extension ([dataplane-selector-configuration](edc-extensions/dataplane-selector-configuration))
- add data plane instances to the control plane by configuration
- Data-Plane extension ([s3-data-plane](https://github.com/eclipse-edc/Connector/tree/main/extensions/aws/data-plane-s3))
- transfer from and to AWS S3 buckets
- Control-Plane extension ([data-encryption](edc-extensions/data-encryption))
- Data-Plane authentication attribute transmitted during data-plane-transfer can be encrypted symmetrically (AES)

### 0.1.0 Changed

- Update setting name (`edc.dataplane.token.validation.endpoint` -> `edc.dataplane.token.validation.endpoint`)
- EDC has been updated to
  version [0.0.1-20220818-SNAPSHOT](https://oss.sonatype.org/#nexus-search;gav~org.eclipse.dataspaceconnector~~0.0.1-20220818-SNAPSHOT~~) -
  implications to the behavior of the connector have been covered in
  the [corresponding migration guide](docs/migration/Version_0.0.x_0.1.x.md)

### 0.1.0 Fixed

- Contract-Offer-Receiving-Connectors must also pass the ContractPolicy of the ContractDefinition before receiving
  offers([issue](https://github.com/eclipse-edc/Connector/issues/1331))
- Deletion of Asset becomes impossible when Contract Negotiation
  exists([issue](https://github.com/eclipse-edc/Connector/issues/1403))
- Deletion of Policy becomes impossible when Contract Definition
  exists([issue](https://github.com/eclipse-edc/Connector/issues/1410))

## [0.0.6] - 2022-07-29

### 0.0.6 Fixed

- Fixes [release 0.0.5](https://github.com/eclipse-tractusx/tractusx-edc/releases/tag/0.0.5), which introduced classpath
  issues due to usage of [net.jodah:failsafe:2.4.3](https://search.maven.org/artifact/net.jodah/failsafe/2.4.3/jar)
  library

## [0.0.5] - 2022-07-28

### 0.0.5 Added

- EDC Health Checks for HashiCorp Vault

### 0.0.5 Changed

- BusinessPartnerNumber constraint supports List structure
- Helm: Confidential EDC settings can be set using k8s secrets
- HashiCorp Vault API path configurable

## [0.0.4] - 2022-06-27

### 0.0.4 Added

- HashiCorp Vault Extension
- Control Plane with HashiCorp Vault and PostgreSQL support

### 0.0.4 Changed

- Release Workflow now publishes EDC Extensions as Maven Artifacts

### 0.0.4 Fixed

- [#1515](https://github.com/eclipse-edc/Connector/issues/1515) SQL: Connector sends out 50
  contract offers max.

### 0.0.4 Removed

- CosmosDB Control Plane
- Control API Extension for all Control Planes

## [0.0.3] - 2022-05-23

## [0.0.2] - 2022-05-20

## [0.0.1] - 2022-05-13

[0.5.1]: https://github.com/eclipse-tractusx/tractusx-edc/compare/0.5.0_rn...0.5.1

[0.5.0]: https://github.com/eclipse-tractusx/tractusx-edc/compare/0.5.0-rc5_rn...0.5.0_rn

[0.5.0-rc5]: https://github.com/eclipse-tractusx/tractusx-edc/compare/0.5.0-rc4...0.5.0-rc5

[0.5.0-rc4]: https://github.com/eclipse-tractusx/tractusx-edc/compare/0.5.0-rc3...0.5.0-rc4

[0.5.0-rc3]: https://github.com/eclipse-tractusx/tractusx-edc/compare/0.5.0-rc2...0.5.0-rc3

[0.5.0-rc2]: https://github.com/eclipse-tractusx/tractusx-edc/compare/0.5.0-rc1...0.5.0-rc2

[0.5.0-rc1]: https://github.com/eclipse-tractusx/tractusx-edc/compare/0.4.1...0.5.0-rc1

[0.4.1]: https://github.com/eclipse-tractusx/tractusx-edc/compare/0.3.3...0.4.1

[0.3.3]: https://github.com/eclipse-tractusx/tractusx-edc/compare/0.3.2...0.3.3

[0.3.2]: https://github.com/eclipse-tractusx/tractusx-edc/compare/0.3.1...0.3.2

[0.3.1]: https://github.com/eclipse-tractusx/tractusx-edc/compare/0.3.0...0.3.1

[0.3.0]: https://github.com/eclipse-tractusx/tractusx-edc/compare/0.2.0...0.3.0

[0.1.6]: https://github.com/eclipse-tractusx/tractusx-edc/compare/0.1.5...0.1.6

[0.1.5]: https://github.com/eclipse-tractusx/tractusx-edc/compare/0.1.2...0.1.5

[0.1.2]: https://github.com/eclipse-tractusx/tractusx-edc/compare/0.1.1...0.1.2

[0.1.1]: https://github.com/eclipse-tractusx/tractusx-edc/compare/0.1.0...0.1.1

[0.1.0]: https://github.com/eclipse-tractusx/tractusx-edc/compare/0.0.6...0.1.0

[0.0.6]: https://github.com/eclipse-tractusx/tractusx-edc/compare/0.0.5...0.0.6

[0.0.5]: https://github.com/eclipse-tractusx/tractusx-edc/compare/0.0.4...0.0.5

[0.0.4]: https://github.com/eclipse-tractusx/tractusx-edc/compare/0.0.3...0.0.4

[0.0.3]: https://github.com/eclipse-tractusx/tractusx-edc/compare/0.0.2...0.0.3

[0.0.2]: https://github.com/eclipse-tractusx/tractusx-edc/compare/0.0.1...0.0.2

[0.0.1]: https://github.com/eclipse-tractusx/tractusx-edc/compare/a02601306fed39a88a3b3b18fae98b80791157b9...0.0.1
