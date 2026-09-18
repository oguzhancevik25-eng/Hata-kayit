package com.oguzhan.hatakayit

import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.max

data class XlsxCell7(val value: Any? = "", val style: Int = 4)

data class XlsxSheet7(
    val name: String,
    val rows: List<List<XlsxCell7>>,
    val widths: List<Double> = emptyList(),
    val merges: List<String> = emptyList(),
    val freezeRows: Int = 0,
    val autoFilterRef: String? = null,
    val landscape: Boolean = true
)

object XlsxWriter7 {
    fun build(sheets: List<XlsxSheet7>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            put(zip, "[Content_Types].xml", contentTypes(sheets.size))
            put(zip, "_rels/.rels", rootRels())
            put(zip, "docProps/app.xml", appXml())
            put(zip, "docProps/core.xml", coreXml())
            put(zip, "xl/workbook.xml", workbookXml(sheets))
            put(zip, "xl/_rels/workbook.xml.rels", workbookRels(sheets.size))
            put(zip, "xl/styles.xml", stylesXml())
            sheets.forEachIndexed { index, sheet ->
                put(zip, "xl/worksheets/sheet${index + 1}.xml", sheetXml(sheet))
            }
        }
        return out.toByteArray()
    }

    private fun put(zip: ZipOutputStream, path: String, content: String) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun contentTypes(count: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        append("""<Default Extension="xml" ContentType="application/xml"/>""")
        append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
        append("""<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""")
        for (i in 1..count) append("""<Override PartName="/xl/worksheets/sheet$i.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
        append("""<Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>""")
        append("""<Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>""")
        append("</Types>")
    }

    private fun rootRels() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
<Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
</Relationships>"""

    private fun appXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
<Application>Operatör Takip</Application>
<AppVersion>1.7</AppVersion>
</Properties>"""

    private fun coreXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
<dc:title>Operatör Takip Excel Raporu</dc:title>
<dc:creator>Operatör Takip</dc:creator>
<cp:lastModifiedBy>Operatör Takip</cp:lastModifiedBy>
</cp:coreProperties>"""

    private fun workbookXml(sheets: List<XlsxSheet7>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
        append("""<bookViews><workbookView activeTab="0"/></bookViews><sheets>""")
        sheets.forEachIndexed { i, s ->
            append("""<sheet name="${xml(safeSheetName(s.name))}" sheetId="${i + 1}" r:id="rId${i + 1}"/>""")
        }
        append("</sheets></workbook>")
    }

    private fun workbookRels(count: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        for (i in 1..count) {
            append("""<Relationship Id="rId$i" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$i.xml"/>""")
        }
        append("""<Relationship Id="rId${count + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""")
        append("</Relationships>")
    }

    private fun stylesXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<numFmts count="1"><numFmt numFmtId="164" formatCode="0.0"/></numFmts>
<fonts count="7">
<font><sz val="11"/><name val="Calibri"/></font>
<font><b/><color rgb="FFFFFFFF"/><sz val="16"/><name val="Calibri"/></font>
<font><i/><color rgb="FF687386"/><sz val="10"/><name val="Calibri"/></font>
<font><b/><color rgb="FFFFFFFF"/><sz val="11"/><name val="Calibri"/></font>
<font><b/><color rgb="FF0B4F7D"/><sz val="11"/><name val="Calibri"/></font>
<font><b/><color rgb="FF15803D"/><sz val="11"/><name val="Calibri"/></font>
<font><b/><color rgb="FFB91C1C"/><sz val="11"/><name val="Calibri"/></font>
</fonts>
<fills count="8">
<fill><patternFill patternType="none"/></fill>
<fill><patternFill patternType="gray125"/></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FF0B4F7D"/><bgColor indexed="64"/></patternFill></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FF0E7490"/><bgColor indexed="64"/></patternFill></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FFEFF6FF"/><bgColor indexed="64"/></patternFill></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FFDCFCE7"/><bgColor indexed="64"/></patternFill></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FFFEE2E2"/><bgColor indexed="64"/></patternFill></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FFFEF3C7"/><bgColor indexed="64"/></patternFill></fill>
</fills>
<borders count="2">
<border><left/><right/><top/><bottom/><diagonal/></border>
<border>
<left style="thin"><color rgb="FFD9E2EC"/></left>
<right style="thin"><color rgb="FFD9E2EC"/></right>
<top style="thin"><color rgb="FFD9E2EC"/></top>
<bottom style="thin"><color rgb="FFD9E2EC"/></bottom>
<diagonal/>
</border>
</borders>
<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
<cellXfs count="11">
<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
<xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyAlignment="1"><alignment horizontal="left" vertical="center"/></xf>
<xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyAlignment="1"><alignment horizontal="left" vertical="center" wrapText="1"/></xf>
<xf numFmtId="0" fontId="3" fillId="3" borderId="1" xfId="0" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
<xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf>
<xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
<xf numFmtId="164" fontId="4" fillId="4" borderId="1" xfId="0" applyNumberFormat="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
<xf numFmtId="0" fontId="5" fillId="5" borderId="1" xfId="0" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
<xf numFmtId="0" fontId="6" fillId="6" borderId="1" xfId="0" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
<xf numFmtId="0" fontId="4" fillId="7" borderId="1" xfId="0" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
<xf numFmtId="0" fontId="4" fillId="0" borderId="1" xfId="0" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
</cellXfs>
<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
</styleSheet>"""

    private fun sheetXml(s: XlsxSheet7): String {
        val rowCount = max(1, s.rows.size)
        val colCount = max(1, s.rows.maxOfOrNull { it.size } ?: 1)
        val endRef = "${colName(colCount)}$rowCount"
        return buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
            append("""<sheetPr><pageSetUpPr fitToPage="1"/></sheetPr>""")
            append("""<dimension ref="A1:$endRef"/>""")
            append("""<sheetViews><sheetView workbookViewId="0">""")
            if (s.freezeRows > 0) {
                append("""<pane ySplit="${s.freezeRows}" topLeftCell="A${s.freezeRows + 1}" activePane="bottomLeft" state="frozen"/>""")
            }
            append("</sheetView></sheetViews>")
            append("""<sheetFormatPr defaultRowHeight="18"/>""")
            val widths = if (s.widths.isEmpty()) List(colCount) { 14.0 } else s.widths
            append("<cols>")
            for (i in 1..colCount) {
                val width = widths.getOrNull(i - 1) ?: 14.0
                append("""<col min="$i" max="$i" width="${"%.1f".format(Locale.US, width.coerceIn(8.0, 42.0))}" customWidth="1"/>""")
            }
            append("</cols><sheetData>")
            s.rows.forEachIndexed { rIndex, row ->
                val rowNum = rIndex + 1
                val height = when (rowNum) { 1 -> 28; 2 -> 34; else -> 20 }
                append("""<row r="$rowNum" ht="$height" customHeight="1">""")
                row.forEachIndexed { cIndex, cell ->
                    append(cellXml(rowNum, cIndex + 1, cell))
                }
                append("</row>")
            }
            append("</sheetData>")
            s.autoFilterRef?.takeIf { it.isNotBlank() }?.let { append("""<autoFilter ref="${xml(it)}"/>""") }
            if (s.merges.isNotEmpty()) {
                append("""<mergeCells count="${s.merges.size}">""")
                s.merges.forEach { append("""<mergeCell ref="${xml(it)}"/>""") }
                append("</mergeCells>")
            }
            append("""<pageMargins left="0.25" right="0.25" top="0.45" bottom="0.45" header="0.2" footer="0.2"/>""")
            append("""<pageSetup orientation="${if (s.landscape) "landscape" else "portrait"}" paperSize="9" fitToWidth="1" fitToHeight="0"/>""")
            append("</worksheet>")
        }
    }

    private fun cellXml(row: Int, col: Int, cell: XlsxCell7): String {
        val ref = "${colName(col)}$row"
        return when (val v = cell.value) {
            null -> """<c r="$ref" s="${cell.style}" t="inlineStr"><is><t></t></is></c>"""
            is Number -> """<c r="$ref" s="${cell.style}"><v>${v.toDouble()}</v></c>"""
            is Boolean -> """<c r="$ref" s="${cell.style}" t="b"><v>${if (v) 1 else 0}</v></c>"""
            else -> """<c r="$ref" s="${cell.style}" t="inlineStr"><is><t xml:space="preserve">${xml(v.toString())}</t></is></c>"""
        }
    }

    private fun colName(col: Int): String {
        var n = col
        val sb = StringBuilder()
        while (n > 0) {
            n--
            sb.append(('A'.code + (n % 26)).toChar())
            n /= 26
        }
        return sb.reverse().toString()
    }

    private fun safeSheetName(name: String): String =
        name.replace(Regex("""[\\/*?:\[\]]"""), " ").take(31).ifBlank { "Rapor" }

    private fun xml(value: String): String = buildString {
        value.forEach { ch ->
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> if (ch.code >= 32 || ch == '\n' || ch == '\r' || ch == '\t') append(ch)
            }
        }
    }
}

object ExcelReports7 {
    private val tr = Locale("tr", "TR")
    private val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy", tr)
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", tr)
    private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", tr)

    fun dashboard(
        records: List<Record4>,
        operators: List<Operator4>,
        settings: ScoringSettings4
    ): XlsxSheet7 {
        val active = operators.filter { it.active }
        val summaries = active.map { it to calculateConfigured4(it, records, settings) }
        val scores = summaries.map { it.second }.filter { it.hasData }.map { it.total }
        val avg = if (scores.isEmpty()) 0.0 else scores.average()
        val rows = mutableListOf<List<XlsxCell7>>()
        rows += titleRow("OPERATÖR TAKİP • YÖNETİCİ DASHBOARD", 10)
        rows += subtitleRow("Dönem: 01.08.2026 – 31.07.2027 • Excel çıktısı yazdırmaya hazırdır.", 10)
        rows += blankRow(10)
        rows += row("Gösterge", "Değer", styles = intArrayOf(3, 3))
        rows += row("Takım Ortalama Puan", avg, styles = intArrayOf(4, 6))
        rows += row("Kaçan Hata", summaries.sumOf { it.second.escaped }, styles = intArrayOf(4, 5))
        rows += row("Yakalanan Hata", summaries.sumOf { it.second.caught }, styles = intArrayOf(4, 5))
        rows += row("KY", summaries.sumOf { it.second.ky }, styles = intArrayOf(4, 5))
        rows += row("Kaizen", summaries.sumOf { it.second.kaizen }, styles = intArrayOf(4, 5))
        rows += row("Aktif Operatör", active.size, styles = intArrayOf(4, 10))
        rows += blankRow(10)
        val headerRow = rows.size + 1
        rows += header("Sicil","Personel","Puan","Sınıf","KY","Kaizen","Kaçan","Yakalanan","Mesai","Devamsızlık")
        summaries.forEach { (op, s) ->
            rows += listOf(
                c(op.sicil), c(op.name), n(if (s.hasData) s.total else 0.0, 6),
                c(s.grade, gradeStyle(s.grade)), n(s.ky), n(s.kaizen), n(s.escaped),
                n(s.caught), n(s.overtime), n(s.absence)
            )
        }
        return XlsxSheet7(
            "Ana Panel", rows,
            widths = listOf(11.0,27.0,11.0,9.0,10.0,10.0,10.0,11.0,10.0,12.0),
            merges = listOf("A1:J1","A2:J2"),
            freezeRows = headerRow,
            autoFilterRef = "A$headerRow:J${rows.size}",
            landscape = true
        )
    }

    fun personnel(
        records: List<Record4>,
        operators: List<Operator4>,
        settings: ScoringSettings4,
        selectedSicil: String?
    ): XlsxSheet7 {
        val selected = selectedSicil?.takeIf { it.isNotBlank() && it != "ALL" }?.let { sicil ->
            operators.find { it.sicil == sicil }
        }
        if (selected != null) return singlePerson(records, selected, settings)

        val rows = mutableListOf<List<XlsxCell7>>()
        rows += titleRow("PERSONEL PERFORMANS RAPORU", 17)
        rows += subtitleRow("Yıllık performans görüşmesi için otomatik puan ve faaliyet özeti.", 17)
        rows += blankRow(17)
        val headerRow = rows.size + 1
        rows += header(
            "Sicil","Ad Soyad","Durum","Puan","Sınıf","KY","Kaizen","Kaçan","Yakalanan",
            "Mesai","Yıllık İzin","Günlük İzin","Rapor","Devamsızlık","Kalite P.","KY P.","Kaizen P."
        )
        operators.forEach { op ->
            val s = calculateConfigured4(op, records, settings)
            rows += listOf(
                c(op.sicil), c(op.name), c(if (op.active) "AKTİF" else "PASİF", if (op.active) 7 else 9),
                n(if (s.hasData) s.total else 0.0, 6), c(s.grade, gradeStyle(s.grade)),
                n(s.ky), n(s.kaizen), n(s.escaped), n(s.caught), n(s.overtime),
                n(s.annualLeave), n(s.dailyLeave), n(s.report), n(s.absence),
                n(s.quality), n(s.kyScore), n(s.kaizenScore)
            )
        }
        return XlsxSheet7(
            "Personel", rows,
            widths = listOf(10.0,26.0,11.0,10.0,8.0,9.0,10.0,9.0,11.0,9.0,12.0,12.0,9.0,12.0,11.0,9.0,11.0),
            merges = listOf("A1:Q1","A2:Q2"),
            freezeRows = headerRow,
            autoFilterRef = "A$headerRow:Q${rows.size}",
            landscape = true
        )
    }

    private fun singlePerson(records: List<Record4>, op: Operator4, settings: ScoringSettings4): XlsxSheet7 {
        val s = calculateConfigured4(op, records, settings)
        val rows = mutableListOf<List<XlsxCell7>>()
        rows += titleRow("${op.name} • PERFORMANS RAPORU", 8)
        rows += subtitleRow("Sicil ${op.sicil} • ${if (op.active) "AKTİF" else "PASİF"} • Dönem 01.08.2026 – 31.07.2027", 8)
        rows += blankRow(8)
        rows += header("Gösterge","Değer","Puan / Bilgi","","","","","")
        rows += row("Toplam Puan", if (s.hasData) s.total else 0.0, s.grade, styles=intArrayOf(4,6,gradeStyle(s.grade),4,4,4,4,4))
        rows += row("Kalite", s.quality, "${fmt(settings.qualityMax)} üzerinden", styles=intArrayOf(4,5,4,4,4,4,4,4))
        rows += row("KY", s.ky, "Puan: ${fmt(s.kyScore)}", styles=intArrayOf(4,5,4,4,4,4,4,4))
        rows += row("Kaizen", s.kaizen, "Puan: ${fmt(s.kaizenScore)}", styles=intArrayOf(4,5,4,4,4,4,4,4))
        rows += row("Mesai", s.overtime, "Puan: ${fmt(s.overtimeScore)}", styles=intArrayOf(4,5,4,4,4,4,4,4))
        rows += row("Kaçan Hata", s.escaped, "", styles=intArrayOf(4,5,4,4,4,4,4,4))
        rows += row("Yakalanan Hata", s.caught, "", styles=intArrayOf(4,5,4,4,4,4,4,4))
        rows += row("Devamsızlık", s.absence, "Devam puanı: ${fmt(s.attendance)}", styles=intArrayOf(4,5,4,4,4,4,4,4))
        rows += row("Yıllık İzin", s.annualLeave, "gün", styles=intArrayOf(4,5,4,4,4,4,4,4))
        rows += row("Günlük İzin", s.dailyLeave, "gün", styles=intArrayOf(4,5,4,4,4,4,4,4))
        rows += row("Rapor", s.report, "gün", styles=intArrayOf(4,5,4,4,4,4,4,4))
        rows += blankRow(8)
        rows += header("Ay","Puan","Sınıf","KY","Kaizen","Kaçan","Yakalanan","Mesai")
        var month = YearMonth.from(periodStart4)
        val end = YearMonth.from(periodEnd4)
        while (!month.isAfter(end)) {
            val m = calculateMonthConfigured4(op, records, month, settings)
            rows += listOf(
                c(monthLabel(month)), n(if (m.hasData) m.total else 0.0, 6), c(m.grade, gradeStyle(m.grade)),
                n(m.ky), n(m.kaizen), n(m.escaped), n(m.caught), n(m.overtime)
            )
            month = month.plusMonths(1)
        }
        return XlsxSheet7(
            "Personel ${op.sicil}", rows,
            widths = listOf(20.0,12.0,12.0,10.0,11.0,10.0,12.0,10.0),
            merges = listOf("A1:H1","A2:H2"),
            freezeRows = 4,
            landscape = false
        )
    }

    fun monthly(
        records: List<Record4>,
        operators: List<Operator4>,
        settings: ScoringSettings4,
        month: YearMonth
    ): XlsxSheet7 {
        val active = operators.filter { it.active }
        val rows = mutableListOf<List<XlsxCell7>>()
        rows += titleRow("AYLIK OPERATÖR ANALİZİ • ${monthLabel(month)}", 14)
        rows += subtitleRow("KY, Kaizen, hata, mesai, izin ve aylık performans puanı.", 14)
        rows += blankRow(14)
        val headerRow = rows.size + 1
        rows += header("Sicil","Ad Soyad","Kayıt","Puan","Sınıf","KY","Kaizen","Yakalanan","Kaçan","Mesai","Yıllık İzin","Günlük İzin","Rapor","Devamsızlık")
        active.forEach { op ->
            val s = calculateMonthConfigured4(op, records, month, settings)
            rows += listOf(
                c(op.sicil), c(op.name), n(recordsForMonth4(records, op.sicil, month).size.toDouble()),
                n(if (s.hasData) s.total else 0.0, 6), c(s.grade, gradeStyle(s.grade)),
                n(s.ky), n(s.kaizen), n(s.caught), n(s.escaped), n(s.overtime),
                n(s.annualLeave), n(s.dailyLeave), n(s.report), n(s.absence)
            )
        }
        return XlsxSheet7(
            "Aylık ${month.year}-${"%02d".format(month.monthValue)}", rows,
            widths = listOf(10.0,26.0,9.0,10.0,8.0,9.0,10.0,11.0,9.0,9.0,12.0,12.0,9.0,12.0),
            merges = listOf("A1:N1","A2:N2"),
            freezeRows = headerRow,
            autoFilterRef = "A$headerRow:N${rows.size}",
            landscape = true
        )
    }

    fun records(records: List<Record4>, operators: List<Operator4>): XlsxSheet7 {
        val names = operators.associate { it.sicil to it.name }
        val sorted = records.sortedByDescending { it.timestamp }
        val rows = mutableListOf<List<XlsxCell7>>()
        rows += titleRow("GÜNLÜK KAYITLAR", 12)
        rows += subtitleRow("Operatör Takip uygulamasındaki tüm kayıtların Excel çıktısı.", 12)
        rows += blankRow(12)
        val headerRow = rows.size + 1
        rows += header("Tarih","Saat","Sicil","Operatör","Kayıt Türü","Hata Türü","Miktar","Birim","Parça / Kalıp","Makine","Açıklama","Fotoğraf")
        sorted.forEach { r ->
            val dt = Instant.ofEpochMilli(r.timestamp).atZone(ZoneId.systemDefault())
            rows += listOf(
                c(dt.toLocalDate().format(dateFmt)), c(dt.toLocalTime().format(timeFmt)),
                c(r.operatorSicil), c(names[r.operatorSicil] ?: r.operatorSicil), c(r.type),
                c(r.defect), n(r.amount), c(unit(r.type)), c(r.part), c(r.machine), c(r.note),
                c(if (r.photoPath.isNotBlank()) "VAR" else "YOK", if (r.photoPath.isNotBlank()) 7 else 10)
            )
        }
        return XlsxSheet7(
            "Kayıtlar", rows,
            widths = listOf(13.0,9.0,10.0,25.0,17.0,17.0,10.0,9.0,35.0,15.0,38.0,11.0),
            merges = listOf("A1:L1","A2:L2"),
            freezeRows = headerRow,
            autoFilterRef = "A$headerRow:L${rows.size}",
            landscape = true
        )
    }

    fun defects(refs: Map<String,String>): XlsxSheet7 {
        val rows = mutableListOf<List<XlsxCell7>>()
        rows += titleRow("HATA KÜTÜPHANESİ", 4)
        rows += subtitleRow("Hata katsayıları ve standart açıklamaları.", 4)
        rows += blankRow(4)
        rows += header("Hata Türü","Katsayı","Fotoğraf","Açıklama")
        defectWeights4.forEach { (name, weight) ->
            rows += listOf(c(name), n(weight), c(if (!refs[name].isNullOrBlank()) "VAR" else "YOK"), c(defectHelp4[name] ?: ""))
        }
        return XlsxSheet7(
            "Hata Kütüphanesi", rows,
            widths = listOf(22.0,10.0,12.0,52.0),
            merges = listOf("A1:D1","A2:D2"),
            freezeRows = 4,
            autoFilterRef = "A4:D${rows.size}",
            landscape = false
        )
    }

    fun management(operators: List<Operator4>, machines: List<String>, parts: List<Part4>): XlsxSheet7 {
        val rows = mutableListOf<List<XlsxCell7>>()
        rows += titleRow("YÖNETİM LİSTELERİ", 4)
        rows += subtitleRow("Operatör, makine ve parça/kalıp listeleri.", 4)
        rows += blankRow(4)
        rows += header("OPERATÖRLER","","","")
        rows += header("Sicil","Ad Soyad","Durum","")
        operators.forEach { rows += row(it.sicil, it.name, if (it.active) "AKTİF" else "PASİF", "", styles=intArrayOf(4,4,if(it.active)7 else 9,4)) }
        rows += blankRow(4)
        rows += header("MAKİNELER","","","")
        rows += header("Makine","","","")
        machines.forEach { rows += row(it,"","","") }
        rows += blankRow(4)
        rows += header("PARÇA / KALIP","MAKİNE","","")
        parts.forEach { rows += row(it.name,it.machine,"","") }
        return XlsxSheet7(
            "Yönetim", rows,
            widths = listOf(38.0,25.0,14.0,14.0),
            merges = listOf("A1:D1","A2:D2"),
            landscape = false
        )
    }

    fun scoring(settings: ScoringSettings4): XlsxSheet7 {
        val rows = mutableListOf<List<XlsxCell7>>()
        rows += titleRow("PUANLAMA AYARLARI", 3)
        rows += subtitleRow("Uygulamadaki aktif puanlama sistemi.", 3)
        rows += blankRow(3)
        rows += header("Ayar","Değer","Açıklama")
        rows += row("Kalite maksimum puan", settings.qualityMax, "")
        rows += row("KY maksimum puan", settings.kyMax, "")
        rows += row("Kaizen maksimum puan", settings.kaizenMax, "")
        rows += row("Devam maksimum puan", settings.attendanceMax, "")
        rows += row("Mesai maksimum puan", settings.overtimeMax, "")
        rows += row("Kalite başlangıç puanı", settings.qualityStart, "")
        rows += row("Kaçan hata cezası × katsayı", settings.escapedPenalty, "")
        rows += row("Yakalanan hata bonusu × katsayı", settings.caughtBonus, "")
        rows += row("Yakalanan hata bonus tavanı", settings.caughtBonusCap, "")
        rows += row("Aylık KY hedefi", settings.kyMonthlyTarget, "adet")
        rows += row("Aylık Kaizen hedefi", settings.kaizenMonthlyTarget, "adet")
        rows += row("Aylık mesai hedefi", settings.overtimeMonthlyTarget, "saat")
        rows += row("Devamsızlık cezası / gün", settings.absencePenaltyPerDay, "")
        rows += row("A sınıfı alt sınır", settings.gradeA, "")
        rows += row("B sınıfı alt sınır", settings.gradeB, "")
        rows += row("C sınıfı alt sınır", settings.gradeC, "")
        return XlsxSheet7(
            "Puanlama", rows,
            widths = listOf(38.0,16.0,30.0),
            merges = listOf("A1:C1","A2:C2"),
            freezeRows = 4,
            landscape = false
        )
    }

    fun all(
        records: List<Record4>,
        operators: List<Operator4>,
        machines: List<String>,
        parts: List<Part4>,
        refs: Map<String,String>,
        settings: ScoringSettings4,
        month: YearMonth
    ): List<XlsxSheet7> = listOf(
        dashboard(records, operators, settings),
        monthly(records, operators, settings, month),
        personnel(records, operators, settings, null),
        records(records, operators),
        defects(refs),
        management(operators, machines, parts),
        scoring(settings)
    )

    private fun header(vararg values: String) = values.map { c(it, 3) }
    private fun titleRow(title: String, cols: Int) = listOf(c(title, 1)) + List(cols - 1) { c("", 1) }
    private fun subtitleRow(text: String, cols: Int) = listOf(c(text, 2)) + List(cols - 1) { c("", 2) }
    private fun blankRow(cols: Int) = List(cols) { c("", 0) }

    private fun row(vararg values: Any?, styles: IntArray? = null): List<XlsxCell7> =
        values.mapIndexed { index, v ->
            val style = styles?.getOrNull(index) ?: when (v) {
                is Number -> 5
                else -> 4
            }
            XlsxCell7(v, style)
        }

    private fun c(v: Any?, style: Int = 4) = XlsxCell7(v, style)
    private fun n(v: Double, style: Int = 5) = XlsxCell7(v, style)

    private fun gradeStyle(grade: String) = when (grade) {
        "A" -> 7
        "D" -> 8
        "B", "C" -> 9
        else -> 10
    }

    private fun unit(type: String) = when (type) {
        "Mesai" -> "saat"
        "Yıllık İzin","Günlük İzin","Rapor","Devamsızlık" -> "gün"
        else -> "adet"
    }

    private fun monthLabel(month: YearMonth): String =
        month.format(monthFmt).replaceFirstChar { it.uppercase(tr) }

    private fun fmt(v: Double) = if (v % 1.0 == 0.0) v.toInt().toString() else "%.1f".format(tr, v)
}
