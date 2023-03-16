
plugins {
    `maven-publish`
    `java-library`
}

dependencies {
    api(edc.spi.core)
    api(edc.junit)
    implementation(edc.spi.dataplane.selector)
    implementation(edc.spi.dataplane.transfer)
    implementation(libs.bouncyCastle.bcpkix)
}
