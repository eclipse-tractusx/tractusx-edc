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

package org.eclipse.tractusx.edc.validation.businesspartner.spi.store;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.StoreResult;

import java.util.List;

@ExtensionPoint
public interface BusinessPartnerStore {
    String NOT_FOUND_TEMPLATE = "BPN %s was not found";
    String ALREADY_EXISTS_TEMPLATE = "BPN %s already exists in database";

    StoreResult<List<String>> resolveForBpn(String businessPartnerNumber);

    StoreResult<List<String>> resolveForBpnGroup(String businessPartnerGroup);

    StoreResult<List<String>> resolveForBpnGroups();

    StoreResult<Void> save(String businessPartnerNumber, List<String> groups);

    StoreResult<Void> delete(String businessPartnerNumber);

    StoreResult<Void> update(String businessPartnerNumber, List<String> groups);
}
