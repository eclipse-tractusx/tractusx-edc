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
import java.util.stream.Stream;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.catenax.edc.tests.data.Asset;
import net.catenax.edc.tests.data.BusinessPartnerNumberConstraint;
import net.catenax.edc.tests.data.Constraint;
import net.catenax.edc.tests.data.ContractDefinition;
import net.catenax.edc.tests.data.ContractOffer;
import net.catenax.edc.tests.data.Permission;
import net.catenax.edc.tests.data.Policy;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

@Slf4j
public class DataManagementAPI {

  private static final String ASSET_PATH = "/assets";
  private static final String POLICY_PATH = "/policies";
  private static final String CONTRACT_DEFINITIONS_PATH = "/contractdefinitions";
  private static final String CATALOG_PATH = "/catalog";

  private static final String PARAM_NO_LIMIT = "limit=" + Integer.MAX_VALUE;

  private final String dataMgmtUrl;
  private final HttpClient httpClient;

  public DataManagementAPI(String dataManagementUrl) {
    this.httpClient = HttpClientBuilder.create().build();
    this.dataMgmtUrl = dataManagementUrl;
  }

  public List<ContractOffer> requestCatalogFrom(String receivingConnectorUrl) throws IOException {
    final String encodedUrl =
        URLEncoder.encode(receivingConnectorUrl, StandardCharsets.UTF_8.toString());
    final DataManagementApiContractOfferCatalog catalog =
        get(
            CATALOG_PATH,
            "providerUrl=" + encodedUrl,
            new TypeToken<DataManagementApiContractOfferCatalog>() {});

    log.debug("Received " + catalog.contractOffers.size() + " offers");

    return catalog.contractOffers.stream().map(this::mapOffer).collect(Collectors.toList());
  }

  public Asset getAsset(String id) throws IOException {
    final DataManagementApiAsset asset =
        get(ASSET_PATH + "/" + id, new TypeToken<DataManagementApiAsset>() {});
    return mapAsset(asset);
  }

  public Policy getPolicy(String id) throws IOException {
    final DataManagementApiPolicy policy =
        get(POLICY_PATH + "/" + id, new TypeToken<DataManagementApiPolicy>() {});
    return mapPolicy(policy);
  }

  public ContractDefinition getContractDefinition(String id)
      throws IOException, ClientProtocolException {
    final DataManagementApiContractDefinition contractDefinition =
        get(
            CONTRACT_DEFINITIONS_PATH + "/" + id,
            new TypeToken<DataManagementApiContractDefinition>() {});
    return mapContractDefinition(contractDefinition);
  }

  public void createAsset(Asset asset) throws IOException {
    final DataManagementApiDataAddress dataAddress = new DataManagementApiDataAddress();
    dataAddress.properties =
        Map.of(
            DataManagementApiDataAddress.TYPE,
            "HttpData",
            "endpoint",
            "https://jsonplaceholder.typicode.com/todos/1");

    final DataManagementApiAssetCreate assetCreate = new DataManagementApiAssetCreate();
    assetCreate.asset = mapAsset(asset);
    assetCreate.dataAddress = dataAddress;

    post(ASSET_PATH, assetCreate);
  }

  public void createPolicy(Policy policy) throws ClientProtocolException, IOException {
    post(POLICY_PATH, mapPolicy(policy));
  }

  public void createContractDefinition(ContractDefinition contractDefinition) throws IOException {
    post(CONTRACT_DEFINITIONS_PATH, mapContractDefinition(contractDefinition));
  }

  public Stream<Asset> getAllAssets() throws IOException, ClientProtocolException {
    final List<DataManagementApiAsset> assets =
        get(ASSET_PATH, PARAM_NO_LIMIT, new TypeToken<ArrayList<DataManagementApiAsset>>() {});
    return assets.stream().map(this::mapAsset);
  }

  public Stream<Policy> getAllPolicies() throws IOException {
    final List<DataManagementApiPolicy> policies =
        get(POLICY_PATH, PARAM_NO_LIMIT, new TypeToken<ArrayList<DataManagementApiPolicy>>() {});
    return policies.stream().map(this::mapPolicy);
  }

  public Stream<ContractDefinition> getAllContractDefinitions() throws IOException {
    final List<DataManagementApiContractDefinition> contractDefinitions =
        get(
            CONTRACT_DEFINITIONS_PATH,
            PARAM_NO_LIMIT,
            new TypeToken<ArrayList<DataManagementApiContractDefinition>>() {});
    return contractDefinitions.stream().map(this::mapContractDefinition);
  }

  public void deleteAsset(String id) throws IOException {
    delete(ASSET_PATH + "/" + id);
  }

  public void deletePolicy(String id) throws IOException {
    delete(POLICY_PATH + "/" + id);
  }

