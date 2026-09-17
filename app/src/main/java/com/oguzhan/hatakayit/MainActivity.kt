package com.oguzhan.hatakayit

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

private val periodStart: LocalDate = LocalDate.of(2026, 8, 1)
private val periodEnd: LocalDate = LocalDate.of(2027, 7, 31)

private data class Operator(val sicil: String, val name: String)
private data class PerfRecord(
    val id: Long,
    val timestamp: Long,
    val operatorSicil: String,
    val type: String,
    val defect: String,
    val amount: Double,
    val machine: String,
    val part: String,
    val note: String
)

private data class Summary(
    val hasData: Boolean,
    val escaped: Double,
    val escapedWeighted: Double,
    val caught: Double,
    val caughtWeighted: Double,
    val ky: Double,
    val kaizen: Double,
    val overtime: Double,
    val annualLeave: Double,
    val dailyLeave: Double,
    val report: Double,
    val absence: Double,
    val qualityScore: Double,
    val kyScore: Double,
    val kaizenScore: Double,
    val attendanceScore: Double,
    val overtimeScore: Double,
    val total: Double,
    val grade: String
)

private val operators = listOf(
    Operator("6095", "NUSRET BULUT"),
    Operator("614", "LEVENT DOĞUER"),
    Operator("6112", "AHMET SEZER"),
    Operator("3388", "İLYAS ÖZDEMİR"),
    Operator("2921", "GİRAY ÇALIŞIR"),
    Operator("4975", "SEZGİN NALBATÇI"),
    Operator("5828", "EREN YİĞİTOĞLU"),
    Operator("686", "RUHAN SEVİL TEKEOĞLU"),
    Operator("596", "FATİH HENDEKÇİ"),
    Operator("2484", "MESUT MÜHÜRDAROÇ")
)

private val recordTypes = listOf(
    "Kaçan Hata", "Yakalanan Hata", "KY", "Kaizen", "Mesai",
    "Yıllık İzin", "Günlük İzin", "Rapor", "Devamsızlık"
)

private val defectWeights = linkedMapOf(
    "Şişme" to 1.5,
    "Felt Eksik" to 1.5,
    "Çapak" to 1.0,
    "Yolluk Kalma" to 3.0,
    "Yolluk Yapışması" to 1.0,
    "Çökme" to 1.0,
    "Eksik" to 2.0,
    "İz" to 1.0,
    "Yabancı Madde" to 1.0,
    "Hatalı Setleme" to 1.0,
    "Kabarma" to 1.0,
    "Beyazlık" to 0.3,
    "Leke" to 0.1,
    "Deforme" to 0.2,
    "Diğer" to 1.0
)

private object RecordStore {
    private const val PREFS = "operator_takip_prefs"
    private const val KEY = "records"

    fun load(context: Context): List<PerfRecord> {
        return try {
            val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY, "[]") ?: "[]"
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(
                        PerfRecord(
                            id = o.getLong("id"),
                            timestamp = o.getLong("timestamp"),
                            operatorSicil = o.getString("operatorSicil"),
                            type = o.getString("type"),
                            defect = o.optString("defect"),
                            amount = o.getDouble("amount"),
                            machine = o.optString("machine"),
                            part = o.optString("part"),
                            note = o.optString("note")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, records: List<PerfRecord>) {
        val array = JSONArray()
        records.forEach { r ->
            array.put(JSONObject().apply {
                put("id", r.id)
                put("timestamp", r.timestamp)
                put("operatorSicil", r.operatorSicil)
                put("type", r.type)
                put("defect", r.defect)
                put("amount", r.amount)
                put("machine", r.machine)
                put("part", r.part)
                put("note", r.note)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, array.toString()).apply()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OperatorTrackingApp() }
    }
}

@Composable
private fun OperatorTrackingApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val records = remember {
        mutableStateListOf<PerfRecord>().apply { addAll(RecordStore.load(context)) }
    }
    var tab by remember { mutableIntStateOf(0) }
    var selectedOperator by remember { mutableStateOf(operators.first()) }

    val colors = lightColorScheme(
        primary = Color(0xFF0B4F7D),
        secondary = Color(0xFF0E7490),
        tertiary = Color(0xFF15803D),
        error = Color(0xFFB91C1C),
        background = Color(0xFFF5F7FA),
        surface = Color.White
    )

    MaterialTheme(colorScheme = colors) {
        Scaffold(
            topBar = {
                Surface(shadowElevation = 3.dp, color = MaterialTheme.colorScheme.primary) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp)) {
                        Text("Operatör Takip", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text("01.08.2026 — 31.07.2027", color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)
                    }
                }
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Text("⌂") }, label = { Text("Dashboard") })
                    NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Text("+") }, label = { Text("Kayıt") })
                    NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Text("◎") }, label = { Text("Personel") })
                    NavigationBarItem(selected = tab == 3, onClick = { tab = 3 }, icon = { Text("⚙") }, label = { Text("Ayarlar") })
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                when (tab) {
                    0 -> DashboardScreen(records) { op -> selectedOperator = op; tab = 2 }
                    1 -> EntryScreen(records) { RecordStore.save(context, records) }
                    2 -> PersonScreen(records, selectedOperator, { selectedOperator = it }) { record ->
                        records.remove(record)
                        RecordStore.save(context, records)
                    }
                    else -> SettingsScreen()
                }
            }
        }
    }
}

