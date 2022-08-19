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
 *       Mercedes-Benz Tech Innovation GmbH - Initial Implementation
 *
 */

package net.catenax.edc.tests.util;

import java.time.Duration;

public class Timeouts {
  private Timeouts() {}

  public static final Duration CONTRACT_NEGOTIATION = Duration.ofSeconds(90);
}
