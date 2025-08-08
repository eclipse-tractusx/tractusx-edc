/********************************************************************************
 * Copyright (c) 2025 Cofinity-X GmbH
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

package org.eclipse.tractusx.edc.agreements.bpns.store;

import org.eclipse.tractusx.edc.agreements.bpns.spi.store.AgreementsBpnsStore;
import org.eclipse.tractusx.edc.agreements.bpns.spi.types.AgreementsBpnsEntry;
import org.junit.jupiter.api.Test;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.tractusx.edc.agreements.bpns.spi.store.AgreementsBpnsStore.ALREADY_EXISTS_TEMPLATE;


public abstract class AgreementsBpnsStoreTestBase {

    @Test
    void save_whenExists() {
        var agreementId = "test-agreement-id";
        var entry = createAgreementsBpnsEntry(agreementId, "providerBpn", "consumerBpn");
        getStore().save(entry);
        var result = getStore().save(entry);
        assertThat(result).isFailed()
                .detail().isEqualTo(ALREADY_EXISTS_TEMPLATE.formatted(agreementId));
    }

    private AgreementsBpnsEntry createAgreementsBpnsEntry(String agreementId, String providerBpn, String consumerBpn) {
        return AgreementsBpnsEntry.Builder.newInstance()
                .withAgreementId(agreementId)
                .withProviderBpn(providerBpn)
                .withConsumerBpn(consumerBpn)
                .build();
    }

    protected abstract AgreementsBpnsStore getStore();
}
