package org.eclipse.tractusx.edc.token;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;

import java.util.Map;

import static java.lang.String.format;

public class MockDapsService implements IdentityService {

    private static final String BUSINESS_PARTNER_NUMBER_CLAIM = "BusinessPartnerNumber";
    private static final String REFERRING_CONNECTOR_CLAIM = "referringConnector";
    private final String businessPartnerNumber;
    private TypeManager typeManager = new TypeManager();

    public MockDapsService(String businessPartnerNumber) {
        this.businessPartnerNumber = businessPartnerNumber;
    }

    @Override
    public Result<TokenRepresentation> obtainClientCredentials(TokenParameters parameters) {
        var token = Map.of(BUSINESS_PARTNER_NUMBER_CLAIM, businessPartnerNumber);

        TokenRepresentation tokenRepresentation = TokenRepresentation.Builder.newInstance()
                .token(typeManager.writeValueAsString(token))
                .build();
        return Result.success(tokenRepresentation);
    }

    @Override
    public Result<ClaimToken> verifyJwtToken(TokenRepresentation tokenRepresentation, String audience) {

        var token = typeManager.readValue(tokenRepresentation.getToken(), Map.class);
        if (token.containsKey(BUSINESS_PARTNER_NUMBER_CLAIM)) {
            return Result.success(ClaimToken.Builder.newInstance()
                    .claim(BUSINESS_PARTNER_NUMBER_CLAIM, token.get(BUSINESS_PARTNER_NUMBER_CLAIM))
                    .claim(REFERRING_CONNECTOR_CLAIM, token.get(BUSINESS_PARTNER_NUMBER_CLAIM)).build());
        }
        return Result.failure(format("Expected %s and %s claims, but token did not contain them", BUSINESS_PARTNER_NUMBER_CLAIM, REFERRING_CONNECTOR_CLAIM));
    }

}
