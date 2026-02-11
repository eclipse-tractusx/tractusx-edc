# Decentralized Credential Protocol (DCP) Extension

## Overview
The DCP extension provides implementations for the Decentralized Credential Protocol.
It handles credential scope extraction (from policies), Secure Token Service (STS) integration, and verifiable presentation caching.

## Modules

### tx-dcp 
Handles Identity and trust protocol (IATP) scope extraction from policies.

Provides:

- **DefaultScopeExtractor**: Adds MembershipCredential. Every interaction in the Catena-X dataspace requires proof of membership. it is always needed to present the MembershipCredential to prove you're a legitimate participant.
- **CredentialScopeExtractor**: It reads the actual policy constraints and determines which additional credentials are needed. The additional scopes hve to be supported.

#### Configuration
It's until now unknown where to set these configuration.

| Property | Description |
|---|---|
| `tx.edc.iam.iatp.default-scopes.<alias>.alias` | Scope namespace alias for the default credential scope |
| `tx.edc.iam.iatp.default-scopes.<alias>.type` | Credential type to require (e.g., `MembershipCredential`) |
| `tx.edc.iam.iatp.default-scopes.<alias>.operation` | Operation permission (e.g., `read`) |

If no default scopes are configured, built-in defaults are used (`MembershipCredential` with `read` operation).

---

### tx-dcp-sts-dim
Integrates with Decentralized Identity Management (DIM) wallet for Secure Token Service operations:

- **StsClientConfigurationExtension**: Configures OAuth2 client credentials for STS authentication. Reads client ID, secret alias, and token URL from configuration.
- **DimOauthClientExtension**: OAuth2 client with automatic token refresh.
- **RemoteTokenServiceClientExtension**: Provides `SecureTokenService` — either based on DIM if `DIM_URL` is set or standard EDC Remote STS.
- **DimSecureTokenService**: Makes `grantAccess` (credential request) or `signToken` (SI token signing) calls to DIM API 
  - grantAccess: Get a SI token that contains a specific credential types.
  - signToken: Just sign a presentation token

#### Configuration
It's until now unknown where to set these configuration.

| Property | Description |
|---|---|
| `tx.edc.iam.sts.dim.url` | DIM STS endpoint URL. If set, uses DIM wallet for token operations; otherwise falls back to standard EDC Remote STS. |
| `edc.iam.sts.oauth.token.url` | OAuth2 token endpoint URL for STS authentication |
| `edc.iam.sts.oauth.client.id` | OAuth2 client ID for STS authentication |
| `edc.iam.sts.oauth.client.secret.alias` | Vault alias for the OAuth2 client secret |


---

### verifiable-presentation-cache
Caches Verifiable Presentations (VPs) to reduce redundant credential service calls:

- **VerifiablePresentationCacheImpl**: Validates VPs before caching, checks expiry and revocation on retrieval.
- **CachePresentationRequestService**: Retrieves the cached VP if exits, otherwise check the underlying service.
- **InMemoryVerifiablePresentationCacheStore**: Default in-memory cache store.

#### Configuration (verifiable-presentation-cache)

| Property | Default | Description |
|---|---|---|
| `tx.edc.dcp.cache.enabled` | `true` | Enable or disable VP caching. Set to `false` to bypass the cache entirely. |
| `tx.edc.dcp.cache.validity.seconds` | `86400` (24 hours) | Cache entry validity duration in seconds. Entries older than this are considered expired. |


## How to Use in EDC
Add the following dependencies to your EDC control plane build:

```kotlin
implementation(project(":edc-extensions:dcp:tx-dcp"))
implementation(project(":edc-extensions:dcp:tx-dcp-sts-dim"))
implementation(project(":edc-extensions:dcp:verifiable-presentation-cache"))
```

Add the following dependencies to your EDC data plane build:
```kotlin
implementation(project(":edc-extensions:dcp:tx-dcp-sts-dim"))
```