package com.mechforge.core.calcs

import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorDefinition
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputOption
import com.mechforge.core.engine.InputSpec
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.util.Fmt
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.pow

/**
 * Carbon dioxide (CO2) total flooding agent quantity - NFPA 12 design route.
 *
 *     W_basic = max(V_net * f, W_min)
 *     W_final = W_basic + W_additional          (additional entered, never a blanket %)
 *     cylinders = ceil(W_final / cylinder charge)
 *
 * The flooding factor f is the NFPA 12 design data. The calculator therefore:
 *
 *  1. cross-checks the net protected volume against the gross and excluded volumes;
 *  2. takes f from the NFPA 12 table / listed value when the engineer enters it, and
 *     otherwise shows the ideal-gas equivalent (f = rho_vapour * ln[100/(100-C)]) clearly
 *     labelled as a theoretical estimate - not as a table value;
 *  3. keeps the basic quantity, the additional quantity (unclosable openings and other
 *     applicable corrections) and the final quantity as separate numbers;
 *  4. never adds a leakage, piping or reserve percentage, and never inflates the
 *     cylinder count: piping/network design is a separate engineered calculation.
 *
 * Nothing here claims NFPA 12 compliance: the hazard classification, the design
 * concentration, the flooding factor, the edition in force and the listed system
 * configuration remain the engineer's responsibility.
 */
private const val R_GAS = 8.31446261815324 // J/(mol*K)
private const val P_STD = 101325.0 // Pa
private const val M_CO2 = 0.04401 // kg/mol
private const val STANDARD_CYLINDER_CHARGE = 45.0 // kg

/** Hazard -> design concentration (% by volume) for CO2 total flooding. */
private val HazardClasses: List<Pair<InputOption, Double>> = listOf(
    InputOption("a", "Class A - surface fire, solid combustibles (34 %)") to 34.0,
    InputOption("b", "Class B - flammable liquids (34 % is a starting point; the design concentration is fuel-specific)") to 34.0,
    InputOption("c", "Class C - energized electrical, surface fire (34 %)") to 34.0,
    InputOption("ds", "Class A - deep-seated (smouldering) fire (50 %)") to 50.0,
    InputOption("dse", "Deep-seated dry electrical hazard (50 %)") to 50.0,
)

