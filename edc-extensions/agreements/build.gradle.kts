plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":edc-extensions:agreements:retirement-evaluation-core"))
    api(project(":edc-extensions:agreements:retirement-evaluation-api"))
    api(project(":edc-extensions:agreements:retirement-evaluation-spi"))
}
