# Event Subscriber Extension
 
The goal of this extension is to provide ability to send domain event's informations directly to the OTEL Collector, bypassing custom logger. 
This extension sends the data using HTTP Protocol.

To turn on this optional extension `tx.edc.otel.events.subscriber.active` setting must be set to true.
Additional configuration is to provide `tx.edc.otel.service.name` in order to be able to distinguish in tooling like grafana from which service business event came from.
Setting `tx.edc.otel.events.endpoint` is to an http url of opentelemetry collector logs endpoint. In default [Umbrella observability deployment value file](https://github.com/eclipse-tractusx/tractus-x-umbrella/blob/main/charts/umbrella/values-adopter-data-exchange-observability.yaml) this value is `http://umbrella-opentelemetry-collector.umbrella:4318/v1/logs`

Data structure contained in `otelutils` package reflects official [OTEL documentation](https://github.com/open-telemetry/opentelemetry-proto/blob/main/examples/logs.json) (by the time of writing)

State of logs.json file (2025-07-11)

```json
{
  "resourceLogs": [
    {
      "resource": {
        "attributes": [
          {
            "key": "service.name",
            "value": {
              "stringValue": "my.service"
            }
          }
        ]
      },
      "scopeLogs": [
        {
          "scope": {
            "name": "my.library",
            "version": "1.0.0",
            "attributes": [
              {
                "key": "my.scope.attribute",
                "value": {
                  "stringValue": "some scope attribute"
                }
              }
            ]
          },
          "logRecords": [
            {
              "timeUnixNano": "1544712660300000000",
              "observedTimeUnixNano": "1544712660300000000",
              "severityNumber": 10,
              "severityText": "Information",
              "traceId": "5B8EFFF798038103D269B633813FC60C",
              "spanId": "EEE19B7EC3C1B174",
              "body": {
                "stringValue": "Example log record"
              },
              "attributes": [
                {
                  "key": "string.attribute",
                  "value": {
                    "stringValue": "some string"
                  }
                },
                {
                  "key": "boolean.attribute",
                  "value": {
                    "boolValue": true
                  }
                },
                {
                  "key": "int.attribute",
                  "value": {
                    "intValue": "10"
                  }
                },
                {
                  "key": "double.attribute",
                  "value": {
                    "doubleValue": 637.704
                  }
                },
                {
                  "key": "array.attribute",
                  "value": {
                    "arrayValue": {
                      "values": [
                        {
                          "stringValue": "many"
                        },
                        {
                          "stringValue": "values"
                        }
                      ]
                    }
                  }
                },
                {
                  "key": "map.attribute",
                  "value": {
                    "kvlistValue": {
                      "values": [
                        {
                          "key": "some.map.key",
                          "value": {
                            "stringValue": "some value"
                          }
                        }
                      ]
                    }
                  }
                }
              ]
            }
          ]
        }
      ]
    }
  ]
}
```


