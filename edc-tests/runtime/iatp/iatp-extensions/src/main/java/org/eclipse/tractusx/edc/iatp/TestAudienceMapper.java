/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.iatp;

import org.eclipse.edc.spi.iam.AudienceResolver;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

import java.util.Map;
import java.util.Optional;

/**
 * Implementations for {@link AudienceResolver} that maps connector URL to participant ID
 */
public class TestAudienceMapper implements AudienceResolver {

    private final Map<String, String> audienceMapping;

    public TestAudienceMapper(Map<String, String> audienceMapping) {
        this.audienceMapping = audienceMapping;
    }

    @Override
    public Result<String> resolve(RemoteMessage remoteMessage) {
        return Result.success(Optional.ofNullable(audienceMapping.get(remoteMessage.getCounterPartyId())).orElse(remoteMessage.getCounterPartyId()));
    }
}
