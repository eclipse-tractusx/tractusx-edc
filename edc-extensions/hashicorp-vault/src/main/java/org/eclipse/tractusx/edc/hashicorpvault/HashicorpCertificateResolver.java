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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.CertificateResolver;
import org.eclipse.edc.spi.security.Vault;

/** Resolves an X.509 certificate in Hashicorp vault. */
@RequiredArgsConstructor
public class HashicorpCertificateResolver implements CertificateResolver {
  @NonNull private final Vault vault;
  @NonNull private final Monitor monitor;

  @Override
  public X509Certificate resolveCertificate(@NonNull String id) {
    String certificateRepresentation = vault.resolveSecret(id);
    if (certificateRepresentation == null) {
      return null;
    }
    try (InputStream inputStream =
        new ByteArrayInputStream(certificateRepresentation.getBytes(StandardCharsets.UTF_8))) {
      X509Certificate x509Certificate = PemUtil.readX509Certificate(inputStream);
      if (x509Certificate == null) {
        monitor.warning(
            String.format("Expected PEM certificate on key %s, but value not PEM.", id));
      }
      return x509Certificate;
    } catch (IOException e) {
      throw new EdcException(e.getMessage(), e);
    }
  }
}
