# Important for Milestone 3!

Please note, that with the start of the **Milestone 3** release (v0.0.1) there exists an issue, where the BPN number cannot be retrieved from the DAPS token. The missing token BPN makes offers, protected by a BPN constraint, unavailable to all connectors.

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

The business partner number of another connector is part of the DAPS token. Once a BPN constraint is used in an access
policy the connector checks the token before sending out contract offers.

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
must contain the Business Partner Number.

The most simple BPN policy would allow the usage of certain data to a single Business Partner. An example `Policy` is
shown below. In this example the `edctype` properties are added, so that this policy may even be sent to the Data
Management API.

**Example 1 for single BPN:**
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

**Example 2 for multiple BPN:**
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
            "value": [ "<BPN1>", "<BPN2>" ]
          },
          "operator": "IN"
        }
      ]
    }
  ]
}
```

# Important: EDC Policies are input sensitive

Please be aware that the EDC ignores all Rules and Constraint it does not understand. This could cause your constrained policies to be public.

---

**Example 3 for accidentially public:**
```json
{
  "uid": "1",
  "prohibitions": [],
  "obligations": [],
  "permissions": [
    {
      "edctype": "dataspaceconnector:permission",
      "action": {
        "type": "MY-USE"
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
            "value": "BPNLCDQ90000X42KU"
          },
          "operator": "EQ"
        }
      ]
    }
  ]
}
```

This policy is public available, even though the constraint is described correct. The reason is, that this extension only registeres the Policy.Action `USE` within the EDC. Any other Action Type will have the EDC ignore the corresponding permission, hence interpret the polics as public policy.

---

**Example 4 for accidentially public:**

```json
{
  "uid": "1",
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
            "value": "BusinesPartnerNumber"
          },
          "rightExpression": {
            "edctype": "dataspaceconnector:literalexpression",
            "value": "BPNLCDQ90000X42KU"
          },
          "operator": "EQ"
        }
      ]
    }
  ]
}
```

This policy is public available, too. The cause is a typo in the left-expression of the constraint. This extension only registeres the Constraint.LeftExpression `BusinessPartnerNumber` within the EDC. Any other term will have the EDC ignore the corresponding constraint, hence interpret the polics as public policy.
