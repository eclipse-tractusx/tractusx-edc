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

package org.eclipse.tractusx.edc.validation.businesspartner.spi.observe;

import org.eclipse.edc.spi.observe.Observable;

import java.util.List;

/**
 * Interface implemented by listeners registered to observe business partner group state changes via
 * {@link Observable#registerListener}. The listener must be called after the state changes are persisted.
 */
public interface BusinessPartnerListener {


    /**
     * Called after a bpn was created.
     *
     * @param bpn the bpn that has been created.
     * @param groups the bpn groups that have been assigned to the bpn
     */
    default void created(String bpn, List<String> groups) {

    }

    /**
     * Called after a bpn was deleted.
     *
     * @param bpn the bpn that has been deleted.
     */
    default void deleted(String bpn) {

    }

    /**
     * Called after a bpn was updated.
     *
     * @param bpn the bpn that has been updated.
     * @param groups the bpn groups that have been assigned to the bpn
     */
    default void updated(String bpn, List<String> groups) {

    }
}
