#################################################################################
#  Copyright (c) 2026 Catena-X Automotive Network e.V.
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

"""MkDocs hook that lets the repository's Markdown render unchanged on the site.

The Markdown under ``docs/`` is written to be read on GitHub, so it links freely to
source files, READMEs of extensions and Helm charts, and folders. Those targets are
not part of the site. This hook rewrites, before rendering:

* a relative link or image whose target lies outside ``docs/`` into a link to the
  file on GitHub (``blob``), the folder (``tree``) or the raw image;
* a link to a file inside ``docs/`` that is excluded from the site likewise;
* a link to a folder inside ``docs/`` into its ``README.md``/``index.md`` page when
  there is one, otherwise into the folder on GitHub;
* the trailing ``## NOTICE`` section (licence and AI disclosure that every page
  carries for the repository) is removed, because the site footer states the licence.

Code blocks are left untouched.
"""

from __future__ import annotations

import posixpath
import re
from pathlib import Path

_FENCE = re.compile(r"^([ \t]*)(`{3,}|~{3,})[^\n]*\n.*?^\1\2[ \t]*$", re.S | re.M)
_INLINE_CODE = re.compile(r"`[^`\n]+`")
_MD_LINK = re.compile(r"(?P<bang>!?)\[(?P<text>[^\]\n]*)\]\((?P<target><[^>\n]+>|[^)\s]+)(?P<title>\s+\"[^\"]*\")?\)")
_HTML_ATTR = re.compile(r"(?P<attr>\b(?:src|href))=\"(?P<target>[^\"]+)\"")
_NOTICE = re.compile(r"\n#{2}[ \t]+NOTICE[ \t]*\n.*\Z", re.S)
_SCHEME = re.compile(r"^[a-zA-Z][a-zA-Z0-9+.-]*:")
_IMAGE_SUFFIXES = {".png", ".jpg", ".jpeg", ".gif", ".svg", ".webp"}


class _Rewriter:
    def __init__(self, page_src: str, config, files) -> None:
        self.files = files
        self.docs_dir = Path(config["docs_dir"]).resolve()
        self.repo_root = Path(config["config_file_path"]).resolve().parent
        self.page_dir = (self.docs_dir / page_src).parent
        self.repo_url = config["repo_url"].rstrip("/")
        self.branch = (config.get("extra") or {}).get("repo_branch", "main")

    def rewrite(self, target: str, is_image: bool) -> str:
        bare = target[1:-1] if target.startswith("<") else target
        if not bare or bare.startswith(("#", "/")) or _SCHEME.match(bare):
            return target
        path_part, sep, fragment = bare.partition("#")
        path_part = path_part.split("?", 1)[0]
        if not path_part:
            return target
        resolved = Path(posixpath.normpath((self.page_dir / path_part).as_posix()))

        if self._inside(resolved, self.docs_dir):
            if resolved.is_dir():
                for index in ("index.md", "README.md"):
                    if (resolved / index).is_file():
                        rel = posixpath.relpath((resolved / index).as_posix(), self.page_dir.as_posix())
                        return rel + (sep + fragment if sep else "")
                return self._github(resolved, fragment, is_image, tree=True)
            if not self._published(resolved):
                return self._github(resolved, fragment, is_image, tree=False)
            rel = posixpath.relpath(resolved.as_posix(), self.page_dir.as_posix())
            return rel.replace(" ", "%20") + (sep + fragment if sep else "")

        if not self._inside(resolved, self.repo_root):
            return target
        return self._github(resolved, fragment, is_image, tree=resolved.is_dir())

    def _github(self, resolved: Path, fragment: str, is_image: bool, tree: bool) -> str:
        rel = resolved.relative_to(self.repo_root).as_posix()
        if is_image or resolved.suffix.lower() in _IMAGE_SUFFIXES:
            return f"{self.repo_url}/raw/{self.branch}/{rel}"
        kind = "tree" if tree else "blob"
        return f"{self.repo_url}/{kind}/{self.branch}/{rel}" + (f"#{fragment}" if fragment else "")

    def _published(self, resolved: Path) -> bool:
        file = self.files.get_file_from_path(resolved.relative_to(self.docs_dir).as_posix())
        return file is not None and not file.inclusion.is_excluded()

    @staticmethod
    def _inside(path: Path, root: Path) -> bool:
        try:
            path.relative_to(root)
            return True
        except ValueError:
            return False


def _rewrite_prose(text: str, rewriter: _Rewriter) -> str:
    def md(match: re.Match[str]) -> str:
        new = rewriter.rewrite(match.group("target"), bool(match.group("bang")))
        return f"{match.group('bang')}[{match.group('text')}]({new}{match.group('title') or ''})"

    def html(match: re.Match[str]) -> str:
        new = rewriter.rewrite(match.group("target"), match.group("attr") == "src")
        return f"{match.group('attr')}=\"{new}\""

    # Protect inline code spans so `[x](y)` examples are not rewritten.
    spans: list[str] = []

    def stash(match: re.Match[str]) -> str:
        spans.append(match.group(0))
        return f"\x00{len(spans) - 1}\x00"

    text = _INLINE_CODE.sub(stash, text)
    text = _MD_LINK.sub(md, text)
    text = _HTML_ATTR.sub(html, text)
    return re.sub(r"\x00(\d+)\x00", lambda m: spans[int(m.group(1))], text)


def on_page_markdown(markdown: str, page, config, files, **_: object) -> str:
    """Rewrite links that leave the site and drop the repository NOTICE footer."""
    markdown = _NOTICE.sub("\n", markdown)
    rewriter = _Rewriter(page.file.src_uri, config, files)
    out, last = [], 0
    for fence in _FENCE.finditer(markdown):
        out.append(_rewrite_prose(markdown[last:fence.start()], rewriter))
        out.append(fence.group(0))
        last = fence.end()
    out.append(_rewrite_prose(markdown[last:], rewriter))
    return "".join(out)
