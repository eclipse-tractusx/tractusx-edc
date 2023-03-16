
plugins {
    `maven-publish`
    `java-library`
}

dependencies {
    api(edc.spi.core)
    implementation(edc.spi.dataplane.transfer)
    implementation(libs.bouncyCastle.bcpkix)
}
