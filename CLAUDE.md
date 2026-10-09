# CLAUDE.md

All project information — context, code style preferences, commands, and architecture
decisions — lives in AGENTS.md. Read it first.

@AGENTS.md

Do not duplicate project information here. This file is reserved for Claude-Code-specific
directives only (e.g. `@imports`, `/init` behavior).

## `/init` behavior

If `/init` is run again in this repository, it must update AGENTS.md directly with any new
codebase documentation it generates — do not expand this file. CLAUDE.md only exists to
point Claude Code at AGENTS.md.
