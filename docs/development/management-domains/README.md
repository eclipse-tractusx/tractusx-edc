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
are available in a particular dataspace. Like so, after the Federated Catalog Crawler retrieves all catalogs, an aggregation of all is made and one single root catalog is exposed, similar to a Catalog Server without CatalogAssets.

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

As mentioned, the returned catalog can be presented in Flat or Hierarchical structures. The latter presents the catalogs hierarchical structure, for example, having a dataset that in itself contains a catalog dataset (that can also contain a catalog and so on) can be clearly understood by the Hierarchical structure. The Flat structure streamlines this view by presenting all datasets from all catalogs at the same level.

To retrieve the flatten structure, include the ```flatten=true``` query param in the previous request.

The following shows an example of a hierarchical structure response followed by a flatten structure response example.

<details>
  <summary>Hierarchical structure response example</summary>

```json
[
  {
    "@id": "f3521137-49dd-443c-9c04-ef945dfd3b1a",
    "@type": "dcat:Catalog",
    "dcat:dataset": [
      {
        "@id": "f3521137-49dd-443c-9c04-ef945dfd3b1c",
        "@type": "dcat:Catalog",
        "dcat:dataset": [
          {
            "@id": "f3521137-49dd-443c-9c04-ef945dfd3b1d",
            "@type": "dcat:Dataset",
            "odrl:hasPolicy": [],
            "dcat:distribution": [
              {
                "@type": "dcat:Distribution",
                "dct:format": {
                  "@id": "HttpData-PULL"
                },
                "dcat:accessService": {
                  "@id": "53b87f7a-dd80-4461-90ae-d7badcb392fc",
                  "@type": "dcat:DataService",
                  "dcat:endpointDescription": "dspace:connector",
                  "dcat:endpointUrl": "http://provider-control-plane:8282/api/v1/dsp"
                }
              }
            ]
          },
          {
            "@id": "f3521137-49dd-443c-9c04-ef945dfd3b1d",
            "@type": "dcat:Catalog",
            "dcat:dataset": [
              {
                "@id": "f3521137-49dd-443c-9c04-ef945dfd3b1d",
                "@type": "dcat:Dataset",
                "odrl:hasPolicy": [],
                "dcat:distribution": [
                  {
                    "@type": "dcat:Distribution",
                    "dct:format": {
                      "@id": "HttpData-PUSH"
                    },
                    "dcat:accessService": {
                      "@id": "53b87f7a-dd80-4461-90ae-d7badcb392fc",
                      "@type": "dcat:DataService",
                      "dcat:endpointDescription": "dspace:connector",
                      "dcat:endpointUrl": "http://provider-control-plane:8282/api/v1/dsp"
                    }
                  }
                ]
              },
              {
                "@id": "f3521137-49dd-443c-9c04-ef945dfd3b2d",
                "@type": "dcat:Dataset",
                "odrl:hasPolicy": [],
                "dcat:distribution": [
                  {
                    "@type": "dcat:Distribution",
                    "dct:format": {
                      "@id": "HttpData-PULL"
                    },
                    "dcat:accessService": {
                      "@id": "53b87f7a-dd80-4461-90ae-d7badcb392fc",
                      "@type": "dcat:DataService",
                      "dcat:endpointDescription": "dspace:connector",
                      "dcat:endpointUrl": "http://provider-control-plane:8282/api/v1/dsp"
                    }
                  }
                ]
              }
            ],
            "dcat:distribution": [],
            "dcat:service": {
              "@id": "53b87f7a-dd80-4461-90ae-d7badcb392fc",
              "@type": "dcat:DataService",
              "dcat:endpointDescription": "dspace:connector",
              "dcat:endpointUrl": "http://provider-control-plane:8282/api/v1/dsp"
            },
            "dspace:participantId": "BPNL000000000001"
          }
        ],
        "dcat:distribution": [],
        "dcat:service": {
          "@id": "53b87f7a-dd80-4461-90ae-d7badcb392fc",
          "@type": "dcat:DataService",
          "dcat:endpointDescription": "dspace:connector",
          "dcat:endpointUrl": "http://provider-control-plane:8282/api/v1/dsp"
        },
        "dspace:participantId": "BPNL000000000001"
      }
    ],
    "dcat:distribution": [],
    "dcat:service": {
      "@id": "53b87f7a-dd80-4461-90ae-d7badcb392fc",
      "@type": "dcat:DataService",
      "dcat:endpointDescription": "dspace:connector",
      "dcat:endpointUrl": "http://provider-control-plane:8282/api/v1/dsp"
    },
    "dspace:participantId": "BPNL000000000001",
    "@context": {
      "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
      "edc": "https://w3id.org/edc/v0.0.1/ns/",
      "odrl": "http://www.w3.org/ns/odrl/2/",
      "dcat": "http://www.w3.org/ns/dcat#",
      "dct": "http://purl.org/dc/terms/",
      "dspace": "https://w3id.org/dspace/v0.8/"
    }
  }
]
```

