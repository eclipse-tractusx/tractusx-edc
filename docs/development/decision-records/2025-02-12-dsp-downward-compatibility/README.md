# Handling of downward compatibility for a DSP version

## Decision

For a given DSP protocol version *N*, there is no restriction concerning backward compatibility towards the previous
versions (*N-1*) implementation of the DSP protocol.

During the support of a certain DSP version *N*, the implementation has to be strictly backward compatible, i.e.,
for two successive connector versions *X-1* and *X* that support both the DSP version *N*, if one participant changes
from connector version *X-1* to version *X*, another dataspace participant using any compatible connector version and
third party tools that use this connector can continue to transfer data to and from that participant without the need
to do any changes in his setup.

## Rationale

In general, the breaking change requirement of Catena-X has to be fulfilled as expressed in the
[Operating model](https://catenax-ev.github.io/docs/operating-model/how-life-cycle-management). The requirement states
that a dataspace participant can continuously exchange data with another particpant without the need to be forced to
update his system due to a change of the other participants service stack. This requires that data sent between
consumer and provider must not change in a breaking fashion. Additions to a data model are in general not seen as
breaking change.

As a dataspace participant is able to control the connector version to use and thereby the DSP version offered in the
dataspace independently from other participants, there is no need for backward compatibility constraints between the
implementations of different DSP versions.

## Approach

- The compatibility tests are enriched by tests that ensure the detection of breaking changes in relevant data models
  defined by the DSP protocol.
  This is achieved by tests which compare the returned data models to an expected result of the data model. There are
  two options to use for the comparison of exchanged data models:
  - For simpler data models, the expected data model is a handcrafted instance of the model that contains the minimum
    properties expected and the comparison checks that the returned data model is a superset of the expected model.
  - For more complex data models that are build deterministically, the expected result is recorded in an initial test
    run and later test executions are compared to the recorded model. If a mismatch occurs, the returned data model
    is compared to the expectation and if the change is reasonable the expected data model is recorded again to reflect
    the now relevant changes.
