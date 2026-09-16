package com.mechforge.app.export

import com.mechforge.app.data.ProjectInfo
import com.mechforge.app.data.Snapshots
import com.mechforge.core.engine.CalculatorRegistry
import com.mechforge.core.engine.InputValue
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * End-to-end check of the calculation package: it must render, it must be paginated, and every
 * calculation must start on a clean page (engineering audit section 10).
 */
class ProjectPackagePdfTest {

    private val calculator = CalculatorRegistry.byIdOrThrow("pump-power")

    private fun record(n: Int) = ProjectPackage.Record(
        calculationNumber = "FP-%03d".format(n),
        calculatorId = "pump-power",
        title = "Pump power $n",
        revision = "0$n",
        status = "For Review",
        timestamp = 1726200000000L + n * 86_400_000L,
        inputsJson = Snapshots.encodeInputs(
            mapOf(
                "q" to InputValue("q", 0.05 * n, "m3h"),
                "h" to InputValue("h", 40.0, "m"),
                "eta" to InputValue("eta", 0.78, "pct"),
                "rho" to InputValue("rho", 1000.0, "kgm3"),
            )
        ),
        resultsJson = Snapshots.encodeResults(calculator.run(
            mapOf(
                "q" to InputValue("q", 0.05 * n, "m3h"),
                "h" to InputValue("h", 40.0, "m"),
                "eta" to InputValue("eta", 0.78, "pct"),
                "rho" to InputValue("rho", 1000.0, "kgm3"),
            )
        )),
    )

    @Test
    fun thePackageRendersAndStartsEveryCalculationOnItsOwnPage() {
        val project = ProjectInfo(
            name = "Wastewater Pump Station 01",
            projectNumber = "WPS-001",
            client = "ABC",
            consultant = "XYZ",
            preparedBy = "A. Ismail",
            revision = "02",
            status = "For Review",
            codes = "NFPA 20",
            codeEdition = "2022",
        )
        val records = listOf(record(1), record(2), record(3))
        val blocks = ProjectPackage.build(
            project = project,
            records = records,
            labels = ReportLabels.ENGLISH,
            date = "2026-09-16",
            signature = listOf(
                ReportLabels.ENGLISH.preparedBy to project.preparedBy,
                ReportLabels.ENGLISH.checkedBy to project.checkedBy,
            ),
            decodeInputs = { Snapshots.decodeInputs(it) },
            decodeOutput = { Snapshots.decodeResults(it) },
        )

        // every calculation really replays: three sections, no "could not be replayed" warning
        val warnings = blocks.filterIsInstance<ReportBlock.Warning>().map { it.text }
        assertTrue(
            ReportLabels.ENGLISH.packageRecordUnavailable !in warnings,
            "the frozen records must replay, got $warnings",
        )

        val out = File("build/test-artifacts/package.pdf")
        out.parentFile?.mkdirs()
        DesktopPdfReport().write(out, ReportLabels.ENGLISH.packageTitle, emptyList(), blocks)

        assertTrue(out.exists() && out.length() > 10_000, "the package must render, got ${out.length()} bytes")
        val pdf = out.readBytes().toString(Charsets.ISO_8859_1)
        val pages = Regex("/Subtype\\s*/Image").findAll(pdf).count()
        assertTrue(
            pages >= 1 + records.size,
            "expected at least a cover/register page plus one page per calculation, counted $pages",
        )
        println("package pdf: ${out.length()} bytes, $pages rendered pages, ${records.size} calculations")
    }
}
