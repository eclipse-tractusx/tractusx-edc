/*
 * Copyright (c) 2022 ZF Friedrichshafen AG
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 * ZF Friedrichshafen AG - Initial API and Implementation
 *
 */

package net.catenax.edc.cp.adapter.process.datareference;

import net.catenax.edc.cp.adapter.messaging.Message;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;

public interface DataStore {
  EndpointDataReference exchangeMessage(Message message, String contractAgreementId);

  Message exchangeDataReference(EndpointDataReference dataReference, String contractAgreementId);

  void removeDataReference(String contractAgreementId);

  void removeMessage(String key);
}
