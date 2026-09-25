/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
 ********************************************************************************/

package org.eclipse.edc.connector.controlplane.transform.odrl;

import org.eclipse.edc.connector.controlplane.transform.odrl.to.JsonObjectToActionTransformer;
import org.eclipse.edc.connector.controlplane.transform.odrl.to.JsonObjectToConstraintTransformer;
import org.eclipse.edc.connector.controlplane.transform.odrl.to.JsonObjectToDutyTransformer;
import org.eclipse.edc.connector.controlplane.transform.odrl.to.JsonObjectToOperatorTransformer;
import org.eclipse.edc.connector.controlplane.transform.odrl.to.JsonObjectToPermissionTransformer;
import org.eclipse.edc.connector.controlplane.transform.odrl.to.JsonObjectToPolicyTransformer;
import org.eclipse.edc.connector.controlplane.transform.odrl.to.JsonObjectToProhibitionTransformer;
import org.eclipse.edc.participant.spi.ParticipantIdMapper;
import org.eclipse.edc.transform.spi.TypeTransformer;

import java.util.stream.Stream;

public final class OdrlTransformersFactory {

    private OdrlTransformersFactory() {
    }

    public static Stream<TypeTransformer<?, ?>> jsonObjectToOdrlTransformers(ParticipantIdMapper participantIdMapper) {
        return Stream.of(
                new JsonObjectToPolicyTransformer(participantIdMapper),
                new JsonObjectToPermissionTransformer(),
                new JsonObjectToProhibitionTransformer(),
                new JsonObjectToDutyTransformer(),
                new JsonObjectToActionTransformer(),
                new JsonObjectToConstraintTransformer(),
                new JsonObjectToOperatorTransformer()
        );
    }
}
