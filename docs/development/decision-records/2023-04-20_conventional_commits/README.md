# Using Conventional Commit messages

## Decision

From now on, Tractus-X EDC will use only conventional commit messages. The specification can be
found [here](https://www.conventionalcommits.org/en/v1.0.0/#summary)

## Rationale

Conventional commits create a structured, explicit and unambiguous commit history, that is easy to read and to
interpret. Conventional commits are widely used in the world of open source development.
On top of that, there
is [extensive tooling](https://www.conventionalcommits.org/en/about/#tooling-for-conventional-commits) to support the
creation, interpretation and enforcement of conventional commits.

## Approach

As a first step, we enforce conventional commits as part of our CI pipeline. Tractus-X EDC is using
Squash-Rebase-merging, and the PR title is used as commit message. We will not dictate how people structure their
commits during the _development_ phase of their PR, but we _will_ enforce, that PR titles (and thus: merge commit
messages) are in the conventional commit format.

To do that, we can use a very simple regex check on the PR title:

```yaml
- uses: deepakputhraya/action-pr-title@master
  with:
    regex: '^(build|chore|ci|docs|feat|fix|perf|refactor|revert|style|test)(\(\w+((,|\/|\\)?\s?\w+)+\))?!?: [\S ]{1,80}[^\.]$'
    allowed_prefixes: 'build,chore,ci,docs,feat,fix,perf,refactor,revert,style,test'
    prefix_case_sensitive: true
```

That way, we can catch malformed PR titles early, which would result in malformed _merge commit messages_. In addition,
we can
use any of the tools linked above to ensure commit messages, e.g. when merge commits are altered manually, etc.

## Future outlook

Once we have a structured commit history done in the conventional commit format, we can auto-generate changelogs, link
to (auto-generated) documentation, render visually appealing version information, etc. Essentially, we can use any
number of tooling on top of cc's.
One key aspect would be to get rid of the manual changelog,
see [this discussion](https://github.com/eclipse-tractusx/tractusx-edc/discussions/253).

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2021,2022,2023 Contributors to the Eclipse Foundation
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
