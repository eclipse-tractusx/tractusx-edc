/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.tractusx.edc.iam.ssi.identity.extractor;

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.agent.ParticipantAgentService;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
public class SsiIdentityExtractorExtensionTest {

    SsiIdentityExtractorExtension extension;

    ParticipantAgentService participantAgentService = mock(ParticipantAgentService.class);

    @BeforeEach
    void setup(ObjectFactory factory, ServiceExtensionContext context) {
        context.registerService(ParticipantAgentService.class, participantAgentService);
        extension = factory.constructInstance(SsiIdentityExtractorExtension.class);
    }

    @Test
    void initialize(ServiceExtensionContext context) {
        extension.initialize(context);
        verify(participantAgentService).register(isA(CredentialIdentityExtractor.class));
    }
}
