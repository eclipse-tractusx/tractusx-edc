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

package net.catenax.edc.hashicorpvault;

import java.security.cert.X509Certificate;
import java.util.UUID;
import lombok.SneakyThrows;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HashicorpCertificateResolverIT extends AbstractHashicorpIT {

  @Test
  @SneakyThrows
  void resolveCertificate_success() {
    String key = UUID.randomUUID().toString();
    X509Certificate certificateExpected = X509CertificateTestUtil.generateCertificate(5, "Test");
    String pem = X509CertificateTestUtil.convertToPem(certificateExpected);

    Vault vault = getVault();
    vault.storeSecret(key, pem);
    CertificateResolver resolver = getCertificateResolver();
    X509Certificate certificateResult = resolver.resolveCertificate(key);

    Assertions.assertEquals(certificateExpected, certificateResult);
  }

  @Test
  @SneakyThrows
  void resolveCertificate_malformed() {
    String key = UUID.randomUUID().toString();
    String value = UUID.randomUUID().toString();
    Vault vault = getVault();
    vault.storeSecret(key, value);

    CertificateResolver resolver = getCertificateResolver();
    X509Certificate certificateResult = resolver.resolveCertificate(key);
    Assertions.assertNull(certificateResult);
  }
}
