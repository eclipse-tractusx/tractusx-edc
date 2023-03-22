package org.eclipse.tractusx.edc.tests;

import java.util.concurrent.TimeUnit;

import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;

public class TestRuntimeConfiguration {


    public static final String IDS_PATH = "/api/v1/ids";


    public static final int PLATO_CONNECTOR_PORT = getFreePort();
    public static final int PLATO_MANAGEMENT_PORT = getFreePort();
    public static final String PLATO_CONNECTOR_PATH = "/api";
    public static final String PLATO_MANAGEMENT_PATH = "/api/v1/management";
    public static final String CONSUMER_CONNECTOR_MANAGEMENT_URL = "http://localhost:" + PLATO_MANAGEMENT_PORT + PLATO_MANAGEMENT_PATH;
    public static final int PLATO_IDS_API_PORT = getFreePort();
    public static final String PLATO_IDS_API = "http://localhost:" + PLATO_IDS_API_PORT;

    public static final int SOKRATES_CONNECTOR_PORT = getFreePort();
    public static final int SOKRATES_MANAGEMENT_PORT = getFreePort();
    public static final String SOKRATES_CONNECTOR_PATH = "/api";
    public static final String SOKRATES_MANAGEMENT_PATH = "/api/v1/management";
    public static final int SOKRATES_IDS_API_PORT = getFreePort();
    public static final String SOKRATES_IDS_API = "http://localhost:" + SOKRATES_IDS_API_PORT;

    public static final String PROVIDER_IDS_API_DATA = "http://localhost:" + SOKRATES_IDS_API_PORT + IDS_PATH + "/data";

    public static final String PROVIDER_ASSET_ID = "test-document";
    public static final long CONTRACT_VALIDITY = TimeUnit.HOURS.toSeconds(1);

    public static final String SOKRATES_ASSET_FILE = "text-document.txt";

    public static final String PROVIDER_CONNECTOR_MANAGEMENT_URL = "http://localhost:" + SOKRATES_MANAGEMENT_PORT + SOKRATES_MANAGEMENT_PATH;


}
