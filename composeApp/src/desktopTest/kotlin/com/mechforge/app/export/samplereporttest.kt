package com.mechforge.app.export

import com.mechforge.core.engine.CalculatorRegistry
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.Units
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Produces a real sample calculation sheet for review (build/reports), using the
 * same ReportSheet + renderer path the app uses. Not a behaviour test: it only
 * writes an artefact and asserts that a non-trivial PDF came out.
 */
class SampleReportTest {

    private fun iv(id: String, value: Double, unitId: String) =
        InputValue(id, Units.byId(unitId).toBase(value), unitId)

    @Test
    fun writesSamplePumpReport() {
        System.setProperty("java.awt.headless", "true")
        val calc = CalculatorRegistry.byIdOrThrow("pump-power")
        val inputs = mapOf(
            "q" to iv("q", 100.0, "m3h"),
            "h" to iv("h", 50.0, "m"),
            "eta" to iv("eta", 80.0, "pct"),
            "rho" to iv("rho", 1000.0, "kgm3"),
        )
        val output = calc.run(inputs)
        val blocks = ReportSheet.build(
            calc, inputs, output, "Pump duty point - Example",
            ReportLabels.ENGLISH,
            listOf(
                "Prepared by" to "M. Ahmed",
                "Checked by" to "A. Ismail",
                "Approved by" to "",
            ),
        )

        // synthetic letterhead logo (blue box with a lighter band)
        val img = java.awt.image.BufferedImage(240, 80, java.awt.image.BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.color = java.awt.Color(0x1B, 0x4F, 0x8A); g.fillRect(0, 0, 240, 80)
        g.color = java.awt.Color(0x4D, 0xD0, 0xE1); g.fillRect(12, 30, 90, 20)
        g.dispose()
        val bos = ByteArrayOutputStream(); javax.imageio.ImageIO.write(img, "png", bos)

        val outDir = File("build/reports"); outDir.mkdirs()
        val target = File(outDir, "sample-calculation-report.pdf")
        val ok = DesktopPdfReport(bos.toByteArray(), rtl = false).write(
            target,
            calc.def.name,
            listOf(
                "Project" to "Pump Station PS-1",
                "Client" to "Example Water Authority",
                "Engineer" to "M. Ahmed",
                "Location" to "Cairo",
                "Date" to java.time.LocalDate.now().toString(),
            ),
            blocks,
        )
        assertTrue(ok && target.exists() && target.length() > 5000, "sample pdf missing")
        println("SAMPLE_REPORT_OK path=${target.absolutePath} bytes=${target.length()}")
    }
}