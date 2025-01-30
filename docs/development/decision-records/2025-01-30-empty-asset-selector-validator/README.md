# Add Validator to Block Contract Definitions with Empty Asset Selector

## Decision

Tractusx-edc will provide a feature that allows blocking the creation or update of contract definitions to contain
an empty asset selector. This feature will have the option to be switched on/off, being off the default state.

## Rationale

The `assetsSelector` property is a query (similar to a SQL SELECT statement) that returns a set of assets the
contract definition applies to. When left empty, the default behavior of the connector is to apply the contract
definition to all registered assets.

Unwillingly, mostly due to lack of proper connector usage knowledge, users might expose datasets that shouldn't be
exposed.

As changing the default behavior of the `assetSelector` is not an option - it is as such by design - preventing
such exposure can be achieved by applying the following approach.

## Approach

- Provide an extension that registers in the `JsonObjectValidatorRegistry` a new `JsonObjectValidator` for the
  `https://w3id.org/edc/v0.0.1/ns/ContractDefinition` type. This validator should replace the one provided by default
  by the contract definitions api.
- The registration of the new `JsonObjectValidator` should be conditioned to setting the connector configuration
  `tx.edc.validator.contractdefinitions.blockemptyassetselector=true`
- Implement the new `JsonObjectValidator` which should behave similarly to the existing
  `ContractDefinitionValidator` except it should fail the validation in case the creation/update of a contract
  definition introduces an empty `assetSelector` properties.


