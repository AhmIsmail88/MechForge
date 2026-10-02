"""Independent equations for the fictional project; no Kotlin implementation imported."""
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
cases = json.loads((ROOT / 'docs/assumed-project-inputs-and-results.json').read_text(encoding='utf-8'))
checks = 0

def check(calc, key, expected, occurrence=0):
    global checks
    case = [c for c in cases if c['calculator'] == calc][occurrence]
    actual = next(r['value'] for r in case['output']['results'] if r['id'] == key)
    assert math.isclose(actual, expected, rel_tol=2e-6, abs_tol=1e-8), (calc, key, actual, expected)
    checks += 1

g, rho, q, d = 9.80665, 998.2, 100 / 3600, .15408
v = q / (math.pi * d**2 / 4)
re = v * d / 1e-6
# Solve Colebrook by bisection, independently of the engine's iteration method.
lo, hi = .008, .1
for _ in range(100):
    f = (lo + hi) / 2
    residual = 1 / math.sqrt(f) + 2 * math.log10(.000045 / d / 3.7 + 2.51 / re / math.sqrt(f))
    if residual > 0: lo = f
    else: hi = f
straight = f * 60 / d * v**2 / (2 * g)
minor = 5 * v**2 / (2 * g)
h = 400000 / (rho * g) + 10 + straight + minor + 1
check('pipe-velocity', 'v', v)
check('reynolds-number', 're', re)
check('darcy-weisbach', 'f', f)
check('darcy-weisbach', 'hf', straight)
check('minor-losses', 'hm', minor)
check('fire-pump-head', 'h', h)
check('fire-pump-power', 'hydraulic', rho * g * q * h / 1000)
check('fire-pump-power', 'shaft', rho * g * q * h / .75 / 1000)
na = (101325 - 2339) / (rho * g) + 2 - 1
check('npsh-available', 'npsha', na)
check('npsh-available', 'margin', na - 3)
check('tank-volume', 'v', 100)
airrho = 101325 / (287.05 * 308.15)
check('heat-dissipation', 'q', 30000 / (airrho * 1005 * 10) * 3600)
check('air-changes-hour', 'q', 320 * 6)
check('fan-power', 'pmotor', (4000 / 3600) * 400 / (.65 * .95) / 1000)
check('duct-velocity', 'v', (12000 / 3600) / (.8 * .6))
check('pipe-wall-thickness', 'tNom', (1.6 * 168.3 / (2 * (138 + 1.6 * .4)) + 1) / .875)
mass = 300 / .1373 * .075 / (1 - .075)
check('fm200-agent-quantity', 'w', mass)
check('fm200-agent-quantity', 'cylinders', math.ceil(mass / 60))
ff = .96 - .28 * math.sqrt(.02339 / 220.64)
limit = (.72 / .9)**2 * (10 - ff * .02339)
for i, dp in enumerate([2, 9.5]):
    check('valve-kv', 'q', .9 * 10 * math.sqrt(min(dp, limit) / .9982), i)
    check('valve-kv', 'dpChoked', limit, i)
# Independent equilibrium screen for the assumed 90-degree bend under test pressure.
force = max(12e5 * math.pi * d**2 / 4 + rho * q * v,
            16e5 * math.pi * d**2 / 4, 24e5 * math.pi * d**2 / 4)
steelmass = math.pi / 4 * (.1683**2 - d**2) * 3 * 7850
watermass = math.pi / 4 * d**2 * 3 * rho
normal = force + (steelmass + watermass + 50) * g
moment = force * .3
check('duck-foot-bend-base', 'hTotal', force / 1000)
check('duck-foot-bend-base', 'nTotal', normal / 1000)
check('duck-foot-bend-base', 'moment', moment)
check('duck-foot-bend-base', 'eccentricity', moment / normal * 1000)
check('duck-foot-bend-base', 'kernRadius', 800 / 8)
assert moment / normal > .8 / 8
print(f'{len(cases)} engine runs; {checks} independent numeric checks passed; uplift correctly identified.')
