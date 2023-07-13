# Control Plane EDR API

This module provides extensions to the EDC management API for dealing with EDR tokens.

The extensions are added to the same context as the management APIs, so no additional configuration is required.

The base path of the API will be `<mgmtContext>/edrs`

This module for now provides three APIs:

- Initiating an EDR negotiation token
- Fetching the available EDRs
- Fetching the single EDR

The initiate negotiation EDR leverage the callbacks mechanism introduced in the latest EDC, and it handles
the contract negotiation and the transfer request in one API call. Once the transfer has been completed
the provider will return the EDR that will be stored into the consumer EDR store/cache. Users can interact
with the EDR store/cache for fetching the EDR and then requesting the data, or can use the `proxy` API described [here](../../dataplane-proxy/edc-dataplane-proxy-consumer-api/README.md)

An overview on how to use the EDR APIs is available [here](../../../docs/samples/edr-api-overview/edr-api-overview.md)
