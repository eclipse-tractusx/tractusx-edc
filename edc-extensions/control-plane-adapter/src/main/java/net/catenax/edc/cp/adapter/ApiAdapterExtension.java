/*
 * Copyright (c) 2022 ZF Friedrichshafen AG
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 * ZF Friedrichshafen AG - Initial API and Implementation
 *
 */

package net.catenax.edc.cp.adapter;

import static java.util.Objects.nonNull;

import net.catenax.edc.cp.adapter.messaging.Channel;
import net.catenax.edc.cp.adapter.messaging.InMemoryMessageBus;
import net.catenax.edc.cp.adapter.messaging.ListenerService;
import net.catenax.edc.cp.adapter.process.contractdatastore.ContractDataStore;
import net.catenax.edc.cp.adapter.process.contractdatastore.InMemoryContractDataStore;
import net.catenax.edc.cp.adapter.process.contractnegotiation.ContractNegotiationHandler;
import net.catenax.edc.cp.adapter.process.contractnotification.*;
import net.catenax.edc.cp.adapter.process.datareference.DataRefInMemorySyncService;
import net.catenax.edc.cp.adapter.process.datareference.DataRefNotificationSyncService;
import net.catenax.edc.cp.adapter.process.datareference.DataReferenceHandler;
import net.catenax.edc.cp.adapter.process.datareference.EndpointDataReferenceReceiverImpl;
import net.catenax.edc.cp.adapter.service.ErrorResultService;
import net.catenax.edc.cp.adapter.service.ResultService;
import net.catenax.edc.cp.adapter.util.ExpiringMap;
import net.catenax.edc.cp.adapter.util.LockMap;
import org.eclipse.dataspaceconnector.api.datamanagement.catalog.service.CatalogServiceImpl;
import org.eclipse.dataspaceconnector.api.datamanagement.configuration.DataManagementApiConfiguration;
import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.service.ContractNegotiationService;
import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.service.TransferProcessService;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.observe.ContractNegotiationListener;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceReceiver;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceReceiverRegistry;

public class ApiAdapterExtension implements ServiceExtension {
  @Inject private Monitor monitor;
  @Inject private ContractNegotiationObservable negotiationObservable;
  @Inject private WebService webService;
  @Inject private ContractNegotiationService contractNegotiationService;
  @Inject private RemoteMessageDispatcherRegistry dispatcher;
  @Inject private EndpointDataReferenceReceiverRegistry receiverRegistry;
  @Inject private DataManagementApiConfiguration apiConfig;
  @Inject private TransferProcessService transferProcessService;

  @Override
  public String name() {
    return "Control Plane Adapter Extension";
  }

  @Override
  public void initialize(ServiceExtensionContext context) {
    ApiAdapterConfig config = new ApiAdapterConfig(context);
    ListenerService listenerService = new ListenerService();
    InMemoryMessageBus messageBus =
        new InMemoryMessageBus(
            monitor, listenerService, config.getInMemoryMessageBusThreadNumber());

    ResultService resultService = new ResultService(config.getDefaultSyncRequestTimeout());
    ErrorResultService errorResultService = new ErrorResultService(monitor, messageBus);
    ContractNotificationSyncService contractSyncService =
        new ContractInMemorySyncService(new LockMap());
    ContractDataStore contractDataStore = new InMemoryContractDataStore();
    DataTransferInitializer dataTransferInitializer =
        new DataTransferInitializer(monitor, transferProcessService);
    ContractNotificationHandler contractNotificationHandler =
        new ContractNotificationHandler(
            monitor,
            messageBus,
            contractSyncService,
            contractNegotiationService,
            dataTransferInitializer);
    ContractNegotiationHandler contractNegotiationHandler =
        getContractNegotiationHandler(
            monitor, contractNegotiationService, messageBus, contractDataStore);
    DataRefNotificationSyncService dataRefSyncService =
        new DataRefInMemorySyncService(new LockMap());
    DataReferenceHandler dataReferenceHandler =
        new DataReferenceHandler(monitor, messageBus, dataRefSyncService);

    listenerService.addListener(Channel.INITIAL, contractNegotiationHandler);
    listenerService.addListener(Channel.CONTRACT_CONFIRMATION, contractNotificationHandler);
    listenerService.addListener(Channel.DATA_REFERENCE, dataReferenceHandler);
    listenerService.addListener(Channel.RESULT, resultService);
    listenerService.addListener(Channel.DLQ, errorResultService);

    initHttpController(monitor, messageBus, resultService, config);
    initContractNegotiationListener(
        monitor,
        negotiationObservable,
        messageBus,
        contractSyncService,
        contractDataStore,
        dataTransferInitializer);
    initDataReferenceReciever(monitor, messageBus, dataRefSyncService);
  }

  private void initHttpController(
      Monitor monitor,
      InMemoryMessageBus messageBus,
      ResultService resultService,
      ApiAdapterConfig config) {
    webService.registerResource(
        apiConfig.getContextAlias(),
        new HttpController(monitor, resultService, messageBus, config));
  }

  private ContractNegotiationHandler getContractNegotiationHandler(
      Monitor monitor,
      ContractNegotiationService contractNegotiationService,
      InMemoryMessageBus messageBus,
      ContractDataStore contractDataStore) {
    return new ContractNegotiationHandler(
        monitor,
        messageBus,
        contractNegotiationService,
        new CatalogServiceImpl(dispatcher),
        contractDataStore,
        new ExpiringMap<>());
  }

  private void initDataReferenceReciever(
      Monitor monitor,
      InMemoryMessageBus messageBus,
      DataRefNotificationSyncService dataRefSyncService) {
    EndpointDataReferenceReceiver dataReferenceReceiver =
        new EndpointDataReferenceReceiverImpl(monitor, messageBus, dataRefSyncService);
    receiverRegistry.registerReceiver(dataReferenceReceiver);
  }

  private void initContractNegotiationListener(
      Monitor monitor,
      ContractNegotiationObservable negotiationObservable,
      InMemoryMessageBus messageBus,
      ContractNotificationSyncService contractSyncService,
      ContractDataStore contractDataStore,
      DataTransferInitializer dataTransferInitializer) {
    ContractNegotiationListener contractNegotiationListener =
        new ContractNegotiationListenerImpl(
            monitor, messageBus, contractSyncService, contractDataStore, dataTransferInitializer);
    if (nonNull(negotiationObservable)) {
      negotiationObservable.registerListener(contractNegotiationListener);
    }
  }
}
