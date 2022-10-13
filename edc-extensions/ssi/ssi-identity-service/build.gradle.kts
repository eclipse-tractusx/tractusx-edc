plugins {
    `maven-publish`
    `java-library`
    signing
}
val nimbusVersion: String by project
val rsApi: String by project

tasks{
//    addNativeLibs(type: Copy) {
//        from 'natives'
//        into "build/jfx/native/$jfx.appName"
//    }
//    build {
//        dependsOn(plugin)
//    }
}

dependencies {
    api(project(":core"))
    api(project(":extensions:http"))
    api(project(":spi"))
    api("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")

    implementation(project(":extensions:iam:ssi:ssi-managed-identity-wallet"))
    implementation(project(":extensions:iam:ssi:ssi-spi"))
    implementation(project(":extensions:api:api-core"))
    implementation(project(":extensions:api:data-management:api-configuration"))
    implementation(project(":extensions:filesystem:configuration-fs"))
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
    implementation("com.danubetech:verifiable-credentials-java:1.0.0")


    implementation("decentralized-identity:jsonld-common-java:1.0.0")
    implementation("info.weboftrust:ld-signatures-java:1.0.0")
    implementation("com.github.multiformats:java-multibase:v1.1.0")

}

repositories {
    maven {
        url = uri("https://repo.danubetech.com/repository/maven-public/")
    }
    maven {
        url = uri("https://jitpack.io/")
    }
}