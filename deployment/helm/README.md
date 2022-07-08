# Chart Linting

Chart linting is performed using [helm's CT tool](https://github.com/helm/chart-testing).

Configuration files for [CT](../../ct.yaml), [Yamale](../../chart_schema.yaml) and [Yamllint](../../lintconf.yaml) have been provided. 

# Generate Chart Readme's

To generate chart README.md files from its respective values.yaml file we use the [helm-docs tool](https://github.com/norwoodj/helm-docs):

```shell
docker run --rm --volume "$(pwd):/helm-docs" -u $(id -u) jnorwood/helm-docs:v1.10.0
```

# Confidential EDC Settings

Some EDC settings should better not be part of the actual deployment (like credentials to the database or the vault). Therefore, it is possible to deploy a secret with these confidential settings beforehand, and make it known to the deployment by setting the secret name in the `envSecretName` field of the deployment.