package org.eclipse.tractusx.edc.spi.dcp;

import org.eclipse.edc.spi.result.StoreResult;

import java.util.List;

public interface VerifiablePresentationCacheStore {

    StoreResult<Void> store(VerifiablePresentationCacheEntry entry);

    StoreResult<VerifiablePresentationCacheEntry> query(String participantContextId, String counterPartyDid, List<String> scopes);

    StoreResult<Void> remove(String participantContextId, String counterPartyDid, List<String> scopes);

    StoreResult<Void> remove(String participantContextId, String counterPartyDid);

}
