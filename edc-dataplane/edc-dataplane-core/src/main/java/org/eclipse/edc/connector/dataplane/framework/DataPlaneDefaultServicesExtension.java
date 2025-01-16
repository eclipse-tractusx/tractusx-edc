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

package org.eclipse.edc.connector.dataplane.framework;

import org.eclipse.edc.connector.dataplane.framework.pipeline.PipelineServiceImpl;
import org.eclipse.edc.connector.dataplane.framework.registry.TransferServiceSelectionStrategy;
import org.eclipse.edc.connector.dataplane.framework.store.InMemoryAccessTokenDataStore;
import org.eclipse.edc.connector.dataplane.framework.store.InMemoryDataPlaneStore;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAuthorizationService;
import org.eclipse.edc.connector.dataplane.spi.iam.NoOpDataPlaneAuthorizationService;
import org.eclipse.edc.connector.dataplane.spi.iam.PublicEndpointGeneratorService;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.connector.dataplane.spi.store.AccessTokenDataStore;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.time.Clock;

@Extension(value = DataPlaneDefaultServicesExtension.NAME)
public class DataPlaneDefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Framework Default Services";
    @Inject
    private Clock clock;
    @Inject
    private CriterionOperatorRegistry criterionOperatorRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Provider(isDefault = true)
    public TransferServiceSelectionStrategy transferServiceSelectionStrategy() {
        return TransferServiceSelectionStrategy.selectFirst();
    }

    @Provider(isDefault = true)
    public DataPlaneStore dataPlaneStore() {
        return new InMemoryDataPlaneStore(clock, criterionOperatorRegistry);
    }

    @Provider(isDefault = true)
    public AccessTokenDataStore defaultAccessTokenDataStore() {
        return new InMemoryAccessTokenDataStore(criterionOperatorRegistry);
    }

    @Provider(isDefault = true)
    public PipelineService pipelineService(ServiceExtensionContext context) {
        return new PipelineServiceImpl(context.getMonitor());
    }

    // todo: should this be a default service?
    @Provider(isDefault = true)
    public PublicEndpointGeneratorService publicEndpointGenerator() {
        return new PublicEndpointGeneratorServiceImpl();
    }

    @Provider(isDefault = true)
    public DataPlaneAuthorizationService dataPlaneAuthorizationService(ServiceExtensionContext context) {
        context.getMonitor().info("No proper DataPlaneAuthorizationService provided. The data-plane won't support PULL transfer types.");
        return new NoOpDataPlaneAuthorizationService();
    }
}