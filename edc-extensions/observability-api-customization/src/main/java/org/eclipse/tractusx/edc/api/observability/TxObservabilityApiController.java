/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.api.observability;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.eclipse.edc.spi.system.health.HealthStatus;
import org.jetbrains.annotations.NotNull;


@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/check")
public class TxObservabilityApiController implements TxObservabilityApi {

    private final HealthCheckService healthCheckService;
    public TxObservabilityApiController(HealthCheckService provider) {
        healthCheckService = provider;
    }

    @GET
    @Path("health")
    @Override
    public Response checkHealth() {
        var status = healthCheckService.getStartupStatus();
        return createResponse(status);
    }

    @GET
    @Path("liveness")
    @Override
    public Response getLiveness() {
        var status = healthCheckService.isLive();
        return createResponse(status);

    }

    @GET
    @Path("readiness")
    @Override
    public Response getReadiness() {
        var status = healthCheckService.isReady();
        return createResponse(status);
    }

    @GET
    @Path("startup")
    @Override
    public Response getStartup() {
        var status = healthCheckService.getStartupStatus();
        return createResponse(status);
    }

    private Response createResponse(HealthStatus status) {
        return status.isHealthy() ?
                Response.ok().entity(status).build() :
                Response.status(503).entity(status).build();
    }

}
