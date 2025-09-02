# Migration Guide `0.10.x -> 0.11.x`

This document outlines the necessary changes for migrating your tractusx-edc installation from versions 0.10.x to 0.11.0.
It also outlines some points that adopters and operators should pay close attention to when migrating from one version
to another.

This document is not a comprehensive feature list.

<!-- TOC -->
- [Migration Guide `0.10.x -> 0.11.x`](#migration-guide-010x---011x)
  - [1. DCP version 1.0](#1-dcp-version-10)
  - [2. Participant ID](#2-participant-id)
  - [3. Protocol handling for different versions](#3-protocol-handling-for-different-versions)
<!-- TOC -->

## 1. DCP version 1.0

Tractus-X EDC now uses DCP 1.0 by default — no switch is required. If DCP 0.8.1 was previously forced via `edc.dcp.v08.forced`,
that setting should be removed; it is deprecated and no flag is provided for 1.0.

## 2. Participant ID

Previously, the BPN was used as the EDC's participant identifier. Starting with `0.11.0`, the primary participant
identifier is now the DID. As the BPN is still required for some processes and also will be used for communication
with older Tractus-X EDC versions, it now needs to be supplied via a new setting. Therefore, the configuration should
be updated as follows:

```properties
edc.participant.id=<did>
tractusx.edc.participant.bpn=<bpn>
```

## 3. Protocol handling for different versions

The `0.11.0` Tractus-X EDC supports both the previously supported 0.8 version of DSP as well as the new 2025-1 version
of DSP. It is in the responsibility of the caller of a management api call to identify the version to be used. The relevant
properties are `counterPartyAddress`, as the EDC supports different versions as own subtrees in the rest api hierarchy,
`protocol` and `counterPartyId`, resp. `policy/assigner`. For a standard runtime, the parameters have to be set:

Properties to be used for version 0.8:

```properties
counterPartyAddress: https://<provider-connector-url>/api/v1/dsp
protocol: dataspace-protocol-http
counterPartyId, resp. policy/assigner: <provider-bpn>
```

For version 2025-1:

```properties
counterPartyAddress: https://<provider-connector-url>/api/v1/dsp/2025-1
protocol: dataspace-protocol-http:2025-1
counterPartyId, resp. policy/assigner: <provider-did>
```

The property values to be used for a specific connector can be retrieved from the new management endpoint
`/v4alpha/connectordiscovery`. It takes as request parameter a payload like:

```json
{
  "@context": {
    "tx": "https://w3id.org/tractusx/v0.0.1/ns/",
    "edc": "https://w3id.org/edc/v0.0.1/ns/",
  },
  "tx:bpnl": "BPNL1234567890",
  "edc:counterPartyAddress": "https://provider.domain.com/api/v1/dsp"
}  
```

As response, it returns all the right version parameter for that connector, depending on whether it supports
DSP 2025-1 or only the old protocol 0.8. For the above example, it would return for version 0.8:

```json
{
  "https://w3id.org/edc/v0.0.1/ns/counterPartyAddress": "https://provider.domain.com/api/v1/dsp/",
  "https://w3id.org/edc/v0.0.1/ns/counterPartyId": "BPNL1234567890",
  "https://w3id.org/edc/v0.0.1/ns/protocol": "dataspace-protocol-http"
}
```

and for version 2025-1:

```json
{
  "https://w3id.org/edc/v0.0.1/ns/counterPartyAddress": "https://provider.domain.com/api/v1/dsp/2025-1",
  "https://w3id.org/edc/v0.0.1/ns/counterPartyId": "<provider-did>",
  "https://w3id.org/edc/v0.0.1/ns/protocol": "dataspace-protocol-http:2025-1"
}
```

## 4. Policy validations according to standard CX-0152

Standard CX-0152 specifies all the allowed actions and respective constraints to be used within the CatenaX dataspace.
To ensure compliance with the newly added standard, Tractus-X EDC has introduced a validation mechanism that validate
newly created policies against the standard. Policies are now validated in their entirety including action types, 
rule types, constraint logic, left and right operand values, as well as operators.

It is highly probable that existing interfaces with the connector that managed policy definitions will be affected by 
this change. We recommend reviewing and updating all policies to ensure they comply with the standard.

We've taken care to ensure that existing policies created before the upgrade are not affected by this validation. This 
means that all existing policies will remain functional and valid after the upgrade. However, any new policies created
after the upgrade will need to adhere to the CX-0152 standard.

For more information on the CX-0152 standard, please refer to the official documentation. The following link
might break since it points to a branch other than main. Nevertheless the repository should contain the required 
information.
- [Catena-X Dataspace Policy Standard (CX-0152)](https://github.com/catenax-eV/product-standardization-prod/blob/R25.09-release-bundle/standards/CX-0152-PolicyConstrainsForDataExchange/CX-0152-PolicyConstraintsForDataExchange.md)
