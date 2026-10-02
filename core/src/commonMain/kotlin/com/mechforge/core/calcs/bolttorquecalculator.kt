package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputError
import com.mechforge.core.engine.InputOption
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.engine.ValidationException
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt
import kotlin.math.PI

/**
 * Bolt tightening torque / preload.
 *
 *     T = K * F * d                 (nut-factor relation)
 *     A_t = PI/4 * (d - 0.9382*p)^2 (tensile stress area of a metric thread)
 *     F_rec = preload% * S_p * A_t  (design preload from the property class)
 *
 * The proof strengths of the ISO 898-1 / ISO 3506 property classes are the standard
 * published values; the preload percentage and the nut factor stay user inputs because
 * they depend on the joint and the lubrication.
 */
/**
 * Proof strength S_p in MPa, per ISO 898-1 (and ISO 3506 for the stainless grades).
 *
 * These are the PROOF strengths, which is the quantity the preload rule uses. They were
 * previously taken from the 0.2 % yield stresses of the same classes, which are 10 to 13 %
 * higher and therefore not conservative - the targeted tension, and with it the 90 % and
 * 40 % limits, moved with them. Class 8.8 depends on the diameter: 580 MPa up to M16 and
 * 600 MPa above M16, applied in the calculation below.
 */
private val PropertyClasses: List<Pair<InputOption, Double>> = listOf(
    InputOption("4.6", "4.6 - low carbon steel") to 225.0,
    InputOption("5.8", "5.8 - low carbon steel, higher strength") to 380.0,
    InputOption("6.8", "6.8 - low carbon steel, quenched and tempered") to 440.0,
    InputOption("8.8", "8.8 - quenched and tempered steel (580 up to M16, 600 above)") to 580.0,
    InputOption("9.8", "9.8 - quenched and tempered steel, lower proof") to 650.0,
    InputOption("10.9", "10.9 - alloy steel") to 830.0,
    InputOption("12.9", "12.9 - alloy steel, higher strength") to 970.0,
    InputOption("a2a4", "A2 / A4-70 - austenitic stainless (ISO 3506)") to 450.0,
)

