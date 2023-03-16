/*
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2021-2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.edc.provision.additionalheaders;

import org.eclipse.edc.connector.transfer.spi.provision.ProvisionManager;
import org.eclipse.edc.connector.transfer.spi.provision.ResourceManifestGenerator;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

public class ProvisionAdditionalHeadersExtension implements ServiceExtension {

  @Inject private ResourceManifestGenerator resourceManifestGenerator;

  @Inject private ProvisionManager provisionManager;

  @Inject private TypeManager typeManager;

  @Override
  public void initialize(ServiceExtensionContext context) {
    typeManager.registerTypes(
        AdditionalHeadersResourceDefinition.class, AdditionalHeadersProvisionedResource.class);
    resourceManifestGenerator.registerGenerator(new AdditionalHeadersResourceDefinitionGenerator());
    provisionManager.register(new AdditionalHeadersProvisioner());
  }
}
