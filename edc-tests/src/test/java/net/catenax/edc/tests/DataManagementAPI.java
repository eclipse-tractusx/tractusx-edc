/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *
 */

package net.catenax.edc.tests;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.catenax.edc.tests.data.Asset;
import net.catenax.edc.tests.data.BusinessPartnerNumberConstraint;
import net.catenax.edc.tests.data.Constraint;
import net.catenax.edc.tests.data.ContractDefinition;
import net.catenax.edc.tests.data.ContractNegotiation;
import net.catenax.edc.tests.data.ContractNegotiationState;
import net.catenax.edc.tests.data.ContractOffer;
import net.catenax.edc.tests.data.PayMeConstraint;
import net.catenax.edc.tests.data.Permission;
import net.catenax.edc.tests.data.Policy;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

@Slf4j
public class DataManagementAPI {

  private static final String ASSET_PATH = "/assets";
  private static final String POLICY_PATH = "/policydefinitions";
  private static final String CONTRACT_DEFINITIONS_PATH = "/contractdefinitions";
  private static final String CATALOG_PATH = "/catalog";
  private static final String NEGOTIATIONS_PATH = "/contractnegotiations";

  private final String dataMgmtUrl;
  private final String dataMgmtAuthKey;
  private final HttpClient httpClient;

  public DataManagementAPI(String dataManagementUrl, String dataMgmtAuthKey) {
    this.httpClient = HttpClientBuilder.create().build();
    this.dataMgmtUrl = dataManagementUrl;
    this.dataMgmtAuthKey = dataMgmtAuthKey;
  }

  public List<ContractOffer> requestCatalogFrom(String receivingConnectorUrl) throws IOException {
    final String encodedUrl = URLEncoder.encode(receivingConnectorUrl, StandardCharsets.UTF_8);
    final DataManagementApiContractOfferCatalog catalog =
        get(
            CATALOG_PATH,
            "providerUrl=" + encodedUrl + "&limit=1000",
            new TypeToken<DataManagementApiContractOfferCatalog>() {});

    log.debug("Received " + catalog.contractOffers.size() + " offers");

    return catalog.contractOffers.stream().map(this::mapOffer).collect(Collectors.toList());
  }

  public String initiateNegotiation(
      String receivingConnectorUrl, String definitionId, String assetId, Policy policy)
      throws IOException {
    final DataManagementApiOffer offer = new DataManagementApiOffer();
    offer.offerId = definitionId + ":foo";
    offer.assetId = assetId;
    offer.policy = mapPolicy(policy);
    offer.policy.permissions.forEach(p -> p.target = assetId);

    final DataManagementApiNegotiationPayload negotiationPayload =
        new DataManagementApiNegotiationPayload();
    negotiationPayload.connectorAddress = receivingConnectorUrl;
    negotiationPayload.offer = offer;

    final DataManagementApiNegotiationResponse response =
        post(
            NEGOTIATIONS_PATH,
            negotiationPayload,
            new TypeToken<DataManagementApiNegotiationResponse>() {});

    if (response == null)
      throw new RuntimeException(
          "Initiated negotiation. Connector did not answer with negotiation ID.");

    log.debug("Initiated negotiation ( id= " + response.getId() + " )");

    return response.getId();
  }

  public ContractNegotiation getNegotiation(String id) throws IOException {
    final DataManagementApiNegotiation negotiation =
        get(NEGOTIATIONS_PATH + "/" + id, new TypeToken<DataManagementApiNegotiation>() {});
    return mapNegotiation(negotiation);
  }

  public void createAsset(Asset asset) throws IOException {
    final DataManagementApiDataAddress dataAddress = new DataManagementApiDataAddress();
    dataAddress.properties =
        Map.of(
            DataManagementApiDataAddress.TYPE,
            "HttpData",
            "baseUrl",
            "https://jsonplaceholder.typicode.com/todos/1");

    final DataManagementApiAssetCreate assetCreate = new DataManagementApiAssetCreate();
    assetCreate.asset = mapAsset(asset);
    assetCreate.dataAddress = dataAddress;

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

    final HttpResponse response = sendRequest(post);

    if (typeToken.equals(new TypeToken<Void>() {})) return null;

    final byte[] responseJson = response.getEntity().getContent().readAllBytes();
    return new Gson()
        .fromJson(new String(responseJson, StandardCharsets.UTF_8), typeToken.getType());
  }

