/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.edc.dataset.fix;

import jakarta.json.JsonObject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import static jakarta.ws.rs.HttpMethod.GET;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DATASET_TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_ATTRIBUTE;

public class DatasetFixExtension implements ServiceExtension {

    @Inject
    private WebService webService;

    @Override
    public void initialize(ServiceExtensionContext context) {
        webService.registerResource(ApiContext.PROTOCOL, new DatasetFilter());
    }

    private static class DatasetFilter implements ContainerResponseFilter {

        private static final String GET_DATASETS_PATH = "catalog/datasets/";

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
            if (requestContext.getUriInfo().getPath().contains(GET_DATASETS_PATH) && requestContext.getMethod().equals(GET)) {
                if (responseContext.getEntity() instanceof JsonObject jsonObject) {
                    if (jsonObject.getString(TYPE).equals(DCAT_DATASET_TYPE) &&
                            jsonObject.containsKey(ODRL_POLICY_ATTRIBUTE) &&
                            jsonObject.getJsonArray(ODRL_POLICY_ATTRIBUTE).isEmpty()
                    ) {
                        throw new NotFoundException();
                    }
                }
            }
        }
    }
}
