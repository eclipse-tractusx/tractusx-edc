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

package org.eclipse.tractusx.edc.iam.ssi.spi;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;

/**
 * Obtains client security tokens from an identity provider.
 * Providers may implement different authorization protocols such as OAuth2.
 */

@ExtensionPoint
public interface SsiCredentialClient {

    /**
     * Obtains a client token encoded as a JWT.
     *
     * @param parameters parameter object defining the token properties.
     * @return generated client token.
     */

    Result<TokenRepresentation> obtainClientCredentials(TokenParameters parameters);

    /**
     * Verifies a JWT bearer token.
     *
     * @param tokenRepresentation A token representation including the token to verify.
     * @return Result of the validation.
     */

    Result<ClaimToken> validate(TokenRepresentation tokenRepresentation);
}
