package com.mechforge.app.export

import com.mechforge.core.engine.CalculatorRegistry
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.Units
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Excel export is a real OOXML package, so it is verified the way a spreadsheet
 * application would open it: unzip the bytes and inspect the parts and the sheet XML.
 * The test also drops a sample workbook next to the sample PDF for review.
 */
class XlsxReportTest {

    private fun iv(id: String, value: Double, unitId: String) =
        InputValue(id, Units.byId(unitId).toBase(value), unitId)

    private fun sampleBlocks(labels: ReportLabels) = Pair(
        listOf(
            "Project" to "Sample project",
            "Client" to "Example client",
            "Engineer" to "M. Ahmed",
            "Date" to "2026-09-15",
        ),
        ReportSheet.build(
            CalculatorRegistry.byIdOrThrow("pump-power"),
            mapOf(
                "q" to iv("q", 100.0, "m3h"),
                "h" to iv("h", 50.0, "m"),
                "eta" to iv("eta", 80.0, "pct"),
                "rho" to iv("rho", 1000.0, "kgm3"),
            ),
            CalculatorRegistry.byIdOrThrow("pump-power").run(
                mapOf(
                    "q" to iv("q", 100.0, "m3h"),
                    "h" to iv("h", 50.0, "m"),
                    "eta" to iv("eta", 80.0, "pct"),
                    "rho" to iv("rho", 1000.0, "kgm3"),
                ),
            ),
            "Pump duty point - Example",
            labels,
            listOf(
                labels.preparedBy to "M. Ahmed",
                labels.checkedBy to "A. Ismail",
                labels.approvedBy to "",
            ),
        ),
    )

    private fun parts(bytes: ByteArray): Map<String, String> {
        val out = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                out[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return out
    }

    @Test
    fun writesAValidWorkbookWithTheReportBlocks() {
        val (meta, blocks) = sampleBlocks(ReportLabels.ENGLISH)
        val bytes = XlsxReport.render("Pump Hydraulic Power & Shaft Power", meta, blocks, ReportLabels.ENGLISH, rtl = false)

        val zip = parts(bytes)
        for (required in listOf(
            "[Content_Types].xml",
            "_rels/.rels",
            "xl/workbook.xml",
            "xl/styles.xml",
            "xl/worksheets/sheet1.xml",
        )) {
            assertTrue(zip.containsKey(required), "missing part: $required")
        }

        val sheet = zip.getValue("xl/worksheets/sheet1.xml")
        assertTrue(sheet.contains("Pump Hydraulic Power"), "title missing from the sheet")
        assertTrue(sheet.contains(ReportLabels.ENGLISH.projectData), "letterhead block missing")
        assertTrue(sheet.contains(ReportLabels.ENGLISH.formula), "formula block missing")
        assertTrue(sheet.contains(ReportLabels.ENGLISH.calculationSteps), "steps block missing")
        assertTrue(sheet.contains(ReportLabels.ENGLISH.results), "results block missing")
        assertTrue(sheet.contains("By Ahmed Ismail"), "signature credit missing")
        assertTrue(sheet.contains("kW"), "result values missing")
        // the workbook must declare at least the style table the renderer uses
        assertTrue(zip.getValue("xl/styles.xml").contains("cellXfs"))
        // a left-to-right report must not ask for RTL rendering
        assertTrue(!sheet.contains("rightToLeft=\"1\""), "LTR report marked as RTL")
    }

    @Test
    fun arabicWorkbookIsRightToLeft() {
        val (meta, blocks) = sampleBlocks(ReportLabels.ARABIC)
        val bytes = XlsxReport.render("قدرة الطلمبة الهيدروليكية", meta, blocks, ReportLabels.ARABIC, rtl = true)
        val sheet = parts(bytes).getValue("xl/worksheets/sheet1.xml")
        assertTrue(sheet.contains("rightToLeft=\"1\""), "Arabic workbook must be RTL")
        assertTrue(sheet.contains(ReportLabels.ARABIC.inputs) || sheet.contains(ReportLabels.ARABIC.results), "Arabic labels missing")
    }

    @Test
    fun xmlSpecialCharactersAreEscaped() {
        val bytes = XlsxReport.render(
            "A & B <test>",
            listOf("Label" to "value < 10 & more"),
            listOf(ReportBlock.Paragraph("Q = m*cp*(T2 - T1) & rate")),
            ReportLabels.ENGLISH,
            rtl = false,
        )
        val sheet = parts(bytes).getValue("xl/worksheets/sheet1.xml")
        assertTrue(sheet.contains("A &amp; B &lt;test&gt;"), "title not escaped")
        assertTrue(sheet.contains("value &lt; 10 &amp; more"), "value not escaped")
    }

    @Test
    fun writesTheSampleWorkbookForReview() {
        val (meta, blocks) = sampleBlocks(ReportLabels.ENGLISH)
        val bytes = XlsxReport.render("Pump duty point - Example", meta, blocks, ReportLabels.ENGLISH, rtl = false)
        val dir = File("../dist").apply { mkdirs() }
        val target = File(dir, "MechForge-sample-report.xlsx")
        target.writeBytes(bytes)
        assertTrue(target.exists() && target.length() > 3000, "sample workbook missing or too small")
        assertEquals("PK", String(target.readBytes().copyOfRange(0, 2), Charsets.ISO_8859_1))
    }
}
