"""Guard for the generated technical reference.

The generator once reported fewer Arabic step sections than the calculators it
covered (58 of 63) while announcing completion, because its reader did not
understand one of the two shapes a calculator can use to build its steps. The
miss was silent.

This script fails loudly instead: it regenerates the document and refuses to let
the numbers regress. Run it before committing a change to the generator or to
any calculator, and let CI run it on every push.

    python tools/docs/selfcheck.py
"""

import io
import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
DOC = os.path.join(REPO, "docs", "MechForge-technical-reference.html")
CALCS = os.path.join(REPO, "core", "src", "commonMain", "kotlin", "com", "mechforge", "core", "calcs")

# A floor, not a target: calculators may be added, none may disappear unnoticed.
MIN_CALCULATORS = 63
MIN_ARABIC_STEPS = 63

# Measured against the current document, not chosen: 85 caveat boxes are emitted today, so 80
# leaves room for a calculator or two to change shape without the check crying wolf.
MIN_CAVEATS = 80


def main() -> int:
    if not os.path.exists(DOC):
        print("FAIL: the reference has not been generated:", DOC)
        return 1
    doc = io.open(DOC, encoding="utf-8").read()

    ids = set()
    for name in sorted(os.listdir(CALCS)):
        if not name.endswith(".kt"):
            continue
        text = io.open(os.path.join(CALCS, name), encoding="utf-8").read()
        ids.update(re.findall(r'id = "([^"]+)"', text))
        ids.update(re.findall(r'converterDefinition\(\s*"([^"]+)"', text))

    per_calc = doc.count("class='calc'")
    steps = doc.count("Solution steps (Arabic)")
    warns = doc.count("Warnings and engineering caveats (Arabic)")
    caveats = doc.count("caveat")

    print("source calculators:      %d" % len(ids))
    print("document calculators:    %d" % per_calc)
    print("Arabic step sections:    %d" % steps)
    print("Arabic warning sections: %d" % warns)
    print("caveat boxes:            %d" % caveats)

    failures = []
    if per_calc != len(ids):
        failures.append("the document covers %d calculators but the sources define %d"
                        % (per_calc, len(ids)))
    if per_calc < MIN_CALCULATORS:
        failures.append("calculator count fell below %d" % MIN_CALCULATORS)
    if steps < MIN_ARABIC_STEPS:
        failures.append("Arabic step sections fell below %d (a calculator lost its Arabic steps, "
                        "or the generator stopped reading one of the two ways a calculator can "
                        "build them)" % MIN_ARABIC_STEPS)
    if warns < 40:
        failures.append("Arabic warning sections fell below 40")
    if caveats < MIN_CAVEATS:
        failures.append("caveat boxes fell below %d - the generator's notes are being dropped"
                        % MIN_CAVEATS)

    if failures:
        print("")
        for f in failures:
            print("FAIL: " + f)
        return 1
    print("")
    print("OK: the reference covers every calculator and every Arabic section")
    return 0


if __name__ == "__main__":
    sys.exit(main())