# Expose BPNs in a Group given a Group identifier

## Decision

TractusX-EDC will expose the BPNs  in a BPN Group provided a Group identifier. For this a new endpoint will be created in the `BusinessPartnerGroupApi`.

## Rationale

Currently, to obtain the list of BPN groups belonging to a certain BPN, there is a need to perform the [request](https://eclipse-tractusx.github.io/tractusx-edc/openapi/control-plane-api/0.9.0/#/Business%20Partner%20Group/resolveV3) ```GET /business-partner-groups/{bpn}``` in the `BusinessPartnerGroup` api which resolves all groups of a specific BPN. However, the contrary is not possible, meaning that to obtain all the BPNs in a BPN Group it is required to make the mentioned request to all BPN's in a space and then iterate over to see the associated groups.

This process is very heavy to compute, specially on the client side since it needs to always be searching for new BPNs and check if existing BPNs were added or removed to a BPN Group.

So having a new endpoint that returns which BPNs are present in a BPN Group allows a more straightforward approach. Using the existing BPN Group API keeps consistency.


## Approach

- In `BusinessPartnerGroupApiV3` create a new GET endpoint able to resolve all BPNs for a particular BPN group given this group name. The group name should be a path param (appending `/group/{group}`).
- Response of the new endpoint should follow the format of the other read endpoint, updated to reflect the groups instead, similar to the next.
```
{
	"@id": "group-name",
	"tx:bpns": [
		"BPNL000000000001",
		"BPNL000000000002"
	],
	"@context": {...}
}
```
- Using the `BusinessPartnerStore` include a new operation that resolves the BPNs for a BPN Group and the change is reflected in the two existing implementations (In Memory and SQL).
- Create a SQL migration to allow faster querying. Create a table for the BPN groups (where the group name is unique) and an intermediate table relating the BPNs listed in the `edc_business_partner_group` table and the new table. After this, [the column groups](https://github.com/eclipse-tractusx/tractusx-edc/blob/0.9.0/edc-extensions/migrations/control-plane-migration/src/main/resources/org/eclipse/tractusx/edc/postgresql/migration/bpn/V0_0_1__Init_BusinessGroup_Schema.sql#L20) in the table `edc_business_partner_group` can be dropped.