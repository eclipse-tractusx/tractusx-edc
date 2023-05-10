plugins {
    `java-library`
}

dependencies {
    implementation(project(":edc-controlplane:edc-controlplane-base"))
    implementation(project(":edc-controlplane:edc-runtime-memory"))
    implementation(project(":edc-controlplane:edc-controlplane-memory-hashicorp-vault"))
    implementation(project(":edc-controlplane:edc-controlplane-postgresql-azure-vault"))
    implementation(project(":edc-controlplane:edc-controlplane-postgresql-hashicorp-vault"))
}
