plugins {
    `java-library`
}

dependencies {
    implementation(project(":edc-extensions:business-partner-validation"))
    implementation(project(":edc-extensions:control-plane-adapter"))
    implementation(project(":edc-extensions:cx-oauth2"))
    implementation(project(":edc-extensions:data-encryption"))
    implementation(project(":edc-extensions:dataplane-selector-configuration"))
    implementation(project(":edc-extensions:hashicorp-vault"))
    implementation(project(":edc-extensions:postgresql-migration"))
    implementation(project(":edc-extensions:provision-additional-headers"))
    implementation(project(":edc-extensions:transferprocess-sftp-client"))
    implementation(project(":edc-extensions:transferprocess-sftp-common"))
    implementation(project(":edc-extensions:transferprocess-sftp-provisioner"))


    testImplementation("com.google.code.gson:gson:2.10")
    testImplementation("org.apache.httpcomponents:httpclient:4.5.14")
    testImplementation("org.junit.platform:junit-platform-suite:1.9.2")
    testImplementation("io.cucumber:cucumber-java:7.11.1")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:7.11.1")
    testImplementation("org.slf4j:slf4j-api:2.0.3")
    testImplementation(libs.restAssured)
    testImplementation(libs.postgres)
    testImplementation(libs.awaitility)
    testImplementation(libs.aws.s3)
    testImplementation(edc.spi.core)
}

tasks.withType(Test::class) {
    onlyIf {
        System.getProperty("cucumber") == "true"
    }
}
