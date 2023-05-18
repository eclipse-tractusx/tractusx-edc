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

plugins {
    `maven-publish`
    `version-catalog`
}

catalog {
    versionCatalog {
        from(files("../gradle/libs.versions.toml"))
    }
}

publishing {
    publications {
        create<MavenPublication>("tractux-edc-version-catalog") {
            from(components["versionCatalog"])
            artifactId = "tractux-edc-versions"
        }
    }
}

edcBuild {
    //TODO: for some reason publishing this fails, e.g. https://github.com/eclipse-tractusx/tractusx-edc/actions/runs/5015011200/jobs/8990164942
    publish.set(false)
}
