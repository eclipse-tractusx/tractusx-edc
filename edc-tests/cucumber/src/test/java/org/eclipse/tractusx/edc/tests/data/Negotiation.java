package org.eclipse.tractusx.edc.tests.data;

import org.eclipse.tractusx.edc.tests.DataManagementAPI;
import org.eclipse.tractusx.edc.tests.util.Timeouts;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.stream.Stream;

import static org.awaitility.Awaitility.await;


public class Negotiation {


    private final String id;

    public Negotiation(String id) {
        this.id = Objects.requireNonNull(id);
    }

    public void waitUntilComplete(DataManagementAPI dataManagementAPI) {
        await()
                .pollDelay(Duration.ofMillis(5000))
                .atMost(Timeouts.CONTRACT_NEGOTIATION)
                .until(() -> isComplete(dataManagementAPI));
    }

    public boolean isComplete(DataManagementAPI dataManagementAPI) throws IOException {
        var negotiation = dataManagementAPI.getNegotiation(id);
        return negotiation != null
                && Stream.of(
                        ContractNegotiationState.ERROR,
                        ContractNegotiationState.CONFIRMED,
                        ContractNegotiationState.DECLINED)
                .anyMatch((l) -> l.equals(negotiation.getState()));
    }

    public String getId() {
        return id;
    }
}
