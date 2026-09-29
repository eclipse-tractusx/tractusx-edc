# Improving the issue quality and using a stale-bot

## Decision

We will improve GitHub issue quality leveraging the following approaches:

- provide a better issue template
- introduce a stalebot workflow, that automatically closes zombie issues and PRs
- reject (or let get stale) issues that aren't actionable
- create a "backlog" milestone where we can put placeholder issues

## Rationale

We have seen quite a number of issues that are not actionable, because they lack detailed requirements, a definition of
done, a motivation why the issue is needed or simply a proper description.

Some of it may have been caused by the auto-migration from Confluence to GitHub, some of it may have been caused by a
lack of planning discipline.
Un-actionable issues are useless, they clutter our boards and needlessly increase workload, because people periodically
need to look at them. If an issue cannot be described in sufficient detail, it should be labelled appropriately, e.g.
with `question` or `triage`.

We understand that it is not always possible to provide all required information at the time of submitting an issue. For
those cases we will create a `backlog` milestone, that doesn't have a due date and on which those incomplete/placeholder
issues should be planned. The backlog must be sanitized manually.

So in order to foster proper OSS hygiene we should aim for properly formulated, actionable issues, and periodically
clear out ones that are inactive or have become irrelevant.

## Approach

### Issue Template

We propose the following issue templates:

```markdown
---
name: Feature Request
about: Request a new feature
title: ''
labels: triage
assignees: ''

---

## WHAT

// describes the desired functionality, how the feature should behave. This should include clear requirements, and a
// "definition-of-done", i.e. what the result of the issue should be. This is important for concept, documentation or
// ideation issues

## WHY

// outlines the motivation, why the feature is desired, and maybe what the impact is if the feature is _not_
implemented.
// "Because we need it" is not a sufficient reason!

## HOW

// if possible, outlines a solution proposal

## FURTHER NOTES

// anything else you want to outline
```

The existing bug report template is quite usable already, it contains the needed information. Bug reports should also
receive the `triage` label upon creation.

### Stale-bot

- issues, that have the `triage` label get staled after 4 weeks and closed after 2 more weeks
- issues, that have an assignee get staled after 4 weeks and closed after 1 more week
- issues, that neither have an `assignee` nor a `triage` label get staled after 2 weeks, and closed after 1 more week
- bug reports are ignored by the stale bot
- issues in the `backlog` are ignored by the stale bot
- pull-requests get staled after 1 week, and closed after 1 more week

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2021,2022,2023 Contributors to the Eclipse Foundation
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
