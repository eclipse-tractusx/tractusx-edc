# Contract Definitions Validator: Empty Asset Selector

The goal of this extension is to provide a replacement validator for contract definition entities.
It is used to validate requests that create or update contract definitions via the data management API endpoint.
When enabled, it prevents contract definitions with no asset selector, or an empty one, from being created which
elsewise leads to all available assets being included in the Contract Definition.

This extension is included with the standard tractusx-edc distribution, but is disabled by default. To enable it,
you can set `tx.edc.validator.contractdefinitions.block-empty-asset-selector`to `true` in your connector configuration.

## Example

When the validator extension is enabled, creating the following contract definition will lead to a 400 error.

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "ContractDefinition",
  "@id": "myContractDefinitionId",
  "accessPolicyId": "myAccessPolicyId",
  "contractPolicyId": "myContractPolicyId"
}
```

Above, the `assetSelector` property is missing from the request, so an empty one is added by default.
The validator will block this contract definition from being created.

Similarly, this will also fail:

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "ContractDefinition",
  "@id": "myContractDefinitionId",
  "accessPolicyId": "myAccessPolicyId",
  "contractPolicyId": "myContractPolicyId",
  "assetSelector": []
}
```

The `assetSelector` property exists, but since it's an empty list the validator will also block this contract
definition from being created. A valid contract definition should have at least one criterion.

However, it's possible to bypass this behavior and force the creation of a contract definition with an
empty asset selector. For that a specific private property has to be defined, as demonstrated below.

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "ContractDefinition",
  "@id": "myContractDefinitionId",
  "accessPolicyId": "myAccessPolicyId",
  "contractPolicyId": "myContractPolicyId",
  "assetSelector": [],
  "privateProperties": {
    "allowEmpty": "assetSelector"
  }
}
```
