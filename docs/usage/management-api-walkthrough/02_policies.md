# Policies

In the EDC, policies are pure [ODRL (Open Digital Rights Language)](https://www.w3.org/TR/odrl-model/).
Like the payloads of the [Dataspace Protocol](README.md), they are written in **JSON-LD**.

## Properties

The `Policy` object is extensible. EDC generally follows the subset that
the [Dataspace Protocol](https://eclipse-dataspace-protocol-base.github.io/DataspaceProtocol/2025-1/message/schema/contract-schema.json#/definitions/Policy)
has selected from [ODRL](https://www.w3.org/TR/odrl-model/#policy).

| Variable                                                                   | Content                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
|----------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `@context`                                                                 | In JSON-LD, `@context` is a fundamental concept used to define the mapping of terms used within the JSON-LD document to specific IRIs (Internationalized Resource Identifiers). It provides a way to establish a shared understanding of the vocabulary used in a JSON-LD document, making it possible to create structured and semantically rich data that can be easily integrated with other data sources on the web. You can choose to bind prefixes to namespaces manually via json properties. However, importing existing remote contexts like `"@context":[ "https://w3id.org/catenax/2025/9/policy/odrl.jsonld" ]` is usually less error-prone and strongly encouraged by the examples. |
| `policy`.`@type`                                                           | `@type` is a property in every json-ld object that describes which class it belongs to. In this context, the only accepted value is `Set`. `Set` is aimed at scenarios where there is an open criteria for the semantics of the policy expressions.                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| `policy`.`permission`<br/>`policy`.`obligation`<br/>`policy`.`prohibition` | These properties define the context of what may, may not under which condition be done with the Dataset in question.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| `policy`.`permission/oblication/prohibition`.`action`                      | Currently only the actions "use" (reused from ODRL) and "access" (specific to Catena-X) are conventions in the Dataspace. In the context of tractusx-edc, `action` should only be used for access policies and `use` should only be used for usage policies in a [`ContractDefinition`](./03_contractdefinitions.md).                                                                                                                                                                                                                                                                                                                                                                            |
| `policy`.`permission/oblication/prohibition`.`constraint`                  | A list of `Constraint` objects that each represent a boolean/logical expression. It binds an `action` to certain rules. The `leftOperand` instances must clearly be defined to indicate the semantics of the `Constraint`. In Catena-X, some `leftOperand`s of a `Constraint` are associated with a check on specific verifiable credentials (VC). As most are use-case-agreements, [this notation](https://github.com/eclipse-tractusx/tractusx-profiles/blob/main/cx/policy/specs/policy.mapping.md) is useful. **Right Operand:** The rightOperand is the value of the Constraint that is to be compared to the leftOperand.                                                                  |

## Policies in Catena-X

### Conventions on Constraints

Catena-X has set strict conventions for participants which kinds of Policies they are expected to process. This is
described in
Standard [CX-0152 - Policy Constraints for Data Exchange](https://catenax-ev.github.io/docs/next/standards/overview).
The standard defines Constraints which Catena-X participants can compose to Policies.

There are certain rules for which Constraints can be used in which context. When setting the helm chart's
`.Values.controlplane.policy.validation.enabled` to true, the `/management/v3/policydefinitions` API will reject those
requests that attempt to create policies with unknown constraints or unconventional constraint combinations. It is
highly recommended to keep the validation enabled to avoid accidental misconfiguration of data offers at runtime.

The following table was compiled from the [
`PolicyValidationConstants`](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/edc-extensions/cx-policy/src/main/java/org/eclipse/tractusx/edc/policy/cx/validator/PolicyValidationConstants.java#L75)
class - please refer back to it when doubts arise.

| Name                              | Action          | usable in                   | side-effects                                                                                                                                 |
|-----------------------------------|-----------------|-----------------------------|:---------------------------------------------------------------------------------------------------------------------------------------------|
| `BusinessPartnerGroup`            | `access`        | `permission`                | validated against the identity extracted from the `MembershipCredential`                                                                     |
| `BusinessPartnerNumber`           | `access`        | `permission`                | validated against the identity extracted from the `MembershipCredential`                                                                     |
| `FrameworkAgreement`              | `access`, `use` | `permission`                |                                                                                                                                              |
| `Membership`                      | `access`, `use` | `permission`                | validated against the `MembershipCredential`                                                                                                 |
| `inForceDate`                     | `access`, `use` | `permission`                | validated continuously - all Transfer Processes relying on an Agreement with this Constraint will be stopped when `inForceDate` is exceeded. |
| `AffiliatesRegion`                | `use`           | `permission`, `prohibition` |                                                                                                                                              |
| `AffiliatesBpnl`                  | `use`           | `permission`, `prohibition` |                                                                                                                                              |
| `DataFrequency`                   | `use`           | `permission`                |                                                                                                                                              |
| `VersionChanges`                  | `use`           | `permission`                |                                                                                                                                              |
| `ContractTermination`             | `use`           | `permission`                |                                                                                                                                              |
| `ConfidentialInformationMeasures` | `use`           | `permission`                |                                                                                                                                              |
| `ConfidentialInformationSharing`  | `use`           | `permission`                |                                                                                                                                              |
| `ExclusiveUsage`                  | `use`           | `permission`                |                                                                                                                                              |
| `Warranty`                        | `use`           | `permission`                |                                                                                                                                              |
| `WarrantyDefinition`              | `use`           | `permission`                |                                                                                                                                              |
| `WarrantyDurationMonths`          | `use`           | `permission`                |                                                                                                                                              |
| `Liability`                       | `use`           | `permission`                |                                                                                                                                              |
| `JurisdictionLocationReference`   | `use`           | `permission`                |                                                                                                                                              |
| `JurisdictionLocation`            | `use`           | `permission`                |                                                                                                                                              |
| `Precedence`                      | `use`           | `permission`                |                                                                                                                                              |
| `DataUsageEndDurationDays`        | `use`           | `permission`                |                                                                                                                                              |
| `DataUsageEndDate`                | `use`           | `permission`                |                                                                                                                                              |
| `DataUsageEndDefinition`          | `use`           | `permission`                |                                                                                                                                              |
| `DataUsageEndDate`                | `use`           | `permission`                |                                                                                                                                              |
| `DataUsageEndDate`                | `use`           | `permission`                |                                                                                                                                              |
| `DataProvisioningEndDurationDays` | `use`           | `obligation`                |                                                                                                                                              |
| `DataProvisioningEndDate`         | `use`           | `obligation`                |                                                                                                                                              |
| `UsageRestriction`                | `use`           | `prohibition`               |                                                                                                                                              |

### Policies & Verifiable Credentials (VC)

Many constraints refer to verifiable credentials (VC) that are stored in participant Credential Services, also known as
wallets. Power over their participants' own credentials is a fundamental principle of self-sovereign identity (SSI).

The key architectural principle underlying this specification is that policy definitions must be decoupled from their
corresponding VC schema. Namely, the specific `Constraint` serves as intermediator to the VC schema in the policy 
definition. This allows VC schemas in source code to be altered without impacting policy definitions.

## Policies in EDC

### Access vs Usage

In EDC, a distinction is made between **Access** and **Usage** Policies.

- **access policy:** determines whether a particular consumer is offered an asset or not. For example, we may want to
  restrict certain assets such that only consumers within a particular geography can see them. Consumers outside that
  geography wouldn't even have them in their catalog.
- **usage policy or contract policy:** determines the conditions for initiating a contract negotiation for a particular
  asset. Note that does not automatically guarantee the successful creation of a contract, it merely expresses the
  eligibility to start the negotiation. The terms "usage policy" and "contract policy" are used synonymously!

Whether a policy is used as access or usage policy is determined
during [contract definition](03_contractdefinitions.md).

### Creating a Policy Definition

Policies can be created in the EDC as follows:

```http request
POST /v3/policydefinitions HTTP/1.1
Host: https://provider-control.plane/api/management
X-Api-Key: password
Content-Type: application/json
```

```json
{
  "@context": [
    "https://w3id.org/catenax/2025/9/policy/odrl.jsonld",
    "https://w3id.org/catenax/2025/9/policy/context.jsonld",
    {
      "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
    }
  ],
  "@type": "PolicyDefinition",
  "@id": "membership-deg",
  "policy": {
    "@type": "Set",
    "permission": [
      {
        "action": "access",
        "constraint": [
          {
            "and": [
              {
                "leftOperand": "Membership",
                "operator": "eq",
                "rightOperand": "active"
              },
              {
                "leftOperand": "FrameworkAgreement",
                "operator": "eq",
                "rightOperand": "DataExchangeGovernance:1.0"
              }
            ]
          }
        ]
      }
    ]
  }
}
```

Please note that in JSON-LD, structures that look different may actually have the same meaning. They may be expanded
or compacted, define additional `@context` objects, refer to a predefined outside `context` or others. Using a parser
or the [json-ld playground](https://json-ld.org/playground/) helps to be consistent.

If the creation of the `policydefinition` was successful, the Management-API will return HTTP 201.

## Exemplary scenarios

For the following Scenarios, we assume there is a **Partner 1 (provider)** who wants to provide Data for **Partner 2
(consumer)**

- Partner 1 (provider) has the Business-Partner-Number BPN12345.
- Partner 2 (consumer) has the Business-Partner-Number BPN6789.

Partner 2 (consumer) signed the **Traceability Framework Agreement** and followed all the necessary steps that the
Credential appears within Partner 2s identity.

When doing a catalog request with
the [DCP](https://eclipse-dataspace-dcp.github.io/decentralized-claims-protocol/v1.0/)
presentation flow
the `MembershipCredential`, `DataExchangeGovernance` and `BpnCredential` are provided to Partner 1:

For example:

```json
{
  "@context": [
    "https://www.w3.org/2018/credentials/v1",
    "https://w3id.org/catenax/credentials/v1.0.0"
  ],
  "id": "1f36af58-0fc0-4b24-9b1c-e37d59668089",
  "type": [
    "VerifiableCredential",
    "MembershipCredential"
  ],
  "issuer": "did:web:com.example.issuer",
  "issuanceDate": "2021-06-16T18:56:59Z",
  "expirationDate": "2022-06-16T18:56:59Z",
  "credentialSubject": {
    "id": "did:web:com.example.participant",
    "holderIdentifier": "BPN6789"
  }
}
```

For other subsequent requests like Contract negotiation requests and transfer process requests, the presented
credentials are based on the usage/contract policy. This means VC based policies can be used only
in the usage/contract policy.

#### Scenario 1

Partner 1 wants to create an Access Policy, that Partner 2 can receive the Contract Offer if its BPN matches. But a
Contract Agreement should only be created if Partner 2 also signed the Traceability Framework Agreement. So in this
case, Partner 2 should receive the Contract Offer in the first place, regardless if it signed the Traceability Framework
Agreement. The signing of the Agreement should be checked at the time of contract negotiation.

##### Partner 1 - Access Policy Example (Scenario 1)

```json
{
  "@context": [
    "https://w3id.org/catenax/2025/9/policy/odrl.jsonld",
    "https://w3id.org/catenax/2025/9/policy/context.jsonld",
    {
      "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
    }
  ],
  "@type": "PolicyDefinition",
  "@id": "{{POLICY_ID}}",
  "policy": {
    "@type": "Set",
    "permission": [
      {
        "action": "access",
        "constraint": {
          "leftOperand": "BusinessPartnerNumber",
          "operator": "eq",
          "rightOperand": "BPN6789"
        }
      }
    ]
  }
}
```

##### Partner 1 - Usage/Contract Policy Example (Scenario 1)

```json
{
  "@context": [
    "https://w3id.org/catenax/2025/9/policy/odrl.jsonld",
    "https://w3id.org/catenax/2025/9/policy/context.jsonld",
    {
      "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
    }
  ],
  "@type": "PolicyDefinition",
  "@id": "{{POLICY_ID}}",
  "policy": {
    "@type": "Set",
    "permission": [
      {
        "action": "use",
        "constraint": {
          "leftOperand": "FrameworkAgreement",
          "operator": "eq",
          "rightOperand": "DataExchangeGovernance:1.0"
        }
      }
    ]
  }
}
```

##### Desired Outcome (Scenario 1)

Partner 2 receives the Contract Offer and is able to negotiate the contract because he presents a valid
`DataExchangeGovernanceCredential`.

#### Scenario 2

Partner 1 wants to create an Access Policy that Partner 2 can receive the Contract Offer if the BPN is matching
but a Contract Agreement should only be created if Partner 2 is identified as a Dismantler (owns the "
DismantlerCredential").

##### Partner 1 - Access Policy Example (Scenario 2)

```json
{
  "@context": [
    "https://w3id.org/catenax/2025/9/policy/odrl.jsonld",
    "https://w3id.org/catenax/2025/9/policy/context.jsonld",
    {
      "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
    }
  ],
  "@type": "PolicyDefinition",
  "@id": "{{POLICY_ID}}",
  "policy": {
    "@type": "Set",
    "permission": [
      {
        "action": "use",
        "constraint": {
          "leftOperand": "BusinessPartnerNumber",
          "operator": "eq",
          "rightOperand": "BPN6789"
        }
      }
    ]
  }
}
```

##### Partner 1 - Usage/Contract Policy Example (Scenario 2)

```json
{
  "@context": [
    "https://w3id.org/catenax/2025/9/policy/odrl.jsonld",
    "https://w3id.org/catenax/2025/9/policy/context.jsonld",
    {
      "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
    }
  ],
  "@type": "PolicyDefinition",
  "@id": "{{POLICY_ID}}",
  "policy": {
    "@type": "Set",
    "permission": [
      {
        "action": "use",
        "constraint": {
          "leftOperand": "Dismantler",
          "operator": "eq",
          "rightOperand": "active"
        }
      }
    ]
  }
}
```

##### Desired Outcome (Scenario 2)

Partner 2 receives the Contract Offer in the first place.

The contract negotiation, started by Partner 2 fails because he has not been identified as Dismantler and therefore does
not own the Dismantler Credential.

#### Writing Policies for the EDC

ℹ️ ODRL's model and expressiveness surpass the EDC's current ability to interpret the policies and derive behavior from
them. This must be kept in mind even when Data Offers based on policies are not yet published to the Dataspace. Here
again, configuring the wrong policies is a risk for unsafe and non-compliant behavior. This is exacerbated by the fact
that the EDC interprets policies it can't evaluate as true by default. A couple of examples:

#### Let all pass

```json
{
  "@context": [
    "https://w3id.org/catenax/2025/9/policy/odrl.jsonld",
    "https://w3id.org/catenax/2025/9/policy/context.jsonld",
    {
      "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
    }
  ],
  "@type": "PolicyDefinition",
  "@id": "{{POLICY_ID}}",
  "policy": {
    "@type": "Set",
    "permission": [
      {
        "action": "use"
      }
    ]
  }
}
```

#### Only let a Business Partner Group pass

A Business Partner Group is a group of BPNs that are allowed to pass this constraint. A BPN can be added
to a group even after a Contract Offer for a certain BPN-Group was published. The groups are persisted and maintained
in the Provider's Control Plane. The EDC-Management-API's `/business-partner-groups` endpoint offers CRUD-operations for
it.

```json
{
  "@context": [
    "https://w3id.org/catenax/2025/9/policy/odrl.jsonld",
    "https://w3id.org/catenax/2025/9/policy/context.jsonld",
    {
      "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
    }
  ],
  "@type": "PolicyDefinition",
  "@id": "{{POLICY_ID}}",
  "policy": {
    "@type": "Set",
    "permission": [
      {
        "action": "use",
        "constraint": {
          "leftOperand": "BusinessPartnerGroup",
          "operator": "eq",
          "rightOperand": "gold-partners"
        }
      }
    ]
  }
}
```

#### Chaining Constraints

Constraints can be chained together via logical constraints. This is currently permitted for `odrl:and`.

```json
{
  "@context": [
    "https://w3id.org/catenax/2025/9/policy/odrl.jsonld",
    "https://w3id.org/catenax/2025/9/policy/context.jsonld",
    {
      "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
    }
  ],
  "@type": "PolicyDefinition",
  "@id": "{{POLICY_ID}}",
  "policy": {
    "@type": "Set",
    "permission": [
      {
        "action": "use",
        "constraint": [
          {
            "and": [
              {
                "leftOperand": "<leftOperand>",
                "operator": "eq",
                "rightOperand": "<value>"
              },
              {
                "leftOperand": "BusinessPartnerGroup",
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

- SPDX-License-Identifier: CC-BY-4.0
- SPDX-FileCopyrightText: 2023, 2025 Contributors of the Eclipse Foundation
- Source URL: [https://github.com/eclipse-tractusx/tractusx-edc](https://github.com/eclipse-tractusx/tractusx-edc)
