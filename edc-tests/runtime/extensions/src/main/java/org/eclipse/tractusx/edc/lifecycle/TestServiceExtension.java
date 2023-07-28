/*
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.lifecycle;

import org.eclipse.edc.connector.transfer.spi.status.StatusCheckerRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

@Extension(value = "Extension used to inject dummy services into E2E runtimes")
public class TestServiceExtension implements ServiceExtension {

    @Inject
    private StatusCheckerRegistry registry;

    @Override
    public void initialize(ServiceExtensionContext context) {
        // takes care that ongoing HTTP transfers are actually completed, otherwise they would
        // always stay in the "STARTED" state
        registry.register("HttpProxy", (transferProcess, resources) -> true);
    }
}
