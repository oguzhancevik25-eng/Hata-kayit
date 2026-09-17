package com.oguzhan.hatakayit

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min

data class Operator4(val sicil: String, val name: String, val active: Boolean = true)
data class Part4(val name: String, val machine: String)
data class Record4(
    val id: Long,
    val timestamp: Long,
    val operatorSicil: String,
    val type: String,
    val defect: String,
    val amount: Double,
    val machine: String,
    val part: String,
    val note: String,
    val photoPath: String = ""
)
data class Score4(
    val hasData: Boolean,
    val escaped: Double,
    val caught: Double,
    val ky: Double,
    val kaizen: Double,
    val overtime: Double,
    val annualLeave: Double,
    val dailyLeave: Double,
    val report: Double,
    val absence: Double,
    val quality: Double,
    val kyScore: Double,
    val kaizenScore: Double,
    val attendance: Double,
    val overtimeScore: Double,
    val total: Double,
    val grade: String
)

val periodStart4 = LocalDate.of(2026, 8, 1)
val periodEnd4 = LocalDate.of(2027, 7, 31)
val defaultOperators4 = listOf(
    Operator4("6095", "NUSRET BULUT"), Operator4("614", "LEVENT DOĞUER"),
    Operator4("6112", "AHMET SEZER"), Operator4("3388", "İLYAS ÖZDEMİR"),
    Operator4("2921", "GİRAY ÇALIŞIR"), Operator4("4975", "SEZGİN NALBATÇI"),
    Operator4("5828", "EREN YİĞİTOĞLU"), Operator4("686", "RUHAN SEVİL TEKEOĞLU"),
    Operator4("596", "FATİH HENDEKÇİ"), Operator4("2484", "MESUT MÜHÜRDAROÇ")
)
val recordTypes4 = listOf("Kaçan Hata", "Yakalanan Hata", "KY", "Kaizen", "Mesai", "Yıllık İzin", "Günlük İzin", "Rapor", "Devamsızlık")
val machines4 = listOf("1600T-1", "1600T-2", "1600T-3", "1700T", "850T", "650T")

val parts4 = listOf(
    Part4("130D FR Upper Resin RH", "1700T"),
    Part4("130D FR Upper TPO RH", "1600T-1"),
    Part4("130D FR Lower RH - DELİKSİZ", "1600T-2"),
    Part4("130D FR Lower RH - DELİKLİ", "1600T-2"),
    Part4("130D FR Lower LH - DELİKSİZ", "1600T-2"),
    Part4("130D FR Lower LH - DELİKLİ", "1600T-2"),
    Part4("130D FR OMT RH", "850T"),
    Part4("130D FR Pocket RH", "1600T-1"),
    Part4("130D RR Upper Base RH", "1700T"),
    Part4("130D RR Lower RH", "1600T-3"),
    Part4("130D RR Lower LH", "1700T"),
    Part4("130D IP LH", "850T"),
    Part4("130D IP RH", "850T"),
    Part4("OUTER SHLD. M6 RHD - SPRT. YOK (EN59)", "650T"),
    Part4("OUTER SHLD. M6 RHD - SPRT. VAR (EN65)", "650T"),
    Part4("OUTER SHLD. M4 A/B'li LHD- SPRT. YOK (EN61)", "650T"),
    Part4("OUTER SHLD. M4 A/B'li LHD- SPRT. VAR (EN62)", "650T"),
    Part4("3ZR CAP", "650T"),
    Part4("FR RH LowerCover", "1700T"),
    Part4("FR LH Lower Cover", "1600T-3"),
    Part4("FR UB RH TPO-DERİ KAPLAMA", "1600T-1"),
    Part4("FR UB RH Lambasız-SİYAH", "1600T-2"),
    Part4("FR UB RH Lambasız-Krem", "1600T-1"),
    Part4("FR UB RH Lambasız-GRİ", "1600T-1"),
    Part4("FR UB RH Lambalı-SİYAH", "1600T-3"),
    Part4("RR UB RH TPO-DERİ KAPLAMA", "1600T-1"),
    Part4("RR UB RH Lambalı-Siyah", "1600T-1"),
    Part4("RR UB RH Lambasız-Siyah", "1600T-3"),
    Part4("RR UB RH Lambasız-Krem", "1600T-3"),
    Part4("RR UB RH Lambasız-GRİ", "1600T-3"),
    Part4("IP LHD", "1600T-1"), Part4("IP RHD", "1600T-1"),
    Part4("2.0 HV CAP", "650T"), Part4("025D ZR-HV CAP", "650T"),
    Part4("025D IP HARD SİYAH", "850T"), Part4("025D IP HARD GREY", "850T"),
    Part4("369 IP 9İNÇ KÜÇÜK RH", "850T"), Part4("369 IP 9İNÇ BÜYÜK RH", "850T"),
    Part4("369 IP 10İNÇ KÜÇÜK RH", "850T"), Part4("369 IP 10İNÇ BÜYÜK RH", "850T"),
    Part4("BACK BOARD BÜYÜK", "850T")
)
val partMachine4 = parts4.associate { it.name to it.machine }

