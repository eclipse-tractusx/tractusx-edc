# Business Partner Validation Extension

Using the Business Partner Validation Extension it's possible to add configurable validation against
Catena-X `Participants` in the `ContractDefinition.AccessPolicy`.

**Why only AccessPolicy?** Because when a custom validation is used in the `ContractPolicy`, it is necessary
to send it to the other connector. But nether is it possible to send a generic constraint using the IDS Protocol,
nor is it possible for another connector to enforce a generic constraint reliable. Hence, the limit
to `AccessPolicy`. This limitation is not technically enforceable, therefore adding Business Partner constraints to the
contract policy simply won't work.

This extension is already included in all the Catena-X control-planes and can be used accordingly.
It is recommended to have a basic understanding of the EDC contract/policy domain before using this extension. The
corresponding documentation can
be found in the [EDC GitHub Repository](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector). For a
simplified overview of the EDC domain please have a look at the Catena-X Control Plane documentation.

Example of business partner constraint:

```json
{
  "leftExpression": {
    "value": "BusinessPartner"
  },
  "rightExpression": {
    "value": "BPNLCDQ90000X42KU"
  },
  "operator": "EQ"
}
```

The `leftExpression` must always contain 'BusinessPartner', so that the policy functions of this extension are invoked.
Additionally, the only `operator` that is supported by these policy functions is 'EQ'. Finally, the `rightExpression`
must contain
the Business Partner Number.

The most simple BPN policy would allow the usage of certain data to a single Business Partner. An example `Policy` is
shown below. In this example the `edctype` properties are added, so that this policy may even be sent to the Data
Management API.

```json
{
  "uid": "<PolicyId>",
  "prohibitions": [],
  "obligations": [],
  "permissions": [
    {
      "edctype": "dataspaceconnector:permission",
      "action": {
        "type": "USE"
      },
      "constraints": [
        {
          "edctype": "AtomicConstraint",
          "leftExpression": {
            "edctype": "dataspaceconnector:literalexpression",
            "value": "BusinessPartnerNumber"
          },
          "rightExpression": {
            "edctype": "dataspaceconnector:literalexpression",
            "value": "<BPN>"
          },
          "operator": "EQ"
        }
      ]
    }
  ]
}
```

The business partner number of another connector is part of the DAPS token. Once a BPN constraint is used in an access
policy the connector checks the token before sending out contract offers.