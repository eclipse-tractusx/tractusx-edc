# Logging/Eventing/Metrics Strategy

The purpose of this decision record is to agree on the general strategy concerning how log information and domain
event information is processed in the connector and in background services that take the information and process
it further.

## Decision

1. The connector provides log information in a structured way in the form of json objects.
2. The technology to provide execution information towards background services is OpenTelemetry.
3. OpenTelemetry is added as direct dependency. The target is that the usage of the java agent is abandonned.
4. The connector will not calculate domain metrics. Instead it will provide relevant domain events that are forwarded
   using OpenTelemetry as well.
5. An OpenTelemetry collector is the recommended way to catch and forward the provided information into a background
   operations stack that is then capable to process the information, generate metrics out of it and present them
   adequately for the different user groups.

## Rationale

There is a large and basically individual number of metrics that are of interest for a company operating a connector.
In addition, the calculation of metrics requires infrastructure that is already provided by typical operation stacks.
As a consequence, it makes sense to just provide the incident information to external services in order to keep
complexity out of the connector and to provide flexibility for an operator to manage the connector according to
the local requirements.

To allow easy processing of the information, a structured output allows to use the data directly without the need
for reengineering the meaning of the information.

There was an agreement by the involved parties, that OpenTelemetry is the right technology to provide incident
information to background services that manage operations. But the usage of the java agent is consuming a
high amount of resources, that is why the SDK approach of using OpenTelemetry was preferred.

## Approach

The concrete changes required to meet the requirements of this decision record are part of further records looking
into single changes needed. The goal here is to bring the different changes into an overall context.
