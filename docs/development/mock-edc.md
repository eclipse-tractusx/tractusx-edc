# Using the Mock-Connector for contract-based testing

Modern testing methodologies are based on small, independent units of code that have a defined behaviour.
Implementations as well as testing should be fast, repeatable, continuous and easily maintainable. In the context of EDC
that means, that downstream projects that are based on EDC should not need to run a fully-fledged connector runtime to
test their workflows. While the Tractus-X EDC project did provide a pure in-memory runtime for testing, that still
requires all the configuration and a complex runtime environment to work, which may be a high barrier of entry.

For this reason, and to developers who primarily interact with the Management API of a connector, the Tractus-X EDC
project provides a testing framework with an even smaller footprint called the "Mock-Connector". It is a Docker image, that
contains just the Management API plus an instrumentation interface to enable developers to use this in their
unit/component testing and in continuous integration.

We call this "contract-based testing", as it defines the specified behaviour of an application (here: the connector).
The Mock-Connector's Management API is guaranteed to behave exactly the same, in fact, it even runs the
same code as a "real" EDC.

## 1. The contract

The [Management API spec](https://eclipse-edc.github.io/Connector/openapi/management-api/).

### 1.1 Definition of terms

- connector: runnable Java application that contains Tractus-X modules. Also referred to as: EDC, runtime, tx-edc
- mock: a replacement for a collaborator object (class, component, application), where the behaviour can be
  controlled
- stub: very similar to a mock, but while a mock oftentimes is a drop-in _replacement_, a stub would be a
  re-implementation with a fixed behaviour. Also referred to as: dummy
- instrumentation: the process of setting up a mock to behave a certain way. Also referred to as priming the mock.

## 2. Intended audience

Developers who build their applications and systems based on EDC, and interact with EDC through the Management API can
use the Mock-Connector to decrease friction by not having to spin up and configure a fully-fledged connector runtime.

Developers who plan to work with (Tractus-X) EDC in another way, like directly using its Maven artifacts, or even by
implementing a DSP protocol head are kindly redirected to
the [additional references section](#5-references-and-further-reading).

## 3. Use with TestContainers

Mock-Connector should be used as Docker image, we publish it as `tractusx/edc-mock`.

Using the Mock-Connector is very easy, we recommend usage via Testcontainers. For example, setting up a JUnit test for a
client application using Testcontainers could be done as follows:

```java

@Testcontainers
@ComponentTest
public class UseMockedEdcSampleTest {
    @Container
    protected static GenericContainer<?> edcContainer = new GenericContainer<>("tractusx/edc-mock:latest")
            .withEnv("WEB_HTTP_PORT", "8080")
            .withEnv("WEB_HTTP_PATH", "/api")
            .withEnv("WEB_HTTP_MANAGEMENT_PORT", "8081")
            .withEnv("WEB_HTTP_MANAGEMENT_PATH", "/api/management")
            .withExposedPorts(8080, 8081);
    private int managementPort;
    private int defaultPort;

    @BeforeEach
    void setup() {
        managementPort = edcContainer.getMappedPort(8081);
        defaultPort = edcContainer.getMappedPort(8080);
    }
}
```

This downloads and runs the Docker image for the Mock-Connector and supplies it with minimal configuration. Specifically, it
exposes the Management API and the default context, because that is needed to set up the mock.

> Please note that in
> the [example](../../samples/testing-with-mocked-edc/src/test/java/org/eclipse/tractusx/edc/samples/mockedc/UseMockedEdcSampleTest.java),
> the image name is `mock-edc` - that is because in our CI testing we build the image and then run the tests, so we
> can't use the official image.

### 3.1 Running a simple positive test

Executing a simple request against the Management API of EDC can be done like this:

```java

@Test
void test_getAsset() {
    //prime the mock - post a RecordedRequest
    setupNextResponse("asset.request.json");

    // perform the actual Asset API request. In a real test scenario, this would be the client code we're testing, i.e. the
    // System-under-Test (SuT).
    var assetArray = mgmtRequest()
            .contentType(ContentType.JSON)
            .body("""
                    {
                      "@context": {
                        "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
                      },
                    "@type": "QuerySpec"
                    }
                    """)
            .post("/v3/assets/request")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().as(JsonArray.class);

    // assert the response
    assertThat(assetArray).hasSize(1);
    assertThat(assetArray.get(0).asJsonObject().get("properties"))
            .hasFieldOrProperty("prop1")
            .hasFieldOrProperty("id")
            .hasFieldOrProperty("contenttype");
}
```

### 3.2 Running a test expecting a failure

```java

@Test
void test_apiNotAuthenticated_expect400() {
    //prime the mock - post a RecordedRequest
    setupNextResponse("asset.creation.failure.json");

    // perform the actual Asset API request. In a real test scenario, this would be the client code we're testing, i.e. the
    // System-under-Test (SuT).
    var assetArray = mgmtRequest()
            .contentType(ContentType.JSON)
            .body("""
                    {
                      "@context": {
                        "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
                      },
                    "@type": "QuerySpec"
                    }
                    """)
            .post("/v3/assets/request")
            .then()
            .log().ifError()
            .statusCode(400)
            .extract().body().as(JsonArray.class);

    // assert the response contains error information
    assertThat(assetArray).hasSize(1);
    var errorObject = assetArray.get(0).asJsonObject();
    assertThat(errorObject.get("message").toString()).contains("This user is not authorized, This is just a second error message");
}
```

Note that the difference here is that we prime the mock with a different JSON file (more on that later), we expect a
different HTTP response code, i.e. 400, and the response body contains an error object instead of an array of Assets.

## 4. Request pipeline and the instrumentation API

The Mock-Connector internally contains a pipeline of "recorded requests", much like mocked HTTP webservers, like WireMock or OkHttp MockWebServer. Out-of-the-box, that pipeline is empty, which means the Management API would always
respond with an error like the following:

```json
[
  {
    "message": "Failure: no recorded request left in queue.",
    "type": "InvalidRequest",
    "path": null,
    "invalidValue": null
  }
]
```

To get beyond that, we need to _prime_ the mock. That means, we need to tell it how to respond to the next request by
inserting a "recorded request" into its request pipeline. In previous code examples, this was done using
the `setupNextResponse()` method. Mock-Connector offers an instrumentation API which can be used to insert recorded requests,
to clear the queue and to get a count.

### 4.1 Recorded requests

A `RecordedRequest` is a POJO, that tells the Mock-Connector how to respond to the _next_ Management API request. To that end,
it contains the input parameter type, the data associated with it, plus the return value type plus - most importantly -
the data that is supposed to be returned.

Recall the [previous example](#31-running-a-simple-positive-test), which tests an Asset request. Thus, we have to prime
the mock such that it responds with a list of `Asset` objects. The semantic being: "on the next request, respond
with ...".

The contents of the [asset.request.json](../../samples/testing-with-mocked-edc/src/test/resources/asset.request.json)
contains a section that defines the `input`, which in this case is a `QuerySpec`, and the `output` is a list of `Asset`
objects. The `data` section must then contain serialized JSON that matches the `class` property. For instance,
the `data` section of the `input` must contain JSON that can be deserialized into an `Asset`.

> _Note that the information about input and output datatypes must currently be obtained from the aggregate services.
Here, that would be the `AssetService` interface. In future iterations there will be a more convenient way to obtain
that information._

> _Note that input argument type matching is currently not supported, it will come in future releases._

### 4.2 Instrumentation API

The instrumentation is done via a simple REST API:

```shell
GET /api/instrumentation/count  -> returns the number of requests in the queue
GET /api/instrumentation        -> returns the list of queued requests 
DELETE /api/instrumentation     -> clears the queue
POST /api/instrumentation       -> adds a new RecordedRequest, JSON must be in the request body
```

## 5. References and further reading

- A complete sample how to run a test using the Mock-Connector in a Testcontainer can be
  found [here](../../samples/testing-with-mocked-edc)
- To test compliance with DSP, use the [TCK](https://github.com/eclipse-dataspacetck/cvf)
- A Mock-IATP runtime is planned for future releases.

## 6. Future improvements

- matching requests to endpoints to allow for a "from-now-on" semantic
- introducing placeholders for domain objects to increase refactoring robustness
- abstract description of the endpoint's inputs and outputs, so developers don't need to know about service signatures
  anymore
- request input matching