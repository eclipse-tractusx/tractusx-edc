package net.catenax.edc.cp.adapter;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

import net.catenax.edc.cp.adapter.messaging.Channel;
import net.catenax.edc.cp.adapter.messaging.InMemoryMessageService;
import net.catenax.edc.cp.adapter.messaging.ListenerService;
import net.catenax.edc.cp.adapter.process.contractconfirmation.ContractConfirmationHandler;
import net.catenax.edc.cp.adapter.process.contractconfirmation.DataStoreLock;
import net.catenax.edc.cp.adapter.process.contractconfirmation.InMemoryDataStore;
import net.catenax.edc.cp.adapter.process.contractdatastore.InMemoryContractDataStore;
import net.catenax.edc.cp.adapter.process.contractnegotiation.ContractNegotiationHandler;
import net.catenax.edc.cp.adapter.process.datareference.DataReferenceHandler;
import net.catenax.edc.cp.adapter.service.ResultService;
import org.eclipse.dataspaceconnector.api.datamanagement.catalog.service.CatalogServiceImpl;
import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.service.ContractNegotiationService;
import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.service.ContractNegotiationServiceImpl;
import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.service.TransferProcessServiceImpl;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transaction.NoopTransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceReceiverRegistry;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;

public class ApiAdapterExtension implements ServiceExtension {
  @Inject private WebService webService;
  @Inject private ContractNegotiationStore store;
  @Inject private ConsumerContractNegotiationManager manager;
  @Inject private TransactionContext transactionContext;
  @Inject private RemoteMessageDispatcherRegistry dispatcher;
  @Inject private TransferProcessStore transferProcessStore;
  @Inject private TransferProcessManager transferProcessManager;
  @Inject private EndpointDataReferenceReceiverRegistry receiverRegistry;

  @Override
  public String name() {
    return "Control Plane Adapter Extension";
  }

  @Override
  public void initialize(ServiceExtensionContext context) {
    /** external dependencies * */
    Monitor monitor = context.getMonitor();
    ContractNegotiationObservable negotiationObservable =
        context.getService(ContractNegotiationObservable.class, false);

    /** internal dependencies * */
    ContractNegotiationService contractNegotiationService =
        new ContractNegotiationServiceImpl(store, manager, getTransactionContext(monitor));
    ListenerService listenerService = new ListenerService();
    InMemoryMessageService messageService = new InMemoryMessageService(monitor, listenerService);
    ResultService resultService = new ResultService();
    listenerService.addListener(Channel.RESULT, resultService);

    initHttpController(monitor, messageService, resultService);
    initContractNegotiationHandler(
        monitor, contractNegotiationService, messageService, listenerService);
    initContractConfirmationHandler(
        monitor,
        negotiationObservable,
        contractNegotiationService,
        messageService,
        listenerService);
    initDataReferenceHandler(monitor, messageService, listenerService);
  }

  private void initHttpController(
      Monitor monitor, InMemoryMessageService messageService, ResultService resultService) {
    webService.registerResource(new HttpController(monitor, resultService, messageService));
  }

  private void initContractNegotiationHandler(
      Monitor monitor,
      ContractNegotiationService contractNegotiationService,
      InMemoryMessageService messageService,
      ListenerService listenerService) {

    listenerService.addListener(
        Channel.INITIAL,
        new ContractNegotiationHandler(
            monitor,
            messageService,
            contractNegotiationService,
            new CatalogServiceImpl(dispatcher),
            new InMemoryContractDataStore()));
  }

  private void initContractConfirmationHandler(
      Monitor monitor,
      ContractNegotiationObservable negotiationObservable,
      ContractNegotiationService contractNegotiationService,
      InMemoryMessageService messageService,
      ListenerService listenerService) {

    ContractConfirmationHandler contractConfirmationHandler =
        new ContractConfirmationHandler(
            monitor,
            messageService,
            new InMemoryDataStore(new DataStoreLock()),
            contractNegotiationService,
            new TransferProcessServiceImpl(
                transferProcessStore, transferProcessManager, getTransactionContext(monitor)),
            new InMemoryContractDataStore());

    listenerService.addListener(Channel.CONTRACT_CONFIRMATION, contractConfirmationHandler);
    if (nonNull(negotiationObservable)) {
      negotiationObservable.registerListener(contractConfirmationHandler);
    }
  }

  private void initDataReferenceHandler(
      Monitor monitor, InMemoryMessageService messageService, ListenerService listenerService) {

    DataReferenceHandler dataReferenceHandler =
        new DataReferenceHandler(
            monitor,
            messageService,
            new net.catenax.edc.cp.adapter.process.datareference.InMemoryDataStore());
    listenerService.addListener(Channel.DATA_REFERENCE, dataReferenceHandler);
    receiverRegistry.registerReceiver(dataReferenceHandler);
  }

  private TransactionContext getTransactionContext(Monitor monitor) {
    return ofNullable(transactionContext)
        .orElseGet(
            () -> {
              monitor.warning(
                  "No TransactionContext registered, a no-op implementation will be used, not suitable for production environments");
              return new NoopTransactionContext();
            });
  }
}
