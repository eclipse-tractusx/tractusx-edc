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

package org.eclipse.tractusx.edc.iam.ssi.miw;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.tractusx.edc.iam.ssi.miw.api.MiwApiClient;
import org.eclipse.tractusx.edc.iam.ssi.miw.credentials.SsiMiwCredentialClient;
import org.eclipse.tractusx.edc.iam.ssi.spi.SsiCredentialClient;

@Extension(SsiMiwCredentialClientExtension.EXTENSION_NAME)
public class SsiMiwCredentialClientExtension implements ServiceExtension {

    public static final String EXTENSION_NAME = "SSI MIW Credential Client";

    @Inject
    private MiwApiClient apiClient;

    @Override
    public String name() {
        return EXTENSION_NAME;
    }

    @Provider
    public SsiCredentialClient credentialVerifier() {
        return new SsiMiwCredentialClient(apiClient);
    }

}
