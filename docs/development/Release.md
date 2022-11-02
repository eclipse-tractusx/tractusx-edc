# Release

## Prerequisites

[![Apache Maven][maven-shield]][maven-url]

## Update DEPENDENCIES file
### 1. Setup Eclipse Dash License Tool Maven Plugin locally

At the time of writing the maven plugin could not be downloaded from the repository.
As alternative check out the repository and build the plugin locally, so that its added to the local maven repository.

#### 1.1 Checkout repository

`git clone https://github.com/eclipse/dash-licenses.git`

#### 1.2 Install Plugin in local maven repository

`mvn clean install`

### 2. Generate DEPENDENCIES file

This call generates the dependencies file. If there is a value set for `dash.iplab.token` it will also automatically create new issues for all unknown dependencies at the Eclipse Intellectual Property board
https://gitlab.eclipse.org/eclipsefdn/emo-team/iplab/-/issues

**Update Dependencies File and create Eclipse Issues (Eclipse Commiters only)**
```bash
./mvnw org.eclipse.dash:license-tool-plugin:license-check \
    -Ddash.summary=DEPENDENCIES \
    -Ddash.projectId=automotive.tractusx \
    -Ddash.iplab.token=<token*>
```

**Update Dependencies File**
```bash
./mvnw org.eclipse.dash:license-tool-plugin:license-check \
    -Ddash.summary=DEPENDENCIES
```

<p>
    <small>
        * see dash <a href="https://github.com/eclipse/dash-licenses#automatic-ip-team-review-requests">documentation</a> on how to get a token
    </small>
</p>

### 3. Resolve restricted Dependencies

If a dependency is `restricted`, it is not approved by the Eclipse Foundation, yet.
The Eclipse Bot is able to approve dependencies automatically, if the license can be resolved by ClearlyDefined.

1. (optional) Visit [https://clearlydefined.io/harvest](https://clearlydefined.io/harvest) and harvest the dependency from maven central.
2. Create the Eclipse IP Issues or ask an Eclipse Commiter to do this for you.


[maven-shield]: https://img.shields.io/badge/Apache%20Maven-URL-blue
[maven-url]: https://maven.apache.org