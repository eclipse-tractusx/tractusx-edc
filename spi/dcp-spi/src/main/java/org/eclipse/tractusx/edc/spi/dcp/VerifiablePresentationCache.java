package org.eclipse.tractusx.edc.spi.dcp;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;
import org.eclipse.edc.spi.result.StoreResult;

import java.util.List;

public interface VerifiablePresentationCache {

    StoreResult<Void> store(String participantContextId, String counterPartyDid, List<String> scopes, List<VerifiablePresentationContainer> presentations);

    StoreResult<List<VerifiablePresentationContainer>> query(String participantContextId, String counterPartyDid, List<String> scopes);

    StoreResult<Void> remove(String participantContextId, String counterPartyDid);
}
