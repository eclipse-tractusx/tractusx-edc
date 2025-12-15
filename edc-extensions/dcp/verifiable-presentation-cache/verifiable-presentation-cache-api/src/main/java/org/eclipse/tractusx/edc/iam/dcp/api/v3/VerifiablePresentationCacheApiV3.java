package org.eclipse.tractusx.edc.iam.dcp.api.v3;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@OpenAPIDefinition(info = @Info(description = "With this API clients can interact with the Verifiable Presentation Cache to remove cached entries for a participant.", title = "Verifiable Presentation Cache API"))
@Tag(name = "Verifiable Presentation Cache")
public interface VerifiablePresentationCacheApiV3 {

    @Operation(description = "Removes all cached entries for a participant by ID.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Entries for the participant were successfully deleted."),
                    @ApiResponse(responseCode = "500", description = "An error occurred removing the entries from the cache.")
            })
    void removeCacheEntries(String participantId);

}
