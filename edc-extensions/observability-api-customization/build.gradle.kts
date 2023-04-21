plugins {
    `maven-publish`
    `java-library`
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

dependencies {
    implementation(libs.edc.spi.core)
    implementation(libs.edc.spi.web)

    // provides the web server
    runtimeOnly(libs.edc.ext.http)

    testImplementation(libs.edc.junit)
    testImplementation(libs.restAssured)

    // needed for auto-registering the Auth Service:
    testImplementation(libs.edc.api.management)

    // provides token-based authentication at test runtime
    testRuntimeOnly(libs.edc.auth.tokenbased)
}

