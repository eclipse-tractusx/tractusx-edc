# Fetching a Provider's Catalog

The catalog API is the first request in a data transfer sequence. It is executed by the Data
Consumer against their own control plane and triggers the retrieval of a catalog of data offers from a specified Data Provider.
Before executing a catalog request, a data consumer must identify which versions of the DSP the data provider supports
and select one to use in the data transfer request chain.

## Discovering DSP versions and parameters

As explained in the Dataspace Protocol document
_"Connectors implementing the Dataspace Protocol may operate on different versions and bindings.
Therefore, it is necessary that they can discover such information reliably and unambiguously"._
The 2025-1 specification dictates that each connector should expose a commonly identifiable and publicly available version
metadata endpoint location (at `/.well-known/dspace-version`)
which dataspace participants should use to discover which versions of the protocol are supported by a Connector.

To ease the discovery of available and supported DSP versions of a Connector, the tractusx-edc project makes available
an API endpoint that proxies the request to the metadata endpoint and returns the corresponding parameters for the
latest supported DSP version.

DSP parameter discovery is done via the following request:

```http request
POST /v4alpha/connectordiscovery/dspversionparams HTTP/1.1
Host: https://consumer-control.plane/api/management
X-Api-Key: password
Content-Type: application/json
```
```json
{
  "@context": {
    "tx": "https://w3id.org/tractusx/v0.0.1/ns/",
    "edc": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "tx:ConnectorParamsDiscoveryRequest",
  "tx:bpnl": "BPNL1234567890",
  "edc:counterPartyAddress": "https://provider.domain.com/api/v1/dsp"
}
```

If the counterparty connector supports DSP version 2025-1, a valid response should be:
```json
[
  {
    "@context": {
      "edc": "https://w3id.org/edc/v0.0.1/ns/"
    },
    "edc:counterPartyId": "did:web:one-example.com",
    "edc:counterPartyAddress": "https://provider.domain.com/api/v1/dsp/2025-1",
    "edc:protocol": "dataspace-protocol-http:2025-1"
  }
]
```

Notice the automatic resolution of the `counterPartyId` from a BPN to a DID, and the appendment of the
correct DSP version path to the counterPartyAddress and to the required protocol.

The information contained in the above discovery response can be directly used in the data transfer request chain, 
as demonstrated in the following example.

## Catalog request

As mentioned in the beginning of this document, a catalog request is the first a data consumer should execute
during the data transfer sequence. A catalog request is done the following way:


```http request
POST /v3/catalog/request HTTP/1.1
Host: https://consumer-control.plane/api/management
X-Api-Key: password
Content-Type: application/json
```
```json
{
  "@context": [
    "https://w3id.org/catenax/2025/9/policy/context.jsonld",
    {
      "@vocab":"https://w3id.org/edc/v0.0.1/ns/"
    }
  ],
  "@type": "CatalogRequest",
  "counterPartyId": "did:web:one-example.com",
  "counterPartyAddress": "https://provider.domain.com/api/v1/dsp/2025-1",
  "protocol": "dataspace-protocol-http:2025-1",
  "querySpec": {
    "@type": "QuerySpec",
    "offset": 0,
    "limit": 50,
    "sortField": "http://purl.org/dc/terms/type",
    "sortOrder": "ASC",
    "filterExpression": [
      {
        "operandLeft": "https://w3id.org/edc/v0.0.1/ns/someProperty",
        "operator": "=",
        "operandRight": "value"
      }
    ]
  }
}
```
The request body is lean. Mandatory properties are:
- `counterPartyAddress` (formerly `providerUrl`): This property points to the DSP-endpoint of the Data Provider's Control
Plane. Note, that this parameter differs dependent on the version of DSP used and potentially differs if the counter party 
uses another implementation of a Connector.
- `counterPartyId`: Must be the counterParty BPN when DSP 0.8 is used. Must be the counterParty DID when DSP 2025-1 is used.
- `protocol`: must be a supported protocol by the provider. Usually `"dataspace-protocol-http"` for providers that support
DSP 0.8 or `"dataspace-protocol-http:2025-1"` for providers that support DSP 2025-1.

