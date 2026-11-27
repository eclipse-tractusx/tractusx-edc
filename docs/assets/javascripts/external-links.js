/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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
 * Assisted-By: Claude Code (claude-opus-5-5)
 ********************************************************************************/

/**
 * Opens links that leave the documentation site in a new tab, with rel="noopener",
 * so readers keep their place in the docs. Links within the site, anchors, and links
 * in the header (the repository, Ask Connector Sensei) are left as they are.
 * Runs again after every instant navigation.
 */
(function () {
  "use strict";

  function markExternalLinks() {
    document.querySelectorAll(".md-content a[href], .md-footer a[href], .md-nav a[href]").forEach(function (a) {
      var url;
      try {
        url = new URL(a.getAttribute("href"), window.location.href);
      } catch (e) {
        return;
      }
      if ((url.protocol === "http:" || url.protocol === "https:") && url.host !== window.location.host) {
        a.setAttribute("target", "_blank");
        a.setAttribute("rel", "noopener");
      }
    });
  }

  if (typeof document$ !== "undefined") {
    document$.subscribe(markExternalLinks);
  } else {
    document.addEventListener("DOMContentLoaded", markExternalLinks);
  }
})();
