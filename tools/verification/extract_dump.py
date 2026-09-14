#!/usr/bin/env python3
"""
Extract the MechForge engine dump from a JUnit XML report.

The Kotlin test `EngineOutputDumpTest` prints one JSON line per scenario, prefixed
with `DUMP|`. Gradle captures that in the test report's system-out section.

Usage:
    python extract_dump.py <path-to-TEST-*.xml> [output.jsonl]
"""
import sys
import xml.etree.ElementTree as ET


def main(argv):
    if len(argv) < 2:
        print(__doc__)
        return 2
    xml_path = argv[1]
    out_path = argv[2] if len(argv) > 2 else "engine_dump.jsonl"

    tree = ET.parse(xml_path)
    lines = []
    for elem in tree.iter("system-out"):
        for line in (elem.text or "").splitlines():
            if line.startswith("DUMP|"):
                lines.append(line[5:])

    with open(out_path, "w", encoding="utf-8") as fh:
        fh.write("\n".join(lines) + "\n")

    print(f"extracted {len(lines)} scenarios -> {out_path}")
    return 0 if lines else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
