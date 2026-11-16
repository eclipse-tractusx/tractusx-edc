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

"""MkDocs hook that generates the index pages of folders that grow with every release.

``docs/development/decision-records/`` gains a folder per decision and
``docs/migration/`` a file per release. Listing them by hand in ``mkdocs.yml`` or
in an index page would drift, so this hook builds both index pages from the files
at build time, newest first, using each document's first heading as its title.
"""

from __future__ import annotations

import re
from pathlib import Path

from mkdocs.structure.files import File, Files

_H1 = re.compile(r"^#[ \t]+(.+?)[ \t]*#*[ \t]*$", re.M)
_DATE = re.compile(r"^(\d{4})[-_](\d{2})(?:[-_](\d{2}))?")

ADR_INDEX = "development/decision-records/index.md"
MIGRATION_INDEX = "migration/index.md"


def _title(path: Path) -> str:
    match = _H1.search(path.read_text(encoding="utf-8"))
    return match.group(1).strip() if match else path.stem


def _date(name: str) -> str:
    match = _DATE.match(name)
    if not match:
        return ""
    return "-".join(part for part in match.groups() if part)


def _decision_records(docs_dir: Path) -> str:
    rows = []
    for readme in (docs_dir / "development" / "decision-records").glob("*/README.md"):
        folder = readme.parent.name
        rows.append((_date(folder), folder, _title(readme)))
    rows.sort(reverse=True)
    lines = [
        "# Decision records",
        "",
        "Architecture and process decisions of Tractus-X EDC, newest first. Each record explains the",
        "context, the decision and its rationale. To propose a new one, follow the",
        "[contribution workflow](../../developer/contributing.md#decision-records).",
        "",
        "| Date | Decision |",
        "|---|---|",
    ]
    lines += [f"| {date} | [{title}]({folder}/README.md) |" for date, folder, title in rows]
    return "\n".join(lines) + "\n"


def _migration_guides(docs_dir: Path) -> str:
    rows = []
    for guide in (docs_dir / "migration").glob("*.md"):
        if guide.name in ("index.md", "README.md"):
            continue
        rows.append((_date(guide.name), guide.name, _title(guide)))
    rows.sort(reverse=True)
    lines = [
        "# Migration guides",
        "",
        "Every release that changes configuration, APIs, database schemas or behaviour that operators",
        "depend on ships with a migration guide. Read **every** guide between your current version and",
        "the target version, oldest first. The [upgrade guide](../deployment/upgrades.md) explains the",
        "procedure.",
        "",
        "| Published | Guide |",
        "|---|---|",
    ]
    lines += [f"| {date} | [{title}]({name.replace(' ', '%20')}) |" for date, name, title in rows]
    return "\n".join(lines) + "\n"


def on_files(files: Files, config, **_: object) -> Files:
    """Add the generated index pages to the site."""
    docs_dir = Path(config["docs_dir"])
    for uri, content in ((ADR_INDEX, _decision_records(docs_dir)), (MIGRATION_INDEX, _migration_guides(docs_dir))):
        if files.get_file_from_path(uri) is None:
            files.append(File.generated(config, uri, content=content))
    return files
