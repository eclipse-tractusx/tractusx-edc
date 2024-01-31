/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.tests.transfer;

import jakarta.json.JsonObject;
import org.eclipse.edc.junit.annotations.EndToEndTest;

import java.util.Map;

import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.TX_CREDENTIAL_NAMESPACE;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.frameworkPolicy;

@EndToEndTest
public class IatpFrameworkAgreementHttpConsumerPullWithProxyInMemoryTest extends IatpHttpConsumerPullWithProxyInMemoryTest {
    
    @Override
    protected JsonObject createContractPolicy(String bpn) {
        return frameworkPolicy(Map.of(TX_CREDENTIAL_NAMESPACE + "FrameworkAgreement.pcf", "active"));
    }

}
