/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.callback;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.tractusx.edc.spi.callback.InProcessCallbackRegistry;

@Extension(InProcessCallbackRegistryExtension.NAME)
public class InProcessCallbackRegistryExtension implements ServiceExtension {

    public static final String NAME = "In process callback registry extension";

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public InProcessCallbackRegistry callbackRegistry() {
        return new InProcessCallbackRegistryImpl();
    }

}
