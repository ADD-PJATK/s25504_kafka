#!/usr/bin/env python3
import argparse
import json
import re
import sys
from pathlib import Path

def validate_mapping(mapping):
    if not isinstance(mapping, dict):
        sys.exit("Error: Mapping file must contain a JSON object.")
    
    if "replacements" not in mapping:
        sys.exit("Error: Mapping file is missing the 'replacements' key.")
        
    replacements = mapping["replacements"]
    if not isinstance(replacements, list):
        sys.exit("Error: 'replacements' must be an array.")
        
    for i, rule in enumerate(replacements):
        if not isinstance(rule, dict):
            sys.exit(f"Error: Replacement rule at index {i} must be an object.")
            
        if "find" not in rule:
            sys.exit(f"Error: Replacement rule at index {i} is missing the 'find' key.")
            
        if not isinstance(rule["find"], list) or len(rule["find"]) == 0:
            sys.exit(f"Error: 'find' in replacement rule at index {i} must be a non-empty array.")
            
        for j, find_str in enumerate(rule["find"]):
            if not isinstance(find_str, str) or len(find_str) == 0:
                sys.exit(f"Error: 'find' array element at index {j} in rule {i} must be a non-empty string.")
                
        if "replace" not in rule:
            sys.exit(f"Error: Replacement rule at index {i} is missing the 'replace' key.")
            
        if not isinstance(rule["replace"], str):
            sys.exit(f"Error: 'replace' in replacement rule at index {i} must be a string.")

def main():
    parser = argparse.ArgumentParser(description="Anonymize sensitive data in text files based on a JSON mapping.")
    parser.add_argument("--mapping", required=True, type=Path, help="Path to the JSON mapping file.")
    parser.add_argument("--input", required=True, type=Path, help="Path to the source file to anonymize.")
    parser.add_argument("--output", required=True, type=Path, help="Path to save the anonymized file.")
    
    args = parser.parse_args()
    
    # 1. Load and validate mapping file
    try:
        with open(args.mapping, 'r', encoding='utf-8') as f:
            mapping = json.load(f)
    except FileNotFoundError:
        sys.exit(f"Error: Mapping file not found at '{args.mapping}'.")
    except json.JSONDecodeError as e:
        sys.exit(f"Error: Mapping file contains invalid JSON: {e}")
    except Exception as e:
        sys.exit(f"Error reading mapping file: {e}")
        
    validate_mapping(mapping)
    
    # 2. Read input file
    try:
        with open(args.input, 'r', encoding='utf-8') as f:
            text = f.read()
    except FileNotFoundError:
        sys.exit(f"Error: Input file not found at '{args.input}'.")
    except UnicodeDecodeError:
        sys.exit(f"Error: Input file '{args.input}' is not valid UTF-8.")
    except Exception as e:
        sys.exit(f"Error reading input file: {e}")
        
    # 3. Perform replacements
    options = mapping.get("options", {})
    case_sensitive = options.get("case_sensitive", False)
    flags = 0 if case_sensitive else re.IGNORECASE
    
    for rule in mapping["replacements"]:
        replace_val = rule["replace"]
        for find_str in rule["find"]:
            # re.escape makes find_str a literal-text pattern.
            # The lambda replacement bypasses re.sub's backslash interpretation of
            # the replace string, so tokens like PERSON_01 or \SAFE are always
            # inserted verbatim regardless of their content.
            escaped_find = re.escape(find_str)
            text = re.sub(escaped_find, lambda _: replace_val, text, flags=flags)
            
    # 4. Write output file
    try:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        with open(args.output, 'w', encoding='utf-8') as f:
            f.write(text)
    except Exception as e:
        sys.exit(f"Error writing to output file '{args.output}': {e}")
        
    print(f"Successfully anonymized '{args.input}' and saved to '{args.output}'.")

if __name__ == "__main__":
    main()
