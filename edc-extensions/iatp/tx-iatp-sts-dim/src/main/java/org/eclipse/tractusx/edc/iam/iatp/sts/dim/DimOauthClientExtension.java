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

package org.eclipse.tractusx.edc.iam.iatp.sts.dim;

import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.tractusx.edc.iam.iatp.sts.dim.oauth.DimOauth2Client;
import org.eclipse.tractusx.edc.iam.iatp.sts.dim.oauth.DimOauthClientImpl;

import java.time.Clock;

/**
 * Extension that provides an implementation if {@link DimOauth2Client} using {@link Oauth2Client}
 * and the {@link StsRemoteClientConfiguration} configuration for fetching an OAuth token
 */
@Extension(DimOauthClientExtension.NAME)
public class DimOauthClientExtension implements ServiceExtension {

    protected static final String NAME = "DIM OAuth client extension";

    @Inject
    private StsRemoteClientConfiguration clientConfiguration;

    @Inject
    private Oauth2Client oauth2Client;

    @Inject
    private Vault vault;

    @Inject
    private Monitor monitor;

    @Inject
    private Clock clock;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public DimOauth2Client oauth2Client() {
        return new DimOauthClientImpl(oauth2Client, vault, clientConfiguration, clock, monitor);
    }

}