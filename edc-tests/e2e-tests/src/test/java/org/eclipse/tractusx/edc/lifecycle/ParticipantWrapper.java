/*
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.lifecycle;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.InjectionContainer;
import org.eclipse.tractusx.edc.token.MockDapsService;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.List;
import java.util.Map;

import static org.eclipse.tractusx.edc.helpers.AssetHelperFunctions.createDataAddressBuilder;


public class ParticipantWrapper extends EdcRuntimeExtension implements BeforeAllCallback, AfterAllCallback {

    private final Participant participant;
    private DataWiper wiper;

    public ParticipantWrapper(String moduleName, String runtimeName, String bpn, Map<String, String> properties) {
        super(moduleName, runtimeName, properties);
        participant = new Participant(runtimeName, bpn, properties);
        this.registerServiceMock(IdentityService.class, new MockDapsService(getBpn()));
    }

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) {
        //do nothing - we only want to start the runtime once
        wiper.clearPersistence();
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        //only run this once
        super.beforeTestExecution(context);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        super.afterTestExecution(context);
    }


    /**
     * Creates an asset with the given ID and props using the participant's Data Management API
     */
    public void createAsset(String id) {
        createAsset(id, Json.createObjectBuilder().build(), createDataAddressBuilder("test-type").build());
    }

    /**
     * Creates an asset with the given ID and props using the participant's Data Management API
     */
    public void createAsset(String id, JsonObject assetProperties, JsonObject dataAddress) {
        participant.createAsset(id, assetProperties, dataAddress);
    }

    /**
     * Creates a {@link org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition} using the participant's Data Management API
     */
    public void createContractDefinition(String assetId, String definitionId, String accessPolicyId, String contractPolicyId) {
        participant.createContractDefinition(assetId, definitionId, accessPolicyId, contractPolicyId);
    }

    public void createPolicy(JsonObject policyDefinition) {
        participant.createPolicy(policyDefinition);
    }

    public void negotiateEdr(ParticipantWrapper other, String assetId, JsonArray callbacks) {
        participant.negotiateEdr(other.getParticipant(), assetId, callbacks);
    }


    /**
     * Returns this participant's BusinessPartnerNumber (=BPN). This is constructed of the runtime name plus "-BPN"
     */
    public String getBpn() {
        return participant.getBpn();
    }


    public JsonArray getCatalogDatasets(ParticipantWrapper provider) {
        return getCatalogDatasets(provider, null);
    }

    public JsonArray getCatalogDatasets(ParticipantWrapper provider, JsonObject querySpec) {
        return participant.getCatalogDatasets(provider.getParticipant(), querySpec);
    }

    public JsonObject getDatasetForAsset(ParticipantWrapper provider, String assetId) {
        return participant.getDatasetForAsset(provider.getParticipant(), assetId);
    }


    @Override
    protected void bootExtensions(ServiceExtensionContext context, List<InjectionContainer<ServiceExtension>> serviceExtensions) {
        super.bootExtensions(context, serviceExtensions);
        wiper = new DataWiper(context);
    }


    public Participant getParticipant() {
        return participant;
    }
}
