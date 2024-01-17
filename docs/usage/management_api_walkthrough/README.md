# Management API Overview

## Introduction

This walkthrough attempts to be a reference for systems integrators attempting to expose APIs safely to the Catena-X
dataspace.
Please note that improper usage of the Management-API can lead to accidental exposure of competitively sensitive data
and trade secrets. The assumption is that the systems integrator has two tractusx-edc deployments of version 0.5.1 or
higher available (one acting as provider, one acting as consumer).

The EDCimplements the [Dataspace Protocol (DSP)](https://docs.internationaldataspaces.org/dataspace-protocol/overview/readme),
as specified by the IDSA. As the DSP uses JSON-LD for all payloads, the EDC Management API reflects this as well, even
though it is not a part of the DSP.

## Endpoints

The `MANAGEMENT_URL` specifies the URL of the management API and the prefixes `v2` and `v3` respect the fact that the
endpoints are currently versioned independently of each other.

| Resource                                           | Endpoint                                   | Involved Actors                           |
|----------------------------------------------------|--------------------------------------------|-------------------------------------------|
| [Asset](01_assets.md)                              | `<MANAGEMENT_URL>/v3/assets`               | Provider Admin & Provider EDC             |
| [Policy Definition](02_policies.md)                | `<MANAGEMENT_URL>/v2/policydefinitions`    | Provider Admin & Provider EDC             |
| [Contract Definition](03_contractdefinitions.md)   | `<MANAGEMENT_URL>/v2/contractdefinitions`  | Provider Admin & Provider EDC             |
| [Catalog](04_catalog.md)                           | `<MANAGEMENT_URL>/v2/catalog`              | Consumer App, Consumer EDC & Provider EDC |
| [Contract Negotiation](05_contractnegotiations.md) | `<MANAGEMENT_URL>/v2/contractnegotiations` | Consumer App, Consumer EDC & Provider EDC |
| [Contract Agreement](08_contractagreements.md)     | `<MANAGEMENT_URL>/v2/contractagreements`   | Provider Admin & Provider EDC             |
| [Transfer Process](06_transferprocesses.md)        | `<MANAGEMENT_URL>/v2/transferprocesses`    | Consumer App, Consumer EDC & Provider EDC |
| [EDR](07_edrs.md)                                  | `<MANAGEMENT_URL>/edrs`                    | Consumer App, Consumer EDC & Provider EDC |
| Data Plane                                         | `<DATAPLANE_URL>`                          | Consumer App & Provider EDC               |

## OpenAPI

The most recent OpenApi documentation can be found on SwaggerHub.
https://app.swaggerhub.com/apis/eclipse-tractusx-bot/tractusx-edc/

## Brief JSON-LD Introduction

JSON-LD (JSON for Linked Data) is an extension of JSON that introduces a set of principles and mechanisms to serialize
RDF-graphs and thus open new opportunities for interoperability. As such, there is a clear separation into identifiable
resources (IRIs) and Literals holding primitive data like strings or integers.For developers used to working with JSON,
JSON-LD can act in unexpected ways, for example a list with one entry will always unwrap to an object which may cause
schema validation to fail on the client side. Please also refer to
the [JSON-LD spec](https://www.w3.org/TR/json-ld11/) and try it out on
the [JSON-LD Playground](https://json-ld.org/playground/).

### Keywords

JSON-LD includes several important keywords that play a crucial role in defining the structure, semantics, and
relationships
within a JSON-LD document. Since some keys which are required in requests for the new management API aren't
self-explanatory
when you first see them, here are some of the most commonly used and important keywords in JSON-LD.
These keys are generally part of the JSON-LD spec and serve as identification on a larger scope.

| Key      | Description                                                                                                                                                                                               |
|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| @context | Specifies the context for interpreting the meaning of terms and properties within a JSON-LD document. It associates terms with namespaces, vocabularies, or URLs.                                         |
| @vocab   | Sets a default namespace or vocabulary for expanding terms within a JSON-LD document. It allows for a more concise representation of properties by omitting the namespace prefix for commonly used terms. |
| @id      | Represents the unique identifier (URI or IRI) for a node or resource within a JSON-LD document. It allows for linking and referencing resources.                                                          |
| @type    | Indicates the type(s) of a node or resource. It is used to specify the class or classes that the resource belongs to, typically using terms from a vocabulary or ontology.                                |

### Namespaces

A namespace is defined by associating a prefix with a URI or IRI in the @context of a JSON-LD document. The prefix is
typically a short string, while the URI or IRI represents a namespace or vocabulary where the terms or properties are
defined.

Some namespaces are known to the EDC internally. That means that the EDC will resolve all resources to non-prefixed IRIs
given they are not part of the following list:

| Key    | Description                                                                                                                                                                                                         |
|--------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| dct    | Defines the prefix "dct" and associates it with the URI "<https://purl.org/dc/terms/>". The prefix "dct" can now be used in the JSON-LD document to represent terms from the Dublin Core Metadata Terms vocabulary. |
| edc    | Defines the prefix "edc" and associates it with the URI "<https://w3id.org/edc/v0.0.1/ns/>". The prefix "edc" can now be used to represent terms from the EDC (Eclipse Dataspace Connect) vocabulary.               |
| dcat   | Defines the prefix "dcat" and associates it with the URI "<https://www.w3.org/ns/dcat/>". The prefix "dcat" can now be used to represent terms from the DCAT (Data Catalog Vocabulary) vocabulary.                  |
| odrl   | Defines the prefix "odrl" and associates it with the URI "<http://www.w3.org/ns/odrl/2/>". The prefix "odrl" can now be used to represent terms from the ODRL (Open Digital Rights Language) vocabulary.            |
| dspace | Defines the prefix "dspace" and associates it with the URI "<https://w3id.org/dspace/v0.8/>". The prefix "dspace" can now be used to represent terms from the DSpace vocabulary.                                    |

> Please note: The namespace `edc` currently is only a placeholder and does not lead to any JSON-LD context definition
> or vocabulary.
> This may change at a later date.
> Please note: In our samples, except from `odrl` vocabulary terms that must override `edc` default prefixing,
> properties **WILL NOT** be explicitly namespaced, and internal nodes **WILL NOT** be typed, relying on `@vocab`
> prefixing and root schema type inheritance respectively.

### More documentation and learning resources

- Setup of EDC infrastructure:
    - Read
      the ["Connect" section of the E2E-Tutorial](https://eclipse-tractusx.github.io/docs/tutorials/e2e/connect/prepareInfrastructure)
      for first steps. It provides an easy-to-start preconfigured deployment of critical Catena-X infrastructure
      components.
    - The [MXD documentation](https://eclipse-edc.github.io/docs/#/submodule/MinimumViableDataspace/docs/developer/continuous-deployment/continuous_deployment) has a similar section on its setup.
    - To deploy and configure the Tractus-X EDC, check
      its [documentation](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/README.md).
- Exchanging data via two EDCs:
    - Via API: ["Boost" section of the E2E-Tutorial](https://eclipse-tractusx.github.io/docs/tutorials/e2e/boost/). It
      is
      exemplary and non-comprehensive.
    - Via
      Frontend: [The MXD-Documentation ](https://eclipse-edc.github.io/docs/#/submodule/MinimumViableDataspace/docs/developer/?id=scenarios-covered)
      explains how to use the open-source web-view.
- [Eclipse-EDC Samples](https://github.com/eclipse-edc/Samples): This repo includes a wide variety of setups - many of
  which
  go beyond this Kit in scope but not in detail.
- openApi-definitions
    - [tractusx-edc](https://app.swaggerhub.com/apis/eclipse-tractusx-bot/tractusx-edc/0.5.1-SNAPSHOT)
    - components-edc: There's separate pages for
      the [Management-API](https://app.swaggerhub.com/apis/eclipse-edc-bot/management-api) and
      the [Control-API](https://app.swaggerhub.com/apis/eclipse-edc-bot/control-api).

## Notice

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- SPDX-License-Identifier: CC-BY-4.0
- SPDX-FileCopyrightText: 2023 Contributors of the Eclipse Foundation
- Source URL: [https://github.com/eclipse-tractusx/tractusx-edc](https://github.com/eclipse-tractusx/tractusx-edc)