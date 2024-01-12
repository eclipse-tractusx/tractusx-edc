# Checking existing Contract Agreements

The Management API has a provider-internal endpoint to retrieve existing Contract Agreements. It also exposes a `/request`
endpoint (to be used with the previously explained `QuerySpec` object) but allows retrieval of single agreements by id
like this:

```http
GET /v2/contractagreements/{{agreementId}} HTTP/1.1
Host: https://consumer-control.plane/api/management
X-Api-Key: password
Content-Type: application/json
```

A Contract Agreement looks like this:

```json
{
  "@type": "edc:ContractAgreement",
  "@id": "<AGREEMENT_ID>",
  "edc:assetId": "<ASSET_ID>",
  "edc:policy": {
    "@id": "<POLICY_ID>",
    "@type": "odrl:Set",
    "odrl:permission": {
      "odrl:target": "<ASSET_ID>",
      "odrl:action": {
        "odrl:type": "USE"
      },
      "odrl:constraint": {
        "odrl:and": {
          "odrl:leftOperand": "BusinessPartnerNumber",
          "odrl:operator": {
            "@id": "odrl:eq"
          },
          "odrl:rightOperand": "<SOME_BPN>"
        }
      }
    },
    "odrl:prohibition": [],
    "odrl:obligation": [],
    "odrl:target": ">ASSET_ID>"
  },
  "edc:contractSigningDate": 1697720380,
  "edc:consumerId": "<BPN_CONSUMER>",
  "edc:providerId": "<BPN_PROVIDER>",
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

Most of this data should already be known to the Data Provider from the negotiation and transfer processes but can be
retrieved at a glance via this API.

## Notice

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- SPDX-License-Identifier: CC-BY-4.0
- SPDX-FileCopyrightText: 2023 Contributors of the Eclipse Foundation
- Source URL: [https://github.com/eclipse-tractusx/tractusx-edc](https://github.com/eclipse-tractusx/tractusx-edc)