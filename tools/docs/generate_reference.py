#!/usr/bin/env python3
"""
Generate the MechForge technical reference (a single self-contained HTML file) from the
engine source itself, so the document can never drift from the code.

What it extracts per calculator:
  * id, name, category, description, formula display, reference, notes, keywords
  * every input: id, symbol, label, unit family, required/optional, default unit,
    validation bounds, pick-one options, library link
  * every reported result: id, label, unit
  * the calculation logic: the assignment lines of the calculate() body
  * the numeric constants declared next to the calculator

Usage:  python tools/docs/generate_reference.py
Output: docs/MechForge-technical-reference.html
"""

from __future__ import annotations

import html
import io
import os
import re
import sys

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
CALCS = os.path.join(REPO, "core", "src", "commonMain", "kotlin", "com", "mechforge", "core", "calcs")
UNITS = os.path.join(REPO, "core", "src", "commonMain", "kotlin", "com", "mechforge", "core", "units")
OUT = os.path.join(REPO, "docs", "MechForge-technical-reference.html")


def read(path: str) -> str:
    with io.open(path, encoding="utf-8") as fh:
        return fh.read()


def paren_block(text: str, start: int) -> str:
    """Return the balanced-parenthesis block that starts at the first '(' at or after start.

    CalculatorDefinition(...) and InputSpec(...) are constructor calls, so their body is
    delimited by parentheses - matching braces would jump past the whole definition.
    """
    i = text.index("(", start)
    depth = 0
    in_str = False
    esc = False
    for j in range(i, len(text)):
        c = text[j]
        if in_str:
            if esc:
                esc = False
            elif c == "\\":
                esc = True
            elif c == '"':
                in_str = False
            continue
        if c == '"':
            in_str = True
        elif c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
            if depth == 0:
                return text[i:j + 1]
    return text[i:]


def brace_block(text: str, start: int) -> str:
    """Return the balanced-brace block that starts at the first '{' at or after start."""
    try:
        i = text.index("{", start)
    except ValueError:
        return ""
    depth = 0
    for j in range(i, len(text)):
        if text[j] == "{":
            depth += 1
        elif text[j] == "}":
            depth -= 1
            if depth == 0:
                return text[i:j + 1]
    return text[i:]


def string_arg(block: str, name: str):
    m = re.search(r'(?<![A-Za-z0-9_])' + name + r'\s*=\s*"((?:[^"\\]|\\.)*)"', block, re.S)
    if m:
        return m.group(1)
    m = re.search(r'\b' + name + r'\(\s*"((?:[^"\\]|\\.)*)"', block, re.S)
    return m.group(1) if m else None


def parse_inputs(def_block: str):
    """All InputSpec(...) calls inside the inputs list."""
    inputs = []
    for m in re.finditer(r"InputSpec\(", def_block):
        block = paren_block(def_block, m.end() - 1)
        # positional: id, label, symbol, UnitFamily.X
        pos = re.findall(r'"((?:[^"\\]|\\.)*)"', block)
        fam = re.search(r"UnitFamily\.(\w+)", block)
        if not pos or not fam:
            continue
        entry = {
            "id": pos[0],
            "label": pos[1] if len(pos) > 1 else "",
            "symbol": pos[2] if len(pos) > 2 else "",
            "family": fam.group(1),
            "required": "required = false" not in block,
            "default_unit": string_arg(block, "defaultUnitId"),
            "default_value": string_arg(block, "defaultValue"),
            "min": (re.search(r"minValue\s*=\s*([0-9.eE+-]+)", block) or [None, None])[1],
            "max": (re.search(r"maxValue\s*=\s*([0-9.eE+-]+)", block) or [None, None])[1],
            "units": (
                re.search(r"allowedUnitIds\s*=\s*listOf\(([^)]*)\)", block).group(1)
                if "allowedUnitIds" in block else None
            ),
            "options": re.findall(r'InputOption\(\s*"([^"]+)"\s*,\s*"([^"]*)"', block),
            "library": string_arg(block, "libraryKey"),
            "assumed": string_arg(block, "assumedWhenOmitted"),
        }
        inputs.append(entry)
    return inputs


def parse_results(text: str):
    out = []
    for m in re.finditer(r'result\(\s*"([^"]+)"\s*,\s*"((?:[^"\\]|\\.)*)"', text):
        tail = text[m.end():m.end() + 160]
        unit = re.search(r'"([a-z0-9]+)"\s*(?:,\s*isPrimary\s*=\s*\w+\s*)?\)', tail)
        out.append({"id": m.group(1), "label": m.group(2), "unit": unit.group(1) if unit else ""})
    # de-duplicate, keeping order
    seen, uniq = set(), []
    for r in out:
        key = (r["id"], r["label"])
        if key not in seen:
            seen.add(key)
            uniq.append(r)
    return uniq


