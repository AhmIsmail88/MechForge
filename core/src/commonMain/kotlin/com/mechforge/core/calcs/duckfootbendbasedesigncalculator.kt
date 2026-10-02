package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Anchor base for a 90 degree duck foot bend (pump discharge thrust block base).
 *
 * The bend turns the flow through 90 degrees, so the thrust of both legs adds vectorially and the
 * base has to carry it. Two load cases are computed and the worse one governs:
 *
 *   operating - pump running pressure plus the momentum thrust rho*Q*V on each leg;
 *   design    - the design pressure acting statically, with no flow.
 *
 * From the governing resultant R the base plate, the stiffener ribs and the anchor bolts are sized:
 *
 *   plate  : cantilever strip of unit width, bending moment q*c^2/2, t = sqrt(6*M/sigma_allow);
 *   ribs   : simple bending with an assumed lever arm equal to the plate cantilever c;
 *   bolts  : elastic method for a bolt group on a circle, T_max = 4*M/(n*BCD).
 *
 * Gravity is taken as 9.80665 m/s2 (a constant, not an input: the engine has no acceleration family,
 * and the value is fixed by the specification). Every allowable stress, the density values and the
 * estimated elbow weight and centre height remain editable inputs, because they come from the
 * project's governing code and from the supplier rather than from this formula.
 */
