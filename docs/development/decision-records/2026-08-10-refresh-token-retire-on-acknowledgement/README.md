# Retire Refresh Tokens on Acknowledgement

## Decision

The provider data plane will retire a rotated refresh token only once the consumer has proven that it received the
replacement. The proof of receipt is the consumer presenting the new refresh token. Until that happens, the token it
replaced remains acceptable, and presenting it again returns the current refresh token — the one the consumer failed to
receive — instead of rotating a second time. The access token is issued fresh on both paths.

This is a behavioural change with no configuration surface and no way to opt out. There is nothing worth switching off:
the old behaviour is the one that breaks transfers on a network timeout, and the new one costs one extra string in the
vault record. The request and response formats are unchanged, so conforming consumers see no difference either way.

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
restarting the transfer process — for a failure mode that is a plain network timeout.

The observation that resolves this is that **the acknowledgement already exists in the protocol**. A consumer that
presents the new refresh token can only have obtained it from the response it is confirming. No extra message, no
profile change, and no timer is needed — the provider simply defers retirement until it sees that evidence.

The refresh token is sender-constrained: holding one does not authorise a refresh. Every request must also carry an
authentication token signed with the key behind the consumer's DID and issued by the participant the EDR was issued to
(`AuthTokenAudienceRule`), so an intercepted or leaked refresh token is unusable on its own, however often it is
replayed. Retention changes how long the consumer's own copy stays valid, not who is able to use it.

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

The record holds refresh tokens only. The access token is never stored — every call mints a new one, whether it rotates
the refresh token or serves a repeat, so a consumer retrying late still gets a usable access token.

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

**Security.** The superseded token stays usable for one extra generation, unbounded in time. That widens an existing
window: a captured *complete* refresh request can be replayed for a generation longer, and without the 401 collision
that makes such an intrusion visible. The authentication token must therefore carry an `exp` claim and is validated
against it.

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

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- SPDX-License-Identifier: CC-BY-4.0
- SPDX-FileCopyrightText: 2026 Cofinity-X GmbH
- Source URL: [https://github.com/eclipse-tractusx/tractusx-edc](https://github.com/eclipse-tractusx/tractusx-edc)