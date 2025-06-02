# Policies

## Policies in Catena-X

In the EDC, policies are pure [ODRL (Open Digital Rights Language)](https://www.w3.org/TR/odrl-model/).
Like the payloads of the [Dataspace Protocol](README.md), they are written in **JSON-LD**. Even if the
user only has rudimentary knowledge of [JSON-LD](https://json-ld.org/), the [**policy playground
**](https://eclipse-tractusx.github.io/tutorial-resources/policy-playground/) will provide a good starting point to
start
writing policies. It is important to keep in mind that the extensive ODRL-context (that the EDC is aware of) allows for
ergonomic reuse of the vocabulary in individual policies.

### Policies & Verifiable Credentials (VC)

#### General Information

Catena-X uses policies to determine access to and use of data. The policies refer to verifiable credentials (VC) that
are stored in the Wallets. Catena-X uses the principle of self-sovereign identity (SSI).

The key architectural principle underlying this specification is that policy definitions must be decoupled from their
corresponding VC schema. Namely, the specific **constraints** (
see [ODRL-classes](#odrl-information-model-classes-excerpt))
and shape of the VC schema must not be reflected in the policy definition. This allows VC schemas to be altered without
impacting policy definitions.

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
    "https://w3id.org/tractusx/policy/v1.0.0",
    "http://www.w3.org/ns/odrl.jsonld",
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
          "rightOperand": "Pcf:<version>"
        }
      }
    ]
  }
}
```

| Variable                           | Content                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
|------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `@context`                         | In JSON-LD, `@context` is a fundamental concept used to define the mapping of terms used within the JSON-LD document to specific IRIs (Internationalized Resource Identifiers). It provides a way to establish a shared understanding of the vocabulary used in a JSON-LD document, making it possible to create structured and semantically rich data that can be easily integrated with other data sources on the web.                                                                                                                                                                                        |
| `@context`.`odrl:`                 | Prefixes allow you to define short aliases for longer IRIs. For example, instead of repeatedly using the full IRI [http://www.w3.org/ns/odrl/2/](http://www.w3.org/ns/odrl/2/), you can define a prefix like "odrl" and append a segment/fragment to identify the resource in the namespace.                                                                                                                                                                                                                                                                                                                    |
| `@id`                              | A Policy MUST have one uid property value (of type IRI) to identify the Policy.  Note: The `@id` is on the upper level. It is a database policy definition which wraps the ODRL policy.                                                                                                                                                                                                                                                                                                                                                                                                                         |
| `policy`.`@type`                   | A Set Policy is the default Policy subclass. The Set is aimed at scenarios where there is an open criteria for the semantics of the policy expressions and typically refined by other systems/profiles that process the information at a later time. No privileges are granted to any Party (if defined). More detailed information about the possible policy subclasses can be found [here](https://w3c.github.io/poe/model/#infoModel).                                                                                                                                                                       |
| `policy`.`permission`              | A Policy MUST have at least one permission, prohibition, or obligation property values.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| `policy`.`permission`.`action`     | "use" the target asset (under a specific permission), currently only the action "use" is used by Catena-X                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| `policy`.`permission`.`constraint` | A boolean/logical expression that refines an Action and Party/Asset collection or the conditions applicable to a Rule. The leftOperand instances MUST clearly be defined to indicate the semantics of the Constraint. Catena-X will use the **left operand** of a *constraint* to associate a specific verifiable credential (VC). As most are use-case-agreements, [this notation](https://github.com/eclipse-tractusx/tractusx-profiles/blob/main/cx/policy/specs/policy.mapping.md) is useful. **Right Operand:** The rightOperand is the value of the Constraint that is to be compared to the leftOperand. |

Please note that in JSON-LD, structures that may look different may actually have the same meaning. They may be expanded
or compacted, define additional `@context` objects, refer to a predefined outside `context` or others. Using a parser
or the [json-ld playground](https://json-ld.org/playground/) helps to be consistent.

If the creation of the `policy-definition` was successful, the Management-API will return HTTP 200.

The JSON-LD context to include depends on the type of constraint:

- ODRL -> `http://www.w3.org/ns/odrl.jsonld` (always)
- BPN/BPN-Groups -> `https://w3id.org/tractusx/edc/v0.0.1`
- VC -> `https://w3id.org/tractusx/policy/v1.0.0`

