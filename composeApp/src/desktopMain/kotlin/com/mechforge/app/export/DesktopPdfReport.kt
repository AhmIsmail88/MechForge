package com.mechforge.app.export

import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.imageio.ImageIO

/**
 * Desktop PDF renderer. No PDF library is available offline, so each A4 page is
 * drawn with Java2D (which shapes Arabic correctly) and encoded as JPEG; the pages
 * are then wrapped in a minimal PDF container written by hand using the JPEG
 * DCTDecode filter. Result: a real, printable PDF with no new dependency.
 */
class DesktopPdfReport(private val logoBytes: ByteArray? = null, private val rtl: Boolean = false) {

    companion object {
        private const val DPI = 150
        private const val W = 1240   // A4 @150dpi
        private const val H = 1754
        private const val MARGIN = 90
        private val ACCENT = Color(0x1B, 0x4F, 0x8A)
        private val INK = Color(0x14, 0x18, 0x1D)
        private val MUTED = Color(0x5B, 0x65, 0x70)
    }

    private val pages = mutableListOf<BufferedImage>()

    // Arabic reports need a font with Arabic glyphs; the logical SANS_SERIF covers Latin only
    // on some JVMs. Segoe UI ships with Windows and shapes Arabic correctly.
    private fun font(size: Int, bold: Boolean = false) =
        Font(
            if (rtl) "Segoe UI" else Font.SANS_SERIF,
            if (bold) Font.BOLD else Font.PLAIN,
            size,
        )

