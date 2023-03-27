package org.eclipse.tractusx.edc.tests.data;

import static org.awaitility.Awaitility.await;

import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import java.io.IOException;
import java.time.Duration;
import java.util.stream.Stream;
import lombok.Value;
import org.eclipse.tractusx.edc.tests.DataManagementAPI;
import org.eclipse.tractusx.edc.tests.util.Timeouts;

@Value
public class Negotiation {

  @NotNull String id;

  public void waitUntilComplete(DataManagementAPI dataManagementAPI) {
    await()
        .pollDelay(Duration.ofMillis(2000))
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
}
