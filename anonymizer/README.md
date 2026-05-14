# Data Anonymizer CLI

A local, self-contained Python CLI application that anonymizes sensitive data in text files based on a predefined JSON mapping file.

## Prerequisites

- **Python**: Version 3.6 or higher.
- No external dependencies are required. The script uses only built-in standard Python libraries (`json`, `re`, `argparse`, `pathlib`, `sys`).

## Installation/Setup

1. Clone or download the source code containing `anonymize.py`.
2. Ensure you have Python installed (`python3 --version`).
3. The script is completely self-contained. No `pip install` or virtual environment is necessary.

## Usage

You can run the script via the command line using `python3 anonymize.py` (or `python anonymize.py` depending on your environment).

### Command-line Arguments

- `--mapping`: Path to the JSON mapping file containing the replacement rules.
- `--input`: Path to the source file to anonymize.
- `--output`: Path to save the anonymized output file.

### Example

```bash
python3 anonymize.py \
  --mapping config/mapping.json \
  --input data/source_data.csv \
  --output data/anonymized_data.csv
```

## Mapping File Format

The mapping file must be a valid JSON file. It defines the replacement rules and execution options.

### Structure

```json
{
  "replacements": [
    { 
      "find": ["string1", "string2"], 
      "replace": "TOKEN_1" 
    },
    { 
      "find": ["another_string"], 
      "replace": "TOKEN_2" 
    }
  ],
  "options": { 
    "case_sensitive": false 
  }
}
```

- **`replacements`** (Required): An array of replacement rules.
  - **`find`** (Required): An array of non-empty strings to search for in the text.
  - **`replace`** (Required): The string to replace the matched text with.
- **`options`** (Optional): A configuration object.
  - **`case_sensitive`** (Optional): A boolean indicating whether the search should be case-sensitive. Defaults to `false` (case-insensitive) if missing.

## Overlap Policy (Sequential Processing)

The script applies replacements using a strict, sequential find-and-replace approach.

1. **Rule Order**: The array of rules within the `replacements` key is processed sequentially from top to bottom.
2. **Find String Order**: Within each rule, the strings defined in the `find` array are processed in the exact order they are listed.
3. **Cumulative State**: Each replacement modifies the entire document state. The next replacement in the sequence operates on the *already modified* text, not the original source. 

**Note**: Because of this sequential, cumulative state processing, if a prior replacement introduces a string that matches a subsequent `find` rule, the subsequent rule *will* replace it. Be mindful of the ordering of your rules in the mapping file to prevent unintended overlaps or double-replacements.
