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
#  Assisted-By: Claude Code (claude-opus-5-5)
#################################################################################

"""MkDocs hook that publishes the Connector Sensei skills.

The skills live in ``.claude/skills/<name>/SKILL.md``, outside ``docs/``, because
Claude Code and GitHub Copilot load them from there. This hook turns them into pages
under ``development/connector-sensei/skills/``: an overview with every skill, its
purpose and its scripts, and one page per skill with its ``SKILL.md``. It also lists
them in the nav under "AI-assisted development", so a new skill shows up on the site
without editing ``mkdocs.yml``.

Skills appear in the order of the table in ``.claude/skills/README.md``; skills that
the table does not list follow alphabetically. Relative links in a ``SKILL.md`` are
rewritten for the page's new location: to the page when the target is published in
``docs/``, otherwise to the file on GitHub.
"""

from __future__ import annotations

import posixpath
import re
from pathlib import Path

from mkdocs.structure.files import File, Files

SKILLS_DIR = ".claude/skills"
PAGES_DIR = "development/connector-sensei/skills"
NAV_SECTION = "AI-assisted development"
SENSEI_PAGE = "development/connector-sensei/README.md"
_SKILLS_BLOCK = re.compile(r"<!-- skills:start.*?-->.*?<!-- skills:end -->", re.S)
# Icons for the cards on the Connector Sensei page; other skills get the default.
ICONS = {
    "edc-upstream-sources": "material-source-branch",
    "upstream-edc-bump": "material-arrow-up-bold-circle-outline",
    "setting-lifecycle": "material-tune-variant",
    "breaking-change-guard": "material-shield-alert-outline",
    "new-extension": "material-puzzle-plus-outline",
    "sql-migration": "material-database-arrow-up-outline",
    "e2e-test-author": "material-test-tube",
    "management-api-endpoint": "material-api",
    "migration-guide-and-adr": "material-map-marker-path",
    "dependencies-and-dash": "material-scale-balance",
}
DEFAULT_ICON = "material-puzzle-outline"
NAV_TITLE = "Skills"

_FRONT_MATTER = re.compile(r"\A---\n(?P<yaml>.*?)\n---\n", re.S)
_LEADING_COMMENT = re.compile(r"\A\s*<!--.*?-->\s*", re.S)
_FIELD = re.compile(r"^(?P<key>[a-z_-]+):\s*(?P<value>.*)$", re.M)
_HEADING = re.compile(r"^#[ \t]+(?P<title>.+)$", re.M)
_MD_LINK = re.compile(r"(?P<bang>!?)\[(?P<text>[^\]\n]*)\]\((?P<target>[^)\s]+)\)")
_FENCE = re.compile(r"^([ \t]*)(`{3,}|~{3,})[^\n]*\n.*?^\1\2[ \t]*$", re.S | re.M)
_SCHEME = re.compile(r"^[a-zA-Z][a-zA-Z0-9+.-]*:")


def _skills(repo_root: Path) -> list[dict]:
    root = repo_root / SKILLS_DIR
    if not root.is_dir():
        return []
    skills = []
    for skill_md in sorted(root.glob("*/SKILL.md")):
        text = skill_md.read_text(encoding="utf-8")
        match = _FRONT_MATTER.match(text)
        fields = {m["key"]: m["value"].strip() for m in _FIELD.finditer(match["yaml"])} if match else {}
        body = _LEADING_COMMENT.sub("", text[match.end():] if match else text, count=1)
        heading = _HEADING.search(body)
        scripts = sorted(p.name for p in (skill_md.parent / "scripts").glob("*") if p.is_file())
        skills.append({
            "name": fields.get("name", skill_md.parent.name),
            "folder": skill_md.parent.name,
            "title": heading["title"].strip() if heading else skill_md.parent.name,
            "description": fields.get("description", ""),
            "body": body,
            "scripts": scripts,
        })
    order = re.findall(r"\]\(([\w-]+)/SKILL\.md\)", (root / "README.md").read_text(encoding="utf-8")) \
        if (root / "README.md").is_file() else []
    rank = {name: i for i, name in enumerate(order)}
    return sorted(skills, key=lambda s: (rank.get(s["folder"], len(rank)), s["folder"]))


def _first_sentence(text: str) -> str:
    match = re.match(r"(.+?\.)(\s|$)", text)
    return match.group(1) if match else text


class _Links:
    def __init__(self, repo_root: Path, docs_dir: Path, skill_folder: str, repo_url: str, branch: str) -> None:
        self.repo_root = repo_root
        self.docs_dir = docs_dir
        self.source_dir = repo_root / SKILLS_DIR / skill_folder
        self.page_dir = docs_dir / PAGES_DIR
        self.repo_url = repo_url.rstrip("/")
        self.branch = branch

    def __call__(self, match: re.Match) -> str:
        target = match["target"]
        if target.startswith(("#", "/")) or _SCHEME.match(target):
            return match.group(0)
        path, sep, fragment = target.partition("#")
        resolved = Path(posixpath.normpath((self.source_dir / path).as_posix()))
        try:
            in_docs = resolved.relative_to(self.docs_dir)
        except ValueError:
            in_docs = None
        if in_docs is not None and resolved.suffix == ".md" and resolved.is_file():
            new = posixpath.relpath(resolved.as_posix(), self.page_dir.as_posix())
        else:
            try:
                rel = resolved.relative_to(self.repo_root).as_posix()
            except ValueError:
                return match.group(0)
            kind = "raw" if match["bang"] else ("tree" if resolved.is_dir() else "blob")
            new = f"{self.repo_url}/{kind}/{self.branch}/{rel}"
        return f"{match['bang']}[{match['text']}]({new}{sep + fragment if sep else ''})"


