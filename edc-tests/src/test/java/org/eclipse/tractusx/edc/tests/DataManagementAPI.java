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
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.tractusx.edc.tests.data.*;

@Slf4j
public class DataManagementAPI {

  private static final String ASSET_PATH = "/assets";
  private static final String POLICY_PATH = "/policydefinitions";
  private static final String CONTRACT_DEFINITIONS_PATH = "/contractdefinitions";
  private static final String CATALOG_PATH = "/catalog";
  private static final String NEGOTIATIONS_PATH = "/contractnegotiations";
  private static final String TRANSFER_PATH = "/transferprocess";

  private final String dataMgmtUrl;
  private final String dataMgmtAuthKey;
  private final CloseableHttpClient httpClient;

  public DataManagementAPI(String dataManagementUrl, String dataMgmtAuthKey) {
    this.httpClient = HttpClientBuilder.create().build();
    this.dataMgmtUrl = dataManagementUrl;
    this.dataMgmtAuthKey = dataMgmtAuthKey;
  }

  public List<ContractOffer> requestCatalogFrom(String receivingConnectorUrl) throws IOException {
    final String encodedUrl = URLEncoder.encode(receivingConnectorUrl, StandardCharsets.UTF_8);
    final ManagementApiContractOfferCatalog catalog =
        get(
            CATALOG_PATH,
            "providerUrl=" + encodedUrl,
            new TypeToken<ManagementApiContractOfferCatalog>() {});

    log.debug("Received " + catalog.contractOffers.size() + " offers");

    return catalog.contractOffers.stream().map(this::mapOffer).collect(Collectors.toList());
  }

  public Negotiation initiateNegotiation(
      String receivingConnectorUrl, String definitionId, String assetId, Policy policy)
      throws IOException {
    final ManagementApiOffer offer = new ManagementApiOffer();
    offer.offerId = definitionId + ":foo";
    offer.assetId = assetId;
    offer.policy = mapPolicy(policy);
    offer.policy.permissions.forEach(p -> p.target = assetId);

    final ManagementApiNegotiationPayload negotiationPayload =
        new ManagementApiNegotiationPayload();
    negotiationPayload.connectorAddress = receivingConnectorUrl;
    negotiationPayload.offer = offer;

    final ManagementApiNegotiationResponse response =
        post(
            NEGOTIATIONS_PATH,
            negotiationPayload,
            new TypeToken<ManagementApiNegotiationResponse>() {});

    if (response == null)
      throw new RuntimeException(
          "Initiated negotiation. Connector did not answer with negotiation ID.");

    log.info(String.format("Initiated negotiation (id=%s)", response.getId()));

    final String negotiationId = response.getId();
    return new Negotiation(negotiationId);
  }

