package com.mechforge.app.export

import java.io.File
import com.mechforge.core.engine.CalculatorRegistry
import com.mechforge.core.engine.InputValue
import com.mechforge.core.units.Units
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Smoke test for the desktop PDF writer: renders a small report with the shared
 * block model and asserts that a real PDF container was produced.
 */
class PdfReportSmokeTest {

    @Test
    fun writesAPdfContainerWithEmbeddedPages() {
        System.setProperty("java.awt.headless", "true")
        val out = File.createTempFile("mechforge-report-", ".pdf")
        out.delete()

        val blocks = listOf(
            ReportBlock.Heading("Inputs"),
            ReportBlock.TableRow(listOf("Q", "100 m3/h")),
            ReportBlock.TableRow(listOf("d", "100 mm")),
            ReportBlock.KeyValue("Flow area", "0.0139 m2"),
            ReportBlock.Heading("Results"),
            ReportBlock.KeyValue("Velocity", "3.5368 m/s"),
            ReportBlock.Divider,
            ReportBlock.Warning("Transitional flow regime: 2300 < Re < 4000."),
            ReportBlock.Paragraph("Step by step: v = Q / A = 0.0278 / 0.00785 = 3.5368 m/s"),
        )

        val ok = DesktopPdfReport().write(
            out,
            "Pipe Flow Velocity",
            listOf("Project" to "Smoke test", "Engineer" to "AutoCoder"),
            blocks,
        )

        assertTrue(ok, "renderer reported failure")
        assertTrue(out.exists() && out.length() > 2000, "pdf is missing or too small: ${out.length()}")
        val head = String(out.readBytes().copyOfRange(0, 5), Charsets.ISO_8859_1)
        assertTrue(head == "%PDF-", "missing PDF header, got: $head")
        val text = out.readText(Charsets.ISO_8859_1)
        assertTrue(text.contains("/Type /Catalog"), "missing catalog")
        // The pages must be embedded losslessly at print resolution: a 150-dpi JPEG
          // looked faded and soft, so the renderer moved to 300 dpi + FlateDecode.
          assertTrue(text.contains("/Filter /FlateDecode"), "pages were not embedded losslessly")
          assertTrue(text.contains("/Width 2480") && text.contains("/Height 3508"), "pages are not 300 dpi (A4)")
          assertFalse(text.contains("/DCTDecode"), "a lossy JPEG page is still being embedded")
        println("PDF_SMOKE_OK path=${out.absolutePath} bytes=${out.length()} pages=${Regex("/Type /Page[^s]").findAll(text).count()}")
    }

    @Test
    fun writesAPdfWithLogoAndRtlLayout() {
        System.setProperty("java.awt.headless", "true")
        // in-memory PNG logo
        val img = java.awt.image.BufferedImage(80, 40, java.awt.image.BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.color = java.awt.Color(0x1B, 0x4F, 0x8A)
        g.fillRect(0, 0, 80, 40)
        g.dispose()
        val bos = java.io.ByteArrayOutputStream()
        javax.imageio.ImageIO.write(img, "png", bos)

        val out = File.createTempFile("mechforge-letterhead-", ".pdf")
        out.delete()

        // A real sheet with Arabic labels and a signature block: exercises Arabic shaping and RTL.
        val calc = CalculatorRegistry.byIdOrThrow("pump-power")
        val inputs = mapOf(
            "q" to InputValue("q", Units.byId("m3h").toBase(100.0), "m3h"),
            "h" to InputValue("h", Units.byId("m").toBase(50.0), "m"),
            "eta" to InputValue("eta", Units.byId("pct").toBase(80.0), "pct"),
        )
        val blocks = ReportSheet.build(
            calc, inputs, calc.run(inputs), "Pump duty point",
            ReportLabels.ARABIC,
            listOf("Prepared" to "M. Ahmed", "Checked" to "", "Approved" to ""),
        )
        val ok = DesktopPdfReport(bos.toByteArray(), rtl = true).write(
            out,
            "Pump Hydraulic Power & Shaft Power",
            listOf("Project" to "Pilot", "Engineer" to "M. Ahmed"),
            blocks,
        )
        assertTrue(ok, "renderer reported failure with a logo")
        assertTrue(out.exists() && out.length() > 2000, "pdf missing/too small: ${out.length()}")
        println("PDF_LOGO_SMOKE_OK bytes=${out.length()}")
    }

    @Test
    fun writesTheSampleSheetForReview() {
        System.setProperty("java.awt.headless", "true")
        val calc = CalculatorRegistry.byIdOrThrow("pump-power")
        val inputs = mapOf(
            "q" to InputValue("q", Units.byId("m3h").toBase(100.0), "m3h"),
            "h" to InputValue("h", Units.byId("m").toBase(50.0), "m"),
            "eta" to InputValue("eta", Units.byId("pct").toBase(80.0), "pct"),
            "rho" to InputValue("rho", Units.byId("kgm3").toBase(1000.0), "kgm3"),
        )
        val labels = ReportLabels.ENGLISH
        val blocks = ReportSheet.build(
            calc, inputs, calc.run(inputs), "Pump duty point - Example", labels,
            listOf(labels.preparedBy to "M. Ahmed", labels.checkedBy to "A. Ismail", labels.approvedBy to ""),
        )
        val dir = File("../dist").apply { mkdirs() }
        val target = File(dir, "MechForge-sample-report.pdf")
        val ok = DesktopPdfReport().write(
            target,
            "Pump Hydraulic Power & Shaft Power",
            listOf(
                "Project" to "Sample project",
                "Client" to "Example client",
                "Engineer" to "M. Ahmed",
                "Date" to "2026-09-15",
            ),
            blocks,
        )
        assertTrue(ok, "sample renderer reported failure")
        assertTrue(target.exists() && target.length() > 10_000, "sample pdf missing or too small")
        val text = target.readText(Charsets.ISO_8859_1)
        assertTrue(text.contains("/Width 2480"), "sample is not 300 dpi")
        assertTrue(!text.contains("/DCTDecode"), "sample still uses lossy JPEG pages")
        println("PDF_SAMPLE_OK bytes=${target.length()}")
    }
}
