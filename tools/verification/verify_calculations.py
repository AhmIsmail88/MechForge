#!/usr/bin/env python3
"""
Independent verification of the MechForge calculation engine (v2).

The engine's dump reports every INPUT in its SI base unit (with the display unit only
as metadata) and every RESULT in the unit the calculator declares. This script
re-derives each calculator independently from the textbook equations in Python and
compares. No MechForge source code is read.
"""
import json
import math
import sys

G = 9.80665
R_UNIVERSAL = 8.314462618
CP_AIR = 1.005
H_FG = 2501.0
KW_PER_TR = 3.5168528
BTUH_PER_KW = 3412.142


def colebrook(re, rel_rough):
    if re < 2300:
        return 64.0 / re
    f = 0.25 / (math.log10(rel_rough / 3.7 + 5.74 / re ** 0.9)) ** 2
    for _ in range(300):
        nxt = 1.0 / (2.0 * math.log10(rel_rough / 3.7 + 2.51 / (re * math.sqrt(f)))) ** 2
        if abs(nxt - f) < 1e-15 * nxt:
            return nxt
        f = nxt
    return f


def lmtd(dt1, dt2):
    return dt1 if abs(dt1 - dt2) < 1e-12 else (dt1 - dt2) / math.log(dt1 / dt2)


