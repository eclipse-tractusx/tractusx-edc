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
 * Makes every grid card that points to exactly one page clickable as a whole tile.
 *
 * The card's link gets a class whose ::after covers the card (see the stylesheet),
 * so a click anywhere on the tile is a click on the real link: middle-click, open in
 * new tab and Material's instant navigation keep working. Cards with several
 * different destinations are left alone.
 *
 * It also marks a card's closing "→ Go there" paragraph (a paragraph that is nothing
 * but one link) with tx-card__more, which the stylesheet pins to the card's bottom.
 * CSS alone cannot tell it from a sentence that merely contains one link.
 * Runs again after every instant navigation.
 */
(function () {
  "use strict";

  function markTiles() {
    document.querySelectorAll(".md-typeset .grid.cards > ul > li").forEach(function (card) {
      var last = card.lastElementChild;
      if (last && last.tagName === "P" && last !== card.firstElementChild) {
        var only = last.querySelectorAll("a[href]");
        if (only.length === 1 && last.textContent.trim() === only[0].textContent.trim()) {
          last.classList.add("tx-card__more");
        }
      }

      var links = Array.prototype.filter.call(card.querySelectorAll("a[href]"), function (a) {
        return !a.classList.contains("headerlink");
      });
      var targets = new Set(links.map(function (a) { return a.href; }));
      if (targets.size !== 1) {
        return;
      }
      card.classList.add("tx-tile");
      links[links.length - 1].classList.add("tx-tile__link");
    });
  }

  if (typeof document$ !== "undefined") {
    document$.subscribe(markTiles);
  } else {
    document.addEventListener("DOMContentLoaded", markTiles);
  }
})();
