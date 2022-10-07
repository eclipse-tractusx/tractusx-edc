package net.catenax.edc.xsuaa.authenticator;

import static java.lang.String.format;

import com.sap.cloud.security.config.Environments;
import com.sap.cloud.security.config.OAuth2ServiceConfiguration;
import com.sap.cloud.security.config.OAuth2ServiceConfigurationBuilder;
import com.sap.cloud.security.config.Service;
import com.sap.cloud.security.config.cf.CFConstants;
import java.util.Objects;
import org.eclipse.dataspaceconnector.api.auth.AuthenticationService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

@Provides(AuthenticationService.class)
public class XsuaaBasedServiceExtension implements ServiceExtension {

  @Override
  public void initialize(ServiceExtensionContext context) {
    Monitor monitor = context.getMonitor();
    monitor.info(format("API Authentication: using XSUAA Token"));
    OAuth2ServiceConfiguration serviceConfig;

    if (Objects.isNull(Environments.getCurrent().getXsuaaConfiguration())) {
      monitor.debug(
          "Service Config from Kubernetes environment could not be loaded. Loading service configs from local env variables");
      // In case of null, load the service configuration using environment variables(LOCAL TESTING
      // ONLY)
      serviceConfig = getServiceConfigFromEnvironmentVariables();
    } else serviceConfig = Environments.getCurrent().getXsuaaConfiguration();

    var authService = new XsuaaBasedAuthenticationService(monitor, serviceConfig);
    context.registerService(AuthenticationService.class, authService);
  }

  private OAuth2ServiceConfiguration getServiceConfigFromEnvironmentVariables() {
    return OAuth2ServiceConfigurationBuilder.forService(Service.XSUAA)
        .withProperty(CFConstants.XSUAA.APP_ID, System.getenv("XSUAA_APP_ID"))
        .withProperty(CFConstants.XSUAA.UAA_DOMAIN, System.getenv("XSUAA_UAA_DOMAIN"))
        .withUrl(System.getenv("XSUAA_AUTH_URL"))
        .withClientId(System.getenv("XSUAA_CLIENT_ID"))
        .withClientSecret(System.getenv("XSUAA_CLIENT_SECRET"))
        .build();
  }
}
