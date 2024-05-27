/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.lifecycle;

import org.eclipse.tractusx.edc.tests.participant.TractusxIatpParticipantBase;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Extension of {@link TractusxIatpParticipantBase} with DIM specific configuration
 */
public class DimParticipant extends TractusxIatpParticipantBase {

    protected URI dimUri;

    protected URI bdrsUri;

    @Override
    public Map<String, String> iatpConfiguration(TractusxIatpParticipantBase... others) {
        var config = new HashMap<>(super.iatpConfiguration(others));
        config.put("tx.edc.iam.sts.dim.url", dimUri.toString());
        config.put("tx.edc.iam.iatp.bdrs.server.url", bdrsUri.toString());
        config.put("edc.transfer.proxy.token.verifier.publickey.alias", getKeyId());
        return config;
    }

    public static class Builder extends TractusxIatpParticipantBase.Builder<DimParticipant, Builder> {

        protected Builder() {
            super(new DimParticipant());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder dimUri(URI dimUri) {
            participant.dimUri = dimUri;
            return self();
        }

        public Builder bdrsUri(URI bdrsUri) {
            participant.bdrsUri = bdrsUri;
            return self();
        }

        @Override
        public DimParticipant build() {
            super.build();
            Objects.requireNonNull(participant.dimUri, "DIM URI should not be null");
            Objects.requireNonNull(participant.bdrsUri, "BDRS URI should not be null");
            return participant;
        }
    }
}
