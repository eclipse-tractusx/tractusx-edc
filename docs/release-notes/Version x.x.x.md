# Release Notes Version 0.1.2
xx.xx.2022

# 1. Product EDC

## 1.1 Business Partner Extension

**Removed support for Constraint with multiple BPNs**
The possibility to use multiple Business Partner Numbers inside of a single constraint has been removed. It looks like this was only possible due to a missing feature and may lead to unexpected side effects (https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/2026)

Hence, this kind of policy is no longer supported!
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

The BPN extension will now always decline BPN policies with 'IN' operators, when asked by the EDC to enforce it.
