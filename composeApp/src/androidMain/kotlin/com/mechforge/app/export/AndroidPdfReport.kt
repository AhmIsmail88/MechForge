package com.mechforge.app.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Structured content of one engineering calculation sheet, shared by both platforms.
 * A platform renderer only has to know how to draw these blocks.
 */
/**
 * Android PDF renderer: A4 portrait (595 x 842 pt), header band, optional company
 * logo, project meta block, content blocks, and a footer with page numbering on
 * every page. Text is measured with [StaticLayout], so Arabic shaping and RTL are
 * handled by the platform text engine and words are never broken mid-word.
 */
class AndroidPdfReport(private val logo: File? = null, private val rtl: Boolean = false) {

    companion object {
        private const val PAGE_W = 595
        private const val PAGE_H = 842
        private const val MARGIN = 42f
        private const val FOOTER_BAND = 30f
        private const val CONTENT_W = PAGE_W - 2 * MARGIN
        private const val ACCENT = 0xFF1B4F8A.toInt()
        private const val INK = 0xFF14181D.toInt()
        private const val MUTED = 0xFF5B6570.toInt()
    }

    private val titlePaint = TextPaint().apply {
        textSize = 17f; color = ACCENT; isAntiAlias = true; isFakeBoldText = true
    }
    private val headingPaint = TextPaint().apply {
        textSize = 11.5f; color = ACCENT; isAntiAlias = true; isFakeBoldText = true
    }
    private val bodyPaint = TextPaint().apply { textSize = 9.5f; color = INK; isAntiAlias = true }
    private val mutedPaint = TextPaint().apply { textSize = 9f; color = MUTED; isAntiAlias = true }
    private val footPaint = TextPaint().apply { textSize = 7.5f; color = MUTED; isAntiAlias = true }
    private val rulePaint = Paint().apply { color = ACCENT; strokeWidth = 1.2f }
    private val hairPaint = Paint().apply { color = 0xFFDFE4EA.toInt(); strokeWidth = 0.7f }

    private val doc = PdfDocument()
    private var pageNo = 0
    private var page: PdfDocument.Page? = null
    private var canvas: Canvas? = null
    private var y = 0f

    private fun align(): Layout.Alignment =
        if (rtl) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL

    private fun layout(text: String, paint: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(align())
            .setLineSpacing(2f, 1f)
            .build()

    private fun openPage() {
        pageNo += 1
        val p = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
        page = p
        canvas = p.canvas
        y = MARGIN
    }

    private fun drawFooter(c: Canvas) {
        val x = if (rtl) PAGE_W - MARGIN else MARGIN
        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        val text = "MechForge  -  $ts  -  page $pageNo"
        val p = footPaint
        c.drawText(text, x - (if (rtl) p.measureText(text) else 0f), PAGE_H - 14f, p)
    }

    private fun closePage() {
        page?.let { p ->
            canvas?.let { drawFooter(it) }
            doc.finishPage(p)
        }
        page = null
        canvas = null
    }

    private fun ensure(space: Float) {
        if (canvas == null || y + space > PAGE_H - MARGIN - FOOTER_BAND) {
            closePage()
            openPage()
        }
    }

    private fun draw(text: String, paint: TextPaint, indent: Float = 0f, gap: Float = 3f) {
        val width = (CONTENT_W - indent).toInt()
        val lay = layout(text, paint, width)
        ensure(lay.height + gap)
        val c = canvas ?: return
        c.save()
        c.translate(MARGIN + indent, y)
        lay.draw(c)
        c.restore()
        y += lay.height + gap
    }

    fun write(out: File, title: String, meta: List<Pair<String, String>>, blocks: List<ReportBlock>): Boolean {
        openPage()
        val c0 = canvas!!
        var headerBottom = MARGIN
        if (logo != null && logo.exists()) {
            try {
                val bmp = android.graphics.BitmapFactory.decodeFile(logo.absolutePath)
                if (bmp != null) {
                    val h = 34f
                    val w = h * bmp.width / bmp.height
                    val left = if (rtl) PAGE_W - MARGIN - w else MARGIN
                    c0.drawBitmap(bmp, null, android.graphics.RectF(left, MARGIN - 6f, left + w, MARGIN - 6f + h), Paint().apply { isFilterBitmap = true })
                    headerBottom = MARGIN - 6f + h
                }
            } catch (_: Throwable) {
                // a broken logo must never block the report
            }
        }
        val titleLeft = if (rtl) MARGIN else MARGIN
        c0.drawText(title, titleLeft, headerBottom + 12f, titlePaint)
        y = headerBottom + 22f
        c0.drawLine(MARGIN, y, PAGE_W - MARGIN, y, rulePaint)
        y += 14f

        if (meta.isNotEmpty()) {
            draw(if (rtl) "بيانات المشروع" else "Project data", headingPaint, gap = 4f)
            for ((k, v) in meta) {
                if (v.isBlank()) continue
                draw("$k: $v", bodyPaint)
            }
            y += 4f
        }

        for (b in blocks) {
            when (b) {
                is ReportBlock.Heading -> {
                    ensure(26f)
                    draw(b.text, headingPaint, gap = 4f)
                    canvas?.let { it.drawLine(MARGIN, y + 1f, PAGE_W - MARGIN, y + 1f, hairPaint) }
                    y += 6f
                }
                is ReportBlock.Paragraph -> draw(b.text, bodyPaint, gap = 4f)
                is ReportBlock.KeyValue -> draw("${b.label}: ${b.value}", bodyPaint, gap = 2f)
                is ReportBlock.TableRow -> draw(b.cells.joinToString("   |   "), bodyPaint, gap = 2f)
                is ReportBlock.Divider -> {
                    ensure(10f)
                    canvas?.let { it.drawLine(MARGIN, y, PAGE_W - MARGIN, y, hairPaint) }
                    y += 8f
                }
                is ReportBlock.Warning -> {
                    val lay = layout(b.text, mutedPaint, (CONTENT_W - 10f).toInt())
                    ensure(lay.height + 14f)
                    val cv = canvas ?: continue
                    val top = y
                    cv.drawRect(MARGIN, top, PAGE_W - MARGIN, top + lay.height + 10f, Paint().apply { color = 0xFFFFF6E0.toInt() })
                    cv.drawRect(MARGIN, top, MARGIN + 3f, top + lay.height + 10f, Paint().apply { color = 0xFFD9A72A.toInt() })
                    cv.save(); cv.translate(MARGIN + 7f, top + 5f); lay.draw(cv); cv.restore()
                    y = top + lay.height + 14f
                }
            }
        }
        closePage()
        return try {
            out.parentFile?.mkdirs()
            out.outputStream().use { doc.writeTo(it) }
            doc.close()
            true
        } catch (t: Throwable) {
            try { doc.close() } catch (_: Throwable) { }
            false
        }
    }
}