def _rewrite(body: str, links: _Links) -> str:
    out, last = [], 0
    for fence in _FENCE.finditer(body):
        out.append(_MD_LINK.sub(links, body[last:fence.start()]))
        out.append(fence.group(0))
        last = fence.end()
    out.append(_MD_LINK.sub(links, body[last:]))
    return "".join(out)


def _repo_root(config) -> Path:
    return Path(config["config_file_path"]).resolve().parent


def on_config(config, **_: object):
    """List the skills in the nav, under "AI-assisted development"."""
    skills = _skills(_repo_root(config))

    def visit(items) -> bool:
        for item in items or []:
            if isinstance(item, dict):
                for title, children in item.items():
                    if title == NAV_SECTION and isinstance(children, list):
                        if any(isinstance(c, dict) and NAV_TITLE in c for c in children):
                            return True
                        children.append({NAV_TITLE: [f"{PAGES_DIR}/index.md"]
                                         + [{s["title"]: f"{PAGES_DIR}/{s['folder']}.md"} for s in skills]})
                        return True
                    if isinstance(children, list) and visit(children):
                        return True
        return False

    if skills:
        visit(config["nav"])
    return config


def on_page_markdown(markdown: str, page, config, **_: object) -> str:
    """On the Connector Sensei page, replace the skills placeholder with a card per skill."""
    if page.file.src_uri != SENSEI_PAGE or not _SKILLS_BLOCK.search(markdown):
        return markdown
    skills = _skills(_repo_root(config))
    if not skills:
        return markdown
    cards = ['<div class="grid cards" markdown>', ""]
    for skill in skills:
        scripts = " ".join(f"`{s}`" for s in skill["scripts"])
        cards += [
            f"-   :{ICONS.get(skill['folder'], DEFAULT_ICON)}: **{skill['title']}**",
            "",
            "    ---",
            "",
            f"    {_first_sentence(skill['description'])}",
            "",
            *([f"    {scripts}", ""] if scripts else []),
            f"    [:octicons-arrow-right-24: `{skill['name']}`](skills/{skill['folder']}.md)",
            "",
        ]
    cards += ["</div>", "", "Each card opens the skill's procedure. [Skills](skills/index.md) lists them in one table."]
    intro = (f"The Sensei has {len(skills)} skills. It loads the one whose description matches your request, runs "
             "its script and follows its procedure; you can run the scripts yourself too.")
    block = "\n".join([intro, "", *cards])
    return _SKILLS_BLOCK.sub(lambda _: block, markdown, count=1)


def on_files(files: Files, config, **_: object) -> Files:
    """Add the overview and one page per skill."""
    repo_root = _repo_root(config)
    docs_dir = Path(config["docs_dir"]).resolve()
    repo_url = config["repo_url"]
    branch = (config.get("extra") or {}).get("repo_branch", "main")
    skills = _skills(repo_root)
    if not skills:
        return files

    rows = []
    for skill in skills:
        scripts = ", ".join(f"`{s}`" for s in skill["scripts"]) or "none"
        rows.append(f"| [{skill['name']}]({skill['folder']}.md) | {_first_sentence(skill['description'])} | {scripts} |")
    overview = "\n".join([
        "# Skills",
        "",
        "The skills are the procedures and scripts the [Connector Sensei](../README.md) works with. Each one is a folder",
        f"in [`{SKILLS_DIR}/`]({repo_url}/tree/{branch}/{SKILLS_DIR}) with a `SKILL.md`, which Claude Code and GitHub",
        "Copilot load when a request matches its description, and usually a `scripts/` folder with a tested, read-only",
        "script you can also run yourself. [How the skills work](../skills.md) explains the idea.",
        "",
        "This page and the pages of the skills are generated from the `SKILL.md` files when the site is built.",
        "",
        "| Skill | Use it for | Scripts |",
        "|---|---|---|",
        *rows,
        "",
        f"Planned skills are described in the [skills catalog]({repo_url}/blob/{branch}/{SKILLS_DIR}/README.md).",
        "",
    ])
    files.append(File.generated(config, f"{PAGES_DIR}/index.md", content=overview))

    for skill in skills:
        source = f"{repo_url}/blob/{branch}/{SKILLS_DIR}/{skill['folder']}/SKILL.md"
        scripts = ", ".join(
            f"[`{s}`]({repo_url}/blob/{branch}/{SKILLS_DIR}/{skill['folder']}/scripts/{s})" for s in skill["scripts"]
        ) or "none"
        intro = "\n".join([
            '!!! info "Connector Sensei skill"',
            "",
            f"    **When it is used:** {skill['description']}",
            "",
            f"    **Scripts:** {scripts} &middot; **Source:** [`{SKILLS_DIR}/{skill['folder']}/SKILL.md`]({source})",
            "",
        ])
        body = _rewrite(skill["body"], _Links(repo_root, docs_dir, skill["folder"], repo_url, branch))
        heading = _HEADING.search(body)
        if heading:
            body = body[:heading.end()] + "\n\n" + intro + body[heading.end():]
        else:
            body = f"# {skill['title']}\n\n{intro}\n{body}"
        files.append(File.generated(config, f"{PAGES_DIR}/{skill['folder']}.md", content=body))
    return files
