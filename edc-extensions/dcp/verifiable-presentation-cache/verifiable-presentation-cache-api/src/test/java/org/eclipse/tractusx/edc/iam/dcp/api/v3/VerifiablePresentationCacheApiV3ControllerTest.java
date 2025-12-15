package org.eclipse.tractusx.edc.iam.dcp.api.v3;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.eclipse.tractusx.edc.spi.dcp.VerifiablePresentationCache;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VerifiablePresentationCacheApiV3ControllerTest extends RestControllerTestBase {

    private final VerifiablePresentationCache cache = mock();
    private final ParticipantContext participantContext = ParticipantContext.Builder.newInstance()
            .participantContextId("participantContextId")
            .identity("participantId")
            .build();
    private final SingleParticipantContextSupplier participantContextSupplier = () -> ServiceResult.success(participantContext);
    private final String counterPartyDid = "did:web:counterparty";

    @Test
    void removeCacheEntries() {
        when(cache.remove(participantContext.getParticipantContextId(), counterPartyDid)).thenReturn(StoreResult.success());

        baseRequest()
                .delete()
                .then()
                .log().ifError()
                .statusCode(204);

        verify(cache).remove(participantContext.getParticipantContextId(), counterPartyDid);
    }

    @Test
    void removeCacheEntries_removingEntriesFails() {
        when(cache.remove(participantContext.getParticipantContextId(), counterPartyDid))
                .thenReturn(StoreResult.generalError("failed to remove entries"));

        baseRequest()
                .delete()
                .then()
                .log().ifError()
                .statusCode(500);

        verify(cache).remove(participantContext.getParticipantContextId(), counterPartyDid);
    }

    @Override
    protected Object controller() {
        return new VerifiablePresentationCacheApiV3Controller(cache, participantContextSupplier);
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port + "/v3/verifiablepresentations/cache/" + counterPartyDid + "/remove")
                .when();
    }
}
