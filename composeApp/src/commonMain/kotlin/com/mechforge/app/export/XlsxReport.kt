package com.mechforge.app.export

/**
 * A tiny, dependency-free XLSX writer plus a renderer that turns the same
 * [ReportBlock] list the PDF uses into a formatted worksheet.
 *
 * Why hand-rolled: the report sheet is a handful of rows and columns, and the app ships
 * no third-party runtime (README 20/39). An .xlsx file is a ZIP of XML parts; the parts
 * are written here with the STORE method (no compression), which keeps the whole thing in
 * `commonMain` and works on both Android and the desktop JVM without a platform API.
 *
 * The worksheet follows the PDF order exactly: title, project data, inputs, formula,
 * steps, results, warnings, notes, reference, signatures - and it honours the report
 * language (labels) and direction (RTL).
 */
internal object XlsxReport {

    /** Cell style indices, matching the cellXfs table below. */
    private object Style {
        const val NORMAL = 0
        const val TITLE = 1
        const val SECTION = 2
        const val LABEL = 3
        const val VALUE = 4
        const val WARNING = 5
        const val PARAGRAPH = 6
        const val SIGNATURE = 7
        const val FORMULA = 8
    }

    private data class Cell(val text: String, val style: Int)

    fun render(
        title: String,
        meta: List<Pair<String, String>>,
        blocks: List<ReportBlock>,
        labels: ReportLabels,
        rtl: Boolean,
        appName: String = "MechForge",
    ): ByteArray {
        val rows = mutableListOf<List<Cell>>()

        rows.add(listOf(Cell(appName, Style.TITLE)))
        rows.add(listOf(Cell(title, Style.TITLE)))
        rows.add(emptyList())

        if (meta.isNotEmpty()) {
            rows.add(listOf(Cell(labels.projectData, Style.SECTION)))
            for ((label, value) in meta) {
                rows.add(listOf(Cell(label, Style.LABEL), Cell(value, Style.VALUE)))
            }
            rows.add(emptyList())
        }

        for (block in blocks) {
            when (block) {
                is ReportBlock.Heading -> rows.add(listOf(Cell(block.text, Style.SECTION)))
                is ReportBlock.KeyValue -> rows.add(listOf(Cell(block.label, Style.LABEL), Cell(block.value, Style.VALUE)))
                is ReportBlock.TableRow -> rows.add(
                    listOf(
                        Cell(block.cells.getOrNull(0) ?: "", Style.LABEL),
                        Cell(block.cells.getOrNull(1) ?: "", Style.VALUE),
                    ),
                )
                is ReportBlock.Paragraph -> rows.add(listOf(Cell(block.text, Style.PARAGRAPH), Cell("", Style.PARAGRAPH)))
                is ReportBlock.Warning -> rows.add(listOf(Cell(block.text, Style.WARNING), Cell("", Style.WARNING)))
                ReportBlock.Divider -> rows.add(emptyList())
                // a spreadsheet has no pages: the page break becomes a separator row
                ReportBlock.PageBreak -> rows.add(emptyList())
            }
        }

        return writeSingleSheet("Report", rows, rtl)
    }

    // ---------------------------------------------------------------- workbook

    private fun writeSingleSheet(name: String, rows: List<List<Cell>>, rtl: Boolean): ByteArray {
        val sheet = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
            append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
            append("<sheetViews><sheetView workbookViewId=\"0\"")
            if (rtl) append(" rightToLeft=\"1\"")
            append("/></sheetViews>")
            append("<sheetFormatPr defaultRowHeight=\"15\"/>")
            append("<cols>")
            append("<col min=\"1\" max=\"1\" width=\"42\" customWidth=\"1\"/>")
            append("<col min=\"2\" max=\"2\" width=\"58\" customWidth=\"1\"/>")
            append("</cols>")
            append("<sheetData>")
            rows.forEachIndexed { index, cells ->
                val rowNumber = index + 1
                if (cells.isEmpty()) {
                    append("<row r=\"$rowNumber\"/>")
                    return@forEachIndexed
                }
                append("<row r=\"$rowNumber\">")
                cells.forEachIndexed { column, cell ->
                    val ref = columnName(column) + rowNumber
                    append("<c r=\"$ref\" s=\"${cell.style}\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                    append(escape(cell.text))
                    append("</t></is></c>")
                }
                append("</row>")
            }
            append("</sheetData></worksheet>")
        }

        val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/></Types>"""

        val rootRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>"""

        val workbook = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="$name" sheetId="1" r:id="rId1"/></sheets></workbook>"""

        val workbookRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>"""

        // Fonts: 0 body (Segoe UI 11), 1 bold, 2 title (bold 14 white), 3 label (bold 11 brand)
        // Fills:  0 none, 1 gray125 (required), 2 brand, 3 light blue, 4 amber, 5 light grey
        // Borders: 0 none, 1 thin grey
        val styles = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><fonts count="4"><font><sz val="11"/><name val="Segoe UI"/><color theme="1"/></font><font><b/><sz val="11"/><name val="Segoe UI"/><color theme="1"/></font><font><b/><sz val="14"/><name val="Segoe UI"/><color rgb="FFFFFFFF"/></font><font><b/><sz val="11"/><name val="Segoe UI"/><color rgb="FF1B4F8A"/></font></fonts><fills count="6"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill><fill><patternFill patternType="solid"><fgColor rgb="FF1B4F8A"/><bgColor indexed="64"/></patternFill></fill><fill><patternFill patternType="solid"><fgColor rgb="FFE8F0FA"/><bgColor indexed="64"/></patternFill></fill><fill><patternFill patternType="solid"><fgColor rgb="FFFFF4E0"/><bgColor indexed="64"/></patternFill></fill><fill><patternFill patternType="solid"><fgColor rgb="FFF2F4F7"/><bgColor indexed="64"/></patternFill></fill></fills><borders count="2"><border><left/><right/><top/><bottom/><diagonal/></border><border><left style="thin"><color rgb="FFBFC7D2"/></left><right style="thin"><color rgb="FFBFC7D2"/></right><top style="thin"><color rgb="FFBFC7D2"/></top><bottom style="thin"><color rgb="FFBFC7D2"/></bottom><diagonal/></border></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="9"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="2" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1" applyAlignment="1"><alignment vertical="center"/></xf><xf numFmtId="0" fontId="1" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf><xf numFmtId="0" fontId="3" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1"><alignment vertical="top" wrapText="1"/></xf><xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment vertical="top" wrapText="1"/></xf><xf numFmtId="0" fontId="0" fillId="4" borderId="1" xfId="0" applyFill="1" applyBorder="1" applyAlignment="1"><alignment vertical="top" wrapText="1"/></xf><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0" applyAlignment="1"><alignment vertical="top" wrapText="1"/></xf><xf numFmtId="0" fontId="0" fillId="5" borderId="0" xfId="0" applyFill="1" applyAlignment="1"><alignment vertical="top" wrapText="1"/></xf><xf numFmtId="0" fontId="1" fillId="5" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment vertical="top" wrapText="1"/></xf></cellXfs><cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles></styleSheet>"""

        return Zip.store(
            listOf(
                "[Content_Types].xml" to contentTypes,
                "_rels/.rels" to rootRels,
                "xl/workbook.xml" to workbook,
                "xl/_rels/workbook.xml.rels" to workbookRels,
                "xl/styles.xml" to styles,
                "xl/worksheets/sheet1.xml" to sheet,
            ),
        )
    }

