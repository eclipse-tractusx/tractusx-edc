# Testing concept for product-edc

## Decision

Henceforth, testing shall be done in accordance with the herein outlined rules and definitions. As the guiding principles we shall establish:

- separation-of-concerns
- fast test execution speed
- debuggability
- low resource footprint
- leverage features provided by, and adopt standards set by the [Eclipse Dataspace Project](https://github.com/eclipse-edc/Connector)
- easy setup on developer systems

## Rationale

Past experiences with product-edc's testing setup has shown that it is time- and resource-consuming, which also makes it unreliable at times. 
Furthermore, a finer-grained test classification such as the one outlined in this document is currently neither present nor documented.

### Definitions and distinction

This section is largely taken from the [EDC's testing documentation](https://github.com/eclipse-edc/Connector/blob/bab97cccf4d61a3a380a1d70925b34f4cec1b401/docs/developer/testing.md) with a few amendments.

- **unit tests**: test one single class by mocking/stubbing all collaborating objects.

- **integration tests**: test one particular aspect of a software, that may involve external systems. Example: testing a particular object store based on PostgreSQL. External systems are to be provided out-of-band, e.g. through a CI pipeline or a script that runs on a local machine. Starting external systems from code violates the separation of concerns and it may also cause problems on some systems, e.g. when docker is not on the `PATH`, or not available at all. Integration tests typically only involve parts of a connector. A _component test_ is a special form of an integration test, where real collaborators, but no external systems are used.

- **system tests**: rely on the _entire system_ being present. This is specific for each variant, for example testing a request against a system's API and verifying that a particular entry was created in the database. System tests involve _one connector_ and the external service.

- **end-to-end-tests**: similar to system tests, but they involve several connectors plus external services such as databases, identity providers, objects stores, etc. This type of test is used to verify that certain business requirements are fulfilled by simulating real user scenarios from start to finish, hence they are sometimes dubbed "business tests". For example, one would send a data request to a connector's public API and expect the connector to behave in a certain way and expect a certain response back. To keep things simple, end-to-end tests can be conjoined with system tests.

- **deployment tests**: tests deployment artifacts such a Docker images or a Helm charts. The purpose of such a test is to verify the correct configuration and composition of an artifact, its purpose is _not_ to test application logic. However, we can use normal requests to _verify_ the correct installation. Sometimes this is referred to as "Smoke test".

- **performance tests**: measure whether a certain iteration of the software fulfills pre-established performance goals. These tests are highly specific and may have dependencies onto specific hardware and network parameters.

It is a [well-established](https://martinfowler.com/articles/practical-test-pyramid.html) fact that unit tests should make up for the majority of tests, because they are easy and quick to write and quick to execute, whereas integration and end-to-end tests are usually more complex and time-consuming to write and run.

## Approach

Generally we should aim at writing unit tests rather than integration tests, because they are simpler, more stable and typically run faster. Sometimes that's not (easily) possible, especially when an implementation relies on an external system that is not easily mocked or stubbed such as cloud-based databases.

Therefore, in many cases writing unit tests is more involved that writing an integration test, for example say we wanted to test our implementation of a PostgreSQL-backed queue. We would have to mock the behaviour of the PostgreSQL API, which - while certainly possible - can get complicated pretty quickly. Now we still might do that for simpler scenarios, but
eventually we might want to write an integration test that uses a (containerized) PostgreSQL test instance.

### Adopt a "local-first" mindset

EDC provides a way to launch (multiple) embedded connector runtimes from within the JVM using the JUnit runner, see [this module](https://github.com/eclipse-edc/Connector/tree/main/system-tests/e2e-transfer-test/runner/src/test/java/org/eclipse/edc/test/e2e) and the [test runtime extension](https://github.com/eclipse-edc/Connector/blob/main/core/common/junit/src/main/java/org/eclipse/edc/junit/extensions/EdcRuntimeExtension.java) for reference. All tests except deployment tests should be implemented using this feature to offer an easily debuggable and maintainable test suite. We call this a "local first" mindset, because we primarily aim at running a test locally (possibly using manual setup of external services). Once we have that, we can execute the test in the same way on CI runners. It should not matter whether we run tests on developer machines or on CI runners.

> As a general rule of thumb, we should aim at running as much code from the JVM as opposed to: in external runtimes such as Docker or Kubernetes.

### Running tests in CI

External systems such as databases or identity providers should be setup "out-of-band" of the test, using a script or the CI pipeline's declarative syntax (e.g. GitHub Actions' `services` feature). If possible, we should employ external systems in a self-contained way, e.g. using docker containers, because that increases portability and decreases the potential for conflict, e.g. in always-on databases.

### DO:

- use integration tests sparingly and only when unit tests are not practical
- deploy the external system as service directly in the workflow or
- use a dedicated always-on test instance if provisioning is complicated and time-consuming (e.g. CosmosDB)
- adopt a local-first mindset, i.e. aim at running test code inside the JVM.
- take into account that external systems might experience transient failures or have degraded performance, so test
  methods should have a timeout so as not to block the runner indefinitely.
- use randomized strings for things like database/table/bucket/container names, etc., especially when the external
  system does not get destroyed after the test.
- use the class annotations provided by EDC to categorize and configure test execution

### DO NOT:

- try to cover everything with integration tests. It's typically a code smell if there are no corresponding unit tests
  for an integration test.
- slip into a habit of testing the external system rather than your usage of it
- store secrets directly in the code. Github will warn about that.
- perform complex external system setup in @BeforeEach or @BeforeAll
- write tests that are opaque, or can only run in certain enviroments
- use Test Containers, as it violates the separation of concerns. If external systems are needed, it should be an integration test.

## Test execution strategies

This section explains _at which point in time_ we should execute which test. This is intended to minimize the impact on overall test execution time on CI, while still maintaining sufficient coverage.

| Test type              | When to run                                                                         | Remarks |
| ---------------------- | ----------------------------------------------------------------------------------- | ------- |
| Unit test              | when running tests locally, without any parameters, on every commit on every branch |         |
| Integration test       | on every commit on every branch                                                     |         |
| System/End-To-End test | on pull request branches except when marked as `draft`                              |         |
| Deployment test        | before merging pull requests and on every commit on `develop`                       |         |
| Performance test       | Only on a specific schedule, e.g. once per day or week                              |         |
