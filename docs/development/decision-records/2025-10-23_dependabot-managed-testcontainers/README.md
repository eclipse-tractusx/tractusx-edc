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
  We apply this workaround.
- The creation of the test containers makes use of the Dockerfile by reading in the image tag from the file and
  instantiate the container based on the read image tag.
- A general test fixture component is introduced that is used within the integration and e2e tests to manage the
  *Postgres* image as well as the *BDRS* image with a central mechanism. The postgres image version is managed 
  centrally as it is used in many places, the bdrs image is handled locally in the component that uses the image.
- The new test fixtures component will be placed to the existing test fixtures in the 'edc-tests/e2e-fixtures' module.
  Although this is actually dedicated to e2e tests, the fixtures are also used within the integration tests as well,
  so no new dependency concept is added and it keeps test fixture functionality together.
