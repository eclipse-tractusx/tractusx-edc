# Management API V2 Overview

## Introduction 

With the introduction of the new [Dataspace Protocol](https://docs.internationaldataspaces.org/dataspace-protocol/overview/readme), now using JSON-LD, all Management API endpoints had to be adapted as well to reflect that.
JSON-LD (JSON for Linked Data) is an extension of JSON that introduces a set of principles and mechanisms to enable interoperability.

This document will showcase how this change impacts the management API usage.

> Please note: Before running the examples the corresponding environment variables must be set.
> How such an environment can be setup locally is documented in [settings changes](../Version_0.3.4_0.4.0.md).

## 1. Modified Endpoints

The `MANAGEMENT_URL` specifies the URL of the management API and the prefix `v2` allows access to the functionality of the new management API. 

| Resource              | Endpoint                                   |
|-----------------------|--------------------------------------------|
| Asset                 | `<MANAGEMENT_URL>/v2/assets`               |
| Policy Definition     | `<MANAGEMENT_URL>/v2/policydefinitions`    |
| Contract Definition   | `<MANAGEMENT_URL>/v2/contractdefinitions`  |
| Catalog               | `<MANAGEMENT_URL>/v2/catalog`              |
| Contract Negotiation  | `<MANAGEMENT_URL>/v2/contractnegotiations` |
| Contract Agreement    | `<MANAGEMENT_URL>/v2/contractagreements`   |
| Transfer Process      | `<MANAGEMENT_URL>/v2/transferprocesses`    |

> Please note: The old Management API is now deprecated and is not tested for compliance. The `/v2` prefix is only a temporary part of the path and will be discarded once the migration to the new protocol is finished and the old API is taken out of service. Once that is done, the new management API can be reached through `MANAGEMENT_URL`.

## 2. Brief JSON-LD Introduction
JSON-LD includes several important keywords that play a crucial role in defining the structure, semantics, and relationships within a JSON-LD document. Since some keys which are required in requests for the new management API aren't self-explanatory when you first see them, here are some of the most commonly used and important keywords in JSON-LD.
These keys are generally part of the JSON-LD spec and serve as identification on a larger scope.

### Keywords

| Key       | Description                                                                                                                                                                                               |
|-----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| @context  | Specifies the context for interpreting the meaning of terms and properties within a JSON-LD document. It associates terms with namespaces, vocabularies, or URLs.                                         |
| @vocab    | Sets a default namespace or vocabulary for expanding terms within a JSON-LD document. It allows for a more concise representation of properties by omitting the namespace prefix for commonly used terms. |
| @id       | Represents the unique identifier (URI or IRI) for a node or resource within a JSON-LD document. It allows for linking and referencing resources.                                                          |
| @type     | Indicates the type(s) of a node or resource. It is used to specify the class or classes that the resource belongs to, typically using terms from a vocabulary or ontology.                                |

### Namespaces
A namespace is defined by associating a prefix with a URI or IRI in the @context of a JSON-LD document. The prefix is typically a short string, while the URI or IRI represents a namespace or vocabulary where the terms or properties are defined.

| Key    | Description                                                                                                                                                                                                       |
|--------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| dct    | Defines the prefix "dct" and associates it with the URI "https://purl.org/dc/terms/". The prefix "dct" can now be used in the JSON-LD document to represent terms from the Dublin Core Metadata Terms vocabulary. |
| edc    | Defines the prefix "edc" and associates it with the URI "https://w3id.org/edc/v0.0.1/ns/". The prefix "edc" can now be used to represent terms from the EDC (Eclipse Dataspace Connect) vocabulary.               |
| dcat   | Defines the prefix "dcat" and associates it with the URI "https://www.w3.org/ns/dcat/". The prefix "dcat" can now be used to represent terms from the DCAT (Data Catalog Vocabulary) vocabulary.                  |
| odrl | Defines the prefix "odrl" and associates it with the URI "http://www.w3.org/ns/odrl/2/". The prefix "odrl" can now be used to represent terms from the ODRL (Open Digital Rights Language) vocabulary.            |
| dspace | Defines the prefix "dspace" and associates it with the URI "https://w3id.org/dspace/v0.8/". The prefix "dspace" can now be used to represent terms from the DSpace vocabulary. |


> Please note: The namespaces `edc` currently is only a placeholder and do not lead to any JSON-LD context definition or vocabulary.
> This will change at a later date.

## 3. Walkthrough
The steps can be followed using the updated [postman collection](../../development/postman/collection.json).

1. [Create an Asset](2-assets.md)
2. [Create a Policy Definition](3-policy-definitions.md)
3. [Create Contract Definition](4-contract-definitions.md)
4. [Fetch provider's Catalog](5-catalog.md)
5. [Initiate Contract Negotiation](6-contract-negotiation.md)
6. [Initiate Transfer Process](7-transfer-process.md)