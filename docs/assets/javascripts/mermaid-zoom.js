/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * Copyright (c) 2026 Catena-X Automotive Network e.V.
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
 * Assisted-By: GitHub Copilot (claude-opus-5.5)
 ********************************************************************************/
// Taken unchanged from the Tractus-X TestLab documentation
// (https://github.com/eclipse-tractusx/tractusx-testlab/blob/main/docs/assets/javascripts/mermaid-zoom.js).

/**
 * Zoom button for Mermaid diagrams in MkDocs Material.
 *
 * MkDocs Material renders mermaid SVGs inside a closed Shadow DOM,
 * so we cannot clone the SVG. Instead, we temporarily move the
 * actual element into a fullscreen modal and move it back on close.
 *
 * Uses MutationObserver to detect rendered .mermaid containers.
 */
(function () {
  "use strict";

  var ZOOM_ICON = '<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/><line x1="11" y1="8" x2="11" y2="14"/><line x1="8" y1="11" x2="14" y2="11"/></svg>';

  function openModal(mermaidEl, placeholder) {
    var overlay = document.createElement("div");
    overlay.className = "mermaid-modal-overlay";

    var closeBtn = document.createElement("button");
    closeBtn.className = "mermaid-modal-close";
    closeBtn.setAttribute("aria-label", "Close diagram");
    closeBtn.innerHTML = "&times;";

    var content = document.createElement("div");
    content.className = "mermaid-modal-content";

    // Move the actual element into the modal
    content.appendChild(mermaidEl);

    overlay.appendChild(closeBtn);
    overlay.appendChild(content);
    document.body.appendChild(overlay);

    void overlay.offsetWidth;
    overlay.classList.add("visible");
    document.body.style.overflow = "hidden";

    function close() {
      overlay.classList.remove("visible");
      document.body.style.overflow = "";
      // Move element back to its original position
      placeholder.appendChild(mermaidEl);
      setTimeout(function () { overlay.remove(); }, 200);
    }

    closeBtn.addEventListener("click", close);
    overlay.addEventListener("click", function (e) {
      if (e.target === overlay) close();
    });
    document.addEventListener("keydown", function handler(e) {
      if (e.key === "Escape") {
        close();
        document.removeEventListener("keydown", handler);
      }
    });
  }

  function processContainer(el) {
    if (el.dataset.zoomBound) return;
    el.dataset.zoomBound = "1";

    // Create a wrapper that holds the diagram + button
    var wrapper = document.createElement("div");
    wrapper.className = "mermaid-wrapper";
    el.parentNode.insertBefore(wrapper, el);
    wrapper.appendChild(el);

    var btn = document.createElement("button");
    btn.className = "mermaid-zoom-btn";
    btn.setAttribute("aria-label", "Zoom diagram");
    btn.innerHTML = ZOOM_ICON + " Zoom";
    btn.addEventListener("click", function (e) {
      e.preventDefault();
      e.stopPropagation();
      openModal(el, wrapper);
    });
    wrapper.insertBefore(btn, el);
  }

  function scan() {
    // MkDocs Material replaces <pre class="mermaid"> with <div class="mermaid">
    var diagrams = document.querySelectorAll("div.mermaid:not([data-zoom-bound])");
    diagrams.forEach(processContainer);
  }

  // Observe DOM for mermaid containers appearing after async render
  var observer = new MutationObserver(scan);
  observer.observe(document.body, { childList: true, subtree: true });

  // Also run on load
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", scan);
  } else {
    scan();
  }
})();
