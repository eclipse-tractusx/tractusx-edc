/*
 * Copyright (c) 2025 Schaeffler AG
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

package org.eclipse.tractusx.edc.eventsubscriber;

import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import static org.eclipse.tractusx.edc.eventsubscriber.EventSubscriberExtension.NAME;

@Extension(value = NAME)
public class EventSubscriberExtension implements ServiceExtension {

    public static final String NAME = "Tractus-X Event Subscriber Extension";
    @Setting(required = false, key = "tx.edc.otel.logs.endpoint", defaultValue = "http://localhost:4318/v1/logs")
    private String otelLogsEndpoint;

    @Setting(required = false, key =  "tx.edc.otel.service.name", defaultValue = "unknown_service")
    private String otelServiceName;

    @Setting(required = false, key = "tx.edc.otel.event.subscriber.active", defaultValue = "false")
    private String isOtelEnabled;

    @Inject
    private EventRouter eventRouter;

    @Inject
    private TypeManager typeManager;
    @Inject
    private EdcHttpClient httpClient;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        if (Boolean.parseBoolean(isOtelEnabled)) {
            eventRouter.register(Event.class, new EventLoggingSubscriber(typeManager, context.getMonitor(), httpClient, otelLogsEndpoint, otelServiceName));
        }
    }
}
