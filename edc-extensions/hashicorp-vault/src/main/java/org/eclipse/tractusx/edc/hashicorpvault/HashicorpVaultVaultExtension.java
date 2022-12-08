/*
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.edc.hashicorpvault;

import okhttp3.OkHttpClient;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.security.CertificateResolver;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.security.VaultPrivateKeyResolver;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

@Provides({Vault.class, CertificateResolver.class, PrivateKeyResolver.class})
public class HashicorpVaultVaultExtension extends AbstractHashicorpVaultExtension
    implements ServiceExtension {

  @Override
  public String name() {
    return "Hashicorp Vault";
  }

  @Override
  public void initialize(ServiceExtensionContext context) {
    final HashicorpVaultClientConfig config = loadHashicorpVaultClientConfig(context);

    final OkHttpClient okHttpClient = createOkHttpClient(config);

    final HashicorpVaultClient client =
        new HashicorpVaultClient(config, okHttpClient, context.getTypeManager().getMapper());

    final HashicorpVault vault = new HashicorpVault(client, context.getMonitor());
    final CertificateResolver certificateResolver =
        new HashicorpCertificateResolver(vault, context.getMonitor());
    final VaultPrivateKeyResolver privateKeyResolver = new VaultPrivateKeyResolver(vault);

    context.registerService(Vault.class, vault);
    context.registerService(CertificateResolver.class, certificateResolver);
    context.registerService(PrivateKeyResolver.class, privateKeyResolver);

    context.getMonitor().info("HashicorpVaultExtension: authentication/initialization complete.");
  }
}
