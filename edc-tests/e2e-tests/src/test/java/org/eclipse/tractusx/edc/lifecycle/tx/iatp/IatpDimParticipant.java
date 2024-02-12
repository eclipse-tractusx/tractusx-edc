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

package org.eclipse.tractusx.edc.lifecycle.tx.iatp;

import org.eclipse.tractusx.edc.lifecycle.tx.TxParticipant;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Extension of {@link IatpParticipant} with DIM specific configuration
 */
public class IatpDimParticipant extends IatpParticipant {
    private final URI dimUri;

    public IatpDimParticipant(TxParticipant participant, URI stsUri, URI dimUri) {
        super(participant, stsUri);
        this.dimUri = dimUri;
    }

    @Override
    public Map<String, String> iatpConfiguration(TxParticipant... others) {
        var config = new HashMap<>(super.iatpConfiguration(others));
        config.put("edc.iam.sts.dim.url", dimUri.toString());
        return config;
    }
}
