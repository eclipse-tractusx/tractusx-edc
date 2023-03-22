# Tests

## Automated Business Test

In addition to unit and integration testing, our EDC releases undergo end-to-end testing.
You can run these business tests against your local environment via Maven:

```shell
./mvnw -pl edc-tests -Pbusiness-tests -pl edc-tests test -Dtest=net.catenax.edc.tests.features.RunCucumberTest
```

You can also run the entire test cycle via [ACT](https://github.com/nektos/act):

```shell
act -j business-test
```
