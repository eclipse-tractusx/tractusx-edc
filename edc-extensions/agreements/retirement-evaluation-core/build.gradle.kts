plugins {
    `java-library`
    `maven-publish`
    `java-test-fixtures`
}

dependencies {

    implementation(libs.edc.runtime.metamodel)
    implementation(libs.edc.spi.boot)
    implementation(libs.edc.spi.policyengine)
    implementation(libs.edc.spi.contract)
    implementation(libs.edc.core.policy.monitor)
    implementation(libs.edc.lib.store)
    implementation(libs.edc.lib.query)
    api(project(":edc-extensions:agreements:retirement-evaluation-spi"))

    testImplementation(libs.edc.junit)
    testFixturesImplementation(libs.edc.junit)
    testFixturesImplementation(libs.junit.jupiter.api)
    testFixturesImplementation(libs.assertj)
}