package com.mechforge.app.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.mechforge.app.data.LanguageMode

/**
 * Minimal, dependency-free UI string layer (Part C2).
 *
 * Scope for this phase: the *UI chrome* only — screen titles, navigation, generic
 * buttons and field labels, dialogs, empty/error/loading states, Settings and About.
 * The 50 calculator definitions in `core` (name, description, formula, notes,
 * reference) are NOT translated yet: `core` is untouched in this task, and the
 * brief keeps their translation for a later phase. So a calculator screen reads as
 * Arabic chrome around English engineering content — deliberate, and reported.
 *
 * Deliberately not a resource framework: an interface with two objects, resolved
 * once per composition through [LocalStrings]. No new dependency, nothing to
 * generate, and a missing string is a compile error rather than a silent fallback.
 */
interface UiStrings {

    val language: LanguageMode

    /** True for the Arabic UI: mirroring is driven from [isRtl]. */
    val isRtl: Boolean

    // ---- App chrome ------------------------------------------------------
    val appName: String
    val appTagline: String
    val navHome: String
    val navConverter: String
    val navHistory: String
    val navFavorites: String
    val navProjects: String
    val navSettings: String
    val navAbout: String
    val openNavigation: String

    // ---- Generic actions -------------------------------------------------
    val back: String
    val cancel: String
    val save: String
    val create: String
    val rename: String
    val delete: String
    val calculate: String
    val reset: String
    val copy: String
    val exportTxt: String
    val retry: String
    val search: String
    val all: String
    val optionalSuffix: String
    val unit: String
    val value: String
    val title: String
    val untitled: String

    // ---- Shared states ---------------------------------------------------
    val stateLoading: String
    val stateErrorTitle: String
    val stateErrorBody: String

    // ---- Home -----------------------------------------------------------
    val homeTitle: String
    val homeSearchLabel: String
    val homeSearchPlaceholder: String
    val homeSectionFavorites: String
    val homeSectionRecent: String
    val homeSectionCategories: String
    val homeNoMatches: String
    val homeNoMatchesHint: String

    // ---- Favorites ------------------------------------------------------
    val favoritesTitle: String
    val favoriteAdd: String
    val favoriteRemove: String
    val favoritesEmpty: String
    val favoritesEmptyHint: String

    // ---- Calculator -----------------------------------------------------
    val calculatorInputs: String
    val calculatorResults: String
    val calculatorPrimaryResult: String
    val calculatorOtherResults: String
    val calculatorDetails: String
    val calculatorFormula: String
    val calculatorSteps: String
    val calculatorReference: String
    val calculatorNotes: String
    val calculatorWarnings: String
    val calculatorSaveToHistory: String
    val calculatorSavedToHistory: String
    val calculatorSaveDialogTitle: String
    val calculatorReportSaved: String
    val calculatorExportCancelled: String
    val calculatorBlockedHint: String
    val calculatorNoResults: String
    val errorInvalidNumber: String
    val errorCalculationFailed: String

    // ---- History --------------------------------------------------------
    val historyTitle: String
    val historyEmpty: String
    val historyEmptyHint: String
    val historyRenameTitle: String
    val historyDuplicate: String
    val historyDuplicateDescription: String
    val historyRenameDescription: String
    val historyDeleteDescription: String
    val historyOpenDescription: String
    val historyMissingCalculator: String

    // ---- Projects -------------------------------------------------------
    val projectsTitle: String
    val projectsEmpty: String
    val projectsEmptyHint: String
    val projectsNew: String
    val projectsRenameTitle: String
    val projectsNameLabel: String
    val projectsDescriptionLabel: String

    // ---- Settings -------------------------------------------------------
    val settingsTitle: String
    val settingsAppearance: String
    val settingsLanguage: String
    val settingsLanguagePhaseNote: String
    val themeSystem: String
    val themeLight: String
    val themeDark: String
    val languageSystem: String
    val languageArabic: String
    val languageEnglish: String
    val settingsPrivacyNote: String

