# Adopt a more regular release train of Tractus-X EDC

## Decision

Starting with `0.8.x`, Tractus-X EDC will adopt a periodic release schedule of 6-8 weeks between versions. This increases the _minor_ version, e.g. `0.8.0 -> 0.9.0` and breaking changes between those versions are to be expected.

## Rationale

In an effort to minimize, technical debt, huge change surfaces and the probability of highly intense crunch times before a (Tractus-X) release, Tractus-X EDC will release roughly every two months.
Note that this is completely independent from Tractus-X releases or Catena-X releases. 

We also do this to give early adopters a chance to update their code base to new features and to shorten the feedback loop, as well as reducing the impact on continuous integration and testing, making the overall developer experience more agile and more manageable.

The EDC project has used this process for a long time with great success.

_The commitment of the Tractus-X EDC team to maintain (= bugfix) the last version used in a Tractus-X/Catena-X release remains unchanged._

## Approach

Weekly committer meetings serve as forum to do issue triage and priorization. We create GitHub milestones as a means of making our release schedule transparent and we plan issues for those milestones.
The exact release date is determined by the committers and will be publicized in appropriate channels, e.g. Matrix.
