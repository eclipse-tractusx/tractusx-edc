# Log4J2 Monitor Extension

The Log4J2MonitorExtension provides a Log4J2-based implementation of the EDC Monitor interface, replacing the default
console-based logging with a more robust and configurable logging framework.
This extension acts as a wrapper for the Log4J2 API and integrates seamlessly with the EDC runtime and
is included by default in the tractusx-edc distributions.

## Default Configuration

By default, the `Log4J2MonitorExtension` provides console-based logging that maintains compatibility with the upstream
`ConsoleMonitor` behavior. The distribution is shipped with a default configuration file that can be found at
edc-extensions/log4j2-monitor/src/main/resources/log4j2.json.

The default setup works out-of-the-box when the extension is included in the EDC runtime, requiring no additional
configuration.

## Structured logging in Json Format with the Tractusx EDC Helm Charts

To enable structured JSON logging output, configure Log4J2 with the JsonTemplateLayout. An example yaml configuration
can be found in one of the existing helm charts. To enable JSON format:

```yaml
log4j2:
  enableJsonLogs: true
```

###  

The Tractusx EDC Helm charts include built-in support for JSON logging configuration. To enable JSON format:

```yaml
log4j2:
  enableJsonLogs: true
```

When `enableJsonLogs` is set to `true`, the Helm chart will automatically set up the necessary environment variable
that overrides the default configuration with a mounted configuration file.

### JSON Configuration Example

```yaml
Configuration:
  Appenders:
    Console:
      name: CONSOLE
      JsonTemplateLayout:
        eventTemplate: |-
          {
            "timestamp": {
              "$resolver": "timestamp",
              "pattern": {
                "format": "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS",
                "timeZone": "UTC"
              }
            },
            "level": {
              "$resolver": "level",
              "field": "severity",
              "severity": {
                "field": "keyword"
              }
            },
            "message": {
              "$resolver": "message"
            }
          }
  Loggers:
    Root:
      level: "OFF"
    Logger:
      name: org.eclipse.edc.monitor.logger
      level: DEBUG
      AppenderRef:
        ref: CONSOLE
```