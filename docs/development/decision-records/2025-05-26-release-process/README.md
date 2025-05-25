# Release Process Changes

## Decision

To streamline our release process, we will split it into two manually triggered workflows:
### Prepare for Release ("Draft Release" workflow):
- **Trigger:** Can be started from a specific commit SHA, the `HEAD` of `main`, the `HEAD` of any branch or tag.
- **Actions:**
  1. Create a new `release/*` or `hotfix/*` branch.
  2. Update dependencies in both the release/hotfix branch and in `main`.
  3. On the release/hotfix branch:
     - Bump the Gradle project version and the Helm chart version based on the workflow’s input parameters.
  4. On the `main` branch:
      - For a real release, bump the Gradle version to the next `-SNAPSHOT`.
      - For release candidates (RC) or hotfixes, leave the Gradle version unchanged.
      - Update the Helm chart version according to the workflow’s input parameters.
### Execute Release ("Release" workflow):
- **Trigger:** Can be started from the `HEAD` of a `release/*`, `hotfix/*` branch.
- **Actions:**
    1) Run automated tests.
    2) Publish artifacts (Maven, Docker, Helm).
    3) Create the Git tag for this release.
    4) Generate a GitHub Release entry.
    5) Publish the OpenAPI UI spec to GitHub Pages.
    6) Update release notes with a link to the Allure test-report.

## Rationale

- **Flexibility:** We can now prepare a release branch from any commit SHA, not just `main`.
- **Clarity:** All tests and changes for a given release live in the release branch, making them easy to find.
- **Simplicity:** Eliminates unnecessary pull requests against `main` and run tests prematurely during the GitHub Release.


## Approach 

1. **`draft-release.yaml`**
    - Update according to the “Prepare for Release” workflow steps.

2. **`release.yaml`**
    - Update according to the “Execute Release” workflow steps.

3. **New Action:** `update-version-and-charts`
    - Automates version update in `gradle.properties` and `Chart.yaml`

4. **`verify.yaml`**
    - Extend to generate the Allure report for any `release/*` or `hotfix/*` branch.