    // ---- About ----------------------------------------------------------
    val aboutTitle: String
    val aboutVersion: String
    val aboutMissionTitle: String
    val aboutMissionBody: String
    val aboutLocalFirstTitle: String
    val aboutLocalFirstBody: String
    val aboutReferencesTitle: String
    val aboutReferencesBody: String

    // ---- Converter ------------------------------------------------------
    val converterTitle: String
    val converterValueLabel: String
    val converterEnterNumber: String
    val converterIncompatibleUnits: String
    val converterAllUnits: String

    /** "3 fields need attention" / "٣ حقول بحاجة إلى تصحيح". */
    fun blockedFields(count: Int): String

    /** "2 saved calculations". */
    fun savedCalculations(count: Int): String
}

/** Arabic UI. */
object ArStrings : UiStrings {

    override val language = LanguageMode.ARABIC
    override val isRtl = true

    override val appName = "MechForge"
    override val appTagline = "مجموعة أدوات الهندسة الميكانيكية"
    override val navHome = "الرئيسية"
    override val navConverter = "المحوّل"
    override val navHistory = "السجل"
    override val navFavorites = "المفضلة"
    override val navProjects = "المشاريع"
    override val navSettings = "الإعدادات"
    override val navAbout = "حول التطبيق"
    override val openNavigation = "فتح قائمة التنقل"

    override val back = "رجوع"
    override val cancel = "إلغاء"
    override val save = "حفظ"
    override val create = "إنشاء"
    override val rename = "إعادة تسمية"
    override val delete = "حذف"
    override val calculate = "احسب"
    override val reset = "تصفير"
    override val copy = "نسخ"
    override val exportTxt = "تصدير نصي"
    override val retry = "إعادة المحاولة"
    override val search = "بحث"
    override val all = "الكل"
    override val optionalSuffix = "اختياري"
    override val unit = "الوحدة"
    override val value = "القيمة"
    override val title = "العنوان"
    override val untitled = "بدون عنوان"

    override val stateLoading = "جارٍ التحميل…"
    override val stateErrorTitle = "تعذّر تحميل البيانات"
    override val stateErrorBody = "حدث خطأ أثناء قراءة البيانات المحلية."

    override val homeTitle = "الرئيسية"
    override val homeSearchLabel = "ابحث في الحاسبات"
    override val homeSearchPlaceholder = "الاسم أو الكلمة المفتاحية أو التصنيف"
    override val homeSectionFavorites = "المفضلة"
    override val homeSectionRecent = "الأحدث"
    override val homeSectionCategories = "التصنيفات"
    override val homeNoMatches = "لا توجد نتائج مطابقة"
    override val homeNoMatchesHint = "جرّب كلمة مفتاحية أخرى أو تصنيفاً مختلفاً."

    override val favoritesTitle = "المفضلة"
    override val favoriteAdd = "أضف إلى المفضلة"
    override val favoriteRemove = "أزل من المفضلة"
    override val favoritesEmpty = "لا توجد حاسبات في المفضلة"
    override val favoritesEmptyHint = "افتح أي حاسبة واضغط النجمة لإضافتها هنا."

    override val calculatorInputs = "المدخلات"
    override val calculatorResults = "النتائج"
    override val calculatorPrimaryResult = "النتيجة الأساسية"
    override val calculatorOtherResults = "نتائج أخرى"
    override val calculatorDetails = "الصيغة والخطوات والمرجع"
    override val calculatorFormula = "الصيغة"
    override val calculatorSteps = "الحل خطوة بخطوة"
    override val calculatorReference = "المرجع"
    override val calculatorNotes = "ملاحظات"
    override val calculatorWarnings = "تحذيرات"
    override val calculatorSaveToHistory = "حفظ في السجل"
    override val calculatorSavedToHistory = "تم الحفظ في السجل."
    override val calculatorSaveDialogTitle = "حفظ في السجل"
    override val calculatorReportSaved = "تم حفظ التقرير:"
    override val calculatorExportCancelled = "أُلغي التصدير."
    override val calculatorBlockedHint = "أكمل الحقول المطلوبة للحساب."
    override val calculatorNoResults = "لم تُرجع الحاسبة أي نتيجة."
    override val errorInvalidNumber = "أدخل رقماً صحيحاً."
    override val errorCalculationFailed = "تعذّر إتمام الحساب"

