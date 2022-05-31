# Chart Linting

Chart linting is performed using [helm's CT tool](https://github.com/helm/chart-testing).

Configuration files for [CT](../../ct.yaml), [Yamale](../../chart_schema.yaml) and [Yamllint](../../lintconf.yaml) have been provided. 

# Generate Chart Readme's

To generate chart README.md files from its respective values.yaml file we use the [helm-docs tool](https://github.com/norwoodj/helm-docs):

```shell
docker run --rm --volume "$(pwd):/helm-docs" -u $(id -u) jnorwood/helm-docs:v1.10.0
```
