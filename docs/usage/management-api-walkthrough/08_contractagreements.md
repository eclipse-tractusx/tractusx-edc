# Checking existing Contract Agreements

The Management API has a provider-internal endpoint to retrieve existing Contract Agreements. It also exposes a `/request`
endpoint (to be used with the previously explained `QuerySpec` object) but allows retrieval of single agreements by id
like this:

```http request
GET /v3/contractagreements/{{AGREEMENT_ID}} HTTP/1.1
Host: https://consumer-control.plane/api/management
X-Api-Key: password
Content-Type: application/json
```

A Contract Agreement looks like this:

```json
{
  "@type": "ContractAgreement",
  "@id": "{{AGREEMENT_ID}}",
  "assetId": "{{ASSET_ID}}",
  "policy": {
    "@id": "{{POLICY_ID}}",
    "@type": "odrl:Agreement",
    "odrl:permission": {
      "odrl:action": {
        "odrl:type": "http://www.w3.org/ns/odrl/2/use"
      },
      "odrl:constraint": {
        "odrl:or": {
          "odrl:leftOperand": "https://w3id.org/catenax/policy/FrameworkAgreement",
          "odrl:operator": {
            "@id": "odrl:eq"
          },
          "odrl:rightOperand": "Pcf"
        }
      }
    },
    "odrl:prohibition": [],
    "odrl:obligation": [],
    "odrl:assignee": "<BPN_CONSUMER>",
    "odrl:assigner": "<BPN_PROVIDER>",
    "odrl:target": {
      "@id": "{{ASSET_ID}}"
    }
  },
  "contractSigningDate": 1713441910,
  "consumerId": "{{BPN_CONSUMER}}",
  "providerId": "{{BPN_PROVIDER}}",
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
    "edc": "https://w3id.org/edc/v0.0.1/ns/",
    "tx": "https://w3id.org/tractusx/v0.0.1/ns/",
    "tx-auth": "https://w3id.org/tractusx/auth/",
    "cx-policy": "https://w3id.org/catenax/policy/",
    "odrl": "http://www.w3.org/ns/odrl/2/"
  }
}
```

Most of this data should already be known to the Data Provider from the negotiation and transfer processes but can be
retrieved at a glance via this API.

## Notice

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- SPDX-License-Identifier: CC-BY-4.0
- SPDX-FileCopyrightText: 2023 Contributors of the Eclipse Foundation
- Source URL: [https://github.com/eclipse-tractusx/tractusx-edc](https://github.com/eclipse-tractusx/tractusx-edc)
