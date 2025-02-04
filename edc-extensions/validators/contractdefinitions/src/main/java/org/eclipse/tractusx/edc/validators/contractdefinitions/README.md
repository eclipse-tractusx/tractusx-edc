# Contract Definitions Validator: Empty Asset Selector

The goal of this extension is to provide a replacement validator for contract definitions entities.
This validator is used to validate incoming request payloads to the contract definitions data management API endpoint.
When enabled, it prevents incoming requests that create or update contract definitions, when no asset selector is
provided.

This extension is included with the standard tractusx-edc distribution, but is disabled by default. To enable it,
you can set `tx.edc.validator.contractdefinitions.block-empty-asset-selector`to `true` in your connector configuration.
