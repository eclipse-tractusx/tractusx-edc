# Contract Definitions Validator: Empty Asset Selector

The goal of this extension is to provide a replacement validator for contract definitions entities.
This validator is used to validate incoming request payloads to the contract definitions data management API endpoint.
When enabled, it prevents incoming requests that create or update contract definitions, when no asset selector is
provided.

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
  // Asset selector is missing
}
```

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
  // Asset selector should contain at least one valid criterion
}
```

However, it's possible to bypass this behavior and force the creation of a contract definition with an
empty asset selector. For that a specific private property as to be defined, as demonstrated below.

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
