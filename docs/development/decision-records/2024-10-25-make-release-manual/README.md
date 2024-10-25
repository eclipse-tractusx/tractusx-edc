# Make Release Workflow Manual

## Decision

The release workflow will now require a manual activation vs. being automatically triggered via a PR merge event.

## Rationale

Having an automatic trigger has a couple of implications:

- GH only allows for the `on pull_request` event to be of type `closed`. This works fine when a PR is merged but will
  also trigger
  if the PR is just `closed` or `dismissed`, leading to the incorrect execution of the release workflow. This could be
  avoided with
  additional job run conditions, but that would add unnecessary complexity to the workflow.
- Given PR `on pull_request` events are monitored for the `main` branch, every PR that is merged to main triggers the
  workflow.
  Conditionals are in place that prevent the workflow from actually running, however the workflow run history is filled
  with skipped run logs.

Additionally:
- We keep a dedicated `manual-release-bugfix` workflow to manually trigger a bugfix release because it is done under
  different conditions.
  This workflow re-uses the main release workflow. There is no need to keep the `manual-release-bugfix` workflow if the
  main release workflow is manually triggered.
- Workflow can only be triggered by commiters so control over releases and its conditions is guaranteed.

## Approach

- Remove existing event hooks from `release.yml`. Add `workflow_dispatch` event hook.
- Remove the `manual-bugfix-release` workflow.
- Add conditionals to `release.yml` to guarantee releases can only be done from `main`, or `bugfix/` branches.