private val Def = CalculatorDefinition(
    id = "bolt-torque",
    name = "Bolt Torque, Preload & Proof Load",
    category = CalculatorCategory.MECHANICAL_DESIGN,
    description = "Tightening torque from the preload (or preload from torque), and the design preload for a bolt property class from its tensile stress area and proof strength.",
    formulaDisplay = "T = K*F*d ;  A_t = PI/4*(d - 0.9382*p)^2 ;  F_rec = preload% * S_p * A_t",
    reference = "Torque-preload relation T = K*F*d (Shigley; VDI 2230) with the metric tensile stress area formula; proof strengths of ISO 898-1 / ISO 3506 property classes.",
    notes = "Proof strengths follow ISO 898-1 (and ISO 3506 for stainless). K = 0.20 is typical for as-received steel bolts, lightly oiled; zinc-plated or otherwise coated fasteners commonly run 0.25-0.35, lubrication, coatings and washers change K a lot, so verify it for critical joints. A_t is the tensile stress area (use the pitch field and it is calculated, or enter the tabulated value). The design preload is normally 65-75% of the proof load (up to 90% for permanent, well-controlled joints). Torque scatter is typically +/-25-35%, so safety-critical joints are verified by torque plus angle, or by bolt elongation.",
    keywords = listOf("bolt", "torque", "preload", "tension", "fastener", "stress area", "proof load", "property class", "iso 898", "vdi 2230"),
    inputs = listOf(
        InputSpec("k", "Nut factor K", "K", UnitFamily.DIMENSIONLESS, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "dash", defaultValue = 0.20, assumedWhenOmitted = "Nut factor K assumed as 0.20 (dry, as-received steel) - plated or lubricated fasteners need a different K."),
        InputSpec("d", "Nominal bolt diameter", "d", UnitFamily.LENGTH, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec("f", "Preload (bolt tension)", "F", UnitFamily.FORCE, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kn"),
        InputSpec("t", "Tightening torque", "T", UnitFamily.TORQUE, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "nm"),
        InputSpec("at", "Bolt tensile stress area", "A_t", UnitFamily.AREA, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm2"),
        InputSpec("pitch", "Thread pitch (to calculate A_t)", "p", UnitFamily.LENGTH, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "mm"),
        InputSpec(
            "class", "Bolt property class (proof strength)", "class", UnitFamily.DIMENSIONLESS,
            required = false, allowedUnitIds = listOf("dash"), defaultUnitId = "dash",
            options = PropertyClasses.map { it.first },
        ),
        InputSpec(
            "preloadpct", "Target preload (% of proof load)", "preload%", UnitFamily.DIMENSIONLESS,
            required = false, minValue = 0.0, exclusiveMin = true, maxValue = 1.0, exclusiveMax = true,
            allowedUnitIds = listOf("pct"), defaultUnitId = "pct", defaultValue = 65.0,
        ),
    ),
)

object BoltTorqueCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val k = optionalValue(inputs, "k", 0.20)
        val d = value(inputs, "d")
        val hasF = has(inputs, "f")
        val hasT = has(inputs, "t")
        val hasClass = has(inputs, "class")
        val hasPitch = has(inputs, "pitch")
        val hasArea = has(inputs, "at")

        val stressArea: Double? = when {
            hasArea -> value(inputs, "at")
            hasPitch -> {
                val pitch = value(inputs, "pitch")
                val minor = d - 0.9382 * pitch
                if (minor <= 0.0) throw ValidationException(listOf(InputError("pitch", "Thread pitch is too large for this diameter.")))
                PI / 4.0 * minor * minor
            }
            else -> null
        }

        if (hasClass && stressArea == null) {
            throw ValidationException(
                listOf(InputError("at", "Give the tensile stress area A_t or the thread pitch so the preload can be calculated.")),
            )
        }
        if ((hasF && hasT) || (!hasF && !hasT && !(hasClass && stressArea != null))) {
            throw ValidationException(
                listOf(
                    InputError(
                        "f",
                        "Enter EITHER the preload (F) OR the tightening torque (T) - or give the property class with the stress area or pitch to get a design preload.",
                    ),
                ),
            )
        }
        val propertyClass = if (hasClass) PropertyClasses[value(inputs, "class").toInt()] else null
        val preloadPct = optionalValue(inputs, "preloadpct", 0.65)
        // ISO 898-1: the proof strength of class 8.8 is 580 MPa up to M16 and 600 MPa above it.
        var proofStress = propertyClass?.second // MPa
        if (propertyClass?.first?.id == "8.8" && d > 0.016) proofStress = 600.0

        val results = mutableListOf<com.mechforge.core.engine.ResultValue>()
        val steps = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val stepsAr = mutableListOf<String>()
        val warningsAr = mutableListOf<String>()

        warnings += if (!has(inputs, "k")) {
            "Nut factor not provided - assumed 0.20 (as-received steel, lightly oiled)."
        } else {
            "K depends strongly on lubrication, plating and washers; verify it for critical joints."
        }
        warningsAr += if (!has(inputs, "k")) {
            "لم يُدخل معامل الصامولة - افتُرض 0.20 (صلب كما هو، مزيّت قليلًا)."
        } else {
            "K تعتمد بشدة على التزييت والطلاء والوردات؛ تحقق منها للوصلات الحرجة."
        }

        var force: Double? = null
        if (hasF) force = value(inputs, "f")
        if (hasT) force = value(inputs, "t") / (k * d)

        if (force != null) {
            val torque = k * force * d
            results += result("t", "Tightening Torque", torque, "nm", isPrimary = true)
            results += result("f", "Preload (Bolt Tension)", force / 1000.0, "kn", isPrimary = true)
            steps += "Nut factor K = ${Fmt.n(k, 3)}, diameter d = ${Fmt.n(d * 1000.0, 2)} mm"
            stepsAr += "معامل الصامولة K = ${Fmt.n(k, 3)}، القطر d = ${Fmt.n(d * 1000.0, 2)} mm"
            steps += if (hasF) {
                "Torque: T = K*F*d = ${Fmt.n(k, 3)} x ${Fmt.n(force / 1000.0, 2)} kN x ${Fmt.n(d * 1000.0, 2)} mm = ${Fmt.n(torque, 2)} N.m"
            } else {
                "Preload: F = T/(K*d) = ${Fmt.n(torque, 2)} / (${Fmt.n(k, 3)} x ${Fmt.n(d * 1000.0, 2)} mm) = ${Fmt.n(force / 1000.0, 2)} kN"
            }
            stepsAr += if (hasF) {
                "العزم: T = K·F·d = ${Fmt.n(k, 3)} × ${Fmt.n(force / 1000.0, 2)} kN × ${Fmt.n(d * 1000.0, 2)} mm = ${Fmt.n(torque, 2)} N·m"
            } else {
                "الشد المسبق: F = T/(K·d) = ${Fmt.n(torque, 2)} / (${Fmt.n(k, 3)} × ${Fmt.n(d * 1000.0, 2)} mm) = ${Fmt.n(force / 1000.0, 2)} kN"
            }
            stressArea?.let { at ->
                val sigma = force / at
                results += result("sigma", "Bolt Tensile Stress", sigma / 1e6, "mpa")
                steps += "Tensile stress: sigma = F/A_t = ${Fmt.n(force / 1000.0, 2)} kN / ${Fmt.n(at * 1e6, 2)} mm2 = ${Fmt.n(sigma / 1e6, 1)} MPa"
                stepsAr += "إجهاد الشد: σ = F/A_t = ${Fmt.n(force / 1000.0, 2)} kN / ${Fmt.n(at * 1e6, 2)} mm2 = ${Fmt.n(sigma / 1e6, 1)} MPa"
                if (proofStress != null) {
                    val utilization = force / (proofStress * 1e6 * at)
                    results += result("util", "Preload / Proof Load", utilization * 100.0, "pct")
                    steps += "Utilisation of the proof load: ${Fmt.n(utilization * 100.0, 1)} %"
                    stepsAr += "نسبة الاستغلال من حمل الإثبات: ${Fmt.n(utilization * 100.0, 1)} %"
                    if (utilization > 0.90) {
                        warnings += "Preload is above 90 % of the proof load - the bolt is at risk of yielding during tightening."
                        warningsAr += "الشد المسبق أعلى من 90 % من حمل الإثبات - البرغي معرض للخضوع أثناء الربط."
                    } else if (utilization < 0.40) {
                        warnings += "Preload is below 40 % of the proof load - the joint may loosen or the bolt may fatigue."
                        warningsAr += "الشد المسبق أقل من 40 % من حمل الإثبات - الوصلة قد ترتخي أو يتعب البرغي."
                    }
                }
            }
        }

        if (proofStress != null && stressArea != null) {
            val fRec = preloadPct * proofStress * 1e6 * stressArea
            val tRec = k * fRec * d
            results += result("at", "Tensile Stress Area", stressArea * 1e6, "mm2")
            results += result("fRec", "Design Preload (${Fmt.n(preloadPct * 100.0, 0)} % of proof load)", fRec / 1000.0, "kn", isPrimary = force == null)
            results += result("tRec", "Torque for the Design Preload", tRec, "nm", isPrimary = force == null)
            steps += "Property class ${propertyClass!!.first.id}: proof strength S_p = ${Fmt.n(proofStress, 0)} MPa"
            stepsAr += "درجة البرغي ${propertyClass!!.first.id}: إجهاد الإثبات S_p = ${Fmt.n(proofStress, 0)} MPa"
            steps += "Tensile stress area: A_t = PI/4*(d - 0.9382*p)^2 = ${Fmt.n(stressArea * 1e6, 2)} mm2"
            stepsAr += "مساحة الشد: A_t = π/4×(d − 0.9382×p)² = ${Fmt.n(stressArea * 1e6, 2)} mm2"
            steps += "Design preload: F = ${Fmt.n(preloadPct * 100.0, 0)} % x ${Fmt.n(proofStress, 0)} MPa x ${Fmt.n(stressArea * 1e6, 2)} mm2 = ${Fmt.n(fRec / 1000.0, 2)} kN"
            stepsAr += "الشد المسبق التصميمي: F = ${Fmt.n(preloadPct * 100.0, 0)} % × ${Fmt.n(proofStress, 0)} MPa × ${Fmt.n(stressArea * 1e6, 2)} mm2 = ${Fmt.n(fRec / 1000.0, 2)} kN"
            steps += "Torque for that preload: T = K*F*d = ${Fmt.n(tRec, 2)} N.m"
            stepsAr += "العزم لهذا الشد المسبق: T = K·F·d = ${Fmt.n(tRec, 2)} N·m"
            warnings += "Check the bolt group, the joint stiffness and the required clamp force for the actual connection - this is the single-bolt preload, not the joint design."
            warningsAr += "راجع مجموعة البراغي وجساءة الوصلة وقوة الضغط المطلوبة للوصلة الفعلية - هذا الشد المسبق لبرغي واحد وليس تصميم الوصلة."
        }

        return CalcOutput(results = results, steps = steps, stepsAr = stepsAr, warnings = warnings, warningsAr = warningsAr)
    }
}
