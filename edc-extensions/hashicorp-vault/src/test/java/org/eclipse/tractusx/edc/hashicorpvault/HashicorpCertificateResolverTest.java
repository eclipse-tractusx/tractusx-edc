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

import java.security.cert.X509Certificate;
import lombok.SneakyThrows;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class HashicorpCertificateResolverTest {
  private static final String key = "key";

  // mocks
  private HashicorpCertificateResolver certificateResolver;
  private HashicorpVault vault;

  @BeforeEach
  void setup() {
    vault = Mockito.mock(HashicorpVault.class);
    final Monitor monitor = Mockito.mock(Monitor.class);
    certificateResolver = new HashicorpCertificateResolver(vault, monitor);
  }

  @Test
  @SneakyThrows
  void resolveCertificate() {
    // prepare
    X509Certificate certificateExpected = X509CertificateTestUtil.generateCertificate(5, "Test");
    String pem = X509CertificateTestUtil.convertToPem(certificateExpected);
    Mockito.when(vault.resolveSecret(key)).thenReturn(pem);

    // invoke
    certificateResolver.resolveCertificate(key);

    // verify
    Mockito.verify(vault, Mockito.times(1)).resolveSecret(key);
  }

  @Test
  @SneakyThrows
  void nullIfVaultEmpty() {
    // prepare
    Mockito.when(vault.resolveSecret(key)).thenReturn(null);

    // invoke
    final X509Certificate certificate = certificateResolver.resolveCertificate(key);

    // verify
    Assertions.assertNull(certificate);
  }
}
