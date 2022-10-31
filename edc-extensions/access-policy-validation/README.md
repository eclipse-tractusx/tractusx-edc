# Policy Validation Extension

Using the Policy Validation Extension it's possible to add configurable validation against
Catena-X `Participants` in the `ContractDefinition.AccessPolicy`. Using a BPN in `ContractDefinition.ContractPolicy` is possible, too, but once the contract is complete there is no policy enforcement in place from the EDC.

The Extension supports the enforcement against the following DAPS claims:
- `BusinessPartnerNumber`
- `Role`
- `Attribute`

It is recommended to have a basic understanding of the EDC contract/policy domain before using this extension. The
corresponding documentation can be found in the [EDC GitHub Repository](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector).

The business partner number of another connector is part of its DAPS token. So are it's role and attribute. Once a cx-constraint is used in an access
policy the connector checks the token before sending out contract offers.

Example of cx-constraint:

```json
{
  "leftExpression": {
    "value": "<BusinessPartner | Role | Attribute>"
  },
  "rightExpression": {
    "value": "<BPNLCDQ90000X42KU | Dismantler | ISO-Certificate >"
  },
  "operator": "EQ"
}
```

The `leftExpression` must always contain 'BusinessPartner', 'Role' or 'Attribute'. Otherwise the policy functions of this extension are not invoked.
Additionally, the only `operator` that is supported by these policy functions is 'EQ'. Finally, the `rightExpression`
can contain any string.

The most simple BPN policy would allow the usage of certain data to a single Business Partner. An example `Policy` is
shown below. In this example the `edctype` properties are added, so that this policy may even be sent to the Data
Management API. 

It is also possible to combine multiple constraints by wrapping them into an EDC `OrConstraint` (see EDC Repository). Otherwise multiple constraints are enforced using logical 'AND'.

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

# Important: EDC Policies are input sensitive

Please be aware that the EDC ignores all Rules and Constraint it does not understand. This could cause your constrained policies to be public.

---

**Example 1 for accidentially public:**
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

**Example 2 for accidentally public:**

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

This policy is public available, too. The cause is a typo in the left-expression of the constraint. This extension only registers the `Constraint.LeftExpression` `BusinessPartnerNumber` within the EDC. Any other term will have the EDC ignore the corresponding constraint, hence interpret the policies as public policy.
