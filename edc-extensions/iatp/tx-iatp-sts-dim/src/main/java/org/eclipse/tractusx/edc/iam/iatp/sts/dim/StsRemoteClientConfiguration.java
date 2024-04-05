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

package org.eclipse.tractusx.edc.iam.iatp.sts.dim;

import org.eclipse.edc.spi.security.Vault;

/**
 * Configuration of the OAuth2 client for the OAuth2 Client credentials flow
 *
 * @param tokenUrl          The token endpoint
 * @param clientId          The identifier of the client
 * @param clientSecretAlias The client secret alias to be used with the {@link Vault} for fetching the secret
 */
public record StsRemoteClientConfiguration(String tokenUrl, String clientId, String clientSecretAlias) {

}