val defectWeights4 = linkedMapOf(
    "Şişme" to 1.5, "Felt Eksik" to 1.5, "Çapak" to 1.0, "Yolluk Kalma" to 3.0,
    "Yolluk Yapışması" to 1.0, "Çökme" to 1.0, "Eksik" to 2.0, "İz" to 1.0,
    "Yabancı Madde" to 1.0, "Hatalı Setleme" to 1.0, "Kabarma" to 1.0,
    "Beyazlık" to 0.3, "Leke" to 0.1, "Deforme" to 0.2, "Diğer" to 1.0
)
val defectHelp4 = mapOf(
    "Şişme" to "Parça yüzeyinde şişme veya kabarma görünümü.",
    "Felt Eksik" to "Parçada olması gereken feltin bulunmaması veya yanlış pozisyonda olması.",
    "Çapak" to "Kenar veya birleşim bölgesinde istenmeyen plastik fazlalığı.",
    "Yolluk Kalma" to "Parça veya kalıp üzerinde yolluk/gate kalıntısı kalması.",
    "Yolluk Yapışması" to "Yolluğun normal ayrılmayıp kalıba veya parçaya yapışması.",
    "Çökme" to "Yüzeyde içeri doğru çökük görünüm.",
    "Eksik" to "Parçanın bir bölgesinin tam dolmaması.",
    "İz" to "Standart dışı çizgi, akış, gate veya itici izi.",
    "Yabancı Madde" to "Farklı renk, nokta veya malzeme dışı görüntü.",
    "Hatalı Setleme" to "Parça, aparat veya ayarın standarda uygun setlenmemesi.",
    "Kabarma" to "Yüzeyde lokal kabarıklık veya yükselti.",
    "Beyazlık" to "Parça yüzeyinde beyazlama veya renk kaybı.",
    "Leke" to "Yüzeyde standart dışı renk, kir veya iz.",
    "Deforme" to "Parça geometrisinde eğilme veya şekil bozukluğu.",
    "Diğer" to "Listede olmayan hata; fotoğraf ve açıklama ile kaydedilir."
)

object Store4 {
    private const val PREFS = "operator_takip_prefs"
    private const val RECORDS = "records"
    private const val REFERENCES = "defect_reference_photos"
    private const val OPERATORS = "operators_v1_2"

