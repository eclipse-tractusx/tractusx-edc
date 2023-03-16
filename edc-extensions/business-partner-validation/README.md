# Business Partner Validation Extension

Using the Business Partner Validation Extension it's possible to add configurable validation against
Catena-X `Participants` in the `ContractDefinition.AccessPolicy`. Using a BPN in `ContractDefinition.ContractPolicy` is possible, too, but once the contract is complete there is no policy enforcement in place from the EDC.

It is recommended to have a basic understanding of the EDC contract/policy domain before using this extension. The
corresponding documentation can be found in the [EDC GitHub Repository](https://github.com/eclipse-edc/Connector).

The business partner number of another connector is part of its DAPS token. Once a BPN constraint is used in an access
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

## Single BusinessPartnerNumber example

The most simple BPN policy would allow the usage of certain data to a single Business Partner. An example `Policy` is
shown below. 
In this example the `edctype` properties are added, so that this policy may even be sent to the Management API.

```json
{
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

## Multiple BusinessPartnerNumber example

To define multiple BPN and allow multiple participants to use the data the `orconstraint` should be used.
It will permit the constraints contained to be evaluated using the `OR` operator.
```json
{
  "permissions": [
    {
      "edctype": "dataspaceconnector:permission",
      "action": {
        "type": "USE",
      },
      "constraints": [
        {
          "edctype": "dataspaceconnector:orconstraint",
          "constraints": [
            {
              "edctype": "AtomicConstraint",
              "leftExpression": {
                "edctype": "dataspaceconnector:literalexpression",
                "value": "BusinessPartnerNumber"
              },
              "rightExpression": {
                "edctype": "dataspaceconnector:literalexpression",
                "value": "<BPN1>"
              },
              "operator": "EQ"
            },
            {
              "edctype": "AtomicConstraint",
              "leftExpression": {
                "edctype": "dataspaceconnector:literalexpression",
                "value": "BusinessPartnerNumber"
              },
              "rightExpression": {
                "edctype": "dataspaceconnector:literalexpression",
                "value": "<BPN2>"
              },
              "operator": "EQ"
            },
            
            ...
            
            // other constraints can be added
          ]
        }
      ],
      "duties": []
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