def parse_text_entries(body: str, key: str):
    """The human-readable entries of a `key = buildList/listOf { ... }` block.

    Each add(...) / list entry contributes one entry: its string literals are joined, so a
    multi-line concatenation comes back as one sentence. ${...} templates are kept verbatim -
    they are filled from the computed values when the calculator runs.
    """
    m = re.search(key + r"\s*=\s*(?:buildList|listOf|mutableListOf)\s*(?:<[^>]*>)?\s*[({]", body)
    if not m:
        return []
    opener = body[m.end() - 1]
    if opener == "(":
        block = paren_block(body, m.end() - 1)
        entries_raw = re.split(r',\s*\n', block)
    else:
        block = brace_block(body, m.end() - 1)
        entries_raw = re.split(r'\badd\(', block)
    entries = []
    for chunk in entries_raw:
        literals = re.findall(r'"((?:[^"\\]|\\.)*)"', chunk)
        if literals:
            text = "".join(literals).strip()
            if text:
                entries.append(re.sub(r"\s+", " ", text))
    if not entries:
        # an incremental list: `key += "..."`, sometimes inside an if/else - each literal is one
        # entry, which is what the document wants
        for line in body.splitlines():
            if line.strip().startswith(key + " += "):
                for literal in re.findall('"([^"]*)"', line):
                    literal = literal.strip()
                    if literal:
                        entries.append(re.sub(r"\s+", " ", literal))

    # keep order, drop duplicates
    seen, out = set(), []
    for e in entries:
        if e not in seen:
            seen.add(e)
            out.append(e)
    return out

def parse_logic(text: str):
    """Assignment / branch lines of the calculate() body - the calculation itself."""
    m = re.search(r"override fun calculate\(.*?\): CalcOutput\s*\{", text, re.S)
    if not m:
        return []
    body = brace_block(text, m.end() - 1)
    lines = []
    for raw in body.split("\n"):
        s = raw.strip()
        if not s or s.startswith("//") or s.startswith("/*"):
            continue
        if s.startswith("add(") or s.startswith("result(") or s.startswith("buildList"):
            continue
        if s.startswith("return ") or s == "}" or s.startswith("}"):
            continue
        if "=" in s or s.startswith("if ") or s.startswith("val ") or s.startswith("when"):
            lines.append(re.sub(r"\s+", " ", s))
    return lines[:40]


def parse_constants(text: str):
    consts = re.findall(r"private const val (\w+)\s*=\s*([^\n/]+)", text)
    return [(n, v.strip(), evaluate(v)) for n, v in consts]


def parse_option_lists(text: str):
    """Named option lists (hazard classes etc.) with their paired value."""
    out = []
    for m in re.finditer(r"private val (\w+)\s*:\s*List<Pair<InputOption, Double>>\s*=\s*listOf\(", text):
        block = paren_block(text, m.end() - 1)
        rows = re.findall(r'InputOption\(\s*"([^"]+)"\s*,\s*"([^"]*)"\s*\)\s*to\s*([0-9.]+)', block)
        if rows:
            out.append((m.group(1), rows))
    return out


def evaluate(expr: str) -> str:
    """Evaluate a Kotlin numeric expression so the document shows the real factor.

    Unit factors are written as expressions (1.0 / 3600.0, 0.45359237 / 3600.0,
    60.0 / (2.0 * Math.PI)); printing only the first literal would be wrong.
    """
    clean = expr.strip().rstrip(",;")
    # the regex capture can leave the parenthesis balance off (60.0 / (2.0 * Math.PI); repair it
    opens, closes = clean.count("("), clean.count(")")
    if opens > closes:
        clean = clean + ")" * (opens - closes)
    elif closes > opens:
        clean = clean[:len(clean) - (closes - opens)]
    stripped = re.sub(r"Math\.PI|Math\.E|math\.pi", "", clean)
    if not re.fullmatch(r"[0-9eE+\-*/(). \t]*", stripped):
        return expr.strip()
    py = clean.replace("Math.PI", "math.pi").replace("Math.E", "math.e")
    try:
        value = eval(py, {"__builtins__": {}}, {"math": __import__("math")})
    except Exception:
        return expr.strip()
    if isinstance(value, float):
        if value == 0:
            return "0"
        if abs(value) >= 1e-4 and abs(value) < 1e7:
            return f"{value:.10g}"
        return f"{value:.10g}"
    return str(value)

def unit_families():
    text = read(os.path.join(UNITS, "UnitFamily.kt"))
    fams = re.findall(r"^\s{4}([A-Z_]+)\(", text, re.M)
    units = read(os.path.join(UNITS, "Units.kt"))
    table = {}
    for m in re.finditer(r'factorUnit\(\s*"([^"]+)"\s*,\s*"([^"]*)"\s*,\s*UnitFamily\.(\w+)\s*,\s*([^)]+)\)', units):
        table.setdefault(m.group(3), []).append((m.group(1), m.group(2), evaluate(m.group(4))))
    return fams, table


def escape(s: str) -> str:
    return html.escape(s or "")


