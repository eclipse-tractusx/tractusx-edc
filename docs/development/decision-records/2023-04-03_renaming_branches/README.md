# Renaming Git branches to comply with TractusX standards

## Decision

TractusX-EDC will rename its Git branching structure to comply with TractusX release guidelines, and to be able to
leverage
GitHub convenience features, while continuing to use the Gitflow branching model.

## Rationale

The TractusX organization has established
a [release guideline](https://eclipse-tractusx.github.io/docs/release/trg-2/trg-2-1/) which mandates that all projects'
default branch be called `main`.

### Selecting default branches

In GitHub, the default branch has a couple of important features attached to it:

- cloning or forking the repository will automatically check out the default branch
- when creating pull requests the default branch is targeted by default
- [automatic issue linking and closing](https://docs.github.com/en/issues/tracking-your-work-with-issues/linking-a-pull-request-to-an-issue)
  only works with the default branch

### The problem with GitFlow

The GitFlow branching model suggests that the day-to-day work be done on a branch called `develop`, while the `main`
branch stores the version history and only receives (merge) commits after a version releases.

This would call for `develop` being the GitHub default branch, which is forbidden by the aforementioned release
guideline.

## Approach

In order to comply with the TractusX release guideline, to make use of the GitHub features _and_ also use GitFlow, we
propose renaming a couple of branches. While GitFlow _suggests_ branch names, it does not _require_ it, and most
tools allow for customizing them anyway. Thus, from an abstract perspective, the following changes are necessary:

- `main` becomes our work/development branch. All pull requests target `main`.
- `develop` gets deleted
- a new branch `releases` is introduced, which tracks the release history and receives post-release merge commits.

Technically this will involve force-pushing, which is a potentially destructive operation. Therefor the following
section outlines the exact sequence of steps. Note that "upstream" refers to `eclipse-tractusx/tractusx-edc`, while "
fork" refers to `catenax-ng/tx-tractusx-edc`.

- create a new branch `upstream/releases`
- create a new branch `fork/releases`, set it to track `upstream/releases`
- push the contents of `fork/main` -> `upstream/releases`
- synchronize `upstream/develop` with `fork/develop`
- force-push the contents of `develop` -> `upstream/main` (do **not** update the tracking branch!)
- synchronize `upstream/main` -> `fork/main`
- delete/archive `upstream/develop` and `fork/develop`

_Note that most of this will likely need to be done manually, since GitHub does not allow for advanced Git operations
like force-pushing. Write access to `upstream` is required!_

## Further notes

The new `releases` branch (note the plural) will serve the same purpose that `main` did up until now, which is to track
all releases (via merge commits and tags) in chronological order. We will continue to have separate `release/x.y.z`
branches for every release.
