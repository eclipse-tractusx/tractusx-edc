/********************************************************************************
 * Copyright (c) 2025 SAP SE
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

package org.eclipse.tractusx.edc.spi.did.document.service;

import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.ServiceResult;

/**
 * Service Provider Interface (SPI) for managing the dynamic service entries in a DID (Decentralized Identifier Document).
 * <p>
 * A DID document typically contains metadata describing a DID subject, including a list of services that can be advertised or discovered.
 * Example structure:
 * <pre>
 * {@code
 * {
 *     "@context": [],
 *     "id": "did:web:example.com:edc01",
 *     "service": [
 *         {
 *             "serviceEndpoint": "https://wallet.example.com/api/holder/edc01",
 *             "type": "CredentialService",
 *             "id": "did:web:example.com:edc01#CredentialService"
 *         }
 *     ],
 *     "verificationMethod": [],
 *     "authentication": [],
 *     "assertionMethod": [],
 *     "keyAgreement": [],
 *     "capabilityInvocation": []
 * }
 * }
 * </pre>
 * <p>
 * The <code>service</code> list can be extended to advertise additional endpoints, such as DSP endpoints,
 * data plane URLs, or other custom services.
 * <p>
 * This SPI allows wallet or DID management solutions to provide implementations for dynamically creating, updating,
 * or removing service entries in a DID document.
 * <p>
 * Implementations should ensure that changes to the DID document are performed according to the underlying wallet
 * or DID registry's requirements and that updates are properly propagated.
 */
@ExtensionPoint
public interface DidDocumentServiceClient {

    /**
     * Creates or updates a service entry in the DID document.
     *
     * @param service to be updated
     * @return a {@link ServiceResult} indicating success or failure of the operation
     */
    ServiceResult<Void> update(Service service);

    ServiceResult<Void> deleteById(String id);
}
