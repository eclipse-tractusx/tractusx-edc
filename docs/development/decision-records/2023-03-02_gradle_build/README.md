# Migration to Gradle

## Decision

Product-EDC will move to Gradle as its build system. This decision
record outlines the reasoning behind the decision as well as the migration path.

## Rationale

The primary motivator for migrating to Gradle is the overarching goal, set by the board of Catena-X, of pursuing an
open-source methodology in general, and to track the Eclipse Datasource Components project in particular. While in
theory that could be achieved with any build tool, much of what is useful or even necessary to achieve that goal, such
as publishing to OSSRH/Sonatype and - in further consequence - to MavenCentral, has already been implemented in the EDC
project. This reduces the implementation and maintenance surface of product-edc with regard to the build, documentation
and testing, and hence increases the development velocity considerably.

It is therefore a foregone conclusion to rely on technology that has already proven itself in the opensource community,
instead of re-implementing (and maintaining) the same functionality all over again.

In detail, the aforementioned features are:

- automatic and structured documentation using the `autodoc` plugin
- generating unified OpenAPI documentation
- default module dependencies
- default configuration (POM, Swagger, artifact signing, code style,...)
- default plugins for project submodules
- publishing to artifact repositories (OSSRH/Sonatype and Maven)
- unified and core-maintained version catalogs and version resolution strategies to avoid version clashes and runtime
  errors

This also contributes to much smaller, easier readable and more succinct build files. Since the build itself is
executable code, a high degree of customization, optimization and modularization is possible.

Furthermore, developers can expect a much improved experience due to Gradle features like caching, task avoidance and
parallelization resulting in faster and more responsive builds.

## Approach

- convert Maven POMs to `build.gradle.kts` files, including BOMs
- generate docker images using [this plugin](https://github.com/bmuschko/gradle-docker-plugin)
- convert maven profiles -> JUnit tags (cf. EDC): run tests using JUnit `@Tag`s and the `includeBuild` feature
- adapt documentation (i.e. exchange the commands)

## Further consideration

Planned improvements regarding the testing procedure (PR https://github.com/catenax-ng/product-edc/pull/781) will also greatly benefit from the EDC build tools such
as JUnit tags and conditional evaluation of the tagged tests. Much of EDC's testing framework is based on Gradle and can
be seamlessly integrated in product-edc.