def expectation(calc, scenario, raw):
    """raw maps input id -> SI base value."""
    x = lambda k: raw[k]

    if calc == "pump-power":
        ph = x("rho") * G * x("q") * x("h") / 1000.0
        sh = ph / x("eta")
        motor = next((r for r in [0.55, 0.75, 1.1, 1.5, 2.2, 3.0, 4.0, 5.5, 7.5, 11.0, 15.0, 18.5,
                                  22.0, 30.0, 37.0, 45.0, 55.0, 75.0, 90.0] if r * 1000 >= sh * 1000), 90.0)
        return {"hydraulic": ph, "shaft": sh, "motor": motor}

    if calc == "pipe-velocity":
        return {"v": 4.0 * x("q") / (math.pi * x("d") ** 2)}

    if calc == "reynolds-number":
        return {"re": x("v") * x("d") / x("nu")}

    if calc == "darcy-weisbach":
        f = x("f") if "f" in raw else colebrook(x("re"), (x("eps") / x("d")) if "eps" in raw else 0.0)
        return {"hf": f * (x("l") / x("d")) * x("v") ** 2 / (2.0 * G), "f": f}

    if calc == "minor-losses":
        return {"hm": x("k") * x("v") ** 2 / (2.0 * G)}

    if calc == "friction-factor":
        rel = (x("eps") / x("d")) if "eps" in raw else 0.0
        return {"f": colebrook(x("re"), rel), "reld": rel}

    if calc == "orifice-flow":
        area = math.pi * x("d") ** 2 / 4.0
        jet = math.sqrt(2.0 * G * x("h"))
        q = x("cd") * area * jet
        return {"q": q * 3600.0, "qSi": q, "v": jet}

    if calc == "manning":
        v = (1.0 / x("n")) * x("r") ** (2.0 / 3.0) * x("s") ** 0.5
        return {"v": v, "q": x("a") * v * 3600.0, "qSi": x("a") * v}

    if calc == "hazen-williams":
        q = 0.278 * x("c") * x("d") ** 2.63 * x("s") ** 0.54
        v = q / (math.pi * x("d") ** 2 / 4.0)
        hf = 10.67 * 1000.0 * q ** 1.852 / (x("c") ** 1.852 * x("d") ** 4.87)
        return {"q": q * 3600.0, "v": v, "hf": hf, "gradient": hf / 1000.0}

    if calc == "npsh-available":
        patm = x("patm") if "patm" in raw else 101325.0
        pv = x("pv") if "pv" in raw else 2339.0
        rho = x("rho") if "rho" in raw else 998.2
        head = (patm - pv) / (rho * G)
        return {"npsha": head + x("hs") - x("hf"), "phead": head}

    if calc == "sensible-heat":
        rho = x("rho") if "rho" in raw else 1.2
        qs = rho * x("q") * CP_AIR * (x("tout") - x("tin"))
        return {"qs": qs, "qsBtuh": qs * BTUH_PER_KW}

    if calc == "total-cooling-load":
        rho = x("rho") if "rho" in raw else 1.2
        m = rho * x("q")
        qs = m * CP_AIR * (x("tout") - x("tin"))
        ql = m * H_FG * (x("wout") - x("win")) if "win" in raw else 0.0
        return {"qs": qs, "ql": ql, "qt": qs + ql}

    if calc == "latent-heat":
        rho = x("rho") if "rho" in raw else 1.2
        return {"ql": rho * x("q") * H_FG * (x("wout") - x("win")),
                "dw": x("wout") - x("win")}

    if calc == "airflow-converter":
        q = x("q")
        return {"m3h": q * 3600.0, "cfm": q / 4.719474432e-4, "ls": q * 1e3, "lmin": q * 6e4}

    if calc == "power-efficiency-converter":
        p = x("p")
        out = {"kw": p / 1000.0, "tr": p / KW_PER_TR / 1000.0}
        if "pelec" in raw:
            cop = p / x("pelec")
            out.update({"cop": cop, "eer": cop * 3.412141633,
                        "kwtr": (x("pelec") / 1000.0) / (p / KW_PER_TR / 1000.0)})
        return out

    if calc == "ideal-gas":
        rho = x("p") * x("m") / (R_UNIVERSAL * x("t"))
        return {"rho": rho, "sv": 1.0 / rho}

    if calc == "carnot-efficiency":
        eta = 1.0 - x("tc") / x("th")
        return {"eta": eta * 100.0, "ratio": eta}

    if calc == "thermal-efficiency":
        eta = x("w") / x("q")
        return {"eta": eta * 100.0, "ratio": eta, "rejected": (x("q") - x("w")) / 1000.0}

    if calc == "isentropic-relation":
        k = x("k") if "k" in raw else 1.4
        r = x("p2") / x("p1")
        t2 = x("t1") * r ** ((k - 1.0) / k)
        return {"t2": t2, "t2c": t2 - 273.15, "tratio": t2 / x("t1"),
                "dratio": r ** (1.0 / k), "pratio": r}

    if calc == "compressor-power":
        cp = x("cp") if "cp" in raw else 1005.0
        k = x("k") if "k" in raw else 1.4
        eta = x("eta") if "eta" in raw else 0.8
        r = x("p2") / x("p1")
        t2s = x("t1") * r ** ((k - 1.0) / k)
        t2a = x("t1") + (t2s - x("t1")) / eta
        return {"t2s": t2s, "t2a": t2a, "t2ac": t2a - 273.15,
                "pideal": x("m") * cp * (t2s - x("t1")) / 1000.0,
                "pshaft": x("m") * cp * (t2a - x("t1")) / 1000.0, "ratio": r}

    if calc == "lmtd":
        counter = x("arr") >= 0.5 if "arr" in raw else True
        dt1 = x("thin") - x("tcout") if counter else x("thin") - x("tcin")
        dt2 = x("thout") - x("tcin") if counter else x("thout") - x("tcout")
        return {"lmtd": lmtd(dt1, dt2), "dt1": dt1, "dt2": dt2}

    if calc == "power-torque-rpm":
        if "p" in raw and "n" in raw:
            return {"t": 9550.0 * (x("p") / 1000.0) / x("n")}
        if "t" in raw and "n" in raw:
            return {"p": x("t") * x("n") / 9550.0}
        if "p" in raw and "t" in raw:
            return {"n": 9550.0 * (x("p") / 1000.0) / x("t")}

    if calc == "torsional-stress":
        out = {}
        if "d" in raw:
            tau = 16.0 * x("t") / (math.pi * x("d") ** 3)
            out["tau"] = tau / 1e6
            if "taual" in raw:
                out["util"] = tau / x("taual")
        if "taual" in raw:
            out["dmin"] = (16.0 * x("t") / (math.pi * x("taual"))) ** (1.0 / 3.0) * 1000.0
        return out

    if calc == "bolt-torque":
        if "f" in raw:
            return {"t": x("k") * x("f") * x("d"), "f": x("f") / 1000.0}
        f = x("t") / (x("k") * x("d"))
        return {"t": x("t"), "f": f / 1000.0}

    if calc == "bearing-l10":
        exp = x("exp") if "exp" in raw else 3.0
        l10 = (x("c") / x("p")) ** exp * 1e6
        out = {"l10": l10}
        if "n" in raw:
            out["l10h"] = l10 / (60.0 * x("n"))
        return out

    if calc == "spring-rate":
        g = x("g") if "g" in raw else 79.3e9
        k = g * x("d") ** 4 / (8.0 * x("dm") ** 3 * x("n"))
        out = {"k": k, "kmm": k / 1000.0}
        if "f" in raw:
            out["delta"] = x("f") / k * 1000.0
        return out

    if calc == "beam-ss-udl":
        defl = 5.0 * x("w") * x("l") ** 4 / (384.0 * x("e") * x("i"))
        M = x("w") * x("l") ** 2 / 8.0
        out = {"defl": defl * 1000.0, "moment": M, "ratio": defl / x("l")}
        if "z" in raw:
            out["sigma"] = M / x("z") / 1e6
        return out

    if calc == "beam-cantilever-point":
        defl = x("p") * x("l") ** 3 / (3.0 * x("e") * x("i"))
        M = x("p") * x("l")
        out = {"defl": defl * 1000.0, "moment": M, "ratio": defl / x("l")}
        if "z" in raw:
            out["sigma"] = M / x("z") / 1e6
        return out

    if calc == "gear-ratio":
        i = x("z2") / x("z1")
        out = {"i": i}
        if "n1" in raw:
            out["n2"] = x("n1") / i
        if "t1" in raw:
            out["t2"] = x("t1") * i
        if "m" in raw:
            out["d1"] = x("m") * 1000.0 * x("z1")
            out["d2"] = x("m") * 1000.0 * x("z2")
        return out

    if calc == "duct-velocity":
        v = x("q") / (x("w") * x("h"))
        return {"v": v, "vFpm": v / 0.00508, "a": x("w") * x("h")}

    if calc == "duct-sizing":
        ratio = x("r") if "r" in raw else 1.0
        a = x("q") / x("v")
        return {"a": a, "deq": math.sqrt(4.0 * a / math.pi) * 1000.0,
                "w": math.sqrt(a * ratio) * 1000.0, "h": math.sqrt(a / ratio) * 1000.0}

    if calc == "duct-pressure-loss":
        rho = x("rho") if "rho" in raw else 1.2
        nu = x("nu") if "nu" in raw else 1.5e-5
        eps = x("eps") if "eps" in raw else 9e-5
        dh = x("d") if "d" in raw else 2.0 * x("w") * x("h") / (x("w") + x("h"))
        re = x("v") * dh / nu
        f = colebrook(re, eps / dh)
        dp = f * (x("l") / dh) * rho * x("v") ** 2 / 2.0
        return {"dp": dp, "dpPerM": dp / x("l"), "f": f, "dh": dh * 1000.0, "re": re}

    if calc == "fan-power":
        etad = x("etad") if "etad" in raw else 1.0
        pair = x("q") * x("dp")
        return {"pair": pair / 1000.0, "pshaft": pair / x("etaf") / 1000.0,
                "pmotor": pair / x("etaf") / etad / 1000.0}

    if calc == "air-changes-hour":
        if "q" in raw and "vroom" in raw:
            q = x("q"); v = x("vroom"); ach = q * 3600.0 / v
        elif "vroom" in raw and "ach" in raw:
            v = x("vroom"); ach = x("ach"); q = ach * v / 3600.0
        else:
            q = x("q"); ach = x("ach"); v = q * 3600.0 / ach
        return {"q": q * 3600.0, "qCfm": q / 4.719474432e-4, "qLs": q * 1000.0,
                "ach": ach, "vroom": v, "time": 60.0 / ach}

    if calc == "pipe-sizing":
        d = math.sqrt(4.0 * x("q") / (math.pi * x("v")))
        return {"d": d * 1000.0, "area": math.pi * d * d / 4.0}

    if calc == "pipe-weight":
        rho = x("rho") if "rho" in raw else 7850.0
        area = math.pi * (x("od") - x("t")) * x("t")
        return {"w": area * rho, "area": area, "id": (x("od") - 2 * x("t")) * 1000.0}

    if calc == "pipe-wall-thickness":
        tp = x("p") * x("d") / (2.0 * x("sigma"))
        return {"tp": tp * 1000.0, "t": (tp + x("ca")) * 1000.0}

    if calc == "thermal-expansion":
        dl = x("alpha") * x("l") * x("dt")
        return {"dl": dl * 1000.0, "perM": x("alpha") * x("dt") * 1000.0}

    if calc == "equivalent-length":
        leq = x("k") * x("d") / x("f")
        return {"leq": leq, "inD": leq / x("d")}

    if calc == "valve-kv":
        dp_bar = x("dp") / 1e5
        return {"q": x("kv") * math.sqrt(dp_bar / x("sg")), "cv": 1.156 * x("kv"), "dp": dp_bar}

    if calc == "tank-volume":
        v = x("q") * x("t")
        return {"v": v, "vl": v * 1000.0}

    if calc == "detention-time":
        t = x("v") / x("q")
        return {"t": t / 3600.0, "tm": t / 60.0}

    if calc == "chlorine-dose":
        m = x("q") * x("dose")
        return {"mr": m * 86400.0, "mh": m * 3600.0}

    if calc == "peak-flow":
        return {"qp": x("q") * x("pf") * 3600.0, "qpd": x("q") * x("pf") * 86400.0}

    if calc == "hydraulic-loading":
        hlr = x("q") / x("a")
        return {"hlr": hlr * 86400.0, "hlrh": hlr * 3600.0}

    if calc == "sprinkler-discharge":
        q_lmin = x("k") * math.sqrt(x("p") / 1e5)
        return {"q": q_lmin, "qGpm": q_lmin / 3.785411784, "qM3h": q_lmin * 0.06}

    if calc == "hose-nozzle-flow":
        d_in = x("d") / 0.0254
        p_psi = x("p") / 6894.757293168361
        q_gpm = 29.7 * d_in * d_in * math.sqrt(p_psi)
        return {"q": q_gpm, "qLmin": q_gpm * 3.785411784, "qM3h": q_gpm * 3.785411784 * 0.06}

    if calc == "fire-pump-head":
        rho = x("rho") if "rho" in raw else 998.2
        dp = x("preq") - (x("pavail") if "pavail" in raw else 0.0)
        phead = dp / (rho * G)
        return {"h": phead + x("hstatic") + x("hf"), "phead": phead, "dp": dp / 1e5}

    if calc == "fire-pump-power":
        rho = x("rho") if "rho" in raw else 998.2
        hyd = rho * G * x("q") * x("h") / 1000.0
        shaft = hyd / x("eta")
        motors = [0.55, 0.75, 1.1, 1.5, 2.2, 3.0, 4.0, 5.5, 7.5, 11.0, 15.0, 18.5, 22.0, 30.0,
                  37.0, 45.0, 55.0, 75.0, 90.0, 110.0, 132.0, 160.0, 200.0]
        motor = next((m for m in motors if m >= shaft), 200.0)
        return {"hydraulic": hyd, "shaft": shaft, "motor": motor}

    if calc == "water-hammer":
        rho = x("rho") if "rho" in raw else 1000.0
        dp = rho * x("c") * x("dv")
        out = {"dp": dp / 1e5, "dpMpa": dp / 1e6, "head": dp / (rho * G)}
        if "l" in raw:
            out["tc"] = 2.0 * x("l") / x("c")
        return out

    if calc == "heat-exchanger-duty":
        cp = x("cp") if "cp" in raw else 4186.0
        q = x("m") * cp * (x("tout") - x("tin"))
        out = {"q": q / 1000.0, "qBtuh": q / 1000.0 * 3412.142, "dt": x("tout") - x("tin")}
        if all(k in raw for k in ("u", "a", "thin", "thout", "tcin", "tcout")):
            counter = (x("arr") >= 0.5) if "arr" in raw else True
            dt1 = x("thin") - x("tcout") if counter else x("thin") - x("tcin")
            dt2 = x("thout") - x("tcin") if counter else x("thout") - x("tcout")
            if dt1 > 0 and dt2 > 0:
                lm = dt1 if abs(dt1 - dt2) < 1e-9 else (dt1 - dt2) / math.log(dt1 / dt2)
                out["lmtd"] = lm
                out["qArea"] = x("u") * x("a") * lm / 1000.0
        return out

    if calc == "hx-effectiveness-ntu":
        ntu = x("ntu")
        cr = x("cr") if "cr" in raw else 0.0
        counter = (x("arr") >= 0.5) if "arr" in raw else True
        if abs(cr) < 1e-9:
            eps = 1.0 - math.exp(-ntu)
        elif counter and abs(1.0 - cr) < 1e-9:
            eps = ntu / (1.0 + ntu)
        elif counter:
            eps = (1.0 - math.exp(-ntu * (1.0 - cr))) / (1.0 - cr * math.exp(-ntu * (1.0 - cr)))
        else:
            eps = (1.0 - math.exp(-ntu * (1.0 + cr))) / (1.0 + cr)
        return {"eps": eps * 100.0, "epsFrac": eps, "ntuUsed": ntu, "crUsed": cr}

    if calc == "pump-affinity-laws":
        r = x("n2") / x("n1")
        out = {"q2": x("q1") * r * 3600.0, "h2": x("h1") * r * r, "p2": x("p1") / 1000.0 * r ** 3}
        if "d1" in raw and "d2" in raw:
            rd = x("d2") / x("d1")
            out.update({"q2t": x("q1") * rd * 3600.0, "h2t": x("h1") * rd * rd,
                        "p2t": x("p1") / 1000.0 * rd ** 3})
        return out

    if calc == "fan-laws":
        r = x("n2") / x("n1")
        dr = (x("rho2") if "rho2" in raw else 1.2) / (x("rho1") if "rho1" in raw else 1.2)
        return {"q2": x("q1") * r * 3600.0, "dp2": x("dp1") * r * r * dr,
                "p2": x("p1") / 1000.0 * r ** 3 * dr, "rp": r, "rd": dr}

    if calc == "fm200-agent-quantity":
        # W = (V/S)*(C/(100-C)); S = R*T/(P*M) for HFC-227ea, M = 170.03 g/mol
        M = 0.17003
        s = x("s") if "s" in raw else R_UNIVERSAL * x("t") / (101325.0 * M)
        # hazard index -> typical design concentration (NFPA 2001 HFC-227ea values)
        c = x("c") * 100.0 if "c" in raw else {0: 7.0, 1: 8.7, 2: 6.25}[int(x("hazard"))]
        ratio = c / (100.0 - c)
        w = (x("v") / s) * ratio
        f = w / x("v")
        out = {"w": w, "wLb": w / 0.45359237, "f": f, "fLb": f / 16.0184634,
               "vapourVolume": w * s, "sUsed": s, "cUsed": c}
        if "mcyl" in raw:
            out["cylinders"] = float(math.ceil(w / x("mcyl")))
        return out

    if calc == "co2-agent-quantity":
        # f = rho_vapour(T) * C/(100-C); rho_vapour = P*M/(R*T), M = 44.01 g/mol
        rho = 101325.0 * 0.04401 / (R_UNIVERSAL * x("t"))
        # hazard index -> design concentration (NFPA 12: 34 % surface, 50 % deep-seated)
        c = x("c") * 100.0 if "c" in raw else {0: 34.0, 1: 34.0, 2: 34.0, 3: 50.0}[int(x("hazard"))]
        ratio = c / (100.0 - c)
        f = rho * ratio
        w = x("v") * f
        charge = x("mcyl") if "mcyl" in raw else 45.0
        return {"w": w, "wLb": w / 0.45359237, "f": f, "fLb": f / 16.0184634,
                "rhoVapour": rho, "cylinders": float(math.ceil(w / charge)), "cUsed": c}

    if calc == "compression-ratio":
        ratio = x("p2") / x("p1")
        crmax = x("crmax") if "crmax" in raw else 4.0
        n = 1 if ratio <= crmax else math.ceil(math.log(ratio) / math.log(crmax))
        per = ratio ** (1.0 / n)
        return {"cr": ratio, "n": float(n), "crStage": per, "pint": x("p1") * per / 1e5}

    if calc == "pressure-converter":
        b = x("v")
        return {"pa": b, "kpa": b / 1e3, "mpa": b / 1e6, "bar": b / 1e5,
                "psi": b / 6894.757293168361, "atm": b / 101325.0,
                "mh2o": b / 9806.65, "fth2o": b / 2989.0669, "nmm2": b / 1e6}

    if calc == "flow-converter":
        b = x("v")
        return {"m3s": b, "m3h": b * 3600.0, "m3d": b * 86400.0, "ls": b * 1e3,
                "lmin": b * 6e4, "gpm": b / 6.30901964e-5, "cfm": b / 4.719474432e-4}

    if calc == "power-converter":
        b = x("v")
        return {"w": b, "kw": b / 1e3, "hp": b / 745.69987158227,
                "tr": b / 3516.8528, "btuh": b / 0.29307107017}

    if calc == "length-converter":
        b = x("v")
        return {"mm": b * 1000.0, "cm": b * 100.0, "m": b, "in": b / 0.0254, "ft": b / 0.3048}

    if calc == "temperature-converter":
        b = x("v")
        return {"k": b, "c": b - 273.15, "f": (b - 273.15) * 9.0 / 5.0 + 32.0}

    raise KeyError(f"no independent expectation for {calc}/{scenario}")