    private fun columnName(index: Int): String {
        var i = index
        val sb = StringBuilder()
        while (true) {
            sb.insert(0, ('A' + (i % 26)))
            i = i / 26 - 1
            if (i < 0) break
        }
        return sb.toString()
    }

    private fun escape(text: String): String = buildString(text.length) {
        for (ch in text) {
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> if (ch.code < 0x20 && ch != '\t') append(' ') else append(ch)
            }
        }
    }
}

/**
 * Minimal ZIP writer using the STORE method (no compression). Enough for an OOXML
 * package, and it needs no platform API, so the same code runs on Android and desktop.
 */
internal object Zip {

    fun store(entries: List<Pair<String, String>>): ByteArray {
        val out = ByteArrayBuilder()
        val central = mutableListOf<ByteArray>()

        for ((name, content) in entries) {
            val nameBytes = name.encodeToByteArray()
            val data = content.encodeToByteArray()
            val crc = crc32(data)
            val offset = out.size

            // local file header
            out.u32(0x04034b50L)
            out.u16(20)
            out.u16(0x0800) // UTF-8 names
            out.u16(0) // stored
            out.u16(0) // time
            out.u16(0x21) // date (1980-01-01)
            out.u32(crc)
            out.u32(data.size)
            out.u32(data.size)
            out.u16(nameBytes.size)
            out.u16(0)
            out.bytes(nameBytes)
            out.bytes(data)

            // central directory record
            val rec = ByteArrayBuilder()
            rec.u32(0x02014b50L)
            rec.u16(20)
            rec.u16(20)
            rec.u16(0x0800)
            rec.u16(0)
            rec.u16(0)
            rec.u16(0x21)
            rec.u32(crc)
            rec.u32(data.size)
            rec.u32(data.size)
            rec.u16(nameBytes.size)
            rec.u16(0)
            rec.u16(0)
            rec.u16(0)
            rec.u16(0)
            rec.u32(0)
            rec.u32(offset)
            rec.bytes(nameBytes)
            central += rec.toByteArray()
        }

        val centralOffset = out.size
        var centralSize = 0
        for (rec in central) {
            out.bytes(rec)
            centralSize += rec.size
        }
        out.u32(0x06054b50L)
        out.u16(0)
        out.u16(0)
        out.u16(central.size)
        out.u16(central.size)
        out.u32(centralSize)
        out.u32(centralOffset)
        out.u16(0)
        return out.toByteArray()
    }

    private fun crc32(data: ByteArray): Long {
        var crc = 0xFFFFFFFFL
        for (b in data) {
            crc = crc xor (b.toLong() and 0xFF)
            repeat(8) {
                crc = if (crc and 1L != 0L) (crc ushr 1) xor 0xEDB88320L else crc ushr 1
            }
        }
        return (crc xor 0xFFFFFFFFL) and 0xFFFFFFFFL
    }

    private class ByteArrayBuilder {
        private var buffer = ByteArray(1024)
        var size = 0
            private set

        private fun ensure(extra: Int) {
            if (size + extra <= buffer.size) return
            var newSize = buffer.size * 2
            while (newSize < size + extra) newSize *= 2
            buffer = buffer.copyOf(newSize)
        }

        fun u16(value: Int) {
            ensure(2)
            buffer[size++] = (value and 0xFF).toByte()
            buffer[size++] = ((value ushr 8) and 0xFF).toByte()
        }

        fun u32(value: Int) = u32(value.toLong())

        fun u32(value: Long) {
            ensure(4)
            buffer[size++] = (value and 0xFF).toByte()
            buffer[size++] = ((value ushr 8) and 0xFF).toByte()
            buffer[size++] = ((value ushr 16) and 0xFF).toByte()
            buffer[size++] = ((value ushr 24) and 0xFF).toByte()
        }

        fun bytes(data: ByteArray) {
            ensure(data.size)
            data.copyInto(buffer, size)
            size += data.size
        }

        fun toByteArray(): ByteArray = buffer.copyOf(size)
    }
}
