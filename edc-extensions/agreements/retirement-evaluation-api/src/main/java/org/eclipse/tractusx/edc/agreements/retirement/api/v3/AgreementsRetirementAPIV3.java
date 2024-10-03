package org.eclipse.tractusx.edc.agreements.retirement.api.v3;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.web.spi.ApiErrorDetail;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;

@OpenAPIDefinition(info = @Info(description = "With this API clients can retire an active Contract Agreement. Clients can also list all retired agreements.", title = "Agreements Retirement API"))
@Tag(name = "Agreements Retirement")
public interface AgreementsRetirementAPIV3 {


    @Operation(description = "Get all retired contract agreements.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "A list of retired contract agreements"),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            })
    JsonArray getAllRetiredV3(JsonObject querySpecJson);

    @Operation(description = "Removes a contract agreement from the retired list, reactivating it.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "The contract agreement is reactivated"),
                    @ApiResponse(responseCode = "404", description = "No entry for the given agreementId was found"),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            })
    void reactivateRetiredV3(@Parameter(name = "agreementId", description = "The contract agreement id") String agreementId);

    @Operation(description = "Retires an active contract agreement.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = RetirementSchema.class))),

            responses = {
                    @ApiResponse(responseCode = "204", description = "The contract agreement was successfully retired"),
                    @ApiResponse(responseCode = "409", description = "The contract agreement is already retired"),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            })
    void retireAgreementV3(JsonObject entry);


    @Schema(name = "Retirement Example", example = RetirementSchema.EXAMPLE)
    record RetirementSchema(
            @Schema(name = ID) String id,
            String reason
    ) {
        public static final String EXAMPLE = """
                {
                    "@context": {
                        "tx": "https://w3id.org/tractusx/v0.0.1/ns/"
                    },
                    "@id": "contract-agreement-id",
                    "tx:reason": "This contract agreement was retired since the physical counterpart is no longer valid."
                }
                """;
    }

}
