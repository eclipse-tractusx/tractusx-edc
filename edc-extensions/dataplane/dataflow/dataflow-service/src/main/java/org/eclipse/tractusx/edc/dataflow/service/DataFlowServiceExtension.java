/********************************************************************************
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
 ********************************************************************************/

package org.eclipse.tractusx.edc.dataflow.service;

import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.tractusx.non.finite.provider.push.spi.FinitenessEvaluator;
import org.eclipse.tractusx.edc.spi.dataflow.DataFlowService;

@Extension(DataFlowServiceExtension.NAME)
public class DataFlowServiceExtension implements ServiceExtension {

    protected static final String NAME = "DataFlow Service";

    @Inject
    private DataPlaneStore dataPlaneStore;

    @Inject
    private FinitenessEvaluator finitenessEvaluator;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public DataFlowService dataFlowService() {
        return new DataFlowServiceImpl(dataPlaneStore, finitenessEvaluator);
    }

}
