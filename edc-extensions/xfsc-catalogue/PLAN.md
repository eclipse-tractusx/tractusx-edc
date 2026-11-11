# Plan

Build [README.md](README.md) in three repos. Do not invent a credential type. The asset VC uses a type the [catalogue](https://github.com/project-construct-x/federated-catalogue) already ingests (`ServiceOffering`, `DataProduct`, `LegalPerson`, …). `MembershipCredential` stays the access credential and is not re-issued here.

Wallet image `ghcr.io/project-construct-x/wallet` (`launcher/con-x-wallet`). Helm splits it: `charts/wallet` is the holder, `charts/issuer-wallet` is the issuer.

| API | Default | Used for |
| --- | --- | --- |
| STS | `:9292/api/sts` | Self-Issued ID Tokens. The connector already calls this through `SecureTokenService` |
| Issuance | `:13132/api/issuance` | `POST /v1alpha/participants/{id}/credentials`, holder token, audience = issuer |
| Issuer admin | `:15152/api/issuer` | `POST /v1alpha/participants/{id}/attestations` and `…/credentialdefinitions`. `X-Api-Key`, not public |
| Credential Service | `:13131/api/credentials` | `POST /v1/participants/{id}/presentations/query`. URL comes from the holder DID document, not from the notification |

## 1. IdentityHub

`AttestationContext` only carries the holder, the participant context, and claim tokens. `extensions/con-x/dev-attestation` returns a static map per holder DID, so it cannot sign a different asset each time.

Add a `catalogue-asset` attestation source. The connector registers one claim set per issuance (holder DID + credential id `cx-asset-<assetId>`). `execute` returns that set once, under the mapping input `content`. A missing or already-consumed registration fails issuance.

One credential definition, not one per asset:

- `credentialType` = the asset's real type (`ServiceOffering`, `DataProduct`, `LegalPerson`, …)
- `format` = `VC1_0_JWT` (the presentation generator already packages this; the catalogue accepts it as `application/vc+jwt`). A Gaia-X 2511 type still needs the `https://w3id.org/gaia-x/2511#` context in the signed payload
- `issuer` and `credentialSubject.id` = holder `did:web` (`edc.iam.issuer.id`)
- subject claims = the catalogue fields for that type. No `edc:` private properties, no secret aliases
- mapping `content` → `credentialSubject`, as the dev source does

Replace `EdcScopeToCriterionTransformer`. It only accepts `org.eclipse.edc.vc.type:<Type>:<read|*|all>` and matches every credential of that type. The [DCP spec](https://eclipse-dataspace-dcp.github.io/decentralized-claims-protocol/v1.0.1/) uses two segments and no operation suffix: `org.eclipse.dspace.dcp.vc.type:<Type>` and `org.eclipse.dspace.dcp.vc.id:<id>`. The new transformer must accept those, and map the id alias onto the credential resource id so a publication query returns one VC. Keep the old three-segment alias working for membership.

`PresentationApiController` reads scopes from the **verifier** Self-Issued ID Token and rejects the query when the requested scope is outside that token. Allow the catalogue DID to `read` the asset type. A request that sets `presentationDefinition` stays "not implemented"; the catalogue must not send one.

## 2. Federated Catalogue

`DcpMachineAuthenticationFilter` already checks a client Self-Issued ID Token (`iss` = `sub`, `capabilityInvocation` key, audience, expiry, zero clock skew) and consumes `(issuer, jti)` in `dcp_used_si_tokens` before calling the Credential Service. `DcpVerifierService.pullForAuthentication` then loads the stored query for purpose `POST_ASSETS`: exactly one `MembershipCredential` from the trusted issuer, subject = holder.

Add `POST /publications` on that same machine chain. Body is `{ publicationId, assetId, credentialId, credentialType, scope, callbackUrl }`. It is not a credential. `POST /assets` stays as it is: its body is the asset, and a `PresentationResponseMessage` there is rejected.

After membership succeeds, pull a second time with purpose `PUBLISH_ASSET` and scope `org.eclipse.dspace.dcp.vc.id:<credentialId>`. `VerifierSiTokenFactory` must put that same scope on the catalogue's own token. Ignore a scope in the body that is not this id or the configured type. Take the single matching JWT from the presentation, require `issuer` = membership subject, and pass it to the existing ingest pipeline as `application/vc+jwt` (Postgres document, then Fuseki claims).

`POST` `{ publicationId, assetId, credentialId, status, catalogueAssetId, reason }` to `callbackUrl` with a catalogue Self-Issued ID Token, audience = connector DID. `status` is `STORED` or `REJECTED`. Remember `publicationId` so a replay returns the stored result instead of ingesting again. The holder token on the wire is still single-use.

## 3. This extension

Module `:edc-extensions:xfsc-catalogue`, registered in `settings.gradle.kts` and depended on by `con-x-controlplane-postgresql-hashicorp-vault`. `ServiceExtension` with `@Setting` / `@Inject`, HTTP via `EdcHttpClient`, JSON via `TypeManager`, tokens via `SecureTokenService`.

On the asset-created event, and only when `cx.catalogue.publish=true`, run off the event thread:

1. Map public asset properties to the credential subject. Write a `publicationId` in state `REQUESTED`.
2. `POST` the claims to the issuer-admin registration. The API key comes from the vault alias and is not logged.
3. `createToken` with audience = issuer DID. `POST` the issuance request. Poll until the credential is in the holder store or the attempt times out.
4. `createToken` again with audience = `cx.edc.xfsc.catalogue.audience` (same value as `FEDERATED_CATALOGUE_DCP_AUDIENCE`) and a new `jti`. `POST /publications`. On HTTP 401, mint another token and retry once.
5. The callback checks the catalogue token (`iss` = catalogue verifier DID, `aud` = connector DID) and sets `STORED` or `REJECTED`.

Settings: `cx.edc.xfsc.catalogue.enabled` (default `false`), `url`, `audience`, `callback.url`, `issuer.issuance.url`, `issuer.admin.url`, `issuer.api.key.alias`, `participant.context.id`.

Done when one flagged asset is one credential of its real type in the wallet, one catalogue asset of that same type, and `STORED` on the connector. A second asset of the same type must not come back in the first asset's presentation query.
