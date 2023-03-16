
plugins {
    `java-library`
}

dependencies {
    implementation(project(":edc-dataplane:edc-dataplane-base"))
    implementation(project(":edc-dataplane:edc-dataplane-azure-vault"))
    implementation(project(":edc-dataplane:edc-dataplane-hashicorp-vault"))
}
