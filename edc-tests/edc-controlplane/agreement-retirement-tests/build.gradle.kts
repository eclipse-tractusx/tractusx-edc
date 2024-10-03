plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    testImplementation(testFixtures(project(":edc-tests:edc-controlplane:fixtures")))

    testImplementation(libs.netty.mockserver)
    testImplementation(libs.edc.junit)
    testImplementation(libs.restAssured)
    testImplementation(libs.awaitility)
    testRuntimeOnly(libs.edc.transaction.local)
    testImplementation(project(":edc-extensions:agreements:retirement-evaluation-spi"))
}

// do not publish
edcBuild {
    publish.set(false)
}