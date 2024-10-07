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

package org.eclipse.tractusx.edc.iam.iatp;

import org.eclipse.edc.iam.identitytrust.spi.DcpParticipantAgentServiceExtension;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.tractusx.edc.iam.iatp.identity.IatpIdentityExtractor;

import static org.eclipse.tractusx.edc.iam.iatp.IatpDefaultScopeExtension.NAME;

@Extension(NAME)
public class IatpIdentityExtension implements ServiceExtension {


    static final String NAME = "Tractusx IATP identity extension";
    private final IatpIdentityExtractor iatpIdentityExtractor = new IatpIdentityExtractor();

    @Override
    public String name() {
        return NAME;
    }


    /**
     * This provider method is mandatory, because it prevents the {@code DefaultDcpParticipantAgentServiceExtension} from being
     * registered, which would cause a race condition in the identity extractors
     */
    @Provider
    public DcpParticipantAgentServiceExtension extractor() {
        return iatpIdentityExtractor;
    }

}
