/*
 * Copyright (c) 2026 Cofinity-X GmbH
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
 */

package org.eclipse.tractusx.edc.discovery.v4alpha.service;

import org.eclipse.edc.protocol.dsp.spi.type.Dsp08Constants;
import org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants;
import org.eclipse.tractusx.edc.discovery.v4alpha.exceptions.UnexpectedResultApiException;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.DspVersionToIdentifierMapper;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;

public class Dsp08ToBpnlMapper implements DspVersionToIdentifierMapper {
    private BdrsClient bdrsClient;

    public Dsp08ToBpnlMapper(BdrsClient bdrsClient) {
        this.bdrsClient = bdrsClient;
    }

    @Override
    public String identifierForDspVersion(String did, String dspVersion) {
        if (Dsp2025Constants.V_2025_1_VERSION.equals(dspVersion)) {
            return did;
        } else if (Dsp08Constants.V_08_VERSION.equals(dspVersion)) {
            var bpn = bdrsClient.resolveBpn(did);
            if (bpn != null) {
                return bpn;
            } else {
                throw new UnexpectedResultApiException("For given DID %s no BPNL found".formatted(did));
            }
        }
        throw new UnexpectedResultApiException("Given dsp version not supported: %s".formatted(dspVersion));
    }
}
