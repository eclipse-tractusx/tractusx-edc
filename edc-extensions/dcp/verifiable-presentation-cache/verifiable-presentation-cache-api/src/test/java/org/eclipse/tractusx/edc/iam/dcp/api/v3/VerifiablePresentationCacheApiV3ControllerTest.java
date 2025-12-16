/********************************************************************************
 * Copyright (c) 2025 Cofinity-X GmbH
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

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
