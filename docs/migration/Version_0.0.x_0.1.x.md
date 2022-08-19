# Migration Version 0.0.x to 0.1.x

This document contains a list of breaking changes that are introduced in version 0.1.x.

## 0. Summary

1. PostgreSQL Database
   1. Criteria in Policy & Contract Definitions Table
   2. Delete Contract Agreements 
2. Data Management API
   1. Policy Path
   2. Policy Payload
   3. Criteria in Payload of Contract Definitions and Policies
3. Connector Configuration
   1. Token Validation Endpoint Setting

## 1. PostgreSQL Database

The Product EDC [PostgreSQL Migration Extension](../../edc-extensions/postgresql-migration/README.md) is able to run
normal migrations. But the extension will never cause a data loss automatically, therefore part of this migration must
be done by the user itself.

### 1.1 Criteria in Policy & Contract Definitions Table

Criteria in Policies and Contract Definitions are serialized as JSON and put into the database. The Criteria schema
changed and already existing database entries will cause _NullPointerExceptions_.



<details>
  <summary>Example Exception</summary>

#### Example Exception

```
[2022-08-02 09:32:37] [SEVERE ] Could not handle multipart request: null
org.eclipse.dataspaceconnector.spi.EdcException
        at org.eclipse.dataspaceconnector.transaction.local.LocalTransactionContext.execute(LocalTransactionContext.java:70)
        at org.eclipse.dataspaceconnector.sql.assetindex.SqlAssetIndex.queryAssets(SqlAssetIndex.java:141)
        at org.eclipse.dataspaceconnector.sql.assetindex.SqlAssetIndex.queryAssets(SqlAssetIndex.java:134)
        at org.eclipse.dataspaceconnector.contract.offer.ContractOfferServiceImpl.lambda$queryContractOffers$2(ContractOfferServiceImpl.java:61)
        at java.base/java.util.stream.ReferencePipeline$7$1.accept(ReferencePipeline.java:271)
        at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:177)
        at java.base/java.util.LinkedList$LLSpliterator.forEachRemaining(LinkedList.java:1239)
        at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:484)
        at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:474)
        at java.base/java.util.stream.ReduceOps$ReduceOp.evaluateSequential(ReduceOps.java:913)
        at java.base/java.util.stream.AbstractPipeline.evaluate(AbstractPipeline.java:234)
        at java.base/java.util.stream.ReferencePipeline.collect(ReferencePipeline.java:578)
        at org.eclipse.dataspaceconnector.ids.core.service.CatalogServiceImpl.getDataCatalog(CatalogServiceImpl.java:55)
        at org.eclipse.dataspaceconnector.ids.core.service.ConnectorServiceImpl.getConnector(ConnectorServiceImpl.java:51)
        at org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.ConnectorDescriptionRequestHandler.handle(ConnectorDescriptionRequestHandler.java:74)
        at org.eclipse.dataspaceconnector.ids.api.multipart.handler.DescriptionHandler.handleRequestInternal(DescriptionHandler.java:117)
        at org.eclipse.dataspaceconnector.ids.api.multipart.handler.DescriptionHandler.handleRequest(DescriptionHandler.java:82)
        at org.eclipse.dataspaceconnector.ids.api.multipart.controller.MultipartController.request(MultipartController.java:146)
        at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
        at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
        at java.base/java.lang.reflect.Method.invoke(Method.java:566)
        at org.glassfish.jersey.server.model.internal.ResourceMethodInvocationHandlerFactory.lambda$static$0(ResourceMethodInvocationHandlerFactory.java:52)
        at org.glassfish.jersey.server.model.internal.AbstractJavaResourceMethodDispatcher$1.run(AbstractJavaResourceMethodDispatcher.java:124)
        at org.glassfish.jersey.server.model.internal.AbstractJavaResourceMethodDispatcher.invoke(AbstractJavaResourceMethodDispatcher.java:167)
        at org.glassfish.jersey.server.model.internal.JavaResourceMethodDispatcherProvider$ResponseOutInvoker.doDispatch(JavaResourceMethodDispatcherProvider.java:176)
        at org.glassfish.jersey.server.model.internal.AbstractJavaResourceMethodDispatcher.dispatch(AbstractJavaResourceMethodDispatcher.java:79)
        at org.glassfish.jersey.server.model.ResourceMethodInvoker.invoke(ResourceMethodInvoker.java:475)
        at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:397)
        at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:81)
        at org.glassfish.jersey.server.ServerRuntime$1.run(ServerRuntime.java:255)
        at org.glassfish.jersey.internal.Errors$1.call(Errors.java:248)
        at org.glassfish.jersey.internal.Errors$1.call(Errors.java:244)
        at org.glassfish.jersey.internal.Errors.process(Errors.java:292)
        at org.glassfish.jersey.internal.Errors.process(Errors.java:274)
        at org.glassfish.jersey.internal.Errors.process(Errors.java:244)
        at org.glassfish.jersey.process.internal.RequestScope.runInScope(RequestScope.java:265)
        at org.glassfish.jersey.server.ServerRuntime.process(ServerRuntime.java:234)
        at org.glassfish.jersey.server.ApplicationHandler.handle(ApplicationHandler.java:684)
        at org.glassfish.jersey.servlet.WebComponent.serviceImpl(WebComponent.java:394)
        at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:346)
        at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:358)
        at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:311)
        at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:205)
        at org.eclipse.jetty.servlet.ServletHolder.handle(ServletHolder.java:764)
        at org.eclipse.jetty.servlet.ServletHandler.doHandle(ServletHandler.java:508)
        at org.eclipse.jetty.server.handler.ScopedHandler.nextHandle(ScopedHandler.java:221)
        at org.eclipse.jetty.server.handler.ContextHandler.doHandle(ContextHandler.java:1375)
        at org.eclipse.jetty.server.handler.ScopedHandler.nextScope(ScopedHandler.java:176)
        at org.eclipse.jetty.servlet.ServletHandler.doScope(ServletHandler.java:463)
        at org.eclipse.jetty.server.handler.ScopedHandler.nextScope(ScopedHandler.java:174)
        at org.eclipse.jetty.server.handler.ContextHandler.doScope(ContextHandler.java:1297)
        at org.eclipse.jetty.server.handler.ScopedHandler.handle(ScopedHandler.java:129)
        at org.eclipse.jetty.server.handler.ContextHandlerCollection.handle(ContextHandlerCollection.java:192)
        at org.eclipse.jetty.server.handler.HandlerWrapper.handle(HandlerWrapper.java:122)
        at org.eclipse.jetty.server.Server.handle(Server.java:562)
        at org.eclipse.jetty.server.HttpChannel.lambda$handle$0(HttpChannel.java:505)
        at org.eclipse.jetty.server.HttpChannel.dispatch(HttpChannel.java:762)
        at org.eclipse.jetty.server.HttpChannel.handle(HttpChannel.java:497)
        at org.eclipse.jetty.server.HttpConnection.onFillable(HttpConnection.java:282)
        at org.eclipse.jetty.io.AbstractConnection$ReadCallback.succeeded(AbstractConnection.java:319)
        at org.eclipse.jetty.io.FillInterest.fillable(FillInterest.java:100)
        at org.eclipse.jetty.io.SelectableChannelEndPoint$1.run(SelectableChannelEndPoint.java:53)
        at org.eclipse.jetty.util.thread.strategy.AdaptiveExecutionStrategy.runTask(AdaptiveExecutionStrategy.java:412)
        at org.eclipse.jetty.util.thread.strategy.AdaptiveExecutionStrategy.consumeTask(AdaptiveExecutionStrategy.java:381)
        at org.eclipse.jetty.util.thread.strategy.AdaptiveExecutionStrategy.tryProduce(AdaptiveExecutionStrategy.java:268)
        at org.eclipse.jetty.util.thread.strategy.AdaptiveExecutionStrategy.produce(AdaptiveExecutionStrategy.java:190)
        at org.eclipse.jetty.util.thread.QueuedThreadPool.runJob(QueuedThreadPool.java:894)
        at org.eclipse.jetty.util.thread.QueuedThreadPool$Runner.run(QueuedThreadPool.java:1038)
        at java.base/java.lang.Thread.run(Thread.java:829)
Caused by: java.lang.NullPointerException
        at org.eclipse.dataspaceconnector.sql.translation.SqlConditionExpression.isValidExpression(SqlConditionExpression.java:53)
        at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:195)
        at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1655)
        at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:484)
        at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:474)
        at java.base/java.util.stream.ReduceOps$ReduceOp.evaluateSequential(ReduceOps.java:913)
        at java.base/java.util.stream.AbstractPipeline.evaluate(AbstractPipeline.java:234)
        at java.base/java.util.stream.ReferencePipeline.collect(ReferencePipeline.java:578)
        at org.eclipse.dataspaceconnector.sql.assetindex.schema.BaseSqlDialectStatements.createQuery(BaseSqlDialectStatements.java:108)
        at org.eclipse.dataspaceconnector.sql.assetindex.SqlAssetIndex.lambda$queryAssets$2(SqlAssetIndex.java:144)
        at org.eclipse.dataspaceconnector.transaction.local.LocalTransactionContext.execute(LocalTransactionContext.java:63)
        ... 69 more
```

