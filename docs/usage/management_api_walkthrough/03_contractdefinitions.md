# Creating a Contract Definition

A Contract Definition is the connection between a set of [Assets](01_assets.md) with one Access Policy and one Contract
Policy. The two policies are both policies as explained [previously](02_policies.md) but checked in different
stages of communication between Data Provider and Data Consumer. The creation request looks like this:

```http
POST /v2/contractdefinitions HTTP/1.1
Host: https://provider-control.plane/api/management
X-Api-Key: password
Content-Type: application/json
```

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "ContractDefinition",
  "@id": "myContractDefinitionId",
  "accessPolicyId": "myAccessPolicyId",
  "contractPolicyId": "myContractPolicyId",
  "assetsSelector": 
    {
      "operandLeft": "https://w3id.org/edc/v0.0.1/ns/id",
      "operator": "=",
      "operandRight": "myAssetId"
    }
  
}
```
`accessPolicyId` and `contractPolicyId` are the identifiers of the [policies](02_policies.md) used in the contract
definition. On creation, the EDC does not automatically check if a policy with the corresponding `@id` exists - the call
sequence will fail later when the Data Consumer attempts to find the offer in the [catalog-request](04_catalog.md).

## assetsSelector

The `assetsSelector` is a EDC-Criterion. This class specifies filters over a set of objects, Assets in this case. The
concept is functionally similar to the `odrl:Constraint` in a [Policy](3-policy-definitions.md) but syntactically different.
- `operandLeft` is a property in the Entity (`edc:Asset` in this case) that is assigned a value. If the property is nested,
  traversion can be achieved by chaining the properties like `"'https://w3id.org/edc/v0.0.1/ns/nested'.'https://w3id.org/edc/v0.0.1/ns/key'"`
  Note that this function is namespace-aware so the `operandLeft` must either be written in extended form (see above)
  or in a prefixed form with a corresponding entry in the `@context`.
- `operator` is the logical operation that will be used to compare the `operandLeft` with the `operandRight`. The possible
  values are `=` (equivalence), `in` (existence in a list) and `like` (regex match).
- `operandRight` is the constant that the dynamically retrieved value of `operandLeft` will be compared to via the `operator`.

This mechanism allows the administrator to bind the same policies to multiple assets. The example on the top of this page
will only match a single Asset as the `edc:id` will be unique as it's derived from the Asset's `@id`. It is however possible
to match multiple Assets if they share a common property:

```json
{
  "assetsSelector": {
    "operandLeft": "https://w3id.org/edc/v0.0.1/ns/myCommonProperty",
    "operator": "=",
    "operandRight": "sharedValue"
  }
}
```
These can also be chained together with a logical AND:

```json
{
  "assetsSelector": [
    {
      "operandLeft": "https://w3id.org/edc/v0.0.1/ns/myCommonProperty",
      "operator": "=",
      "operandRight": "sharedValue"
    },
    {
      "operandLeft": "https://w3id.org/edc/v0.0.1/ns/myOtherProperty",
      "operator": "=",
      "operandRight": "otherSharedValue"
    }
  ]
}
```

The `edc:Criterion` mechanism is used as well in the provider-internal request-endpoints where it's
part of the `edc:QuerySpec` objects that also allow pagination:

- `POST /v3/assets/request`
- `POST /v2/policydefinitions/request`
- `POST /v2/contractdefinitions/request`

## Side-Effects

The [Adoption View](../README) shows the basic connection between the core concepts of
the EDC. Contract Offers for a particular Data Consumer are created dynamically from the Contract Definitions created
by a Data Provider. The mechanics are explained in the section on the [catalog-API](04_catalog.md). But already at this
stage, Data Providers must be aware that creating a Contract Definition is sufficient to expose a Backend System
(as defined in the [Asset](01_assets.md)) to the Dataspace and let third parties access it.
After contract definition, an EDC will automatically allow data access if a requesting party passes the policies.

Contract Definitions thus must be created with great care. It is essential to align the backend-credentials with the
Access and Contract Policies to manage access consistently from the Dataspace to the backend data.

## Notice

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- SPDX-License-Identifier: CC-BY-4.0
- SPDX-FileCopyrightText: 2023 Contributors of the Eclipse Foundation
- Source URL: [https://github.com/eclipse-tractusx/tractusx-edc](https://github.com/eclipse-tractusx/tractusx-edc)