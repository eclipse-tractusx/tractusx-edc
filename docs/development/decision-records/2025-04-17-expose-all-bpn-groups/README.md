# Expose all Business Partner Groups

## Decision

TractusX-EDC will expose an endpoint that will provide all the BPN groups with the BPN assigned to them.

## Rationale

Currently, listing all available BPN Groups requires [retrieving all BPNs](https://eclipse-tractusx.github.io/tractusx-edc/openapi/control-plane-api/0.9.0/#/Business%20Partner%20Group/resolveV3) in the `BusinessPartnerGroup` api and iterating through their assigned groups.
This approach is consuming for the user and providing an endpoint to return all BPN groups with respective BPN assigned would improve usability since a single request can be performed. Using the existing BPN Group API keeps consistency.

Since the response of this new endpoint includes the same information provided from the `/group/{group}` GET request and this one was not published on any release, it will be removed.

## Approach

1. In `BusinessPartnerGroupApiV3` create a new POST endpoint, appending `/request`. This returns filtered BPNs (according to a particular query sent as a QuerySpec in the body of the request) each containing the list of groups it is assigned to.

The endpoint will accept a request body with the `querySpec` containing the filtering BPN's. If no BPN is sent as filter, the response should be all BPNs and their assigned groups.

2. Create a query targeting the `edc_business_partner_group` table to evaluate and extract all BPNs and respective groups.
3. Using the `BusinessPartnerStore` include a new operation that returns the BPNs and BPN groups and the change is reflected in the two existing implementations (In Memory and SQL).
4. The response will include a list of filtered BPNs and their assigned groups and should be similar to the next block.

```json
[
  {
    "bpn": "BPNL000000000001",
    "groups": ["group1","group5","group4"]
  },
  {
    "bpn": "BPNL000000000002",
    "groups": ["group1","group5","group4"]
  },
  {
    "bpn": "BPNL000000000003",
    "groups": ["group1"]
  }
]
```
5. Delete the `/group/{group}` GET endpoint from the `BusinessPartnerGroupApiV3` interface and remove the implementation from `BusinessPartnerGroupApiControllerV3`.

