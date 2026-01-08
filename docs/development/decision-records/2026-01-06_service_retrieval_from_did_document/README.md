# Connector endpoint retrieval from DID Document service section

## Decision

We will implement an additional endpoint in the connector discovery endpoint family. The endpoint will, based on a
DID retrieves the DID document, parses the service section for `DataService` entries, and retrieves for the detected
connector endpoints the dsp version parameters. It returns a list of version parameter sets for all found connectors
in the DID document. There will be a general support for other identifiers, using BPNLs as a second supported
identifier type.

## Rationale

The current Tractus-X connector supports multiple versions of the DSP protocol the ones supported published
in the `.well-known/dspace-version`
[endpoint](https://eclipse-dataspace-protocol-base.github.io/DataspaceProtocol/2025-1/#exposure-of-dataspace-protocol-versions).
In addition, the DSP spec suggests to use the
[DID document](https://eclipse-dataspace-protocol-base.github.io/DataspaceProtocol/2025-1/#discovery-of-service-endpoints)
to publish connector endpoints. With this feature, the Tractus-X connector is about to support this retrieval of
connector endpoints together with the detection of the right version parameters used in the management api to
initiate DSP calls in the right version.

As for multi version connectors, only the option to use a `DataService` reference in the DID document makes sense,
the discovery of endpoints is limited to this type of connector references.

## Approach

There is already a 
[connector discovery extension](https://github.com/eclipse-tractusx/tractusx-edc/blob/eaa7084e83912e6dae42c13c948607a68d85ffa7/edc-extensions/connector-discovery/connector-discovery-api) 
that implements the retrieval of the `.well-known/dspace-version` endpoint and to create the proper dsp version
parameters for a single connector endpoint provided as parameter. There is a second extension there which provides
a default implementation of the defined api.

As the intended api is related to this functionality, the approach is, to add another management api endpoint
called `/connectors` in this management api section that takes the following input parameters:

```json
{
  "identifier": "did:web:",
  "knowns": [
    "https://first.provider-domain.com/somepath/dsp/v1/api",
    "https://first.provider-domain.com/otherpath/dsp/v1/api",
    "https://second.provider-domain.com/dsp/v1/api"
  ]
}
```

The identifier field is kept neutral, in order to support different identifier types. The service will interpret
the identifier based on properties of the identifier, for now, DIDs and BPNLs will be supported. The mechanism
will be implemented in an extensible fashion, so that a general mapping from any identifier to a DID can be added.
The default implementation will detect DIDs and map them to themselves. A second extension will allow to handle
BPNLs and map them to the DID using the BDRS client.

The second input parameter `knowns` is optional and allows to add already known connector endpoints, so that also
for such them the version discovery can be executed and the right management api parameters are determined.

Consequently, the response has a body like this:

```json
[
  {
    "counterPartyAddress": "https://provider-domain/somepath/dsp/v1/api/2025-1",
    "counterPartyId": "did:web:...",
    "protocol": "dataspace-protocol-http:2025-1"
  },
  {
    "counterPartyAddress": "https://other-provider-domain/otherpath/dsp/v1/api",
    "counterPartyId": "BPNL...",
    "protocol": "dataspace-protocol-http"
  }
]
```

So it returns a list of parameter sets for the listed connectors in the DID document. It adds the known connectors
given as input and also requests the central discovery for now and provides the list of connectors from there.

The algorithm will make use of the existing features to download the DID document as well as the `dspace-version`
endpoint and processes the information. In case of a BPNL provided, it makes use of the BDRS client to translate
the BPNL to the DID.

## NOTICE

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- SPDX-License-Identifier: CC-BY-4.0
- SPDX-FileCopyrightText: 2026 Cofinity-X GmbH
- Source URL: [https://github.com/eclipse-tractusx/tractusx-edc](https://github.com/eclipse-tractusx/tractusx-edc)