</details>

<details>

  <summary>Solution 1: Update all Criteria manually</summary>

#### Update all Criteria manually

Root of this issue is that the operator, left- and right-operand Criteria field names changed.

| Old       | New          |
|:----------|:-------------|
| left      | operandLeft  |
| right     | operandRight |
| op        | operator     |

It is possible to resolve this issue by updating the content of the column, that contain JSON serialized constraints,
from

```
{"criteria":[{"left":"asset:prop:id","op":"=","right":"asset-1"}]}
```

to

```
{"criteria":[{"operandLeft":"asset:prop:id","operator":"=","operandRight":"asset-1"}]}
```

</details>

<details>

  <summary>Solution 2: Delete all rows containing Constraints</summary>

#### Delete all rows containing Criteria

Instead of updating each row in the database it's also possible to delete all Contract Definitions and Policies.
Additionally it's necessary to delete all Negotiations, as they might reference existing Contract Definitions and/or
Policies.

Theoretically it's also necessary to delete Contract Agreements. As their deletion is already described in another
section, we can skip them here.

**Required Queries**

```sql
DELETE
FROM edc_contract_negotiation;
```

```sql
DELETE
FROM edc_contract_definitions;
```

```sql
DELETE
FROM edc_policydefinitins;
```

</details>

### 1.2 Delete Contract Agreements

