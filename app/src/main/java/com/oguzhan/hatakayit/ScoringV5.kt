package com.oguzhan.hatakayit

import android.content.Context
import org.json.JSONObject
import java.time.YearMonth
import kotlin.math.max
import kotlin.math.min

data class ScoringSettings4(
    val qualityMax: Double = 40.0,
    val kyMax: Double = 15.0,
    val kaizenMax: Double = 15.0,
    val attendanceMax: Double = 20.0,
    val overtimeMax: Double = 10.0,
    val qualityStart: Double = 35.0,
    val escapedPenalty: Double = 4.0,
    val caughtBonus: Double = 0.5,
    val caughtBonusCap: Double = 5.0,
    val kyMonthlyTarget: Double = 6.0,
    val kaizenMonthlyTarget: Double = 1.0,
    val overtimeMonthlyTarget: Double = 10.0,
    val absencePenaltyPerDay: Double = 5.0,
    val gradeA: Double = 90.0,
    val gradeB: Double = 80.0,
    val gradeC: Double = 70.0
) {
    val maxTotal: Double
        get() = qualityMax + kyMax + kaizenMax + attendanceMax + overtimeMax
}

val defaultScoringSettings4 = ScoringSettings4()

private const val SCORING_PREFS = "operator_takip_prefs"
private const val SCORING_KEY = "scoring_settings_v1_5"

fun loadScoringSettings4(context: Context): ScoringSettings4 = try {
    val raw = context.getSharedPreferences(SCORING_PREFS, Context.MODE_PRIVATE).getString(SCORING_KEY, null)
    if (raw.isNullOrBlank()) defaultScoringSettings4 else {
        val o = JSONObject(raw)
        ScoringSettings4(
            qualityMax = o.optDouble("qualityMax", 40.0),
            kyMax = o.optDouble("kyMax", 15.0),
            kaizenMax = o.optDouble("kaizenMax", 15.0),
            attendanceMax = o.optDouble("attendanceMax", 20.0),
            overtimeMax = o.optDouble("overtimeMax", 10.0),
            qualityStart = o.optDouble("qualityStart", 35.0),
            escapedPenalty = o.optDouble("escapedPenalty", 4.0),
            caughtBonus = o.optDouble("caughtBonus", 0.5),
            caughtBonusCap = o.optDouble("caughtBonusCap", 5.0),
            kyMonthlyTarget = o.optDouble("kyMonthlyTarget", 6.0),
            kaizenMonthlyTarget = o.optDouble("kaizenMonthlyTarget", 1.0),
            overtimeMonthlyTarget = o.optDouble("overtimeMonthlyTarget", 10.0),
            absencePenaltyPerDay = o.optDouble("absencePenaltyPerDay", 5.0),
            gradeA = o.optDouble("gradeA", 90.0),
            gradeB = o.optDouble("gradeB", 80.0),
            gradeC = o.optDouble("gradeC", 70.0)
        )
    }
} catch (_: Exception) {
    defaultScoringSettings4
}

fun saveScoringSettings4(context: Context, s: ScoringSettings4) {
    val o = JSONObject().apply {
        put("qualityMax", s.qualityMax)
        put("kyMax", s.kyMax)
        put("kaizenMax", s.kaizenMax)
        put("attendanceMax", s.attendanceMax)
        put("overtimeMax", s.overtimeMax)
        put("qualityStart", s.qualityStart)
        put("escapedPenalty", s.escapedPenalty)
        put("caughtBonus", s.caughtBonus)
        put("caughtBonusCap", s.caughtBonusCap)
        put("kyMonthlyTarget", s.kyMonthlyTarget)
        put("kaizenMonthlyTarget", s.kaizenMonthlyTarget)
        put("overtimeMonthlyTarget", s.overtimeMonthlyTarget)
        put("absencePenaltyPerDay", s.absencePenaltyPerDay)
        put("gradeA", s.gradeA)
        put("gradeB", s.gradeB)
        put("gradeC", s.gradeC)
    }
    context.getSharedPreferences(SCORING_PREFS, Context.MODE_PRIVATE)
        .edit().putString(SCORING_KEY, o.toString()).apply()
}

private fun calculateConfiguredRows4(
    rows: List<Record4>,
    targetMonths: Int,
    s: ScoringSettings4
): Score4 {
    fun sum(type: String) = rows.filter { it.type == type }.sumOf { it.amount }
    fun weighted(type: String) = rows.filter { it.type == type }
        .sumOf { it.amount * (defectWeights4[it.defect] ?: 1.0) }

    val escaped = sum("Kaçan Hata")
    val caught = sum("Yakalanan Hata")
    val ky = sum("KY")
    val kaizen = sum("Kaizen")
    val overtime = sum("Mesai")
    val absence = sum("Devamsızlık")
    val months = targetMonths.coerceAtLeast(1)

    val qualityStart = s.qualityStart.coerceIn(0.0, s.qualityMax)
    val quality = (
        qualityStart +
            min(weighted("Yakalanan Hata") * s.caughtBonus, s.caughtBonusCap) -
            weighted("Kaçan Hata") * s.escapedPenalty
        ).coerceIn(0.0, s.qualityMax)

    val kyTarget = (months * s.kyMonthlyTarget).coerceAtLeast(0.0001)
    val kaizenTarget = (months * s.kaizenMonthlyTarget).coerceAtLeast(0.0001)
    val overtimeTarget = (months * s.overtimeMonthlyTarget).coerceAtLeast(0.0001)

    val kyScore = min(s.kyMax, ky / kyTarget * s.kyMax)
    val kaizenScore = min(s.kaizenMax, kaizen / kaizenTarget * s.kaizenMax)
    val attendance = max(0.0, s.attendanceMax - absence * s.absencePenaltyPerDay)
    val overtimeScore = min(s.overtimeMax, overtime / overtimeTarget * s.overtimeMax)

    val rawTotal = quality + kyScore + kaizenScore + attendance + overtimeScore
    val maxTotal = s.maxTotal.coerceAtLeast(0.0001)
    val normalizedTotal = (rawTotal / maxTotal * 100.0).coerceIn(0.0, 100.0)

    val grade = when {
        normalizedTotal >= s.gradeA -> "A"
        normalizedTotal >= s.gradeB -> "B"
        normalizedTotal >= s.gradeC -> "C"
        else -> "D"
    }

    return Score4(
        rows.isNotEmpty(),
        escaped,
        caught,
        ky,
        kaizen,
        overtime,
        sum("Yıllık İzin"),
        sum("Günlük İzin"),
        sum("Rapor"),
        absence,
        quality,
        kyScore,
        kaizenScore,
        attendance,
        overtimeScore,
        if (rows.isEmpty()) 0.0 else normalizedTotal,
        if (rows.isEmpty()) "—" else grade
    )
}

fun calculateConfigured4(
    op: Operator4,
    all: List<Record4>,
    settings: ScoringSettings4
): Score4 = calculateConfiguredRows4(
    all.filter { it.operatorSicil == op.sicil && inPeriod4(it) },
    activeMonths4(),
    settings
)

fun calculateMonthConfigured4(
    op: Operator4,
    all: List<Record4>,
    month: YearMonth,
    settings: ScoringSettings4
): Score4 = calculateConfiguredRows4(
    recordsForMonth4(all, op.sicil, month),
    1,
    settings
)
