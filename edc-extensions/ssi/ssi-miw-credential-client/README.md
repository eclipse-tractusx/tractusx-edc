# MIW Client Credential Module

This module contains an implementation of the `SsiCredentialClient` interface for SSI.
It basically narrow down to two operations:

- obtaining a token for protocol communication
- validating the token

For validating the token accordingly to the first milestone [here](https://github.com/eclipse-tractusx/ssi-docu/tree/main/docs/architecture/cx-3-2), the implemetation
just call the MIW for checking that the token and the VP claim inside are correct. Then extract the `JWT` claims into the `ClaimToken` for further checks.

For obtaining a `JWT` token also it reaches the MIW, that will create a token with the `VP` claim inside.

## Configuration

| Key                                     | Required | Example        | Description                       |
|-----------------------------------------|----------|----------------|-----------------------------------|
| tx.ssi.miw.url                          | X        |                | MIW URL                           |
| tx.ssi.miw.authority.id                 | X        |                | BPN number of the authority       |
| tx.ssi.oauth.token.url                  | X        |                | Token URL (Keycloak)              |
| tx.ssi.oauth.client.id                  | X        |                | Client id                         |
| tx.ssi.oauth.client.secret.alias        | X        |                | Vault alias for the client secret |