  public void deleteContractDefinition(String id) throws IOException {
    delete(CONTRACT_DEFINITIONS_PATH + "/" + id);
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

  private void delete(String path) throws IOException {
    final HttpDelete delete = new HttpDelete(dataMgmtUrl + path);

    sendRequest(delete);
  }

  private void post(String path, Object object) throws IOException {
    final String url = String.format("%s%s", dataMgmtUrl, path);
    final HttpPost post = new HttpPost(url);
    post.addHeader("Content-Type", "application/json");

    var json = new Gson().toJson(object);

    log.debug("POST Payload: " + json);

    post.setEntity(new StringEntity(json));
    sendRequest(post);
  }

  private HttpResponse sendRequest(HttpRequestBase request) throws IOException {
    request.addHeader("X-Api-Key", "password");

    log.debug(String.format("Send %-6s %s", request.getMethod(), request.getURI()));

    final HttpResponse response = httpClient.execute(request);
    if (200 > response.getStatusLine().getStatusCode()
        || response.getStatusLine().getStatusCode() >= 300) {
      throw new RuntimeException(
          String.format("Unexpected response: %s", response.getStatusLine()));
    }

    return response;
  }

  private Asset mapAsset(DataManagementApiAsset DataManagementApiAsset) {
    final String id = (String) DataManagementApiAsset.properties.get(DataManagementApiAsset.ID);
    final String description =
        (String) DataManagementApiAsset.properties.get(DataManagementApiAsset.DESCRIPTION);

    return new Asset(id, description);
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
    final List<DataManagementApiPermission> permission =
        policy.getPermission().stream().map(this::mapPermission).collect(Collectors.toList());
    final DataManagementApiPolicy apiObject = new DataManagementApiPolicy();
    apiObject.uid = policy.getId();
    apiObject.permissions = permission;
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
    if (!(constraint instanceof BusinessPartnerNumberConstraint))
      throw new UnsupportedOperationException(
          "Unsupported constraint type: " + constraint.getClass().getName());

    final BusinessPartnerNumberConstraint businessPartnerNumberConstraint =
        (BusinessPartnerNumberConstraint) constraint;

    final DataManagementApiLiteralExpression leftExpression =
        new DataManagementApiLiteralExpression();
    leftExpression.value = "BusinessPartnerNumber";

    final DataManagementApiLiteralExpression rightExpression =
        new DataManagementApiLiteralExpression();
    rightExpression.value = businessPartnerNumberConstraint.getBusinessPartnerNumber();

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

  private ContractDefinition mapContractDefinition(
      DataManagementApiContractDefinition dataManagementContractDefinition) {
    final String id = dataManagementContractDefinition.id;
    final String accessPolicy = dataManagementContractDefinition.accessPolicyId;
    final String contractPolicy = dataManagementContractDefinition.contractPolicyId;

    final List<String> assetIds;
    assetIds =
        dataManagementContractDefinition.getCriteria() == null
            ? new ArrayList<>()
            : dataManagementContractDefinition.getCriteria().stream()
                .filter(c -> c.left.equals(DataManagementApiAsset.ID))
                .filter(c -> c.op.equals("="))
                .map(DataManagementApiCriterion::getRight)
                .map(c -> (String) c)
                .collect(Collectors.toList());

    return new ContractDefinition(id, contractPolicy, accessPolicy, assetIds);
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
      criterion.left = DataManagementApiAsset.ID;
      criterion.op = "=";
      criterion.right = assetId;

      apiObject.criteria.add(criterion);
    }

    return apiObject;
  }

  @Data
  private class DataManagementApiAssetCreate {
    private DataManagementApiAsset asset;
    private DataManagementApiDataAddress dataAddress;
  }

  @Data
  private class DataManagementApiAsset {
    public static final String ID = "asset:prop:id";
    public static final String DESCRIPTION = "asset:prop:description";

    private Map<String, Object> properties;
  }

  @Data
  private class DataManagementApiDataAddress {
    public static final String TYPE = "type";
    private Map<String, Object> properties;
  }

  @Data
  private class DataManagementApiPolicy {
    private String uid;
    private List<DataManagementApiPermission> permissions;
  }

  @Data
  private class DataManagementApiPermission {
    private String edctype = "dataspaceconnector:permission";
    private DataManagementApiRuleAction action;
    private String target;
    private List<DataManagementApiConstraint> constraints;
  }

  @Data
  private class DataManagementApiConstraint {
    private String edctype = "AtomicConstraint";
    private DataManagementApiLiteralExpression leftExpression;
    private DataManagementApiLiteralExpression rightExpression;
    private String operator;
  }

  @Data
  private class DataManagementApiLiteralExpression {
    private String edctype = "dataspaceconnector:literalexpression";
    private String value;
  }

  @Data
  private class DataManagementApiRuleAction {
    private String type;
  }

  @Data
  private class DataManagementApiContractDefinition {
    private String id;
    private String accessPolicyId;
    private String contractPolicyId;
    private List<DataManagementApiCriterion> criteria;
  }

  @Data
  private class DataManagementApiCriterion {
    private Object left;
    private String op;
    private Object right;
  }

  @Data
  private class DataManagementApiContractOffer {
    private String id;
    private DataManagementApiPolicy policy;
    private DataManagementApiAsset asset;
    private String assetId;
  }

  @Data
  private class DataManagementApiContractOfferCatalog {
    private String id;
    private List<DataManagementApiContractOffer> contractOffers;
  }
}
