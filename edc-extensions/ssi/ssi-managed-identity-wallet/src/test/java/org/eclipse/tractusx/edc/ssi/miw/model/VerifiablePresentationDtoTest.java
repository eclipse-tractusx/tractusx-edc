/*
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
