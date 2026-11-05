# Connector Sensei

<img src="../../../.github/agents/connector-sensei.jpg" width="96" align="right" alt="Connector Sensei avatar">

The **Connector Sensei** is an AI agent for developing and operating Tractus-X EDC. It knows this repository, the
upstream Eclipse Dataspace Components (EDC) version it is built on, and the specifications behind both: DSP, DCP,
ODRL/JSON-LD and the Catena-X standards. It answers from **evidence it opens in the session**, such as source code,
configuration, docs and specs, and cites `file:line`. It does not answer from memory.

It works with **Claude Code** and **GitHub Copilot**. Its procedures and deterministic helper scripts, the *skills*, are
shared by both. [How the skills work](skills.md) explains the idea behind them.

<!-- TOC -->
- [What is in the repository](#what-is-in-the-repository)
- [Prerequisites](#prerequisites)
- [Using it with Claude Code](#using-it-with-claude-code)
- [Using it with GitHub Copilot](#using-it-with-github-copilot)
- [What to ask: example requests](#what-to-ask-example-requests)
- [Using the scripts without an AI](#using-the-scripts-without-an-ai)
- [What the Sensei does and does not do](#what-the-sensei-does-and-does-not-do)
- [Maintaining the Sensei](#maintaining-the-sensei)
- [Troubleshooting](#troubleshooting)
<!-- TOC -->

## What is in the repository

| Path | Purpose |
|---|---|
| [`.claude/agents/connector-sensei.md`](../../../.claude/agents/connector-sensei.md) | The agent for Claude Code (subagent definition: persona, knowledge map, ground rules, playbooks) |
| [`.github/agents/connector-sensei.agent.md`](../../../.github/agents/connector-sensei.agent.md) | The same agent for GitHub Copilot (custom agent), with the avatar as `icon` |
| [`.github/copilot-instructions.md`](../../../.github/copilot-instructions.md) | Repository-wide Copilot instructions: Copilot adopts the Sensei identity and its rules even when the agent is not selected |
| [`.claude/skills/`](../../../.claude/skills/README.md) | The skills: one folder per task with `SKILL.md` (procedure) and `scripts/` (deterministic tooling). Loaded by Claude Code and Copilot. |
| [`.github/agents/REUSE.toml`](../../../.github/agents/REUSE.toml) | License information for the avatar images (CC-BY-4.0) |
| [`.github/agents/connector-sensei-profile.jpg`](../../../.github/agents/connector-sensei-profile.jpg) | Close-up profile avatar (256px) used in the GitHub comment header |
| [`comment-template.md`](comment-template.md) | The template the Sensei uses on GitHub (issues, comments, PR descriptions, reviews): avatar header, AI disclosure, evidence section, sign-off |

## Prerequisites

The agent itself needs only Claude Code or GitHub Copilot with this repository open. The skill scripts use standard
tools:

| Tool | Needed for |
|---|---|
| `git`, `bash` (3.2+ is enough, i.e. stock macOS), `awk`, `sed`, `unzip`, `curl`, `perl` | All scripts |
| JDK 17+ and the Gradle wrapper | Builds and tests, `dash-check.sh`, `scaffold-extension.sh` verification |
| Docker | Running E2E and `PostgresqlIntegrationTest` suites (Testcontainers) |
| Network access to Maven Central, `repo.eclipse.org` and `eclipse-tractusx.github.io` | Downloading upstream sources the first time, Eclipse Dash, the published DEPENDENCIES |

Upstream EDC sources are cached in `~/.cache/edc-sources`, outside the repository. The first lookup for a version takes
up to a minute; later ones are instant.

## Using it with Claude Code

1. Open a Claude Code session in the repository root (or a worktree of it). The agent and the skills are picked up from
   `.claude/` automatically. Start a new session after pulling changes to them.
2. Ask for the Sensei by name, or run `/agents` to see it:

   ```text
   Ask the connector-sensei: why does my contract negotiation end in TERMINATED with a BPN group policy?
   ```

   Claude also delegates to the Sensei on its own when a request matches its description (Tractus-X EDC, EDC, DSP, DCP,
   policies, Helm/config, upgrades, debugging, extensions).
3. Skills are loaded when a task matches their description. You can also name one explicitly:

   ```text
   Use the breaking-change-guard skill on my branch and tell me what still needs a migration-guide entry.
   ```

### Start a chat from the command line

Install Claude Code once (`npm install -g @anthropic-ai/claude-code`) and run `claude` once to sign in. Then, from the
repository root or a worktree of it:

```bash
# Chat with the Sensei: the whole session runs as the agent
claude --agent connector-sensei

# The same, with your first question already asked
claude --agent connector-sensei "Why does my contract negotiation end in TERMINATED with a BPN group policy?"

# One answer printed to the terminal, then exit (quick lookups, scripts)
claude --agent connector-sensei -p "Which setting configures the BDRS server URL?"

# Continue your last chat in this directory
claude --agent connector-sensei --continue
```

Plain `claude` works too: Claude then hands questions to the Sensei when they match its description (step 2 above).
With `--agent`, the Sensei answers every message itself, with its persona and rules from the first line. To make it
your default in this repository, put `{"agent": "connector-sensei"}` in `.claude/settings.local.json` (personal settings, keep
them out of commits).

## Using it with GitHub Copilot

- **VS Code (Copilot Chat, agent mode):** open the repository, open the chat, and select **Connector Sensei** in the
  agent picker. Reload the window if it does not appear yet. Copilot loads the skills from `.claude/skills/` in agent
  mode.
- **Copilot on github.com (coding agent):** custom agents in `.github/agents/` can be selected when you hand a task or
  issue to Copilot. The repository-wide `.github/copilot-instructions.md` also applies to Copilot code review and PR
  descriptions.
- **Without selecting the agent:** because of `.github/copilot-instructions.md`, Copilot still speaks as the Sensei and
  follows its ground rules (verify before asserting, cite sources, run `bc-scan.sh` before writing a PR description).

## What to ask: example requests

| Goal | Example request | Skill(s) the Sensei uses |
|---|---|---|
| Understand a concept | "Explain how the EDR refresh works between consumer and provider, with the classes involved." | `edc-upstream-sources` |
| Find a setting | "What is the env var and Helm value for the BDRS URL, and where is it read?" | `setting-lifecycle` |
| Add or rename a setting | "Rename `tx.edc.foo.bar` to `tx.edc.foo.baz` with a deprecation period." | `setting-lifecycle`, `breaking-change-guard` |
| Upgrade upstream EDC | "Plan the upgrade from EDC 0.17.0 to 0.18.0: what breaks, which migrations do we need?" | `upstream-edc-bump`, `edc-upstream-sources`, `sql-migration` |
| Adopt an upstream change | "Upstream deprecated `EndpointDataReferenceEntry` in 0.17.0. Where do we use it and how should we migrate?" | `edc-upstream-sources` |
| Review a PR | "Review PR #1234 for breaking changes and missing follow-ups." | `breaking-change-guard` |
| Prepare a release | "Draft the 0.13 → 0.14 migration guide." | `migration-guide-and-adr`, `setting-lifecycle` |
| Record a decision | "Create a decision record for caching DID documents and fill the rationale from our discussion." | `migration-guide-and-adr` |
| New feature module | "Where should a Factory-X-agnostic notification extension live? Scaffold it." | `new-extension` |
| Schema change | "Add an index on `edc_transfer_process.state` and tell me what to test." | `sql-migration` |
| Prove a feature end to end | "Write an E2E test that a BPN group policy hides the offer from other consumers." | `e2e-test-author` |
| New endpoint | "Add `GET /v3/edrs/{id}/status`, following our API conventions." | `management-api-endpoint` |
| Dependency hygiene | "Does bumping `com.fasterxml.jackson.core:jackson-databind` introduce restricted licenses?" | `dependencies-and-dash` |

Good requests name the **side** (consumer or provider), the **version** (deployed release, DSP version) and paste
**evidence** (log excerpt, request payload, error). The Sensei asks for them when they matter.

## Using the scripts without an AI

Every script is a plain shell script. Run them yourself, in CI, or in a pre-push hook. All print `--help`.

```bash
S=.claude/skills
$S/edc-upstream-sources/scripts/edc-src.sh read DcpIdentityService        # upstream class at the pinned version
$S/upstream-edc-bump/scripts/bump-impact.sh 0.17.0 0.18.0                  # impact report of an EDC bump
$S/setting-lifecycle/scripts/setting-trace.sh TX_EDC_IAM_DCP_BDRS_SERVER_URL
$S/breaking-change-guard/scripts/bc-scan.sh --strict                       # exit 1 on BLOCKER/BREAKING
$S/new-extension/scripts/scaffold-extension.sh edc-extensions/cx-foo org.eclipse.tractusx.edc.foo CxFooExtension
$S/sql-migration/scripts/migrations.sh status
$S/e2e-test-author/scripts/e2e.sh list
$S/management-api-endpoint/scripts/api-inventory.sh
$S/migration-guide-and-adr/scripts/draft-migration-guide.sh --branch HEAD
$S/dependencies-and-dash/scripts/dash-check.sh --list-only
```

The full catalog, including planned skills, is in [`.claude/skills/README.md`](../../../.claude/skills/README.md).

## What the Sensei does and does not do

**Does:**
- Opens the code, the upstream sources at the pinned version, specs and standards before it answers, and cites them.
- Says whether behaviour comes from Tractus-X, upstream EDC, a specification or a Catena-X standard.
- Runs the skill scripts to collect facts, then gives a recommendation with the concrete next step: a payload, config,
  command, patch or test.
- Speaks in first person, in the voice of a calm dojo master, while code and config stay exact.

**Does not:**
- Change files unless you ask it to implement something. By default it is an advisor.
- Push, open PRs or post comments on its own. You stay in control of everything outward-facing.
- Replace review. Its output is a proposal. AI-generated contributions carry the AI disclosure header (see the skill
  files) and must be reviewed and verified by the human contributor who submits them.

## Maintaining the Sensei

- **Keep both agent files in sync.** `.claude/agents/connector-sensei.md` and `.github/agents/connector-sensei.agent.md`
  share the same body; only the front matter differs.
- **Add a skill** by following [How the skills work → Writing a new skill](skills.md#writing-a-new-skill). Then list it
  in `.claude/skills/README.md`, in the skill table of both agent files, and in `.github/copilot-instructions.md`.
- **License headers:** AI-generated files carry the Apache-2.0 header with the AI disclosure block (AI-generated portions
  under CC0-1.0, `SPDX-License-Identifier: Apache-2.0 AND CC0-1.0`, `Assisted-By:`). Images are covered by
  `.github/agents/REUSE.toml`.
- **Posting on GitHub as the Sensei:** use [`comment-template.md`](comment-template.md). It keeps the avatar, the AI
  disclosure and the evidence section consistent across issues, PRs and reviews.
- **After an upstream EDC bump**, re-run the skill scripts once against the new version. They read the version from
  `gradle/libs.versions.toml`, so they normally need no change.

## Troubleshooting

| Symptom | Fix |
|---|---|
| The Sensei or a skill does not show up | Start a new Claude Code session, or reload the VS Code window, after pulling. Check that the files are on your branch. |
| `edc-src: no org.eclipse.edc artifacts in the Gradle cache` | Run `./gradlew compileJava` once so Gradle resolves the upstream artifacts. |
| First upstream lookup is slow | Sources are downloaded once per version into `~/.cache/edc-sources`. Set `EDC_SRC_OFFLINE=1` to forbid downloads. |
| `e2e: suite '…' needs Docker` | Start Docker (Testcontainers). `e2e.sh compile <suite>` works without it. |
| `dash-check.sh` cannot reach the internet | Use `--list-only` for the dependency diff; the Dash run needs `repo.eclipse.org` and the Eclipse/ClearlyDefined APIs. |
| An answer cites nothing | Ask it to show the evidence. Its rules forbid unverified claims, and it should say "I could not find X" rather than guess. |

## NOTICE

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- AI Disclosure: This file was entirely AI-generated. The AI-generated portions are made available under CC0-1.0 and
  not subject to the project's license. The human contributor has reviewed and verified that the content is correct.
- SPDX-License-Identifier: CC-BY-4.0 AND CC0-1.0
- SPDX-FileCopyrightText: 2026 Catena-X Automotive Network e.V.
- SPDX-FileCopyrightText: 2026 Contributors to the Eclipse Foundation
- Assisted-By: Claude Code (claude-opus-5-5)
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