The `https://w3id.org/tractusx/edc/v0.0.1` context is available only from 0.7.1 version and not in the initial one 0.7.0

An equivalent syntax would be 

```json
{
  "@context" : [
    "http://www.w3.org/ns/odrl.jsonld",
    {
      "tx": "https://w3id.org/tractusx/v0.0.1/ns/"
    }
  ],
  "policy": {
    "permission" : {
      "constraint" : {
        "leftOperand" : "tx:BusinessPartnerGroup" // or tx:BusinessPartnerNumber
        ..
      }
    }
  }
}
```

#### Catena-X specific `constraints`

This implementation (`tractusx-edc`) contains extensions that trigger specific behavior when encountering specific
policies.

1. **Checks against the use-case**: The [cx-policy extension](https://github.com/eclipse-tractusx/tractusx-edc/tree/main/edc-extensions/cx-policy/src/main/java/org/eclipse/tractusx/edc/policy/cx) is responsible to resolve a use-case-specific
   leftOperand against a VC. The list of use-case credentials can be found [here](https://github.com/eclipse-tractusx/tractusx-profiles/tree/main/cx/credentials/samples).
2. **Checks against the BPN**: The [BPN-validation extension](https://github.com/eclipse-tractusx/tractusx-edc/tree/main/edc-extensions/bpn-validation) allows to define either a single Business Partner
   authorized to pass the constraint or define a group of BPNs that may pass and can be extended at runtime.
3. **Checks for temporal validity**: If a usage policy is defined against a HTTP-based asset accessible via EDR-tokens,
   the Data Provider can prohibit issuance of new tokens by defining a specific constraint based on the
   [contract validity check extension](https://eclipse-edc.github.io/documentation/for-adopters/control-plane/policy-engine/#in-force-policy)

### Access & Usage Policies

In Catena-X, a distinction is made between **Access** and **Usage** Policies.

- **access policy:** determines whether a particular consumer is offered an asset or not. For example, we may want to
  restrict certain assets such that only consumers within a particular geography can see them. Consumers outside that
  geography wouldn't even have them in their catalog.
- **usage policy or contract policy:** determines the conditions for initiating a contract negotiation for a particular
  asset. Note that does not automatically guarantee the successful creation of a contract, it merely expresses the
  eligibility to start the negotiation. The terms "usage policy" and "contract policy" are used synonymously!

**The Access and Usage Policies are not distinguished by any special semantics, but rather by the time at which they are
checked.**

Whether a policy is used as access or usage policy is determined during [contract definition](03_contractdefinitions.md).

### Exemplary scenarios

For the following Scenarios, we assume there is a **Partner 1 (provider)** who wants to provide Data for **Partner 2
(consumer)**

- Partner 1 (provider) has the Business-Partner-Number BPN12345.
- Partner 2 (consumer) has the Business-Partner-Number BPN6789.

Partner 2 (consumer) signed the **Traceability Framework Agreement** and followed all the necessary steps that the
Credential appears within Partner 2s identity.

When doing a catalog request with the [IATP](https://github.com/eclipse-tractusx/identity-trust/blob/main/specifications/verifiable.presentation.protocol.md) presentation flow
the `MembershipCredential` is provided to Partner 1:

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
    "https://w3id.org/tractusx/edc/v0.0.1",
    "http://www.w3.org/ns/odrl.jsonld",
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

##### Partner 1 - Usage/Contract Policy Example (Scenario 1)

```json
{
  "@context": [
    "https://w3id.org/tractusx/policy/v1.0.0",
    "http://www.w3.org/ns/odrl.jsonld",
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
          "rightOperand": "Traceability:<version>"
        }
      }
    ]
  }
}
```

##### Desired Outcome (Scenario 1)

Partner 2 receives the Contract Offer and is able to negotiate the contract because he owns the Traceability Credential.

#### Scenario 2

Partner 1 wants to create an Access Policy that Partner 2 can receive the Contract Offer if the BPN is matching 
but a Contract Agreement should only be created if Partner 2 is identified as a Dismantler (owns the "DismantlerCredential").

##### Partner 1 - Access Policy Example (Scenario 2)

```json
{
  "@context": [
    "https://w3id.org/tractusx/edc/v0.0.1",
    "http://www.w3.org/ns/odrl.jsonld",
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
    "https://w3id.org/tractusx/policy/v1.0.0",
    "http://www.w3.org/ns/odrl.jsonld",
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
    "http://www.w3.org/ns/odrl.jsonld",
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
    "https://w3id.org/tractusx/edc/v0.0.1",
    "http://www.w3.org/ns/odrl.jsonld",
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

Constraints can be chained together via logical constraints. This is currently implemented for `odrl:and`, `odrl:or`
and `odrl:xone` (exactly one constraint evaluates to `true`).

```json
{
  "@context": [
    "https://w3id.org/tractusx/edc/v0.0.1",
    "http://www.w3.org/ns/odrl.jsonld",
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

## Additional Information about Policies

ℹ️ All explanations in this chapter "General Information about Policies" were taken from the
following [source](https://w3c.github.io/poe/model/).

### Introduction

Several business scenarios require expressing what are the permitted and prohibited actions over resources. These
permitted/prohibited actions are usually expressed under the form of policies, i.e., expressions that indicate those
uses and re-uses of the content which are conformant with existing regulations or to the constraints assigned by the
owner. Policies may also be enriched with additional information, i.e., who are the entities in charge of the definition
of such Policy and those who are required to conform to it, what are the additional constrains to be associated with the
Permissions, Prohibitions and Duties expressed by the Policy. The ability to express these concepts and relationships is
important both for the producers of content, i.e., they may state in a clear way what are the permitted and the
prohibited actions to prevent misuse, and for the consumers, i.e., they may know precisely what resources they are
allowed to use and re-use to avoid breaking any rules, laws or the owner's constraints. This specification describes a
common approach to expressing these policy concepts.

### Semantic Model

The ODRL Information Model defines the underlying semantic model for permission, prohibition, and obligation statements
describing content usage. The information model covers the core concepts, entities and relationships that provide the
foundational model for content usage statements. These machine-readable policies may be linked directly with the content
they are associated to with the aim to allow consumers to easily retrieve this information.

#### ODRL Information Model classes (excerpt)

| Class                          | Description                                                                                                                                                                                                                   |
|--------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `Policy`                       | A non-empty group of Permissions (via the permission property) and/or Prohibitions (via the prohibition property) and/or Duties (via the obligation property).                                                                |
| `Set`                          | Subclass of `Policy`:Supports expressing generic Rules                                                                                                                                                                        |
| `Action`                       | An operation on an Asset                                                                                                                                                                                                      |
| `Rule`                         | An abstract concept that represents the common characteristics of Permissions, Prohibitions, and Duties.                                                                                                                      |
| `Prohibition`                  | Subclass of `Rule`: The ability to exercise an Action over an Asset. The Permission MAY also have the duty property that expresses an agreed Action that MUST be exercised (as a pre-condition to be granted the Permission). |
| `Permission`                   | Subclass of `Rule`: The inability to exercise an Action over an Asset.                                                                                                                                                        |
| `Duty`                         | Subclass of `Rule`: The obligation to exercise an Action.                                                                                                                                                                     |
| `Constraint/LogicalConstraint` | A boolean/logical expression that refines an Action and Party/Asset collection or the conditions applicable to a Rule.                                                                                                        |

#### The `Policy` Class (excerpt)

The Policy class has the following properties (see example below):

- A Policy MUST have one uid property value (of type IRI [rfc3987]) to identify the Policy.
- A Policy MUST have at least one permission, prohibition, or obligation property values of type Rule. (See the
  Permission, Prohibition, and Obligation sections for more details.)

#### The `Set` Class

An ODRL Policy of subclass `Set` represents any combination of Rules. The `Set` Policy subclass is also the **default**
subclass of Policy (if none is specified).

Example:

```json
{
  "@context": "http://www.w3.org/ns/odrl.jsonld",
  "@type": "Set",
  "@id": "{{ID_SET_BY_CLIENT}}",
  "target": "{{ID_OF_TARGET_DATASET}}",
  "permission": [
    {
      "action": "use"
    }
  ]
}
```

ℹ️ For the examples in this document, the ODRL Policy subclasses are mapped to the JSON-LD `@type` tokens. The above
example could have also used `Policy` type instead of `Set` type (**as they are equivalent**).

- SPDX-License-Identifier: CC-BY-4.0
- SPDX-FileCopyrightText: 2023 Contributors of the Eclipse Foundation
- Source URL: [https://github.com/eclipse-tractusx/tractusx-edc](https://github.com/eclipse-tractusx/tractusx-edc)
