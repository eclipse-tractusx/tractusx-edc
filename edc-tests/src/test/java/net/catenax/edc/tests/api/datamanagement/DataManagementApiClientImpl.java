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

package net.catenax.edc.tests.api.datamanagement;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.catenax.edc.tests.data.Asset;
import net.catenax.edc.tests.data.Catalog;
import net.catenax.edc.tests.data.ContractDefinition;
import net.catenax.edc.tests.data.ContractNegotiation;
import net.catenax.edc.tests.data.Policy;
import net.catenax.edc.tests.data.TransferProcess;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

@Slf4j
@RequiredArgsConstructor
public class DataManagementApiClientImpl implements DataManagementApiClient, AutoCloseable {
  private static final String ASSET_PATH = "/assets";
  private static final String POLICY_PATH = "/policydefinitions";
  private static final String CONTRACT_DEFINITIONS_PATH = "/contractdefinitions";
  private static final String CATALOG_PATH = "/catalog";
  private static final String NEGOTIATIONS_PATH = "/contractnegotiations";
  private static final String TRANSFER_PROCESS_PATH = "/transferprocess";
  private static final String QUERY_PARAM_PROVIDER_URL = "providerUrl";
  private static final Gson GSON = new Gson();
  @NonNull private final String dataManagementUrl;
  @NonNull private final String dataManagementAuthKey;

  @Getter(lazy = true, value = AccessLevel.PRIVATE)
  private final CloseableHttpClient httpClient = loadHttpClient();

  @Override
  @SneakyThrows
  public Catalog getCatalog(@NonNull final String receivingConnectorUrl) {
    final URI uri =
        uri(
            Collections.singletonMap(QUERY_PARAM_PROVIDER_URL, receivingConnectorUrl),
            CATALOG_PATH);
    final HttpGet get = get(uri);

    if (log.isDebugEnabled()) {
      log.debug("Send {} {}", get.getMethod(), get.getURI());
    }

    final DataManagementApiContractOfferCatalog dataManagementApiContractOfferCatalog =
        getHttpClient().execute(get, DataManagementApiContractOfferCatalogResponseHandler.INSTANCE);

    if (log.isDebugEnabled()) {
      log.debug(
          "Received catalog {} with {} contract offers.",
          dataManagementApiContractOfferCatalog.getId(),
          Optional.ofNullable(dataManagementApiContractOfferCatalog.getContractOffers())
              .orElseGet(Collections::emptyList)
              .size());
    }

    return DataManagementApiContractOfferCatalogMapper.INSTANCE.map(
        dataManagementApiContractOfferCatalog);
  }

  @Override
  @SneakyThrows
  public ContractNegotiation initiateNegotiation(
      @NonNull final String receivingConnectorUrl,
      @NonNull final String definitionId,
      @NonNull final String assetId,
      @NonNull final Policy policy) {

    final DataManagementApiOffer offer = new DataManagementApiOffer();

    final String offerId = String.join(":", definitionId, "foo");
    offer.setOfferId(offerId);
    offer.setAssetId(assetId);

    Optional.ofNullable(DataManagementApiPolicyMapper.INSTANCE.map(policy))
        .ifPresent(
            dataManagementApiPolicy -> {
              Optional.ofNullable(dataManagementApiPolicy.getPermissions())
                  .orElseGet(Collections::emptyList)
                  .forEach(a -> a.setTarget(assetId));
              offer.setPolicy(dataManagementApiPolicy);
            });

    final DataManagementApiNegotiationPayload negotiationPayload =
        new DataManagementApiNegotiationPayload();
    negotiationPayload.setConnectorAddress(receivingConnectorUrl);
    negotiationPayload.setOffer(offer);

    final URI uri = uri(NEGOTIATIONS_PATH);
    final HttpPost post = post(uri);
    post.setEntity(entity(negotiationPayload));

    if (log.isDebugEnabled()) {
      log.debug("Send {} {} {}", post.getMethod(), post.getURI(), negotiationPayload);
    }

    final DataManagementApiNegotiation dataManagementApiNegotiation =
        getHttpClient().execute(post, DataManagementApiNegotiationResponseHandler.INSTANCE);

    if (dataManagementApiNegotiation == null) {
      throw new RuntimeException(
          "Initiated negotiation. Connector did not answer with negotiation ID.");
    }

    final ContractNegotiation contractNegotiation =
        DataManagementApiNegotiationMapper.INSTANCE.map(dataManagementApiNegotiation);

    if (log.isDebugEnabled()) {
      log.debug(
          "Initiated negotiation ( id={} )",
          Optional.ofNullable(contractNegotiation).map(ContractNegotiation::getId).orElse(null));
    }

    return contractNegotiation;
  }

  @Override
  @SneakyThrows
  public ContractNegotiation getNegotiation(final String id) {
    final URI uri = uri(NEGOTIATIONS_PATH, id);
    final HttpGet get = get(uri);

    if (log.isDebugEnabled()) {
      log.debug("Send {} {}", get.getMethod(), get.getURI());
    }

    final DataManagementApiNegotiation dataManagementApiNegotiation =
        getHttpClient().execute(get, DataManagementApiNegotiationResponseHandler.INSTANCE);

    return DataManagementApiNegotiationMapper.INSTANCE.map(dataManagementApiNegotiation);
  }

