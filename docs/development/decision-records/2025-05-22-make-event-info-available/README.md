# Make event information available in basic logging & monitoring stack

## Decision

A small extension will be added to the Tractus-X connector which subscribes to all internal events 
and publishes the information directly to an OpenTelemetry collector instance via http.
The extension can be switched of by configuration. By default it is disabled.

## Rationale

The Tractus-X connector has an embedded event mechanism that informs potential listeners about specific incidents, 
like finalized contract negotiations or ongoing transfers. 
It should be possible to forward these events to logging & monitorings stacks
to provide the information for further processing, e.g., for metric calculation or dashboards.
Using an OpenTelemetry collector instance is also agnostic to the concrete logging & monitoring stack used.

## Approach

1. Create a lightweight configurable extension which subscribes to all events.
2. By default, the events are published via http directly to the OpenTelemetry collector instance ( more info see here:
 https://github.com/open-telemetry/opentelemetry-collector/blob/main/receiver/otlpreceiver/README.md ),
 which needs to be provided by the surrounding infrastructure.
3. The extension will be shipped with the existing Tractus-X connector distributions,
but will be disabled by default by means of connector configuration.
With this, potential problems regarding performance of the connector are not a concern,
only if the additional information is really needed.