  public Transfer initiateTransferProcess(
      String receivingConnectorUrl,
      String contractAgreementId,
      String assetId,
      DataAddress dataAddress)
      throws IOException {
    final ManagementApiTransfer transfer = new ManagementApiTransfer();

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
    final ManagementApiTransfer transfer = new ManagementApiTransfer();

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

  private Transfer initiateTransferProcess(ManagementApiTransfer transfer) throws IOException {
    final ManagementApiTransferResponse response =
        post(TRANSFER_PATH, transfer, new TypeToken<ManagementApiTransferResponse>() {});

    if (response == null)
      throw new RuntimeException(
          "Initiated transfer process. Connector did not answer with transfer process ID.");

    log.info(String.format("Initiated transfer process (id=%s)", response.getId()));

    final String transferId = response.getId();
    return new Transfer(transferId);
  }

  public TransferProcess getTransferProcess(String id) throws IOException {
    final ManagementApiTransferProcess transferProcess =
        get(TRANSFER_PATH + "/" + id, new TypeToken<ManagementApiTransferProcess>() {});
    return mapTransferProcess(transferProcess);
  }

  public ContractNegotiation getNegotiation(String id) throws IOException {
    final ManagementApiNegotiation negotiation =
        get(NEGOTIATIONS_PATH + "/" + id, new TypeToken<ManagementApiNegotiation>() {});
    return mapNegotiation(negotiation);
  }

  public List<ContractNegotiation> getNegotiations() throws IOException {
    final List<ManagementApiNegotiation> negotiations =
        get(NEGOTIATIONS_PATH + "/", new TypeToken<List<ManagementApiNegotiation>>() {});
    return negotiations.stream().map(this::mapNegotiation).collect(Collectors.toList());
  }

  public void createAsset(Asset asset) throws IOException {
    final ManagementApiAssetCreate assetCreate = new ManagementApiAssetCreate();

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

  private <T> T get(String path, String params, TypeToken<?> typeToken) throws IOException {
    return get(path + "?" + params, typeToken);
  }

  private <T> T get(String path, TypeToken<?> typeToken) throws IOException {

    final HttpGet get = new HttpGet(dataMgmtUrl + path);
    final HttpResponse response = sendRequest(get);
    final byte[] json = response.getEntity().getContent().readAllBytes();

    log.debug("Received response: {}", new String(json, StandardCharsets.UTF_8));
    return new Gson().fromJson(new String(json, StandardCharsets.UTF_8), typeToken.getType());
  }

  private void post(String path, Object object) throws IOException {
    post(path, object, new TypeToken<Void>() {});
  }

  private <T> T post(String path, Object object, TypeToken<?> typeToken) throws IOException {
    final String url = String.format("%s%s", dataMgmtUrl, path);
    final HttpPost post = new HttpPost(url);
    post.addHeader("Content-Type", "application/json");

    var json = new Gson().toJson(object);

    log.debug("POST Payload: " + json);

    post.setEntity(new StringEntity(json));
    final CloseableHttpResponse response = sendRequest(post);

    T responseJson = null;
    if (!typeToken.equals(new TypeToken<Void>() {})) {
      final byte[] responseBytes = response.getEntity().getContent().readAllBytes();
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

    final CloseableHttpResponse response = httpClient.execute(request);
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

    return new ContractNegotiation(negotiation.id, negotiation.contractAgreementId, state);
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

  private ManagementApiDataAddress mapDataAddress(@NonNull DataAddress dataAddress) {
    final ManagementApiDataAddress apiObject = new ManagementApiDataAddress();

    if (dataAddress instanceof HttpProxySourceDataAddress) {
      final var address = (HttpProxySourceDataAddress) dataAddress;
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
      final S3DataAddress a = (S3DataAddress) dataAddress;
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
    final Map<String, Object> properties =
        Map.of(
            ManagementApiAsset.ID, asset.getId(),
            ManagementApiAsset.DESCRIPTION, asset.getDescription());

    final ManagementApiAsset apiObject = new ManagementApiAsset();
    apiObject.setProperties(properties);
    return apiObject;
  }

  private Policy mapPolicy(ManagementApiPolicy managementApiPolicy) {
    final String id = managementApiPolicy.uid;
    final List<Permission> permissions =
        managementApiPolicy.permissions.stream()
            .map(this::mapPermission)
            .collect(Collectors.toList());

    return new Policy(id, permissions);
  }

  private ManagementApiPolicy mapPolicy(Policy policy) {
    final List<ManagementApiPermission> permissions =
        policy.getPermission().stream().map(this::mapPermission).collect(Collectors.toList());
    final ManagementApiPolicy managementApiPolicy = new ManagementApiPolicy();
    managementApiPolicy.permissions = permissions;

    return managementApiPolicy;
  }

  private ManagementApiPolicyDefinition mapPolicyDefinition(Policy policy) {
    final ManagementApiPolicyDefinition apiObject = new ManagementApiPolicyDefinition();
    apiObject.id = policy.getId();
    apiObject.policy = mapPolicy(policy);
    return apiObject;
  }

  private Permission mapPermission(ManagementApiPermission managementApiPermission) {
    final String target = managementApiPermission.target;
    final String action = managementApiPermission.action.type;
    return new Permission(action, target, new ArrayList<>());
  }

  private ManagementApiPermission mapPermission(Permission permission) {
    final String target = permission.getTarget();
    final String action = permission.getAction();

    final ManagementApiRuleAction apiAction = new ManagementApiRuleAction();
    apiAction.type = action;

    var constraints =
        permission.getConstraints().stream().map(this::mapConstraint).collect(Collectors.toList());

    final ManagementApiPermission apiObject = new ManagementApiPermission();
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
    final ManagementApiLiteralExpression leftExpression = new ManagementApiLiteralExpression();
    leftExpression.value = "PayMe";

    final ManagementApiLiteralExpression rightExpression = new ManagementApiLiteralExpression();
    rightExpression.value = String.valueOf(constraint.getAmount());

    final ManagementAtomicConstraint dataManagementApiConstraint = new ManagementAtomicConstraint();
    dataManagementApiConstraint.leftExpression = leftExpression;
    dataManagementApiConstraint.rightExpression = rightExpression;
    dataManagementApiConstraint.operator = "EQ";

    return dataManagementApiConstraint;
  }

  private ManagementAtomicConstraint mapConstraint(BusinessPartnerNumberConstraint constraint) {
    final ManagementApiLiteralExpression leftExpression = new ManagementApiLiteralExpression();
    leftExpression.value = "BusinessPartnerNumber";

    final ManagementApiLiteralExpression rightExpression = new ManagementApiLiteralExpression();
    rightExpression.value = constraint.getBusinessPartnerNumber();

    final ManagementAtomicConstraint dataManagementApiConstraint = new ManagementAtomicConstraint();
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
    final String id = managementApiContractOffer.id;
    final String assetId =
        managementApiContractOffer.assetId != null
            ? managementApiContractOffer.assetId
            : (String) managementApiContractOffer.asset.getProperties().get(ManagementApiAsset.ID);

    final Policy policy = mapPolicy(managementApiContractOffer.getPolicy());

    return new ContractOffer(id, policy, assetId);
  }

  private ManagementApiContractDefinition mapContractDefinition(
      ContractDefinition contractDefinition) {

    final ManagementApiContractDefinition apiObject = new ManagementApiContractDefinition();
    apiObject.id = contractDefinition.getId();
    apiObject.accessPolicyId = contractDefinition.getAcccessPolicyId();
    apiObject.contractPolicyId = contractDefinition.getContractPolicyId();
    apiObject.criteria = new ArrayList<>();

    for (final String assetId : contractDefinition.getAssetIds()) {
      ManagementApiCriterion criterion = new ManagementApiCriterion();
      criterion.operandLeft = ManagementApiAsset.ID;
      criterion.operator = "=";
      criterion.operandRight = assetId;

      apiObject.criteria.add(criterion);
    }

    return apiObject;
  }

  @Data
  private static class ManagementApiNegotiationResponse {
    private String id;
  }

  @Data
  private static class ManagementApiNegotiationPayload {
    private String connectorId = "foo";
    private String connectorAddress;
    private ManagementApiOffer offer;
  }

  @Data
  private static class ManagementApiNegotiation {
    private String id;
    private String state;
    private String contractAgreementId;
  }

  @Data
  private static class ManagementApiTransferProcess {
    private String id;
    private String state;
  }

  @Data
  private static class ManagementApiOffer {
    private String offerId;
    private String assetId;
    private ManagementApiPolicy policy;
  }

  @Data
  private static class ManagementApiTransfer {
    private String connectorId = "foo";
    private String connectorAddress;
    private String contractId;
    private String assetId;
    private String protocol;
    private ManagementApiDataAddress dataDestination;
    private boolean managedResources;
    private ManagementApiTransferType transferType;
    private ManagementApiProperties properties;
  }

  @Data
  private static class ManagementApiTransferType {
    private String contentType = "application/octet-stream";
    private boolean isFinite = true;
  }

  @Data
  private static class ManagementApiTransferResponse {
    private String id;
  }

  @Data
  private static class ManagementApiAssetCreate {
    private ManagementApiAsset asset;
    private ManagementApiDataAddress dataAddress;
  }

  @Data
  private static class ManagementApiAsset {
    public static final String ID = "asset:prop:id";
    public static final String DESCRIPTION = "asset:prop:description";

    private Map<String, Object> properties;
  }

  @Data
  private static class ManagementApiDataAddress {
    public static final String TYPE = "type";
    private Map<String, Object> properties;
  }

  @Data
  private static class ManagementApiProperties {
    @SerializedName(value = "receiver.http.endpoint")
    private final String receiverHttpEndpoint;
  }

  @Data
  private static class ManagementApiPolicyDefinition {
    private String id;
    private ManagementApiPolicy policy;
  }

  @Data
  private static class ManagementApiPolicy {
    private String uid;
    private List<ManagementApiPermission> permissions = new ArrayList<>();
  }

  @Data
  private static class ManagementApiPermission {
    private String edctype = "dataspaceconnector:permission";
    private ManagementApiRuleAction action;
    private String target;
    private List<ManagementConstraint> constraints = new ArrayList<>();
  }

  @Data
  private static class ManagementAtomicConstraint implements ManagementConstraint {
    private String edctype = "AtomicConstraint";
    private ManagementApiLiteralExpression leftExpression;
    private ManagementApiLiteralExpression rightExpression;
    private String operator;
  }

  @Data
  private static class ManagementOrConstraint implements ManagementConstraint {
    private String edctype = "dataspaceconnector:orconstraint";
    private List<ManagementConstraint> constraints;
  }

  private interface ManagementConstraint {}

  @Data
  private static class ManagementApiLiteralExpression {
    private String edctype = "dataspaceconnector:literalexpression";
    private String value;
  }

  @Data
  private static class ManagementApiRuleAction {
    private String type;
  }

  @Data
  private static class ManagementApiContractDefinition {
    private String id;
    private String accessPolicyId;
    private String contractPolicyId;
    private List<ManagementApiCriterion> criteria = new ArrayList<>();
  }

  @Data
  private static class ManagementApiCriterion {
    private Object operandLeft;
    private String operator;
    private Object operandRight;
  }

  @Data
  private static class ManagementApiContractOffer {
    private String id;
    private ManagementApiPolicy policy;
    private ManagementApiAsset asset;
    private String assetId;
  }

  @Data
  private static class ManagementApiContractOfferCatalog {
    private String id;
    private List<ManagementApiContractOffer> contractOffers = new ArrayList<>();
  }
}
