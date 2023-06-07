# MIW Client Credential Module

This module contains an implementation of the `SsiCredentialClient` interface for SSI.
It basically narrow down to two operations:

- validating the token
- Obtaining a token for protocol communication

For validating the token accordingly to the first milestone [here](https://github.com/eclipse-tractusx/ssi-docu/tree/main/docs/architecture/cx-3-2), the implemetation
just call the MIW for checking that the token and the VP claim inside are correct. Then extract the `JWT` claims into the `ClaimToken` for further checks.

For obtaining a `JWT` token also it reaches the MIW, that will create a token with the `VP` claim inside.

The MIW interaction in this first implementation it's still WIP, since the MIW interface it's not stable or complete yet.
