# Make event information available in basic logging & monitoring stack

## Decision

A small extension (disabled by default) will be added to the TractusX-EDC which subscribes to all internal events and publishes the information directly to an opentelemetry collector instance via http. 

## Rationale

The Tractus-X EDC has an embedded event mechanism that informs potential listeners about specific incidents, like finalized contract negotiations or ongoing transfers. These events should be routed to a standard logging & monitoring stack to provide the information for further processing, e.g., in Dashboards.
Using an opentelemetry collector instance is also agnostic to the concrete logging & monitoring stack used.



## Approach

1. Create a lightweight configurable extension which subscribes to all events.
2. By default, the events are published via http directly to the opentelemetry collector, more info see here: https://github.com/open-telemetry/opentelemetry-collector/blob/main/receiver/otlpreceiver/README.md
3. The extension is disabled by default, so that eventual problems with performance of the EDC are not a concern, only if the additional information is really needed

