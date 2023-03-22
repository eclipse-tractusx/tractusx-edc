package org.eclipse.tractusx.edc.token;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;

import java.util.Map;

public class TestIdentityService implements IdentityService {

    private static final String BUSINESS_PARTNER_NUMBER = "BusinessPartnerNumber";
    private final String businessPartnerNumber;
    private TypeManager typeManager = new TypeManager();

    public TestIdentityService(String businessPartnerNumber) {
        this.businessPartnerNumber = businessPartnerNumber;
    }

    @Override
    public Result<TokenRepresentation> obtainClientCredentials(TokenParameters parameters) {
        var token = Map.of(BUSINESS_PARTNER_NUMBER, businessPartnerNumber);

        TokenRepresentation tokenRepresentation = TokenRepresentation.Builder.newInstance()
                .token(typeManager.writeValueAsString(token))
                .build();
        return Result.success(tokenRepresentation);
    }

    @Override
    public Result<ClaimToken> verifyJwtToken(TokenRepresentation tokenRepresentation, String audience) {

        var token = typeManager.readValue(tokenRepresentation.getToken(), Map.class);
        if (token.containsKey(BUSINESS_PARTNER_NUMBER)) {
            return Result.success(ClaimToken.Builder.newInstance().claim(BUSINESS_PARTNER_NUMBER, token.get(BUSINESS_PARTNER_NUMBER)).claim("referringConnector", token.get(BUSINESS_PARTNER_NUMBER)).build());
        }
        return Result.failure("Expected businessPartnerNumber, but token did not contain the claim");
    }

}
