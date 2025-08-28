# Migration from 0.3.4 to 0.4.0

## Switching to DSP

The Eclipse Dataspace Connector protocol recently moved its protocol implementation from IDS to DSP as of
version `0.0.1-milestone-9`.
From the Tractus-X EDC perspective this causes breaking changes in the following areas:

- the Management API: because DSP uses JSON-LD, all Management API endpoints had to be adapted as well to reflect that.
  The old Management API is now deprecated and is **not** tested for compliance. Please upgrade using the `/v2/` path
  for every endpoint, e.g. `<PATH>/management/v2/assets`. Please also refer to
  the [EDC OpenAPI spec](https://app.swaggerhub.com/apis/eclipse-edc-bot/management-api/0.0.1-SNAPSHOT#/).
  An updated postman collection with the `v2` flow is available [here](../development/postman/collection.json)

- modules: all `*ids*` modules are deprecated and cannot be used anymore. Please migrate over
  to `org.eclipse.edc:dsp:0.0.1-milestone-9`.

- path: the default protocol path is now `/api/v1/dsp` instead of `/api/v1/ids`

- `edc.participant.id`: new mandatory configuration for the participant id in the dataspace (BPN number).
  It's configured via mandatory property in the charts with object ```yaml participant: id: "id"```.

**Please note that this is not a complete documentation of the DSP Protocol, please refer to
the [official documentation](https://docs.internationaldataspaces.org/dataspace-protocol/overview/readme)**

## Removal of the Business Tests

The business tests were removed from the code base, because all the ever tested is already by other tests, specifically
the JUnit-based tests, deployment tests, or other tests that are already done upstream in EDC.

The Business tests were brittle, consumed a lot of resources and were quite cumbersome to run and debug locally.

## New implementation for the Control Plane Adapter

Since the old Control-Plane-Adapter is incompatible with DSP, a new iteration was created.
**Due to time constraints with this release documentation for this feature will to be published subsequently**

## New Policies for expressing validity of the agreement

The dates in `ContractOffer` and `ContractAgreement` has been removed in favour of a policy based contract validity check, see [here](https://github.com/eclipse-edc/Connector/issues/2758)

## Other changes

- When using the EDR [HttpDynamicReceiverExtension](https://github.com/eclipse-edc/Connector/tree/main/extensions/control-plane/transfer/transfer-pull-http-dynamic-receiver) in the transfer process initiation
  the properties for configuring the receiver on single transfer process has been changed to:
  
  ```json
  "privateProperties": {
        "receiverHttpEndpoint": "{{BACKEND_SERVICE}}"
  }
  ```

  instead of:

  ```json
  "properties": {
        "receiver.http.endpoint": "{{BACKEND_SERVICE}}"
  }
  ```

## New Catalog

The DSP catalog is expressed as [DCat Catalog](https://www.w3.org/TR/vocab-dcat-3/), when querying the catalog a response like this will return:

```json
{
    "@id": "5a3207ae-bd0d-4a3b-bc8a-05adfbe75d95",
    "@type": "dcat:Catalog",
    "dcat:dataset": {
        "@id": "e6279569-17a9-4ba3-9401-a8ae4100e4eb",
        "@type": "dcat:Dataset",
        "odrl:hasPolicy": {
            "@id": "2:1:535def6e-8321-4c0e-a595-aabdd9c18eed",
            "@type": "odrl:Set",
            "odrl:permission": [],
            "odrl:prohibition": [],
            "odrl:obligation": [],
            "odrl:target": "1"
        },
        "dcat:distribution": [
         ...
        ],
        "edc:description": "Product EDC Demo Asset",
        "edc:id": "1"
    },
    "dcat:service": {
      ...
    },
    "edc:participantId": "participantId",
    "@context": {
    }
} 
```

When starting a new contract negotiation for an asset:

- the `@id` of `odrl:hasPolicy` object should be passed in the `offerId` field
- the `edc:participantId` should be passed in the `providerId` and `connectorId` fields. `connectorId` it's still needed for backward compatibility and it will probably be removed in the next versions.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2021,2022,2023 Contributors to the Eclipse Foundation
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
