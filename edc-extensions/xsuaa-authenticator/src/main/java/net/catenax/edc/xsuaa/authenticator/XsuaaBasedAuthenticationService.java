package net.catenax.edc.xsuaa.authenticator;

import com.sap.cloud.security.config.OAuth2ServiceConfiguration;
import com.sap.cloud.security.token.SecurityContext;
import com.sap.cloud.security.token.Token;
import com.sap.cloud.security.token.validation.CombiningValidator;
import com.sap.cloud.security.token.validation.ValidationResult;
import com.sap.cloud.security.token.validation.validators.JwtValidatorBuilder;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.eclipse.dataspaceconnector.api.auth.AuthenticationService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

public class XsuaaBasedAuthenticationService implements AuthenticationService {

  private static final String BEARER_TOKEN_AUTH_HEADER_NAME = "Authorization";

  private final OAuth2ServiceConfiguration serviceConfig;
  // logging service
  private final Monitor monitor;

  public XsuaaBasedAuthenticationService(
      Monitor monitor, OAuth2ServiceConfiguration serviceConfig) {
    this.monitor = monitor;
    this.serviceConfig = serviceConfig;
  }

  @Override
  public boolean isAuthenticated(Map<String, List<String>> headers) {
    Objects.requireNonNull(headers, "headers");

    return headers.keySet().stream()
        .filter(k -> k.equalsIgnoreCase(BEARER_TOKEN_AUTH_HEADER_NAME))
        .map(headers::get)
        .filter(list -> !list.isEmpty())
        .anyMatch(list -> list.stream().anyMatch(this::validateBearerToken));
  }

  private boolean validateBearerToken(Object authorizationHeader) {
    ValidationResult result;

    try {
      // Token Validators - supports tokens issued by xsuaa and ias
      Token token = Token.create(authorizationHeader.toString());
      CombiningValidator<Token> validators =
          JwtValidatorBuilder.getInstance(this.serviceConfig).build();
      result = validators.validate(token);

      if (result.isErroneous()) {
        this.monitor.severe("Invalid token" + result.getErrorDescription());
        return false;
      }
      // SecurityContext caches only successfully validated tokens within the same thread
      SecurityContext.setToken(token);
      this.monitor.info("Token validated successfully");
    } catch (Exception e) {
      this.monitor.severe("Token Invalid Exception: " + e.getMessage());
      e.printStackTrace();
      return false;
    }

    return result.isValid();
  }
}
