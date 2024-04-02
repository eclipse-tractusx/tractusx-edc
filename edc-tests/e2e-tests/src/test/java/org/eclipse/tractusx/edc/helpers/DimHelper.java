/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.helpers;

import org.eclipse.tractusx.edc.lifecycle.DimParticipant;

import java.net.URI;
import java.util.Objects;

import static java.lang.String.format;
import static java.lang.System.getenv;

public interface DimHelper {

    /**
     * Configure a {@link DimParticipant} from env variables
     *
     * @param name The participant name
     * @return The composed {@link DimParticipant}
     */
    static DimParticipant configureParticipant(String name, String bdrsUrl) {
        var bpn = getEnv(format("DIM_%s_BPN", name));
        var dimUrl = getEnv(format("DIM_%s_DIM_URL", name));
        var stsUrl = getEnv(format("DIM_%s_STS_URL", name));
        var stsClientId = getEnv(format("DIM_%s_STS_CLIENT_ID", name));
        var stsClientSecret = getEnv(format("DIM_%s_STS_CLIENT_SECRET", name));
        var did = getEnv(format("DIM_%s_DID", name));
        var trustedIssuer = getEnv("DIM_TRUSTED_ISSUER");
        return DimParticipant.Builder.newInstance().id(bpn)
                .name(name)
                .stsUri(URI.create(stsUrl))
                .stsClientId(stsClientId)
                .stsClientSecret(stsClientSecret)
                .dimUri(URI.create(dimUrl))
                .trustedIssuer(trustedIssuer)
                .did(did)
                .bdrsUri(URI.create(bdrsUrl))
                .build();
    }

    private static String getEnv(String env) {
        return Objects.requireNonNull(getenv(env), "%s env variable not present".formatted(env));
    }

}
