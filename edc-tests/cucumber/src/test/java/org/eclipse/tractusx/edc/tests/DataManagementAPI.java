/*
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.edc.tests;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.tractusx.edc.tests.data.Asset;
import org.eclipse.tractusx.edc.tests.data.BusinessPartnerNumberConstraint;
import org.eclipse.tractusx.edc.tests.data.Constraint;
import org.eclipse.tractusx.edc.tests.data.ContractDefinition;
import org.eclipse.tractusx.edc.tests.data.ContractNegotiation;
import org.eclipse.tractusx.edc.tests.data.ContractNegotiationState;
import org.eclipse.tractusx.edc.tests.data.ContractOffer;
import org.eclipse.tractusx.edc.tests.data.DataAddress;
import org.eclipse.tractusx.edc.tests.data.HttpProxySinkDataAddress;
import org.eclipse.tractusx.edc.tests.data.HttpProxySourceDataAddress;
import org.eclipse.tractusx.edc.tests.data.Negotiation;
import org.eclipse.tractusx.edc.tests.data.NullDataAddress;
import org.eclipse.tractusx.edc.tests.data.OrConstraint;
import org.eclipse.tractusx.edc.tests.data.PayMeConstraint;
import org.eclipse.tractusx.edc.tests.data.Permission;
import org.eclipse.tractusx.edc.tests.data.Policy;
import org.eclipse.tractusx.edc.tests.data.S3DataAddress;
import org.eclipse.tractusx.edc.tests.data.Transfer;
import org.eclipse.tractusx.edc.tests.data.TransferProcess;
import org.eclipse.tractusx.edc.tests.data.TransferProcessState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class DataManagementAPI {

    private static final Logger log = LoggerFactory.getLogger(DataManagementAPI.class);
    private static final String ASSET_PATH = "/assets";
    private static final String POLICY_PATH = "/policydefinitions";
    private static final String CONTRACT_DEFINITIONS_PATH = "/contractdefinitions";
    private static final String CATALOG_PATH = "/catalog";
    private static final String NEGOTIATIONS_PATH = "/contractnegotiations";
    private static final String TRANSFER_PATH = "/transferprocess";
    private static final String ADAPTER_PATH = "/adapter/asset/sync/";

    private final String dataMgmtUrl;
    private final String dataMgmtAuthKey;
    private final CloseableHttpClient httpClient;

    public DataManagementAPI(String dataManagementUrl, String dataMgmtAuthKey) {
        httpClient = HttpClientBuilder.create().build();
        dataMgmtUrl = dataManagementUrl;
        this.dataMgmtAuthKey = dataMgmtAuthKey;
    }

    public List<ContractOffer> requestCatalogFrom(String receivingConnectorUrl) throws IOException {
        String encodedUrl = URLEncoder.encode(receivingConnectorUrl, StandardCharsets.UTF_8);
        ManagementApiContractOfferCatalog catalog =
                get(
                        CATALOG_PATH,
                        "providerUrl=" + encodedUrl,
                        new TypeToken<ManagementApiContractOfferCatalog>() {
                        });

        log.debug("Received " + catalog.contractOffers.size() + " offers");

        return catalog.contractOffers.stream().map(this::mapOffer).collect(Collectors.toList());
    }

    public Negotiation initiateNegotiation(
            String receivingConnectorUrl, String definitionId, String assetId, Policy policy)
            throws IOException {
        ManagementApiOffer offer = new ManagementApiOffer();
        offer.offerId = definitionId + ":foo";
        offer.assetId = assetId;
        offer.policy = mapPolicy(policy);
        offer.policy.permissions.forEach(p -> p.target = assetId);

        ManagementApiNegotiationPayload negotiationPayload =
                new ManagementApiNegotiationPayload();
        negotiationPayload.connectorAddress = receivingConnectorUrl;
        negotiationPayload.offer = offer;

        ManagementApiNegotiationResponse response =
                post(
                        NEGOTIATIONS_PATH,
                        negotiationPayload,
                        new TypeToken<ManagementApiNegotiationResponse>() {
                        });

        if (response == null) {
            throw new RuntimeException(
                    "Initiated negotiation. Connector did not answer with negotiation ID.");
        }

        log.info(String.format("Initiated negotiation (id=%s)", response.getId()));

        String negotiationId = response.getId();
        return new Negotiation(negotiationId);
    }

    public Transfer initiateTransferProcess(
            String receivingConnectorUrl,
            String contractAgreementId,
            String assetId,
            DataAddress dataAddress)
            throws IOException {
        ManagementApiTransfer transfer = new ManagementApiTransfer();

        transfer.connectorAddress = receivingConnectorUrl;
        transfer.contractId = contractAgreementId;
        transfer.assetId = assetId;
        transfer.transferType = new ManagementApiTransferType();
        transfer.managedResources = false;
        transfer.dataDestination = mapDataAddress(dataAddress);
        transfer.protocol = "ids-multipart";

        return initiateTransferProcess(transfer);
    }

    public Transfer initiateTransferProcess(
            String receivingConnectorUrl,
            String contractAgreementId,
            String assetId,
            DataAddress dataAddress,
            String receiverEndpoint)
            throws IOException {
        ManagementApiTransfer transfer = new ManagementApiTransfer();

        transfer.connectorAddress = receivingConnectorUrl;
        transfer.contractId = contractAgreementId;
        transfer.assetId = assetId;
        transfer.transferType = new ManagementApiTransferType();
        transfer.managedResources = false;
        transfer.dataDestination = mapDataAddress(dataAddress);
        transfer.protocol = "ids-multipart";
        transfer.properties = new ManagementApiProperties(receiverEndpoint);

        return initiateTransferProcess(transfer);
    }

    public Asset initiateTransferProcess(
            String endpointUrl, String endpointAuthKey, String endpointAuthCode) throws IOException {
        Header header = new BasicHeader(endpointAuthKey, endpointAuthCode);
        return get(endpointUrl, header, new TypeToken<Asset>() {
        });
    }

    public TransferProcess getTransferProcess(String id) throws IOException {
        ManagementApiTransferProcess transferProcess =
                get(TRANSFER_PATH + "/" + id, new TypeToken<ManagementApiTransferProcess>() {
                });
        return mapTransferProcess(transferProcess);
    }

    public ContractNegotiation getNegotiation(String id) throws IOException {
        ManagementApiNegotiation negotiation =
                get(NEGOTIATIONS_PATH + "/" + id, new TypeToken<ManagementApiNegotiation>() {
                });
        return mapNegotiation(negotiation);
    }

    public List<ContractNegotiation> getNegotiations() throws IOException {
        List<ManagementApiNegotiation> negotiations =
                get(NEGOTIATIONS_PATH + "/", new TypeToken<List<ManagementApiNegotiation>>() {
                });
        return negotiations.stream().map(this::mapNegotiation).collect(Collectors.toList());
    }

    public void createAsset(Asset asset) throws IOException {
        ManagementApiAssetCreate assetCreate = new ManagementApiAssetCreate();

        assetCreate.asset = mapAsset(asset);
        assetCreate.dataAddress = mapDataAddress(asset.getDataAddress());

        post(ASSET_PATH, assetCreate);
    }

    public void createPolicy(Policy policy) throws IOException {
        post(POLICY_PATH, mapPolicyDefinition(policy));
    }

    public void createContractDefinition(ContractDefinition contractDefinition) throws IOException {
        post(CONTRACT_DEFINITIONS_PATH, mapContractDefinition(contractDefinition));
    }

    public EndpointDataReference getEdcEndpoint(String assetId, String receivingConnectorUrl)
            throws IOException {
        String encodedUrl = ADAPTER_PATH + assetId + "?providerUrl=" + receivingConnectorUrl;

        EndpointDataReference endpoint =
                get(encodedUrl, new TypeToken<EndpointDataReference>() {
                });

        return endpoint;
    }

    private Transfer initiateTransferProcess(ManagementApiTransfer transfer) throws IOException {
        ManagementApiTransferResponse response =
                post(TRANSFER_PATH, transfer, new TypeToken<ManagementApiTransferResponse>() {
                });

        if (response == null) {
            throw new RuntimeException(
                    "Initiated transfer process. Connector did not answer with transfer process ID.");
        }

        log.info(String.format("Initiated transfer process (id=%s)", response.getId()));

        String transferId = response.getId();
        return new Transfer(transferId);
    }

    private <T> T get(String path, String params, TypeToken<?> typeToken) throws IOException {
        return get(path + "?" + params, typeToken);
    }

    private <T> T get(String path, TypeToken<?> typeToken) throws IOException {

        HttpGet get = new HttpGet(dataMgmtUrl + path);
        HttpResponse response = sendRequest(get);
        byte[] json = response.getEntity().getContent().readAllBytes();

        log.debug("Received response: {}", new String(json, StandardCharsets.UTF_8));
        return new Gson().fromJson(new String(json, StandardCharsets.UTF_8), typeToken.getType());
    }

    private <T> T get(String path, Header header, TypeToken<?> typeToken) throws IOException {

        HttpGet get = new HttpGet(path);
        get.addHeader(header);
        HttpResponse response = sendRequest(get);
        byte[] json = response.getEntity().getContent().readAllBytes();

        log.debug("Received response: {}", new String(json, StandardCharsets.UTF_8));
        return new Gson().fromJson(new String(json, StandardCharsets.UTF_8), typeToken.getType());
    }

    private void post(String path, Object object) throws IOException {
        post(path, object, new TypeToken<Void>() {
        });
    }

    private <T> T post(String path, Object object, TypeToken<?> typeToken) throws IOException {
        String url = String.format("%s%s", dataMgmtUrl, path);
        HttpPost post = new HttpPost(url);
        post.addHeader("Content-Type", "application/json");

        var json = new Gson().toJson(object);

        log.debug("POST Payload: " + json);

        post.setEntity(new StringEntity(json));
        CloseableHttpResponse response = sendRequest(post);

        T responseJson = null;
        if (!typeToken.equals(new TypeToken<Void>() {
        })) {
            byte[] responseBytes = response.getEntity().getContent().readAllBytes();
            responseJson =
                    new Gson()
                            .fromJson(new String(responseBytes, StandardCharsets.UTF_8), typeToken.getType());
        }

        response.close();

        return responseJson;
    }

    private CloseableHttpResponse sendRequest(HttpRequestBase request) throws IOException {
        request.addHeader("X-Api-Key", dataMgmtAuthKey);

        log.debug(String.format("Send %-6s %s", request.getMethod(), request.getURI()));

        CloseableHttpResponse response = httpClient.execute(request);
        if (200 > response.getStatusLine().getStatusCode()
                || response.getStatusLine().getStatusCode() >= 300) {
            throw new RuntimeException(
                    String.format("Unexpected response: %s", response.getStatusLine()));
        }

        return response;
    }

    private ContractNegotiation mapNegotiation(ManagementApiNegotiation negotiation) {

        ContractNegotiationState state;

        switch (negotiation.state) {
            case "ERROR":
                state = ContractNegotiationState.ERROR;
                break;
            case "INITIAL":
                state = ContractNegotiationState.INITIAL;
                break;
            case "DECLINED":
                state = ContractNegotiationState.DECLINED;
                break;
            case "CONFIRMED":
                state = ContractNegotiationState.CONFIRMED;
                break;
            default:
                state = ContractNegotiationState.UNKNOWN;
        }

        return new ContractNegotiation(negotiation.id, state, negotiation.contractAgreementId);
    }

    private TransferProcess mapTransferProcess(ManagementApiTransferProcess transferProcess) {

        TransferProcessState state;

        switch (transferProcess.state) {
            case "COMPLETED":
                state = TransferProcessState.COMPLETED;
                break;
            case "ERROR":
                state = TransferProcessState.ERROR;
                break;
            default:
                state = TransferProcessState.UNKNOWN;
        }

        return new TransferProcess(transferProcess.id, state);
    }

    private ManagementApiDataAddress mapDataAddress(DataAddress dataAddress) {
        Objects.requireNonNull(dataAddress);
        ManagementApiDataAddress apiObject = new ManagementApiDataAddress();

        if (dataAddress instanceof HttpProxySourceDataAddress) {
            var address = (HttpProxySourceDataAddress) dataAddress;
            var properties = new HashMap<String, Object>();
            properties.put("type", "HttpData");
            properties.put("baseUrl", address.getBaseUrl());
            var oauth2Provision = address.getOauth2Provision();
            if (oauth2Provision != null) {
                properties.put("oauth2:tokenUrl", oauth2Provision.getTokenUrl());
                properties.put("oauth2:clientId", oauth2Provision.getClientId());
                properties.put("oauth2:clientSecret", oauth2Provision.getClientSecret());
                properties.put("oauth2:scope", oauth2Provision.getScope());
            }
            apiObject.setProperties(properties);
        } else if (dataAddress instanceof HttpProxySinkDataAddress) {
            apiObject.setProperties(Map.of("type", "HttpProxy"));
        } else if (dataAddress instanceof S3DataAddress) {
            S3DataAddress a = (S3DataAddress) dataAddress;
            apiObject.setProperties(
                    Map.of(
                            "type",
                            "AmazonS3",
                            "bucketName",
                            a.getBucketName(),
                            "region",
                            a.getRegion(),
                            "keyName",
                            a.getKeyName()));
        } else if (dataAddress instanceof NullDataAddress) {
            // set something that passes validation
            apiObject.setProperties(Map.of("type", "HttpData", "baseUrl", "http://localhost"));
        } else {
            throw new UnsupportedOperationException(
                    String.format(
                            "Cannot map data address of type %s to EDC domain", dataAddress.getClass()));
        }

        return apiObject;
    }

    private ManagementApiAsset mapAsset(Asset asset) {
        Map<String, Object> properties =
                Map.of(
                        ManagementApiAsset.ID, asset.getId(),
                        ManagementApiAsset.DESCRIPTION, asset.getDescription());

        ManagementApiAsset apiObject = new ManagementApiAsset();
        apiObject.setProperties(properties);
        return apiObject;
    }

    private Policy mapPolicy(ManagementApiPolicy managementApiPolicy) {
        String id = managementApiPolicy.uid;
        List<Permission> permissions =
                managementApiPolicy.permissions.stream()
                        .map(this::mapPermission)
                        .collect(Collectors.toList());

        return new Policy(id, permissions);
    }

    private ManagementApiPolicy mapPolicy(Policy policy) {
        List<ManagementApiPermission> permissions =
                policy.getPermission().stream().map(this::mapPermission).collect(Collectors.toList());
        ManagementApiPolicy managementApiPolicy = new ManagementApiPolicy();
        managementApiPolicy.permissions = permissions;

        return managementApiPolicy;
    }

    private ManagementApiPolicyDefinition mapPolicyDefinition(Policy policy) {
        ManagementApiPolicyDefinition apiObject = new ManagementApiPolicyDefinition();
        apiObject.id = policy.getId();
        apiObject.policy = mapPolicy(policy);
        return apiObject;
    }

    private Permission mapPermission(ManagementApiPermission managementApiPermission) {
        String target = managementApiPermission.target;
        String action = managementApiPermission.action.type;
        return new Permission(action, new ArrayList<>(), target);
    }

    private ManagementApiPermission mapPermission(Permission permission) {
        String target = permission.getTarget();
        String action = permission.getAction();

        ManagementApiRuleAction apiAction = new ManagementApiRuleAction();
        apiAction.type = action;

        var constraints =
                permission.getConstraints().stream().map(this::mapConstraint).collect(Collectors.toList());

        ManagementApiPermission apiObject = new ManagementApiPermission();
        apiObject.target = target;
        apiObject.action = apiAction;
        apiObject.constraints = constraints;
        return apiObject;
    }

    private ManagementConstraint mapConstraint(Constraint constraint) {
        if (OrConstraint.class.equals(constraint.getClass())) {
            return mapConstraint((OrConstraint) constraint);
        } else if (BusinessPartnerNumberConstraint.class.equals(constraint.getClass())) {
            return mapConstraint((BusinessPartnerNumberConstraint) constraint);
        } else if (PayMeConstraint.class.equals(constraint.getClass())) {
            return mapConstraint((PayMeConstraint) constraint);
        } else {
            throw new UnsupportedOperationException(
                    "Unsupported constraint type: " + constraint.getClass().getName());
        }
    }

    private ManagementAtomicConstraint mapConstraint(PayMeConstraint constraint) {
        ManagementApiLiteralExpression leftExpression = new ManagementApiLiteralExpression();
        leftExpression.value = "PayMe";

        ManagementApiLiteralExpression rightExpression = new ManagementApiLiteralExpression();
        rightExpression.value = String.valueOf(constraint.getAmount());

        ManagementAtomicConstraint dataManagementApiConstraint = new ManagementAtomicConstraint();
        dataManagementApiConstraint.leftExpression = leftExpression;
        dataManagementApiConstraint.rightExpression = rightExpression;
        dataManagementApiConstraint.operator = "EQ";

        return dataManagementApiConstraint;
    }

    private ManagementAtomicConstraint mapConstraint(BusinessPartnerNumberConstraint constraint) {
        ManagementApiLiteralExpression leftExpression = new ManagementApiLiteralExpression();
        leftExpression.value = "BusinessPartnerNumber";

        ManagementApiLiteralExpression rightExpression = new ManagementApiLiteralExpression();
        rightExpression.value = constraint.getBusinessPartnerNumber();

        ManagementAtomicConstraint dataManagementApiConstraint = new ManagementAtomicConstraint();
        dataManagementApiConstraint.leftExpression = leftExpression;
        dataManagementApiConstraint.rightExpression = rightExpression;
        dataManagementApiConstraint.operator = "EQ";

        return dataManagementApiConstraint;
    }

    private ManagementOrConstraint mapConstraint(OrConstraint constraint) {
        var orConstraint = new ManagementOrConstraint();
        orConstraint.constraints =
                constraint.getConstraints().stream().map(this::mapConstraint).collect(Collectors.toList());
        return orConstraint;
    }

    private ContractOffer mapOffer(ManagementApiContractOffer managementApiContractOffer) {
        String id = managementApiContractOffer.id;
        String assetId =
                managementApiContractOffer.assetId != null
                        ? managementApiContractOffer.assetId
                        : (String) managementApiContractOffer.asset.getProperties().get(ManagementApiAsset.ID);

        Policy policy = mapPolicy(managementApiContractOffer.getPolicy());

        return new ContractOffer(id, policy, assetId);
    }

    private ManagementApiContractDefinition mapContractDefinition(
            ContractDefinition contractDefinition) {

        ManagementApiContractDefinition apiObject = new ManagementApiContractDefinition();
        apiObject.id = contractDefinition.getId();
        apiObject.accessPolicyId = contractDefinition.getAcccessPolicyId();
        apiObject.contractPolicyId = contractDefinition.getContractPolicyId();
        apiObject.criteria = new ArrayList<>();

        for (String assetId : contractDefinition.getAssetIds()) {
            ManagementApiCriterion criterion = new ManagementApiCriterion();
            criterion.operandLeft = ManagementApiAsset.ID;
            criterion.operator = "=";
            criterion.operandRight = assetId;

            apiObject.criteria.add(criterion);
        }

        return apiObject;
    }

    private interface ManagementConstraint {
    }


    private static class ManagementApiNegotiationResponse {
        private String id;


        public String getId() {
            return id;
        }
    }


    private static class ManagementApiNegotiationPayload {
        private final String connectorId = "foo";
        private String connectorAddress;
        private ManagementApiOffer offer;
    }

    private static class ManagementApiNegotiation {
        private String id;
        private String state;
        private String contractAgreementId;
    }

    private static class ManagementApiTransferProcess {
        private String id;
        private String state;
    }


    private static class ManagementApiOffer {
        private String offerId;
        private String assetId;
        private ManagementApiPolicy policy;
    }


    private static class ManagementApiTransfer {
        private final String connectorId = "foo";
        private String connectorAddress;
        private String contractId;
        private String assetId;
        private String protocol;
        private ManagementApiDataAddress dataDestination;
        private boolean managedResources;
        private ManagementApiTransferType transferType;
        private ManagementApiProperties properties;
    }


    private static class ManagementApiTransferType {
        private final String contentType = "application/octet-stream";
        private final boolean isFinite = true;
    }


    private static class ManagementApiTransferResponse {
        private String id;


        public String getId() {
            return id;
        }
    }

    private static class ManagementApiAssetCreate {
        private ManagementApiAsset asset;
        private ManagementApiDataAddress dataAddress;
    }

    private static class ManagementApiAsset {
        public static final String ID = "asset:prop:id";
        public static final String DESCRIPTION = "asset:prop:description";

        private Map<String, Object> properties;


        public Map<String, Object> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, Object> properties) {
            this.properties = properties;
        }
    }


    private static class ManagementApiDataAddress {
        public static final String TYPE = "type";
        private Map<String, Object> properties;


        public Map<String, Object> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, Object> properties) {
            this.properties = properties;
        }
    }


    private static class ManagementApiProperties {
        @SerializedName(value = "receiver.http.endpoint")
        private final String receiverHttpEndpoint;

        private ManagementApiProperties(String receiverHttpEndpoint) {
            this.receiverHttpEndpoint = receiverHttpEndpoint;
        }
    }


    private static class ManagementApiPolicyDefinition {
        private String id;
        private ManagementApiPolicy policy;
    }


    private static class ManagementApiPolicy {
        private String uid;
        private List<ManagementApiPermission> permissions = new ArrayList<>();
    }


    private static class ManagementApiPermission {
        private final String edctype = "dataspaceconnector:permission";
        private ManagementApiRuleAction action;
        private String target;
        private List<ManagementConstraint> constraints = new ArrayList<>();
    }


    private static class ManagementAtomicConstraint implements ManagementConstraint {
        private final String edctype = "AtomicConstraint";
        private ManagementApiLiteralExpression leftExpression;
        private ManagementApiLiteralExpression rightExpression;
        private String operator;
    }


    private static class ManagementOrConstraint implements ManagementConstraint {
        private final String edctype = "dataspaceconnector:orconstraint";
        private List<ManagementConstraint> constraints;
    }


    private static class ManagementApiLiteralExpression {
        private final String edctype = "dataspaceconnector:literalexpression";
        private String value;
    }


    private static class ManagementApiRuleAction {
        private String type;
    }


    private static class ManagementApiContractDefinition {
        private String id;
        private String accessPolicyId;
        private String contractPolicyId;
        private List<ManagementApiCriterion> criteria = new ArrayList<>();
    }


    private static class ManagementApiCriterion {
        private Object operandLeft;
        private String operator;
        private Object operandRight;
    }


    private static class ManagementApiContractOffer {
        private String id;
        private ManagementApiPolicy policy;
        private ManagementApiAsset asset;
        private String assetId;


        public ManagementApiPolicy getPolicy() {
            return policy;
        }
    }


    private static class ManagementApiContractOfferCatalog {
        private final List<ManagementApiContractOffer> contractOffers = new ArrayList<>();
        private String id;
    }
}
