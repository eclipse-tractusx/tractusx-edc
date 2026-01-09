package org.eclipse.tractusx.edc.discovery.v4alpha.spi;

import java.util.concurrent.CompletableFuture;

public interface DspVersionToIdentifierMapper {
    default CompletableFuture<String> identifierForDspVersion(String did, String dspVersion) {
        return CompletableFuture.completedFuture(did);
    }
}
