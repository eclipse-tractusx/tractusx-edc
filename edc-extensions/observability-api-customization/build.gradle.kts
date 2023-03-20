
plugins {
    `maven-publish`
    `java-library`
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

dependencies {
    implementation(edc.spi.core)
    implementation(edc.spi.web)

    // provides the web server
    runtimeOnly(edc.ext.http)

    testImplementation(edc.junit)
    testImplementation(libs.restAssured)

    // needed for auto-registering the Auth Service:
    testImplementation(edc.api.management)

    // provides token-based authentication at test runtime
    testRuntimeOnly(edc.auth.tokenbased)


}