</details>

Notice the `@type: "dcat:Catalog` of the first `dataset` entry. This indicates that the "outer" Catalog actually contains another Catalog:



<details>
  <summary>Flatten structure response example</summary>

```json
[
  {
    "@id": "f3521137-49dd-443c-9c04-ef945dfd3b1a",
    "@type": "dcat:Catalog",
    "dcat:dataset": [
      {
        "@id": "f3521137-49dd-443c-9c04-ef945dfd3b1d",
        "@type": "dcat:Dataset",
        "odrl:hasPolicy": [],
        "dcat:distribution": [
          {
            "@type": "dcat:Distribution",
            "dct:format": {
              "@id": "HttpData-PULL"
            },
            "dcat:accessService": {
              "@id": "53b87f7a-dd80-4461-90ae-d7badcb392fc",
              "@type": "dcat:DataService",
              "dcat:endpointDescription": "dspace:connector",
              "dcat:endpointUrl": "http://provider-control-plane:8282/api/v1/dsp"
            }
          }
        ]
      },
      {
        "@id": "f3521137-49dd-443c-9c04-ef945dfd3b1d",
        "@type": "dcat:Dataset",
        "odrl:hasPolicy": [],
        "dcat:distribution": [
          {
            "@type": "dcat:Distribution",
            "dct:format": {
              "@id": "HttpData-PUSH"
            },
            "dcat:accessService": {
              "@id": "53b87f7a-dd80-4461-90ae-d7badcb392fc",
              "@type": "dcat:DataService",
              "dcat:endpointDescription": "dspace:connector",
              "dcat:endpointUrl": "http://provider-control-plane:8282/api/v1/dsp"
            }
          },
          {
            "@id": "f3521137-49dd-443c-9c04-ef945dfd3b2d",
            "@type": "dcat:Dataset",
            "odrl:hasPolicy": [],
            "dcat:distribution": [
              {
                "@type": "dcat:Distribution",
                "dct:format": {
                  "@id": "HttpData-PULL"
                },
                "dcat:accessService": {
                  "@id": "53b87f7a-dd80-4461-90ae-d7badcb392fc",
                  "@type": "dcat:DataService",
                  "dcat:endpointDescription": "dspace:connector",
                  "dcat:endpointUrl": "http://provider-control-plane:8282/api/v1/dsp"
                }
              }
            ]
          }
        ]
      }
    ],
    "dcat:distribution": [],
    "dcat:service": [
      {
        "@id": "53b87f7a-dd80-4461-90ae-d7badcb392fc",
        "@type": "dcat:DataService",
        "dcat:endpointDescription": "dspace:connector",
        "dcat:endpointUrl": "http://provider-control-plane:8282/api/v1/dsp"
      },
      {
        "@id": "53b87f7a-dd80-4461-90ae-d7badcb392fc",
        "@type": "dcat:DataService",
        "dcat:endpointDescription": "dspace:connector",
        "dcat:endpointUrl": "http://provider-control-plane:8282/api/v1/dsp"
      },
      {
        "@id": "53b87f7a-dd80-4461-90ae-d7badcb392fc",
        "@type": "dcat:DataService",
        "dcat:endpointDescription": "dspace:connector",
        "dcat:endpointUrl": "http://provider-control-plane:8282/api/v1/dsp"
      },
      {
        "@id": "53b87f7a-dd80-4461-90ae-d7badcb392fc",
        "@type": "dcat:DataService",
        "dcat:endpointDescription": "dspace:connector",
        "dcat:endpointUrl": "http://provider-control-plane:8282/api/v1/dsp"
      }
    ],
    "dspace:participantId": "BPNL000000000SMT",
    "@context": {
      "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
      "edc": "https://w3id.org/edc/v0.0.1/ns/",
      "odrl": "http://www.w3.org/ns/odrl/2/",
      "dcat": "http://www.w3.org/ns/dcat#",
      "dct": "http://purl.org/dc/terms/",
      "dspace": "https://w3id.org/dspace/v0.8/"
    }
  }
]
```
</details>


