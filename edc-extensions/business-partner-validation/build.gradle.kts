plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(edc.spi.core)
    implementation(edc.spi.policy)
    implementation(edc.spi.contract)
    implementation(edc.spi.policyengine)
}
