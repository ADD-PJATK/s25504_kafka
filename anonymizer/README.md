# Data Anonymizer CLI

A local, self-contained Python CLI that anonymizes sensitive data in text files
based on a JSON mapping file. All processing is done locally — no network calls,
no external services.

---

## Prerequisites

- **Python 3.6 or higher** (`python3 --version`)
- No external packages required. The script uses only the Python standard library
  (`argparse`, `json`, `pathlib`, `re`, `sys`).

---

## Installation

1. Download or clone the repository.
2. No `pip install` or virtual environment is needed — the script is self-contained.

---

## Usage

Run from inside the `anonymizer/` directory:

```bash
python3 anonymize.py --mapping <mapping.json> --input <source> --output <dest>
```

| Argument | Required | Description |
|----------|----------|-------------|
| `--mapping` | yes | Path to the JSON mapping file (see format below) |
| `--input`   | yes | Path to the source file to anonymize (must exist, must be UTF-8) |
| `--output`  | yes | Path to write the anonymized file |

On success the script prints a confirmation line to **stdout**:

```
Successfully anonymized '<input>' and saved to '<output>'.
```

On any error (missing file, invalid mapping, invalid UTF-8) the script prints a
readable message and exits with a non-zero status code. No partial output file
is written on error.

### Output directory

If the parent directory of `--output` does not exist, it is created automatically
(including any intermediate directories). The `--input` file is **never modified**
unless you deliberately pass the same path for both `--input` and `--output`.

---

## Examples

```bash
# Markdown
python3 anonymize.py \
  --mapping examples/mapping.json \
  --input  examples/note.md \
  --output examples/note.anon.md

# CSV
python3 anonymize.py \
  --mapping examples/mapping.json \
  --input  examples/data.csv \
  --output examples/data.anon.csv

# Plain text log
python3 anonymize.py \
  --mapping examples/mapping.json \
  --input  examples/log.txt \
  --output examples/log.anon.txt

# JSON config
python3 anonymize.py \
  --mapping examples/mapping.json \
  --input  examples/config.json \
  --output examples/config.anon.json
```

---

## Supported file types

The tool reads any UTF-8 text file regardless of extension. The following
extensions are explicitly tested and documented:

| Extension | Treatment |
|-----------|-----------|
| `.txt`    | Plain text — read and written as UTF-8 text |
| `.md`     | Markdown — read and written as UTF-8 text |
| `.csv`    | CSV — read and written as UTF-8 text; CSV structure is not parsed |
| `.json`   | JSON — read and written as UTF-8 text; JSON tree is not parsed |

On invalid UTF-8 input the script exits with an error.

---

## Mapping file format

The mapping file must be a valid JSON object with a required `replacements` key
and an optional `options` key.

```json
{
  "replacements": [
    {
      "find":    ["Anna Nowak", "A. Nowak", "NOWAK, Anna"],
      "replace": "PERSON_A"
    },
    {
      "find":    ["anna@firma.test"],
      "replace": "EMAIL_A"
    }
  ],
  "options": {
    "case_sensitive": false
  }
}
```

### `replacements` (required)

An array of replacement rules. Each rule has:

- **`find`** (required) — a JSON **array** of one or more non-empty strings to
  search for. Every entry in the array is replaced by the same token.
- **`replace`** (required) — a single string used as the verbatim substitution
  for every match of any entry in `find`.

Validation errors that cause the script to exit:

- `replacements` key absent or not an array
- A rule missing `find` or `replace`
- `find` is an empty array
- Any element of `find` is an empty string or not a string

### `options` (optional)

The entire `options` key may be omitted; defaults apply.

- **`case_sensitive`** (optional, boolean) — when `false` (the default when
  absent), matching is case-insensitive. When `true`, matching is
  case-sensitive. Case-insensitive mode replaces all case variants
  (`ANNA NOWAK`, `Anna Nowak`, `anna nowak`) with the single `replace` token.

---

## Matching behaviour

### Literal string matching

All `find` strings are matched **literally** — they are not treated as regular
expressions. Characters that have special meaning in regex (`$`, `.`, `(`, `)`,
`*`, etc.) are matched as plain text. You do not need to escape them.

### Replace value

The `replace` string is also inserted verbatim. Any content is safe to use as a
token (e.g. `PERSON_01`, `<REDACTED>`, `***`).

### Scanning strategy

For each `find` entry the **entire document** is scanned left-to-right and all
non-overlapping matches are replaced in a single pass before moving to the next
entry. There is no merge of multiple find patterns into one pass.

---

## Overlap and ordering policy

Rules and find strings within rules are applied in **strict sequential order**
with **cumulative document state** — each replacement operates on the text as
already modified by all previous replacements.

### Between rules

Rules are processed in the order they appear in the `replacements` array. A
replacement introduced by rule N is visible to rules N+1, N+2, … .

**Implication**: if rule 1 matches a substring of a string that rule 2 would
also match, rule 2 may never fire. Order your rules from most-specific to
least-specific, or from longest to shortest match, to avoid unintended
shadowing.

Example: placing a rule for `"anna"` before a rule for `"anna@firma.test"` will
turn `anna@firma.test` into `PERSON_A@firma.test` before the email rule can
match — the email rule then finds nothing.

### Within one rule

The strings in `find` are processed in array order, each against the current
document state. The first match "consumes" those characters; subsequent find
strings operate on the already-modified text.

Example: `find: ["ab", "bc"]` on input `"abc"`:

1. Pass for `"ab"`: `"abc"` → `"TOKENc"`
2. Pass for `"bc"`: `"TOKENc"` (no match — `"bc"` no longer exists)

Result: `"TOKENc"`. If the order were reversed (`["bc", "ab"]`):

1. Pass for `"bc"`: `"abc"` → `"aTOKEN"`
2. Pass for `"ab"`: `"aTOKEN"` (no match)

Result: `"aTOKEN"`. The outcome depends on the order of entries in `find`.

### Case-insensitive matching and replacement

When `case_sensitive` is `false`, the match is case-insensitive but the
replacement token is always inserted exactly as written in `replace`. Original
casing is not preserved.

Example: `find: ["Anna Nowak"]`, `replace: "PERSON_A"`, input `"ANNA NOWAK"`:

Result: `"PERSON_A"` (not `"ANNA PERSON_A"` or any other variant).
