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

package org.eclipse.tractusx.edc.validation.businesspartner.store.sql;

/**
 * Statement templates and SQL table+column names required for the {@link SqlBusinessPartnerStore}
 */
public interface BusinessPartnerGroupStatements {

    default String getBpnColumn() {
        return "bpn";
    }

    default String getGroupsColumn() {
        return "groups";
    }

    default String getTable() {
        return "edc_business_partner_group";
    }

    String findByBpnTemplate();

    String findByBpnGroupTemplate();

    String findByBpnGroupsTemplate();

    String insertTemplate();

    String deleteTemplate();

    String countQuery();

    String updateTemplate();
}
