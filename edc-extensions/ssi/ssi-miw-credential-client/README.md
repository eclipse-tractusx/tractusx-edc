# MIW Client Credential Module

This module contains an implementation of the `SsiCredentialClient` interface for SSI.
It basically narrows down to two operations:

- obtaining a token for protocol communication
- validating the token

For validating the token accordingly to the first milestone [here](https://github.com/eclipse-tractusx/ssi-docu/tree/main/docs/architecture/cx-3-2), the implemetation
just call the MIW for checking that the token and the VP claim inside are correct. Then extract the `JWT` claims into the `ClaimToken` for further checks.

For obtaining a `JWT` token also it reaches the MIW, that will create a token with the `VP` claim inside.

This module also contains two additional validation rules of VP/VC on the provider side.

- `SsiCredentialIssuerValidationRule`    checks if the issuer of the Verifiable Credential matches `tx.ssi.miw.authority.issuer`
- `SsiCredentialSubjectIdValidationRule` checks if the issuer of the JWT/VP matches the credential subject id in the Verifiable Credential

## Configuration

| Key                              | Required | Example        | Description                       |
|----------------------------------|----------|----------------|-----------------------------------|
| tx.ssi.miw.url                   | X        |                | MIW URL                           |
| tx.ssi.miw.authority.id          | X        |                | BPN number of the authority       |
| tx.ssi.miw.authority.issuer      |          |                | The id of the issuer (DID)        |
| tx.ssi.oauth.token.url           | X        |                | Token URL (Keycloak)              |
| tx.ssi.oauth.client.id           | X        |                | Client id                         |
| tx.ssi.oauth.client.secret.alias | X        |                | Vault alias for the client secret |

By default, the `tx.ssi.miw.authority.issuer` is composed with `did:web:<tx.ssi.miw.url>:<tx.ssi.miw.authority.id>`

Another mandatory settings is `tx.ssi.endpoint.audience` which is described [here](../ssi-identity-core/README.md)

> Note: the `edc.participant.id` should match the BPN number contained in the OAuth2/Keycloak token and the one assigned by the portal to the user's organization.
