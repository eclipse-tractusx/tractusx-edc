# Endpoint Data Reference API Overview

## Introduction

The **Endpoint Data Reference** serves the purpose of streamlining the interactions between consumer connector operators. Through its API, acts as a facade for two fundamental processes involved in Asset consumption: **contract negotiation** and **transfer process** initialization. Additionally, it enables the management of **EDR**s (Endpoint Data References).

## Configuration

The new API is seamlessly deployed under the well-known management API context, requiring no additional configuration. However, this is not the case for the EDR Cache storage. The EDR Cache provides two implementations: `InMemory` and `SQL`. The `edc-runtime-memory` bundle utilizes the `InMemory` implementation for the EDR Cache without any extra setup. On the other hand, the SQL Cache, found in the `edc-controlplane-postgresql-*` bundles, utilizes the SQL implementation and requires database access for schema migration and data management.

| property                                                  | description                                          | required  | default value |
|-----------------------------------------------------------|------------------------------------------------------|-----------|---------------|
| edc.datasource.edr.name                                   | Defines the name associated with the EDR data source | false     | edr           |
| edc.datasource.edr.url                                    | Defines the database address to access the EDR data  | true      |               |
| edc.datasource.edr.user                                   | Defines EDR database username                        | true      |               |
| edc.datasource.edr.password                               | Defines EDR database password                        | true      |               |
| org.eclipse.tractusx.edc.postgresql.migration.edr.enabled | Enables EDR database migration	                      | true      |               |

## Features

### EDR Negotiation | Contract Negotiation and Transfer Process in a single request

- This endpoint will perform the contract negotiation, transfer process and EDR storage respectively.

> Please note that the `data destination` will always be `HttpProxy`, requiring a request against the provider's `data-plane` to fetch the asset data.

| Path                            | Method | Query Params             |
|---------------------------------|--------|--------------------------|
| `<MANAGEMENT_URL>/edrs`         | POST   | none                     |

#### Payload

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
    "odrl": "http://www.w3.org/ns/odrl/2/"
  },
  "@type": "NegotiationInitiateRequestDto",
  "connectorAddress": "{{ _['-provider-dsp-url'] }}",
  "protocol": "dataspace-protocol-http",
  "connectorId": "{{ _['connector-id'] }}",
  "providerId": "{{ _['provider-id'] }}",
  "offer": {
    "offerId": "{{ _['offer-id'] }}",
    "assetId": "{{ _['asset-id'] }}",
    "policy": {
      "@type": "odrl:Set",
      "odrl:permission": {
        "odrl:target": "{{ _['asset-id'] }}",
        "odrl:action": {
          "odrl:type": "USE"
        }
      },
      "odrl:target": "{{ _['asset-id'] }}"
    }
  }
}
```

#### Response

```json
{
  "@type": "edc:IdResponseDto",
  "@id": "contract-negotiation-id",
  "edc:createdAt": 1687405819736,
  "@context": {
    "dct": "https://purl.org/dc/terms/",
    "tx": "https://w3id.org/tractusx/v0.0.1/ns/",
    "edc": "https://w3id.org/edc/v0.0.1/ns/",
    "dcat": "https://www.w3.org/ns/dcat/",
    "odrl": "http://www.w3.org/ns/odrl/2/",
    "dspace": "https://w3id.org/dspace/v0.8/"
  }
}
```

### EDR Management | Fetch cached EDRs

- This endpoint will retrieve all EDR entries by their `assetId` or `agreementId` references, which are passed as `query parameters`.

| Path                                         | Method | Query Params         |
|----------------------------------------------|--------|----------------------|
| `<MANAGEMENT_URL>/edrs`                      | GET    | assetId, agreementId |

#### Response

```json
[
  {
    "@type": "tx:EndpointDataReferenceEntry",
    "edc:agreementId": "contract-agreement-id",
    "edc:transferProcessId": "transfer-process-id",
    "edc:assetId": "asset-id",
    "@context": {
      "dct": "https://purl.org/dc/terms/",
      "tx": "https://w3id.org/tractusx/v0.0.1/ns/",
      "edc": "https://w3id.org/edc/v0.0.1/ns/",
      "dcat": "https://www.w3.org/ns/dcat/",
      "odrl": "http://www.w3.org/ns/odrl/2/",
      "dspace": "https://w3id.org/dspace/v0.8/"
    }
  }
]
```

- This endpoint, through the `transfer-process-id` passed as `path variable`, will retrieve the actual EDR.

| Path                                         | Method | Query Params             |
|----------------------------------------------|--------|--------------------------|
| `<MANAGEMENT_URL>/edrs/{trasfer-process-id}` | GET    | none                     |

#### Response

```json
{
  "@type": "edc:DataAddress",
  "edc:cid": "cid",
  "edc:type": "EDR",
  "edc:authCode": "authcode",
  "edc:endpoint": "http://provider-data-plane/public-url",
  "edc:id": "transfer-process-id",
  "edc:authKey": "Authorization",
  "@context": {
    "dct": "https://purl.org/dc/terms/",
    "tx": "https://w3id.org/tractusx/v0.0.1/ns/",
    "edc": "https://w3id.org/edc/v0.0.1/ns/",
    "dcat": "https://www.w3.org/ns/dcat/",
    "odrl": "http://www.w3.org/ns/odrl/2/",
    "dspace": "https://w3id.org/dspace/v0.8/"
  }
}
```

> Please note that now with the EDR you are able to request the `Asset` data from provider's `data-plane`.
