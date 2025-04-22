# Expose all Business Partner Groups

## Decision

TractusX-EDC will expose an endpoint that will provide all the BPN groups with the BPN assigned to them.

## Rationale

Currently, listing all available BPN Groups requires [retrieving all BPNs](https://eclipse-tractusx.github.io/tractusx-edc/openapi/control-plane-api/0.9.0/#/Business%20Partner%20Group/resolveV3) in the `BusinessPartnerGroup` api and iterating through their assigned groups.
This approach is consuming for the user and providing an endpoint to return all BPN groups with respective BPN assigned would improve usability since a single request can be performed. Using the existing BPN Group API keeps consistency.

## Approach

1. In `BusinessPartnerGroupApiV3` create a new POST endpoint, appending `/request`. This returns filtered BPNs (according to a particular query sent as a QuerySpec in the body of the request) each containing the list of groups it is assigned to.

The endpoint will accept a request body with the `querySpec` like the following.

```json
{
  "filterExpression": [
    {
      "operandLeft": "BusinessPartnerNumber",
      "operator": "IN",
      "operandRight": ["BPN000000001", "BPN000000002", "BPN000000003"]
    }
  ],
  "limit": 50,
  "offset": 0,
  "sortOrder": "ASC",
  "sortField": "BusinessPartnerNumber"
}
```

If the `operandRight` is empty, the response should be all BPNs and their assigned groups.

The response for the above request will be similar to the next block.

```json
[
   {
      "@id": "BPN000000001",
      "tx:groups": [
         "group4",
         "group3",
         "group5",
         "group2",
         "group1"
      ]
   },
   {
      "@id": "BPN000000002",
      "tx:groups": [
         "group1",
         "group2",
         "group5"
      ]
   },
   {
      "@id": "BPN000000003",
      "tx:groups": [
         "group1"
      ]
   }
]
```
   
  - The `query` object should contain the type of object to be queried and the criteria for filtering.
  - The response should include a list of BPNs and their assigned groups.
2. Create a query targeting the `edc_business_partner_group` table to evaluate and extract all BPNs and respective groups.
3. Using the `BusinessPartnerStore` include a new operation that returns the BPNs and BPN groups and the change is reflected in the two existing implementations (In Memory and SQL).
4. Response of the new endpoint is a key-value map with the key being the BPNs that meet the query constraints and the value containing the corresponding groups.

