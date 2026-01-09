package org.eclipse.tractusx.edc.discovery.v4alpha.service;

import org.eclipse.edc.protocol.dsp.spi.type.Dsp08Constants;
import org.eclipse.tractusx.edc.discovery.v4alpha.exceptions.UnexpectedResultApiException;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.DspVersionToIdentifierMapper;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;

import java.util.concurrent.CompletableFuture;

public class Dsp08ToBpnlMapper implements DspVersionToIdentifierMapper {
    private BdrsClient bdrsClient;

    public Dsp08ToBpnlMapper(BdrsClient bdrsClient) {
        this.bdrsClient = bdrsClient;
    }

    @Override
    public CompletableFuture<String> identifierForDspVersion(String did, String dspVersion) {
        switch (dspVersion) {
            case Dsp08Constants.V_08_VERSION -> extractBpnl(did);
            default -> CompletableFuture.completedFuture(did);
        }
    }

    private CompletableFuture<String> extractBpnl(String did) {
        return CompletableFuture.supplyAsync(() -> {
            var bpn = bdrsClient.resolveBpn(did);
            if (bpn != null) {
                return bpn;
            } else {
                throw new UnexpectedResultApiException(
                        "For given DID %s no BPNL found".formatted(did));
            }
        });
    }
}
