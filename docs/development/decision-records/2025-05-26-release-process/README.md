# Release Process Changes

## Decision

To harmonize the release process, all releases will be based solely on `release/*` branches. We will no longer create or 
use separate `bugfix/*` branches for release purposes, nor will we cut releases directly from `main`. In other words, 
there will be no distinction between bugfix and release branches — everything intended for a release must reside in a 
`release/*` branch.

## Rationale

We want to follow the release process of providing release candidates (RC) for testing and then promoting the
successfully tested candidate to an official release. Simultaneously, we maintain a separate bugfix release process that
needs to be harmonized by providing some cohesion  — whether a bugfix, RC, or the official version, the release will
start from a `release/*` branch.


## Approach 

Branch flows:

```mermaid
sequenceDiagram
participant main
participant release/X.Y.0_RC1
participant release/X.Y.0_RC2
participant release/X.Y.0
participant release/X.Y.1
participant QA

Note over main, QA: RC and official release flow
main ->> release/X.Y.0_RC1 : Draft release "X.Y.0-RC1"
release/X.Y.0_RC1 ->> release/X.Y.0_RC1 : Release "X.Y.0-RC1"
QA ->> release/X.Y.0_RC1 : Found problem
main ->> main : Fix problem

main ->> release/X.Y.0_RC2 : Draft release "X.Y.0-RC2"
release/X.Y.0_RC2 ->> release/X.Y.0_RC2 : Release "X.Y.0-RC2"
QA ->> release/X.Y.0_RC2 : RC satisfies requirements

release/X.Y.0_RC2 ->> release/X.Y.0 : Draft official release "X.Y.0" from the "X.Y.0-RC2"
release/X.Y.0 ->> release/X.Y.0 : Release "X.Y.0"
release/X.Y.0 ->> main : bump version to X.Y+1.0-SNAPSHOT

Note over main, QA: Bugfix release flow
QA ->> release/X.Y.0 : Found problem
main ->> main : Fix problem if actual
release/X.Y.0 ->> release/X.Y.1 : Draft release "X.Y.1"
main ->> release/X.Y.1 : cherry-pick fixes
release/X.Y.1 ->> release/X.Y.1 : Release "X.Y.1"
```

1. **`draft-release.yaml`**
- **Trigger:** Must be started from the `HEAD` of `main` or a `release/*` branch.
- **Inputs:**
    1) Version - free text field
- **Actions:**
    1) Validation
       - forbid to create a release candidate from the official release or another release candidate
       - forbid to create a bugfix from the release candidate or main
       - forbid to create an official release from the bugfix or another official release
    2) Create a new `release/*` branch.
    3) Run `generate-and-check-dependencies` action in strict mode.
    4) Update the project version in `gradle.properties` and the Helm chart version based on the workflow’s input parameter.

2. **`release.yml`**
- **Trigger:** Must be started from the `HEAD` of a `release/*` branch.
- **Actions:**
    1) Run automated tests.  
    2) Publish artifacts (Maven, Docker, Helm).
    3) Create the Git tag for this release.
    4) Generate a GitHub Release entry.
    5) Publish the OpenAPI UI spec to GitHub Pages.
    6) Update release notes with a link to the Allure test-report.
    7) If the `official release`  released, make a changes on `main`:
       - Update the project version in `gradle.properties` to the X.Y+1.0-SNAPSHOT.
       - Update the Helm chart version with the X.Y+1.0 version.

3. **New Action:** `update-version-and-charts`
    - Automates version update in `gradle.properties` and `Chart.yaml`

4. **`verify.yaml`**
    - Extend to generate the Allure report for any `release/*` branch.
