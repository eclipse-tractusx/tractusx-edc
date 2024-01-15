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

package org.eclipse.tractusx.edc.iam.iatp;

import java.util.Set;

import static java.lang.String.format;

public interface TxIatpConstants {

    String CREDENTIAL_TYPE_NAMESPACE = "org.eclipse.tractusx.vc.type";
    String DEFAULT_CREDENTIAL = "MembershipCredential";
    String READ_OPERATION = "read";
    String DEFAULT_MEMBERSHIP_SCOPE = format("%s:%s:%s", CREDENTIAL_TYPE_NAMESPACE, DEFAULT_CREDENTIAL, READ_OPERATION);
    Set<String> DEFAULT_SCOPES = Set.of(DEFAULT_MEMBERSHIP_SCOPE);

}
