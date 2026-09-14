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
}
