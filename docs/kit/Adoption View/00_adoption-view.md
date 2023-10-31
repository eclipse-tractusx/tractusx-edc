---
id: Adoption View
title: Adoption View
description: 'Connector Kit'
sidebar_position: 1
---

## Introduction

The ConnectorKit provides a connector framework, based on the [Eclipse Dataspace Connector][edc-url] for sovereign, cross-enterprise data exchange.

![EDC Overview](images/edc_overview.png)

With the [EDC][edc-url], a new communication component was introduced, which implements the following architectural principles:

- Simple. Maintaining a small and efficient core with as few external dependencies as possible.
- Interoperable. Independent of platforms and ecosystems.
- Decentralized. Software components with the necessary capabilities for participating in a Data Space are located on the partners' side, data is only exchanged within agreed contracts.
- Data protection is more important than data sharing. Data to be transmitted is fundamentally linked to policies via contracts; a transfer without a contract is not possible.
- Separation of metadata and data. Enables high throughput rates for the actual data transfer.
- Consistent semantics. Is the basis for interoperability and digital value creation.
- Automation. As far as possible, all processes, starting with determining the identity, through ensuring the contractually agreed regulations to data transmission, are automated.
- Standardization. Existing standards and protocols ([GAIA-X][gaiax-url] and [IDSA][idsa-url]) are used as far as possible.

## Use Case

The EDC is the enabling component for all use cases within Catena-X and the only component to execute the exchange of operational data to Data Space participants. The base use case is therfore the exchange of data between to partners. Value creation along the automotive value chain is tied to processing data in specific contexts. Within Catena-X exist 10 use cases companies can participate in. Active participation in given use cases always requires the use of an [EDC][edc-url]. Depending on the use case a participant chooses to participate in, further components will be required.

- [Sustainability][sustainability-url]
- [Traceability][traceability-url]
- [Demand and Capacity Management][DCM-url]
- [Predictive Unit Real-Time Information Service (PURIS)][PURIS-url]
- [Business Partner Data Management][BPDM-url]
- [Digital Product pass][digital-product-pass-url]

## Business Architecture

The [EDC][edc-url] as a connector implements a framework agreement for sovereign, cross-organizational data exchange. The International Data Spaces Standard (IDS) and relevant principles in connection with [GAIA-X][gaiax-url] were implemented. The connector is designed to be extensible to support alternative protocols and to be integrated into different ecosystems.

The objective is to set up a decentralized software component on the part of the respective partner, which bundles the skills required to participate in a Data Space and enables peer-to-peer connections between participants.
The focus here is particularly on the data sovereignty of the independent companies.
The functionality required for this is bundled in the open-source project "Eclipse Dataspace Connector", to which members of the Eclipse Foundation contribute.

The main difference between the EDC and the previous connectors of the [IDSA][idsa-url] is the separation of the communication into a channel for the metadata and one for the actual data exchange. The channel for the data supports various transmission protocols via so-called data plane extensions. The metadata is transmitted directly via the EDC interface, while the actual data exchange then takes place via the appropriate channel extension. In this way, a highly scalable data exchange is made possible.

![EDC Architecture](images/edc_architecture.png)

The architecture of the EDC combines various services that are necessary for the above principles:

- An interface to the Identity Provider service, currently the [Managed identity Wallet][miw-url]. This central service provides the identity and the corresponding authentication of the participants in the data exchange. Decentralized solutions will also be supported in the future, following [Gaia-X]'s[gaiax-url] principles and implementing self sovereign identity.
- The provision of possible offers (contract offering) which, on the one hand, stipulates the data offered and the associated terms of use (policies) in corresponding contracts.
- An interface for manual selection of data and associated contract offers.
- The actual data transfer via the data plane extension
- The connection of software systems on the customer and provider side

## Additional Resources

### Catena-X Standards

The Connector KIT builds on the [Catena-X Standards][Catena-X-Standards-url]. Every data consumer and provider in Catena-X is required to comply with these standards. The [EDC][edc-url] builds on the following standards:

- [CX - 0002 Digital Twins in Catena - X][CX0002]
- [CX - 0003 BAMM Aspect Meta Model][CX0003]
- [CX - 0008 Relevant standards for conformity assessments][CX0008]
- [CX - 0018 Eclipse Data Space Connector (EDC)][CX0018]
- [CX - 0044 ECLASS][CX044]
- [CX - 0047 DCM Data Model Material Demand & Capacity Group][CX0047]
- [CX - 0059 Triangle Behavioral Twin Endurance Predictor][CX0059]

### Terminology

![domain-model](images/domain-model.png)

> The shown picture illustrates only a generic view of the Domain Model and is not intended to show all aspects of the project.

#### Extensions

There are different extenions for the Connector, e.g. for the Data Plane. This enables various tranfer modes like httpData or via blob-storage.

#### Data Plane

The Data Plane handles several forms of actual data exchange by utilizing various extensions.

#### Control Plane

The Control Plane handles meta data exchange with other components and Connectors, as well as transfer of access tokens

#### Data Assets