    private fun newPage(): Pair<BufferedImage, Graphics2D> {
        val img = BufferedImage(W, H, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.color = Color.WHITE
        g.fillRect(0, 0, W, H)
        g.color = INK
        return img to g
    }

    private fun originX(width: Int): Int = if (rtl) W - MARGIN - width else MARGIN

    fun write(out: File, title: String, meta: List<Pair<String, String>>, blocks: List<ReportBlock>): Boolean {
        var (img, g) = newPage()
        var y = MARGIN + 20
        var pageNo = 1

        fun footer() {
            g.font = font(16)
            g.color = MUTED
            val ts = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
            val t = "MechForge  -  $ts  -  page $pageNo"
            g.drawString(t, originX(g.fontMetrics.stringWidth(t)), H - 40)
            g.color = INK
        }

        fun roll() {
            footer()
            g.dispose()
            pages.add(img)
            pageNo += 1
            val np = newPage()
            img = np.first
            g = np.second
            y = MARGIN + 20
        }

        fun need(px: Int) {
            if (y + px > H - MARGIN - 60) roll()
        }

        fun wrapped(text: String, size: Int, bold: Boolean = false): List<String> {
            g.font = font(size, bold)
            val max = W - 2 * MARGIN
            val out = mutableListOf<String>()
            var line = StringBuilder()
            for (word in text.split(" ")) {
                val candidate = if (line.isEmpty()) word else line.toString() + " " + word
                if (g.fontMetrics.stringWidth(candidate) <= max) {
                    line = StringBuilder(candidate)
                } else {
                    if (line.isNotEmpty()) out.add(line.toString())
                    line = StringBuilder(word)
                }
            }
            if (line.isNotEmpty()) out.add(line.toString())
            return out
        }

        // header + logo
        var headerY = MARGIN + 20
        if (logoBytes != null && logoBytes.isNotEmpty()) {
            try {
                val bmp = ImageIO.read(ByteArrayInputStream(logoBytes))
                if (bmp != null) {
                    val h = 70
                    val w = h * bmp.width / bmp.height
                    g.drawImage(bmp, originX(w), MARGIN - 10, w, h, null)
                    headerY = MARGIN - 10 + h
                }
            } catch (_: Throwable) {
            }
        }
        g.font = font(30, true)
        g.color = ACCENT
        for (l in wrapped(title, 30, true)) {
            g.drawString(l, originX(g.fontMetrics.stringWidth(l)), headerY + 32)
            headerY += 34
        }
        y = headerY + 18
        g.drawLine(MARGIN, y, W - MARGIN, y)
        y += 30
        g.color = INK

        if (meta.any { it.second.isNotBlank() }) {
            g.font = font(20, true)
            g.color = ACCENT
            val h1 = if (rtl) "\u0628\u064a\u0627\u0646\u0627\u062a \u0627\u0644\u0645\u0634\u0631\u0648\u0639" else "Project data"
            g.drawString(h1, originX(g.fontMetrics.stringWidth(h1)), y)
            y += 26
            g.color = INK
            for ((k, v) in meta) {
                if (v.isBlank()) continue
                need(24)
                g.font = font(17)
                val line = "$k: $v"
                g.drawString(line, originX(g.fontMetrics.stringWidth(line)), y)
                y += 22
            }
            y += 10
        }

        for (b in blocks) {
            when (b) {
                is ReportBlock.Heading -> {
                    need(40)
                    g.font = font(21, true)
                    g.color = ACCENT
                    g.drawString(b.text, originX(g.fontMetrics.stringWidth(b.text)), y)
                    y += 8
                    g.drawLine(MARGIN, y, W - MARGIN, y)
                    y += 24
                    g.color = INK
                }
                is ReportBlock.Paragraph -> {
                    g.font = font(17)
                    for (l in wrapped(b.text, 17)) {
                        need(24)
                        g.drawString(l, originX(g.fontMetrics.stringWidth(l)), y)
                        y += 22
                    }
                    y += 4
                }
                is ReportBlock.KeyValue -> {
                    need(24)
                    g.font = font(17)
                    val line = "${b.label}: ${b.value}"
                    g.drawString(line, originX(g.fontMetrics.stringWidth(line)), y)
                    y += 22
                }
                is ReportBlock.TableRow -> {
                    need(26)
                    g.font = font(17)
                    val label = b.cells.getOrNull(0) ?: ""
                    val value = b.cells.getOrNull(1) ?: ""
                    val colW = ((W - 2 * MARGIN) * 0.62).toInt()
                    val labelX = if (rtl) W - MARGIN - colW else MARGIN
                    val valueX = if (rtl) MARGIN else MARGIN + colW + 10
                    g.drawString(label, labelX, y)
                    if (value.isNotEmpty()) g.drawString(value, valueX, y)
                    y += 22
                    g.color = Color(0xDF, 0xE4, 0xEA)
                    g.drawLine(MARGIN, y - 17, W - MARGIN, y - 17)
                    g.color = INK
                }
                is ReportBlock.Divider -> {
                    need(20)
                    g.color = Color(0xDF, 0xE4, 0xEA)
                    g.drawLine(MARGIN, y, W - MARGIN, y)
                    g.color = INK
                    y += 18
                }
                is ReportBlock.Warning -> {
                    val lines = wrapped(b.text, 16)
                    val boxH = lines.size * 22 + 20
                    need(boxH + 12)
                    g.color = Color(0xFF, 0xF6, 0xE0)
                    g.fillRect(MARGIN, y - 18, W - 2 * MARGIN, boxH)
                    g.color = Color(0xD9, 0xA7, 0x2A)
                    g.fillRect(MARGIN, y - 18, 5, boxH)
                    g.color = MUTED
                    g.font = font(16)
                    var wy = y
                    for (l in lines) {
                        g.drawString(l, originX(g.fontMetrics.stringWidth(l)), wy)
                        wy += 22
                    }
                    g.color = INK
                    y += boxH + 8
                }
            }
        }
        footer()
        g.dispose()
        pages.add(img)

        return try {
            out.parentFile?.mkdirs()
            out.writeBytes(buildPdf())
            true
        } catch (t: Throwable) {
            false
        }
    }

    /** Minimal PDF: one page per rendered image, embedded as a DCTDecode XObject. */
    private fun buildPdf(): ByteArray {
        val out = ByteArrayOutputStream()
        val offsets = mutableListOf<Int>()
        fun w(s: String) = out.write(s.toByteArray(Charsets.ISO_8859_1))
        fun count() = out.size()

        w("%PDF-1.4\n")
        val pageObjIds = mutableListOf<Int>()
        val imgObjIds = mutableListOf<Int>()
        // ids: 1 catalog, 2 pages, 3..: per page (page, content, image)
        var nextId = 3
        for (i in pages.indices) {
            pageObjIds.add(nextId)
            imgObjIds.add(nextId + 2)
            nextId += 3
        }

        fun obj(id: Int, body: String, stream: ByteArray? = null) {
            while (offsets.size <= id) offsets.add(0)
            offsets[id] = count()
            w("$id 0 obj\n$body\n")
            if (stream != null) {
                w("stream\n")
                out.write(stream)
                w("\nendstream\n")
            }
            w("endobj\n")
        }

        val kids = pageObjIds.joinToString(" ") { "$it 0 R" }
        obj(1, "<< /Type /Catalog /Pages 2 0 R >>")
        obj(2, "<< /Type /Pages /Count ${pages.size} /Kids [$kids] >>")

        for (i in pages.indices) {
            val jpg = ByteArrayOutputStream().also { ImageIO.write(pages[i], "jpg", it) }.toByteArray()
            val pid = pageObjIds[i]
            val cid = pid + 1
            val iid = imgObjIds[i]
            obj(pid, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] " +
                "/Resources << /XObject << /Im0 $iid 0 R >> >> /Contents $cid 0 R >>")
            val content = "q 595 0 0 842 0 0 cm /Im0 Do Q".toByteArray(Charsets.ISO_8859_1)
            obj(cid, "<< /Length ${content.size} >>", content)
            val dict = "<< /Type /XObject /Subtype /Image /Width ${W} /Height $H " +
                "/ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${jpg.size} >>"
            obj(iid, dict, jpg)
        }

        val xrefPos = count()
        val total = nextId
        w("xref\n0 $total\n")
        w("0000000000 65535 f \n")
        for (id in 1 until total) {
            val off = if (id < offsets.size) offsets[id] else 0
            w(String.format("%010d 00000 n \n", off))
        }
        w("trailer\n<< /Size $total /Root 1 0 R >>\nstartxref\n$xrefPos\n%%EOF\n")
        return out.toByteArray()
    }
}
