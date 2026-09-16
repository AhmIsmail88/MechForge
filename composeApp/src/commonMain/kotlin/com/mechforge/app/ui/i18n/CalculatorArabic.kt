package com.mechforge.app.ui.i18n

/**
 * Arabic content for calculators.
 *
 * This is a translation layer over the engine, never a second source of engineering data: the
 * equations, the units, the validation and every number still come from the calculator itself.
 * Anything not translated here falls back to the engine's English text, so a calculator can be
 * translated at any time without touching the calculation.
 *
 * Keys are the calculator, input and result ids; `CalculatorArabicTest` checks that every key
 * exists in the registry, so a rename in the engine cannot leave a stale translation behind.
 */
data class CalculatorArabicContent(
    val name: String,
    val description: String = "",
    val inputs: Map<String, String> = emptyMap(),
    val results: Map<String, String> = emptyMap(),
)

object CalculatorArabic {

    val CONTENT: Map<String, CalculatorArabicContent> = mapOf(

        "air-changes-hour" to CalculatorArabicContent(
            name = "تدفق التهوية (سعة المروحة / ACH)",
            description = "تدفق الهواء المطلوب (سعة المروحة) من معدل تغيير الهواء وحجم الغرفة، أو العكس.",
            inputs = mapOf(
                "q" to "التدفق (سعة المروحة)",
                "vroom" to "حجم الغرفة",
                "ach" to "عدد مرات تغيير الهواء في الساعة",
                "fancap" to "سعة المروحة الواحدة",
                "nfans" to "عدد المراوح المختارة",
            ),
            results = mapOf(
                "q" to "التدفق المطلوب (سعة المروحة)",
                "qCfm" to "التدفق المطلوب (إمبريال)",
                "qLs" to "التدفق المطلوب (لتر/ث)",
                "ach" to "تغييرات الهواء في الساعة",
                "vroom" to "حجم الغرفة",
                "time" to "زمن تغيير الهواء",
                "fansNeeded" to "عدد المراوح المطلوب",
                "fanTotal" to "السعة المختارة للمراوح",
                "fanTotalCfm" to "السعة المختارة (إمبريال)",
                "fanMargin" to "هامش السعة المختارة",
            ),
        ),

        "heat-dissipation" to CalculatorArabicContent(
            name = "تدفق التهوية من الحرارة المُهدَرة",
            description = "تدفق الهواء (سعة المروحة) اللازم لتصريف حمل حراري محسوس عند فرق حرارة مسموح.",
            inputs = mapOf(
                "p" to "الحرارة المُهدَرة (حمل حراري محسوس)",
                "dt" to "فرق الحرارة المسموح للهواء",
                "rho" to "كثافة الهواء",
                "cp" to "الحرارة النوعية للهواء",
                "fancap" to "سعة المروحة الواحدة",
                "nfans" to "عدد المراوح المختارة",
            ),
            results = mapOf(
                "q" to "التدفق المطلوب (سعة المروحة)",
                "qCfm" to "التدفق المطلوب (إمبريال)",
                "qLs" to "التدفق المطلوب (لتر/ث)",
                "mdot" to "معدل كتلة الهواء",
                "densityUsed" to "كثافة الهواء المستخدمة",
                "cpUsed" to "الحرارة النوعية المستخدمة",
                "fansNeeded" to "عدد المراوح المطلوب",
                "fanTotal" to "السعة المختارة للمراوح",
                "fanTotalCfm" to "السعة المختارة (إمبريال)",
                "fanMargin" to "هامش السعة المختارة",
            ),
        ),

        "pump-power" to CalculatorArabicContent(
            name = "قدرة المضخة",
            description = "القدرة الهيدروليكية وقدرة العمود وقدرة المحرك القياسية الموصى بها.",
            inputs = mapOf(
                "q" to "معدل السريان",
                "h" to "الرفع الكلي",
                "eta" to "كفاءة المضخة",
                "rho" to "كثافة السائل",
            ),
            results = mapOf(
                "hydraulic" to "القدرة الهيدروليكية",
                "shaft" to "قدرة العمود",
                "motor" to "المحرك القياسي الموصى به",
            ),
        ),

        "pipe-velocity" to CalculatorArabicContent(
            name = "سرعة السريان في المواسير",
            description = "سرعة السريان من معدل التدفق والقطر الداخلي للماسورة.",
            inputs = mapOf(
                "q" to "معدل السريان",
                "d" to "القطر الداخلي",
            ),
            results = mapOf(
                "v" to "سرعة السريان",
            ),
        ),

        "airflow-converter" to CalculatorArabicContent(
            name = "محوّل تدفق الهواء",
            description = "تحويل تدفق الهواء بين الوحدات الشائعة.",
            inputs = mapOf(
                "q" to "تدفق الهواء",
            ),
            results = mapOf(
                "cfm" to "قدم مكعب/دقيقة",
                "lmin" to "لتر/دقيقة",
                "ls" to "لتر/ثانية",
                "m3h" to "متر مكعب/ساعة",
            ),
        ),

        "sensible-heat" to CalculatorArabicContent(
            name = "الحمل الحراري المحسوس",
            description = "الحمل الحراري المحسوس للهواء من معدل التدفق وفرق الحرارة.",
            inputs = mapOf(
                "q" to "تدفق الهواء",
                "tin" to "حرارة الهواء الداخل",
                "tout" to "حرارة الهواء الخارج",
                "rho" to "كثافة الهواء",
            ),
            results = mapOf(
                "qs" to "الحمل الحراري المحسوس",
                "qsBtuh" to "الحمل الحراري المحسوس (إمبريال)",
            ),
        ),
        "reynolds-number" to CalculatorArabicContent(
            name = "رقم رينولدز",
            description = "تحديد نظام السريان - صفحي أو انتقالي أو مضطرب - من السرعة والقطر واللزوجة الحركية.",
            inputs = mapOf(
                "v" to "السرعة",
                "d" to "القطر الداخلي",
                "nu" to "اللزوجة الحركية",
            ),
            results = mapOf(
                "re" to "رقم رينولدز",
            ),
        ),

        "darcy-weisbach" to CalculatorArabicContent(
            name = "فقد الاحتكاك (دارسي-فايسباخ)",
            description = "فقد الرفع بالاحتكاك في ماسورة من السرعة والقطر والطول ومعامل الاحتكاك.",
            inputs = mapOf(
                "v" to "السرعة",
                "d" to "القطر الداخلي",
                "l" to "طول الماسورة",
                "re" to "رقم رينولدز",
                "eps" to "الخشونة المطلقة",
                "f" to "معامل الاحتكاك",
            ),
            results = mapOf(
                "f" to "معامل الاحتكاك f",
                "hf" to "فقد الرفع بالاحتكاك",
            ),
        ),

        "friction-factor" to CalculatorArabicContent(
            name = "معامل الاحتكاك (كولبروك-وايت)",
            description = "معامل احتكاك دارسي بطريقة كولبروك-وايت من رقم رينولدز والخشونة النسبية.",
            inputs = mapOf(
                "re" to "رقم رينولدز",
                "eps" to "الخشونة المطلقة",
                "d" to "القطر الداخلي",
            ),
            results = mapOf(
                "f" to "معامل الاحتكاك",
                "reld" to "الخشونة النسبية ε/D",
            ),
        ),

        "minor-losses" to CalculatorArabicContent(
            name = "الفقد في الوصلات والصمامات",
            description = "فقد الرفع في الوصلات والصمامات من معامل الفقد والسرعة.",
            inputs = mapOf(
                "k" to "معامل الفقد K",
                "v" to "السرعة",
            ),
            results = mapOf(
                "hm" to "فقد الرفع الفرعي",
            ),
        ),

        "hazen-williams" to CalculatorArabicContent(
            name = "هازن-ويليامز (خطوط المياه)",
            description = "فقد الرفع والتصرف والسرعة في مواسير المياه بمعادلة هازن-ويليامز.",
            inputs = mapOf(
                "c" to "معامل هازن-ويليامز C",
                "d" to "القطر الداخلي",
                "s" to "الميل الهيدروليكي",
            ),
            results = mapOf(
                "gradient" to "الميل الهيدروليكي",
                "hf" to "فقد الرفع لكل 1000 م",
                "q" to "التصرف",
                "v" to "السرعة",
            ),
        ),

        "manning" to CalculatorArabicContent(
            name = "معادلة مانينج (قنوات مفتوحة)",
            description = "التصرف والسرعة في القنوات المفتوحة بمعادلة مانينج.",
            inputs = mapOf(
                "n" to "خشونة مانينج n",
                "r" to "نصف القطر الهيدروليكي",
                "s" to "ميل الطاقة",
                "a" to "مساحة السريان",
            ),
            results = mapOf(
                "q" to "التصرف",
                "qSi" to "التصرف (وحدات دولية)",
                "v" to "متوسط السرعة",
            ),
        ),

        "npsh-available" to CalculatorArabicContent(
            name = "NPSH المتاح",
            description = "صافي رفع الشفط الموجب المتاح من الضغط الجوي وضغط البخار وكثافة السائل وفقد خط الشفط.",
            inputs = mapOf(
                "patm" to "الضغط المطلق على السطح",
                "pv" to "ضغط تبخر السائل",
                "rho" to "كثافة السائل",
                "hs" to "رفع الشفط الاستاتيكي",
                "hf" to "فقد خط الشفط",
            ),
            results = mapOf(
                "npsha" to "NPSH المتاح",
                "phead" to "مساهمة ضغط الرأس",
            ),
        ),

        "pump-affinity-laws" to CalculatorArabicContent(
            name = "قوانين تشابه المضخات",
            description = "التدفق والرفع والقدرة عند سرعة جديدة أو قطر مروحة بعد التقليم.",
            inputs = mapOf(
                "q1" to "التدفق عند السرعة الأصلية",
                "h1" to "الرفع عند السرعة الأصلية",
                "p1" to "القدرة عند السرعة الأصلية",
                "n1" to "السرعة الأصلية",
                "n2" to "السرعة الجديدة",
                "d1" to "قطر المروحة الأصلي (اختياري)",
                "d2" to "قطر المروحة بعد التقليم (اختياري)",
            ),
            results = mapOf(
                "q2" to "التدفق عند السرعة الجديدة",
                "h2" to "الرفع عند السرعة الجديدة",
                "p2" to "القدرة عند السرعة الجديدة",
                "q2t" to "التدفق بعد التقليم",
                "h2t" to "الرفع بعد التقليم",
                "p2t" to "القدرة بعد التقليم",
            ),
        ),

        "hose-nozzle-flow" to CalculatorArabicContent(
            name = "تصرف خرطوم وفوهة الحريق",
            description = "تصرف الفوهة من قطرها وضغطها ومعامل التصرف.",
            inputs = mapOf(
                "d" to "قطر الفوهة",
                "p" to "ضغط الفوهة",
                "c" to "معامل تصرف الفوهة",
            ),
            results = mapOf(
                "q" to "تصرف الفوهة (إمبريال)",
                "qLmin" to "تصرف الفوهة (لتر/دقيقة)",
                "qM3h" to "تصرف الفوهة (م³/ساعة)",
            ),
        ),

        "orifice-flow" to CalculatorArabicContent(
            name = "تصرف الفتحة (الأوريفيس)",
            description = "التصرف عبر فتحة تحت فرق ضغط، مع سرعة النفث النظرية.",
            inputs = mapOf(
                "cd" to "معامل التصرف",
                "d" to "قطر الفتحة",
                "h" to "الفرق في الرفع",
            ),
            results = mapOf(
                "q" to "التصرف",
                "qSi" to "التصرف (وحدات دولية)",
                "v" to "سرعة النفث النظرية",
            ),
        ),
        "duct-velocity" to CalculatorArabicContent(
            name = "سرعة الهواء في الدكت",
            description = "سرعة الهواء من التدفق وأبعاد الدكت الدائري أو المستطيل.",
            inputs = mapOf(
                "q" to "التدفق",
                "d" to "قطر الدكت الدائري",
                "w" to "عرض الدكت المستطيل",
                "h" to "ارتفاع الدكت المستطيل",
            ),
            results = mapOf(
                "v" to "سرعة الهواء",
                "vFpm" to "سرعة الهواء (إمبريال)",
                "a" to "مساحة الدكت",
            ),
        ),

        "duct-sizing" to CalculatorArabicContent(
            name = "مقاس الدكت (طريقة السرعة)",
            description = "المقاس المطلوب للدكت من التدفق والسرعة المستهدفة ونسبة الأبعاد.",
            inputs = mapOf(
                "q" to "التدفق",
                "v" to "السرعة المستهدفة",
                "r" to "نسبة العرض للارتفاع W/H",
            ),
            results = mapOf(
                "a" to "مساحة الدكت المطلوبة",
                "deq" to "القطر الدائري المكافئ",
                "w" to "عرض الدكت المستطيل",
                "h" to "ارتفاع الدكت المستطيل",
            ),
        ),

        "duct-pressure-loss" to CalculatorArabicContent(
            name = "فقد الضغط بالاحتكاك في الدكت",
            description = "فقد الضغط بالاحتكاك في الدكت من السرعة والطول والأبعاد وخواص الهواء.",
            inputs = mapOf(
                "v" to "سرعة الهواء",
                "l" to "طول الدكت",
                "d" to "قطر الدكت الدائري",
                "w" to "عرض الدكت المستطيل",
                "h" to "ارتفاع الدكت المستطيل",
                "rho" to "كثافة الهواء",
                "nu" to "اللزوجة الحركية",
                "eps" to "الخشونة المطلقة",
            ),
            results = mapOf(
                "dp" to "فقد الضغط بالاحتكاك",
                "dpPerM" to "الفقد لكل متر",
                "f" to "معامل الاحتكاك",
                "re" to "رقم رينولدز",
                "dh" to "القطر الهيدروليكي",
            ),
        ),

        "fan-power" to CalculatorArabicContent(
            name = "قدرة المروحة",
            description = "القدرة الهوائية وقدرة العمود وقدرة المحرك القياسية الموصى بها.",
            inputs = mapOf(
                "q" to "التدفق",
                "dp" to "الضغط الكلي للمروحة",
                "etaf" to "كفاءة المروحة",
                "etad" to "كفاءة الإدارة أو النقل",
            ),
            results = mapOf(
                "pair" to "القدرة الهوائية",
                "pshaft" to "قدرة العمود",
                "pmotor" to "قدرة المحرك",
                "motor" to "المحرك القياسي الموصى به (IEC 60034)",
            ),
        ),

        "fan-laws" to CalculatorArabicContent(
            name = "قوانين المراوح",
            description = "التدفق والضغط والقدرة عند سرعة جديدة أو كثافة هواء مختلفة.",
            inputs = mapOf(
                "q1" to "التدفق عند السرعة الأصلية",
                "dp1" to "الضغط الكلي عند السرعة الأصلية",
                "p1" to "قدرة العمود عند السرعة الأصلية",
                "n1" to "السرعة الأصلية",
                "n2" to "السرعة الجديدة",
                "rho1" to "كثافة الهواء عند السرعة الأصلية (اختياري)",
                "rho2" to "كثافة الهواء عند السرعة الجديدة (اختياري)",
            ),
            results = mapOf(
                "q2" to "التدفق عند السرعة الجديدة",
                "dp2" to "الضغط الكلي عند السرعة الجديدة",
                "p2" to "قدرة العمود عند السرعة الجديدة",
                "rp" to "نسبة السرعة",
                "rd" to "نسبة الكثافة",
            ),
        ),

        "total-cooling-load" to CalculatorArabicContent(
            name = "حمل التبريد الكلي (جانب الهواء)",
            description = "الحمل المحسوس والكامن والإجمالي من التدفق وفرق الحرارة وفرق الرطوبة.",
            inputs = mapOf(
                "q" to "التدفق",
                "tin" to "حرارة الهواء الداخل (جاف)",
                "tout" to "حرارة الهواء الخارج (جاف)",
                "win" to "نسبة الرطوبة الداخلة",
                "wout" to "نسبة الرطوبة الخارجة",
                "rho" to "كثافة الهواء",
            ),
            results = mapOf(
                "qs" to "الحمل المحسوس",
                "ql" to "الحمل الكامن",
                "qt" to "حمل التبريد الكلي",
            ),
        ),

        "latent-heat" to CalculatorArabicContent(
            name = "الحمل الكامن للهواء",
            description = "الحمل الكامن من فرق نسبة الرطوبة ومعدل التدفق.",
            inputs = mapOf(
                "q" to "التدفق",
                "win" to "نسبة الرطوبة الداخلة",
                "wout" to "نسبة الرطوبة الخارجة",
                "rho" to "كثافة الهواء",
            ),
            results = mapOf(
                "ql" to "الحمل الكامن",
                "dw" to "فرق نسبة الرطوبة",
            ),
        ),

        "power-efficiency-converter" to CalculatorArabicContent(
            name = "محوّل القدرة وكفاءة التبريد",
            description = "تحويل بين kW والطن التبريدي مع حساب COP و EER.",
            inputs = mapOf(
                "p" to "قدرة التبريد أو الحرارة",
                "pelec" to "القدرة الكهربائية الداخلة",
            ),
            results = mapOf(
                "kw" to "القدرة",
                "tr" to "طن تبريد",
                "cop" to "معامل الأداء COP",
                "eer" to "معامل الكفاءة EER",
                "kwtr" to "kW لكل طن (كهربائي)",
            ),
        ),
    )

    fun name(calculatorId: String, fallback: String): String =
        CONTENT[calculatorId]?.name?.takeIf { it.isNotBlank() } ?: fallback

    fun description(calculatorId: String, fallback: String): String =
        CONTENT[calculatorId]?.description?.takeIf { it.isNotBlank() } ?: fallback

    fun inputLabel(calculatorId: String, inputId: String, fallback: String): String =
        CONTENT[calculatorId]?.inputs?.get(inputId)?.takeIf { it.isNotBlank() } ?: fallback

    fun resultLabel(calculatorId: String, resultId: String, fallback: String): String =
        CONTENT[calculatorId]?.results?.get(resultId)?.takeIf { it.isNotBlank() } ?: fallback

    /** True when this calculator has any Arabic content at all. */
    fun isTranslated(calculatorId: String): Boolean = CONTENT.containsKey(calculatorId)
}
