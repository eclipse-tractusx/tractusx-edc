
plugins {
    `java-library`
}

dependencies {
    implementation(project(":edc-controlplane:edc-controlplane-base"))
    implementation(project(":edc-controlplane:edc-controlplane-memory"))
    implementation(project(":edc-controlplane:edc-controlplane-memory-hashicorp-vault"))
    implementation(project(":edc-controlplane:edc-controlplane-postgresql"))
    implementation(project(":edc-controlplane:edc-controlplane-postgresql-hashicorp-vault"))
}
