
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
}
