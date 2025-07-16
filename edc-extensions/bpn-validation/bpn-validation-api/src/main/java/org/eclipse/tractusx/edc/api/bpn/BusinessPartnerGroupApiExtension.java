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

package org.eclipse.tractusx.edc.api.bpn;

import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;
import org.eclipse.tractusx.edc.api.bpn.v1.BusinessPartnerGroupApiV1Controller;
import org.eclipse.tractusx.edc.api.bpn.v3.BusinessPartnerGroupApiV3Controller;
import org.eclipse.tractusx.edc.validation.businesspartner.spi.observe.BusinessPartnerObservableImpl;
import org.eclipse.tractusx.edc.validation.businesspartner.spi.store.BusinessPartnerStore;

import java.time.Clock;

import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_NAMESPACE;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_PREFIX;

@Extension(value = "Registers the Business Partner Group API")
public class BusinessPartnerGroupApiExtension implements ServiceExtension {

    @Inject
    private WebService webService;
    @Inject
    private JsonLd jsonLdService;
    @Inject
    private BusinessPartnerStore businessPartnerStore;

    @Inject
    private Clock clock;
    @Inject
    private EventRouter eventRouter;

    @Override
    public void initialize(ServiceExtensionContext context) {
        jsonLdService.registerNamespace(TX_PREFIX, TX_NAMESPACE);

        var businessPartnerObservable = new BusinessPartnerObservableImpl();
        businessPartnerObservable.registerListener(new BusinessPartnerEventListener(clock, eventRouter));

        webService.registerResource(ApiContext.MANAGEMENT, new BusinessPartnerGroupApiV1Controller(
                businessPartnerStore, businessPartnerObservable, context.getMonitor()
        ));
        webService.registerResource(ApiContext.MANAGEMENT, new BusinessPartnerGroupApiV3Controller(
                businessPartnerStore, businessPartnerObservable
        ));
    }
}
