# Transfer Data

This document will showcase a data transfer between two connectors. It uses two connectors from the *All-in-one deployment* of this repository.

---

Before running the commands setup the all-in-one deployment from the. This is documented in it's
[README.md](../../edc-tests/src/main/resources/deployment/helm/all-in-one/README.md).

Please install [jq](https://stedolan.github.io/jq/), as it is used in the bash calls of this document.

---

For this transfer connector **Plato** will act as data provider, and connector **Sokrates** will act as data
consumer. But the roles could be inverse as well.

**Contents**

0. Before running the demo
    1. Ensure all pods are running
    2. Set environment variables
1. Setup Data Offer
2. Request Contract Offers
3. Negotiate Contract
4. Transfer Data
5. Verify Data Transfer

## 0. Before Running the demo

### 0.1 Wait until all pods are running

Get all the pods and wait until all pods are in a `Running` state before executing the next steps.
Please ignore that the EDC applications will crash 2-3 times during the start-up phase. This is normal.

**Run**

```bash
minikube kubectl -- -n edc-all-in-one get pods
```

### 0.2 Set environment variables used in subsequent calls

Initialize the following environment variables, that are used in the upcoming API calls.

**Run**

```bash
export PLATO_DATAMGMT_URL=$(minikube service plato-edc-controlplane -n edc-all-in-one --url | sed -n 3p)
export PLATO_IDS_URL="http://plato-edc-controlplane:8282"
export SOKRATES_DATAMGMT_URL=$(minikube service sokrates-edc-controlplane -n edc-all-in-one --url | sed -n 3p)
export SOKRATES_BACKEND_URL=$(minikube service sokrates-backend-application -n edc-all-in-one --url | sed -n 2p)
```

Please note: The IDS URL is used for DAPS Token Audience validation. Therefore it must be the internal IDS url, that is configured inside the connector.

## 1. Setup Data Offer

Set up a data offer in **Plato**, so that **Sokrates** has something to consume.

In case you are unfamiliar with the EDC terms `Asset`, `Policy` or `ContractDefinition` please have a look at the official open
source documentation ([link](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/blob/main/docs/developer/architecture/domain-model.md)).

![Sequence 1](diagrams/transfer_sequence_1.png)

**Run**


The following commands will create an Asset, a Policy and a Contract Definition.
For simplicity `https://jsonplaceholder.typicode.com/todos/1` is used as data source of the asset, but could be any
other API, that is reachable from the Provider Data Plane.

```bash
curl -X POST "$PLATO_DATAMGMT_URL/data/assets" \
    --header 'X-Api-Key: password' \
    --header 'Content-Type: application/json' \
    --data '{
             "asset": {
                "properties": {
                        "asset:prop:id": "1",
                        "asset:prop:description": "Product EDC Demo Asset"
                    }
                },
                "dataAddress": {
                    "properties": {
                        "type": "HttpData",
                        "baseUrl": "https://jsonplaceholder.typicode.com/todos/1"
                    }
                }
            }' \
    -s -o /dev/null -w 'Response Code: %{http_code}\n'
```

```bash
curl -X POST "${PLATO_DATAMGMT_URL}/data/policydefinitions" \
    --header 'X-Api-Key: password' \
    --header 'Content-Type: application/json' \
    --data '{
               "id": "1",
                "policy": {
                    "prohibitions": [],
                    "obligations": [],
                    "permissions": [
                        {
                            "edctype": "dataspaceconnector:permission",
                            "action": { "type": "USE" },
                            "constraints": []
                        }
                    ]
                }
            }' \
    -s -o /dev/null -w 'Response Code: %{http_code}\n'
```


```bash
curl -X POST "${PLATO_DATAMGMT_URL}/data/contractdefinitions" \
    --header 'X-Api-Key: password' \
    --header 'Content-Type: application/json' \
    --data '{
                "id": "1",
                "criteria": [
                    {
                        "operandLeft": "asset:prop:id",
                        "operator": "=",
                        "operandRight": "1"
                    }
                ],
                "accessPolicyId": "1",
                "contractPolicyId": "1"
            }' \
    -s -o /dev/null -w 'Response Code: %{http_code}\n'
```

## 2. Request Contract Offer Catalog

In this step Sokrates gets told to request contract offers from another connector (in this case Plato). Sokrates will
then request the catalog over IDS messaging.

For IDS messaging connectors will identify each other using the configured IDS DAPS. Therefore, it is important that
connectors, that intent to send messages to each other, have the same DAPS instance configured.

![Sequence 1](diagrams/transfer_sequence_2.png)

**Run**

```bash
curl -G -X GET "${SOKRATES_DATAMGMT_URL}/data/catalog" \
    --data-urlencode "providerUrl=${PLATO_IDS_URL}/api/v1/ids/data" \
    --header 'X-Api-Key: password' \
    --header 'Content-Type: application/json' \
    -s | jq
```

## 3. Negotiate Contract

Initiate a contract negotiation for the asset (from step 1). Part of the negotiation payload is the contract
offer (received in step 2).

In the diagram the IDS contract negotiation is marked as simplified, because the EDC is exchanging multiple messages
during contract negotiation. But the inter-controlplane communication is not in the scope of this document.

After the negotiation is initiated ensure that is has concluded. This is done by requesting the negotiation from the API
and checking whether the `contractAgreementId` is set. This might take a few seconds.

![Sequence 1](diagrams/transfer_sequence_3.png)

**Run**

```bash
export NEGOTIATION_ID=$( \
    curl -X POST "${SOKRATES_DATAMGMT_URL}/data/contractnegotiations" \
        --header "X-Api-Key: password" \
        --header "Content-Type: application/json" \
        --data "{
                    \"connectorId\": \"foo\",
                    \"connectorAddress\": \"${PLATO_IDS_URL}/api/v1/ids/data\",
                    \"offer\": {
                        \"offerId\": \"1:foo\",
                        \"assetId\": \"1\",
                        \"policy\": {
                            \"uid\": \"1\",
                            \"prohibitions\": [],
                            \"obligations\": [],
                            \"permissions\": [
                                {
                                    \"edctype\": \"dataspaceconnector:permission\",
                                    \"action\": { \"type\": \"USE\" },
                                    \"target\": \"1\",
                                    \"constraints\": []
                                }
                            ]
                        }
                    }
                }" \
    -s | jq -r '.id')
```


```bash
curl -X GET "${SOKRATES_DATAMGMT_URL}/data/contractnegotiations/${NEGOTIATION_ID}" \
    --header 'X-Api-Key: password' \
    --header 'Content-Type: application/json' \
    -s | jq
```

## 4. Transfer Data

Initiate a data transfer using the contract agreement from the negotiation (from step 3). Then wait until the state of
the transfer process is `COMPLETED`.

![Sequence 1](diagrams/transfer_sequence_4.png)

**Run**

```bash
export CONTRACT_AGREEMENT_ID=$( \
    curl -X GET "$SOKRATES_DATAMGMT_URL/data/contractnegotiations/$NEGOTIATION_ID" \
    --header 'X-Api-Key: password' \
    --header 'Content-Type: application/json' \
    -s | jq -r '.contractAgreementId')
```

```bash
export TRANSFER_PROCESS_ID=$(tr -dc '[:alnum:]' < /dev/urandom | head -c20)
export TRANSFER_ID=$( \
    curl -X POST "${SOKRATES_DATAMGMT_URL}/data/transferprocess" \
    --header "X-Api-Key: password" \
    --header "Content-Type: application/json" \
    --data "{
                \"id\": \"${TRANSFER_PROCESS_ID}\", 
                \"connectorId\": \"foo\", 
                \"connectorAddress\": \"${PLATO_IDS_URL}/api/v1/ids/data\", 
                \"contractId\": \"${CONTRACT_AGREEMENT_ID}\", 
                \"assetId\": \"1\", 
                \"managedResources\": \"false\", 
                \"dataDestination\": { \"type\": \"HttpProxy\" }
            }" \
     -s | jq -r '.id')
```

```bash
curl -X GET "$SOKRATES_DATAMGMT_URL/data/transferprocess/$TRANSFER_ID" \
    --header 'X-Api-Key: password' \
    --header 'Content-Type: application/json' \
    -s | jq
```

## 5. Verify Data Transfer

After the transfer is complete the Backend Application has downloaded the data. The Backend Application stores the data
locally. In this demo the transfer can be verified by executing a simple `cat` call in the Pod.

![Sequence 1](diagrams/transfer_sequence_5.png)

```bash
curl -X GET "${SOKRATES_BACKEND_URL}/${TRANSFER_PROCESS_ID}" \
    --header 'Accept: application/octet-stream' \
    -s | jq
```

# Delete All Data

```bash
minikube kubectl -- delete pvc -n edc-all-in-one --all
```

```bash
minikube kubectl -- delete pv -n edc-all-in-one --all
```
