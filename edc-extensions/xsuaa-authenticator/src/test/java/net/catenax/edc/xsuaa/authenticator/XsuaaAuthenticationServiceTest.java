package net.catenax.edc.xsuaa.authenticator;

import com.sap.cloud.security.config.OAuth2ServiceConfiguration;
import com.sap.cloud.security.config.OAuth2ServiceConfigurationBuilder;
import com.sap.cloud.security.config.Service;
import com.sap.cloud.security.config.cf.CFConstants;
import com.sap.cloud.security.test.JwtGenerator;
import com.sap.cloud.security.test.SecurityTestRule;
import com.sap.cloud.security.token.Token;
import java.util.*;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class XsuaaAuthenticationServiceTest {

  private Token token;
  private String authorizationHeaderValue;
  private Monitor monitor;
  private Map<String, List<String>> httpHeaders;
  private OAuth2ServiceConfiguration serviceConfiguration;

  @BeforeEach
  void setup() {
    monitor = Mockito.mock(Monitor.class);

    serviceConfiguration =
        OAuth2ServiceConfigurationBuilder.forService(Service.XSUAA)
            .withProperty(CFConstants.XSUAA.APP_ID, SecurityTestRule.DEFAULT_APP_ID)
            .withProperty(CFConstants.XSUAA.UAA_DOMAIN, SecurityTestRule.DEFAULT_DOMAIN)
            .withClientId(SecurityTestRule.DEFAULT_CLIENT_ID)
            .withClientSecret("dummyClientSecret")
            .build();

    token =
        JwtGenerator.getInstance(Service.XSUAA, SecurityTestRule.DEFAULT_CLIENT_ID)
            .withAppId(SecurityTestRule.DEFAULT_APP_ID)
            .withClaimValue(CFConstants.CLIENT_ID, SecurityTestRule.DEFAULT_CLIENT_ID)
            .withClaimValue(CFConstants.CLIENT_SECRET, "dummyClientSecret")
            .createToken();

    authorizationHeaderValue = "Bearer " + token.getTokenValue();

    httpHeaders = new HashMap<>();
    httpHeaders.put(
        "Content-Type", new ArrayList<String>(Collections.singleton("application/json")));
  }

  @Test
  void testIsTokenNull() {
    Assertions.assertNotNull(token);
  }

  @Test
  void testAuthorizationHeaderIsNUll() {
    XsuaaBasedAuthenticationService xsuaaBasedAuthenticationService =
        new XsuaaBasedAuthenticationService(monitor, serviceConfiguration);
    Assertions.assertNull(httpHeaders.get("Authorization"));
    Assertions.assertFalse(xsuaaBasedAuthenticationService.isAuthenticated(httpHeaders));
  }

  @Test
  void testAuthorizationHeader() {
    httpHeaders.put(
        "Authorization", new ArrayList<String>(Collections.singleton(authorizationHeaderValue)));
    Assertions.assertNotNull(httpHeaders.get("Authorization"));
  }
}
