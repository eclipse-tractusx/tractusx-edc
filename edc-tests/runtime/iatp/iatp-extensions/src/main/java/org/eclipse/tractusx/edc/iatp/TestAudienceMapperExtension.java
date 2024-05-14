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

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.iam.AudienceResolver;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.util.HashMap;

@Extension("Audience mapper extension")
public class TestAudienceMapperExtension implements ServiceExtension {

    @Setting
    private static final String TX_IATP_AUDIENCES = "tx.edc.iam.iatp.audiences";

    @Provider
    public AudienceResolver audienceResolver(ServiceExtensionContext context) {

        var audienceMapping = new HashMap<String, String>();
        context.getConfig(TX_IATP_AUDIENCES).partition().forEach(partition -> {
            audienceMapping.put(partition.getString("from"), partition.getString("to"));
        });

        return new TestAudienceMapper(audienceMapping);
    }
}