  @Override
  @SneakyThrows
  public void createAsset(final Asset asset) {
    final DataManagementApiDataAddress dataAddress = new DataManagementApiDataAddress();
    dataAddress.setProperties(
        Map.of(
            DataManagementApiDataAddress.TYPE,
            "HttpData",
            "baseUrl",
            "https://jsonplaceholder.typicode.com/todos/1"));

    final DataManagementApiAssetCreate assetCreate = new DataManagementApiAssetCreate();
    assetCreate.setAsset(DataManagementApiAssetMapper.INSTANCE.map(asset));
    assetCreate.setDataAddress(dataAddress);

    final URI uri = uri(ASSET_PATH);
    final HttpPost post = post(uri);
    post.setEntity(entity(assetCreate));

    if (log.isDebugEnabled()) {
      log.debug("Send {} {} {}", post.getMethod(), post.getURI(), assetCreate);
    }

    getHttpClient().execute(post, new StatusCodeResponseHandler(HttpStatus.SC_NO_CONTENT));
  }

  @Override
  @SneakyThrows
  public void createPolicy(final Policy policy) {
    final DataManagementApiPolicyDefinition dataManagementApiPolicyDefinition =
        DataManagementApiPolicyDefinitionMapper.INSTANCE.map(policy);

    final URI uri = uri(POLICY_PATH);
    final HttpPost post = post(uri);
    post.setEntity(entity(dataManagementApiPolicyDefinition));

    if (log.isDebugEnabled()) {
      log.debug(
          "Send {} {} {}", post.getMethod(), post.getURI(), dataManagementApiPolicyDefinition);
    }

    getHttpClient().execute(post, new StatusCodeResponseHandler(HttpStatus.SC_NO_CONTENT));
  }

  @Override
  @SneakyThrows
  public void createContractDefinition(final ContractDefinition contractDefinition) {
    final DataManagementApiContractDefinition dataManagementApiContractDefinition =
        DataManagementApiContractDefinitionMapper.INSTANCE.map(contractDefinition);

    final URI uri = uri(CONTRACT_DEFINITIONS_PATH);
    final HttpPost post = post(uri);
    post.setEntity(entity(dataManagementApiContractDefinition));

    if (log.isDebugEnabled()) {
      log.debug(
          "Send {} {} {}", post.getMethod(), post.getURI(), dataManagementApiContractDefinition);
    }

    getHttpClient().execute(post, new StatusCodeResponseHandler(HttpStatus.SC_NO_CONTENT));
  }

  @Override
  @SneakyThrows
  public String initiateTransferProcess(@NonNull final TransferProcess transferProcess) {
    final DataManagementApiTransferProcess dataManagementApiTransferProcess =
        DataManagementApiTransferProcessMapper.INSTANCE.map(transferProcess);

    final URI uri = uri(TRANSFER_PROCESS_PATH);
    final HttpPost post = post(uri);
    post.setEntity(entity(dataManagementApiTransferProcess));

    // TODO refactoring
    Map<String, String> response =
        getHttpClient()
            .execute(post, new GsonResponseHandler<>(new TypeToken<Map<String, String>>() {}));

    return response.get("id");
  }

  @Override
  public void close() throws Exception {
    getHttpClient().close();
  }

  private CloseableHttpClient loadHttpClient() {
    return HttpClientBuilder.create().build();
  }

  private HttpGet get(@NonNull final URI uri) {
    final HttpGet get = new HttpGet(uri);
    get.addHeader("Content-Type", "application/json");
    get.addHeader("Accept", "application/json");
    get.addHeader("X-Api-Key", dataManagementAuthKey);
    return get;
  }

  private HttpPost post(@NonNull final URI uri) {
    final HttpPost post = new HttpPost(uri);
    post.addHeader("Content-Type", "application/json");
    post.addHeader("Accept", "application/json");
    post.addHeader("X-Api-Key", dataManagementAuthKey);
    return post;
  }

  @SneakyThrows
  private URI uri(@NonNull final String... path) {
    return uri(Collections.emptyMap(), path);
  }

  @SneakyThrows
  private URI uri(@NonNull final Map<String, String> queryParams, @NonNull final String... path) {
    final List<String> pathSegments =
        Arrays.stream(path)
            .flatMap(
                string -> Arrays.stream(string.split("/")).filter(segment -> !segment.isBlank()))
            .collect(Collectors.toList());

    final URIBuilder uriBuilder = new URIBuilder(dataManagementUrl);

    uriBuilder.setPathSegments(
        Stream.concat(uriBuilder.getPathSegments().stream(), pathSegments.stream())
            .collect(Collectors.toList()));

    queryParams.forEach(uriBuilder::addParameter);

    return uriBuilder.build();
  }

  private HttpEntity entity(@NonNull final Object payload) {
    return new StringEntity(GSON.toJson(payload), ContentType.APPLICATION_JSON);
  }
}
