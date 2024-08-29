# Refactor of the Release Workflow

## Decision

Refactor the existing workflows to accomodate the release of bugfix versions as described in [this](../2024-07-18-Improvements-versioning-scheme/README.md) decision record:
- Remove the releases and release/X-Y.Z branches in favor of using just the main branch and tags.
- Enable hotfix releases. 
- Should be possible to trigger the publishing of snapshots manually and automatically on every commit on `main` and after a release.
- Intermediate builds ("nightly builds") should be triggered on a cron schedule, where the version string is `X.X.X-YYYYMMDD-SNAPSHOT`
- publishing releases: triggered by manual interaction, supplying the version as workflow input

## Rationale

As of this decision record creation date, the release of a new version is triggered with the execution of the `draft-new-release` workflow.
The workflow branches from main and checks out into a release/X.Y.Z branch where a new commit to represent the releasable state is created. Afterwards a PR is created to merge the release/X.Y.Z branch into `releases`.
Once the PR is approved and merged, the `publish-new-release` workflow is automaticaly triggered to launch the required steps to publish all artifacts
in their desired repositories/platforms. With the successful publication of said artifacts, the head of the releases branch is merged back into main 
to keep the integrity of the commit tree. Finally, the current project version is automatically bumped to the next minor version in a new commit to main 
to kickstart the next development cycle.

As they stand, the current release workflows don't allow the release of bugfix versions and can't be quickly adapted to include such requirement since:
- the draft-new-release workflow doesn't create any bugfix branch;
- even if the branch is manually created, merging it to `releases` via PR, to trigger the publish-new-release workflow is not possible;
- The publish-new-release workflow can't be manually triggered;
- Even if it could be manually triggered, there are steps that shouldn't apply for bugfix releases such as bumping the project version or merging to main;


## Approach

The draft-new-release workflow should include two extra inputs:
- A `choice` input to allow the distinction between a release or bugfix.
- A commit that will be used to branch the release of (ideally should be head of main or a tag)

Github Actions Jobs and steps conditionals are then used to make the execution follow the required steps, as follows.

### Prepare Release workflow
- Triggers manually. Input is the version, choice and base branch
- if choice is release
  - branches from main into `release/<input_version>`
- if choice is bugfix
  - branches from tag into `bugfix/<input_version>`
- replaces the project default version `(gradle.properties)` for the WF input
- replaces openapi spec version
- bumps chart version and readMe
- commits and pushes to `release/` or `bugfix/` branch
- if new release
   - opens PR with new `release/` branch as head and `main/` as base

Then, a different process occurs for the release of a latest version or a bugfix. 

### Latest Release
After the prepare release PR is approved, merging into main will trigger the publish-release workflow where the required publication steps will take place.

### Bugfix Releases
After the bugfix branch is created, developers commit the fix or set of fixes to it, either via direct push or PR.
The release of the bugfix version should be triggered manualy through the publishing workflow and should execute most of the publication steps as a normal release.
However, for a bugfix release the workflow shouldn't:
- Make any commits to the main branch
- Bump the project version to the next minor version

### Publish Release workflow
- Triggers automatically when prepare release PR is merged into main from release/X.Y.Z branch
- Triggers manually only from branch release/ or hotfix/

- Extracts release version from release/ or hotfix/ branches
- Triggers release to maven repository (trigger-maven-publish.yaml)
- Triggers docker image publishing (trigger-maven-publish.yaml)
- Releases helm-charts
- Creates Github Tag and Release
- if PR was merged to main it means a new release then:
   - Updates project version to next version, commits and pushes to main
      - if final version, increments to next patch version and -SNAPSHOT
      - if not final version (e.g.-rc1), do not increment
- (elif PR was merged to hotfix then dont bump)
- Publishes to swaggerhub (triggers publish-swaggerhub.yaml)
- Publishes OpenAPI UI spec to Github Pages (publish-openapi-ui.yml)


## Other Considerations

- Release workflows should not run on forks

