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
package org.eclipse.tractusx.edc.ssi.core.claims;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.eclipse.tractusx.edc.ssi.miw.model.VerifiableCredentialDto;
import org.eclipse.tractusx.edc.ssi.miw.model.VerifiablePresentationDto;
import org.eclipse.tractusx.edc.ssi.spi.IdentityWalletApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SSIVerifiablePresentationImplTest {

  private final String VC_FILE = "verifiablecredential.json";
  private final String VP_FILE = "verifiablepresentation.json";

  VerifiableCredentialDto verifiableCredentialDto = null;
  VerifiablePresentationDto verifiablePresentationDto = null;
  String vpString;
  private SSIVerifiablePresentationImpl ssiVerifiablePresentationImpl;
  private IdentityWalletApiService identityWalletApiService;

  @BeforeEach
  public void setUp() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    var vcStream = getClass().getClassLoader().getResourceAsStream(VC_FILE);
    String vcString = new String(vcStream.readAllBytes());
    this.verifiableCredentialDto = mapper.readValue(vcString, VerifiableCredentialDto.class);

    var vpStream = getClass().getClassLoader().getResourceAsStream(VP_FILE);
    this.vpString = new String(vpStream.readAllBytes());
    this.verifiablePresentationDto = mapper.readValue(vpString, VerifiablePresentationDto.class);

    identityWalletApiService = mock(IdentityWalletApiService.class);
  }

  @Test
  public void testFindByScope_Success() throws Exception {
    // given
    when(identityWalletApiService.getOwnerBPN()).thenReturn("BPN1");
    when(identityWalletApiService.issueVerifiablePresentation(anyString()))
        .thenReturn(this.vpString);
    ssiVerifiablePresentationImpl = new SSIVerifiablePresentationImpl(identityWalletApiService);
    // when
    VerifiablePresentationDto result =
        ssiVerifiablePresentationImpl.getPresentation(this.verifiableCredentialDto);
    // then
    assertEquals(this.verifiablePresentationDto.getId(), result.getId());
  }
}
