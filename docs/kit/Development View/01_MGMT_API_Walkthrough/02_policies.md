# Creating a Policy Definition

A policy is a declaration of a Data Consumer's rights and duties. Policies themselves make no statements about the
object that they may grant access and usage permission to. They are created at the EDC like this:

```http
POST /v3/assets HTTP/1.1
Host: https://provider-control.plane/api/management
X-Api-Key: password
Content-Type: application/json
```

```json
{
  "@context": {
    "odrl": "http://www.w3.org/ns/odrl/2/"
  },
  "@type": "PolicyDefinitionRequestDto",
  "@id": "<ID_SET_BY_CLIENT>",
  "policy": {
    "@type": "Policy",
    "odrl:permission": [
      {
        "odrl:action": "USE",
        "odrl:constraint": {
          "@type": "Constraint",
          "odrl:leftOperand": "BusinessPartnerNumber",
          "odrl:operator": {
            "@id": "odrl:eq"
          },
          "odrl:rightOperand": "<BPN_CONSUMER>"
        }
      }
    ]
  }
}

```

In the EDC, policies are pure [ODRL (Open Digital Rights Language)](https://www.w3.org/TR/odrl-model/).
Like the payloads of the [Dataspace Protocol](1-management-api-overview), they are written in JSON-LD. Even if the user
only has rudimentary knowledge of JSON-LD, the [policy playground](https://eclipse-tractusx.github.io/tutorial-resources/policy-playground/)
will provide a good starting point to start writing policies. It is important to keep in mind that the extensive ODRL-
context (that the EDC is aware of) allows for ergonomic reuse of the vocabulary in individual policies.

## Writing Policies for the EDC

ODRL's model and expressiveness surpass the EDC's current ability to interpret the policies and derive behavior from
them. This must be kept in mind even when Data Offers based on policies are not yet published to the Dataspace. Here again,
configuring the wrong policies is a risk for unsafe and non-compliant behavior. This is exacerbated by the fact that
the EDC interprets policies it can't evaluate as true by default. A couple of examples:

### Let all pass
```json
{
  "@context": {
    "odrl": "http://www.w3.org/ns/odrl/2/"
  },
  "@type": "PolicyDefinitionRequest",
  "@id": "{% uuid 'v4' %}",
  "policy": {
    "@type": "Policy",
    "odrl:permission": [
      {
        "odrl:action": "USE"
      }
    ]
  }
}
```

### Only let a Business Partner Group pass

A Business Partner Group is a group of BPNs that are allowed to pass this constraint. A BPN can be added
to a group even after a Contract Offer for a certain BPN-Group was published. The groups are persisted and maintained
in the Provider's Control Plane. The EDC-Management-API's `/business-partner-groups` endpoint offers CRUD-operations for
it. 

```json
{
  "@context": {
    "tx": "https://w3id.org/tractusx/v0.0.1/ns/"
  },
  "@type": "PolicyDefinitionRequest",
  "@id": "{% uuid 'v4' %}",
  "policy": {
    "@type": "Set",
    "@context": "http://www.w3.org/ns/odrl.jsonld",
    "permission": [
      {
        "action": "use",
        "constraint": [
          {
            "leftOperand": "tx:BusinessPartnerGroup",
            "operator": "isPartOf",
            "rightOperand": "<group>"
          }
        ]
      }
    ]
  }
}

```

### Chaining Constraints

Constraints can be chained together via logical constraints. This is currently implemented for `odrl:and`, `odrl:or`
and `odrl:xone` (exactly one constraint evaluates to `true`).

```json
{
  "@context": {
    "tx": "https://w3id.org/tractusx/v0.0.1/ns/"
  },
  "@type": "PolicyDefinitionRequest",
  "@id": "{{POLICY_ID}}",
  "policy": {
    "@type": "Set",
    "@context": "http://www.w3.org/ns/odrl.jsonld",
    "permission": [
      {
        "action": "use",
        "constraint": [
          {
            "@type": "LogicalConstraint",
            "and": [
              {
                "leftOperand": {
                  "@value": "<field>"
                },
                "operator": "eq",
                "rightOperand": "<value>"
              },
              {
                "leftOperand": "tx:BusinessPartnerGroup",
                "operator": "isPartOf",
                "rightOperand": "<group>"
              }
            ]
          }
        ]
      }
    ]
  }
}
```

Some permission-constraints trigger specific behavior in the EDC. That should be kept in mind when designing policies
and requires an understanding of how the EDC evaluates and acts upon them.

| `leftOperand`                                                | `rightOperand`                                   | usage in <br/> [Contract Definition](03_contractdefinitions.md) | description                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
|--------------------------------------------------------------|--------------------------------------------------|-----------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `BusinessPartnerNumber`                                      | a BPNL                                           | access or contract                                              | _This function is deprecated._ <br /> The leftOperand "BusinessPartnerNumber" will trigger a check against the property in a Consumer's Verfifiable Credential (VC) that holds said BPNL.                                                                                                                                                                                                                                                                                    |
| `https://w3id.org/tractusx/v0.0.1/ns/BusinessPartnerGroup`   | a Business Partner Group                         | access or contract                                              | see [above](#only-let-pass-a-business-partner-group). The `leftOperand` is in this case not queried from the Consumer's VC but acts as a signal to check the Consumer's BPN for membership in the designated Business Partner Group.                                                                                                                                                                                                                                         |
| `https://w3id.org/edc/v0.0.1/ns/InForceDate`                 | json-object with properties `@value` and `@type` | contract                                                        | If the negotiation via either [Contract Negotiation](6-contract-negotiation.md) or the [EDR process](8-edr.md) is successful, the EDC will only renew short-lived Data-Plane tokens for a contract if the contract is still valid (in force). Start and end dates can be set with absolute timestamps or relative to the time of the contract agreement. For exact syntax, visit the [playground](https://eclipse-tractusx.github.io/tutorial-resources/policy-playground/). |
| `https://w3id.org/tractusx/v0.0.1/ns/FrameworkAgreement.pcf` | "active"                                         | access or contract                                              | Framework agreements in Catena-X are legal documents signed by a Business Partner to participate in a Business Scenario. In return, her credential is enhanced with a reference to the corresponding framework agreement - like in this case `pcf`. A complete list of framework agreements is maintained by the Catena-X association in standards CX-0049 and -0050.                                                                                                        |

For more on the integration of Verifiable Credentials and the EDC in Catena-X, see the [specification of the Identity
and Trust Protocol (IATP)](https://github.com/eclipse-tractusx/identity-trust).

## Notice

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- SPDX-License-Identifier: CC-BY-4.0
- SPDX-FileCopyrightText: 2023 Contributors of the Eclipse Foundation
- Source URL: [https://github.com/eclipse-tractusx/tractusx-edc](https://github.com/eclipse-tractusx/tractusx-edc)