private val Def = CalculatorDefinition(
    id = "co2-agent-quantity",
    name = "CO2 Total Flooding Quantity (NFPA 12)",
    category = CalculatorCategory.FIRE_PROTECTION,
    description = "CO2 total flooding system in the NFPA 12 order: net protected volume, hazard classification, design concentration, the applicable flooding factor, the basic quantity, separately entered additional quantity, and the cylinder selection.",
    formulaDisplay = "W_basic = max(V_net * f, W_min) ; W_final = W_basic + W_additional (f: NFPA 12 table/listed factor; theoretical comparison only: f = rho_vapour * ln[100/(100-C)])",
    reference = "NFPA 12 total flooding: agent mass = net enclosure volume x flooding factor, with the flooding factor and design concentration taken from the standard (table/listed data) for the hazard classification. State the edition in force in the report.",
    notes = "Step 1: net protected volume = gross volume - excluded (non-protected) volume; enter the gross volume too and the calculator cross-checks the net value and prints the equivalent room size, which catches a misplaced decimal point. Step 2-3: the hazard classification sets the design concentration (editable) - 34 % for surface fires, 50 % for deep-seated and for deep-seated dry electrical hazards. Step 4: the flooding factor is the NFPA 12 design data - enter the applicable table/listed value in the f field; if it is left empty the ideal-gas equivalent is shown and labelled as a theoretical estimate. Step 5-6: W_basic = V_net x f, plus an additional quantity entered for the applicable special conditions (unclosable openings and similar); no leakage, piping or reserve percentage is ever added automatically.",
    keywords = listOf("co2", "carbon dioxide", "total flooding", "nfpa 12", "fire", "suppression", "flooding factor", "gas", "hazard class", "deep seated", "deep-seated dry electrical", "cylinder", "unclosable openings"),
    inputs = listOf(
        InputSpec("v", "Net protected volume (gross - excluded)", "V_net", UnitFamily.VOLUME, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3"),
        InputSpec("vgross", "Gross room volume (for the volume check)", "V_gross", UnitFamily.VOLUME, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3"),
        InputSpec("vexcl", "Excluded / non-protected volume", "V_excluded", UnitFamily.VOLUME, required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "m3"),
        InputSpec(
            "hazard", "Hazard classification (sets the design concentration)", "Hazard", UnitFamily.DIMENSIONLESS,
            allowedUnitIds = listOf("dash"), defaultUnitId = "dash", defaultValue = 0.0,
            options = HazardClasses.map { it.first },
        ),
        InputSpec(
            "c", "Design concentration (override)", "C", UnitFamily.DIMENSIONLESS,
            required = false, minValue = 0.0, exclusiveMin = true, maxValue = 1.0, exclusiveMax = true,
            allowedUnitIds = listOf("pct"), defaultUnitId = "pct",
        ),
        InputSpec("t", "Design temperature", "T", UnitFamily.TEMPERATURE, minValue = 0.0, exclusiveMin = true, defaultUnitId = "c", defaultValue = 21.0),
        InputSpec(
            "ftable", "Flooding factor from the NFPA 12 table / listed data", "f", UnitFamily.DENSITY,
            required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kgm3",
        ),
        InputSpec("wmin", "Applicable minimum basic quantity (after hazard corrections)", "W_min", UnitFamily.MASS,
            required = false, minValue = 0.0, defaultUnitId = "kg"),
        InputSpec(
            "addkg", "Additional CO2 - unclosable openings / applicable corrections", "W_add", UnitFamily.MASS,
            required = false, minValue = 0.0, exclusiveMin = false, defaultUnitId = "kg", defaultValue = 0.0,
        ),
        InputSpec(
            "mcyl", "Cylinder charge (standard 45 kg)", "m_cyl", UnitFamily.MASS,
            required = false, minValue = 0.0, exclusiveMin = true, defaultUnitId = "kg",
            defaultValue = STANDARD_CYLINDER_CHARGE,
        assumedWhenOmitted = "Cylinder charge assumed as 45 kg - a common commercial size, not one defined by NFPA 12. Enter the cylinder charge listed by the manufacturer."),
    ),
)

object Co2AgentQuantityCalculator : Calculator(Def) {

    override fun calculate(inputs: Map<String, InputValue>): CalcOutput {
        val volume = value(inputs, "v") // m3
        val gross = if (has(inputs, "vgross")) value(inputs, "vgross") else null
        val hazard = HazardClasses[value(inputs, "hazard").toInt()]
        val concentrationPct = if (has(inputs, "c")) value(inputs, "c") * 100.0 else hazard.second
        val cFraction = concentrationPct / 100.0
        val temperatureK = value(inputs, "t") // K
        val charge = optionalValue(inputs, "mcyl", STANDARD_CYLINDER_CHARGE)
        val additional = optionalValue(inputs, "addkg", 0.0)
        val fEntered = has(inputs, "ftable")

        val vapourDensity = P_STD * M_CO2 / (R_GAS * temperatureK) // kg/m3
        // The theoretical gas quantity for a constant-volume vented enclosure follows the logarithmic dilution relation
        // V_gas/V_room = ln(100/(100-C)). The C/(100-C) form used before belongs to the clean agents of
        // NFPA 2001, not to CO2: at 65 % it is about 77 % higher, so the quantity it produced was not
        // conservative. That form is kept only to show how far the two are apart.
        val ratioLog = ln(100.0 / (100.0 - concentrationPct))
        val fTheory = vapourDensity * ratioLog // kg/m3 - theoretical, comparison only
        val fCleanAgentForm = vapourDensity * cFraction / (1.0 - cFraction) // kg/m3 - the old form
        val floodingFactor = if (fEntered) value(inputs, "ftable") else fTheory

        if (has(inputs, "wmin") && !fEntered) {
            throw com.mechforge.core.engine.ValidationException(listOf(com.mechforge.core.engine.InputError("ftable", "A table minimum requires an entered flooding factor on the same hazard/correction basis.")))
        }
        val minimum = optionalValue(inputs, "wmin", 0.0)
        val basic = maxOf(volume * floodingFactor, minimum) // kg
        val final = basic + additional // kg
        val finalLb = final / 0.45359237
        val cylinders = ceil(final / charge)
        val installed = cylinders * charge

        val excludedFromGross = if (gross != null) gross - volume else null
        val netFromGross = if (gross != null && has(inputs, "vexcl")) gross - value(inputs, "vexcl") else null
        val side = volume.pow(1.0 / 3.0)
        val consistentVolumes = (gross == null || volume <= gross) &&
            (netFromGross == null || abs(netFromGross - volume) <= 0.02 * volume)
        val selectionReady = fEntered && has(inputs, "mcyl") && consistentVolumes

        val results = buildList {
            add(result("vnet", "Net protected volume used", volume, "m3"))
            if (gross != null) add(result("vexcl", "Excluded volume (gross - net)", (excludedFromGross ?: 0.0).coerceAtLeast(0.0), "m3"))
            add(result("cUsed", "Design concentration used", concentrationPct, "pct"))
            add(
                result(
                    "f",
                    if (fEntered) {
                        "Flooding factor used (entered)"
                    } else {
                        "Theoretical flooding factor (comparison only - enter f from NFPA 12)"
                    },
                    floodingFactor, "kgm3", isPrimary = fEntered,
                ),
            )
            add(result("fIdeal", "Clean-agent form rho*C/(100-C), shown for comparison - NOT the CO2 relation", fCleanAgentForm, "kgm3"))
            add(result("fLb", "Flooding factor used (imperial)", floodingFactor / 16.0184634, "lbft3"))
            add(result("rhoVapour", "CO2 vapour density at design T", vapourDensity, "kgm3"))
            add(
                result(
                    "wbasic",
                    if (fEntered) "Basic CO2 quantity W_basic" else "Theoretical CO2 quantity (comparison only)",
                    basic, "kg",
                ),
            )
            add(result("wadd", "Additional quantity W_add", additional, "kg"))
            add(
                result(
                    "w",
                    if (fEntered) {
                        "Preliminary final quantity W_final"
                    } else {
                        "Theoretical final quantity (NOT a design value - enter f from NFPA 12)"
                    },
                    final, "kg", isPrimary = fEntered,
                ),
            )
            add(result("wLb", if (fEntered) "Preliminary final quantity (imperial)" else "Theoretical final quantity (imperial)", finalLb, "lb", isPrimary = fEntered))
            if (selectionReady) {
            add(result("cylinders", "Preliminary cylinders (${Fmt.n(charge, 1)} kg each)", cylinders, "dash"))
            add(result("installed", "Installed CO2 capacity", installed, "kg"))
            add(result("margin", "Capacity margin", installed - final, "kg"))
            add(result("marginPct", "Capacity margin", (installed - final) / final * 100.0, "pct"))
            }
        }

        val steps = buildList {
            add("Step 1  Volumes: net V = ${Fmt.n(volume, 3)} m3" +
                (if (gross != null) "   gross = ${Fmt.n(gross, 3)} m3   excluded = ${Fmt.n((excludedFromGross ?: 0.0).coerceAtLeast(0.0), 3)} m3" else "") +
                "   (equivalent room size about ${Fmt.n(side, 2)} x ${Fmt.n(side, 2)} x ${Fmt.n(side, 2)} m)")
            add("Step 2  Agent: carbon dioxide (CO2), M = ${Fmt.n(M_CO2, 5)} kg/mol, design temperature ${Fmt.n(temperatureK, 3)} K")
            add("Step 3  Hazard: ${hazard.first.label} -> design concentration C = ${Fmt.n(concentrationPct, 3)} %" +
                if (has(inputs, "c")) " (entered)" else " (from the hazard classification)")
            add(
                if (fEntered) {
                    "Step 4  Flooding factor f = ${Fmt.n(floodingFactor, 4)} kg/m3 (entered: NFPA 12 table / listed data). For reference, the theoretical constant-volume vented-mixing value is ${Fmt.n(fTheory, 4)} kg/m3 and the clean-agent form would be ${Fmt.n(fCleanAgentForm, 4)} kg/m3"
                } else {
                    "Step 4  Theoretical flooding factor = rho_vapour x ln(100/(100-C)) = ${Fmt.n(vapourDensity, 4)} x ${Fmt.n(ratioLog, 5)} = ${Fmt.n(floodingFactor, 4)} kg/m3 - comparison only. The NFPA 12 flooding factor comes from its table, with the volume factor depending on the enclosure size and the material factor on the hazard: enter it in the f field for a design quantity. (The clean-agent form rho*C/(100-C) would give ${Fmt.n(fCleanAgentForm, 4)} kg/m3, which is not the CO2 relation.)"
                },
            )
            add("Step 5  W_basic = max(V_net x f, W_min) = max(${Fmt.n(volume, 3)} x ${Fmt.n(floodingFactor, 4)}, ${Fmt.n(minimum, 3)}) = ${Fmt.n(basic, 2)} kg")
            add(
                if (additional <= 0.0) {
                    "Step 6  Additional CO2 quantity = 0 kg (no unclosable openings or other applicable corrections entered)"
                } else {
                    "Step 6  Additional CO2 quantity = ${Fmt.n(additional, 2)} kg (entered for the stated condition)"
                },
            )
            add("Step 7  W_final = W_basic + W_add = ${Fmt.n(basic, 2)} + ${Fmt.n(additional, 2)} = ${Fmt.n(final, 2)} kg")
            if (selectionReady) add("Step 8  Cylinders = CEILING(${Fmt.n(final, 2)} / ${Fmt.n(charge, 1)} kg) = ${Fmt.n(cylinders, 0)} x ${Fmt.n(charge, 1)} kg = ${Fmt.n(installed, 1)} kg installed, margin ${Fmt.n(installed - final, 2)} kg (${Fmt.n((installed - final) / final * 100.0, 1)} %)") else add("Step 8  Cylinder count requires the NFPA 12 factor and an entered listed cylinder charge; also verify table minimum quantities and all applicable corrections.")
            add("Step 9  Assumptions: NFPA 12 methodology; state the edition in force for the project (Settings > Report details > Code / edition), and confirm the design concentration and flooding factor with the standard and the system manufacturer.")
        }

        val stepsAr = buildList {
            add("خطوة 1  الأحجام: الحجم الصافي V = ${Fmt.n(volume, 3)} m3" +
                (if (gross != null) "   الإجمالي = ${Fmt.n(gross, 3)} m3   المستثنى = ${Fmt.n((excludedFromGross ?: 0.0).coerceAtLeast(0.0), 3)} m3" else "") +
                "   (مكافئ غرفة بحجم ${Fmt.n(side, 2)} × ${Fmt.n(side, 2)} × ${Fmt.n(side, 2)} م)")
            add("خطوة 2  العامل: ثاني أكسيد الكربون (CO2)، M = ${Fmt.n(M_CO2, 5)} kg/mol، حرارة التصميم ${Fmt.n(temperatureK, 3)} K")
            add("خطوة 3  الخطر: ${hazard.first.label} ← تركيز التصميم C = ${Fmt.n(concentrationPct, 3)} %" +
                if (has(inputs, "c")) " (مُدخل)" else " (من تصنيف الخطر)")
            add(
                if (fEntered) {
                    "خطوة 4  معامل الغمر f = ${Fmt.n(floodingFactor, 4)} kg/m3 (مُدخل: جدول NFPA 12 أو بيانات مُدرجة). وللمراجعة: القيمة النظرية لحيز ثابت الحجم مع تنفيس ${Fmt.n(fTheory, 4)} kg/m3 وصيغة الوكلاء النظيفين ${Fmt.n(fCleanAgentForm, 4)} kg/m3"
                } else {
                    "خطوة 4  معامل الغمر النظري = ρ_بخار × ln(100/(100−C)) = ${Fmt.n(vapourDensity, 4)} × ${Fmt.n(ratioLog, 5)} = ${Fmt.n(floodingFactor, 4)} kg/m3 - للمقارنة فقط. معامل الغمر في NFPA 12 يأتي من جدوله، بمعامل الحجم حسب حجم الحيز ومعامل المادة حسب الخطر: أدخله في خانة f للحصول على كمية تصميمية. (وصيغة الوكلاء النظيفين ρ×C/(100−C) تعطي ${Fmt.n(fCleanAgentForm, 4)} kg/m3 وهي ليست علاقة CO2.)"
                },
            )
            add("خطوة 5  W_basic = max(V_net × f, W_min) = max(${Fmt.n(volume, 3)} × ${Fmt.n(floodingFactor, 4)}, ${Fmt.n(minimum, 3)}) = ${Fmt.n(basic, 2)} kg")
            add(
                if (additional <= 0.0) {
                    "خطوة 6  كمية CO2 الإضافية = 0 kg (لا توجد فتحات غير قابلة للغلق أو تصحيحات مطبقة)"
                } else {
                    "خطوة 6  كمية CO2 الإضافية = ${Fmt.n(additional, 2)} kg (مُدخلة للظرف المذكور)"
                },
            )
            add("خطوة 7  W_final = W_basic + W_add = ${Fmt.n(basic, 2)} + ${Fmt.n(additional, 2)} = ${Fmt.n(final, 2)} kg")
            if (selectionReady) add("خطوة 8  الأسطوانات = CEILING(${Fmt.n(final, 2)} / ${Fmt.n(charge, 1)} kg) = ${Fmt.n(cylinders, 0)} × ${Fmt.n(charge, 1)} kg = ${Fmt.n(installed, 1)} kg مُركّبة، والهامش ${Fmt.n(installed - final, 2)} kg (${Fmt.n((installed - final) / final * 100.0, 1)} %)") else add("خطوة 8  عدد الأسطوانات يتطلب معامل NFPA 12 وشحنة أسطوانة مُدرجة مُدخلة؛ يلزم أيضًا مراجعة الكميات الدنيا بالجدول وكل التصحيحات المطبقة.")
            add("خطوة 9  الافتراضات: منهجية NFPA 12؛ اذكر الإصدار الساري للمشروع (الإعدادات > تفاصيل التقرير > الكود/الإصدار)، وأكّد تركيز التصميم ومعامل الغمر مع المعيار ومُصنّع النظام.")
        }

        val warnings = buildList {
            if (fEntered && !has(inputs, "wmin")) add("The applicable minimum table quantity was not entered; only V*f is used before additions. Verify the minimum on the same hazard/correction basis before selection.")
            add("CO2 design concentrations are life threatening: the applicable standard's requirements for occupant evacuation, alarms, warning signs, ventilation and lockout must be met before commissioning.")
            if (netFromGross != null && abs(netFromGross - volume) > 0.02 * volume) {
                add("Volume check: gross - excluded = ${Fmt.n(netFromGross, 3)} m3 does not match the entered net volume ${Fmt.n(volume, 3)} m3. Correct the net volume before using the result.")
            }
            if (gross != null && volume > gross) {
                add("Volume check: the net volume is larger than the gross room volume - check the decimal point (a 100x slip gives a 100x agent quantity).")
            }
            if (!fEntered) {
                add("No NFPA 12 flooding factor was entered: the quantity shown is the theoretical constant-volume vented-mixing value from rho*ln(100/(100-C)) and is a comparison, not a design value. The NFPA 12 flooding factor depends on the enclosure size (larger for smaller enclosures) and on the material - take it from the standard's table and enter it in the f field.")
                add("The clean-agent form rho*C/(100-C) would give ${Fmt.n(fCleanAgentForm, 4)} kg/m3 here. It belongs to NFPA 2001 and overstates CO2 at high concentrations, so it is shown only to make the difference visible.")
            }
            add("No additional allowance is added automatically: no leakage %, no piping %, no reserve. Any additional CO2 must come from the actual NFPA 12 provisions and the actual enclosure conditions (for example unclosable openings), entered separately - 0 kg when none apply.")
            add("The cylinder count comes from W_final and the cylinder charge; it is not inflated by an allowance. Piping/network design, discharge time, pressure relief and venting are separate engineered-system items.")
            add("Confirm the design concentration and the applicable flooding factor for the hazard with the NFPA 12 edition in force.")
            if (hazard.first.id == "b") {
                add("Class B: flammable-liquid design concentrations are fuel-specific. The 34 % figure is a starting point from the surface-fire value - the NFPA 12 table lists the minimum design concentration per fuel, and it is frequently much higher.")
            }
            if (concentrationPct > 5.0) {
                add("The design concentration (${Fmt.n(concentrationPct, 1)} %) is above the NOAEL of about 5 % for CO2: a CO2 total flooding system is lethal - the space must be evacuated, and alarms, discharge delay, pre-discharge warning and lock-out are life-safety requirements, not options.")
            }
        }

        val warningsAr = buildList {
            if (fEntered && !has(inputs, "wmin")) add("لم تُدخل كمية الجدول الدنيا؛ استُخدم V*f فقط قبل الإضافات. تحقق من الحد الأدنى على نفس أساس الخطر والتصحيحات قبل الاختيار.")
            add("تركيزات CO2 التصميمية مهددة للحياة: يجب استيفاء متطلبات المعيار المطبق لإخلاء الموجودين والإنذار ولوحات التحذير والتهوية والفصل قبل التشغيل.")
            if (netFromGross != null && abs(netFromGross - volume) > 0.02 * volume) {
                add("فحص الحجم: الإجمالي − المستثنى = ${Fmt.n(netFromGross, 3)} m3 لا يطابق الحجم الصافي المُدخل ${Fmt.n(volume, 3)} m3. صحّح الحجم الصافي قبل استخدام النتيجة.")
            }
            if (gross != null && volume > gross) {
                add("فحص الحجم: الحجم الصافي أكبر من حجم الغرفة الإجمالي - راجع الفاصلة العشرية (خطأ 100 مرة يعطي كمية عامل 100 مرة).")
            }
            if (!fEntered) {
                add("لم يُدخل معامل غمر من NFPA 12: الكمية المعروضة هي القيمة النظرية لحيز ثابت الحجم مع تنفيس من ρ×ln(100/(100−C)) وهي للمقارنة لا للتصميم. معامل الغمر في NFPA 12 يعتمد على حجم الحيز (أكبر للحيزات الصغيرة) وعلى المادة - خذه من جدول المعيار وأدخله في خانة f.")
                add("صيغة الوكلاء النظيفين ρ×C/(100−C) تعطي ${Fmt.n(fCleanAgentForm, 4)} kg/m3 هنا، وهي تنتمي إلى NFPA 2001 وتبالغ عند التركيزات العالية، لذا تُعرض فقط لإظهار الفرق.")
            }
            add("لا تُضاف أي بدلات تلقائيًا: لا نسبة تسريب ولا نسبة مواسير ولا احتياطي. أي كمية CO2 إضافية يجب أن تأتي من أحكام NFPA 12 الفعلية وظروف الحيز الفعلية (مثل الفتحات غير القابلة للغلق) وتُدخل منفصلة - 0 kg عند عدم وجودها.")
            add("عدد الأسطوانات ينتج من W_final وشحنة الأسطوانة؛ ولا يُضخَّم بأي بدل. تصميم المواسير والشبكة وزمن التصريف وتنفيس الضغط والتهوية بنود نظام هندسي منفصلة.")
            add("أكّد تركيز التصميم ومعامل الغمر المطبق للخطر من إصدار NFPA 12 الساري.")
            if (hazard.first.id == "b") {
                add("الفئة B: تركيزات السوائل القابلة للاشتعال تعتمد على الوقود. نسبة 34 % نقطة بداية من قيمة حرائق السطح - جدول NFPA 12 يذكر الحد الأدنى لكل وقود، وغالبًا أعلى بكثير.")
            }
            if (concentrationPct > 5.0) {
                add("تركيز التصميم (${Fmt.n(concentrationPct, 1)} %) أعلى من NOAEL لنحو 5 % لثاني أكسيد الكربون: نظام الإغراق بـ CO2 قاتل - يجب إخلاء المكان، وإنذارات ومهلة تصريف وتحذير مسبق وقفل التشغيل ليست خيارات بل متطلبات سلامة أرواح.")
            }
        }

        return CalcOutput(results = results, steps = steps, stepsAr = stepsAr, warnings = warnings, warningsAr = warningsAr)
    }
}
