# Invoke Business-Tests via Maven

```shell
./mvnw -pl edc-tests -Pbusiness-tests -pl edc-tests test -Dtest=org.eclipse.tractusx.edc.tests.features.RunCucumberTest
```

# Test locally using Act Tool

> "Think globally, [`act`](https://github.com/nektos/act) locally"

```shell
act -j business-test
```