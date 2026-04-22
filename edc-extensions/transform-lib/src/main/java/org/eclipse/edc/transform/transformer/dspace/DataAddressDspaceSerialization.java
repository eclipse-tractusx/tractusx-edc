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

package org.eclipse.edc.transform.transformer.dspace;

import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;

/**
 * Contains constants specifically intended for serializing a {@link org.eclipse.edc.spi.types.domain.DataAddress}
 * to JSON-LD using the `dspace:` prefix format.
 */
public interface DataAddressDspaceSerialization {
    String DSPACE_DATAADDRESS_TYPE_TERM = "DataAddress";
    String DSPACE_DATAADDRESS_TYPE_IRI = DSPACE_SCHEMA + DSPACE_DATAADDRESS_TYPE_TERM;
    String ENDPOINT_TYPE_PROPERTY_TERM = "endpointType";
    String ENDPOINT_TYPE_PROPERTY_IRI = DSPACE_SCHEMA + ENDPOINT_TYPE_PROPERTY_TERM;
    String ENDPOINT_PROPERTY_TERM = "endpoint";
    String ENDPOINT_PROPERTY_IRI = DSPACE_SCHEMA + ENDPOINT_PROPERTY_TERM;
    String ENDPOINT_PROPERTIES_PROPERTY_TERM = "endpointProperties";
    String ENDPOINT_PROPERTIES_PROPERTY_IRI = DSPACE_SCHEMA + ENDPOINT_PROPERTIES_PROPERTY_TERM;
    String ENDPOINT_PROPERTY_PROPERTY_TYPE_TERM = "EndpointProperty";
    String ENDPOINT_PROPERTY_PROPERTY_TYPE_IRI = DSPACE_SCHEMA + ENDPOINT_PROPERTY_PROPERTY_TYPE_TERM;
    String ENDPOINT_PROPERTY_NAME_PROPERTY_TERM = "name";
    String ENDPOINT_PROPERTY_NAME_PROPERTY_IRI = DSPACE_SCHEMA + ENDPOINT_PROPERTY_NAME_PROPERTY_TERM;
    String ENDPOINT_PROPERTY_VALUE_PROPERTY_TERM = "value";
    String ENDPOINT_PROPERTY_VALUE_PROPERTY_IRI = DSPACE_SCHEMA + ENDPOINT_PROPERTY_VALUE_PROPERTY_TERM;
}
