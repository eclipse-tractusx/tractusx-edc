/********************************************************************************
 * Copyright (c) 2025 Metaform Systems Inc.
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

package org.eclipse.edc.protocol.dsp.spi.type;

import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.protocol.spi.ProtocolVersion;

import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_CONTEXT_SEPARATOR;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_HTTPS_BINDING;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_SCOPE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_TRANSFORMER_CONTEXT;

public interface Dsp08Constants {

    String DSPACE_SCHEMA = "https://w3id.org/dspace/v0.8/";
    String V_08_VERSION = "v0.8";
    String V_08_PATH = "/";
    ProtocolVersion V_08 = new ProtocolVersion(V_08_VERSION, V_08_PATH, DSP_HTTPS_BINDING);

    String DSP_SCOPE_V_08 = DSP_SCOPE + DSP_CONTEXT_SEPARATOR + V_08_VERSION;

    String DSP_TRANSFORMER_CONTEXT_V_08 = DSP_TRANSFORMER_CONTEXT + DSP_CONTEXT_SEPARATOR + V_08_VERSION;

    JsonLdNamespace DSP_NAMESPACE_V_08 = new JsonLdNamespace(DSPACE_SCHEMA);
}
