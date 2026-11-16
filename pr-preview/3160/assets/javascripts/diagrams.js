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
 * Assisted-By: Claude Code (claude-opus-5-5)
 ********************************************************************************/

/**
 * Zoom and pan for diagrams: Mermaid diagrams and large images (draw.io exports).
 *
 * Each diagram gets a toolbar with zoom out, the zoom level (tap to fit again), zoom
 * in and full screen. A zoomed diagram scrolls inside its frame: swipe on a phone,
 * scroll or drag with the mouse on a desktop. Ctrl or Cmd with the mouse wheel zooms
 * too, and Escape leaves full screen.
 *
 * Material draws a Mermaid diagram inside a closed shadow root, so the SVG cannot be
 * resized from outside; the CSS zoom property scales the element that hosts it,
 * which keeps the drawing sharp and makes the frame scroll. Started from the zoom
 * button of the Tractus-X TestLab documentation (mermaid-zoom.js).
 */
(function () {
  "use strict";

  var STEPS = [0.5, 0.75, 1, 1.25, 1.5, 2, 2.5, 3, 4];
  var MIN_IMAGE_WIDTH = 600;
  var ICONS = {
    out: '<path d="M19 13H5v-2h14z"/>',
    in: '<path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6z"/>',
    full: '<path d="M7 14H5v5h5v-2H7zm-2-4h2V7h3V5H5zm12 7h-3v2h5v-5h-2zM14 5v2h3v3h2V5z"/>',
    exit: '<path d="M5 16h3v3h2v-5H5zm3-8H5v2h5V5H8zm6 11h2v-3h3v-2h-5zm2-11V5h-2v5h5V8z"/>',
  };

  function icon(name) {
    return '<svg viewBox="0 0 24 24" aria-hidden="true">' + ICONS[name] + "</svg>";
  }

  function button(cls, label, html) {
    var b = document.createElement("button");
    b.type = "button";
    b.className = "tx-diagram__button " + cls;
    b.title = label;
    b.setAttribute("aria-label", label);
    b.innerHTML = html;
    return b;
  }

  function setUp(content) {
    if (content.closest(".tx-diagram")) {
      return;
    }
    var figure = document.createElement("div");
    figure.className = "tx-diagram";
    var bar = document.createElement("div");
    bar.className = "tx-diagram__bar";
    var viewport = document.createElement("div");
    viewport.className = "tx-diagram__viewport";
    content.parentNode.insertBefore(figure, content);
    figure.appendChild(bar);
    figure.appendChild(viewport);
    viewport.appendChild(content);

    var zoomOut = button("tx-diagram__out", "Zoom out", icon("out"));
    var level = button("tx-diagram__level", "Fit to the frame", "100%");
    var zoomIn = button("tx-diagram__in", "Zoom in", icon("in"));
    var full = button("tx-diagram__full", "Full screen", icon("full"));
    [zoomOut, level, zoomIn, full].forEach(function (b) { bar.appendChild(b); });

    var scale = 1;
    var base = 0;

    function apply(next, anchorX, anchorY) {
      next = Math.min(STEPS[STEPS.length - 1], Math.max(STEPS[0], next));
      // Keep the point under the anchor (the centre, or the mouse) in place.
      var ax = anchorX === undefined ? viewport.clientWidth / 2 : anchorX;
      var ay = anchorY === undefined ? viewport.clientHeight / 2 : anchorY;
      var x = (viewport.scrollLeft + ax) / scale;
      var y = (viewport.scrollTop + ay) / scale;
      if (next === 1) {
        content.style.zoom = "";
        content.style.width = "";
        content.style.maxWidth = "";
        base = 0;
      } else {
        if (!base) {
          base = content.getBoundingClientRect().width / scale;
        }
        content.style.maxWidth = "none";
        content.style.width = base + "px";
        content.style.zoom = String(next);
      }
      scale = next;
      viewport.scrollLeft = x * scale - ax;
      viewport.scrollTop = y * scale - ay;
      level.textContent = Math.round(scale * 100) + "%";
      zoomOut.disabled = scale <= STEPS[0];
      zoomIn.disabled = scale >= STEPS[STEPS.length - 1];
      figure.classList.toggle("tx-diagram--zoomed", scale > 1);
    }

    function step(direction, anchorX, anchorY) {
      var next = direction > 0
        ? STEPS.find(function (s) { return s > scale + 0.001; })
        : STEPS.slice().reverse().find(function (s) { return s < scale - 0.001; });
      if (next) {
        apply(next, anchorX, anchorY);
      }
    }

    function fit() {
      apply(1);
      viewport.scrollLeft = 0;
      viewport.scrollTop = 0;
    }

    function onKey(event) {
      if (event.key === "Escape") {
        toggleFull(false);
      }
    }

    function toggleFull(on) {
      var isFull = figure.classList.contains("tx-diagram--full");
      if (on === isFull) {
        return;
      }
      figure.classList.toggle("tx-diagram--full", on);
      document.documentElement.classList.toggle("tx-diagram-open", on);
      full.innerHTML = icon(on ? "exit" : "full");
      full.title = on ? "Leave full screen" : "Full screen";
      full.setAttribute("aria-label", full.title);
      fit();
      if (on) {
        document.addEventListener("keydown", onKey);
      } else {
        document.removeEventListener("keydown", onKey);
        full.focus();
      }
    }

    zoomOut.addEventListener("click", function () { step(-1); });
    zoomIn.addEventListener("click", function () { step(1); });
    level.addEventListener("click", fit);
    full.addEventListener("click", function () {
      toggleFull(!figure.classList.contains("tx-diagram--full"));
    });

    viewport.addEventListener("wheel", function (event) {
      if (!event.ctrlKey && !event.metaKey) {
        return;
      }
      event.preventDefault();
      var rect = viewport.getBoundingClientRect();
      step(event.deltaY < 0 ? 1 : -1, event.clientX - rect.left, event.clientY - rect.top);
    }, { passive: false });

    // Drag to move around with the mouse; touch screens scroll the frame natively.
    var drag = null;
    viewport.addEventListener("pointerdown", function (event) {
      var scrollable = viewport.scrollWidth > viewport.clientWidth || viewport.scrollHeight > viewport.clientHeight;
      if (event.pointerType !== "mouse" || event.button !== 0 || !scrollable) {
        return;
      }
      drag = { x: event.clientX, y: event.clientY, left: viewport.scrollLeft, top: viewport.scrollTop };
      viewport.setPointerCapture(event.pointerId);
      viewport.classList.add("tx-diagram__viewport--dragging");
      event.preventDefault();
    });
    viewport.addEventListener("pointermove", function (event) {
      if (drag) {
        viewport.scrollLeft = drag.left - (event.clientX - drag.x);
        viewport.scrollTop = drag.top - (event.clientY - drag.y);
      }
    });
    function endDrag() {
      drag = null;
      viewport.classList.remove("tx-diagram__viewport--dragging");
    }
    viewport.addEventListener("pointerup", endDrag);
    viewport.addEventListener("pointercancel", endDrag);
  }

  function isDiagramImage(img) {
    return img.naturalWidth >= MIN_IMAGE_WIDTH &&
      !img.closest("a, .grid, .tx-chips, .md-header, .md-footer") &&
      !img.classList.contains("tx-sensei-picture") &&
      !img.classList.contains("twemoji") &&
      !img.hasAttribute("width");
  }

  function scan() {
    document.querySelectorAll(".md-typeset div.mermaid").forEach(setUp);
    document.querySelectorAll(".md-content .md-typeset img").forEach(function (img) {
      if (img.complete) {
        if (isDiagramImage(img)) {
          setUp(img);
        }
      } else if (!img.dataset.txDiagramWait) {
        img.dataset.txDiagramWait = "1";
        img.addEventListener("load", function () {
          if (isDiagramImage(img)) {
            setUp(img);
          }
        }, { once: true });
      }
    });
  }

  // Material renders Mermaid after the page is shown, and again after each instant navigation.
  new MutationObserver(scan).observe(document.body, { childList: true, subtree: true });
  if (typeof document$ !== "undefined") {
    document$.subscribe(function () {
      document.documentElement.classList.remove("tx-diagram-open");
      scan();
    });
  }
  scan();
})();
