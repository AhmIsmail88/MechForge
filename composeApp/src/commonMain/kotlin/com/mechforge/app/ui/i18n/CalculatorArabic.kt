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
