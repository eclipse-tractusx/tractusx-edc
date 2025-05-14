# Expose all Business Partner Groups

## Decision

TractusX-EDC will expose an endpoint that provides all the BPN groups that have a BPN assigned to them.

## Rationale

Currently, listing all available BPN Groups requires [retrieving all BPNs](https://eclipse-tractusx.github.io/tractusx-edc/openapi/control-plane-api/0.9.0/#/Business%20Partner%20Group/resolveV3) in the `BusinessPartnerGroup` api and iterating through their assigned groups. This approach is consuming for the user and providing an endpoint to return all BPN groups would improve usability. Using the existing BPN Group API keeps consistency.

## Approach

1. In `BusinessPartnerGroupApiV3` create a new GET endpoint that returns all groups, appending `/groups`.
2. Create a query targeting the `edc_business_partner_group` table to evaluate and extract all BPN groups.
3. Using the `BusinessPartnerStore` include a new operation that returns all the BPN groups and the change is reflected in the two existing implementations (In Memory and SQL).
4. Response of the new endpoint is a list containing all the BPN groups.

