# Verifiable Presentation Caching

## Decision

We will implement a caching mechanism for Verifiable Presentations (VPs) within the DCP communication flow. Each
requested VP will be cached and before any new presentation request is made, the cache is checked for a matching VP
first.

## Rationale

For each DSP message exchanged, the receiving connector requests a VP of the sending participant. This includes multiple
requests to the wallet (sending participant's STS, receiving participant's STS, sending participant's presentation API).
This causes quite high network traffic, as e.g. during a contract negotiation at least 4 DSP messages are exchanged,
i.e. the whole request sequence will be run at least 4 times during a single contract negotiation.

As available Verifiable Credentials (VCs) do not frequently change, part of the request sequence can be omitted after
the whole sequence has been executed once by introducing a cache for VPs. The initial call to the sending participant's
STS always needs to be made, as the receiving participant may not have any VPs cached. But after initially requesting a
VP for a participant, the requests to the receiving participant's STS as well as to the sending participant's
presentation API can be skipped for subsequent DSP messages exchanged with the same participant, thus greatly reducing
network traffic.

## Approach

### VerifiablePresentationCache

First, we need to define an interface for the cache. It will provide methods for storing a new entry, retrieving an
entry and removing entries for a participant. As each VP is requested for a specific participant and specific scopes,
both `counterPartyDid` and `scopes` need to be used for storing and retrieving entries. As starting from EDC version
`0.15.0` the `participantContextId` is passed to the `DcpIdentityService` and `PresentationRequestService`, this should
also be included in the cache.

```java
public interface VerifiablePresentationCache {
    
    StoreResult<Void> store(String participantContextId, String counterPartyDid, List<String> scopes, List<VerifiablePresentationContainer> presentations);
    
    StoreResult<List<VerifiablePresentationContainer>> query(String participantContextId, String counterPartyDid, List<String> scopes);
    
    StoreResult<Void> remove(String participantContextId, String counterPartyDid);
}
```

The interface will be located in a new, dedicated spi module `dcp-spi`. Additionally, a model class
`VerifiablePresentationCacheEntry` will be added to this spi module, which encapsulates all values to be cached as well
as the timestamp at which the cache entry was created. To decouple common cache behavior from the underlying persistence
layer, we will create a second interface `VerifiablePresentationCacheStore`, which provides similar methods to the
cache, but uses the `VerifiablePresentationCacheEntry` as parameter/return value for storing/retrieving, and
additionally provides a second `remove` method to remove a single entry by participant ID and scopes.

#### VerifiablePresentationCacheImpl

The `VerifiablePresentationCacheImpl` will wrap the `VerifiablePresentationCacheStore` with common cache behaviour,
like checking entries for expiry before returning them. To ensure that no invalid VPs are returned from the cache,
i.e. no expired or revoked VCs or VCs with invalid issuers, as these would cause a validation failure later on,
the `VerifiablePresentationCacheImpl` will utilize the `VerifiableCredentialValidationService`. This will lead to
duplication of some checks, as they will be run once within the cache implementation and once later on in the
`DcpIdentityService`, but as all checks executed in the `VerifiableCredentialValidationService` are lightweight, this
should not be an issue.

```java
public class VerifiablePresentationCacheImpl implements VerifiablePresentationCache {
    
    // ...

    public StoreResult<Void> store(String participantContextId, String counterPartyDid, List<String> scopes,
                                   List<VerifiablePresentationContainer> presentations) {
        var entry = new VerifiablePresentationCacheEntry(participantContextId, counterPartyDid, scopes, presentations, Instant.now(clock));
        return store.store(entry);
    }

    public StoreResult<List<VerifiablePresentationContainer>> query(String participantContextId, String counterPartyDid, List<String> scopes) {
        var cacheResult = store.query(participantContextId, counterPartyDid, scopes);

        if (cacheResult.failed()) {
            return StoreResult.notFound("No cached entry found for given participant and scopes.");
        }

        if (isExpired(cacheResult.getContent()) || !areCredentialsValid(cacheResult.getContent().getPresentations(), participantContextId)) {
            store.remove(participantContextId, counterPartyDid, scopes);
            return StoreResult.notFound("No cached entry found for given participant and scopes.");
        }

        return cacheResult.map(VerifiablePresentationCacheEntry::getPresentations);
    }

    @Override
    public StoreResult<Void> remove(String participantContextId, String counterPartyDid) {
        return store.remove(participantContextId, counterPartyDid);
    }

    private boolean isExpired(VerifiablePresentationCacheEntry entry) {
        // ...
    }

    private boolean areCredentialsValid(List<VerifiablePresentationContainer> presentations, String participantContextId) {
        // ...
    }
}
```

The `VerifiablePresentationCacheImpl` will be added as part of the new module `verifiable-presentation-cache` located
in the `dcp` super-module.

#### VerifiablePresentationCacheStore Implementations

The default implementation of the `VerifiablePresentationCacheStore` will be an in-memory implementation. But as
EDCs may be downscaled when no processes are running, different implementations using SQL-based persistence or utilizing
external cache solutions like Redis may be beneficial, to not lose the benefits of caching VPs in scenarios where
EDCs are frequently downscaled.

### CachePresentationRequestService

To include the cache in the DCP flow, a custom implementation of `PresentationRequestService` needs to be provided.
This service encapsulates the steps of creating an SI token for the receiving participant and requesting the sending
participant's VP. The `DefaultPresentationRequestService` is available in the `dcp-lib` module and will be extended by
the custom implementation as to not duplicate the existing code. The custom implementation will wrap the existing code
with calls to the cache:

```java
public class CachePresentationRequestService extends DefaultPresentationRequestService {
    
    private final VerifiablePresentationCache cache;
    
    // ...

    @Override
    public Result<List<VerifiablePresentationContainer>> requestPresentation(String participantContextId, String ownDid,
                                                                             String counterPartyDid, String counterPartyToken,
                                                                             List<String> scopes) {
        var cacheResult = cache.query(participantContextId, counterPartyDid, scopes);
        if (cacheResult.succeeded()) {
            return Result.success(cacheResult.getContent());
        }

        var vpResult = super.requestPresentation(participantContextId, ownDid, counterPartyDid, counterPartyToken, scopes);

        if (vpResult.succeeded()) {
            cache.store(participantContextId, counterPartyDid, scopes, vpResult.getContent());
        }

        return vpResult;
    }
}
```

The `CachePresentationRequestService` will also be added in the new module `verifiable-presentation-cache`.

### Cache Invalidation API

Even though the VCs are checked also within the cache, there may be situations where an invalid VP is cached, e.g.
when a VC defined in the requested scopes was initially missing and shortly after added to the wallet. To not block
communication in these situations, there needs to be a way to trigger removal of cache entries. For this purpose,
we'll add an API, which will comprise a single endpoint which takes a participant ID as parameter and removes all cache
entries for that participant when called.

### Configuration

The cache will be enabled by default and have a default validity period of 24 hours. There will be settings for both
disabling the cache and configuring the validity period.
