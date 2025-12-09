package org.eclipse.tractusx.edc.iam.dcp.api.v3;

import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.tractusx.edc.spi.dcp.VerifiablePresentationCache;

import static java.lang.String.format;

public class VerifiablePresentationCacheApiController implements VerifiablePresentationCacheApi {

    private final VerifiablePresentationCache cache;
    private final SingleParticipantContextSupplier participantContextSupplier;

    public VerifiablePresentationCacheApiController(VerifiablePresentationCache cache,  SingleParticipantContextSupplier participantContextSupplier) {
        this.cache = cache;
        this.participantContextSupplier = participantContextSupplier;
    }

    @Override
    public void removeCacheEntries(String participantId) {
        var participantContext = participantContextSupplier.get()
                .orElseThrow(ignore -> new EdcException("Failed to resolve participant context."));

        cache.remove(participantContext.getParticipantContextId(), participantId)
                .orElseThrow(result -> new EdcException(format("Failed to remove entries from cache: %s.", result.getFailureDetail())));
    }

}
