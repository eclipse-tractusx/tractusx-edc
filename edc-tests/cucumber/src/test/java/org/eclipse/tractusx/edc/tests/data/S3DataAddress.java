/*
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.tests.data;

import java.util.Objects;

public class S3DataAddress implements DataAddress {

    private final String bucketName;
    private final String region;
    private final String keyName;

    public S3DataAddress(String bucketName, String region, String keyName) {
        this.bucketName = Objects.requireNonNull(bucketName);
        this.region = Objects.requireNonNull(region);
        this.keyName = Objects.requireNonNull(keyName);
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getRegion() {
        return region;
    }

    public String getKeyName() {
        return keyName;
    }
}
