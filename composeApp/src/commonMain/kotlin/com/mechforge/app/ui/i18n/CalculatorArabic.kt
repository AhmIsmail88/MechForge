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
        "power-torque-rpm" to CalculatorArabicContent(
            name = "القدرة والعزم والسرعة",
            description = "العلاقة بين القدرة والعزم وسرعة الدوران - أدخل أي اثنين ويُحسب الثالث.",
            inputs = mapOf(
                "p" to "القدرة",
                "t" to "العزم",
                "n" to "سرعة الدوران",
            ),
            results = mapOf(
                "p" to "القدرة",
                "t" to "العزم",
                "n" to "السرعة",
            ),
        ),

        "torsional-stress" to CalculatorArabicContent(
            name = "إجهاد اللي (الفتل)",
            description = "إجهاد القص من العزم، مع أقل قطر عمود مصمت آمن مقابل الإجهاد المسموح.",
            inputs = mapOf(
                "t" to "العزم",
                "d" to "قطر العمود",
                "taual" to "إجهاد القص المسموح",
            ),
            results = mapOf(
                "tau" to "إجهاد القص اللي",
                "dmin" to "أقل قطر لعمود مصمت",
                "util" to "النسبة إلى الإجهاد المسموح",
            ),
        ),

        "bolt-torque" to CalculatorArabicContent(
            name = "عزم ربط البراغي",
            description = "الشد المسبق وعزم الربط من قطر البرغي ودرجته ومعامل الصامولة (ISO 898-1).",
            inputs = mapOf(
                "d" to "القطر الاسمي للبرغي",
                "pitch" to "خطوة القلاووظ (لحساب مساحة الشد)",
                "class" to "درجة البرغي (إجهاد الإثبات)",
                "k" to "معامل الصامولة K",
                "at" to "مساحة شد البرغي",
                "f" to "الشد المسبق (قوة الشد)",
                "t" to "عزم الربط",
                "preloadpct" to "نسبة الشد المسبق المستهدفة (من حمل الإثبات)",
            ),
            results = mapOf(
                "at" to "مساحة الشد",
                "f" to "الشد المسبق",
                "fRec" to "الشد المسبق التصميمي",
                "t" to "عزم الربط",
                "tRec" to "عزم الشد التصميمي",
                "sigma" to "إجهاد شد البرغي",
                "util" to "نسبة الشد إلى حمل الإثبات",
            ),
        ),

        "bearing-l10" to CalculatorArabicContent(
            name = "عمر الرولمان (L10 / Lnm)",
            description = "العمر الأساسي والمُعدَّل للرولمان بالدورات وبالساعات وفق ISO 281.",
            inputs = mapOf(
                "c" to "حمل التحميل الديناميكي",
                "p" to "الحمل الديناميكي المكافئ",
                "n" to "سرعة الدوران",
                "exp" to "أُس العمر p",
                "rel" to "الموثوقية (معامل a1)",
                "aiso" to "معامل تعديل العمر a_ISO",
            ),
            results = mapOf(
                "l10" to "العمر الأساسي L10 (دورات)",
                "l10h" to "العمر الأساسي بالساعات L10h",
                "lnm" to "العمر المُعدَّل Lnm (دورات)",
                "lnmh" to "العمر المُعدَّل بالساعات Lnmh",
                "a1" to "معامل الموثوقية a1",
                "aisoUsed" to "معامل التعديل a_ISO المستخدم",
            ),
        ),

        "spring-rate" to CalculatorArabicContent(
            name = "معدل الياي (الصلابة)",
            description = "معدل الياي والانحراف من قطر السلك والقطر المتوسط وعدد اللفات.",
            inputs = mapOf(
                "d" to "قطر السلك",
                "dm" to "القطر المتوسط للفة",
                "n" to "عدد اللفات الفعّالة",
                "g" to "معامل القص",
                "f" to "القوة المؤثرة (اختياري)",
            ),
            results = mapOf(
                "k" to "معدل الياي",
                "kmm" to "المعدل لكل مم",
                "delta" to "الانحراف",
            ),
        ),

        "beam-ss-udl" to CalculatorArabicContent(
            name = "كمرة بسيطة بحمل موزّع",
            description = "الانحراف والعزم والإجهاد الأقصى لكمرة بسيطة تحت حمل موزّع منتظم.",
            inputs = mapOf(
                "w" to "الحمل الموزّع",
                "l" to "البحر (الامتداد)",
                "e" to "معامل المرونة",
                "i" to "عزم القصور الذاتي",
                "z" to "معامل المقطع (اختياري)",
            ),
            results = mapOf(
                "defl" to "أقصى انحراف",
                "moment" to "أقصى عزم انحناء",
                "sigma" to "أقصى إجهاد انحناء",
                "ratio" to "الانحراف إلى البحر",
            ),
        ),

        "beam-cantilever-point" to CalculatorArabicContent(
            name = "كمرة كابولية بحمل مركّز",
            description = "انحراف الطرف والعزم والإجهاد الأقصى لكمرة كابولية تحت حمل مركّز.",
            inputs = mapOf(
                "p" to "الحمل المركّز",
                "l" to "الطول",
                "e" to "معامل المرونة",
                "i" to "عزم القصور الذاتي",
                "z" to "معامل المقطع (اختياري)",
            ),
            results = mapOf(
                "defl" to "انحراف الطرف",
                "moment" to "أقصى عزم عند التثبيت",
                "sigma" to "أقصى إجهاد انحناء",
                "ratio" to "الانحراف إلى الطول",
            ),
        ),

        "pipe-wall-thickness" to CalculatorArabicContent(
            name = "سماكة جدار الماسورة (ASME B31.3)",
            description = "سماكة الجدار المطلوبة للضغط، مع بدل التآكل وتفاوت الطاحونة.",
            inputs = mapOf(
                "p" to "ضغط التصميم",
                "d" to "القطر الخارجي",
                "sigma" to "الإجهاد المسموح عند حرارة التصميم",
                "e" to "معامل جودة اللحام",
                "y" to "المعامل Y (جدول 304.1.1)",
                "ca" to "بدل التآكل",
                "mill" to "تفاوت الطاحونة",
            ),
            results = mapOf(
                "tp" to "سماكة التصميم للضغط",
                "t" to "السماكة الدنيا مع بدل التآكل",
                "tNom" to "السماكة الاسمية المطلوبة",
                "odRatio" to "نسبة القطر إلى السماكة",
            ),
        ),

        "pipe-weight" to CalculatorArabicContent(
            name = "وزن المواسير",
            description = "وزن الماسورة والمحتوى لكل متر، مع الوزن التشغيلي الكلي.",
            inputs = mapOf(
                "od" to "القطر الخارجي",
                "t" to "سماكة الجدار",
                "rho" to "كثافة المادة",
                "rhoc" to "كثافة المحتوى (للوزن التشغيلي)",
            ),
            results = mapOf(
                "w" to "الوزن لكل متر",
                "wc" to "وزن المحتوى لكل متر",
                "wtot" to "الوزن التشغيلي الكلي لكل متر",
                "area" to "مساحة مقطع المعدن",
                "id" to "القطر الداخلي",
            ),
        ),

        "thermal-expansion" to CalculatorArabicContent(
            name = "التمدد الحراري",
            description = "التمدد الحراري الحر لطول ماسورة عند فرق حرارة.",
            inputs = mapOf(
                "alpha" to "معامل التمدد",
                "l" to "طول الماسورة",
                "dt" to "فرق الحرارة",
            ),
            results = mapOf(
                "dl" to "التمدد الحر",
                "perM" to "التمدد لكل متر",
            ),
        ),
        "fire-pump-head" to CalculatorArabicContent(
            name = "رفع مضخة الحريق",
            description = "الرفع الكلي المطلوب لمضخة الحريق من الضغط المطلوب والمتاح وفرق الارتفاع والفقد.",
            inputs = mapOf(
                "preq" to "ضغط التصريف المطلوب",
                "pavail" to "ضغط الإمداد المتاح",
                "hstatic" to "فرق الارتفاع الاستاتيكي",
                "hf" to "فقد الاحتكاك والفقد الفرعي",
                "rho" to "كثافة المياه",
            ),
            results = mapOf(
                "h" to "الرفع الكلي للمضخة",
                "dp" to "فرق الضغط",
                "phead" to "مساهمة ضغط الرأس",
            ),
        ),

        "fire-pump-power" to CalculatorArabicContent(
            name = "قدرة مضخة الحريق",
            description = "القدرة الهيدروليكية وقدرة العمود وقدرة المحرك الموصى بها لمضخة الحريق.",
            inputs = mapOf(
                "q" to "معدل السريان",
                "h" to "الرفع الكلي",
                "eta" to "كفاءة المضخة",
                "rho" to "كثافة المياه",
            ),
            results = mapOf(
                "hydraulic" to "القدرة الهيدروليكية",
                "shaft" to "قدرة العمود",
                "motor" to "قدرة المحرك الموصى بها",
            ),
        ),

        "sprinkler-discharge" to CalculatorArabicContent(
            name = "تصرف الرشاش (معامل K)",
            description = "تصرف الرشاش من معامل K والضغط عنده.",
            inputs = mapOf(
                "k" to "معامل K للرشاش",
                "p" to "الضغط عند الرشاش",
            ),
            results = mapOf(
                "q" to "التصرف (لتر/دقيقة)",
                "qM3h" to "التصرف (م³/ساعة)",
                "qGpm" to "التصرف (إمبريال)",
            ),
        ),

        "fm200-agent-quantity" to CalculatorArabicContent(
            name = "كمية عامل الإطفاء FM-200",
            description = "كمية FM-200 من الحجم المحمي وتصنيف الخطر ودرجة التصميم (NFPA 2001).",
            inputs = mapOf(
                "v" to "الحجم المحمي الصافي (الإجمالي ناقص المستثنى)",
                "vgross" to "حجم الغرفة الإجمالي (للمراجعة)",
                "vexcl" to "الحجم المستثنى",
                "hazard" to "تصنيف الخطر (يحدد تركيز التصميم)",
                "c" to "تركيز التصميم (تجاوز يدوي)",
                "t" to "حرارة التصميم",
                "s" to "الحجم النوعي للبخار (NFPA 2001 / قيم مُدرجة)",
                "addkg" to "كمية إضافية للظروف الخاصة",
                "mcyl" to "شحنة الأسطوانة (قيم مُدرجة)",
            ),
            results = mapOf(
                "w" to "الكمية النهائية المطلوبة",
                "wbasic" to "الكمية الأساسية",
                "wadd" to "الكمية الإضافية",
                "wLb" to "الكمية النهائية (إمبريال)",
                "cUsed" to "تركيز التصميم المستخدم",
                "vnet" to "الحجم الصافي المستخدم",
                "sUsed" to "الحجم النوعي المستخدم",
                "vapourVolume" to "حجم بخار العامل عند حرارة التصميم",
                "f" to "معامل الغمر المكافئ",
                "fLb" to "معامل الغمر المكافئ (إمبريال)",
                "cylinders" to "عدد الأسطوانات المطلوبة",
                "installed" to "سعة العامل المُركّبة",
                "margin" to "هامش السعة (كمية)",
                "marginPct" to "هامش السعة (نسبة)",
            ),
        ),

        "co2-agent-quantity" to CalculatorArabicContent(
            name = "كمية ثاني أكسيد الكربون CO₂",
            description = "كمية CO₂ من الحجم المحمي وتصنيف الخطر ومعامل الغمر، مع عدد الأسطوانات القياسية (NFPA 12).",
            inputs = mapOf(
                "v" to "الحجم المحمي الصافي (الإجمالي ناقص المستثنى)",
                "vgross" to "حجم الغرفة الإجمالي (للمراجعة)",
                "vexcl" to "الحجم المستثنى",
                "hazard" to "تصنيف الخطر (يحدد تركيز التصميم)",
                "c" to "تركيز التصميم (تجاوز يدوي)",
                "t" to "حرارة التصميم",
                "ftable" to "معامل الغمر من جدول NFPA 12 أو بيانات مُدرجة",
                "addkg" to "كمية إضافية للفتحات غير القابلة للغلق",
                "mcyl" to "شحنة الأسطوانة (45 كجم قياسي)",
            ),
            results = mapOf(
                "w" to "الكمية النهائية المطلوبة",
                "wbasic" to "الكمية الأساسية",
                "wadd" to "الكمية الإضافية",
                "wLb" to "الكمية النهائية (إمبريال)",
                "cUsed" to "تركيز التصميم المستخدم",
                "f" to "معامل الغمر المستخدم",
                "fIdeal" to "معامل الغمر المكافئ للغاز المثالي",
                "fLb" to "معامل الغمر المستخدم (إمبريال)",
                "vnet" to "الحجم الصافي المستخدم",
                "rhoVapour" to "كثافة بخار CO₂ عند حرارة التصميم",
                "cylinders" to "عدد الأسطوانات المطلوبة",
                "installed" to "سعة CO₂ المُركّبة",
                "margin" to "هامش السعة (كمية)",
                "marginPct" to "هامش السعة (نسبة)",
            ),
        ),

        "tank-volume" to CalculatorArabicContent(
            name = "حجم الخزان أو الحوض",
            description = "الحجم المطلوب من معدل السريان وزمن المكث.",
            inputs = mapOf(
                "q" to "معدل السريان",
                "t" to "زمن المكث",
            ),
            results = mapOf(
                "v" to "الحجم المطلوب",
                "vl" to "الحجم المطلوب (لتر)",
            ),
        ),

        "detention-time" to CalculatorArabicContent(
            name = "زمن المكث",
            description = "زمن مكث المياه من حجم الخزان ومعدل السريان.",
            inputs = mapOf(
                "v" to "حجم الخزان أو الحوض",
                "q" to "معدل السريان عبره",
            ),
            results = mapOf(
                "t" to "زمن المكث",
                "tm" to "زمن المكث (دقائق)",
            ),
        ),

        "chlorine-dose" to CalculatorArabicContent(
            name = "الجرعة الكلورية",
            description = "معدل إضافة المادة الكيميائية من تدفق المياه والجرعة المستهدفة.",
            inputs = mapOf(
                "q" to "تدفق المياه",
                "dose" to "الجرعة المستهدفة",
            ),
            results = mapOf(
                "mh" to "معدل إضافة المادة (كجم/ساعة)",
                "mr" to "معدل إضافة المادة",
            ),
        ),

        "peak-flow" to CalculatorArabicContent(
            name = "تدفق الذروة",
            description = "تدفق الذروة من متوسط التدفق ومعامل الذروة.",
            inputs = mapOf(
                "q" to "متوسط التدفق",
                "pf" to "معامل الذروة",
            ),
            results = mapOf(
                "qp" to "تدفق الذروة",
                "qpd" to "تدفق الذروة (م³/يوم)",
            ),
        ),

        "hydraulic-loading" to CalculatorArabicContent(
            name = "التحميل الهيدروليكي",
            description = "معدل التحميل الهيدروليكي من التدفق ومساحة السطح.",
            inputs = mapOf(
                "q" to "معدل السريان",
                "a" to "مساحة السطح",
            ),
            results = mapOf(
                "hlr" to "معدل التحميل الهيدروليكي",
                "hlrh" to "معدل التحميل (م/ساعة)",
            ),
        ),

        "water-hammer" to CalculatorArabicContent(
            name = "المطرقة المائية (Joukowsky)",
            description = "ارتفاع الضغط المفاجئ وزمن الغلق الحرج من سرعة الموجة وتغير السرعة.",
            inputs = mapOf(
                "rho" to "كثافة السائل",
                "c" to "سرعة موجة الضغط (اتركه فارغًا لحسابها)",
                "dv" to "تغير السرعة",
                "d" to "القطر الداخلي للماسورة",
                "t" to "سماكة جدار الماسورة",
                "kbulk" to "معامل مرونة السائل K",
                "epipe" to "معامل مرونة الماسورة E",
                "l" to "طول الماسورة (اختياري)",
            ),
            results = mapOf(
                "dp" to "ارتفاع الضغط (Joukowsky)",
                "dpMpa" to "ارتفاع الضغط (ميجاباسكال)",
                "head" to "ضغط الصدم (رفع)",
                "cUsed" to "سرعة الموجة المستخدمة",
                "tc" to "زمن الغلق الحرج",
            ),
        ),

        "valve-kv" to CalculatorArabicContent(
            name = "معامل الصمام Kv / Cv",
            description = "التصرف عبر الصمام من معامل التصرف وفرق الضغط والكثافة النوعية.",
            inputs = mapOf(
                "kv" to "معامل التصرف Kv",
                "dp" to "فرق الضغط على الصمام",
                "sg" to "الكثافة النوعية (مياه = 1)",
            ),
            results = mapOf(
                "q" to "معدل السريان",
                "dp" to "فرق الضغط",
                "cv" to "المكافئ Cv (أمريكي)",
            ),
        ),

        "equivalent-length" to CalculatorArabicContent(
            name = "الطول المكافئ",
            description = "الطول المكافئ للوصلات والصمامات من معامل الفقد والقطر ومعامل الاحتكاك.",
            inputs = mapOf(
                "k" to "معامل الفقد",
                "d" to "القطر الداخلي",
                "f" to "معامل الاحتكاك",
            ),
            results = mapOf(
                "leq" to "الطول المكافئ",
                "inD" to "الطول المكافئ بالأقطار",
            ),
        ),
        "carnot-efficiency" to CalculatorArabicContent(
            name = "كفاءة كارنو",
            description = "أقصى كفاءة نظرية لمحرك حراري بين مخزنين حراريين.",
            inputs = mapOf(
                "th" to "حرارة المخزن الساخن",
                "tc" to "حرارة المخزن البارد",
            ),
            results = mapOf(
                "eta" to "كفاءة كارنو",
                "ratio" to "الكفاءة (كسر)",
            ),
        ),

        "compression-ratio" to CalculatorArabicContent(
            name = "نسبة الضغط (متعدد المراحل)",
            description = "نسبة الضغط الكلية وعدد المراحل المتساوية المطلوبة عند حد أقصى لكل مرحلة.",
            inputs = mapOf(
                "p1" to "ضغط الدخول",
                "p2" to "ضغط التصريف",
                "crmax" to "أقصى نسبة لكل مرحلة",
            ),
            results = mapOf(
                "cr" to "نسبة الضغط الكلية",
                "n" to "عدد المراحل المطلوبة",
                "crStage" to "النسبة لكل مرحلة",
                "pint" to "ضغط ما بين المرحلتين",
            ),
        ),

        "compressor-power" to CalculatorArabicContent(
            name = "قدرة الضاغط",
            description = "القدرة المثالية والفعلية وقدرة العمود من ظروف الدخول والخروج.",
            inputs = mapOf(
                "m" to "معدل الكتلة",
                "t1" to "حرارة الدخول",
                "p1" to "ضغط الدخول",
                "p2" to "ضغط التصريف",
                "cp" to "الحرارة النوعية",
                "k" to "نسبة الحرارة النوعية",
                "eta" to "الكفاءة الأيزنتروبية",
            ),
            results = mapOf(
                "pideal" to "القدرة المثالية (أيزنتروبية)",
                "pshaft" to "قدرة العمود",
                "t2s" to "حرارة التصريف الأيزنتروبية",
                "t2a" to "حرارة التصريف الفعلية",
                "t2ac" to "حرارة التصريف الفعلية (سلزيوس)",
                "ratio" to "نسبة الضغط",
            ),
        ),

        "gear-ratio" to CalculatorArabicContent(
            name = "نسبة التروس",
            description = "نسبة التروس وسرعة الخرج وعزمه من عدد أسنان الترسين.",
            inputs = mapOf(
                "z1" to "أسنان الترس القائد",
                "z2" to "أسنان الترس المقتاد",
                "n1" to "سرعة الترس القائد (اختياري)",
                "t1" to "عزم الترس القائد (اختياري)",
                "m" to "الموديول (اختياري)",
            ),
            results = mapOf(
                "i" to "نسبة التروس",
                "n2" to "سرعة الخرج",
                "t2" to "عزم الخرج (مثالي)",
                "d1" to "قطر الترس الصغير",
                "d2" to "قطر الترس الكبير",
            ),
        ),

        "heat-exchanger-duty" to CalculatorArabicContent(
            name = "حمل المبادل الحراري",
            description = "الحمل الحراري والفرق الحراري اللوغاريتمي من التدفق والحرارات.",
            inputs = mapOf(
                "m" to "معدل الكتلة",
                "cp" to "الحرارة النوعية",
                "tin" to "حرارة الدخول",
                "tout" to "حرارة الخروج",
                "u" to "معامل الانتقال الكلي U (اختياري)",
                "a" to "مساحة الانتقال A (اختياري)",
                "thin" to "حرارة الساخن الداخلة (اختياري)",
                "thout" to "حرارة الساخن الخارجة (اختياري)",
                "tcin" to "حرارة البارد الداخلة (اختياري)",
                "tcout" to "حرارة البارد الخارجة (اختياري)",
                "arr" to "الترتيب (1 = متعاكس، 0 = متوازي)",
            ),
            results = mapOf(
                "q" to "الحمل الحراري (جانب السائل)",
                "qArea" to "الحمل الحراري (جانب المساحة)",
                "qBtuh" to "الحمل الحراري (إمبريال)",
                "lmtd" to "الفرق الحراري اللوغاريتمي",
                "dt" to "فرق الحرارة",
            ),
        ),

        "hx-effectiveness-ntu" to CalculatorArabicContent(
            name = "فعالية المبادل (ε-NTU)",
            description = "فعالية المبادل من عدد وحدات الانتقال ونسبة السعات والترتيب.",
            inputs = mapOf(
                "ntu" to "عدد وحدات الانتقال NTU",
                "cr" to "نسبة السعات",
                "arr" to "الترتيب (1 = متعاكس، 0 = متوازي)",
            ),
            results = mapOf(
                "eps" to "الفعالية",
                "epsFrac" to "الفعالية (كسر)",
                "ntuUsed" to "عدد الوحدات المستخدم",
                "crUsed" to "نسبة السعات المستخدمة",
            ),
        ),

        "ideal-gas" to CalculatorArabicContent(
            name = "الغاز المثالي",
            description = "الكثافة والكتلة والحجم النوعي من معادلة الغاز المثالي.",
            inputs = mapOf(
                "p" to "الضغط المطلق",
                "t" to "الحرارة المطلقة",
                "m" to "الكتلة المولية",
                "v" to "الحجم (اختياري)",
            ),
            results = mapOf(
                "rho" to "كثافة الغاز",
                "mass" to "كتلة الغاز",
                "sv" to "الحجم النوعي",
            ),
        ),

        "isentropic-relation" to CalculatorArabicContent(
            name = "العلاقات الأيزنتروبية",
            description = "الحرارة ونسب الضغط والكثافة والحرارة في عملية أيزنتروبية.",
            inputs = mapOf(
                "p1" to "ضغط الدخول",
                "p2" to "ضغط الخروج",
                "t1" to "حرارة الدخول",
                "k" to "نسبة الحرارة النوعية",
            ),
            results = mapOf(
                "t2" to "حرارة الخروج",
                "t2c" to "حرارة الخروج (سلزيوس)",
                "pratio" to "نسبة الضغط P₂/P₁",
                "tratio" to "نسبة الحرارة T₂/T₁",
                "dratio" to "نسبة الكثافة ρ₂/ρ₁",
            ),
        ),

        "lmtd" to CalculatorArabicContent(
            name = "الفرق الحراري اللوغاريتمي (LMTD)",
            description = "الفرق الحراري اللوغاريتمي المتوسط وحمل المبادل من الحرارات الأربع.",
            inputs = mapOf(
                "thin" to "حرارة الساخن الداخلة",
                "thout" to "حرارة الساخن الخارجة",
                "tcin" to "حرارة البارد الداخلة",
                "tcout" to "حرارة البارد الخارجة",
                "arr" to "الترتيب (1 = متعاكس، 0 = متوازي)",
                "u" to "معامل الانتقال الكلي U (اختياري)",
                "a" to "مساحة الانتقال A (اختياري)",
            ),
            results = mapOf(
                "lmtd" to "الفرق الحراري اللوغاريتمي",
                "dt1" to "الفرق الطرفي الأول",
                "dt2" to "الفرق الطرفي الثاني",
                "q" to "الحمل الحراري",
            ),
        ),

        "pipe-sizing" to CalculatorArabicContent(
            name = "تحديد قطر الماسورة",
            description = "القطر الداخلي المطلوب من معدل السريان والسرعة التصميمية.",
            inputs = mapOf(
                "q" to "معدل السريان",
                "v" to "السرعة التصميمية",
            ),
            results = mapOf(
                "d" to "القطر الداخلي المطلوب",
                "area" to "مساحة السريان",
            ),
        ),

        "thermal-efficiency" to CalculatorArabicContent(
            name = "الكفاءة الحرارية",
            description = "الكفاءة الحرارية والحرارة المطرودة من الشغل والحرارة الداخلة.",
            inputs = mapOf(
                "w" to "الشغل الصافي الخارج",
                "q" to "الحرارة الداخلة",
            ),
            results = mapOf(
                "eta" to "الكفاءة الحرارية",
                "ratio" to "الكفاءة (كسر)",
                "rejected" to "الحرارة المطرودة",
            ),
        ),
        "pressure-converter" to CalculatorArabicContent(
            name = "محوّل الضغط",
            description = "تحويل قيمة ضغط إلى كل وحدات الضغط المدعومة.",
            inputs = mapOf(
                "v" to "القيمة",
            ),
        ),

        "flow-converter" to CalculatorArabicContent(
            name = "محوّل التدفق",
            description = "تحويل قيمة تدفق إلى كل وحدات التدفق المدعومة.",
            inputs = mapOf(
                "v" to "القيمة",
            ),
        ),

        "power-converter" to CalculatorArabicContent(
            name = "محوّل القدرة",
            description = "تحويل قيمة قدرة إلى كل وحدات القدرة المدعومة.",
            inputs = mapOf(
                "v" to "القيمة",
            ),
        ),

        "length-converter" to CalculatorArabicContent(
            name = "محوّل الأطوال",
            description = "تحويل قيمة طول إلى كل وحدات الطول المدعومة.",
            inputs = mapOf(
                "v" to "القيمة",
            ),
        ),

        "temperature-converter" to CalculatorArabicContent(
            name = "محوّل درجات الحرارة",
            description = "تحويل قيمة حرارة إلى كل وحدات الحرارة المدعومة.",
            inputs = mapOf(
                "v" to "القيمة",
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
