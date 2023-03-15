/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Mercedes-Benz Tech Innovation GmbH - refactoring
 */

package org.eclipse.tractusx.edc.oauth2.jwk;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class JsonWebKey {
  @JsonProperty("kty")
  private String kty;

  @JsonProperty("use")
  private String use;

  @JsonProperty("kid")
  private String kid;

  @JsonProperty("x5t")
  private String x5t;

  @JsonProperty("n")
  private String nn;

  @JsonProperty("e")
  private String ee;

  @JsonProperty("x5c")
  private List<String> x5c;

  @JsonProperty("alg")
  private String alg;
}
