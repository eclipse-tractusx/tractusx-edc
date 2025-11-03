# Create a common test fixture component to manage dependencies as separate containers used for tests

## Decision

We introduce a test fixture functionality that provides management of services used in tests by running container
images. The image tags will be managed in a way, that dependabot can propose updates.

## Rationale

Currently, the container images used for dependent components, like *Postgres* or *BDRS* are hardcoded within
the corresponding test classes. The *BDRS* component is used only once so far, but *Postgres* is referenced
multiple times across integration tests and once in the e2e tests. For integration tests, we rely on the management
of the version upstream, as we basically use a component out-of-the-box with a fixed postgres version which differs
from the one used in the e2e tests (17.3 vs. 17.4).

Intentions:
- Get control on the version used for testing
- Manage the version in one version
- Make the version manageable by dependabot

## Approach

- Dependabot can manage container image versions by monitoring Dockerfiles. There is a workaround to use a dummy
  Dockerfile that just contains a FROM clause with the image name and tag. Updates are then proposed by dependabot.
- The creation of the test containers makes use of the Dockerfile by reading in the image tag from the file and
  instantiate the container based on the read image tag.
- A general test fixture component is introduced that is used within the integration and e2e tests to manage the
  *Postgres* image as well as the *BDRS* image centrally. The usage is then depending on this central provisioning.
- The new test fixtures will be placed in the core-utils component and then used in integration tests as well as
  e2e tests.
