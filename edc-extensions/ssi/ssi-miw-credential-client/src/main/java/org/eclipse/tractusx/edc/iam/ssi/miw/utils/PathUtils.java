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

package org.eclipse.tractusx.edc.iam.ssi.miw.utils;

public class PathUtils {

    private PathUtils() {
    }

    public static String removeTrailingSlash(String path) {
        var fixedPath = path;
        if (fixedPath.endsWith("/")) {
            fixedPath = fixedPath.substring(0, fixedPath.length() - 1);
        }
        return fixedPath;
    }
}
