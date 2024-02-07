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

package org.eclipse.edc.security.signature.jws2020;

import com.apicatalog.ld.schema.LdTerm;
import com.apicatalog.ld.signature.CryptoSuite;
import com.apicatalog.ld.signature.primitive.MessageDigest;
import com.apicatalog.ld.signature.primitive.Urdna2015;

class Jws2020CryptoSuite extends CryptoSuite {
    Jws2020CryptoSuite(LdTerm id) {
        super(id, new Urdna2015(), new MessageDigest("SHA-256"), new Jws2020SignatureProvider());
    }
}
