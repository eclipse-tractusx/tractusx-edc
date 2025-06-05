/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
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

package org.eclipse.tractusx.edc;

import java.util.Set;

import static java.lang.String.format;

public interface TxIatpConstants {

    String CREDENTIAL_TYPE_NAMESPACE = "org.eclipse.tractusx.vc.type";
    String MEMBERSHIP_CREDENTIAL = "MembershipCredential";
    String DATA_EXCHANGE_GOVERNANCE_CREDENTIAL = "DataExchangeGovernanceCredential";
    String BPN_CREDENTIAL = "BpnCredential";
    String READ_OPERATION = "read";
    String MEMBERSHIP_SCOPE = format("%s:%s:%s", CREDENTIAL_TYPE_NAMESPACE, MEMBERSHIP_CREDENTIAL, READ_OPERATION);
    String DATA_EXCHANGE_GOVERNANCE_SCOPE = format("%s:%s:%s", CREDENTIAL_TYPE_NAMESPACE, DATA_EXCHANGE_GOVERNANCE_CREDENTIAL, READ_OPERATION);
    String BPN_SCOPE = format("%s:%s:%s", CREDENTIAL_TYPE_NAMESPACE, BPN_CREDENTIAL, READ_OPERATION);
    Set<String> DEFAULT_SCOPES = Set.of(MEMBERSHIP_SCOPE, DATA_EXCHANGE_GOVERNANCE_SCOPE, BPN_SCOPE);

}
