package com.mechforge.app.export

import java.io.File
import kotlin.test.Test
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
        assertTrue(text.contains("/Filter /DCTDecode"), "pages were not embedded as JPEG images")
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

        val blocks = listOf(
            ReportBlock.Heading("Inputs"),
            ReportBlock.TableRow(listOf("Q  Flow rate", "100 m3/h")),
            ReportBlock.Heading("Results"),
            ReportBlock.TableRow(listOf("Shaft Power", "17.0254 kW")),
            ReportBlock.Warning("Efficiency above 85% is optimistic."),
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
}
