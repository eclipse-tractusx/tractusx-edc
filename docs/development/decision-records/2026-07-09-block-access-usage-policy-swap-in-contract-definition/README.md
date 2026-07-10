# Add validation to Contract Definitions API

## Decision

We will implement a validator that blocks the creation or the update of a contract definition when the referenced 
policies are used in the wrong role: an access policy must not be bound as the usage policy, and vice-versa. Such 
contract definition will be rejected by the management API instead of being persisted and failing later at 
negotiation time.

## Rationale

There are rules as to which constraints are allowed in access policies versus usage policies. These rules are
enforced on the `policydefinitions` API by checking against the policy's `action` property. However, this
enforcement does not extend to the `contractdefinitions` API, so binding a valid access policy as the usage policy
of a contract definition, or the other way around, is currently not forbidden.

This leads to an ungraceful failure mode. Once a provider successfully binds an access policy as the usage policy,
the contract definition is stored without complaint. When a consumer later triggers a negotiation, the provider
connector fails with an HTTP 500 and logs an unhandled error because no policy handler is registered for the scope
in which the misused policy is evaluated.

Rejecting the invalid contract definition at submission time gives the provider a clear and actionable error instead
of a runtime failure that only appears at negotiation time.

## Approach

- Provide an extension that injects the `JsonObjectValidatorRegistry` and registers an additional `JsonObjectValidator`
for the `https://w3id.org/edc/v0.0.1/ns/ContractDefinition` type. We propose placing the new validator under
`edc-extensions/validators`, alongside the existing empty-asset-selector validator.
- Implement the `ContractDefinitionValidator`, which runs on top of the standard validator and adds two checks that
resolve the referenced policy definitions and verify their intended scope:
  - the policy referenced by `accessPolicyId` must be a valid **access** policy and must not be a usage policy.
  - the policy referenced by `contractPolicyId` must be a valid **usage** policy and must not be an access policy.
    
  Each policy's role is determined the same way it is enforced on the `policydefinitions` API.
- When either check fails, the validator returns a validation failure and the management API responds with a
`400 Bad Request` describing the misused policy, instead of persisting the contract definition and failing later at
negotiation time with an HTTP 500.
