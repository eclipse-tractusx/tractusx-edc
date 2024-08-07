# Improvements to the Tractus-X EDC versioning scheme

## Decision

Starting with `0.8.x`, Tractus-X EDC will publish an official release roughly every 12 weeks (once per quarter).
Bugfixes will be provided as "pure bugfixes". That means, that a bugfix version is created off of the previous release
version, and contains _only_ the bugfix and nothing else. For example, a bugfix `0.8.2` would branch off of `0.8.1`
rather than `main`.

Further, Tractus-X EDC will provide more frequent intermediate builds.

## Rationale

Tractus-X adopted a quarterly release cadence, so we need to align with that, because Tractus-X EDC versions should be
supported (= bugfixed) for that time.
Up until now, Tractus-X EDC bugfixes were created off of the `main` branch, which means that not only the fix, but also
potentially new features were included in that version. This new scheme keeps bugfixes clean and provides a higher level
of backwards compatibility.

## Approach

In our release workflow we need to be able to distinguish between a "release" and a "bugfix", because the latter would
branch off of - and merge back into - a branch other than `main`, i.e. the bugfix branch. In addition, the version
bumping logic must be adapted.

### Backporting fixes

There are several distinct scenarios that can arise when a bugfix becomes necessary:

1. Cherry-picking: the fix is implemented on the `main` branch, and the relevant commit can be cherry-picked into
   the bugfix branch.
2. Re-implementing: the fix is implemented on the `main` branch, but has to be manually backported into the bugfix
   branch. This can happen if the `main` branch has changed enough since the last release that cherry-picking is
   not possible.
3. Fix-only: the bug does not occur on `main`, thus needs to be implemented on the bugfix branch only.
4. Upstream fix required: in cases where the fix must be implemented in the upstream EDC project, Tractus-X EDC must
   request that an upstream bugfix version be released. This upstream bugfix can then be incorporated into the Tractus-X
   EDC bugfix version.

### Release cadence

The cadence of official releases will be lengthened from [6-8 weeks](../2024-04-11_txedc_release_train) to 12 weeks (
quarterly). This is to keep in step with the overall Tractus-X release cadence.

### Version maintenance

As before, Tractus-X EDC will _only_ maintain the latest version. For example, if version `0.8.0` is the latest
official release, bugfixes will only be supplied for that version, i.e. `0.8.1`, `0.8.2` and so forth. Earlier versions
will **not** be maintained.

Additionally, maintenance will be done exclusively for critical functional or security flaws if no other remedy is
available. No features will be backported. The classification and triage of such flaws remains at the discretion of the
Tractus-X EDC team.

### Use of release candidates and intermediate builds

In order to shorten the feedback loop with downstream projects, Tractus-X EDC will publish intermediate builds and
release candidates between official releases. Intermediate builds are technically snapshots and are published
regularly (for example "nightly", "weekly", etc), release candidates are stable releases and are published irregularly
and less frequently.

- Intermediate builds are denominated `<VERSION>-<DATE>-SNAPSHOT`, for example `0.8.0-20240718-SNAPSHOT`
- release candidates would be named `<VERSION>-rcX`, for example `0.8.0-rc2`

> _NB: intermediate builds don't include dedicated Helm charts, but they do include Maven artefacts and
Docker images. Release candidates also include Helm charts._