private fun activeMonths(): Int {
    val today = LocalDate.now()
    val capped = when {
        today.isBefore(periodStart) -> periodStart
        today.isAfter(periodEnd) -> periodEnd
        else -> today
    }
    return (ChronoUnit.MONTHS.between(
        YearMonth.from(periodStart), YearMonth.from(capped)
    ).toInt() + 1).coerceIn(1, 12)
}

private fun dateOf(timestamp: Long): LocalDate =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()

private fun inPeriod(r: PerfRecord): Boolean {
    val d = dateOf(r.timestamp)
    return !d.isBefore(periodStart) && !d.isAfter(periodEnd)
}

private fun calculateSummary(operator: Operator, all: List<PerfRecord>, targetMonths: Int = activeMonths()): Summary {
    val records = all.filter { it.operatorSicil == operator.sicil && inPeriod(it) }
    fun total(type: String) = records.filter { it.type == type }.sumOf { it.amount }
    fun weighted(type: String) = records.filter { it.type == type }.sumOf {
        it.amount * (defectWeights[it.defect] ?: 1.0)
    }

    val escaped = total("Kaçan Hata")
    val escapedWeighted = weighted("Kaçan Hata")
    val caught = total("Yakalanan Hata")
    val caughtWeighted = weighted("Yakalanan Hata")
    val ky = total("KY")
    val kaizen = total("Kaizen")
    val overtime = total("Mesai")
    val annualLeave = total("Yıllık İzin")
    val dailyLeave = total("Günlük İzin")
    val report = total("Rapor")
    val absence = total("Devamsızlık")

    val quality = (35.0 + min(caughtWeighted * 0.5, 5.0) - escapedWeighted * 4.0).coerceIn(0.0, 40.0)
    val kyScore = min(15.0, if (targetMonths > 0) ky / (targetMonths * 6.0) * 15.0 else 0.0)
    val kaizenScore = min(15.0, if (targetMonths > 0) kaizen / targetMonths * 15.0 else 0.0)
    val attendance = max(0.0, 20.0 - absence * 5.0)
    val overtimeScore = min(10.0, if (targetMonths > 0) overtime / (targetMonths * 10.0) * 10.0 else 0.0)
    val totalScore = quality + kyScore + kaizenScore + attendance + overtimeScore
    val grade = when {
        totalScore >= 90 -> "A"
        totalScore >= 80 -> "B"
        totalScore >= 70 -> "C"
        else -> "D"
    }

    return Summary(
        hasData = records.isNotEmpty(),
        escaped = escaped,
        escapedWeighted = escapedWeighted,
        caught = caught,
        caughtWeighted = caughtWeighted,
        ky = ky,
        kaizen = kaizen,
        overtime = overtime,
        annualLeave = annualLeave,
        dailyLeave = dailyLeave,
        report = report,
        absence = absence,
        qualityScore = quality,
        kyScore = kyScore,
        kaizenScore = kaizenScore,
        attendanceScore = attendance,
        overtimeScore = overtimeScore,
        total = if (records.isEmpty()) 0.0 else totalScore,
        grade = if (records.isEmpty()) "—" else grade
    )
}