    override val historyTitle = "السجل"
    override val historyEmpty = "لا توجد حسابات بعد"
    override val historyEmptyHint = "افتح أي حاسبة واضغط احسب، وسيظهر الحساب هنا."
    override val historyRenameTitle = "إعادة تسمية"
    override val historyDuplicate = "نسخة"
    override val historyDuplicateDescription = "إنشاء نسخة من هذا الحساب"
    override val historyRenameDescription = "إعادة تسمية هذا الحساب"
    override val historyDeleteDescription = "حذف هذا الحساب"
    override val historyOpenDescription = "فتح هذا الحساب في الحاسبة"
    override val historyMissingCalculator = "هذه الحاسبة لم تعد متوفرة"

    override val projectsTitle = "المشاريع"
    override val projectsEmpty = "لا توجد مشاريع بعد"
    override val projectsEmptyHint = "أنشئ مشروعاً لتجميع الحسابات المحفوظة."
    override val projectsNew = "مشروع جديد"
    override val projectsRenameTitle = "إعادة تسمية المشروع"
    override val projectsNameLabel = "اسم المشروع"
    override val projectsDescriptionLabel = "الوصف"

    override val settingsTitle = "الإعدادات"
    override val settingsAppearance = "المظهر"
    override val settingsLanguage = "اللغة"
    override val settingsLanguagePhaseNote =
        "تُترجم واجهة التطبيق بالكامل إلى العربية. أما أسماء الحاسبات ووصفها " +
            "ومعادلاتها الهندسية فتبقى بالإنجليزية في هذه المرحلة."
    override val themeSystem = "حسب النظام"
    override val themeLight = "فاتح"
    override val themeDark = "داكن"
    override val languageSystem = "حسب لغة النظام"
    override val languageArabic = "العربية"
    override val languageEnglish = "الإنجليزية"
    override val settingsPrivacyNote =
        "يعمل MechForge محلياً بالكامل: تبقى حساباتك على هذا الجهاز. " +
            "لا حساب ولا اتصال بالإنترنت ولا تخزين سحابي مطلوب لأي وظيفة أساسية."

    override val aboutTitle = "حول التطبيق"
    override val aboutVersion = "الإصدار 0.1.0 — pilot"
    override val aboutMissionTitle = "احسب. تحقّق. اهندس."
    override val aboutMissionBody =
        "يمنح MechForge المهندس الميكانيكي مجموعة واحدة موثوقة تعمل دون اتصال " +
            "للحسابات اليومية: الهيدروليكا والتكييف والديناميكا الحرارية والتصميم " +
            "الميكانيكي والأنابيب. كل حاسبة تعرض صيغتها وخطواتها ومرجعها الهندسي — " +
            "نتيجة يمكنك التحقق منها، لا مجرد رقم."
    override val aboutLocalFirstTitle = "محلي أولاً"
    override val aboutLocalFirstBody =
        "تبقى كل البيانات في قاعدة بيانات SQLite محلية على هذا الجهاز. " +
            "لا حساب ولا قياس عن بُعد ولا حاجة للإنترنت."
    override val aboutReferencesTitle = "المراجع والترخيص"
    override val aboutReferencesBody =
        "الصيغ مُنفَّذة وموثّقة بالمراجع؛ ولا تُضمَّن جداول المواصفات المحمية بحقوق " +
            "النشر. النتائج حسابات هندسية وليست إثباتاً للمطابقة."

