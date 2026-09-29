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

package org.eclipse.tractusx.edc.validation.businesspartner.store;

import org.eclipse.tractusx.edc.validation.businesspartner.spi.store.BusinessPartnerStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

public abstract class BusinessPartnerStoreTestBase {

    @Test
    void resolveForBpn() {
        getStore().save("test-bpn", List.of("group1", "group2", "group3"));
        assertThat(getStore().resolveForBpn("test-bpn").getContent()).containsExactly("group1", "group2", "group3");
    }

    @Test
    void resolveForBpn_notExists() {
        assertThat(getStore().resolveForBpn("test-bpn").succeeded()).isFalse();
    }

    @Test
    void resolveForBpn_existsNoGroups() {
        getStore().save("test-bpn", List.of());
        assertThat(getStore().resolveForBpn("test-bpn").getContent()).isNotNull().isEmpty();
    }

    @Test
    void resolveForBpnGroup_multipleBpns() {
        getStore().save("test-bpn-0", List.of("group2"));
        getStore().save("test-bpn-1", List.of("test-bpn-group", "group2", "group3"));
        getStore().save("test-bpn-2", List.of("test-bpn-group"));
        getStore().save("test-bpn-3", List.of("test-bpn-group", "group17"));

        assertThat(getStore().resolveForBpnGroup("test-bpn-group"))
                .isSucceeded()
                .asInstanceOf(list(String.class))
                .containsExactly("test-bpn-1", "test-bpn-2", "test-bpn-3");
    }

    @Test
    void resolveForBpnGroup_bpnGroupNotExists() {
        assertThat(getStore().resolveForBpnGroup("group-not-exist")).isFailed();
    }

    @Test
    void resolveForBpnGroups() {
        getStore().save("test-bpn-0", List.of("group2"));
        getStore().save("test-bpn-1", List.of("test-bpn-group", "group2", "group3"));
        getStore().save("test-bpn-2", List.of("test-bpn-group"));
        getStore().save("test-bpn-3", List.of("test-bpn-group", "group17"));

        assertThat(getStore().resolveForBpnGroups())
                .isSucceeded()
                .asInstanceOf(list(String.class))
                .contains("group2", "test-bpn-group", "group3", "group17");
    }

    @Test
    void save() {
        getStore().save("test-bpn", List.of("group1", "group2", "group3"));
        assertThat(getStore().resolveForBpn("test-bpn").getContent()).containsExactly("group1", "group2", "group3");
    }

    @Test
    void save_exists() {
        getStore().save("test-bpn", List.of("group1", "group2", "group3"));
        assertThat(getStore().save("test-bpn", List.of("group4")).succeeded()).isFalse();
    }

    @Test
    void delete() {
        var businessPartnerNumber = "test-bpn";
        getStore().save(businessPartnerNumber, List.of("group1", "group2", "group3"));
        var delete = getStore().delete(businessPartnerNumber);
        assertThat(delete.succeeded()).withFailMessage(delete::getFailureDetail).isTrue();
    }

    @Test
    void delete_notExist() {
        var businessPartnerNumber = "test-bpn";
        getStore().delete(businessPartnerNumber);
        assertThat(getStore().resolveForBpn(businessPartnerNumber).succeeded()).isFalse();
    }

    @Test
    void update() {
        var businessPartnerNumber = "test-bpn";
        getStore().save(businessPartnerNumber, List.of("group1", "group2", "group3"));
        assertThat(getStore().update(businessPartnerNumber, List.of("group4", "group5")).succeeded()).isTrue();
    }

    @Test
    void update_notExists() {
        assertThat(getStore().update("test-bpn", List.of("foo", "bar")).succeeded()).isFalse();
    }

    protected abstract BusinessPartnerStore getStore();
}