  private HttpResponse sendRequest(HttpRequestBase request) throws IOException {
    request.addHeader("X-Api-Key", dataMgmtAuthKey);

    log.debug(String.format("Send %-6s %s", request.getMethod(), request.getURI()));

    final HttpResponse response = httpClient.execute(request);
    if (200 > response.getStatusLine().getStatusCode()
        || response.getStatusLine().getStatusCode() >= 300) {
      throw new RuntimeException(
          String.format("Unexpected response: %s", response.getStatusLine()));
    }

    return response;
  }

  private ContractNegotiation mapNegotiation(DataManagementApiNegotiation negotiation) {

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

    return new ContractNegotiation(negotiation.id, negotiation.agreementId, state);
  }

  private DataManagementApiAsset mapAsset(Asset asset) {
    final Map<String, Object> properties =
        Map.of(
            DataManagementApiAsset.ID, asset.getId(),
            DataManagementApiAsset.DESCRIPTION, asset.getDescription());

    final DataManagementApiAsset apiObject = new DataManagementApiAsset();
    apiObject.setProperties(properties);
    return apiObject;
  }

  private Policy mapPolicy(DataManagementApiPolicy dataManagementApiPolicy) {
    final String id = dataManagementApiPolicy.uid;
    final List<Permission> permissions =
        dataManagementApiPolicy.permissions.stream()
            .map(this::mapPermission)
            .collect(Collectors.toList());

    return new Policy(id, permissions);
  }

  private DataManagementApiPolicy mapPolicy(Policy policy) {
    final List<DataManagementApiPermission> permissions =
        policy.getPermission().stream().map(this::mapPermission).collect(Collectors.toList());
    final DataManagementApiPolicy dataManagementApiPolicy = new DataManagementApiPolicy();
    dataManagementApiPolicy.permissions = permissions;

    return dataManagementApiPolicy;
  }

  private DataManagementApiPolicyDefinition mapPolicyDefinition(Policy policy) {
    final DataManagementApiPolicyDefinition apiObject = new DataManagementApiPolicyDefinition();
    apiObject.id = policy.getId();
    apiObject.policy = mapPolicy(policy);
    return apiObject;
  }

  private Permission mapPermission(DataManagementApiPermission dataManagementApiPermission) {
    final String target = dataManagementApiPermission.target;
    final String action = dataManagementApiPermission.action.type;
    return new Permission(action, target, new ArrayList<>());
  }

  private DataManagementApiPermission mapPermission(Permission permission) {
    final String target = permission.getTarget();
    final String action = permission.getAction();

    final DataManagementApiRuleAction apiAction = new DataManagementApiRuleAction();
    apiAction.type = action;

    final List<DataManagementApiConstraint> constraints =
        permission.getConstraints().stream().map(this::mapConstraint).collect(Collectors.toList());

    final DataManagementApiPermission apiObject = new DataManagementApiPermission();
    apiObject.target = target;
    apiObject.action = apiAction;
    apiObject.constraints = constraints;
    return apiObject;
  }

  private DataManagementApiConstraint mapConstraint(Constraint constraint) {

    if (BusinessPartnerNumberConstraint.class.equals(constraint.getClass())) {
      return mapConstraint((BusinessPartnerNumberConstraint) constraint);
    } else if (PayMeConstraint.class.equals(constraint.getClass())) {
      return mapConstraint((PayMeConstraint) constraint);
    } else {
      throw new UnsupportedOperationException(
          "Unsupported constraint type: " + constraint.getClass().getName());
    }
  }

  private DataManagementApiConstraint mapConstraint(PayMeConstraint constraint) {
    final DataManagementApiLiteralExpression leftExpression =
        new DataManagementApiLiteralExpression();
    leftExpression.value = "PayMe";

    final DataManagementApiLiteralExpression rightExpression =
        new DataManagementApiLiteralExpression();
    rightExpression.value = String.valueOf(constraint.getAmount());

    final DataManagementApiConstraint dataManagementApiConstraint =
        new DataManagementApiConstraint();
    dataManagementApiConstraint.leftExpression = leftExpression;
    dataManagementApiConstraint.rightExpression = rightExpression;
    dataManagementApiConstraint.operator = "EQ";

    return dataManagementApiConstraint;
  }

