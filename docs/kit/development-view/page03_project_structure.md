# Project Structure

## Issue Tracking

Issues are maintained in [GitHub Issues](https://github.com/eclipse-tractusx/tractusx-edc/issues).

## Reporting Vulnerabilities

Vulnerabilities in the Eclipse Tractus-X code base are best reported directly to the
[Eclipse Foundation](https://www.eclipse.org/security/).

## Git Flow

The Tractus-X EDC repository uses a Git Flow, with `main` as the development branch and `releases` as the release branch.
Other branches should follow the naming conventions of `feature/x` or `hotfix/x`, though this is not strictly enforced.

## Tooling

We use Java 17 with Gradle for dependencies and builds.
We use [Checkstyle](https://github.com/checkstyle/checkstyle) for code formatting.
Releases are in the form of Docker containers and Helm charts.
