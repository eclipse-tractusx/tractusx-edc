package org.eclipse.tractusx.edc.tests.data;

import org.eclipse.tractusx.edc.tests.DataManagementAPI;
import org.eclipse.tractusx.edc.tests.util.Timeouts;

import java.io.IOException;
import java.time.Duration;

import static org.awaitility.Awaitility.await;


public class Transfer {

    private final String id;

    public Transfer(String id) {
        this.id = id;
    }

    public void waitUntilComplete(DataManagementAPI dataManagementAPI) {
        await()
                .pollDelay(Duration.ofMillis(2000))
                .atMost(Timeouts.FILE_TRANSFER)
                .until(() -> isComplete(dataManagementAPI));
    }

    public boolean isComplete(DataManagementAPI dataManagementAPI) throws IOException {
        var transferProcess = dataManagementAPI.getTransferProcess(id);
        if (transferProcess == null) {
            return false;
        }

        var state = transferProcess.getState();

        return state == TransferProcessState.COMPLETED || state == TransferProcessState.ERROR;
    }

    public String getId() {
        return id;
    }
}
