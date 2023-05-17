# Release process of tractusx-edc

## Decision

To improve stability, reproducibility and maintainability of releases, tractusx-edc will undergo the following changes:

- use EDC `SNAPSHOT` builds during development
- use release versions of EDC in releases. Release branches must not change upstream dependency versions, unless there
  is a clear and concise reason to do so.
- slightly update branching model
- if possible, bugs/defects should be fixed on `main` and be backported to the respective `hotfix/` branch
- only hotfixes for critical security bugs will be provided as defined by the committers for the
  currently released version. Nothing else.
- feature development happens _in developers' forks only_ to keep the Git reflog of the `origin` clean.

## Rationale

Having releases depend on snapshot versions of upstream projects, such as EDC, is inherently dangerous, particularly
when that dependency has not yet reached a final state and breaking changes are to be expected. Most problems will stem
from breaking changes, such as Java SPIs, APIs and changes in service contracts.

Up until now, the only way out was cherry-picking, which is extremely cumbersome and error-prone, and requires a
parallel build pipeline to publish the cherry-picked artifacts of EDC (and potentially others). With the approach
presented here, cherry-picking is still an option, but there are easier alternatives to it.

Every release version published by tractusx-edc must be reproducible at any time.

## Approach

### Use EDC `SNAPSHOT` versions during development

During feature development we only use `-SNAPSHOT` versions of EDC packages. It is assumed that when the build breaks
due to changes in upstream, the fix can be done quickly and easily, much more so than working off technical
debt that would otherwise accumulate over several months. Builds on `main` are therefore _not repeatable_, but that
downside is easily offset by the tighter alignment with and smaller technical debt and integration pain with the
upstream EDC.

### Use release versions of EDC in releases

First, a new branch `release/X.Y.Z` based off of `main` is created. This can either be done
on `HEAD`, or - if desired - on a particular ref. The latter case is relevant if there are already features
in `main` that are not scoped for a particular release.

Second, the dependency onto EDC is updated to the most recent build. For example, if a release is
created on March 27th 2023, the most recent nightly would be `0.0.1-20230326`.

_Updating Gradle files or Maven POMs, creating branches and tags in Git should be automated through GitHub Actions as
part of the release process. For reference_:

- Modifying and committing files: <https://github.com/orgs/community/discussions/26842#discussioncomment-3253612>
- Creating branches: <https://github.com/marketplace/actions/create-branch>
- Creating tags using GitHub's
  API: <https://github.com/eclipse-edc/Connector/blob/b24a5cacbc9fcabdfd8020d779399b3e56856661/.github/workflows/release-edc.yml#L21> (
  example)
- Create GitHub Release: <https://github.com/eclipse-edc/Connector/blob/b24a5cacbc9fcabdfd8020d779399b3e56856661/.github/workflows/release-edc.yml#L56> (example)

Once a release is created, the EDC upstream version must not change anymore, unless there is good reason to do so, for
example, a defect, that needs to be fixed upstream. At that point a decision can also be made to employ a cherry-pick model, in case the
upstream's development has progressed to the point of breaking changes.

### Changes to the branching model

Tractusx-edc's branching model is already very close to
the [GitFlow branching model](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow#:~:text=What%20is%20Gitflow%3F,lived%20branches%20and%20larger%20commits)
which is good. The following changes need to be made:

- feature development happens in forks only. Thus, `feature/` branches don't exist in the `origin`. This is important
  when moving to OSS.
- adhere to GitFlow branch naming conventions, i.e. no `docu`, `tryout_something` branches. Increases readability and
  clarity, improves tool support.

_Other guidelines w.r.t. the review process, merging etc. will follow in a later DR._

## Further considerations

### A word on Bugfixes/Hotfixes

Once a release is published, for example `0.3.1` it will receive no further development other than hotfixes. Similarly,
hotfix branches are created based off of the release branch, here `releases/0.3.1`, thus, `hotfix/0.3.1`. From this,
three scenarios emerge:

1. The actual fix is done on `main` and can be cherry-picked into the `hotfix/0.3.1` branch. No new commits are
   made directly in that branch.
2. The actual fix is done on `main` and must be manually ported into the `hotfix/0.3.1` branch. One or several new
   commits are made on `hotfix/0.3.1`. This is needed when cherry-picking is not available due to incompatibilities
   between `main` and the hotfix branch due to intermittent changes.
3. The fix is only relevant for the `0.3.1` hotfix, it is not needed in `main`. This can happen, when the problem is
   not present on `main`, because it was already implicitly fixed, or otherwise doesn't exist.

This might produce many branches, and the first `hotfix` makes the release obsolete, but it will greatly help
readability and make a release's history readily apparent.

### Nightly builds

Nightly builds are generated according to a fixed schedule. Upstream EDC will soon begin to publish nightly
builds as actual releases (as opposed to: snapshots) to a separate OSSRH-operated repository (see
[this EDC decision record](https://github.com/eclipse-edc/Connector/tree/main/docs/developer/decision-records/2023-02-10-nightly-builds)).

Unfortunately there is no way to automatically trigger the tractusx-edc build whenever a new EDC nightly is
created. The most reliable method is to periodically query for the latest EDC nightly, e.g. leveraging GitHub's
`dependabot` feature, or using the following `curl` command:

```shell
curl <OSSHR_RELEASES_REPO_URL>/org/eclipse/edc/connector-core/maven-metadata.xml | xmllint --xpath "//metadata/versioning/versions/version[last()]" -
```

That would return something like to `0.0.1-20230213`. As soon as the version string (here: Feb 13th, 2023) matches the
current date, we can start the nightly. If the EDC nightly doesn't appear within a set timeframe, we throw an error.

## Notes for becoming OpenSource

- All artifacts (docker images, helm charts, Maven artifacts) should be published to well-known and publicly accessible
  locations such as MavenCentral, DockerHub, etc. The GitHub Packages repository is only accessible to authenticated
  users.
- When the project was migrated to be an Eclipse project, we'll have to adopt the Eclipse Foundation's publishing guidelines,
  which prescribes the use of Jenkins for publishing to MavenCentral and OSSRH.
- Typically, GitHub Actions should perform all verification tasks, running tests, etc. and Jenkins' only purpose is to
  publish.
