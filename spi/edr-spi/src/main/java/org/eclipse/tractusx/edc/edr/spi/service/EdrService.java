/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.edr.spi.service;

import org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.tractusx.edc.edr.spi.types.RefreshMode;

import java.util.List;

/**
 * Mediate the access to the {@link EndpointDataReferenceEntry} and cached {@link DataAddress} EDRs.
 */
@ExtensionPoint
public interface EdrService {


    /**
     * Resolve a {@link DataAddress} EDR associated with the transfer process. The token will be refreshed
     * accordingly the {@link RefreshMode}
     *
     * @param transferProcessId The id of the transfer process
     * @param mode              The {@link RefreshMode}
     * @return If the token in {@link DataAddress} is expired a refresh one
     */
    ServiceResult<DataAddress> resolveByTransferProcess(String transferProcessId, RefreshMode mode);

    /**
     * Search for {@link EndpointDataReferenceEntry}
     *
     * @param query The {@link QuerySpec}
     * @return The list of matching {@link EndpointDataReferenceEntry} if success, failure otherwise
     */
    ServiceResult<List<EndpointDataReferenceEntry>> query(QuerySpec query);

}
