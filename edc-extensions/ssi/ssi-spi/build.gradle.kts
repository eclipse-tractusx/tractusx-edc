plugins {
    `java-library`
}


publishing {
    publications {
        create<MavenPublication>("ssi-spi") {
            artifactId = "ssi-spi"
            from(components["java"])
        }
    }
}