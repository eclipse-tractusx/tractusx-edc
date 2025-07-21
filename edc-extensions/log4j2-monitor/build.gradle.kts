plugins {
    `java-library`
}


dependencies {
    api(libs.edc.spi.core)
    api(libs.log4j2.api)

    implementation(libs.opentelemetry.instrumentation.annotations)
    testImplementation(libs.edc.junit)
    testImplementation(libs.log4j2.core.test)
}


