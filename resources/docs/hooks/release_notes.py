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
#  Assisted-By: Claude Code (claude-opus-5-5)
#################################################################################

"""MkDocs hook that builds the changelog page from the GitHub releases of the repository.

The release notes live on GitHub Releases, written by the release workflow, so this hook
fetches them at build time and renders ``changelog/index.md``: newest first, one section
per release, with pull request links, ``@user`` mentions and compare links turned into links,
and the notes' own headings turned into bold lines below the release heading. Nothing has to be copied by
hand; the docs workflow rebuilds the site on every release.

Settings (environment variables):

* ``GITHUB_TOKEN``: optional, raises the GitHub API rate limit (set in GitHub Actions).
* ``TX_DOCS_OFFLINE=1``: skip the download, for example when building without network.

If the releases cannot be fetched, the page links to GitHub Releases instead and the build
goes on, so a GitHub outage never breaks the documentation build.
"""

from __future__ import annotations

import json
import logging
import os
import re
import urllib.error
import urllib.request

from mkdocs.structure.files import File, Files

REPO = "eclipse-tractusx/tractusx-edc"
CHANGELOG = "changelog/index.md"
RELEASES_URL = f"https://github.com/{REPO}/releases"
API_URL = f"https://api.github.com/repos/{REPO}/releases?per_page=100&page={{page}}"
MAX_PAGES = 5

log = logging.getLogger("mkdocs.hooks.release_notes")

_PULL = re.compile(rf"(?<![(\[<])https://github\.com/{re.escape(REPO)}/pull/(\d+)")
_COMPARE = re.compile(rf"(?<![(\[<])(https://github\.com/{re.escape(REPO)}/compare/([\w.\-]+\.\.\.[\w.\-]+))")
_MENTION = re.compile(r"(?<![\w/@\[`])@([A-Za-z0-9](?:[A-Za-z0-9-]{0,38}))(?![\w-])")
_HEADING = re.compile(r"^#{1,6}[ \t]+(.+?)[ \t]*#*[ \t]*$", re.M)


def _fetch_releases() -> list[dict]:
    headers = {"Accept": "application/vnd.github+json", "User-Agent": "tractusx-edc-docs"}
    token = os.environ.get("GITHUB_TOKEN")
    if token:
        headers["Authorization"] = f"Bearer {token}"
    releases: list[dict] = []
    for page in range(1, MAX_PAGES + 1):
        request = urllib.request.Request(API_URL.format(page=page), headers=headers)
        with urllib.request.urlopen(request, timeout=20) as response:
            batch = json.load(response)
        releases += batch
        if len(batch) < 100:
            break
    return [r for r in releases if not r.get("draft")]


def _render_body(body: str) -> str:
    body = (body or "").replace("\r\n", "\n").strip()
    if not body:
        return "_No release notes._"
    # The notes' own headings ("What's Changed", "Bugfixes", ...) become bold lines, so that
    # "On this page" lists one entry per release.
    body = _HEADING.sub(lambda m: f"**{m.group(1)}**\n", body)
    body = _PULL.sub(lambda m: f"[#{m.group(1)}]({m.group(0)})", body)
    body = _COMPARE.sub(lambda m: f"[{m.group(2)}]({m.group(1)})", body)
    body = _MENTION.sub(lambda m: f"[@{m.group(1)}](https://github.com/{m.group(1)})", body)
    return body


def _page(releases: list[dict]) -> str:
    lines = [
        "# Changelog",
        "",
        "Every release of Tractus-X EDC with its release notes, newest first. This page is generated from",
        f"[GitHub Releases]({RELEASES_URL}) when the documentation is built. Before you upgrade, read the",
        "[migration guides](../migration/index.md) and the [upgrade procedure](../deployment/upgrades.md).",
        "",
    ]
    if not releases:
        lines += [
            '!!! info "Release notes not available in this build"',
            "",
            f"    The release notes could not be loaded. See them on [GitHub Releases]({RELEASES_URL}).",
            "",
        ]
        return "\n".join(lines)

    releases = sorted(releases, key=lambda r: r.get("published_at") or "", reverse=True)
    lines += ["| Release | Published |", "|---|---|"]
    for release in releases:
        tag = release["tag_name"]
        kind = " (pre-release)" if release.get("prerelease") else ""
        anchor = re.sub(r"[^a-z0-9-]", "", tag.lower().replace(".", ""))
        lines.append(f"| [{tag}](#{anchor}){kind} | {(release.get('published_at') or '')[:10]} |")
    lines.append("")
    for release in releases:
        tag = release["tag_name"]
        anchor = re.sub(r"[^a-z0-9-]", "", tag.lower().replace(".", ""))
        title = release.get("name") or tag
        meta = [f"Published {(release.get('published_at') or '')[:10]}"]
        if release.get("prerelease"):
            meta.append("pre-release")
        meta.append(f"[on GitHub]({release['html_url']})")
        lines += [
            f"## {title} {{ #{anchor} }}",
            "",
            " · ".join(meta),
            "",
            _render_body(release.get("body", "")),
            "",
        ]
    return "\n".join(lines) + "\n"


def on_files(files: Files, config, **_: object) -> Files:
    """Add the generated changelog page to the site."""
    releases: list[dict] = []
    if os.environ.get("TX_DOCS_OFFLINE") != "1":
        try:
            releases = _fetch_releases()
        except (urllib.error.URLError, TimeoutError, ValueError, OSError) as error:
            log.info("Release notes not loaded (%s); the changelog links to GitHub Releases.", error)
    if files.get_file_from_path(CHANGELOG) is None:
        files.append(File.generated(config, CHANGELOG, content=_page(releases)))
    return files
