plugins {
    `maven-publish`
    `java-library`
}

dependencies {
    api(libs.edc.spi.core)
    implementation(libs.edc.spi.dataplane.transfer)
    implementation(libs.bouncyCastle.bcpkixJdk18on)
}
