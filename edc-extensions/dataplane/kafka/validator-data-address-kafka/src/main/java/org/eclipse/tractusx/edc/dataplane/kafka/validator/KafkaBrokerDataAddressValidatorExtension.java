/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.edc.dataplane.kafka.validator;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.validator.spi.DataAddressValidatorRegistry;

import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.KAFKA_TYPE;

@Extension(value = KafkaBrokerDataAddressValidatorExtension.NAME)
public class KafkaBrokerDataAddressValidatorExtension implements ServiceExtension {
    public static final String NAME = "DataAddress KafkaBroker Validator";

    @Inject
    private DataAddressValidatorRegistry dataAddressValidatorRegistry;

    public KafkaBrokerDataAddressValidatorExtension() {
    }

    @Override
    public void initialize(final ServiceExtensionContext context) {
        var validator = new KafkaBrokerDataAddressValidator();
        this.dataAddressValidatorRegistry.registerSourceValidator(KAFKA_TYPE, validator);
        this.dataAddressValidatorRegistry.registerDestinationValidator(KAFKA_TYPE, validator);
    }
}