Data Sources (databases, files, cache information, etc.) are connected to the Connector and are represented by Data Assets. For each asset, a [`DataAddress`](#data-address) needs to be resolvable.

#### Data address

A data address is a pointer into the physical storage location where an asset will be stored.

#### Policy Definition

A standardized set of policies can be used to define actions regarding access to and usage of assets. These actions can be limited further by constraints (temporal or spatial) and duties ("e.g. deletion of the data after 30 days").

#### Contract Definition

By combining [`Assets`](#data asset) and Policies, Contracts for data offerings are defined. These Contracts need to be accepted by consuming participants (Connectors) for the data exchange to take place.

#### Contract offer

The contract offer is a representation of the [`ContractDefinition`](#contract-definition) for a specific consumer and serves as protocol for a data transfer object (DTO) for a particular contract negotiation. If a data consumer wants to conclude a binding data exchange contract based on the terms of a Data Offer, the data consumer can communicate such desire to the data provider by referencing to a specific Data Offer. This constitutes a binding offer by the data consumer. For now, the data consumer only has the option to accept all terms of a Data Offer (or not). The Data Exchange Process does not yet provide for the data consumer to make an offer that deviates from the terms of a Data Offer as set by the data provider.

#### Contract negotiation

A `ContractNegotiation` captures the current state of the negotiation of a contract (`ContractOffer` ->
`ContractAgreement`) between two parties. This process is inherently asynchronous.

#### Contract agreement

A contract agreement represents the agreed-upon terms of access and usage of an asset's data between two data space
participants, including a start and an end date and further relevant information.

#### Transfer process

After a successful contract negotiation, `DataRequests` can be sent from the consumer connector to the provider connector to initiate the data transfer. It references the requested [`Asset`](#data asset) and [`ContractAgreement`](#contract-agreement) as well as information about the [data destination](#data-address).

Similar to the `ContractNegotiation`, this object captures the current state of a data transfer. This process is
inherently asynchronous, so the `TransferProcess` objects are stored in a backing data store (`TransferProcessStore`).

## Notice

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- SPDX-License-Identifier: CC-BY-4.0
- SPDX-FileCopyrightText: 2023 sovity GmbH
- SPDX-FileCopyrightText: 2023 msg systems AG
- SPDX-FileCopyrightText: 2023 Mercedes-Benz Group AG
- Source URL: [https://github.com/eclipse-tractusx/tractusx-edc](https://github.com/eclipse-tractusx/tractusx-edc)

[edc-url]: https://github.com/eclipse-edc/Connector
[gaiax-url]: https://www.data-infrastructure.eu/GAIAX/Navigation/EN/Home/home.html
[idsa-url]: https://internationaldataspaces.org/
[miw-url]: https://github.com/eclipse-tractusx/managed-identity-wallet
[traceability-url]: https://github.com/eclipse-tractusx/traceability-foss
[sustainability-url]: https://github.com/ChristopherWinterZF/pcf-exchange-kit/tree/featurebranch/devlopmentview
[BPDM-url]: https://github.com/eclipse-tractusx/bpdm
[DCM-url]: https://github.com/eclipse-tractusx/demand-capacity-mgmt/blob/main/README.md
[PURIS-url]: https://github.com/eclipse-tractusx/puris
[digital-product-pass-url]:https://github.com/eclipse-tractusx/digital-product-pass
[Catena-X-Standards-url]:https://catena-x.net/de/standard-library
[CX0002]:https://catena-x.net/fileadmin/user_upload/Standard-Bibliothek/Archiv/Update_Juli_23_R_3.2/CX-0002-DigitalTwinsInCatena-X-v.1.0.2.pdf
[CX0003]:https://catena-x.net/fileadmin/user_upload/Standard-Bibliothek/Archiv/Update_Juli_23_R_3.2/CX-0003-SAMMSemanticAspectMetaModel-v.1.0.2.pdf
[CX0008]:https://catena-x.net/fileadmin/user_upload/Standard-Bibliothek/Update_PDF_Maerz/6_Onboarding/CX_-_0008_Conformity_Assessment_PlatformCapabilityOnboarding_v_1.0.1.pdf
[CX0018]:https://catena-x.net/fileadmin/user_upload/Standard-Bibliothek/Update_PDF_Maerz/3_Sovereign_Data_Exchange/CX_-_0018_EDC_PlatformCapabilitySovereignDataExchange_v_1.0.1.pdf
[CX044]:https://catena-x.net/fileadmin/user_upload/Standard-Bibliothek/Archiv/Update_Juli_23_R_3.2/CX-0044-ECLASS-v1.0.2.pdf
[CX0047]:https://catena-x.net/fileadmin/user_upload/Standard-Bibliothek/Archiv/Update_Juli_23_R_3.2/CX-0047-DemandCapacityManagementDataModels-v1.0.0.pdf
[CX0059]:https://catena-x.net/fileadmin/user_upload/Standard-Bibliothek/Archiv/Update_Juli_23_R_3.2/CX-0059-TriangleBehavioralTwinEndurancePredictorService-v.1.0.0.pdf
