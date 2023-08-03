package org.eclipse.tractusx.edc.api.bpn;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectConflictException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;
import org.eclipse.tractusx.edc.validation.businesspartner.spi.BusinessPartnerStore;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_NAMESPACE;


@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/business-partner-groups")
public class BusinessPartnerGroupApiController implements BusinessPartnerGroupApi {

    private final BusinessPartnerStore businessPartnerService;


    public BusinessPartnerGroupApiController(BusinessPartnerStore businessPartnerService) {
        this.businessPartnerService = businessPartnerService;
    }

    @GET
    @Path("/{bpn}")
    @Override
    public JsonObject resolve(@PathParam("bpn") String bpn) {

        // StoreResult does not support the .map() operator, because it does not override newInstance()
        var result = businessPartnerService.resolveForBpn(bpn);
        if (result.succeeded()) {
            return createObject(bpn, result.getContent());
        }

        throw new ObjectNotFoundException(List.class, result.getFailureDetail());
    }

    @DELETE
    @Path("/{bpn}")
    @Override
    public void deleteEntry(@PathParam("bpn") String bpn) {
        businessPartnerService.delete(bpn)
                .orElseThrow(f -> new ObjectNotFoundException(List.class, f.getFailureDetail()));
    }

    @PUT
    @Override
    public void updateEntry(@RequestBody JsonObject object) {
        var bpn = getBpn(object);
        var groups = getGroups(object);
        businessPartnerService.update(bpn, groups)
                .orElseThrow(f -> new ObjectNotFoundException(List.class, f.getFailureDetail()));
    }

    @POST
    @Override
    public void createEntry(@RequestBody JsonObject object) {
        var bpn = getBpn(object);
        var groups = getGroups(object);
        businessPartnerService.save(bpn, groups)
                .orElseThrow(f -> new ObjectConflictException(f.getFailureDetail()));
    }

    private JsonObject createObject(String bpn, List<String> list) {
        return Json.createObjectBuilder()
                .add(ID, bpn)
                .add(TX_NAMESPACE + "groups", Json.createArrayBuilder(list))
                .build();
    }


    private String getBpn(JsonObject object) {
        try {
            return object.getString(ID);
        } catch (Exception ex) {
            throw new InvalidRequestException(ex.getMessage());
        }
    }

    @NotNull
    private List<String> getGroups(JsonObject object) {
        try {
            return object.getJsonArray(TX_NAMESPACE + "groups").stream().map(jv -> ((JsonString) jv).getString()).toList();
        } catch (Exception ex) {
            throw new InvalidRequestException(ex.getMessage());
        }
    }

}
