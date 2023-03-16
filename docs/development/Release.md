# Release

## Prerequisites

[![Apache Maven][maven-shield]][maven-url]

## Update DEPENDENCIES file

### 1. Setup Eclipse Dash License Tool locally

For instructions on how to download the Eclipse Dash Tool executable, refer to the
project's [GitHub page](https://github.com/eclipse/dash-licenses#get-it). 

### 2. Generate DEPENDENCIES file

This call generates the dependencies file. This list is populated by deriving dependencies using the build tool (i.e.,
gradle), analysing them using an IP tool (i.e., Eclipse Dash Tool), and decorating the resulting report with additional
information using a custom script.

Execute the gradle task `allDependencies` for creating an integrated dependency report over all sub-modules of the
project (including isolated modules). To process the dependencies of a specific module (e.g., an individual launcher)
execute the standard `dependencies` task:

- First, the dependencies of this module are calculated with gradle and passed to the Dash tool:

```shell
gradle allDependencies | grep -Poh "(?<=\s)[\w.-]+:[\w.-]+:[^:\s]+" | sort | uniq | java -jar /path/org.eclipse.dash.licenses-0.0.1-SNAPSHOT.jar - -summary DEPENDENCIES
```

_Note: on some machines (e.g. macOS) [the ack tool](https://beyondgrep.com/install/) should be used instead of `grep`._ 

### 3. Resolve restricted Dependencies

If a dependency is `restricted`, it is not approved by the Eclipse Foundation, yet.
The Eclipse Bot is able to approve dependencies automatically, if the license can be resolved by ClearlyDefined.

1. (optional) Visit [https://clearlydefined.io/harvest](https://clearlydefined.io/harvest) and harvest the dependency
   from maven central.
2. Create the Eclipse IP Issues or ask an Eclipse Commiter to do this for you.

[maven-shield]: https://img.shields.io/badge/Apache%20Maven-URL-blue

[maven-url]: https://maven.apache.org