/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial Test
 *
 */

package org.eclipse.tractusx.edc.hashicorpvault;

import java.security.cert.X509Certificate;
import lombok.SneakyThrows;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
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
