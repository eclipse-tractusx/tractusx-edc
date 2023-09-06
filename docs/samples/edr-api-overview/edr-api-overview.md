# Endpoint Data Reference API Overview

## Introduction

The **Endpoint Data Reference** serves the purpose of streamlining the consumer connector operators' interactions. Through its API, acts as a facade for two fundamental processes involved in Asset consumption: **contract negotiation** and **transfer process** initialization and additionally, it enables the management of **EDR**s (Endpoint Data References).

## Configuration

The new API is seamlessly deployed under the well-known management API context, requiring no additional configuration. However, this is not the case for the EDR Cache storage. The EDR Cache provides two implementations: `InMemory` and `SQL`. The `edc-runtime-memory` bundle utilizes the `InMemory` implementation for the EDR Cache without any extra setup. On the other hand, the SQL Cache, found in the `edc-controlplane-postgresql-*` bundles, utilizes the SQL implementation and requires database access for schema migration and data management.

| property                                                  | description                                          | required | default value |
|-----------------------------------------------------------|------------------------------------------------------|----------|---------------|
| edc.datasource.edr.name                                   | Defines the name associated with the EDR data source | false    | edr           |
| edc.datasource.edr.url                                    | Defines the database address to access the EDR data  | true     |               |
| edc.datasource.edr.user                                   | Defines EDR database username                        | true     |               |
| edc.datasource.edr.password                               | Defines EDR database password                        | true     |               |

## Features

### EDR Negotiation | Contract Negotiation and Transfer Process in a single request

This endpoint will perform the contract negotiation, transfer process and EDR storage respectively.

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
  "connectorAddress": "<PROVIDER_DSP_URL>",
  "protocol": "dataspace-protocol-http",
  "connectorId": "<CONNECTOR_ID>",
  "providerId": "<PROVIDER_ID>",
  "offer": {
    "offerId": "<OFFER_ID>",
    "assetId": "<ASSET_ID>",
    "policy": {
      "@type": "odrl:Set",
      "odrl:permission": {
        "odrl:target": "<ASSET_ID>",
        "odrl:action": {
          "odrl:type": "USE"
        }
      },
      "odrl:target": "<ASSET_ID>"
    }
  }
}
```

#### EDR Negotiation Response

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

The EDR negotiation returns only the id of the negotiation process that has been started.

The EDR negotiation rely on two steps, contract negotiation and transfer process, both of which are asynchronous.
In order to plug-in and get notified in every state of the two state machines, callbacks can be configured while starting
the EDR negotiation:

```json
{
  ...
  "callbackAddresses": [
    {
      "uri": "http://localhost:8080/hooks",
      "events": [
        "transfer.process.started"
      ],
      "transactional": false
    }
  ]
}
```

In this case we are interested only when the transfer process transition to the `STARTED` state.

Once the EDR has been negotiated with the provider, the EDR itself will be stored in the configured vault and the metadata
associated to it in the configured datasource for future querying.

Since `tractusx-edc` v0.5.1 the cached EDRs also come with a state machine that will manage the lifecycle of an EDR
on the consumer side. That means that it will auto-renew itself when the expiration date is approaching by
firing another transfer process request with the same parameters as the original one. Once renewed the old-one
will transition to the state `EXPIRED` and it will be removed from the database and the vault according to the [configuration](../../../core/edr-core/README.md).

### EDR Management | Fetch cached EDRs

This endpoint will retrieve all EDR entries by their `assetId` or `agreementId` references, which are passed as `query parameters`.

| Path                                         | Method | Query Params         |
|----------------------------------------------|--------|----------------------|
| `<MANAGEMENT_URL>/edrs`                      | GET    | assetId, agreementId |

#### EDR Entry Response

```json
[
  {
    "@type": "tx:EndpointDataReferenceEntry",
    "edc:agreementId": "<agreement-id>",
    "edc:transferProcessId": "<transfer-process-od>",
    "edc:assetId": "<asset-id>",
    "edc:providerId": "<provider-id>",
    "tx:edrState": "NEGOTIATED",
    "tx:expirationDate": 1693928132000,
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

### EDR Management | Fetch single EDR

This endpoint, through the `transfer-process-id` passed as `path variable`, will retrieve the actual EDR.

| Path                                          | Method | Query Params             |
|-----------------------------------------------|--------|--------------------------|
| `<MANAGEMENT_URL>/edrs/{transfer-process-id}` | GET    | none                     |

#### EDR Response

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

### EDR Management | Deleting a cached EDR

This endpoint will delete the EDR entry associated with the `transfer-process-id` and it will remove the EDR itself
from the vault.

| Path                                          | Method | Query Params             |
|-----------------------------------------------|--------|--------------------------|
| `<MANAGEMENT_URL>/edrs/{transfer-process-id}` | DELETE | none                     |

### EDR Usage | Fetching data

Once the EDR has been negotiated and stored, the data can be fetched in two ways depending on the use-case:

- Provider data-plane (EDC way)
- Consumer proxy (Tractus-X EDC simplified)

#### Provider data-plane

Once the right EDR has been identified using the EDR management API the current asset/agreement/transfer-process that
you want to transfer, we can use the `endpont`, `authCode` and `authKey` to make the request:

```sh
curl --request GET \
  --url http://provider-data-plane/public-url \
  --header 'Authorization: auth-code' \
  --header 'Content-Type: application/json' 
```

If the HTTP asset has been configured to proxy also query parameters and path segments they will be forwarded
to the backend from the provider data-plane:

```sh
curl --request GET \
  --url http://provider-data-plane/public-url/subroute?foo=bar \
  --header 'Authorization: auth-code' \
  --header 'Content-Type: application/json' 
```

#### Consumer data-plane (proxy)

The Consumer data-plane proxy is an extension available in `tractusx-edc` that will use the EDR store to simplify
the data request on consumer side. The documentation is available [here](../../../edc-extensions/dataplane-proxy/edc-dataplane-proxy-consumer-api/README.md).

The only API is:

| Path                      | Method | Query Params             |
|---------------------------|--------|--------------------------|
| `<PROXY_URL>/aas/request` | POST   | none                     |

which fetches the data according to the input body. The body should contain the `assetId` plus `providerId` or the `transferProcessId`,
which identifies the EDR to use for fetching data and an `endpointUrl` which is the [provider gateway](../../../edc-extensions/dataplane-proxy/edc-dataplane-proxy-provider-api/README.md)
on which the data is available.

Example:

```sh
curl --request POST \
  --url http://localhost:8186/proxy/aas/request \
  --header 'Content-Type: application/json' \
  --header 'X-Api-Key: password' \
  --data '{"assetId": "1","providerId": "BPNL000000000001","endpointUrl": "http://localhost:8080/api/gateway/aas/test"}'
```

Alternatively if the `endpointUrl` is not known or the gateway on the provider side is not configured, it can be omitted and the `Edr#endpointUrl`
will be used as base url. In this scenario if needed, users can provide additional properties to the request for composing the final
url:

- `pathSegments` sub path to append to the base url
- `queryParams` query parameters to add to the url

Example of an asset with base url `http://localhost:8080/test`

```sh
curl --request POST \
  --url http://localhost:8186/proxy/aas/request \
  --header 'Content-Type: application/json' \
  --header 'X-Api-Key: password' \
  --data '{"assetId": "1","providerId": "BPNL000000000001","pathSegments": "/sub","queryParams": "foo=bar"}'
```

The final url will look like `http://localhost:8080/test/sub?foo=bar`.
