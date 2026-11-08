# Commit Conventions

## Format

`<type>(<scope>): <what changed>` — imperative mood, ≤72 characters.

## Types

`fix` · `feat` · `test` · `ci` · `docs` · `chore` · `refactor` · `perf` · `build` · `revert`

## Scopes

`security` · `jwt` · `auth` · `user` · `validation` · `ci` · `docker` · `config` · `crawler` · `indexing` · `search` · `admin` · `ui`

## Rules

- The message describes **what changed in behavior**, not which file was touched.
- `Update <file>` is forbidden.
- Write the message **before** clicking Commit.
- `--amend` after push is allowed **only** if the message is wrong; use `--force-with-lease`, never `--force`.