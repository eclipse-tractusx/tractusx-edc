# Removal of manually curated CHANGELOG.md

## Decision

We will not maintain a manually curated `CHANGELOG.md` file anymore. Instead, we will rely on an automatically generated
one. GitHub Releases offers that feature.

## Rationale

Manually curating a CHANGELOG.md is an arduous process, that does not offer any real value, rather, it introduces mostly
problems. Chief amongst those are the fact that typically the curation happens shortly before the release, which
increases pressure, and introduces the possibility that something is forgotten. Also, digging up the correct issue/PR
numbers is a tedious process.

Relying solely on GitHub Releases fixes all that, because it will generate a succinct changelog, complete with issue/PR
number and contributor.

We would lose the possibility to formulate "humanly readable" change log entries, but that is easily offset by the fact
that we [use conventional commits](../2023-04-20_conventional_commits).

Thus, the commit log should be easily digestible and understandable, assuming a frequent release cycle.

## Approach

First, we need to fix the release process. It seems that at the time of this writing, the release PR contains _all_
commits, instead of just the delta between `main` and the last release.

This is because upon merging normal PRs, we typically do a "Squash-And-Merge". The same thing was done on release PRs,
which caused the git histories of `main` and `releases` to diverge.

> **For this to work, it is imperative to create a "Merge commit" for release PRs as that will preserve commits!**

This is also reflected in the automatically generated changelog, for
example [0.5.0-rc5](https://github.com/eclipse-tractusx/tractusx-edc/releases/tag/0.5.0-rc5). Once that is fixed, we can
delete the CHANGELOG.md file
and [this GH action to update it (line 44)](../../../../.github/workflows/draft-new-release.yaml).

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2021,2022,2023 Contributors to the Eclipse Foundation
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
