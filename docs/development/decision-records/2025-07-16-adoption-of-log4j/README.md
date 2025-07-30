# Introduction of Log4j Logging Framework

## Decision

TractusX-EDC will adopt Log4j as the primary logging framework to replace the current `ConsoleMonitor` implementation.

## Rationale

The current logging implementation uses a simple `ConsoleMonitor` class that implements the EDC Monitor interface.
While this approach works for basic console output, it lacks some advanced functionality that is standard in modern
logging frameworks. Additionally, to effectively integrate with the existing OpenTelemetry collector, Loki,
and Grafana infrastructure used in the Tractus-X project we need a flexible solution that can easily accomodate future
requirements.

Log4j2 was the chosen framework due to the following advantages:

1. **Structured Logging**: Native support for JSON output format enabling machine-readable logs
2. **Performance**: Asynchronous logging capabilities with minimal impact on application performance
3. **Flexibility**: Multiple appenders (console, file, network) and configurable output destinations
4. **Log Level Management**: Fine-grained control over log levels and filtering across different components
5. **OpenTelemetry Integration**: Seamless correlation between logs and traces for comprehensive observability
6. **Industry Standard**: Mature, well-documented framework with extensive community support and tooling
7. **Configuration Management**: External configuration files allowing runtime adjustments without code changes
8. **Open Source**: Log4j is an open-source project distributed under the Apache License 2.0.

Although the ConsoleMonitor implementation could be extended to support future requirements,
it was decided to instead adopt an existing logging framework.

This aligns with the logging strategy outlined in the decision record `2025-07-09-logging_and_metrics_strategy`,
which emphasizes providing log information in a structured way as JSON objects for processing by background services.

## Approach

1. Create a new extension of the EDC Monitor interface that uses the Log4j2 API.
1. Add Log4j2 dependency to the project's Gradle build configuration
2. Create Log4j2 configuration files that output structured JSON logs
3. Create Log4j2 configuration file that outputs logs to the console in a similar format to the existing ConsoleMonitor
   and use it as the default logging configuration.

The implementation shall maintain backward compatibility with the EDC Monitor interface as much as possible, by
providing a similar level of information in the console output.