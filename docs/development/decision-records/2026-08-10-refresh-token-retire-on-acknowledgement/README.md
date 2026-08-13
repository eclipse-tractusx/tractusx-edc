# Retire Refresh Tokens on Acknowledgement

## Decision

The provider data plane will retire a rotated refresh token only once the consumer has proven that it received the
replacement. The proof of receipt is the consumer presenting the new refresh token. Until that happens, the token it
replaced remains acceptable, and presenting it again returns the current refresh token — the one the consumer failed to
receive — instead of rotating a second time. The access token is issued fresh on both paths.

This is a behavioural change with no configuration surface and no way to opt out.

## Rationale

Token refresh as specified in the
[Tractus-X Refresh Token Grant Profile](https://github.com/eclipse-tractusx/tractusx-profiles/blob/main/tx/refresh/refresh.token.grant.profile.md)
and implemented per [2024-03-05_token_refresh](../2024-03-05_token_refresh/README.md) rotates the refresh token on every
successful call. The implementation treated the moment of *issuing* a new token as the moment the old one dies: the
vault entry was overwritten and the previous token was rejected from then on.

That is only correct if the response always reaches the consumer, which is not a property an HTTP call has. Observed in
production: a refresh takes longer than a proxy or ingress in front of the public API tolerates, the request is answered
with HTTP 504, or the consumer cancels and the access log shows 499. The provider has rotated; the consumer still holds
the old refresh token; it never saw the new one. Every subsequent refresh is answered with HTTP 401 because the token
presented no longer matches the stored one. The transfer is permanently broken and can only be recovered by
renegotiating the contract — for a failure mode that is a plain network timeout.

The observation that resolves this is that **the acknowledgement already exists in the protocol**. A consumer that
presents the new refresh token can only have obtained it from the response it is confirming. No extra message, no
profile change, and no timer is needed — the provider simply defers retirement until it sees that evidence.

## Approach

### Retained state

`RefreshToken`, the record persisted in the vault under the access token's ID, gains a single component:

```java
public record RefreshToken(String refreshToken, Long expiresIn, String refreshEndpoint,
                           @Nullable String previousRefreshToken) {
}
```

`previousRefreshToken` is the token that `refreshToken` replaced. Entries written by earlier versions deserialize to
`null` there and are treated as having no predecessor, so no migration of existing vault entries is required. The record
additionally ignores unknown properties, so a further component could be added without breaking records already stored.

Only the refresh token is retained; the access token is issued fresh on both paths. `resolve()` validates an access
token against its own claims and the existence of the `AccessTokenData` entry rather than against a stored string, and
`DataPlaneAuthorizationServiceImpl` sets no `exp` claim, so every issue — including one serving a repeat — gets the full
configured lifetime. Issuing it fresh keeps a live bearer credential out of the vault, keeps the record to a single
added component, and hands the consumer a usable access token however late it retries.

At most two generations of refresh token are live at any time, and the older one is retired by the very request that
proves it is no longer needed.

### Refresh flow

`RefreshTokenValidationRule` keeps its place in the access token's validation chain and its single vault read, and is
widened to accept the superseded token in addition to the current one. Deciding between the two outcomes needs to know
*which* token matched, which a `Result<Void>` cannot express, so the rule exposes the resolved record through
`replayedToken()` — non-null only in the superseded case. Instances are already created per refresh request, so this
stays confined to one request.

`DataPlaneTokenRefreshServiceImpl.refreshToken()` then branches on that single value when deciding what to pair the
newly issued access token with:

- `replayedToken()` is non-null — the consumer never saw the response that rotated its token, so it is given the current
  refresh token from the stored record. Nothing is rotated and nothing is written. Rotating here would be worse than
  useless: the provider cannot tell which of the two tokens the consumer ended up with, so it would strand the consumer
  for good.
- otherwise — the presented token is the current one, which is the acknowledgement. A new refresh token is issued and
  stored, with the presented token recorded as the new `previousRefreshToken`.

A token matching neither is still rejected by the rule, with the message unchanged, so client-visible behaviour and the
existing end-to-end assertions are unaffected.

## Further considerations

**Security.** The superseded token stays usable for one extra generation, unbounded in time. This does not widen the
attack surface: a refresh request must also carry an authentication token signed with the consumer's DID key, so an
attacker able to use the old token could equally use the current one. Revocation is unaffected — it deletes the vault
entry and the access token data, dropping both generations at once.

**Standards conformance.** The behaviour stays within [RFC 6749](https://www.rfc-editor.org/rfc/rfc6749). Section 6
makes both halves of rotation optional — "The authorization server MAY issue a new refresh token […] The authorization
server MAY revoke the old refresh token after issuing a new refresh token to the client." A new refresh token is issued
and only the optional revocation is deferred, until the client has demonstrably received it, which is also what that
sentence's "to the client" describes. Its three MUSTs are unaffected: client authentication is required, the refresh
token is checked to have been issued to the authenticated client (`AuthTokenAudienceRule` together with the DID
signature on the authentication token), and the refresh token is validated. Section 10.4 holds as well — both
generations stay confidential in the vault, both are bound to the same client through the shared `AccessTokenData`, and
refresh tokens remain unguessable provider-signed JWTs. What is given up is the detection property of
strict single-use rotation: the first appearance of a superseded token no longer signals a possible compromise, because
it is indistinguishable from the legitimate retry this decision exists to support.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2026 Cofinity-X GmbH
- SPDX-FileCopyrightText: 2026 Contributors to the Eclipse Foundation
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
