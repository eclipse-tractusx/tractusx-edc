package org.eclipse.tractusx.edc.api.observability;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.connector.spi.asset.AssetService;
import org.eclipse.edc.connector.spi.catalog.CatalogService;
import org.eclipse.edc.connector.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.connector.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.connector.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.spi.asset.DataAddressResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;

//todo: uncomment after test refactoring
//@ApiTest
@ExtendWith(EdcExtension.class)
public class TxObservabilityApiControllerTest {

    private static final String API_KEY = "12345";

    @Nested
    @DisplayName("Allow unauthenticated access")
    class AllowsUnauthenticatedContextTest extends BaseTest {

        protected AllowsUnauthenticatedContextTest() {
            super("/api/v1/observability");
        }

        @BeforeEach
        void setUp(EdcExtension extension) {
            registerServiceMocks(extension);
            var config = Map.of(
                    "web.http.management.port", String.valueOf(port + 1),
                    "web.http.management.path", "/api/v1/management",
                    "edc.api.auth.key", API_KEY,
                    TxObservabilityApiExtension.ALLOW_INSECURE_API_SETTING, "true",
                    "web.http.observability.port", String.valueOf(port),
                    "web.http.observability.path", basePath
            );
            extension.setConfiguration(config);
        }

        @ParameterizedTest
        @ValueSource(strings = { "/health", "/liveness", "/readiness", "/startup" })
        void allowUnauthenticated_verifyPath(String path) {
            baseRequest()
                    .header("x-api-key", API_KEY)
                    .get("/check" + path)
                    .then()
                    .statusCode(200)
                    .body(notNullValue());
        }
    }

    @Nested
    @DisplayName("Disallow unauthenticated access")
    class NoUnauthenticatedAccessTest extends BaseTest {

        protected NoUnauthenticatedAccessTest() {
            super("/api/v1/management");
        }

        @BeforeEach
        void setUp(EdcExtension extension) {
            registerServiceMocks(extension);
            var config = Map.of(
                    "web.http.management.port", String.valueOf(port),
                    "web.http.management.path", basePath,
                    "edc.api.auth.key", API_KEY);
            extension.setConfiguration(config);
        }

        @ParameterizedTest
        @ValueSource(strings = { "/health", "/liveness", "/readiness", "/startup" })
        void defaultContext_withAuthHeader_shouldReturn200(String path) {
            baseRequest()
                    .header("x-api-key", API_KEY)
                    .get("/check" + path)
                    .then()
                    .statusCode(200)
                    .body(notNullValue());
        }

        @ParameterizedTest
        @ValueSource(strings = { "/health", "/liveness", "/readiness", "/startup" })
        void defaultContext_whenNoAuthHeader_shouldReturn401(String path) {
            baseRequest()
                    .get("/check" + path)
                    .then()
                    .statusCode(401)
                    .body(notNullValue());
        }
    }

    // register all services that are required by the management API
    protected void registerServiceMocks(EdcExtension extension) {
        extension.registerServiceMock(DataAddressResolver.class, mock(DataAddressResolver.class));
        extension.registerServiceMock(CatalogService.class, mock(CatalogService.class));
        extension.registerServiceMock(ContractAgreementService.class, mock(ContractAgreementService.class));
        extension.registerServiceMock(ContractDefinitionService.class, mock(ContractDefinitionService.class));
        extension.registerServiceMock(AssetService.class, mock(AssetService.class));
        extension.registerServiceMock(ContractNegotiationService.class, mock(ContractNegotiationService.class));
        extension.registerServiceMock(PolicyDefinitionService.class, mock(PolicyDefinitionService.class));
        extension.registerServiceMock(TransferProcessService.class, mock(TransferProcessService.class));
    }

    static class BaseTest {
        protected final int port = getFreePort();
        protected String basePath;

        protected BaseTest(String basePath) {
            this.basePath = basePath;
        }

        protected RequestSpecification baseRequest() {
            return given()
                    .baseUri("http://localhost:" + port)
                    .basePath(basePath)
                    .when();
        }
    }

}