    override val converterTitle = "محوّل الوحدات"
    override val converterValueLabel = "القيمة"
    override val converterEnterNumber = "أدخل رقماً."
    override val converterIncompatibleUnits = "وحدات غير متوافقة."
    override val converterAllUnits = "كل وحدات"

    override fun blockedFields(count: Int): String = when (count) {
        1 -> "حقل واحد بحاجة إلى تصحيح."
        2 -> "حقلان بحاجة إلى تصحيح."
        else -> "$count حقول بحاجة إلى تصحيح."
    }

    override fun savedCalculations(count: Int): String = when (count) {
        1 -> "حساب محفوظ واحد"
        2 -> "حسابان محفوظان"
        else -> "$count حسابات محفوظة"
    }
}

/** English UI — the reference implementation; every other language mirrors it. */
object EnStrings : UiStrings {

    override val language = LanguageMode.ENGLISH
    override val isRtl = false

    override val appName = "MechForge"
    override val appTagline = "Mechanical Engineering Toolkit"
    override val navHome = "Home"
    override val navConverter = "Converter"
    override val navHistory = "History"
    override val navFavorites = "Favorites"
    override val navProjects = "Projects"
    override val navSettings = "Settings"
    override val navAbout = "About"
    override val openNavigation = "Open navigation"

    override val back = "Back"
    override val cancel = "Cancel"
    override val save = "Save"
    override val create = "Create"
    override val rename = "Rename"
    override val delete = "Delete"
    override val calculate = "Calculate"
    override val reset = "Reset"
    override val copy = "Copy"
    override val exportTxt = "Export txt"
    override val retry = "Retry"
    override val search = "Search"
    override val all = "All"
    override val optionalSuffix = "optional"
    override val unit = "Unit"
    override val value = "Value"
    override val title = "Title"
    override val untitled = "Untitled"

    override val stateLoading = "Loading…"
    override val stateErrorTitle = "Could not load data"
    override val stateErrorBody = "Something went wrong while reading the local database."

    override val homeTitle = "Home"
    override val homeSearchLabel = "Search calculators"
    override val homeSearchPlaceholder = "name, keyword, category"
    override val homeSectionFavorites = "Favorites"
    override val homeSectionRecent = "Recent"
    override val homeSectionCategories = "Categories"
    override val homeNoMatches = "No matching calculators"
    override val homeNoMatchesHint = "Try a different keyword or pick another category."

    override val favoritesTitle = "Favorites"
    override val favoriteAdd = "Add to favorites"
    override val favoriteRemove = "Remove from favorites"
    override val favoritesEmpty = "No favorites yet"
    override val favoritesEmptyHint = "Open any calculator and tap the star to keep it here."

    override val calculatorInputs = "Inputs"
    override val calculatorResults = "Results"
    override val calculatorPrimaryResult = "Primary result"
    override val calculatorOtherResults = "Other results"
    override val calculatorDetails = "Formula, steps and reference"
    override val calculatorFormula = "Formula"
    override val calculatorSteps = "Step by step"
    override val calculatorReference = "Reference"
    override val calculatorNotes = "Notes"
    override val calculatorWarnings = "Warnings"
    override val calculatorSaveToHistory = "Save to history"
    override val calculatorSavedToHistory = "Saved to history."
    override val calculatorSaveDialogTitle = "Save to history"
    override val calculatorReportSaved = "Report saved:"
    override val calculatorExportCancelled = "Export cancelled."
    override val calculatorBlockedHint = "Complete the required inputs to calculate."
    override val calculatorNoResults = "This calculator returned no results."
    override val errorInvalidNumber = "Enter a valid number."
    override val errorCalculationFailed = "Calculation failed"

