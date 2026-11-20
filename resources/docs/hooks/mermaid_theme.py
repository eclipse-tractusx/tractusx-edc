#################################################################################
#  Copyright (c) 2026 Contributors to the Eclipse Foundation
#
#  See the NOTICE file(s) distributed with this work for additional
#  information regarding copyright ownership.
#
#  This program and the accompanying materials are made available under the
#  terms of the Apache License, Version 2.0 which is available at
#  https://www.apache.org/licenses/LICENSE-2.0.
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
#  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
#  License for the specific language governing permissions and limitations
#  under the License.
#
#  SPDX-License-Identifier: Apache-2.0
#  Assisted-By: GitHub Copilot (claude-opus-5.5)
#################################################################################

# Taken unchanged from the Tractus-X TestLab documentation
# (https://github.com/eclipse-tractusx/tractusx-testlab/blob/main/tools/mkdocs_mermaid_theme.py).

"""MkDocs hook that makes hand-coloured Mermaid diagrams readable in both schemes.

Diagram authors colour nodes with ``style X fill:#e1f5fe,stroke:#0288d1`` — a
pastel picked against a white page.  Material draws node labels in the scheme's
text colour, so on the dark (slate) scheme that pastel carries near-white text
and the label disappears.  The SVG lives in a closed shadow root, so no site
stylesheet can reach it; the fix has to happen in the diagram source.

Rather than asking every author to remember, the build rewrites each colour
declaration that leaves the text colour to the theme:

* a light fill becomes a translucent tint of its stroke (or of itself), so the
  box keeps its hue on either background and the theme's text stays legible;
* a dark fill gets ``color:#fff``, which the light scheme would otherwise paint
  dark-on-dark;
* a light ``rect rgb(...)`` band in a sequence diagram becomes translucent.

A declaration that already sets ``color:`` chose its own contrast and is kept.
"""

from __future__ import annotations

import re

_FENCE = re.compile(r"^(?P<indent>[ \t]*)(?P<open>`{3,}|~{3,})mermaid[^\n]*\n.*?^(?P=indent)(?P=open)[ \t]*$", re.S | re.M)
_STYLE = re.compile(r"^(?P<head>[ \t]*(?:style|classDef)[ \t]+\S+[ \t]+)(?P<props>[^\n]*?)(?P<tail>;?[ \t]*)$", re.M)
_RECT = re.compile(r"^(?P<head>[ \t]*rect[ \t]+)rgb\([ \t]*(\d+)[ \t]*,[ \t]*(\d+)[ \t]*,[ \t]*(\d+)[ \t]*\)", re.M)
_HEX = re.compile(r"#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})")

TINT_ALPHA = "33"
"""Opacity of a rewritten fill: a visible tint on white and on the slate ground."""

LIGHT_THRESHOLD = 0.5
"""Relative luminance above which a fill is treated as designed for a white page."""


def _rgb(hex_digits: str) -> tuple[int, int, int]:
    if len(hex_digits) == 3:
        hex_digits = "".join(c * 2 for c in hex_digits)
    return int(hex_digits[0:2], 16), int(hex_digits[2:4], 16), int(hex_digits[4:6], 16)


def _luminance(rgb: tuple[int, int, int]) -> float:
    r, g, b = (c / 255 for c in rgb)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def _is_grey(rgb: tuple[int, int, int]) -> bool:
    return max(rgb) - min(rgb) < 24


def _restyle(props: str) -> str:
    pairs = [p.split(":", 1) for p in props.split(",") if ":" in p]
    declared = {key.strip(): value.strip() for key, value in pairs}
    fill = _HEX.fullmatch(declared.get("fill", ""))
    if "color" in declared or fill is None:
        return props

    fill_rgb = _rgb(fill.group(1))
    if _luminance(fill_rgb) < LIGHT_THRESHOLD:
        declared["color"] = "#fff"
    else:
        stroke = _HEX.fullmatch(declared.get("stroke", ""))
        stroke_rgb = _rgb(stroke.group(1)) if stroke else None
        base = stroke_rgb if stroke_rgb and not _is_grey(stroke_rgb) else fill_rgb
        base_hex = "#{:02x}{:02x}{:02x}".format(*base)
        declared["fill"] = base_hex + TINT_ALPHA
        declared.setdefault("stroke", base_hex)
    return ",".join(f"{key}:{value}" for key, value in declared.items())


def _restyle_rect(match: re.Match[str]) -> str:
    rgb = (int(match.group(2)), int(match.group(3)), int(match.group(4)))
    if _luminance(rgb) < LIGHT_THRESHOLD:
        return match.group(0)
    return f"{match.group('head')}rgba({rgb[0]}, {rgb[1]}, {rgb[2]}, 0.2)"


def restyle_diagram(source: str) -> str:
    """Return ``source`` with every theme-blind colour declaration rewritten."""
    source = _STYLE.sub(lambda m: m.group("head") + _restyle(m.group("props")) + m.group("tail"), source)
    return _RECT.sub(_restyle_rect, source)


def on_page_markdown(markdown: str, **_: object) -> str:
    """MkDocs hook: restyle every Mermaid fence on the page."""
    return _FENCE.sub(lambda m: restyle_diagram(m.group(0)), markdown)
