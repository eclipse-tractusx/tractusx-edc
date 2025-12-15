package org.eclipse.tractusx.edc.iam.dcp.api.v3;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.tractusx.edc.spi.dcp.VerifiablePresentationCache;

import static java.lang.String.format;

@Path("/v3/verifiablepresentations/cache")
public class VerifiablePresentationCacheApiV3Controller implements VerifiablePresentationCacheApiV3 {

    private final VerifiablePresentationCache cache;
    private final SingleParticipantContextSupplier participantContextSupplier;

    public VerifiablePresentationCacheApiV3Controller(VerifiablePresentationCache cache, SingleParticipantContextSupplier participantContextSupplier) {
        this.cache = cache;
        this.participantContextSupplier = participantContextSupplier;
    }

    @DELETE
    @Path("{counterPartyDid}/remove")
    @Override
    public void removeCacheEntries(@PathParam("counterPartyDid") String participantId) {
        var participantContext = participantContextSupplier.get()
                .orElseThrow(ignore -> new EdcException("Failed to resolve participant context."));

        cache.remove(participantContext.getParticipantContextId(), participantId)
                .orElseThrow(result -> new EdcException(format("Failed to remove entries from cache: %s.", result.getFailureDetail())));
    }
}
