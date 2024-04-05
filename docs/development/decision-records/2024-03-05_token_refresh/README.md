# Tractus-X Token Refresh

## Decision

Implement token refresh for the Tractus-X EDC Data Plane according to the [Tractus-X Refresh Token Grant Profile](https://github.com/eclipse-tractusx/tractusx-profiles/blob/main/tx/refresh/refresh.token.grant.profile.md).
This enables provider data planes to implement refresh tokens in an interoperable way for client applications using the
Tractus-X EDC Connector.

Note refresh token support will be done almost exclusively in Tractus-X EDC, except for a few minor updates to upstream
EDC.

## Rationale

Data providers may use short-lived tokens for HTTP client pull data transfers to enhance security. Refresh tokens allow
data access to be maintained using short-lived tokens without starting a new transfer process.

## Approach

The upstream EDC `DefaultDataPlaneAccessTokenServiceImpl` will be replaced to create a refresh token pinned to the
client's DID. This TX implementation will use the following upstream EDC enhancements:

- An upstream EDC decorator that adds the client DID as an extension property to the signaling request.
- Upgrade the `AccessTokenData` type to allow for extensible properties to enable the `AccessTokenDataStore` to persist
  the renewal token.

Tractus-X EDC will implement token refresh based on
the [Trasctus-X Refresh Token Grant Profile](https://github.com/eclipse-tractusx/tractusx-profiles/blob/main/tx/refresh/refresh.token.grant.profile.md)

### Provider Data Plane

#### TX DataPlaneAccessTokenService

An implementation of the `DataPlaneAccessTokenService` will be provided that overrides the
default `DefaultDataPlaneAccessTokenServiceImpl` implementation. The TX implementation will do the following:

- Create an expiring access token and set the `expiresIn` attribute of the `DataAddress.`
- Create a refresh token that is returned in the `refreshToken` property of the `DataAddress`.
- Provide a configuration option to set the `refreshEndpoint` property of the `DataAddress.` If not set,
  the `refreshEndpoint` property will not be included.

It will be possible for distributions not to include the TX implementation of the `DataPlaneAccessTokenService` and
substitute another or revert to the EDC default implementation.

The refresh token will be sender-constrained to the client DID passed as part of the `DataFlowStartMessage`.

#### OAuth 2 Refresh API

The provider data plane will be extended to add a public API that implements the OAuth2 `refresh_token` grant. The
refresh token will be verified according to the steps laid out in the _Tractus-X Refresh Token Grant Profile._ When a
refresh request is successfully made, a new access token and refresh token will be generated and returned to the client.
If a refresh token request fails due to a validation error, the associated access token and refresh token will be
invalidated.

### Client-side Token Refresh

A client will be able to initiate a token refresh request in three ways:

- Request data through a client data plane instance, which will check the token expiration and issue a refresh request
  if required. This behavior will be transparent to the client.
- Request data directly and check if the access token is expired. If the token is expired, the client can call a client
  data plane management API to renew the access token.
- Request data directly and handle access token renewal using OAuth 2 directly.

To support refresh token renewal, a DIM specific extension will be added that requests the authentication JWT to be
signed using a key managed by DIM. This extension can use the same DIM API as the IATP extension.  

