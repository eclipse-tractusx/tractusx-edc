plugins {
    `maven-publish`
    `java-library`
}

dependencies {
    api(libs.edc.spi.core)
    api(libs.edc.junit)
    implementation(libs.edc.spi.dataplane.selector)
    implementation(libs.edc.spi.dataplane.transfer)
    implementation(libs.bouncyCastle.bcpkixJdk18on)
}