## Implementation guidance

Under the hood, a Tractus-X EDC connector leverages
the [Federated Catalog](https://github.com/eclipse-edc/FederatedCatalog/) and its crawler mechanism to periodically
scrape the dataspace. Note that without additional and explicit configuration, this feature is **disabled**
out-of-the-box!

There are several steps a consumer EDC needs to take before being able to use it:

### Crawl `CatalogAssets`

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
A Catalog Server can have CatalogAssets in addition to other "common" Assets.
On the consumer side these should be crawled recursively and all catalogs jointed, forming the Federated Catalog.

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

all of these config values are optional and come preconfigured with defaults, except for `tx.edc.catalog.node.list.file`.

To enable this feature, at least two properties must be updated to not use default values. These are the `edc.catalog.cache.execution.enabled` which must be enabled and `tx.edc.catalog.node.list.file` pointing to a TargetNodeDirectory (ex: local file with TargetNodes). Simply, the `TargetNodeDirectory` consists on a list containing all `TargetNodes` which can be subject to crawling (querying).

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

## Manage Access

Considering the [documented](https://github.com/eclipse-edc/Connector/blob/main/docs/developer/management-domains/management-domains.md#21-access-control) possibility of attach access policies to sub-catalogs (CatalogAssets) using contract definitions, the Catalog Server can confirm permissions of the client credentials.
An example of a CatalogAsset with dummy credentials.
```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "CatalogAsset",
  "@id": "catalog-asset-example-id",
  "properties": {
    "test": "some test"
  },
  "dataAddress": {
    "type": "HttpData",
    "@type": "DataAddress",
    "baseUrl": "https://example-edc.com/api/dsp",
    "credentials": "provided_credentials"
  }
}
```

Can be later be checked based on similar catalog response.
```json
[
  {
    "@id": "f3521137-49dd-443c-9c04-ef945dfd3b1a",
    "@type": "dcat:Catalog",
    "dspace:participantId": "BPNL000000000001",
    "isCatalog": true,
    "id": "catalog-asset-example-id",
    "test": "some test",
    "dcat:dataset": [
      {
        "@id": "catalog-asset-example-id",
        "@type": "dcat:Dataset",
        "odrl:hasPolicy": [
          {
            "@id": "",
            "@type": "odrl:Offer",
            "odrl:permission": {
              "odrl:action": {
                "@id": "USE"
              },
              "odrl:constraint": {
                "odrl:leftOperand": {
                  "@id": "credentials"
                },
                "odrl:operator": {
                  "@id": "odrl:eq"
                },
                "odrl:rightOperand": "provided_credentials"
              }
            },
            "odrl:prohibition": [],
            "odrl:obligation": []
          }
        ],
        "dcat:distribution": [
          {
            "@type": "dcat:Distribution",
            "dct:format": {
              "@id": "AzureStorage-PUSH"
            },
            "dcat:accessService": {
              "@id": "3eb13e90-f5ed-46f5-9287-99fca35a722c",
              "@type": "dcat:DataService",
              "dcat:endpointDescription": "dspace:connector",
              "dcat:endpointUrl": "https://some_edc/api/v1/dsp",
              "dct:terms": "dspace:connector",
              "dct:endpointUrl": "https://some_edc/api/v1/dsp",
            }
          }
        ]
      }
    ]
  }
]
```


## References

- [Management Domains
  documentation](https://github.com/eclipse-edc/Connector/blob/main/docs/developer/management-domains/management-domains.md)
- [Federated Catalog
  documentation](https://github.com/eclipse-edc/FederatedCatalog/tree/main/docs/developer/architecture)