/*
 * Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.api.bpn;

import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.tractusx.edc.validation.businesspartner.spi.event.BusinessPartnerCreated;
import org.eclipse.tractusx.edc.validation.businesspartner.spi.event.BusinessPartnerDeleted;
import org.eclipse.tractusx.edc.validation.businesspartner.spi.event.BusinessPartnerEvent;
import org.eclipse.tractusx.edc.validation.businesspartner.spi.event.BusinessPartnerUpdated;
import org.eclipse.tractusx.edc.validation.businesspartner.spi.observe.BusinessPartnerListener;

import java.time.Clock;
import java.util.List;

public class BusinessPartnerEventListener implements BusinessPartnerListener {
    private final Clock clock;
    private final EventRouter eventRouter;

    public BusinessPartnerEventListener(Clock clock, EventRouter eventRouter) {
        this.clock = clock;
        this.eventRouter = eventRouter;
    }

    @Override
    public void created(String bpn, List<String> groups) {
        var event = BusinessPartnerCreated.Builder.newInstance()
                .bpn(bpn)
                .groups(groups)
                .build();

        publish(event);
    }

    @Override
    public void deleted(String bpn) {
        var event = BusinessPartnerDeleted.Builder.newInstance()
                .bpn(bpn)
                .build();

        publish(event);
    }

    @Override
    public void updated(String bpn, List<String> groups) {
        var event = BusinessPartnerUpdated.Builder.newInstance()
                .bpn(bpn)
                .groups(groups)
                .build();

        publish(event);
    }

    private void publish(BusinessPartnerEvent event) {
        var envelope = EventEnvelope.Builder.newInstance()
                .payload(event)
                .at(clock.millis())
                .build();
        eventRouter.publish(envelope);
    }
}
