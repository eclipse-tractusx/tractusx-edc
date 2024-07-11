# Management Domains Primer

> Disclaimer: this is an incubating feature that comes without any guarantees of any sort. Changes and even complete
> removal are possible without prior notice. Due to its experimental nature, this feature is disabled by default.

Management Domains are a way to reflect a company's internal organizational structure in the deployment of several
connectors. For details please refer to
the [official documentation](https://github.com/eclipse-edc/Connector/blob/main/docs/developer/management-domains/management-domains.md).

## Usage in Tractus-X

There are several reasons why a company might consider the use of Management Domains:

- independent management of connector instances: multiple departments within a (larger) company want to maintain
  independence w.r.t. their data, so they operate individual EDCs
- independent version upgrade cycles of connector instances: multiple departments may choose to upgrade their EDCs at
  different intervals or velocities. **Note that this only refers to minor changes of APIs, SPIs, configuration etc. All
  instances must still maintain protocol (DSP, DCP,...) compatibility!**

For the purposes of Tractus-X, the usage of
deployment [type 2b](https://github.com/eclipse-edc/Connector/blob/main/docs/developer/management-domains/management-domains.md#type-2b-edc-catalog-server-and-controldata-plane-runtimes)
or [type 2c](https://github.com/eclipse-edc/Connector/blob/main/docs/developer/management-domains/management-domains.md#type-2c-catalog-servercontrol-plane-with-data-plane-runtime)
is assumed:

![type 2b](https://github.com/eclipse-edc/Connector/blob/main/docs/developer/management-domains/distributed.type2.b.svg)

Note that is possible to use a conventional Tractus-X EDC runtime as catalog server.

### Limitations and Caveats

All runtimes within one company share the same `participantId`, thus they are one single logical entity. They must share
the same set of VerifiableCredentials. In practice, they could either share one credential wallet instance, or have
multiple identical instances.

## The Federated Catalog crawler

The Federated Catalog crawler subsystem periodically scrapes target nodes (i.e. catalog servers or EDC runtimes) by
executing a catalog request. If it encounters an `Asset`, that points to another `Asset`, it will automatically recurse
down by following the link, thus building a hierarchical catalog. In other words, there can "Catalogs of Catalogs". A
special [asset type](https://github.com/eclipse-edc/Connector/blob/main/docs/developer/management-domains/management-domains.md#31-asset-and-dataset-specialization)
was introduced for this purpose.

Every target node produces one `Catalog`, so in the end there is a `List<Catalog>` which contains all the assets that
are available in a particular dataspace.

## The Federated Catalog QueryApi

After some time, when all crawlers have returned, this list of catalogs can be queried using a new REST endpoint:

```shell
POST /v1alpha/catalog/query 
{
    "@context": {
        "edc": "https://w3id.org/edc/v0.0.1/ns/"
    },
    "@type": "QuerySpec"
}
```

the response body contains a list of catalogs as JSON-LD array (`hasPolicy` omitted for legibility). Notice
the `@type: "dcat:Catalog`
of the first `dataset` entry. This indicates that the "outer" Catalog actually contains another Catalog:

```json
[
  {
    "@id": "a6574324-8dd2-4169-adf1-f94423c5d213",
    "@type": "dcat:Catalog",
    "dcat:dataset": [
      {
        "@id": "1af92996-0bb7-4bdd-b04e-938fe54fb27f",
        "@type": "dcat:Catalog",
        "dcat:dataset": [
          {
            "@id": "asset-2",
            "@type": "dcat:Dataset",
            "odrl:hasPolicy": {
            },
            "dcat:distribution": [],
            "id": "asset-2"
          },
          {
            "@id": "asset-1",
            "@type": "dcat:Dataset",
            "odrl:hasPolicy": {
            },
            "dcat:distribution": [],
            "id": "asset-1"
          }
        ],
        "dcat:distribution": [],
        "dcat:service": {
          "@id": "684635d3-acc8-4ff5-ba71-d1e968be5e3b",
          "@type": "dcat:DataService",
          "dcat:endpointDescription": "dspace:connector",
          "dcat:endpointUrl": "http://localhost:8192/api/dsp",
          "dct:terms": "dspace:connector",
          "dct:endpointUrl": "http://localhost:8192/api/dsp"
        },
        "dspace:participantId": "did:web:localhost%3A7093",
        "participantId": "did:web:localhost%3A7093"
      }
    ],
    "dcat:distribution": [],
    "dcat:service": {
      "@id": "8c99b5d6-0c46-455e-97b1-7b31f32a714b",
      "@type": "dcat:DataService",
      "dcat:endpointDescription": "dspace:connector",
      "dcat:endpointUrl": "http://localhost:8092/api/dsp",
      "dct:terms": "dspace:connector",
      "dct:endpointUrl": "http://localhost:8092/api/dsp"
    },
    "dspace:participantId": "did:web:localhost%3A7093",
    "originator": "http://localhost:8092/api/dsp",
    "participantId": "did:web:localhost%3A7093"
  }
]
```

There is an additional optional query param `?flatten=true` that puts all `dataset` objects in a flat list for
linear consumption. Note that the hierarchy and provenance of a single `dataset` can't be restored anymore.

## Implementation guidance

Under the hood, a Tractus-X EDC connector leverages
the [Federated Catalog](https://github.com/eclipse-edc/FederatedCatalog/) and its crawler mechanism to periodically
scrape the dataspace. Note that without additional and explicit configuration, this feature is **disabled**
out-of-the-box!

There are several steps a consumer EDC needs to take before being able to use it:

### Create `CatalogAssets`

`CatalogAssets` are assets that point to another catalog using hyperlinks. They can be thought of as pointers to another
catalog. A catalog server (on the provider side) creates `CatalogAsset` via the Management API by using the following
request body:

```json
{
  "@id": "linked-asset-1",
  "@type": "CatalogAsset",
  "properties": {
    "description": "This is a linked asset that points to another EDC's catalog."
  },
  "dataAddress": {
    "@type": "DataAddress",
    "type": "HttpData",
    "baseUrl": "https://another-edc.com/api/dsp"
  }
}
```

### Enable and configure the crawler subsystem

The following config values are used to configure the crawlers:

| Configuration property                       | Helm value                                  | default value | description                                                        |
|----------------------------------------------|---------------------------------------------|---------------|--------------------------------------------------------------------|
| `edc.catalog.cache.execution.enabled`        | `controlplane.catalog.enabled`              | false         | enables/disables periodic crawling                                 |
| `edc.catalog.cache.execution.period.seconds` | `controlplane.catalog.crawler.period`       | 60            | period between two crawl runs                                      |
| `edc.catalog.cache.execution.delay.seconds`  | `controlplane.catalog.crawler.initialDelay` | random        | initial delay before the first crawl run                           |
| `edc.catalog.cache.partition.num.crawlers`   | `controlplane.catalog.crawler.num`          | 2             | desired number of crawlers                                         |
| `web.http.catalog.port`                      | `controlplane.endpoints.catalog.port`       | 8085          | port of the catalog QueryApi's web context                         |
| `web.http.catalog.path`                      | `controlplane.endpoints.catalog.path`       | /catalog      | URL path of the catalog QueryApi's web context                     |
| `tx.edc.catalog.node.list.file`              | `controlplane.catalog.crawler.targetsFile ` |               | path to a JSON file that contains an array of `TargetNode` objects |

all of these config values are optional and come preconfigured with defaults.

### Configure the target nodes

The crawler subsystem needs a list of `TargetNode` objects, which it obtains from the `TargetNodeDirectory`. Currently,
for testing purposes, there is a file-based implementation. To use it, a JSON file is needed that contains an array
of `TargetNode` objects:

```json
[
  {
    "name": "test-1",
    "url": "https://nodes.com/test-1/api/dsp",
    "id": "1",
    "supportedProtocols": "dataspace-protocol-http"
  },
  {
    "name": "test-2",
    "url": "https://nodes.com/test-2/api/dsp",
    "id": "2",
    "supportedProtocols": "dataspace-protocol-http"
  }
]
```

On Kubernetes, a common way to achieve this is using ConfigMaps.

## References

- [Management Domains
  documentation](https://github.com/eclipse-edc/Connector/blob/main/docs/developer/management-domains/management-domains.md)
- [Federated Catalog
  documentation](https://github.com/eclipse-edc/FederatedCatalog/tree/main/docs/developer/architecture)