# Project Structure

## Issue Tracking

Issues are maintained in [TraxtusX JIRA](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/SECURITY.md).
To request access, please contact the [TractusX mailing list](https://accounts.eclipse.org/mailing-list/tractusx-dev).

## Reporting Vulnerabilities

Vulnerabilities in the TractusX code base are best reported directly to the
[Eclipse Foundation](https://www.eclipse.org/security/).

## Git Flow

The TractusX EDC repository uses a Git Flow, with `main` as the release branch and `develop` as the development branch.
Other branches should follow the naming conventions of `feature/x` or `hotfix/x`, though this is not strictly enforced.

## Tooling

We use Java 11 with Maven for dependencies and builds.
We use [Lombok](https://projectlombok.org/features/) annotations.
We use [Spotless](https://github.com/diffplug/spotless) for code formatting.
We cannot use Spring, as Core EDC does not support it.
Releases are in the form of Docker containers and Helm charts.
