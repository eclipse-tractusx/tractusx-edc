# Refactor of the Release Workflow

## Decision

Refactor the existing workflows to accommodate the release of bugfix versions as described in [this](../2024-07-18-Improvements-versioning-scheme/README.md) decision record:
- Remove the releases and release/X.Y.Z branches in favor of using just the main branch and tags.
- Change the draft-new-release and publish-new-release to enable hotfix releases

## Rationale

As of this decision record creation date, the release of a new version is triggered with the execution of the `draft-new-release` workflow.
The workflow branches from main and checks out into a `release/*` branch where a new commit to represent the releasable state is created. Afterward a PR is created to merge the release/X.Y.Z branch into `releases`.
Once the PR is approved and merged, the `publish-new-release` workflow is automaticaly triggered to run the required steps in order to publish all artifacts
in their desired repositories/platforms. With the successful publication of said artifacts, the head of the `releases` branch is merged back into `main` 
to keep the integrity of the commit tree. Finally, the current project version is automatically bumped to the next minor version in a new commit to `main` 
to kickstart the next development cycle.

As they stand, the current release workflows don't allow the release of bugfix versions since:
- the `draft-new-release` workflow doesn't create any bugfix branch;
- even if the branch is manually created, merging it to `releases` via PR to trigger the `publish-new-release` workflow, is not possible;
- The `publish-new-release` workflow can't be manually triggered;
- Even if it could be manually triggered, there are steps that shouldn't apply for bugfix releases such as bumping the project version or merging to main;


## Approach

The draft-new-release workflow should include one extra inputs:
- A `choice` input to allow the distinction between a release or bugfix.

Github Actions Jobs and Step conditionals are then used to make the execution follow the required steps, as follows.

### Prepare Release workflow logic
- Triggers manually. Input is the version, choice and base ref (should be either commit from main or tag)
- if choice is release
  - branches from main into `release/<input_version>`
- if choice is bugfix
  - branches from tag into `bugfix/<input_version>`
- replaces the project default version `(gradle.properties)` with the workflow version input
- replaces openapi spec version
- replaces charts version and README
- commits and pushes to `release/*` or `bugfix/X.Y.Z` branch
- if new release
   - opens PR with new `release/*` branch as head and `main/` as base

Then, a different process takes places if we intend to release a latest version or a bugfix. 

### Case for latest releases
After the prepare release PR is approved, merging into main will trigger the publish-release workflow where the required publication steps will take place, as shown in [Publish release workflow logic](#publish-release-workflow-logic).

### Case for bugfix Releases
After the bugfix branch is created, developers commit the fix or set of fixes to it via PR.
The release of the bugfix version should be triggered manually through the publishing workflow and should execute most of the publication steps as a normal release.
However, for a bugfix release, the workflow shouldn't:
- Make any commits to the main branch
- Bump the project version to the next minor version

### Publish release workflow logic
- Triggers automatically when prepare release PR is merged into main from `release/*` branch
- Triggers manually only from branch `bugfix/X.Y.Z`

- Extracts release version from `gradle.properties`
- Triggers release to maven repository by calling `trigger-maven-publish.yaml`
- Triggers docker image publishing by calling `trigger-maven-publish.yaml`
- Releases helm-charts
- Creates Github Tag and Release
- if PR was merged to main it means a new release then:
   - Updates project version to next version, commits and pushes to main
      - if final version, increments to next minor version and adds "-SNAPSHOT"
      - if not final version (e.g.-rc1), do not increment
- _(elif PR was merged to hotfix then don't increment)_
- Publishes to Swaggerhub by calling `publish-swaggerhub.yaml`
- Publishes OpenAPI UI spec to Github Pages by calling `publish-openapi-ui.yml`

## Other Considerations

- Keeping `release/*` branches is no longer needed since the tag references the commit on main.
- Release candidates should be handled the same using the same process as a latest release.
- Release workflows should not run on forks.
- Should be possible to trigger the publishing of snapshots manually from main or bugfix branches.
- `draft-new-release` workflow could be named to `manual-release-draft`.
- `publish-new-release` workflow could be named `publish-release`.

