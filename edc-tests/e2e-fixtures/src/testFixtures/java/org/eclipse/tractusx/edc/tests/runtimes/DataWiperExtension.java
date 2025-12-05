/*
 * Copyright (c) 2024 Cofinity-X
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

package org.eclipse.tractusx.edc.tests.runtimes;

import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Extension that can be used to initialize a {@link DataWiper} during runtime startup.
 */
public class DataWiperExtension implements ServiceExtension {

    private final AtomicReference<DataWiper> wiper;
    private final Function<ServiceExtensionContext, DataWiper> wiperProvider;

    public DataWiperExtension(AtomicReference<DataWiper> reference, Function<ServiceExtensionContext, DataWiper> wiperProvider) {
        this.wiper = reference;
        this.wiperProvider = wiperProvider;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        wiper.set(wiperProvider.apply(context));
    }
}