SCALING = [
    ("pump-power", "double-flow", "base", "hydraulic", 2.0, "P ~ Q"),
    ("pipe-velocity", "double-flow", "base", "v", 2.0, "v ~ Q"),
    ("reynolds-number", "double-velocity", "base", "re", 2.0, "Re ~ v"),
    ("darcy-weisbach", "fixed-f-double-v", "fixed-f", "hf", 4.0, "hf ~ v^2"),
    ("minor-losses", "double-velocity", "base", "hm", 4.0, "hm ~ v^2"),
    ("orifice-flow", "quadruple-head", "base", "q", 2.0, "Q ~ sqrt(H)"),
    ("manning", "quadruple-slope", "base", "q", 2.0, "Q ~ sqrt(S)"),
    ("hazen-williams", "d-600", "d-300", "q", 2.0 ** 2.63, "Q ~ D^2.63"),
    ("sensible-heat", "double-flow", "base", "qs", 2.0, "Qs ~ flow"),
    ("power-torque-rpm", "20kw-1500", "10kw-1500", "t", 2.0, "T ~ P"),
    ("torsional-stress", "200nm-20mm", "100nm-20mm", "tau", 2.0, "tau ~ T"),
    ("bearing-l10", "c60", "c30", "l10", 8.0, "L10 ~ (C/P)^3"),
    ("spring-rate", "n20", "n10", "k", 0.5, "k ~ 1/n"),
    ("beam-ss-udl", "L12", "L6", "defl", 16.0, "delta ~ L^4"),
    ("beam-ss-udl", "L12", "L6", "moment", 4.0, "M ~ L^2"),
    ("beam-cantilever-point", "L4", "L2", "defl", 8.0, "delta ~ L^3"),
    ("beam-cantilever-point", "L4", "L2", "moment", 2.0, "M ~ L"),
    ("pipe-sizing", "200m3h-2ms", "100m3h-2ms", "d", math.sqrt(2.0), "D ~ sqrt(Q)"),
    ("pipe-wall-thickness", "p20bar", "p10bar", "tp", 2.0, "t_p ~ p"),
    ("thermal-expansion", "dt100", "dt50", "dl", 2.0, "dL ~ dT"),
    ("equivalent-length", "k5", "k2.5", "leq", 2.0, "Leq ~ K"),
    ("valve-kv", "dp4bar", "dp1bar", "q", 2.0, "Q ~ sqrt(dP)"),
    ("tank-volume", "4h", "2h", "v", 2.0, "V ~ t"),
    ("detention-time", "1000-250", "500-250", "t", 2.0, "t ~ V"),
    ("chlorine-dose", "5000-4", "5000-2", "mr", 2.0, "m ~ dose"),
    ("peak-flow", "pf5", "pf2.5", "qp", 2.0, "Qpeak ~ PF"),
    ("hydraulic-loading", "1000-500", "1000-250", "hlr", 0.5, "HLR ~ 1/A"),
    ("air-changes-hour", "fan-480m3-ach15", "fan-240m3-ach15", "q", 2.0, "fan capacity ~ V at fixed ACH"),
    ("sprinkler-discharge", "p-28psi", "p-7psi", "q", 2.0, "sprinkler Q ~ sqrt(P)"),
    ("pump-affinity-laws", "n2-1800", "n2-1200", "q2", 1.5, "affinity: Q2 ~ N2"),
    ("fan-laws", "n2-1200", "n2-1000", "q2", 1.2, "fan laws: Q2 ~ N2"),
    ("heat-exchanger-duty", "m-2kgs", "m-1kgs", "q", 2.0, "HX duty ~ m_dot"),
    ("compression-ratio", "16bar", "4bar", "cr", 4.0, "CR ~ P2/P1"),
    ("fm200-agent-quantity", "class-a-v200", "class-a-v100", "w", 2.0, "FM-200 mass ~ V at fixed class/concentration"),
    ("co2-agent-quantity", "class-a-v250", "class-a-v100", "w", 2.5, "CO2 mass ~ V at fixed class/concentration"),
]


