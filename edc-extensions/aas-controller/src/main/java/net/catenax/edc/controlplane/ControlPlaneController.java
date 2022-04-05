/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package net.catenax.edc.controlplane;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/v1")
public class ControlPlaneController {
  private final Monitor monitor;
  private final AssetLoader assetLoader;
  private final ContractDefinitionStore contractDefinitionStore;
  private final TransferProcessStore transferProcessStore;

  public ControlPlaneController(
      Monitor monitor,
      AssetLoader assetLoader,
      ContractDefinitionStore contractDefinitionStore,
      TransferProcessStore transferProcessStore) {
    this.monitor = monitor;
    this.assetLoader = assetLoader;
    this.contractDefinitionStore = contractDefinitionStore;
    this.transferProcessStore = transferProcessStore;
  }

  // TODO: most of these api will be replaced by data management api
  @Path("/assets")
  @POST
  public String createAsset(AssetEntryDto assetEntry) {
    var assetProperties = assetEntry.asset.properties;
    var asset = Asset.Builder.newInstance().properties(assetProperties).build();

    var dataAddressProperties = assetEntry.dataAddress.properties;
    var dataAddress = DataAddress.Builder.newInstance().properties(dataAddressProperties).build();
    monitor.debug("Create asset: " + asset.getId());
    assetLoader.accept(asset, dataAddress);
    return asset.getId();
  }

  @Path("/contractdefinitions")
  @POST
  public void createContractDefinition(ContractDefinition definition) {
    monitor.debug("Create contract definition: " + definition.getId());
    contractDefinitionStore.save(definition);
  }

  @Path("/transfers/{id}")
  @GET
  public TransferProcess getTransferProcess(@PathParam("id") String id) {
    return transferProcessStore.find(id);
  }

  private static class AssetDto {

    public AssetDto() {}

    Map<String, String> properties;

    public Map<String, String> getProperties() {
      return properties;
    }

    public void setProperties(Map<String, String> properties) {
      this.properties = properties;
    }
  }

  private static class DataAddressDto {

    public DataAddressDto() {}

    Map<String, String> properties;

    public Map<String, String> getProperties() {
      return properties;
    }

    public void setProperties(Map<String, String> properties) {
      this.properties = properties;
    }
  }

  private static class AssetEntryDto {

    public AssetEntryDto() {}

    private AssetDto asset;

    private DataAddressDto dataAddress;

    public AssetDto getAsset() {
      return asset;
    }

    public void setAsset(AssetDto asset) {
      this.asset = asset;
    }

    public DataAddressDto getDataAddress() {
      return dataAddress;
    }

    public void setDataAddress(DataAddressDto dataAddress) {
      this.dataAddress = dataAddress;
    }
  }
}