    override val historyTitle = "History"
    override val historyEmpty = "No calculations yet"
    override val historyEmptyHint = "Open any calculator and press Calculate — it will show up here."
    override val historyRenameTitle = "Rename"
    override val historyDuplicate = "Copy"
    override val historyDuplicateDescription = "Duplicate this calculation"
    override val historyRenameDescription = "Rename this calculation"
    override val historyDeleteDescription = "Delete this calculation"
    override val historyOpenDescription = "Open this calculation in its calculator"
    override val historyMissingCalculator = "This calculator is no longer available"

    override val projectsTitle = "Projects"
    override val projectsEmpty = "No projects yet"
    override val projectsEmptyHint = "Create a project to group saved calculations."
    override val projectsNew = "New project"
    override val projectsRenameTitle = "Rename project"
    override val projectsNameLabel = "Project name"
    override val projectsDescriptionLabel = "Description"

    override val settingsTitle = "Settings"
    override val settingsAppearance = "Appearance"
    override val settingsLanguage = "Language"
    override val settingsLanguagePhaseNote =
        "The app interface is fully available in Arabic. Calculator names, descriptions " +
            "and engineering formulas stay in English in this phase."
    override val themeSystem = "System"
    override val themeLight = "Light"
    override val themeDark = "Dark"
    override val languageSystem = "Follow system"
    override val languageArabic = "Arabic"
    override val languageEnglish = "English"
    override val settingsPrivacyNote =
        "MechForge is local-first: your calculations stay on this machine. " +
            "No account, no internet connection and no cloud storage are required " +
            "for any core feature."

    override val aboutTitle = "About"
    override val aboutVersion = "Version 0.1.0 — Pilot"
    override val aboutMissionTitle = "Calculate. Check. Engineer."
    override val aboutMissionBody =
        "MechForge gives mechanical engineers one reliable, offline toolkit for the " +
            "calculations they use every day: hydraulics, HVAC, thermodynamics, mechanical " +
            "design and piping. Every calculator shows its formula, its steps and its " +
            "engineering reference — a result you can check, not just a number."
    override val aboutLocalFirstTitle = "Local-first"
    override val aboutLocalFirstBody =
        "All data stays in a local SQLite database on this machine. No account, " +
            "no telemetry, no internet required."
    override val aboutReferencesTitle = "References & licensing"
    override val aboutReferencesBody =
        "Formulas are implemented and cited; copyrighted standard tables are NOT embedded. " +
            "Results are engineering calculations, not code compliance."

    override val converterTitle = "Unit Converter"
    override val converterValueLabel = "Value"
    override val converterEnterNumber = "Enter a number."
    override val converterIncompatibleUnits = "Incompatible units."
    override val converterAllUnits = "All units"

    override fun blockedFields(count: Int): String = when (count) {
        1 -> "1 field needs attention."
        else -> "$count fields need attention."
    }

    override fun savedCalculations(count: Int): String = when (count) {
        1 -> "1 saved calculation"
        else -> "$count saved calculations"
    }
}

/**
 * The strings for the current language. Defaults to English when no
 * [com.mechforge.app.ui.theme.MechForgeTheme] is above the composition (previews,
 * tests), so no composable has to defend against a missing provider.
 */
val LocalStrings = staticCompositionLocalOf<UiStrings> { EnStrings }

/** Current UI strings. */
val uiStrings: UiStrings
    @Composable
    @ReadOnlyComposable
    get() = LocalStrings.current

/**
 * Resolves [LanguageMode.SYSTEM] to the device language. The platform actuals read
 * the JVM default locale (both targets are JVM based), so this needs no dependency.
 */
fun resolveStrings(mode: LanguageMode): UiStrings = when (mode) {
    LanguageMode.ARABIC -> ArStrings
    LanguageMode.ENGLISH -> EnStrings
    LanguageMode.SYSTEM -> if (systemLanguageIsArabic()) ArStrings else EnStrings
}

/** True when the OS language is Arabic. Implemented per platform target. */
expect fun systemLanguageIsArabic(): Boolean
