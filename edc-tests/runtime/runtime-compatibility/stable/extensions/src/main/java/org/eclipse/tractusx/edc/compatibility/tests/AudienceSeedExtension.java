/*******************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2026 Cofinity-X GmbH
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
 ******************************************************************************/

package org.eclipse.tractusx.edc.compatibility.tests;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.iam.AudienceResolver;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;

import java.util.Map;
import java.util.stream.Collectors;


@Extension("Bdrs Seed Extension")
public class AudienceSeedExtension implements ServiceExtension {


    public static final String BDRS_TESTING_PREFIX = "testing.edc.bdrs";

    public static final String BDRS_TESTING_KEY = "key";
    public static final String BDRS_TESTING_VALUE = "value";

    private Map<String, String> dids;

    @Provider
    public BdrsClient bdrsClient(ServiceExtensionContext context) {
        var dids = readDidsMapping(context);
        return dids::get;
    }

    @Provider
    public AudienceResolver audienceResolver(ServiceExtensionContext context) {
        var dids = readDidsMapping(context);
        return message -> Result.success(dids.get(message.getCounterPartyId()));
    }

    private Map<String, String> readDidsMapping(ServiceExtensionContext context) {
        if (dids == null) {
            var config = context.getConfig(BDRS_TESTING_PREFIX);
            dids = config.partition().map((partition) -> {
                var key = partition.getString(BDRS_TESTING_KEY);
                var value = partition.getString(BDRS_TESTING_VALUE);
                return Map.entry(key, value);
            }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        return dids;
    }
}
