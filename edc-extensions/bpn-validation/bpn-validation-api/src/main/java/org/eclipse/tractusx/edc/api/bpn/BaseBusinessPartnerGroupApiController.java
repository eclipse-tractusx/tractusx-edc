/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.edc.api.bpn;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.ws.rs.PathParam;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectConflictException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;
import org.eclipse.tractusx.edc.validation.businesspartner.spi.BusinessPartnerStore;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_NAMESPACE;


public abstract class BaseBusinessPartnerGroupApiController {

    private final BusinessPartnerStore businessPartnerService;

    public BaseBusinessPartnerGroupApiController(BusinessPartnerStore businessPartnerService) {
        this.businessPartnerService = businessPartnerService;
    }

    public JsonObject resolve(String bpn) {

        // StoreResult does not support the .map() operator, because it does not override newInstance()
        var result = businessPartnerService.resolveForBpn(bpn);
        if (result.succeeded()) {
            return createObject(bpn, result.getContent());
        }

        throw new ObjectNotFoundException(List.class, result.getFailureDetail());
    }

    public void deleteEntry(@PathParam("bpn") String bpn) {
        businessPartnerService.delete(bpn)
                .orElseThrow(f -> new ObjectNotFoundException(List.class, f.getFailureDetail()));
    }

    public void updateEntry(@RequestBody JsonObject object) {
        var bpn = getBpn(object);
        var groups = getGroups(object);
        businessPartnerService.update(bpn, groups)
                .orElseThrow(f -> new ObjectNotFoundException(List.class, f.getFailureDetail()));
    }

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
            return object.getJsonArray(TX_NAMESPACE + "groups")
                    .stream()
                    .map(jv -> ((JsonString) jv.asJsonObject().get(VALUE)).getString())
                    .toList();
        } catch (Exception ex) {
            throw new InvalidRequestException(ex.getMessage());
        }
    }

}