The `querySpec` section is optional and allows the Data Consumer to specify what entries from the catalog shall be returned.
Apart from the demonstrated query spec fields such as `offset`, `limit`, `sortField` and `sortOrder`, a `querySpec` also allows the
definition of `filterExpressions`. A filter expression is a list of 0 to many `Criterion`, that will be logically evaluated
as `AND`. Please refer to the [Contract Definitions Asset Selector](03_contractdefinitions.md#assetsselector) section where
the creation of Criterion was already explained.

## What happens in the background

In this walkthrough's sequence of API-calls, this is the first that triggers interaction between two Connectors.
Partners in the Dataspace are authenticated via Verifiable Credentials (VC).
These can broadly be conceptualized as another JSON-LD document that holds information on a business partner's identity,
and the information in this document might be used to cross-check certain conditions.

When the Consumer makes a catalog-request to the Provider, the provider collects the Consumer's VC and checks it against
each of the `accessPolicies` defined in his [Contract Definitions](03_contractdefinitions.md). If the VC passes the
`accessPolicy`, the Contract Definition is transformed to a Data Offer and added to the catalog. If the content of the VC
does not fulfil the `accessPolicy`, the Contract Definition is invisible for the requesting Data Consumer - rendering
any further communication between the Business Partners useless.

## Returned Payload

The returned payload is a `dcat:Catalog` as specified by the DSP version used in the request.

```json
{
  "@id": "acd67c9c-a5c6-4c59-9474-fcd3f948eab8",
  "@type": "Catalog",
  "participantId": "did:web:one-example.com",
  "dataset": {
    "@id": "{{ASSET_ID}}",
    "@type": "Dataset",
    "hasPolicy": {
      "@id": "MQ==:MQ==:M2ZmZDRhY2MtMzkyNy00NGI4LWJlZDItNDcwY2RiZGRjN2Ex",
      "@type": "odrl:Offer",
      "permission": {
        "action": "use",
        "constraint": {
          "leftOperand": "FrameworkAgreement",
          "operand": "eq",
          "rightOperand": "DataExchangeGovernance:1.0"
        },
        "prohibition": [],
        "obligation": []
      }
    },
    "distribution": [
      {
        "@type": "dcat:Distribution",
        "format": {
          "@id": "AzureStorage-PUSH"
        },
        "accessService": {
          "@id": "1338f9ac-1728-4a7e-b3dc-31fe5bc109f6",
          "@type": "DataService",
          "terms": "connector",
          "endpointUrl": "https://provider.domain.com/api/v1/dsp/2025-1"
        }
      },
      {
        "@type": "dcat:Distribution",
        "format": {
          "@id": "HttpData-PULL"
        },
        "accessService": {
          "@id": "1338f9ac-1728-4a7e-b3dc-31fe5bc109f6",
          "@type": "DataService",
          "terms": "connector",
          "endpointUrl": "https://provider.domain.com/api/v1/dsp/2025-1"
        }
      },
      {
        "@type": "Distribution",
        "format": {
          "@id": "AmazonS3-PUSH"
        },
        "accessService": {
          "@id": "1338f9ac-1728-4a7e-b3dc-31fe5bc109f6",
          "@type": "dcat:DataService",
          "terms": "connector",
          "endpointUrl": "https://provider.domain.com/api/v1/dsp/2025-1"
        }
      }
    ],
    "description": "Product Connector Demo Asset 1",
    "id": "1"
  },
  "service": {
    "@id": "1338f9ac-1728-4a7e-b3dc-31fe5bc109f6",
    "@type": "dcat:DataService",
    "terms": "connector",
    "endpointUrl": "https://provider.domain.com/api/v1/dsp/2025-1"
  },
  "@context": [
    "https://w3id.org/tractusx/auth/v1.0.0",
    "https://w3id.org/catenax/2025/9/policy/context.jsonld",
    "https://w3id.org/catenax/2025/9/policy/odrl.jsonld",
    "https://w3id.org/dspace/2025/1/context.jsonld",
    "https://w3id.org/edc/dspace/v0.0.1"
  ]
}
```
In the payload above, some properties are meta-data that's independent of whether the Provider extends any Data Offers
to the Consumer.

- The `@id` is the identifier for this catalog. As the catalog is created dynamically, the id is a UUID regenerated for each
  request to the Provider's catalog.
- `service` holds data about the Provider's connector that the Consumer's connector communicated with.
- `participantId` signifies the identifier of the Provider. This is specific to the Connector and not mandated by the DSP-spec.
- `@context` is part of every JSON-LD document.

The Data Offers are hidden in the `dataset` section, grouped by the [Asset](01_assets.md) that the offer is made for.
Consequently, if there may be more than one offer for the same Asset, requiring a Data Consumer to select based on the
policies included.

- The `@id` corresponds to the id of the Asset that can be negotiated for.
- `Distribution` makes statements over which Data Planes an Asset's data can be retrieved. Currently, the TractusX-EDC supports
  `HttpData-PULL`, `HttpData-PUSH`, `AmazonS3-PUSH` and `AzureStorage-PUSH` capabilities.
- `hasPolicy` holds the Data Offer that is relevant for the Consumer.
    - `@id` is the identifier for the Data Offer. The Connector composes this id by concatenating three identifiers in base64-encoding.
      separated with `:` (colons). The format is `base64(contractDefinitionId):base64(assetId):base64(newUuidV4)`. The last
      of three UUIDs changes with every request as every /v3/catalog/request call yields a new catalog with new Data Offers.
    - The `permission`, `prohibition` and `obligation` will hold the content of the contractPolicy configured
      in the [Contract Definition](03_contractdefinitions.md) the Contract Offer was derived from.


## Reference
- [Connector Discovery API](https://eclipse-tractusx.github.io/tractusx-edc/openapi/control-plane-api/#/Connector%20Discovery/discoverDspVersionParamsV4Alpha)

## Notice

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- SPDX-License-Identifier: CC-BY-4.0
- SPDX-FileCopyrightText: 2023 Contributors of the Eclipse Foundation
- Source URL: [https://github.com/eclipse-tractusx/tractusx-edc](https://github.com/eclipse-tractusx/tractusx-edc)
