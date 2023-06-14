/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.policy.cx.common;

/**
 * Defines policy namespaces.
 */
public interface PolicyNamespaces {

    String W3C_VC_PREFIX = "https://www.w3.org/2018/credentials";
    String W3C_VC_NS = W3C_VC_PREFIX + "/v1";
    String W3_VP_PROPERTY = W3C_VC_PREFIX + "/vp";

    String CX_NS = "https://w3id.org/2023/catenax/credentials/";
    String CX_SUMMARY_NS = CX_NS + "summary";
    String CX_SUMMARY_NS_V1 = CX_SUMMARY_NS + "/v1";
    String CX_USE_CASE_NS = CX_NS + "usecase";
    String CX_USE_CASE_NS_V1 = CX_USE_CASE_NS + "/v1";

    String CX_SUMMARY_CREDENTIAL = "SummaryCredential";

}
