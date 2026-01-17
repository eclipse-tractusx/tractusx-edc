/*
 * Copyright (c) 2026 Cofinity-X GmbH
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

package org.eclipse.tractusx.edc.discovery.v4alpha.service;

import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.IdentifierToDidMapper;

import java.util.Collection;

import static java.util.List.of;

public class AggregatedIdentifierMapper implements IdentifierToDidMapper {
    private final Collection<IdentifierToDidMapper> aggregatedMapper;

    public AggregatedIdentifierMapper(IdentifierToDidMapper... mappers) {
        this.aggregatedMapper = of(mappers);
    }

    @Override
    public boolean canHandle(String identifier) {
        return aggregatedMapper.stream().anyMatch(mapper -> mapper.canHandle(identifier));
    }

    @Override
    public String mapToDid(String identifier) {
        return aggregatedMapper.stream()
                .filter(mapper -> mapper.canHandle(identifier))
                .findAny()
                .map(mapper -> mapper.mapToDid(identifier))
                .orElseThrow(() -> new InvalidRequestException("Given counterPartyId %s is of unknown type".formatted(identifier)));
    }
}
