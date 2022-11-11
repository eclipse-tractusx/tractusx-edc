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

package org.eclipse.tractusx.edc.ssi.miw.model;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

public class VerifiablePresentationDtoTest {

  @Test
  public void verifiablePresentationVerifyMapping() {
    // given
    String jsonVpFilePath = "verifiablepresentation.json";
    String jsonVpString = "";
    VerifiablePresentationDto vp = null;
    // when
    try (var stream = getClass().getClassLoader().getResourceAsStream(jsonVpFilePath)) {
      jsonVpString = new String(stream.readAllBytes());
      vp = new ObjectMapper().readValue(jsonVpString, VerifiablePresentationDto.class);
    } catch (Exception e) {
      fail(e.getMessage());
    }
    // then
    assertNotNull(vp);
  }
}