def main() -> int:
    calculators = []
    for name in sorted(os.listdir(CALCS)):
        if not name.endswith(".kt"):
            continue
        text = read(os.path.join(CALCS, name))
        # every definition in the file, keyed by the val that holds it
        defs = {}
        for m in re.finditer(r"val (\w+)\s*=\s*CalculatorDefinition\(", text):
            block = paren_block(text, m.end() - 1)
            defs[m.group(1)] = block
        # every calculator object, paired with the definition it was built from: this keeps the
        # logic, the results and the constants attached to the right calculator even in files
        # that hold several of them.
        for m in (re.finditer(r"object (\w+) : Calculator\((\w+)\)\s*\{", text) if defs else ()):
            owner, def_name = m.group(1), m.group(2)
            block = defs.get(def_name)
            if block is None:
                continue
            cid = string_arg(block, "id")
            if not cid:
                continue
            body = brace_block(text, m.end() - 1)
            calculators.append({
                "file": name,
                "object": owner,
                "id": cid,
                "name": string_arg(block, "name") or cid,
                "category": (re.search(r"CalculatorCategory\.(\w+)", block) or [None, "?"])[1],
                "description": string_arg(block, "description") or "",
                "formula": string_arg(block, "formulaDisplay") or "",
                "reference": string_arg(block, "reference") or "",
                "notes": string_arg(block, "notes") or "",
                "keywords": re.findall(r'"([^"]+)"', (re.search(r"keywords = listOf\(([^)]*)\)", block, re.S) or [None, ""])[1]),
                "inputs": parse_inputs(block),
                "results": parse_results(body),
                "logic": parse_logic(body),
                "steps": parse_text_entries(body, "steps"),
                "stepsAr": parse_text_entries(body, "stepsAr"),
                "warnings": parse_text_entries(body, "warnings"),
                "warningsAr": parse_text_entries(body, "warningsAr"),
                "constants": parse_constants(text),
                "option_lists": parse_option_lists(text),
            })
        # generic family converters: object X : FamilyConverter(converterDefinition(...))
        for m in re.finditer(r"object (\w+) : (\w+)\(", text):
            args = paren_block(text, m.end() - 1)
            cd = re.search(
                r'converterDefinition\(\s*"([^"]+)"\s*,\s*"([^"]*)"\s*,\s*UnitFamily\.(\w+)\s*,\s*"([^"]+)"',
                args,
            )
            if not cd:
                continue
            tpl = re.search(r"converterDefinition\([^)]*\)\s*=\s*CalculatorDefinition\(", text, re.S)
            tpl_block = paren_block(text, tpl.end() - 1) if tpl else ""
            body = brace_block(text, m.end() - 1)
            if not body:
                # the converter objects inherit their implementation from the shared class
                cls = re.search(r"class FamilyConverter\(.*?\) : Calculator\(\w+\)\s*\{", text, re.S)
                body = brace_block(text, cls.end() - 1) if cls else ""
            calculators.append({
                "file": name,
                "object": m.group(1),
                "id": cd.group(1),
                "name": cd.group(2),
                "category": (re.search(r"CalculatorCategory\.(\w+)", tpl_block) or [None, "UNIT_CONVERSION"])[1],
                "description": string_arg(tpl_block, "description") or "",
                "formula": string_arg(tpl_block, "formulaDisplay") or "",
                "reference": string_arg(tpl_block, "reference") or "",
                "notes": string_arg(tpl_block, "notes") or "",
                "keywords": [],
                "inputs": parse_inputs(tpl_block),
                "results": parse_results(body),
                "logic": parse_logic(body),
                "steps": parse_text_entries(body, "steps"),
                "stepsAr": parse_text_entries(body, "stepsAr"),
                "warnings": parse_text_entries(body, "warnings"),
                "warningsAr": parse_text_entries(body, "warningsAr"),
                "constants": parse_constants(text),
                "option_lists": [],
                "family": cd.group(3),
                "default_unit": cd.group(4),
            })

    calculators.sort(key=lambda c: (c["category"], c["id"]))
    families, unit_table = unit_families()

    parts = []
    A = parts.append
    A("<!DOCTYPE html><html lang='en'><head><meta charset='utf-8'>")
    A("<title>MechForge - technical reference</title>")
    A("""<style>
 :root { --ink:#0f1523; --mut:#5b6675; --line:#e0e5ec; --bg:#f5f7fa; --accent:#1b4f8a;
         --cyan:#0a8fa0; --code:#eef2f7; }
 * { box-sizing:border-box; }
 body { margin:0; padding:0 0 80px; background:var(--bg); color:var(--ink);
        font:15px/1.55 "Segoe UI",system-ui,-apple-system,sans-serif; }
 header { background:linear-gradient(140deg,#060a14,#0b1730 55%,#071019); color:#eef4ff;
          padding:44px 40px 36px; }
 header h1 { margin:0 0 6px; font-size:30px; letter-spacing:-.3px; }
 header p { margin:4px 0; color:#9fb0c8; }
 header .tag { display:inline-block; margin:10px 6px 0 0; padding:3px 10px; border-radius:999px;
               background:rgba(255,255,255,.08); border:1px solid rgba(255,255,255,.18);
               font-size:12.5px; color:#cfe0f5; }
 .wrap { max-width:1180px; margin:0 auto; padding:0 24px; }
 section { background:#fff; border:1px solid var(--line); border-radius:12px; padding:22px 24px;
           margin:20px 0; }
 h2 { font-size:21px; margin:0 0 12px; color:var(--accent); }
 h3 { font-size:16px; margin:22px 0 8px; }
 p, li { color:#26303d; }
 code, .mono { font-family:ui-monospace,Consolas,monospace; background:var(--code);
               padding:1px 5px; border-radius:4px; font-size:13px; }
 pre { background:var(--code); border:1px solid var(--line); border-radius:8px; padding:12px 14px;
       overflow-x:auto; font-size:12.5px; }
 table { width:100%; border-collapse:collapse; margin:10px 0 4px; font-size:13.5px; }
 th, td { text-align:left; padding:7px 9px; border-bottom:1px solid var(--line); vertical-align:top; }
 th { background:#eef2f7; font-size:12px; text-transform:uppercase; letter-spacing:.04em; color:#33404f; }
 .k { color:var(--mut); }
 .calc { border-left:3px solid var(--accent); padding-left:16px; margin:26px 0; }
 .calc h3 { margin:0 0 4px; }
 .pill { display:inline-block; padding:2px 9px; border-radius:999px; background:#e9f1fa;
         color:var(--accent); font-size:11.5px; margin-right:6px; }
 .warn { background:#fff8e8; border-left:3px solid #d9a72a; padding:10px 14px; border-radius:6px;
         margin:10px 0; }
 .note { background:#f0f6fb; border-left:3px solid var(--cyan); padding:10px 14px; border-radius:6px; }
 #filter { width:100%; padding:11px 13px; border:1px solid var(--line); border-radius:9px;
           font-size:15px; margin:6px 0 4px; }
 .toc { columns:2; column-gap:26px; }
 .toc a { color:var(--accent); text-decoration:none; font-size:13.5px; }
 .toc a:hover { text-decoration:underline; }
 .ar { direction:rtl; text-align:right; font-size:14.5px; }
</style></head><body>""")

    A("<header><div class='wrap'>")
    A("<h1>MechForge &mdash; technical reference</h1>")
    A("<p>Generated from the engine source: every calculator, its equation, its inputs, its outputs, "
      "its calculation logic, its constants and its engineering references.</p>")
    A("<span class='tag'>62 calculators</span><span class='tag'>one shared engine</span>"
      "<span class='tag'>offline-first</span><span class='tag'>Arabic + English</span>"
      "<span class='tag'>PDF + Excel reports</span>")
    A("</div></header><div class='wrap'>")

    A("<section><h2>كيف تستخدم هذا الملف (بالعربي)</h2><div class='ar'>")
    A("<p>ده مرجع تقني كامل للمشروع، <b>متولّد أوتوماتيك من كود المحرك</b> نفسه، فمفيش تعارض بينه وبين الكود. "
      "فيه: معادلة كل حاسبة، ومدخلاتها بالوحدات والتحقق، ونتايجها، و<b>منطق الحساب</b> سطر بسطر، "
      "والثوابت الرقمية، والمراجع الهندسية، والملاحظات والتحذيرات.</p>")
    A("<p>لو غيّرت أي حاسبة في <code>core/src/commonMain/kotlin/com/mechforge/core/calcs</code>، "
      "أعِد توليد الملف بالأمر: <code>python tools/docs/generate_reference.py</code> "
      "وهيتحدّث كله تلقائيًا ✓.</p>")
    A("<p>فوق كل حاسبة هتلاقي <b>معرّفها (id)</b> — ده اللي تستخدمه في الكود والتقارير. "
      "وفي مربع بحث تحت تقدر تفلتر بالاسم أو المعرّف أو كلمة في المعادلة.</p>")
    A("</div></section>")

    # ---- architecture
    A("<section><h2>1. Architecture and data flow</h2>")
    A("<table><tr><th>Module</th><th>Responsibility</th></tr>")
    A("<tr><td><code>core/</code></td><td>Kotlin Multiplatform engine: unit families and conversion, "
      "input validation, the 62 calculator definitions, calculation output model. No UI, no database, "
      "no platform API &mdash; so the whole engineering layer is testable on the JVM.</td></tr>")
    A("<tr><td><code>composeApp/commonMain</code></td><td>Shared Compose UI, the data layer "
      "(SQLDelight repositories, settings, reference library), the report model and the Excel writer, "
      "i18n, the glass theme.</td></tr>")
    A("<tr><td><code>composeApp/androidMain</code></td><td>Android entry point, manifest, launcher icons, "
      "the Android SQLite driver, the Android PDF renderer, file pickers, notifications.</td></tr>")
    A("<tr><td><code>composeApp/desktopMain</code></td><td>Desktop entry point, the desktop SQLite driver, "
      "native save dialogs, and the desktop PDF renderer (300&nbsp;dpi, lossless).</td></tr>")
    A("<tr><td><code>tools/verification/</code></td><td>Independent verification: it dumps the engine "
      "output and re-derives every equation in Python.</td></tr>")
    A("<tr><td><code>tools/docs/</code></td><td>This generator.</td></tr></table>")
    A("<h3>An input, end to end</h3>")
    A("<pre>UI text field  ->  unit conversion to the family base unit (Units.toBase)\n"
      "                ->  InputValue(id, baseValue, unitId)\n"
      "                ->  Calculator.run()  ->  validateDefinition()  (bounds, options, required)\n"
      "                ->  calculate()       ->  the equation in the base units\n"
      "                ->  CalcOutput(results, steps, warnings)  ->  report blocks  ->  PDF / Excel</pre>")
    A("<div class='note'><b>The rule that keeps it honest:</b> engineering formulas exist only in "
      "<code>core</code>; the UI never performs engineering arithmetic. Every result is expressed in the "
      "unit the calculator declares, and every calculator carries its formula, its reference and its "
      "assumptions.</div>")
    A("</section>")

    # ---- units
    A("<section><h2>2. Unit families</h2>")
    A("<p>Every input declares a family; the value is converted to the family's base unit before any "
      "arithmetic, so the equations stay in SI. Base unit shown first.</p>")
    A("<table><tr><th>Family</th><th>Units (id &rarr; symbol, factor to base)</th></tr>")
    for fam in sorted(unit_table.keys()):
        rows = unit_table[fam]
        def cell(u, sym, f):
            evaluated = evaluate(f)
            shown = f"&times;{escape(f)}"
            if evaluated != f:
                # unit factors are written as expressions (1.0 / 3600.0): show the value too,
                # so a reader cannot mistake the first literal for the factor.
                shown += f" <b>(= {escape(evaluated)})</b>"
            return f"<code>{escape(u)}</code> &rarr; {escape(sym)} {shown}"

        cells = ", ".join(cell(u, sym, f) for u, sym, f in rows)
        A(f"<tr><td><code>{escape(fam)}</code></td><td>{cells}</td></tr>")
    A("</table></section>")

    # ---- calculator index
    A("<section><h2>3. Calculator index</h2>")
    A("<input id='filter' placeholder='Filter: name, id, category, formula, keyword...' onkeyup='flt()'>")
    A("<p class='k' id='count'></p>")
    A("<div class='toc'>")
    for c in calculators:
        A(f"<a href='#{escape(c['id'])}'>{escape(c['name'])} <span class='k'>({escape(c['id'])})</span></a><br>")
    A("</div></section>")

    # ---- calculators
    A("<section><h2>4. Calculators in detail</h2>")
    current_cat = None
    for c in calculators:
        if c["category"] != current_cat:
            current_cat = c["category"]
            A(f"<h2 style='margin-top:34px'>{escape(current_cat.replace('_',' ').title())}</h2>")
        A(f"<div class='calc' id='{escape(c['id'])}' data-search=\"{escape((c['name'] + ' ' + c['id'] + ' ' + c['category'] + ' ' + c['formula'] + ' ' + ' '.join(c['keywords'])).lower())}\">")
        A(f"<h3>{escape(c['name'])}</h3>")
        A(f"<p><span class='pill'>{escape(c['id'])}</span>"
          f"<span class='pill'>{escape(c['category'])}</span>"
          f"<span class='k'>source: {escape(c['file'])}</span></p>")
        if c["description"]:
            A(f"<p>{escape(c['description'])}</p>")
        if c["formula"]:
            A(f"<p><b>Equation.</b> <code>{escape(c['formula'])}</code></p>")
        if c["option_lists"]:
            for lname, rows in c["option_lists"]:
                A(f"<p><b>{escape(lname)}</b></p><table><tr><th>Option id</th><th>Label</th><th>Value</th></tr>")
                for oid, label, value in rows:
                    A(f"<tr><td><code>{escape(oid)}</code></td><td>{escape(label)}</td><td><code>{escape(value)}</code></td></tr>")
                A("</table>")
        A("<p><b>Inputs</b></p><table><tr><th>id</th><th>symbol</th><th>label</th><th>family</th>"
          "<th>unit</th><th>required</th><th>validation</th><th>notes</th></tr>")
        for i in c["inputs"]:
            val = []
            if i["min"]:
                val.append(f"&gt; {escape(i['min'])}")
            if i["max"]:
                val.append(f"&lt; {escape(i['max'])}")
            extra = []
            if i["units"]:
                extra.append("units: " + escape(i["units"].replace('"', "")))
            if i["options"]:
                extra.append("options: " + escape(", ".join(o[0] for o in i["options"])))
            if i["default_value"]:
                extra.append("default: " + escape(i["default_value"]))
            if i["library"]:
                extra.append("library: " + escape(i["library"]))
            if i["assumed"]:
                # the engine raises this warning when the input is omitted (review P1-6)
                extra.append("<b>ASSUMED if omitted:</b> " + escape(i["assumed"]))
            A(f"<tr><td><code>{escape(i['id'])}</code></td><td>{escape(i['symbol'])}</td>"
              f"<td>{escape(i['label'])}</td><td><code>{escape(i['family'])}</code></td>"
              f"<td>{escape(i['default_unit'] or '')}</td><td>{'yes' if i['required'] else 'optional'}</td>"
              f"<td>{' / '.join(val)}</td><td>{' &middot; '.join(extra)}</td></tr>")
        A("</table>")
        if c["results"]:
            A("<p><b>Results</b></p><table><tr><th>id</th><th>label</th><th>unit</th></tr>")
            for r in c["results"]:
                A(f"<tr><td><code>{escape(r['id'])}</code></td><td>{escape(r['label'])}</td>"
                  f"<td><code>{escape(r['unit'])}</code></td></tr>")
            A("</table>")
        if c["logic"]:
            A("<p><b>Calculation logic</b> (straight from the engine)</p><pre>")
            A(escape("\n".join(c["logic"])))
            A("</pre>")
        if c.get("steps"):
            A("<p><b>Solution steps</b> (the narrative the report prints; ${...} are filled from the "
              "computed values)</p><ol>")
            for s in c["steps"]:
                A(f"<li><code>{escape(s)}</code></li>")
            A("</ol>")
        if c.get("stepsAr"):
            A("<p><b>Solution steps (Arabic)</b> &mdash; the same narrative the report prints in Arabic</p>")
            A("<ol>" + "".join(f"<li><code>{escape(x)}</code></li>" for x in c["stepsAr"]) + "</ol>")
        if c.get("warningsAr"):
            A("<p><b>Warnings and engineering caveats (Arabic)</b></p>")
            for w in c["warningsAr"]:
                A(f"<div class='warn'>{escape(w)}</div>")
        if c.get("warnings"):
            A("<p><b>Warnings and engineering caveats</b></p>")
            for w in c["warnings"]:
                A(f"<div class='warn'>{escape(w)}</div>")
        if c["constants"]:
            A("<p><b>Constants used</b></p><table><tr><th>name</th><th>as written</th><th>value</th></tr>")
            for n, raw_v, evaluated in c["constants"]:
                A(f"<tr><td><code>{escape(n)}</code></td><td><code>{escape(raw_v)}</code></td>"
                  f"<td><code>{escape(evaluated)}</code></td></tr>")
            A("</table>")
        if c["reference"]:
            A(f"<p><b>Reference.</b> {escape(c['reference'])}</p>")
        if c["notes"]:
            A(f"<div class='note'>{escape(c['notes'])}</div>")
        if c["keywords"]:
            A(f"<p class='k'>keywords: {escape(', '.join(c['keywords']))}</p>")
        A("</div>")
    A("</section>")

    # ---- reports
    A("<section><h2>5. Reports (PDF and Excel)</h2>")
    A("<p>Both renderers consume the same structured block model, so they can never drift apart. "
      "The block order is: title, project data (letterhead), inputs, formula, calculation steps, results, "
      "warnings, notes, reference, signatures &mdash; plus the credit line under the signatures.</p>")
    A("<table><tr><th></th><th>PDF</th><th>Excel (.xlsx)</th></tr>")
    A("<tr><td>Android</td><td><code>PdfDocument</code> + canvas text and "
      "<code>StaticLayout</code> (vector text, correct Arabic shaping), paginated A4</td>"
      "<td rowspan='2'>hand-written OOXML writer (<code>XlsxReport</code>) using ZIP store + CRC32, "
      "one styled worksheet, inline strings, RTL sheet flag for Arabic, no third-party library</td></tr>")
    A("<tr><td>Desktop</td><td>Java2D page images rendered at 300&nbsp;dpi in a 1240&times;1754 drawing "
      "space and embedded with FlateDecode (lossless)</td></tr></table>")
    A("<p><b>Report language</b> is a setting of its own (follow the app / Arabic / English) and drives "
      "the labels, the layout direction and the font. <b>Code / edition</b> is part of the project data "
      "and is printed on the letterhead.</p>")
    A("<h3>Where the numbers come from</h3>")
    A("<p>The PDF and the Excel file contain the <i>same</i> results the screen shows: the engine output "
      "is the single source, and the report model is built once per export from "
      "<code>ReportSheet.build(definition, inputs, output, title, labels, signature)</code>.</p>")
    A("</section>")

    # ---- verification
    A("<section><h2>6. Verification and how to re-run it</h2>")
    A("<pre># engine and app tests\n"
      "./gradlew :core:jvmTest :composeApp:desktopTest\n\n"
      "# independent re-derivation of every equation\n"
      "python tools/verification/extract_dump.py \\\n"
      "    core/build/test-results/jvmTest/TEST-com.mechforge.core.EngineOutputDumpTest.xml engine_dump.jsonl\n"
      "python tools/verification/verify_calculations.py engine_dump.jsonl\n\n"
      "# this document\n"
      "python tools/docs/generate_reference.py</pre>")
    A("<p>The harness re-implements the equations in Python from the engine's own output, and also checks "
      "physical scaling laws (doubling a volume doubles a flow, and so on). Any disagreement is a bug in "
      "one of the two implementations.</p>")
    A("</section>")

    # ---- build
    A("<section><h2>7. Building and packaging</h2>")
    A("<pre># desktop app (dev)\n./gradlew :composeApp:run\n\n"
      "# Android app on a connected device\n./gradlew :composeApp:installDebug\n\n"
      "# signed release APK  (signing/keystore.properties, gitignored)\n./gradlew :composeApp:assembleRelease\n\n"
      "# Windows .exe app image  (needs jpackage: a full JDK 17+ or JBR 21 as JAVA_HOME)\n"
      "./gradlew :composeApp:createDistributable</pre>")
    A("<div class='warn'><b>Packaging note.</b> The app stores data through SQLite over JDBC, so the "
      "trimmed runtime jpackage builds must include <code>java.sql</code> (and <code>jdk.unsupported</code>); "
      "this is declared in <code>nativeDistributions { modules(...) }</code>. Without it the packaged app "
      "fails at startup with <code>NoClassDefFoundError: java/sql/DriverManager</code>. An MSI also needs "
      "the WiX toolset; the Exe app image does not.</div>")
    A("</section>")

    # ---- conventions
    A("<section><h2>8. Conventions, assumptions and limits</h2>")
    A("<ul>")
    A("<li><b>No copyrighted tables are embedded.</b> Calculators implement and cite the engineering "
      "relations; code data (allowable stresses, flooding factors, design concentrations, listed "
      "equipment data) is entered by the engineer or imported into the reference library.</li>")
    A("<li><b>No compliance claim.</b> A mathematically correct result is not a code-compliant design: "
      "the hazard classification, the edition in force, the manufacturer's listed data and the "
      "installation requirements remain the engineer's responsibility.</li>")
    A("<li><b>Base units.</b> All arithmetic happens in SI base units; the UI converts in and out, and "
      "range checks are expressed in base units too.</li>")
    A("<li><b>Optional inputs</b> change the shape of the answer (a missing S falls back to the ideal-gas "
      "estimate, a missing cylinder charge suppresses the cylinder block) and the calculator always shows "
      "which assumption it used, in the steps and in the warnings.</li>")
    A("<li><b>Offline.</b> The Android app declares no INTERNET permission; everything is local. "
      "The only declared permission is POST_NOTIFICATIONS for the &quot;report exported&quot; notification.</li>")
    A("</ul></section>")

    # ---- screens and interface (10) ----
    base = os.path.join(REPO, "composeApp", "src", "commonMain", "kotlin", "com", "mechforge", "app")
    app_src = read(os.path.join(base, "ui", "app.kt"))
    theme_src = read(os.path.join(base, "ui", "theme", "theme.kt"))
    screens = re.findall(r"data (?:object|class)\s+(\w+)\s*(?:\([^)]*\))?\s*:\s*Screen", app_src)
    SCREENS = {
        "Home": "Entry screen: a searchable catalogue of every calculator, grouped by category, with the user's favourites and the most recently used calculations above it. The search filters by name, keywords and category.",
        "Converter": "A unit-converter surface: choose a quantity type, enter one value and read it in every unit of that family at once. It uses the same unit engine as the calculators, so a conversion can never disagree with a calculator's answer.",
        "Calculator": "The calculation screen: the name, description, equation and reference of the calculator are shown before anything is calculated; then the declared inputs, each with its own unit picker, validation message and - where the input declares a library key - a pick-list from the reference library; then Calculate and Reset; then the primary and secondary results with per-result unit switching; then the numeric step-by-step working, the warnings, the save-to-history dialog, and the PDF and Excel exports.",
        "History": "Every saved calculation with its calculator, inputs and results, the calculation number, revision and status, the project it belongs to and the frozen project snapshot it was calculated under. Entries can be renamed, duplicated, deleted and reopened, which restores the inputs, the title, the revision metadata and the project context into the calculator screen.",
        "Favorites": "The calculators the user starred, for quick access. Starring is done from the calculator screen.",
        "Projects": "The project register: create, rename and delete projects, set the active project, and edit the project record - number, client, consultant, contractor, location, revision, status, prepared and checked by, codes, code edition, department, document package, drawing and specification references and the design standard. Each project also opens a dashboard of its disciplines, its recent calculations and its status percentages, and can print a project register and a full calculation package to PDF or Excel.",
        "References": "The engineering reference library: built-in datasets (pipe outside diameters, pipe wall thickness by schedule, motor ratings, material densities, water properties, pipe roughness) plus CSV and JSON datasets the user imports and names. Datasets can be browsed, expanded row by row and deleted, and inputs whose spec declares a library key offer their values as pick-lists.",
        "Settings": "Appearance (system, light, dark), the application language - Arabic mirrors the whole interface to RTL - the report language independently of it, the report identification fields printed in every exported header (project, client, engineer, location, document number and revision, code, checked by) and the company logo used on the PDF letterhead.",
        "About": "What the app is and is not: the mission, the local-first and offline stance, how the reference data is treated, the version, and the disclaimer that a result is engineering support and not an approval.",
    }
    missing = [s for s in screens if s not in SCREENS]
    if missing:
        print("FAIL: screens declared in the code but not described here:", missing)
        return 1
    nav = re.findall(r"NavItem\(Screen\.(\w+),", app_src)
    width = re.search(r"private val CompactWidth = ([0-9.]+)\.dp", app_src)

    A("<section><h2>10. Screens and the interface</h2>")
    A("<p>The app is one window whose navigation shell switches between the screens below. The shell "
      "has two layouts: below " + (width.group(1) if width else "600") + "dp of width (phones) the "
      "navigation becomes a drawer behind a top bar, and from that width up (desktop, tablets) a fixed "
      "230dp sidebar replaces it. The application language drives the whole layout - Arabic mirrors "
      "everything to RTL, which moves the sidebar to the other side and reverses every row.</p>")
    A("<h3>10.1 The screens, in navigation order</h3>")
    A("<table><tr><th>Screen</th><th>What it does</th></tr>")
    for name in (nav or []) + ["Calculator"]:
        A("<tr><td><b>" + escape(name) + "</b></td><td>" + SCREENS[name] + "</td></tr>")
    A("</table>")
    A("<p><i>The generator reads the screen list from the code and fails if a screen exists without a "
      "written description here, so this table cannot fall behind the app.</i></p>")

    A("<h3>10.2 Colours</h3>")
    A("<p>Two complete schemes are declared in <code>theme.kt</code>. The values below are read from "
      "that file, so the document cannot disagree with the build.</p>")
    light_block = re.search(r"lightColorScheme\(([\s\S]*?)\n\)", theme_src)
    dark_block = re.search(r"darkColorScheme\(([\s\S]*?)\n\)", theme_src)
    light_tokens = dict(re.findall(r"(\w+)\s*=\s*Color\((0x[0-9A-Fa-f]+)\)", light_block.group(1))) if light_block else {}
    dark_tokens = dict(re.findall(r"(\w+)\s*=\s*Color\((0x[0-9A-Fa-f]+)\)", dark_block.group(1))) if dark_block else {}
    A("<table><tr><th>Token</th><th>Light</th><th>Dark</th></tr>")
    for tok in light_tokens.keys() | dark_tokens.keys():
        A("<tr><td><code>" + tok + "</code></td><td><code>" + light_tokens.get(tok, "&mdash;") +
          "</code></td><td><code>" + dark_tokens.get(tok, "&mdash;") + "</code></td></tr>")
    A("</table>")
    A("<p>The glass system sits on top of the scheme: a dark vertical gradient with two soft light orbs "
      "is painted once behind everything, and cards, sheets, dialogs and navigation chrome are "
      "translucent panes with a luminous hairline border - deep navy at 90 to 95 percent opacity in dark "
      "mode, white at 82 to 92 percent in light mode. No blur filter is used, so text never sits behind "
      "a blurred layer.</p>")

    A("<h3>10.3 Reading order, type and states</h3>")
    A("<ul>")
    A("<li><b>Reading order.</b> Arabic is the primary language and mirrors the layout to RTL. Every row "
      "uses start/end padding and AutoMirrored icons, so nothing breaks when the direction flips, and "
      "each calculator screen shows its equation before its inputs.</li>")
    A("<li><b>Type scale.</b> Material 3: headline for screen titles, title for section headings, body "
      "for content, label for captions, and the equation set in body-large directly under the "
      "calculator's description.</li>")
    A("<li><b>Shapes.</b> Material 3 shapes - 12dp for cards, 16dp for large panes - with content "
      "padding kept at or above the corner radius so a rounded corner can never clip text.</li>")
    A("<li><b>States.</b> Validation errors in the scheme's error colour, warnings prefixed with a "
      "warning sign, the selected navigation item and the primary action in the primary colour, and the "
      "favourite star in amber.</li>")
    A("<li><b>Branding.</b> The TAQARUB mark sits beside the product name in the navigation header with "
      "the company name under it, the development credit is printed at the foot of the navigation, and "
      "the signature of the author appears on the About screen and on every exported report.</li>")
    A("</ul>")
    A("</section>")

    A("<section><h2>9. Regenerating and editing this document</h2>")
    A("<p>Everything above section 5 is generated from the sources listed under <i>source</i> in each "
      "calculator block. Change a calculator (or add one), run "
      "<code>python tools/docs/generate_reference.py</code>, and this file is up to date again. "
      "Narrative sections (1, 2, 5&ndash;9) live in the generator "
      "(<code>tools/docs/generate_reference.py</code>) and can be edited there.</p>")
    A("</section>")

    A("</div><script>")
    A("function flt(){var q=document.getElementById('filter').value.toLowerCase();"
      "var d=document.querySelectorAll('.calc');var n=0;"
      "for(var i=0;i<d.length;i++){var m=d[i].getAttribute('data-search').indexOf(q)>=0;"
      "d[i].style.display=m?'':'none';if(m)n++;}"
      "document.getElementById('count').textContent=n+' of '+d.length+' calculators shown';}")
    A("flt();</script></body></html>")

    doc = "".join(parts)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with io.open(OUT, "w", encoding="utf-8", newline="") as fh:
        fh.write(doc)
    print(f"calculators: {len(calculators)}")
    print(f"written: {OUT}  ({len(doc)//1024} KB)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
