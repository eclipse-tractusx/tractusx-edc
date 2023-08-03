package org.eclipse.tractusx.edc.api.bpn;

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
import jakarta.json.JsonObject;
import org.eclipse.edc.web.spi.ApiErrorDetail;

import java.util.Set;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;

@OpenAPIDefinition(info = @Info(description = "With this API clients can create, read, update and delete BusinessPartnerNumber groups. It allows the assigning of BPNs to groups.", title = "Business Partner Group API"))
@Tag(name = "BusinessPartnerGroup")
public interface BusinessPartnerGroupApi {


    @Operation(description = "Resolves all groups for a particular BPN",
            responses = {
                    @ApiResponse(responseCode = "200", description = "An object containing an array with the assigned groups"),
                    @ApiResponse(responseCode = "404", description = "No entry for the given BPN was found"),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            })
    JsonObject resolve(@Parameter(name = "bpn", description = "The business partner number") String bpn);

    @Operation(description = "Deletes the entry for a particular BPN",
            responses = {
                    @ApiResponse(responseCode = "204", description = "The object was successfully deleted"),
                    @ApiResponse(responseCode = "404", description = "No entry for the given BPN was found"),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            })
    void deleteEntry(@Parameter(name = "bpn", description = "The business partner number") String bpn);

    @Operation(description = "Updates the entry for a particular BPN",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = ListSchema.class))),

            responses = {
                    @ApiResponse(responseCode = "204", description = "The object was successfully updated"),
                    @ApiResponse(responseCode = "404", description = "No entry for the given BPN was found"),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            })
    void updateEntry(JsonObject object);

    @Operation(description = "Creates an entry for a particular BPN",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = ListSchema.class))),

            responses = {
                    @ApiResponse(responseCode = "204", description = "The object was successfully created"),
                    @ApiResponse(responseCode = "409", description = "An entry already exists for that BPN"),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            })
    void createEntry(JsonObject entry);


    @Schema(name = "List", example = ListSchema.EXAMPLE)
    record ListSchema(
            @Schema(name = ID) String id,
            Set<String> groups
    ) {
        public static final String EXAMPLE = """
                {
                    "@context": {
                        "tx": "https://w3id.org/tractusx/v0.0.1/ns/"
                    },
                    "@id": "tx:BPN000001234",
                    "tx:groups": ["group1", "group2", "group3"]
                }
                """;
    }
}