  private DataManagementApiConstraint mapConstraint(BusinessPartnerNumberConstraint constraint) {
    final DataManagementApiLiteralExpression leftExpression =
        new DataManagementApiLiteralExpression();
    leftExpression.value = "BusinessPartnerNumber";

    final DataManagementApiLiteralExpression rightExpression =
        new DataManagementApiLiteralExpression();
    rightExpression.value = constraint.getBusinessPartnerNumber();

    final DataManagementApiConstraint dataManagementApiConstraint =
        new DataManagementApiConstraint();
    dataManagementApiConstraint.leftExpression = leftExpression;
    dataManagementApiConstraint.rightExpression = rightExpression;
    dataManagementApiConstraint.operator = "EQ";

    return dataManagementApiConstraint;
  }

  private ContractOffer mapOffer(DataManagementApiContractOffer dataManagementApiContractOffer) {
    final String id = dataManagementApiContractOffer.id;
    final String assetId =
        dataManagementApiContractOffer.assetId != null
            ? dataManagementApiContractOffer.assetId
            : (String)
                dataManagementApiContractOffer.asset.getProperties().get(DataManagementApiAsset.ID);

    final Policy policy = mapPolicy(dataManagementApiContractOffer.getPolicy());

    return new ContractOffer(id, policy, assetId);
  }

  private DataManagementApiContractDefinition mapContractDefinition(
      ContractDefinition contractDefinition) {

    final DataManagementApiContractDefinition apiObject = new DataManagementApiContractDefinition();
    apiObject.id = contractDefinition.getId();
    apiObject.accessPolicyId = contractDefinition.getAcccessPolicyId();
    apiObject.contractPolicyId = contractDefinition.getContractPolicyId();
    apiObject.criteria = new ArrayList<>();

    for (final String assetId : contractDefinition.getAssetIds()) {
      DataManagementApiCriterion criterion = new DataManagementApiCriterion();
      criterion.operandLeft = DataManagementApiAsset.ID;
      criterion.operator = "=";
      criterion.operandRight = assetId;

      apiObject.criteria.add(criterion);
    }

    return apiObject;
  }

  @Data
  private static class DataManagementApiNegotiationResponse {
    private String id;
  }

  @Data
  private static class DataManagementApiNegotiationPayload {
    private String connectorId = "foo";
    private String connectorAddress;
    private DataManagementApiOffer offer;
  }

  @Data
  private static class DataManagementApiNegotiation {
    private String id;
    private String state;
    private String agreementId;
  }

  @Data
  private static class DataManagementApiOffer {
    private String offerId;
    private String assetId;
    private DataManagementApiPolicy policy;
  }

  @Data
  private static class DataManagementApiAssetCreate {
    private DataManagementApiAsset asset;
    private DataManagementApiDataAddress dataAddress;
  }

  @Data
  private static class DataManagementApiAsset {
    public static final String ID = "asset:prop:id";
    public static final String DESCRIPTION = "asset:prop:description";

    private Map<String, Object> properties;
  }

  @Data
  private static class DataManagementApiDataAddress {
    public static final String TYPE = "type";
    private Map<String, Object> properties;
  }

  @Data
  private static class DataManagementApiPolicyDefinition {
    private String id;
    private DataManagementApiPolicy policy;
  }

  @Data
  private static class DataManagementApiPolicy {
    private String uid;
    private List<DataManagementApiPermission> permissions = new ArrayList<>();
  }

  @Data
  private static class DataManagementApiPermission {
    private String edctype = "dataspaceconnector:permission";
    private DataManagementApiRuleAction action;
    private String target;
    private List<DataManagementApiConstraint> constraints = new ArrayList<>();
  }

  @Data
  private static class DataManagementApiConstraint {
    private String edctype = "AtomicConstraint";
    private DataManagementApiLiteralExpression leftExpression;
    private DataManagementApiLiteralExpression rightExpression;
    private String operator;
  }

  @Data
  private static class DataManagementApiLiteralExpression {
    private String edctype = "dataspaceconnector:literalexpression";
    private String value;
  }

  @Data
  private static class DataManagementApiRuleAction {
    private String type;
  }

  @Data
  private static class DataManagementApiContractDefinition {
    private String id;
    private String accessPolicyId;
    private String contractPolicyId;
    private List<DataManagementApiCriterion> criteria = new ArrayList<>();
  }

  @Data
  private static class DataManagementApiCriterion {
    private Object operandLeft;
    private String operator;
    private Object operandRight;
  }

  @Data
  private static class DataManagementApiContractOffer {
    private String id;
    private DataManagementApiPolicy policy;
    private DataManagementApiAsset asset;
    private String assetId;
  }

  @Data
  private static class DataManagementApiContractOfferCatalog {
    private String id;
    private List<DataManagementApiContractOffer> contractOffers = new ArrayList<>();
  }
}
