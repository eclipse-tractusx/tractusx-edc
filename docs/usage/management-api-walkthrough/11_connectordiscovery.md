# Discovering DSP parameters

As explained in the Dataspace Protocol document 
_"Connectors implementing the Dataspace Protocol may operate on different versions and bindings. 
Therefore, it is necessary that they can discover such information reliably and unambiguously"._
The 2025-1 specification dictates that each connector should expose a commonly identifiable and publicly available version 
metadata endpoint location (at `/.well-known/dspace-version`)
which dataspace participants should use to discover which versions of the protocol are supported by a Connector.

To ease the discovery of available and supported DSP versions of a Connector, the tractusx-edc project makes available
and API endpoint that proxies the request to metadata endpoint and returns the corresponding parameters for
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
  "edc:counterPartyAddress": "https://provider.domain.com/api/dsp"
}
```

If the counterpart connector supports DSP version 2025-1, a valid response should be:
```json
[
  {
    "@context": {
      "edc": "https://w3id.org/edc/v0.0.1/ns/"
    },
    "edc:counterPartyId": "did:web:one-example.com",
    "edc:counterPartyAddress": "https://provider.domain.com/api/dsp/2025-1",
    "edc:protocol": "dataspace-protocol-http:2025-1"
  }
]
```
Notice the automatic resolution of the `counterPartyId` to DID, and the appendment of the 
correct DSP version path to the counterPartyAddress and to the required protocol.

If the counterpart does not support DSP version 2025-1, then the correct parameters for a previous version will be returned.

The expectation for a client of this API is to directly use the information provided in the above response in the request
chain initiated by a catalog request, as described in the [catalog walkthrough](04_catalog.md).

## Reference
- [Connector Discovery API](https://eclipse-tractusx.github.io/tractusx-edc/openapi/control-plane-api/0.11.0/#/Connector%20Discovery/discoverDspVersionParamsV4Alpha)

## Notice

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- SPDX-License-Identifier: CC-BY-4.0
- SPDX-FileCopyrightText: 2023 Contributors of the Eclipse Foundation
- Source URL: [https://github.com/eclipse-tractusx/tractusx-edc](https://github.com/eclipse-tractusx/tractusx-edc)