    fun loadRecords(context: Context): List<Record4> = try {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(RECORDS, "[]") ?: "[]"
        val a = JSONArray(raw)
        buildList {
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                add(Record4(
                    o.getLong("id"), o.getLong("timestamp"), o.getString("operatorSicil"), o.getString("type"),
                    o.optString("defect"), o.getDouble("amount"), o.optString("machine"), o.optString("part"),
                    o.optString("note"), o.optString("photoPath")
                ))
            }
        }
    } catch (_: Exception) { emptyList() }

    fun saveRecords(context: Context, records: List<Record4>) {
        val a = JSONArray()
        records.forEach { r ->
            a.put(JSONObject().apply {
                put("id", r.id); put("timestamp", r.timestamp); put("operatorSicil", r.operatorSicil); put("type", r.type)
                put("defect", r.defect); put("amount", r.amount); put("machine", r.machine); put("part", r.part)
                put("note", r.note); put("photoPath", r.photoPath)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(RECORDS, a.toString()).apply()
    }

    fun loadReferences(context: Context): MutableMap<String, String> = try {
        val o = JSONObject(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(REFERENCES, "{}") ?: "{}")
        mutableMapOf<String, String>().apply {
            defectWeights4.keys.forEach { d -> o.optString(d).takeIf { it.isNotBlank() }?.let { put(d, it) } }
        }
    } catch (_: Exception) { mutableMapOf() }

    fun saveReferences(context: Context, refs: Map<String, String>) {
        val o = JSONObject(); refs.forEach { (k, v) -> o.put(k, v) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(REFERENCES, o.toString()).apply()
    }

    fun loadOperators(context: Context): List<Operator4> = try {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(OPERATORS, null)
        if (raw.isNullOrBlank()) defaultOperators4 else {
            val a = JSONArray(raw)
            val loaded = buildList {
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    add(Operator4(o.getString("sicil"), o.getString("name"), o.optBoolean("active", true)))
                }
            }
            if (loaded.isEmpty()) defaultOperators4 else loaded
        }
    } catch (_: Exception) { defaultOperators4 }

    fun saveOperators(context: Context, operators: List<Operator4>) {
        val a = JSONArray()
        operators.forEach { op -> a.put(JSONObject().apply { put("sicil", op.sicil); put("name", op.name); put("active", op.active) }) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(OPERATORS, a.toString()).apply()
    }
}

fun activeMonths4(): Int {
    val today = LocalDate.now()
    val capped = when { today.isBefore(periodStart4) -> periodStart4; today.isAfter(periodEnd4) -> periodEnd4; else -> today }
    return (ChronoUnit.MONTHS.between(YearMonth.from(periodStart4), YearMonth.from(capped)).toInt() + 1).coerceIn(1, 12)
}

fun inPeriod4(record: Record4): Boolean {
    val d = Instant.ofEpochMilli(record.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    return !d.isBefore(periodStart4) && !d.isAfter(periodEnd4)
}

fun calculate4(op: Operator4, all: List<Record4>): Score4 {
    val rows = all.filter { it.operatorSicil == op.sicil && inPeriod4(it) }
    fun sum(type: String) = rows.filter { it.type == type }.sumOf { it.amount }
    fun weighted(type: String) = rows.filter { it.type == type }.sumOf { it.amount * (defectWeights4[it.defect] ?: 1.0) }
    val escaped = sum("Kaçan Hata"); val caught = sum("Yakalanan Hata"); val ky = sum("KY")
    val kaizen = sum("Kaizen"); val overtime = sum("Mesai"); val absence = sum("Devamsızlık")
    val months = activeMonths4()
    val quality = (35.0 + min(weighted("Yakalanan Hata") * .5, 5.0) - weighted("Kaçan Hata") * 4.0).coerceIn(0.0, 40.0)
    val kyScore = min(15.0, ky / (months * 6.0) * 15.0)
    val kaizenScore = min(15.0, kaizen / months * 15.0)
    val attendance = max(0.0, 20.0 - absence * 5.0)
    val overtimeScore = min(10.0, overtime / (months * 10.0) * 10.0)
    val total = quality + kyScore + kaizenScore + attendance + overtimeScore
    val grade = when { total >= 90 -> "A"; total >= 80 -> "B"; total >= 70 -> "C"; else -> "D" }
    return Score4(
        rows.isNotEmpty(), escaped, caught, ky, kaizen, overtime,
        sum("Yıllık İzin"), sum("Günlük İzin"), sum("Rapor"), absence,
        quality, kyScore, kaizenScore, attendance, overtimeScore,
        if (rows.isEmpty()) 0.0 else total, if (rows.isEmpty()) "—" else grade
    )
}
