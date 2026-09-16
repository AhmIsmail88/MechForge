package com.mechforge.app.export

/**
 * Labels printed on an exported calculation sheet. Engineering reports are commonly
 * issued in Arabic or English depending on the office, so the report has its own small
 * label set instead of reusing the UI chrome strings.
 *
 * Numbers, symbols, units and formulas always stay in the international form.
 */
data class ReportLabels(
    val documentTitle: String,
    val projectData: String,
    val project: String,
    val client: String,
    val engineer: String,
    val location: String,
    val date: String,
    val reportNo: String,
    val revision: String,
    /** The code/edition the calculation is based on, e.g. "NFPA 12 (2018)". */
    val code: String,
    val consultant: String,
    val contractor: String,
    val status: String,
    val documentNo: String,
    val inputs: String,
    val formula: String,
    val calculationSteps: String,
    val results: String,
    val warnings: String,
    val notes: String,
    val reference: String,
    val signatures: String,
    val preparedBy: String,
    val checkedBy: String,
    val approvedBy: String,
    val signatureLine: String,
    val dateLine: String,
) {
    companion object {
        val ENGLISH = ReportLabels(
            documentTitle = "Calculation Sheet",
            projectData = "Project data",
            project = "Project",
            client = "Client",
            engineer = "Engineer",
            location = "Location",
            date = "Date",
            reportNo = "Report no.",
            revision = "Rev.",
            code = "Code / edition",
            consultant = "Consultant",
            contractor = "Contractor",
            status = "Status",
            documentNo = "Document no.",
            inputs = "Inputs",
            formula = "Formula",
            calculationSteps = "Calculation steps",
            results = "Results",
            warnings = "Warnings",
            notes = "Engineering notes",
            reference = "Reference",
            signatures = "Signatures",
            preparedBy = "Prepared by",
            checkedBy = "Checked by",
            approvedBy = "Approved by",
            signatureLine = "Signature",
            dateLine = "Date",
        )

        val ARABIC = ReportLabels(
            documentTitle = "ورقة حساب",
            projectData = "بيانات المشروع",
            project = "المشروع",
            client = "العميل",
            engineer = "المهندس",
            location = "الموقع",
            date = "التاريخ",
            reportNo = "رقم التقرير",
            revision = "المراجعة",
            code = "الكود / الإصدار",
            consultant = "الاستشاري",
            contractor = "المقاول",
            status = "الحالة",
            documentNo = "رقم المستند",
            inputs = "المدخلات",
            formula = "المعادلة",
            calculationSteps = "خطوات الحل",
            results = "النتائج",
            warnings = "تنبيهات",
            notes = "ملاحظات هندسية",
            reference = "المرجع",
            signatures = "التوقيعات",
            preparedBy = "إعداد",
            checkedBy = "مراجعة",
            approvedBy = "اعتماد",
            signatureLine = "التوقيع",
            dateLine = "التاريخ",
        )

        fun of(rtl: Boolean): ReportLabels = if (rtl) ARABIC else ENGLISH
    }
}