def main(dump_path):
    dumps = {}
    with open(dump_path, encoding="utf-8") as fh:
        for line in fh:
            line = line.strip()
            if line:
                rec = json.loads(line)
                dumps[(rec["calc"], rec["scenario"])] = rec

    failures, checked, per_calc = [], 0, {}
    for (calc, scenario), rec in sorted(dumps.items()):
        raw = {k: v["value"] for k, v in rec["inputs"].items()}
        if rec["results"].get("__error__"):
            failures.append((calc, scenario, "-", "engine returned an error"))
            continue
        try:
            expected = expectation(calc, scenario, raw)
        except KeyError as exc:
            failures.append((calc, scenario, "-", f"no independent model: {exc}"))
            continue
        for key, exp_val in expected.items():
            if key not in rec["results"]:
                failures.append((calc, scenario, key, "result missing in engine output"))
                continue
            got = rec["results"][key]
            tol = max(1e-9, abs(exp_val) * 1e-6)
            if (calc, key) in {("friction-factor", "f"), ("darcy-weisbach", "f"),
                               ("duct-pressure-loss", "f"), ("duct-pressure-loss", "dp"),
                               ("duct-pressure-loss", "dpPerM")}:
                tol = max(1e-9, abs(exp_val) * 1e-4)
            checked += 1
            per_calc[calc] = per_calc.get(calc, 0) + 1
            if abs(got - exp_val) > tol:
                failures.append((calc, scenario, key,
                                 f"expected {exp_val:.10g}, engine {got:.10g}, delta {abs(got-exp_val):.3e}"))

    scaling_failures = []
    for calc, num, den, key, ratio, label in SCALING:
        a = dumps.get((calc, num), {}).get("results", {}).get(key)
        b = dumps.get((calc, den), {}).get("results", {}).get(key)
        if a is None or b is None or b == 0:
            scaling_failures.append((calc, label, "missing scenario data"))
            continue
        got_ratio = a / b
        if abs(got_ratio - ratio) > max(1e-9, abs(ratio) * 1e-6):
            scaling_failures.append((calc, label, f"engine ratio {got_ratio:.10g}, expected {ratio:.10g}"))

    print("=" * 78)
    print("INDEPENDENT VERIFICATION REPORT (Python re-derivation vs Kotlin engine)")
    print("=" * 78)
    print(f"scenarios dumped        : {len(dumps)}")
    print(f"per-result comparisons  : {checked}")
    print(f"calculators covered     : {len(per_calc)}")
    print(f"value mismatches        : {len(failures)}")
    print(f"scaling-law mismatches  : {len(scaling_failures)}")
    print("-" * 78)
    if failures:
        print("MISMATCHES:")
        for row in failures:
            print("  FAIL:", " | ".join(str(v) for v in row))
    else:
        print("OK: every engine value agrees with the independent Python implementation.")
    if scaling_failures:
        print("SCALING MISMATCHES:")
        for row in scaling_failures:
            print("  FAIL:", " | ".join(str(v) for v in row))
    else:
        print("OK: every physical scaling law holds.")
    print("-" * 78)
    return 0 if not failures and not scaling_failures else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else "engine_dump.jsonl"))
