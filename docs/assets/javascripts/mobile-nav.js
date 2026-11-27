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
 * The navigation drawer on phones.
 *
 * The stylesheet turns Material's slide-in levels into an accordion: the drawer opens
 * at the main navigation, with the sections that lead to the current page open below
 * their rows. On a deep page the current page can be far down the list, so opening the
 * drawer scrolls it into view.
 */
(function () {
  "use strict";

  var MOBILE = window.matchMedia("(max-width: 59.984375em)");

  document.addEventListener("change", function (event) {
    var drawer = event.target;
    if (!drawer || drawer.id !== "__drawer" || !drawer.checked || !MOBILE.matches) {
      return;
    }
    var nav = document.querySelector(".md-sidebar--primary .md-nav--primary");
    var list = nav && nav.querySelector(":scope > .md-nav__list");
    var links = nav ? nav.querySelectorAll("a.md-nav__link--active") : [];
    var active = links[links.length - 1];
    if (!list || !active) {
      return;
    }
    var row = active.closest(".md-nav__item") || active;
    var top = row.getBoundingClientRect().top - list.getBoundingClientRect().top + list.scrollTop;
    // Leave the parent sections in sight above the current page.
    list.scrollTop = Math.max(0, top - list.clientHeight / 3);
  });
})();
