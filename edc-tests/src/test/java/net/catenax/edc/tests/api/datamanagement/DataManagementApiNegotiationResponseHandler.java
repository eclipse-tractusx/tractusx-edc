/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *
 */

package net.catenax.edc.tests.api.datamanagement;

import com.google.gson.reflect.TypeToken;

public class DataManagementApiNegotiationResponseHandler
    extends GsonResponseHandler<DataManagementApiNegotiation> {
  public static final DataManagementApiNegotiationResponseHandler INSTANCE =
      new DataManagementApiNegotiationResponseHandler();

  protected DataManagementApiNegotiationResponseHandler() {
    super(new TypeToken<DataManagementApiNegotiation>() {});
  } // Keep - JVM type erasure!
}
