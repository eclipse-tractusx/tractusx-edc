# SSI Core Identity Service Module

This module contains an implementation of the EDC identity service for SSI.
The SsiIdentityService contains a `SsiTokenValidationService` for validating the `JWT` token,
that uses an implementation of `SsiCredentialClient` for validating the JWT token and then check custom rules registered in the `SsiValidationRuleRegistry`

For obtaining the `JWT` token, the identity service also delegate to the `SsiCredentialClient` .

The default implementation according to the first milestone [here](https://github.com/eclipse-tractusx/ssi-docu/tree/main/docs/architecture/cx-3-2)
will rely on an MIW and the implementations in available in the module `:edc-extensions:ssi:ssi-miw-credential-client`.

The implementation also provide a rule registry `SsiValidationRuleRegistry` where custom rule can be registered for validating the `ClaimToken` extracted from the `JWT` token.

Custom rule could be like:

- Audience validation
- VP/VC validation
- Expiration
- ..etc

## Configuration

| Key                                     | Required | Example        | Description                           |
|-----------------------------------------|----------|----------------|---------------------------------------|
| tx.ssi.endpoint.audience                | X        |                | Endpoint URL for audience check (DSP) |
