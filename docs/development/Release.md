# Release

## Prerequisites

[![Apache Maven][maven-shield]][maven-url]

## Update DEPENDENCIES file
### 1. Setup Eclipse Dash License Tool Maven Plugin locally

At the time of writing there maven plugin could not be downloaded from the repository.
As alternative check out the repository and build the plugin locally.

#### 1.1 Checkout repository

`git clone https://github.com/eclipse/dash-licenses.git`

#### 1.2 Install Plugin in local maven repository

`mvn clean install`

### 2. Generate DEPENDENCIES file

`./mvnw org.eclipse.dash:license-tool-plugin:license-check -Ddash.summary=DEPENDENCIES`


[maven-shield]: https://img.shields.io/badge/Apache%20Maven-URL-blue
[maven-url]: https://maven.apache.org