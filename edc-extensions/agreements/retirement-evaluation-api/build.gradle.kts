plugins {
    `java-library`
    `maven-publish`
}
dependencies {

    implementation(libs.edc.runtime.metamodel)
    implementation(libs.edc.spi.boot)

    testImplementation(libs.edc.junit)
}
