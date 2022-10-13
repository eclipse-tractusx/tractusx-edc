/*
 * Copyright (c) 2022 ZF Friedrichshafen AG
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *      ZF Friedrichshafen AG - Initial API and Implementation
 */

package org.eclipse.dataspaceconnector.iam.ssi.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class VerifiableCredentialDtoTest {

  private final String VC_File = "verifiablecredential.json";

  @Test
  void testVerifiableCredentialMapper(){
    try (var stream = getClass().getClassLoader().getResourceAsStream(VC_File)) {
      String vcJson = new String(stream.readAllBytes());
      VerifiableCredentialDto vc = new ObjectMapper().readValue(vcJson, VerifiableCredentialDto.class);
      assertThat(vc.getId()).isNotNull();
    } catch (IOException e) {
      throw new EdcException(e.getMessage());
    }
  }

}
