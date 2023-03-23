package org.eclipse.tractusx.edc.lifecycle;


import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

import static java.lang.String.format;
import static org.eclipse.edc.junit.testfixtures.TestUtils.tempDirectory;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.IDS_PATH;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_CONNECTOR_PATH;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_CONNECTOR_PORT;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_IDS_API;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_IDS_API_PORT;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_MANAGEMENT_PATH;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_MANAGEMENT_PORT;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_ASSET_FILE;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_CONNECTOR_PATH;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_CONNECTOR_PORT;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_IDS_API;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_IDS_API_PORT;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_MANAGEMENT_PATH;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_MANAGEMENT_PORT;


public class MultiRuntimeTest {

    public static final String SOKRATES_ASSET_PATH = format("%s/%s.txt", tempDirectory(), SOKRATES_ASSET_FILE);

    @RegisterExtension
    protected static Participant sokrates = new Participant(
            ":edc-tests:runtime",
            "SOKRATES",
            Map.of(
                    "edc.ids.id", "urn:connector:sokrates",
                    "web.http.port", String.valueOf(SOKRATES_CONNECTOR_PORT),
                    "web.http.path", SOKRATES_CONNECTOR_PATH,
                    "edc.test.asset.path", SOKRATES_ASSET_PATH,
                    "web.http.management.port", String.valueOf(SOKRATES_MANAGEMENT_PORT),
                    "web.http.management.path", SOKRATES_MANAGEMENT_PATH,
                    "web.http.ids.port", String.valueOf(SOKRATES_IDS_API_PORT),
                    "web.http.ids.path", IDS_PATH,
                    "edc.api.auth.key", "testkey",
                    "ids.webhook.address", SOKRATES_IDS_API));
    @RegisterExtension
    protected static Participant plato = new Participant(
            ":edc-tests:runtime",
            "PLATO",
            Map.of(
                    "edc.ids.id", "urn:connector:plato",
                    "web.http.default.port", String.valueOf(PLATO_CONNECTOR_PORT),
                    "web.http.default.path", PLATO_CONNECTOR_PATH,
                    "web.http.management.port", String.valueOf(PLATO_MANAGEMENT_PORT),
                    "web.http.management.path", PLATO_MANAGEMENT_PATH,
                    "web.http.ids.port", String.valueOf(PLATO_IDS_API_PORT),
                    "web.http.ids.path", IDS_PATH,
                    "edc.api.auth.key", "testkey",
                    "ids.webhook.address", PLATO_IDS_API));


}
