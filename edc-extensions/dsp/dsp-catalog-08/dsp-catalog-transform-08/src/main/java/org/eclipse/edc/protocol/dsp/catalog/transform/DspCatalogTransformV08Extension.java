/********************************************************************************
 * Copyright (c) 2023 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
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

package org.eclipse.edc.protocol.dsp.catalog.transform;

import jakarta.json.Json;
import org.eclipse.edc.participant.spi.ParticipantIdMapper;
import org.eclipse.edc.protocol.dsp.catalog.transform.from.JsonObjectFromCatalogErrorTransformer;
import org.eclipse.edc.protocol.dsp.catalog.transform.from.JsonObjectFromCatalogRequestMessageTransformer;
import org.eclipse.edc.protocol.dsp.catalog.transform.from.JsonObjectFromCatalogTransformer;
import org.eclipse.edc.protocol.dsp.catalog.transform.from.JsonObjectFromDataServiceTransformer;
import org.eclipse.edc.protocol.dsp.catalog.transform.from.JsonObjectFromDatasetTransformer;
import org.eclipse.edc.protocol.dsp.catalog.transform.from.JsonObjectFromDistributionTransformer;
import org.eclipse.edc.protocol.dsp.catalog.transform.to.JsonObjectToCatalogRequestMessageTransformer;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.util.Map;

import static org.eclipse.edc.protocol.dsp.spi.type.Dsp08Constants.DSP_NAMESPACE_V_08;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp08Constants.DSP_TRANSFORMER_CONTEXT_V_08;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

/**
 * Provides the transformers for DSP v0.8 catalog message types via the {@link TypeTransformerRegistry}.
 */
@Extension(value = DspCatalogTransformV08Extension.NAME)
public class DspCatalogTransformV08Extension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol Catalog Transform v08 Extension";

    @Inject
    private TypeTransformerRegistry registry;

    @Inject
    private TypeManager typeManager;

    @Inject
    private ParticipantIdMapper participantIdMapper;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        registerTransformers();
    }

    private void registerTransformers() {
        var jsonFactory = Json.createBuilderFactory(Map.of());

        var dspApiTransformerRegistry = registry.forContext(DSP_TRANSFORMER_CONTEXT_V_08);
        dspApiTransformerRegistry.register(new JsonObjectFromCatalogRequestMessageTransformer(jsonFactory, DSP_NAMESPACE_V_08));
        dspApiTransformerRegistry.register(new JsonObjectToCatalogRequestMessageTransformer(DSP_NAMESPACE_V_08));

        dspApiTransformerRegistry.register(new JsonObjectFromCatalogTransformer(jsonFactory, typeManager, JSON_LD, participantIdMapper, DSP_NAMESPACE_V_08));
        dspApiTransformerRegistry.register(new JsonObjectFromDatasetTransformer(jsonFactory, typeManager, JSON_LD));
        dspApiTransformerRegistry.register(new JsonObjectFromDistributionTransformer(jsonFactory));
        dspApiTransformerRegistry.register(new JsonObjectFromDataServiceTransformer(jsonFactory));
        dspApiTransformerRegistry.register(new JsonObjectFromCatalogErrorTransformer(jsonFactory, DSP_NAMESPACE_V_08));
    }
}