In the new version contract agreement rows contain a serialized policy at the time, the contract was concluded.
With the EDC update all existing Contract Agreements must be deleted.

<details>
    <summary>Required Query</summary>

```sql
DELETE
FROM edc_contract_agreement;
```

</details>

## 2. Data Management API

It might be necessary to update applications and scripts that use the Data Management API. This section covers the most
important changes in endpoints and payloads.

### 2.1 Policy Path

The Data Management API Path for Policies changes from
`/policies` to `/policydefinitions`.

<details>
  <summary>Example Call</summary>

#### Get All Policies

```bash
curl -X GET "${DATA_MGMT_ENDPOINT}/data/policydefinitions" --header "X-Api-Key: <key>" --header "Content-Type: application/json"
```

</details>

### 2.2 Policy Payload

The Policy Payload now wraps the policy details in an additional policy object.

<details>

<summary>Payload Comparison</summary>

**New Payload**

```json
{
  "uid": "1",
  "policy": {
    "prohibitions": [],
    "obligations": [],
    "permissions": []
  }
}
```

**Old Payload**

```json
{
  "uid": "1",
  "prohibitions": [],
  "obligations": [],
  "permissions": []
}
```

</details>

### 2.3 Criteria in Payload of Contract Definitions and Policies

The payload of a Policy or a Contract Definition may contain one or more Criteria. The format of these serialized Criteria changed.
Please note that there is no input validation, that detects errors when the old Criteria format is used!

<details>

<summary>Criterion Format Change</summary>

**Old Criterion Format**
```
{ "left": "asset:prop:id", "op": "=", "right": "1" }
```

**New Criterion Format**
```
{ "operandLeft": "asset:prop:id", "operator": "=", "operandRight": "1" }
```

**Example Call**

```bash
curl -X POST "${DATA_MGMT_ENDPOINT}/data/contractdefinitions" --header "X-Api-Key: <key>" --header "Content-Type: application/json" --data "{ \"id\": \"1\", \"criteria\": [ { \"operandLeft\": \"asset:prop:id\", \"operator\": \"=\", \"operandRight\": \"1\" } ], \"accessPolicyId\": \"1\", \"contractPolicyId\": \"1\" }"
```

</details>

### 2.4 Data Address

When using a Data Address of type `HttpData` please notice that the property `endpoint` changed to `baseUrl`. This property is mostly used when creating assets.

<details>

<summary>Example Call</summary>

```bash
curl -X POST "$PLATO_DATAMGMT_URL/data/assets" --header "X-Api-Key: password" --header "Content-Type: application/json" --data "{ \"asset\": { \"properties\": { \"asset:prop:id\": \"1\", \"asset:prop:description\": \"Product EDC Demo Asset\" } }, \"dataAddress\": { \"properties\": { \"type\": \"HttpData\", \"baseUrl\": \"https://jsonplaceholder.typicode.com/todos/1\" } } }" -s -o /dev/null -w 'Response Code: %{http_code}\n'
```

</details>

## 3. Connector Configuration

### 3.1 Token Validation Endpoint Setting

In the past the token validation endpoint was configured in `edc.controlplane.validation-endpoint`. This setting key
must be renamed to `edc.dataplane.token.validation.endpoint`.
