# Validate Contract Definitions Policies

## Decision

The Tractux-X project will provide custom validations when creating and updating contract definitions so that the
access policy references a valid access policy and the contract policy references a valid usage policy.

## Rationale

Currently it is possible to have contract definitions where the access policy references a contract policy and/or
the contract policy references an access policy. This creates an invalid application state.

As there are specific constraints only allowed in certain types of policies, the contract policy evaluation during
the negotiation request leads to unhandled errors, and therefore no information about what caused the failure back
to the user.

Rejecting the invalid contract definition at submission time gives the provider a clear and actionable error instead
of a runtime failure at a later stage.

## Approach

- Provide an extension that injects the [`JsonObjectValidatorRegistry`](https://github.com/eclipse-edc/Connector/blob/v0.17.0/spi/common/validator-spi/src/main/java/org/eclipse/edc/validator/spi/JsonObjectValidatorRegistry.java)
  and registers an additional [`JsonObjectValidator`](https://github.com/eclipse-edc/Connector/blob/v0.17.0/core/common/lib/validator-lib/src/main/java/org/eclipse/edc/validator/jsonobject/JsonObjectValidator.java)
  for the `https://w3id.org/edc/v0.0.1/ns/ContractDefinition` type.
- The new validator must resolve the referenced policy definitions and verify the following:
  - the policy definitions exist
  - the policy referenced by `accessPolicyId` is a valid **access** policy, i.e. has `policy.permission.action`
    equal to `https://w3id.org/catenax/2025/9/policy/access`.
  - the policy referenced by `contractPolicyId` is a valid **usage** policy, i.e. has `policy.permission.action`
    equal to `http://www.w3.org/ns/odrl/2/use`.
- If either check fails, the validator returns a validation failure and the management API responds with a
  `400 Bad Request`. Otherwise, the contract definition is persisted.

Additionally, another validator must be implemented to deny updating policy definitions referenced by contract 
definitions. Without it, an access policy could be updated into an usage policy and vice-versa, making the bound
contract definition invalid.

- Provide an extension that injects the [`JsonObjectValidatorRegistry`](https://github.com/eclipse-edc/Connector/blob/v0.17.0/spi/common/validator-spi/src/main/java/org/eclipse/edc/validator/spi/JsonObjectValidatorRegistry.java)
  and registers an additional [`JsonObjectValidator`](https://github.com/eclipse-edc/Connector/blob/v0.17.0/core/common/lib/validator-lib/src/main/java/org/eclipse/edc/validator/jsonobject/JsonObjectValidator.java)
  for the `https://w3id.org/edc/v0.0.1/ns/PolicyDefinition` type.
- The new validator must verify if the policy definition is referenced by any contract definition.
- If it is, the validator  returns a validation failure and the management API responds with a
  `400 Bad Request`. Otherwise, the policy definition is updated.