private val Def = CalculatorDefinition(
    id = "duck-foot-bend-base",
    name = "Duck Foot Bend Base — Preliminary Load and Section Estimates",
    category = CalculatorCategory.PIPING,
    description = "Preliminary equal-leg bend loads and simplified section estimates. Does not determine structural adequacy or code compliance.",
    formulaDisplay = "R = sqrt(2)*max(P_pump*A + rho*Q*V, P_des*A)  |  t_plate = sqrt(6*q*c^2/2 / sigma_allow)  |  T_max = 4*M / (n*BCD)",
    reference = "Initial design assistance following the vector thrust of a 90 degree bend and the cantilever-plate, rib-bending and elastic bolt-group methods. The pressure side cites B31.3; the base plate, the bolts and the ribs are foundation items whose methods come from AISC 360-22 / AISC Design Guide 1 and whose loads from AWWA M11 practice. The concrete anchorage is governed by ACI 318-25 chapter 17 and is not checked here.",
    notes = "The two legs are assumed equal in diameter, which is what makes the resultant sqrt(2) times the leg thrust. H is the height of the bend centre above the plate and W_elbow is the bend weight: both are estimates and must be confirmed against the fabrication drawing and the supplier. The rib is designed on an assumed lever arm equal to the plate cantilever, and the plate as a cantilever strip under uniform bearing. Structural checks such as concrete bearing, weld design, plate flexibility limits and the effect of the thrust on the pipe itself are outside this calculator. This is initial design assistance: the result must be reviewed and approved by a licensed structural or mechanical engineer before construction.",
    keywords = listOf("duck foot bend", "thrust block", "base plate", "anchor bolt", "stiffener", "rib", "gusset", "bolt circle", "pump base", "momentum thrust", "bend", "support"),
    inputs = listOf(
        InputSpec("d", "Elbow / pipe internal diameter", "D", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("tElbow", "Elbow wall thickness", "t_elbow", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("tPipe", "Vertical pipe wall thickness", "t_pipe", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("l", "Vertical pipe length above the elbow", "L", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m"),
        InputSpec("q", "Water flow", "Q", UnitFamily.FLOW, minValue = 0.0, exclusiveMin = true, defaultUnitId = "ls"),
        InputSpec("pPump", "Pump operating pressure", "P_pump", UnitFamily.PRESSURE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "bar"),
        InputSpec("pDes", "Design pressure (static case)", "P_des", UnitFamily.PRESSURE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "bar"),
        InputSpec(
            "pTest", "Specified hydrostatic test pressure (verify code and temperature corrections)", "P_test", UnitFamily.PRESSURE,
            required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "bar",
            assumedWhenOmitted = "No hydrostatic test case computed: enter the specified pressure after verifying the applicable code, test/design temperature stress ratio and component limits; do not assume a universal 1.5 multiplier.",
        ),
        InputSpec("fy", "Steel yield stress", "Fy", UnitFamily.PRESSURE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mpa"),
        InputSpec("fos", "Design factor of safety", "FOS", UnitFamily.DIMENSIONLESS, minValue = 1.0, defaultUnitId = "dash"),
        InputSpec("rhoW", "Water density", "rho_w", UnitFamily.DENSITY, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kgm3"),
        InputSpec("gammaS", "Steel density", "gamma_s", UnitFamily.DENSITY, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kgm3"),
        InputSpec("h", "Height of the elbow centre above the plate", "H", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m"),
        InputSpec("wElbow", "Elbow weight", "W_elbow", UnitFamily.MASS, minValue = 0.0, defaultUnitId = "kg"),
        InputSpec("dPlate", "Base plate outer diameter", "D_plate", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("nRibs", "Number of stiffener ribs", "n_ribs", UnitFamily.DIMENSIONLESS, minValue = 3.0, maxValue = 64.0, defaultUnitId = "dash", integerOnly = true),
        InputSpec("hRib", "Rib height above the plate", "h_rib", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("nBolts", "Number of anchor bolts", "n_bolts", UnitFamily.DIMENSIONLESS, minValue = 4.0, maxValue = 64.0, defaultUnitId = "dash", integerOnly = true),
        InputSpec("bcd", "Bolt circle diameter", "BCD", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("sigmaBolt", "Allowable bolt tensile stress", "sigma_bolt", UnitFamily.PRESSURE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mpa"),
        InputSpec("tPlateSel", "Selected base plate thickness", "t_plate_sel", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("tRibSel", "Selected rib thickness", "t_rib_sel", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("dBoltSel", "Selected bolt diameter", "d_bolt_sel", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("atBolt", "Tabulated threaded tensile stress area per anchor", "A_t", UnitFamily.AREA,
            required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm2"),
        InputSpec("edgeSel", "Selected edge distance (bolt centre to plate edge)", "edge_sel", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
    ),
)

object DuckFootBendBaseDesignCalculator : Calculator(Def) {

    /** Gravity, m/s2 - one value across the whole engine. */
    private const val G = 9.80665

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val d = value(inputs, "d")                 // m
        val tElbow = value(inputs, "tElbow")       // m (reported, kept with the geometry)
        val tPipe = value(inputs, "tPipe")         // m
        val l = value(inputs, "l")                 // m
        val q = value(inputs, "q")                 // m3/s
        val pPump = value(inputs, "pPump")         // Pa
        val pDes = value(inputs, "pDes")           // Pa
        val fy = value(inputs, "fy")               // Pa
        val fos = value(inputs, "fos")
        val rhoW = value(inputs, "rhoW")           // kg/m3
        val gammaS = value(inputs, "gammaS")       // kg/m3
        val h = value(inputs, "h")                 // m
        val wElbow = value(inputs, "wElbow")       // kg
        val dPlate = value(inputs, "dPlate")       // m
        val nRibs = value(inputs, "nRibs")
        val hRib = value(inputs, "hRib")           // m
        val nBolts = value(inputs, "nBolts")
        val bcd = value(inputs, "bcd")             // m
        val sigmaBolt = value(inputs, "sigmaBolt") // Pa
        val tPlateSel = value(inputs, "tPlateSel") // m
        val tRibSel = value(inputs, "tRibSel")     // m
        val dBoltSel = value(inputs, "dBoltSel")   // m
        val edgeSel = value(inputs, "edgeSel")     // m

        if (dPlate <= d + 2.0 * tElbow || bcd >= dPlate) {
            throw com.mechforge.core.engine.ValidationException(listOf(com.mechforge.core.engine.InputError("dPlate", "Plate diameter must exceed the elbow outside diameter and bolt circle diameter.")))
        }
        // Geometry and flow
        val a = Math.PI / 4.0 * d * d
        val v = q / a

        // Operating thrust: pump pressure plus momentum change on each leg
        val fpOp = pPump * a
        val fm = rhoW * q * v
        val fOp = fpOp + fm
        val rOp = sqrt(2.0) * fOp

        // Design thrust: static, no flow
        val fpDes = pDes * a
        val rDes = sqrt(2.0) * fpDes

        // Hydrostatic test: static, no flow, at the test pressure. For a bend with a dead end this
        // case is frequently the governing one and leaving it out understates the thrust.
        val fpTest = if (has(inputs, "pTest")) value(inputs, "pTest") * a else null
        val rTest = fpTest?.let { sqrt(2.0) * it }

        val rGov = max(rOp, max(rDes, rTest ?: 0.0))
        val rh = rGov / sqrt(2.0)
        val rv = rGov / sqrt(2.0)

        // Loads on the base
        // Mean-diameter wall: the metal ring carries the mid-surface, and the inner-diameter
        // form understated the weight slightly.
        val wPipe = Math.PI * (d + tPipe) * tPipe * l * gammaS
        val wWater = a * l * rhoW
        val dead = wPipe + wWater + wElbow
        val nTotal = rv + dead * G
        val hTotal = rh
        val moment = hTotal * h

        // Base plate as a cantilever strip
        val aPlate = Math.PI / 4.0 * dPlate * dPlate
        val bearing = nTotal / aPlate
        val cantilever = (dPlate - (d + 2.0 * tElbow)) / 2.0
        val mPlate = bearing * cantilever * cantilever / 2.0
        val sigmaAllow = fy / fos
        val tPlateReq = sqrt(6.0 * mPlate / sigmaAllow)

        // Ribs: simple bending, assumed lever arm = plate cantilever
        val fRib = nTotal / nRibs
        val mRib = fRib * cantilever
        val tRibReq = 6.0 * mRib / (sigmaAllow * hRib * hRib)

        // Anchor bolts: elastic method for a circular group
        val tMax = 4.0 * moment / (nBolts * bcd)
        val vBolt = hTotal / nBolts
        val asReq = tMax / sigmaBolt
        val dBoltReq = sqrt(4.0 * asReq / Math.PI)
        val asSel = Math.PI / 4.0 * dBoltSel * dBoltSel
        val threadedArea = inputs["atBolt"]?.baseValue
        if (threadedArea != null && threadedArea > asSel) {
            throw com.mechforge.core.engine.ValidationException(listOf(com.mechforge.core.engine.InputError("atBolt", "Threaded tensile area cannot exceed the gross shank area.")))
        }
        val eccentricity = moment / nTotal
        val fullContact = eccentricity <= dPlate / 8.0
        val bearingMoment = 32.0 * moment / (Math.PI * dPlate * dPlate * dPlate)
        val sigmaTAct = tMax / asSel
        val tauAct = vBolt / asSel
        val interaction = sqrt(
            (sigmaTAct / sigmaBolt) * (sigmaTAct / sigmaBolt) +
                (tauAct / (0.6 * sigmaBolt)) * (tauAct / (0.6 * sigmaBolt)),
        )

        // The plate has to accommodate the bolt circle
        val edgeMin = 2.0 * dBoltSel
        val dPlateMin = bcd + 2.0 * edgeSel

        val plateOk = tPlateSel >= tPlateReq
        val ribOk = tRibSel >= tRibReq
        val boltOk = interaction <= 1.0
        val plateDiaOk = dPlate >= dPlateMin

        val warnings = buildList {
            if (!fullContact) add("The rigid circular full-contact model predicts uplift (e > D_plate/8). Uniform bearing and the plate/rib estimates cannot establish adequacy; solve partial contact and anchor forces together.")
            if (threadedArea == null) add("Threaded tensile stress area is missing. Gross-shank stresses are theoretical only; enter the tabulated A_t for a threaded-stress estimate.")
            add("PRELIMINARY MODEL ONLY: no structural pass/fail is issued. Uniform bearing ignores overturning; rib load sharing and the elastic bolt group are unverified assumptions. Bolt stresses use gross shank area, not threaded tensile area; equivalent solid diameter is NOT a selectable bolt size. Complete contact/uplift, threaded area, weld, anchor and concrete checks are required.")
            if (!plateOk) {
                add("Base plate thickness NOT sufficient: the selected ${Fmt.n(tPlateSel * 1000.0, 1)} mm is below the required ${Fmt.n(tPlateReq * 1000.0, 2)} mm - increase the plate thickness.")
            }
            if (!ribOk) {
                add("Rib thickness NOT sufficient: the selected ${Fmt.n(tRibSel * 1000.0, 1)} mm is below the required ${Fmt.n(tRibReq * 1000.0, 2)} mm - increase the thickness or the number of ribs.")
            }
            if (!boltOk) {
                add("Anchor bolts NOT sufficient: interaction ${Fmt.n(interaction, 3)} exceeds 1.0 - increase the bolt diameter, the number of bolts, or the bolt circle diameter.")
            }
            if (!plateDiaOk) {
                add("Plate diameter NOT sufficient for the bolt circle: ${Fmt.n(dPlate * 1000.0, 1)} mm against ${Fmt.n(dPlateMin * 1000.0, 1)} mm required - increase the plate diameter or reduce the bolt circle.")
            }
            if (kotlin.math.abs(nRibs - Math.round(nRibs).toDouble()) > 1e-9) {
                add("The number of ribs is not a whole number (${Fmt.n(nRibs, 3)}) - rib load sharing only means anything for a whole number of ribs.")
            }
            if (kotlin.math.abs(nBolts - Math.round(nBolts).toDouble()) > 1e-9) {
                add("The number of bolts is not a whole number (${Fmt.n(nBolts, 3)}) - bolt load sharing only means anything for a whole number of bolts.")
            }
            if (tPlateSel < tPlateReq * 1.05) {
                add("The plate thickness margin is under 5% - a small load increase will fail this check; confirm the sizes against the next commercial plate thickness.")
            }
            add("D = ${Fmt.n(d * 1000.0, 1)} mm legs are assumed equal in diameter, which is what makes the resultant sqrt(2) times one leg. H = ${Fmt.n(h, 3)} m and W_elbow = ${Fmt.n(wElbow, 1)} kg are estimates: confirm them against the fabrication drawing and the supplier before construction.")
            val gov = when (rGov) {
                rTest ?: -1.0 -> if (rTest != null) "hydrostatic test pressure" else ""
                rDes -> "design pressure (static)"
                else -> "operating (pump pressure plus momentum)"
            }
            add("The governing case was " + gov + " at ${Fmt.n(rGov / 1000.0, 1)} kN. Gravity is taken as 9.80665 m/s2, the same constant as the rest of the engine.")
            add("Initial design assistance: the result must be reviewed and approved by a licensed structural or mechanical engineer. NOT covered here: concrete anchorage (ACI 318-25 ch. 17 breakout, pullout, pryout, side-blowout), plate bending at the bolt group (AISC Design Guide 1, frequently the governing plate check), rib shear and bearing, wind and seismic loads, and friction as a shear path - all of which can govern.")
            add("Pump operating pressure should be entered as the maximum the pump can develop (shut-off or relief setting, gauge), not the rated duty pressure.")
        }

        return CalcOutput(
            results = listOf(
                result("v", "Water velocity in the bend", v, "ms"),
                result("rGov", "Governing resultant thrust R", rGov / 1000.0, "kn", isPrimary = true),
                result("nTotal", "Total vertical load on the base N_total", nTotal / 1000.0, "kn"),
                result("hTotal", "Total horizontal load H_total", hTotal / 1000.0, "kn"),
                result("moment", "Overturning moment at the plate M", moment, "nm"),
                result("bearing", "Theoretical mean bearing pressure (overturning omitted)", bearing / 1000.0, "kpa"),
                result("cantilever", "Plate cantilever c", cantilever * 1000.0, "mm"),
                result("tPlateReq", "Theoretical uniform-bearing plate thickness", tPlateReq * 1000.0, "mm"),
                result("tPlateMargin", "Base plate margin (selected - required)", (tPlateSel - tPlateReq) * 1000.0, "mm"),
                result("tRibReq", "Theoretical equal-load rib thickness", tRibReq * 1000.0, "mm"),
                result("tRibMargin", "Rib margin (selected - required)", (tRibSel - tRibReq) * 1000.0, "mm"),
                result("tMax", "Maximum tensile force per bolt T_max", tMax / 1000.0, "kn", isPrimary = true),
                result("vBolt", "Shear force per bolt", vBolt / 1000.0, "kn"),
                result("dBoltReq", "Theoretical equivalent solid diameter (not a thread size)", dBoltReq * 1000.0, "mm"),
                result("sigmaTAct", "Theoretical gross-section tensile stress (thread area not used)", sigmaTAct / 1e6, "mpa"),
                result("tauAct", "Theoretical gross-section shear stress", tauAct / 1e6, "mpa"),
                result("interaction", "Theoretical gross-section interaction (not a code check)", interaction, "dash"),
                result("edgeMin", "Guide minimum edge distance (2 x bolt diameter)", edgeMin * 1000.0, "mm"),
                result("dPlateMin", "Minimum plate diameter for the bolt circle", dPlateMin * 1000.0, "mm"),
            ) + buildList {
                add(result("eccentricity", "Theoretical load eccentricity M/N", eccentricity * 1000.0, "mm"))
                add(result("kernRadius", "Circular full-contact kern radius D/8", dPlate * 1000.0 / 8.0, "mm"))
                if (fullContact) {
                    add(result("bearingMax", "Theoretical full-contact maximum pressure", (bearing + bearingMoment) / 1000.0, "kpa"))
                    add(result("bearingMin", "Theoretical full-contact minimum pressure", (bearing - bearingMoment) / 1000.0, "kpa"))
                }
                threadedArea?.let {
                    add(result("sigmaThread", "Theoretical threaded tensile stress (elastic bolt-group force)", tMax / it / 1e6, "mpa"))
                }
            },
            steps = listOf(
                "Rigid circular contact screening: e=M/N=${Fmt.n(eccentricity * 1000.0, 3)} mm; kern D/8=${Fmt.n(dPlate * 1000.0 / 8.0, 3)} mm. Full contact: ${if (fullContact) "yes within this model" else "no; partial-contact analysis required"}.",
                "Linear full-contact pressure: q = N/A +/- 32*M/(pi*D^3); apply only when e <= D/8.",
                if (threadedArea != null) "Threaded area A_t=${Fmt.n(threadedArea * 1e6, 3)} mm2; sigma_thread=T_max/A_t=${Fmt.n(tMax / threadedArea / 1e6, 3)} MPa (elastic-group force, not anchor acceptance)." else "Threaded area was not entered; no threaded tensile stress is reported.",
                "Geometry: A = pi/4 x D^2 = pi/4 x ${Fmt.n(d, 4)}^2 = ${Fmt.n(a, 6)} m2   |   V = Q/A = ${Fmt.n(q, 6)} / ${Fmt.n(a, 6)} = ${Fmt.n(v, 4)} m/s",
                "Operating thrust per leg: Fp_op = P_pump x A = ${Fmt.n(pPump, 1)} x ${Fmt.n(a, 6)} = ${Fmt.n(fpOp, 1)} N   |   Fm = rho x Q x V = ${Fmt.n(rhoW, 1)} x ${Fmt.n(q, 6)} x ${Fmt.n(v, 4)} = ${Fmt.n(fm, 1)} N",
                "F_op = ${Fmt.n(fpOp, 1)} + ${Fmt.n(fm, 1)} = ${Fmt.n(fOp, 1)} N   ->   R_op = sqrt(2) x F_op = ${Fmt.n(rOp, 1)} N",
                "Design thrust (static): Fp_des = ${Fmt.n(pDes, 1)} x ${Fmt.n(a, 6)} = ${Fmt.n(fpDes, 1)} N   ->   R_des = sqrt(2) x Fp_des = ${Fmt.n(rDes, 1)} N",
                "Governing: R_gov = max(${Fmt.n(rOp / 1000.0, 1)}, ${Fmt.n(rDes / 1000.0, 1)}, ${Fmt.n((rTest ?: 0.0) / 1000.0, 1)}) = ${Fmt.n(rGov / 1000.0, 1)} kN   |   Rh = Rv = R_gov / sqrt(2) = ${Fmt.n(rh / 1000.0, 1)} kN",
                "Weights: W_pipe = pi x (D + t_pipe) x t_pipe x L x gamma_s = ${Fmt.n(wPipe, 2)} kg   |   W_water = A x L x rho_w = ${Fmt.n(wWater, 2)} kg   |   W_elbow = ${Fmt.n(wElbow, 1)} kg",
                "N_total = Rv + (W_pipe + W_water + W_elbow) x 9.81 = ${Fmt.n(rv / 1000.0, 1)} kN + ${Fmt.n(dead, 2)} x 9.80665 / 1000 = ${Fmt.n(nTotal / 1000.0, 1)} kN   |   M = H_total x H = ${Fmt.n(moment / 1000.0, 1)} kN.m",
                "Base plate: q = N_total / A_plate = ${Fmt.n(bearing / 1000.0, 2)} kPa   |   c = (D_plate - (D + 2*t_elbow))/2 = ${Fmt.n(cantilever * 1000.0, 1)} mm   |   M_plate = q x c^2 / 2 = ${Fmt.n(mPlate / 1000.0, 2)} kN.m/m",
                "sigma_allow = Fy / FOS = ${Fmt.n(fy / 1e6, 1)} / ${Fmt.n(fos, 3)} = ${Fmt.n(sigmaAllow / 1e6, 1)} MPa   ->   t_plate_req = sqrt(6 x M_plate / sigma_allow) = ${Fmt.n(tPlateReq * 1000.0, 2)} mm",
                "Ribs: F_rib = N_total / n_ribs = ${Fmt.n(fRib, 1)} N   |   M_rib = F_rib x c = ${Fmt.n(mRib / 1000.0, 2)} kN.m   ->   t_rib_req = 6 x M_rib / (sigma_allow x h_rib^2) = ${Fmt.n(tRibReq * 1000.0, 2)} mm",
                "Bolts: T_max = 4 x M / (n_bolts x BCD) = ${Fmt.n(tMax, 1)} N   |   V_bolt = H_total / n_bolts = ${Fmt.n(vBolt, 1)} N   ->   d_bolt_req = sqrt(4 x T_max / (pi x sigma_bolt)) = ${Fmt.n(dBoltReq * 1000.0, 2)} mm",
                "Bolt interaction = sqrt((sigma_t/sigma_bolt)^2 + (tau/(0.6 x sigma_bolt))^2) = sqrt((${Fmt.n(sigmaTAct / 1e6, 1)}/${Fmt.n(sigmaBolt / 1e6, 1)})^2 + (${Fmt.n(tauAct / 1e6, 1)}/${Fmt.n(0.6 * sigmaBolt / 1e6, 1)})^2) = ${Fmt.n(interaction, 3)}",
                "Plate fit: edge_min = 2 x d_bolt = ${Fmt.n(edgeMin * 1000.0, 1)} mm   |   D_plate_min = BCD + 2 x edge_sel = ${Fmt.n(dPlateMin * 1000.0, 1)} mm",
                "Elbow geometry recorded for the drawing: t_elbow = ${Fmt.n(tElbow * 1000.0, 1)} mm, D = ${Fmt.n(d * 1000.0, 1)} mm, L = ${Fmt.n(l, 3)} m, h_rib = ${Fmt.n(hRib * 1000.0, 1)} mm, n_ribs = ${Fmt.n(nRibs, 0)}, n_bolts = ${Fmt.n(nBolts, 0)}.",
            ),
            stepsAr = listOf(
                "فحص تلامس دائرة صلبة: e=M/N=${Fmt.n(eccentricity * 1000.0, 3)} mm; نواة التلامس D/8=${Fmt.n(dPlate * 1000.0 / 8.0, 3)} mm. التلامس الكامل: ${if (fullContact) "نعم داخل هذا النموذج" else "لا؛ يلزم تحليل التلامس الجزئي"}.",
                "ضغط التلامس الكامل الخطي: q = N/A +/- 32*M/(pi*D^3)؛ صالح فقط عند e <= D/8.",
                if (threadedArea != null) "مساحة القلاووظ A_t=${Fmt.n(threadedArea * 1e6, 3)} mm2; sigma_thread=T_max/A_t=${Fmt.n(tMax / threadedArea / 1e6, 3)} MPa (قوة المجموعة المرنة وليست قبول الأنكر)." else "مساحة القلاووظ غير مدخلة؛ لا توجد نتيجة إجهاد شد القلاووظ.",
                "الهندسة: A = π/4 × D^2 = π/4 × ${Fmt.n(d, 4)}^2 = ${Fmt.n(a, 6)} m2   |   V = Q/A = ${Fmt.n(q, 6)} / ${Fmt.n(a, 6)} = ${Fmt.n(v, 4)} m/s",
                "قوة الدفع لحالة التشغيل لكل فرع: Fp_op = P_pump × A = ${Fmt.n(pPump, 1)} × ${Fmt.n(a, 6)} = ${Fmt.n(fpOp, 1)} N   |   Fm = ρ × Q × V = ${Fmt.n(rhoW, 1)} × ${Fmt.n(q, 6)} × ${Fmt.n(v, 4)} = ${Fmt.n(fm, 1)} N",
                "F_op = ${Fmt.n(fpOp, 1)} + ${Fmt.n(fm, 1)} = ${Fmt.n(fOp, 1)} N   →   R_op = √2 × F_op = ${Fmt.n(rOp, 1)} N",
                "قوة الدفع لحالة الضغط التصميمي (استاتيكي): Fp_des = ${Fmt.n(pDes, 1)} × ${Fmt.n(a, 6)} = ${Fmt.n(fpDes, 1)} N   →   R_des = √2 × Fp_des = ${Fmt.n(rDes, 1)} N",
                "الحالة الحاكمة: R_gov = max(${Fmt.n(rOp / 1000.0, 1)}, ${Fmt.n(rDes / 1000.0, 1)}, ${Fmt.n((rTest ?: 0.0) / 1000.0, 1)}) = ${Fmt.n(rGov / 1000.0, 1)} kN   |   Rh = Rv = R_gov / √2 = ${Fmt.n(rh / 1000.0, 1)} kN",
                "الأوزان: W_pipe = π × (D + t_pipe) × t_pipe × L × γ_s = ${Fmt.n(wPipe, 2)} kg   |   W_water = A × L × ρ_w = ${Fmt.n(wWater, 2)} kg   |   W_elbow = ${Fmt.n(wElbow, 1)} kg",
                "N_total = Rv + (W_pipe + W_water + W_elbow) × 9.81 = ${Fmt.n(rv / 1000.0, 1)} kN + ${Fmt.n(dead, 2)} × 9.80665 / 1000 = ${Fmt.n(nTotal / 1000.0, 1)} kN   |   M = H_total × H = ${Fmt.n(moment / 1000.0, 1)} kN.m",
                "بالتة القاعدة: q = N_total / A_plate = ${Fmt.n(bearing / 1000.0, 2)} kPa   |   c = (D_plate - (D + 2*t_elbow))/2 = ${Fmt.n(cantilever * 1000.0, 1)} mm   |   M_plate = q × c^2 / 2 = ${Fmt.n(mPlate / 1000.0, 2)} kN.m/m",
                "σ_allow = Fy / FOS = ${Fmt.n(fy / 1e6, 1)} / ${Fmt.n(fos, 3)} = ${Fmt.n(sigmaAllow / 1e6, 1)} MPa   →   t_plate_req = √(6 × M_plate / σ_allow) = ${Fmt.n(tPlateReq * 1000.0, 2)} mm",
                "الأعصاب: F_rib = N_total / n_ribs = ${Fmt.n(fRib, 1)} N   |   M_rib = F_rib × c = ${Fmt.n(mRib / 1000.0, 2)} kN.m   →   t_rib_req = 6 × M_rib / (σ_allow × h_rib^2) = ${Fmt.n(tRibReq * 1000.0, 2)} mm",
                "البراغي: T_max = 4 × M / (n_bolts × BCD) = ${Fmt.n(tMax, 1)} N   |   V_bolt = H_total / n_bolts = ${Fmt.n(vBolt, 1)} N   →   d_bolt_req = √(4 × T_max / (π × σ_bolt)) = ${Fmt.n(dBoltReq * 1000.0, 2)} mm",
                "تفاعل البرغي = √((σ_t/σ_bolt)^2 + (τ/(0.6 × σ_bolt))^2) = √((${Fmt.n(sigmaTAct / 1e6, 1)}/${Fmt.n(sigmaBolt / 1e6, 1)})^2 + (${Fmt.n(tauAct / 1e6, 1)}/${Fmt.n(0.6 * sigmaBolt / 1e6, 1)})^2) = ${Fmt.n(interaction, 3)}",
                "استيعاب البالتة: edge_min = 2 × d_bolt = ${Fmt.n(edgeMin * 1000.0, 1)} mm   |   D_plate_min = BCD + 2 × edge_sel = ${Fmt.n(dPlateMin * 1000.0, 1)} mm",
                "مقاسات الكوع للرسم: t_elbow = ${Fmt.n(tElbow * 1000.0, 1)} mm, D = ${Fmt.n(d * 1000.0, 1)} mm, L = ${Fmt.n(l, 3)} m, h_rib = ${Fmt.n(hRib * 1000.0, 1)} mm, n_ribs = ${Fmt.n(nRibs, 0)}, n_bolts = ${Fmt.n(nBolts, 0)}.",
            ),
            warningsAr = buildList {
                if (!fullContact) add("نموذج التلامس الكامل للوحة الدائرية الصلبة يتوقع رفعًا (e > D_plate/8). الضغط المنتظم وتقديرات البالتة والأعصاب لا تثبت الكفاية؛ يلزم حل التلامس الجزئي وقوى الأنكر معًا.")
                if (threadedArea == null) add("مساحة شد القلاووظ غير مدخلة. إجهاد مساحة الساق نظري فقط؛ أدخل A_t الجدولية لتقدير إجهاد القلاووظ.")
                add("نموذج مبدئي فقط: لا يصدر حكم أمان إنشائي. الضغط المنتظم يهمل الانقلاب، وتوزيع حمل الأعصاب ومجموعة البراغي افتراضات غير متحققة. إجهاد البرغي محسوب بمساحة الساق لا مساحة شد القلاووظ؛ القطر المصمت المكافئ ليس مقاس برغي للاختيار. يلزم فحص التلامس والرفع والقلاووظ واللحام والأنكر والخرسانة.")
                if (!plateOk) add("سماكة بالتة القاعدة غير كافية: المختار ${Fmt.n(tPlateSel * 1000.0, 1)} mm أقل من المطلوب ${Fmt.n(tPlateReq * 1000.0, 2)} mm - زِد سماكة البالتة.")
                if (!ribOk) add("سماكة العصب غير كافية: المختار ${Fmt.n(tRibSel * 1000.0, 1)} mm أقل من المطلوب ${Fmt.n(tRibReq * 1000.0, 2)} mm - زِد السماكة أو عدد الأعصاب.")
                if (!boltOk) add("براغي التثبيت غير كافية: معامل التفاعل ${Fmt.n(interaction, 3)} أكبر من 1.0 - زِد قطر البرغي أو عددهم أو قطر دائرة التوزيع.")
                if (!plateDiaOk) add("قطر البالتة غير كافٍ لدائرة البراغي: ${Fmt.n(dPlate * 1000.0, 1)} mm مقابل ${Fmt.n(dPlateMin * 1000.0, 1)} mm مطلوبة - زِد قطر البالتة أو قلل دائرة التوزيع.")
                if (kotlin.math.abs(nRibs - Math.round(nRibs).toDouble()) > 1e-9) add("عدد الأعصاب ليس عددًا صحيحًا (${Fmt.n(nRibs, 3)}) - توزيع الحمل على الأعصاب لا معنى له إلا بعدد صحيح.")
                if (kotlin.math.abs(nBolts - Math.round(nBolts).toDouble()) > 1e-9) add("عدد البراغي ليس عددًا صحيحًا (${Fmt.n(nBolts, 3)}) - توزيع الحمل على البراغي لا معنى له إلا بعدد صحيح.")
                if (tPlateSel < tPlateReq * 1.05) add("هامش سماكة البالتة أقل من 5% - زيادة صغيرة في الحمل ستُفشل هذا الفحص؛ أكّد المقاسات على السماكة التجارية التالية.")
                add("الأقطار D = ${Fmt.n(d * 1000.0, 1)} mm مفترض تساويها، وهو ما يجعل المحصلة √2 من القوة لفرع واحد. و H = ${Fmt.n(h, 3)} m و W_elbow = ${Fmt.n(wElbow, 1)} kg تقديرية: أكّدها من الرسم التصنيعي والمورد قبل التنفيذ.")
                add("الحالة الحاكمة كانت " + (if (rTest != null && rGov == rTest) "ضغط الاختبار الهيدروستاتيكي" else if (rGov == rDes) "الضغط التصميمي (استاتيكي)" else "التشغيل (ضغط المضخة مع الدفع الديناميكي)") + "عند ${Fmt.n(rGov / 1000.0, 1)} kN. الجاذبية مأخوذة 9.80665 m/s2، نفس الثابت في باقي المحرك.")
                add("مساعدة تصميم مبدئية: يجب مراجعة النتيجة واعتمادها من مهندس إنشائي/ميكانيكي مرخّص. غير مغطى هنا: تثبيت الخرسانة (ACI 318-25 الفصل 17 - الانفصال والقشط والانحشار والقص الجانبي)، وانحناء اللوحة عند مجموعة البراغي (AISC Design Guide 1 وهو غالبًا الفحص الحاكم للوحة)، وقص وانحشار الأعصاب، وأحمال الرياح والزلازل، والاحتكاك كمسار قص - وكلها قد تحكم.")
                add("يُفضَّل إدخال ضغط تشغيل المضخة كأقصى ضغط يمكن أن تنتجه (ضغط الغلق أو ضبط صمام الأمان، gauge) لا ضغط نقطة التشغيل المقننة.")
            },
            warnings = warnings,
        )
    }
}
