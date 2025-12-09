package org.eclipse.tractusx.edc.spi.dcp;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;

import java.time.Instant;
import java.util.List;

public class VerifiablePresentationCacheEntry {

    private final String participantContextId;
    private final String counterPartyDid;
    private final List<String> scopes;
    private final List<VerifiablePresentationContainer> presentations;
    private final Instant cachedAt;

    public VerifiablePresentationCacheEntry(String participantContextId, String counterPartyDid, List<String> scopes,
                                            List<VerifiablePresentationContainer> presentations, Instant cachedAt) {
        this.participantContextId = participantContextId;
        this.counterPartyDid = counterPartyDid;
        this.scopes = scopes;
        this.presentations = presentations;
        this.cachedAt = cachedAt;
    }

    public String getParticipantContextId() {
        return participantContextId;
    }

    public String getCounterPartyDid() {
        return counterPartyDid;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public Instant getCachedAt() {
        return cachedAt;
    }

    public List<VerifiablePresentationContainer> getPresentations() {
        return presentations;
    }

}
