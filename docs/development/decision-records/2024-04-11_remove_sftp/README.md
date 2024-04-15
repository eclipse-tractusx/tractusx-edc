# Remove SFTP modules

## Decision

Starting with the next release of Tractus-X EDC, the SFTP modules will be removed from the code base. Specifically `transferprocess-sftp-client`, `transferprocess-sftp-common` and `transferprocess-sftp-provisioner`.

## Rationale

These modules don't implement any official Catena-X Standard, and as such, have not been part of any official Tractus-X EDC distribution for a significant period of time. 
Further, those modules are not up to the coding standards established by Tractus-X EDC, and would have to be refactored significantly. Even then, having dead code in a repository is bad hygiene, as it generates maintenance churn. Tractus-X EDC is not responsible for maintaining niche extensions, that are not officially sanctioned.

## Approach

Starting with version `0.8.x` of Tractus-X EDC, these modules will be deleted from the code base. At that time, stakeholders interested in continuing to maintain them are welcome to adopt them into their own repositories.
