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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.eclipse.tractusx.edc.ssi.miw.model.VerifiableCredentialDto;
import org.eclipse.tractusx.edc.ssi.miw.registry.VerifiableCredentialRegistry;
import org.eclipse.tractusx.edc.ssi.spi.IdentityWalletApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;

public class SSIVerifiableCredentialsImplTest {

  private final String VC_FILE = "verifiablecredential.json";

  VerifiableCredentialDto verifiableCredentialDto = null;

  private SSIVerifiableCredentialsImpl ssiVerifiableCredentialsImpl;
  private IdentityWalletApiService identityWalletApiService;

  @Spy private VerifiableCredentialRegistry vcRegistry;

  @BeforeEach
  public void setUp() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    var vcStream = getClass().getClassLoader().getResourceAsStream(VC_FILE);
    String vcString = new String(vcStream.readAllBytes());
    this.verifiableCredentialDto = mapper.readValue(vcString, VerifiableCredentialDto.class);

    identityWalletApiService = mock(IdentityWalletApiService.class);
    vcRegistry = mock(VerifiableCredentialRegistry.class);

    ssiVerifiableCredentialsImpl =
        new SSIVerifiableCredentialsImpl(identityWalletApiService, vcRegistry);
  }

  @Test
  public void testFindByScope_Success() throws Exception {
    // given
    when(vcRegistry.getVerifiableCredential("scope")).thenReturn(this.verifiableCredentialDto);
    // when
    VerifiableCredentialDto result = ssiVerifiableCredentialsImpl.findByScope("scope");
    // then
    assertEquals(this.verifiableCredentialDto.getId(), result.getId());
  }
}
