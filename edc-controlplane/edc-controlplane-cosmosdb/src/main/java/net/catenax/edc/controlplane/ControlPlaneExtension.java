package net.catenax.edc.controlplane;

import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;

public class ControlPlaneExtension implements ServiceExtension {

  @Inject private WebService webService;

  @Inject private AssetLoader assetLoader;

  @Inject private ContractDefinitionStore contractDefinitionStore;

  @Inject private TransferProcessStore transferProcessStore;

  @Override
  public String name() {
    return "Control Plane";
  }

  @Override
  public void initialize(ServiceExtensionContext context) {
    webService.registerResource(
        new ControlPlaneController(
            context.getMonitor(), assetLoader, contractDefinitionStore, transferProcessStore));
  }
}