@Composable
private fun DashboardScreen(records: List<PerfRecord>, openPerson: (Operator) -> Unit) {
    val summaries = operators.map { it to calculateSummary(it, records) }
    val withData = summaries.filter { it.second.hasData }
    val average = if (withData.isEmpty()) 0.0 else withData.map { it.second.total }.average()
    val totalKy = summaries.sumOf { it.second.ky }
    val totalKaizen = summaries.sumOf { it.second.kaizen }
    val escaped = summaries.sumOf { it.second.escaped }
    val caught = summaries.sumOf { it.second.caught }
    val follow = withData.count { it.second.grade == "C" || it.second.grade == "D" }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Yönetici Dashboard", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Aktif hedef süresi: ${activeMonths()} ay", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("Ortalama Puan", fmt(average), Modifier.weight(1f), Color(0xFFE0F2FE))
                MetricCard("Takip Gereken", follow.toString(), Modifier.weight(1f), Color(0xFFFFE4E6))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("Toplam KY", fmt0(totalKy), Modifier.weight(1f), Color(0xFFDCFCE7))
                MetricCard("Toplam Kaizen", fmt0(totalKaizen), Modifier.weight(1f), Color(0xFFFEF3C7))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("Kaçan Hata", fmt0(escaped), Modifier.weight(1f), Color(0xFFFEE2E2))
                MetricCard("Yakalanan Hata", fmt0(caught), Modifier.weight(1f), Color(0xFFDCFCE7))
            }
        }
        item {
            Spacer(Modifier.height(4.dp))
            Text("Personel Performansı", fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
        items(summaries) { (op, s) ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { openPerson(op) },
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(op.name, fontWeight = FontWeight.Bold)
                        Text("Sicil ${op.sicil} • KY ${fmt0(s.ky)} • Kaizen ${fmt0(s.kaizen)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (s.hasData) {
                        Text(fmt(s.total), fontSize = 23.sp, fontWeight = FontWeight.Bold, color = scoreColor(s.total))
                        Spacer(Modifier.width(10.dp))
                        GradeBadge(s.grade)
                    } else {
                        Text("Veri yok", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryScreen(records: SnapshotStateList<PerfRecord>, onSaved: () -> Unit) {
    var operator by remember { mutableStateOf(operators.first()) }
    var type by remember { mutableStateOf(recordTypes.first()) }
    var defect by remember { mutableStateOf(defectWeights.keys.first()) }
    var amount by remember { mutableStateOf("1") }
    var machine by remember { mutableStateOf("1600T-1") }
    var part by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val isError = type == "Kaçan Hata" || type == "Yakalanan Hata"

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Günlük Kayıt", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Kayıtlar telefonda çevrimdışı saklanır.", color = MaterialTheme.colorScheme.onSurfaceVariant)

        DropdownSelector("Operatör", operators, operator, { it.name }) { operator = it }
        DropdownSelector("Kayıt Türü", recordTypes, type, { it }) { type = it }
        if (isError) {
            DropdownSelector("Hata Türü", defectWeights.keys.toList(), defect, { "$it  ×${defectWeights[it]}" }) { defect = it }
        }

        OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text(if (type == "Mesai") "Saat" else if (type.contains("İzin") || type == "Rapor" || type == "Devamsızlık") "Gün" else "Miktar") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = machine, onValueChange = { machine = it }, label = { Text("Makine") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = part, onValueChange = { part = it }, label = { Text("Model / Parça") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Açıklama") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

        Button(
            onClick = {
                val n = amount.replace(',', '.').toDoubleOrNull() ?: 0.0
                if (n <= 0.0) {
                    message = "Miktar 0'dan büyük olmalı."
                } else {
                    records.add(
                        PerfRecord(
                            id = System.currentTimeMillis(),
                            timestamp = System.currentTimeMillis(),
                            operatorSicil = operator.sicil,
                            type = type,
                            defect = if (isError) defect else "",
                            amount = n,
                            machine = machine.trim(),
                            part = part.trim(),
                            note = note.trim()
                        )
                    )
                    onSaved()
                    message = "✓ Kayıt kaydedildi"
                    note = ""
                    amount = "1"
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("KAYDI KAYDET", fontWeight = FontWeight.Bold) }

        if (message.isNotEmpty()) {
            Text(message, color = if (message.startsWith("✓")) Color(0xFF15803D) else MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
        }

        HorizontalDivider()
        Text("Son Kayıtlar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        records.takeLast(5).reversed().forEach { r ->
            val op = operators.firstOrNull { it.sicil == r.operatorSicil }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("${op?.name ?: r.operatorSicil} • ${r.type}", fontWeight = FontWeight.Bold)
                    Text("${fmt0(r.amount)} ${unitFor(r.type)}${if (r.defect.isNotBlank()) " • ${r.defect}" else ""}", fontSize = 13.sp)
                    Text(formatDate(r.timestamp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun PersonScreen(records: List<PerfRecord>, selected: Operator, select: (Operator) -> Unit, delete: (PerfRecord) -> Unit) {
    val s = calculateSummary(selected, records)
    val own = records.filter { it.operatorSicil == selected.sicil && inPeriod(it) }.sortedByDescending { it.timestamp }
    val components = listOf(
        "Kalite" to (s.qualityScore / 40.0),
        "KY" to (s.kyScore / 15.0),
        "Kaizen" to (s.kaizenScore / 15.0),
        "Devam" to (s.attendanceScore / 20.0),
        "Mesai" to (s.overtimeScore / 10.0)
    )
    val strongest = components.maxByOrNull { it.second }?.first ?: "—"
    val development = components.minByOrNull { it.second }?.first ?: "—"

    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            DropdownSelector("Operatör", operators, selected, { "${it.sicil} - ${it.name}" }) { select(it) }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(selected.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Sicil: ${selected.sicil}")
                    }
                    if (s.hasData) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${fmt(s.total)} / 100", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = scoreColor(s.total))
                            GradeBadge(s.grade)
                        }
                    } else Text("Veri yok")
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("KY", fmt0(s.ky), Modifier.weight(1f), Color(0xFFDCFCE7))
                MetricCard("Kaizen", fmt0(s.kaizen), Modifier.weight(1f), Color(0xFFFEF3C7))
                MetricCard("Mesai", "${fmt0(s.overtime)} s", Modifier.weight(1f), Color(0xFFE0E7FF))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("Kaçan", fmt0(s.escaped), Modifier.weight(1f), Color(0xFFFEE2E2))
                MetricCard("Yakalanan", fmt0(s.caught), Modifier.weight(1f), Color(0xFFDCFCE7))
                MetricCard("Devamsızlık", fmt0(s.absence), Modifier.weight(1f), Color(0xFFF3F4F6))
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Puan Bileşenleri", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    ScoreRow("Kalite", s.qualityScore, 40.0)
                    ScoreRow("KY", s.kyScore, 15.0)
                    ScoreRow("Kaizen", s.kaizenScore, 15.0)
                    ScoreRow("Devam", s.attendanceScore, 20.0)
                    ScoreRow("Mesai / Katılım", s.overtimeScore, 10.0)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("Aylık Puan Trendi", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    MonthlyChart(selected, records)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("Güçlü Alan", if (s.hasData) strongest else "—", Modifier.weight(1f), Color(0xFFDCFCE7))
                MetricCard("Gelişim Alanı", if (s.hasData) development else "—", Modifier.weight(1f), Color(0xFFFFE4E6))
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("Performans Görüşmesi Özeti", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    if (!s.hasData) {
                        Text("Bu dönem için henüz kayıt bulunmuyor.")
                    } else {
                        Text("${selected.name}; KY ${fmt0(s.ky)}, Kaizen ${fmt0(s.kaizen)}, kaçan hata ${fmt0(s.escaped)}, yakalanan hata ${fmt0(s.caught)}, mesai ${fmt0(s.overtime)} saat, yıllık izin ${fmt0(s.annualLeave)} gün, günlük izin ${fmt0(s.dailyLeave)} gün, rapor ${fmt0(s.report)} gün ve devamsızlık ${fmt0(s.absence)} gün. Toplam performans puanı ${fmt(s.total)}/100, sınıfı ${s.grade}.")
                    }
                }
            }
        }
        item { Text("Kayıt Geçmişi", fontSize = 19.sp, fontWeight = FontWeight.Bold) }
        if (own.isEmpty()) item { Text("Kayıt yok.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(own, key = { it.id }) { r ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r.type, fontWeight = FontWeight.Bold)
                        Text("${fmt0(r.amount)} ${unitFor(r.type)}${if (r.defect.isNotBlank()) " • ${r.defect}" else ""}", fontSize = 13.sp)
                        if (r.machine.isNotBlank() || r.part.isNotBlank()) Text("${r.machine} ${r.part}".trim(), fontSize = 12.sp)
                        if (r.note.isNotBlank()) Text(r.note, fontSize = 12.sp)
                        Text(formatDate(r.timestamp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { delete(r) }) { Text("Sil", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
private fun MonthlyChart(operator: Operator, records: List<PerfRecord>) {
    val months = (0 until 12).map { YearMonth.of(2026, 8).plusMonths(it.toLong()) }
    val monthNames = listOf("Ağu", "Eyl", "Eki", "Kas", "Ara", "Oca", "Şub", "Mar", "Nis", "May", "Haz", "Tem")
    val scores = months.map { ym ->
        val monthRecords = records.filter { r ->
            r.operatorSicil == operator.sicil && YearMonth.from(dateOf(r.timestamp)) == ym
        }
        calculateSummary(operator, monthRecords, 1).let { if (monthRecords.isEmpty()) 0.0 else it.total }
    }
    Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
        scores.forEachIndexed { index, score ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                if (score > 0) Text(fmt0(score), fontSize = 8.sp)
                Box(
                    Modifier
                        .fillMaxWidth(0.65f)
                        .height(max(3.0, score * 1.05).dp)
                        .background(if (score > 0) scoreColor(score) else Color(0xFFE5E7EB), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                )
                Text(monthNames[index], fontSize = 8.sp)
            }
        }
    }
}

@Composable
private fun SettingsScreen() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Ayarlar ve Puanlama", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        InfoCard("Performans Dönemi", "01.08.2026 — 31.07.2027")
        InfoCard("Kalite • 40 puan", "Başlangıç 35 puan. Kaçan hata: ağırlıklı hata × 4 ceza. Yakalanan hata: ağırlıklı hata × 0,5 bonus; bonus en fazla 5 puan.")
        InfoCard("KY • 15 puan", "Aylık hedef 6 KY. Hedef, geçen aktif ay sayısına göre otomatik büyür.")
        InfoCard("Kaizen • 15 puan", "Aylık hedef 1 Kaizen.")
        InfoCard("Devam • 20 puan", "Her devamsızlık günü 5 puan düşürür. Yıllık izin, günlük izin ve rapor bilgi amaçlı takip edilir; varsayılan olarak puan düşürmez.")
        InfoCard("Mesai / Katılım • 10 puan", "Aylık hedef 10 saat.")
        InfoCard("Sınıflar", "A: 90–100  •  B: 80–89,9  •  C: 70–79,9  •  D: 70 altı")
        Text("Veriler yalnızca bu telefonda saklanır. Uygulamayı silmeden önce ileride ekleyeceğimiz yedekleme/dışa aktarma özelliğini kullanmak gerekir.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

@Composable
private fun <T> DropdownSelector(label: String, options: List<T>, selected: T, text: (T) -> String, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
                Text(text(selected), modifier = Modifier.weight(1f))
                Text("▼")
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
                options.forEach { item ->
                    DropdownMenuItem(text = { Text(text(item)) }, onClick = { onSelect(item); open = false })
                }
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier, color: Color = Color.White) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color)) {
        Column(Modifier.padding(12.dp)) {
            Text(title, fontSize = 11.sp, color = Color(0xFF475569))
            Text(value, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(body)
        }
    }
}

@Composable
private fun ScoreRow(name: String, score: Double, maxScore: Double) {
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text(name, Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Text("${fmt(score)} / ${fmt0(maxScore)}")
        }
        LinearProgressIndicator(progress = (score / maxScore).toFloat().coerceIn(0f, 1f), modifier = Modifier.fillMaxWidth().height(7.dp))
    }
}

@Composable
private fun GradeBadge(grade: String) {
    val color = when (grade) {
        "A" -> Color(0xFF16A34A)
        "B" -> Color(0xFF65A30D)
        "C" -> Color(0xFFF59E0B)
        else -> Color(0xFFDC2626)
    }
    Surface(color = color, shape = RoundedCornerShape(8.dp)) {
        Text(grade, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
    }
}

private fun scoreColor(score: Double): Color = when {
    score >= 90 -> Color(0xFF15803D)
    score >= 80 -> Color(0xFF4D7C0F)
    score >= 70 -> Color(0xFFD97706)
    else -> Color(0xFFB91C1C)
}

private fun fmt(v: Double): String = String.format(Locale("tr", "TR"), "%.1f", v)
private fun fmt0(v: Double): String = String.format(Locale("tr", "TR"), "%.0f", v)
private fun formatDate(ts: Long): String = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR")).format(Date(ts))
private fun unitFor(type: String): String = when (type) {
    "Mesai" -> "saat"
    "Yıllık İzin", "Günlük İzin", "Rapor", "Devamsızlık" -> "gün"
    else -> "adet"
}
