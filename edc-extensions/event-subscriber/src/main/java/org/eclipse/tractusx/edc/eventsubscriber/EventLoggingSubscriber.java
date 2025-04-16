/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
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

import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.types.TypeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventLoggingSubscriber implements EventSubscriber {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventLoggingSubscriber.class);
    private final TypeManager typeManager;

    EventLoggingSubscriber(TypeManager typeManager) {
        this.typeManager = typeManager;
    }

    @Override
    public <E extends Event> void on(EventEnvelope<E> event) {
        var json = typeManager.writeValueAsBytes(event.getPayload());

        LOGGER.info("Event happened with ID {} and Type {} and data {}", event.getId(), event.getPayload().getClass().getName(), json);

    }

}
