package com.oguzhan.hatakayit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

private data class OperatorV3(val sicil: String, val name: String, val active: Boolean = true)
private data class PerfRecordV3(
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
private data class SummaryV3(
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

private val periodStartV3 = LocalDate.of(2026, 8, 1)
private val periodEndV3 = LocalDate.of(2027, 7, 31)
private val defaultOperatorsV3 = listOf(
    OperatorV3("6095", "NUSRET BULUT"),
    OperatorV3("614", "LEVENT DOĞUER"),
    OperatorV3("6112", "AHMET SEZER"),
    OperatorV3("3388", "İLYAS ÖZDEMİR"),
    OperatorV3("2921", "GİRAY ÇALIŞIR"),
    OperatorV3("4975", "SEZGİN NALBATÇI"),
    OperatorV3("5828", "EREN YİĞİTOĞLU"),
    OperatorV3("686", "RUHAN SEVİL TEKEOĞLU"),
    OperatorV3("596", "FATİH HENDEKÇİ"),
    OperatorV3("2484", "MESUT MÜHÜRDAROÇ")
)
private val recordTypesV3 = listOf("Kaçan Hata", "Yakalanan Hata", "KY", "Kaizen", "Mesai", "Yıllık İzin", "Günlük İzin", "Rapor", "Devamsızlık")
private val machinesV3 = listOf("1600T-1", "1600T-2", "1600T-3", "1700T", "850T", "650T")
private val defectWeightsV3 = linkedMapOf(
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
private val defectHelpV3 = mapOf(
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

private object StoreV3 {
    private const val PREFS = "operator_takip_prefs"
    private const val RECORDS = "records"
    private const val REFERENCES = "defect_reference_photos"
    private const val OPERATORS = "operators_v1_2"

    fun loadRecords(context: Context): List<PerfRecordV3> {
        return try {
            val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(RECORDS, "[]") ?: "[]"
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(
                        PerfRecordV3(
                            id = o.getLong("id"),
                            timestamp = o.getLong("timestamp"),
                            operatorSicil = o.getString("operatorSicil"),
                            type = o.getString("type"),
                            defect = o.optString("defect"),
                            amount = o.getDouble("amount"),
                            machine = o.optString("machine"),
                            part = o.optString("part"),
                            note = o.optString("note"),
                            photoPath = o.optString("photoPath")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveRecords(context: Context, records: List<PerfRecordV3>) {
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
                put("photoPath", r.photoPath)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(RECORDS, array.toString()).apply()
    }

    fun loadReferences(context: Context): MutableMap<String, String> {
        return try {
            val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(REFERENCES, "{}") ?: "{}"
            val obj = JSONObject(raw)
            mutableMapOf<String, String>().apply {
                defectWeightsV3.keys.forEach { defect ->
                    val path = obj.optString(defect)
                    if (path.isNotBlank()) put(defect, path)
                }
            }
        } catch (_: Exception) {
            mutableMapOf()
        }
    }

    fun saveReferences(context: Context, refs: Map<String, String>) {
        val obj = JSONObject()
        refs.forEach { (defect, path) -> obj.put(defect, path) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(REFERENCES, obj.toString()).apply()
    }

    fun loadOperators(context: Context): List<OperatorV3> {
        return try {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val raw = prefs.getString(OPERATORS, null)
            if (raw.isNullOrBlank()) {
                defaultOperatorsV3
            } else {
                val array = JSONArray(raw)
                val loaded = buildList {
                    for (i in 0 until array.length()) {
                        val o = array.getJSONObject(i)
                        add(OperatorV3(o.getString("sicil"), o.getString("name"), o.optBoolean("active", true)))
                    }
                }
                if (loaded.isEmpty()) defaultOperatorsV3 else loaded
            }
        } catch (_: Exception) {
            defaultOperatorsV3
        }
    }

    fun saveOperators(context: Context, operators: List<OperatorV3>) {
        val array = JSONArray()
        operators.forEach { op ->
            array.put(JSONObject().apply {
                put("sicil", op.sicil)
                put("name", op.name)
                put("active", op.active)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(OPERATORS, array.toString()).apply()
    }
}

class MainActivityV3 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OperatorTrackingV3() }
    }
}

@Composable
private fun OperatorTrackingV3() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val records = remember { mutableStateListOf<PerfRecordV3>().apply { addAll(StoreV3.loadRecords(context)) } }
    val refs = remember { mutableStateMapOf<String, String>().apply { putAll(StoreV3.loadReferences(context)) } }
    val operators = remember { mutableStateListOf<OperatorV3>().apply { addAll(StoreV3.loadOperators(context)) } }
    var tab by remember { mutableIntStateOf(0) }
    var selectedSicil by remember { mutableStateOf(operators.firstOrNull { it.active }?.sicil ?: "") }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF0B4F7D),
            secondary = Color(0xFF0E7490),
            tertiary = Color(0xFF15803D),
            error = Color(0xFFB91C1C),
            background = Color(0xFFF5F7FA)
        )
    ) {
        Scaffold(
            topBar = {
                Surface(color = MaterialTheme.colorScheme.primary, shadowElevation = 3.dp) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp)) {
                        Text("Operatör Takip", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text("Fotoğraflı Performans Sistemi • v1.2", color = Color.White.copy(alpha = .82f), fontSize = 11.sp)
                    }
                }
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Text("⌂") }, label = { Text("Ana") })
                    NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Text("+") }, label = { Text("Kayıt") })
                    NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Text("▣") }, label = { Text("Hatalar") })
                    NavigationBarItem(selected = tab == 3, onClick = { tab = 3 }, icon = { Text("◎") }, label = { Text("Personel") })
                    NavigationBarItem(selected = tab == 4, onClick = { tab = 4 }, icon = { Text("⚙") }, label = { Text("Ekip") })
                }
            }
        ) { padding ->
            val activeOperators = operators.filter { it.active }
            Box(Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                when (tab) {
                    0 -> DashboardV3(records, activeOperators) { op -> selectedSicil = op.sicil; tab = 3 }
                    1 -> EntryV3(records, refs, activeOperators, operators) { StoreV3.saveRecords(context, records) }
                    2 -> DefectLibraryV3(refs) { StoreV3.saveReferences(context, refs) }
                    3 -> PersonV3(records, activeOperators, selectedSicil, onSelect = { selectedSicil = it.sicil }, onDelete = {
                        records.remove(it)
                        StoreV3.saveRecords(context, records)
                    })
                    else -> TeamManagerV3(operators, records) {
                        StoreV3.saveOperators(context, operators)
                        if (operators.none { it.sicil == selectedSicil && it.active }) {
                            selectedSicil = operators.firstOrNull { it.active }?.sicil ?: ""
                        }
                    }
                }
            }
        }
    }
}

private fun activeMonthsV3(): Int {
    val today = LocalDate.now()
    val capped = when {
        today.isBefore(periodStartV3) -> periodStartV3
        today.isAfter(periodEndV3) -> periodEndV3
        else -> today
    }
    return (ChronoUnit.MONTHS.between(YearMonth.from(periodStartV3), YearMonth.from(capped)).toInt() + 1).coerceIn(1, 12)
}

private fun inPeriodV3(record: PerfRecordV3): Boolean {
    val date = Instant.ofEpochMilli(record.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    return !date.isBefore(periodStartV3) && !date.isAfter(periodEndV3)
}

private fun calculateSummaryV3(op: OperatorV3, all: List<PerfRecordV3>): SummaryV3 {
    val rows = all.filter { it.operatorSicil == op.sicil && inPeriodV3(it) }
    fun sum(type: String) = rows.filter { it.type == type }.sumOf { it.amount }
    fun weighted(type: String) = rows.filter { it.type == type }.sumOf { it.amount * (defectWeightsV3[it.defect] ?: 1.0) }
    val escaped = sum("Kaçan Hata")
    val caught = sum("Yakalanan Hata")
    val ky = sum("KY")
    val kaizen = sum("Kaizen")
    val overtime = sum("Mesai")
    val absence = sum("Devamsızlık")
    val months = activeMonthsV3()
    val quality = (35.0 + min(weighted("Yakalanan Hata") * 0.5, 5.0) - weighted("Kaçan Hata") * 4.0).coerceIn(0.0, 40.0)
    val kyScore = min(15.0, ky / (months * 6.0) * 15.0)
    val kaizenScore = min(15.0, kaizen / months * 15.0)
    val attendance = max(0.0, 20.0 - absence * 5.0)
    val overtimeScore = min(10.0, overtime / (months * 10.0) * 10.0)
    val total = quality + kyScore + kaizenScore + attendance + overtimeScore
    val grade = when {
        total >= 90 -> "A"
        total >= 80 -> "B"
        total >= 70 -> "C"
        else -> "D"
    }
    return SummaryV3(
        hasData = rows.isNotEmpty(),
        escaped = escaped,
        caught = caught,
        ky = ky,
        kaizen = kaizen,
        overtime = overtime,
        annualLeave = sum("Yıllık İzin"),
        dailyLeave = sum("Günlük İzin"),
        report = sum("Rapor"),
        absence = absence,
        quality = quality,
        kyScore = kyScore,
        kaizenScore = kaizenScore,
        attendance = attendance,
        overtimeScore = overtimeScore,
        total = if (rows.isEmpty()) 0.0 else total,
        grade = if (rows.isEmpty()) "—" else grade
    )
}

private fun saveCameraV3(context: Context, bitmap: Bitmap, prefix: String): String {
    return try {
        val dir = File(context.filesDir, "operator_photos").apply { mkdirs() }
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        file.absolutePath
    } catch (_: Exception) {
        ""
    }
}

private fun saveGalleryV3(context: Context, uri: Uri, prefix: String): String {
    return try {
        val dir = File(context.filesDir, "operator_photos").apply { mkdirs() }
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID()}.img")
        val input = context.contentResolver.openInputStream(uri) ?: return ""
        input.use { source -> file.outputStream().use { target -> source.copyTo(target) } }
        file.absolutePath
    } catch (_: Exception) {
        ""
    }
}

@Composable
private fun DashboardV3(records: List<PerfRecordV3>, operators: List<OperatorV3>, openPerson: (OperatorV3) -> Unit) {
    val summaries = operators.map { it to calculateSummaryV3(it, records) }
    val withData = summaries.filter { it.second.hasData }
    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Yönetici Dashboard", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Aktif personel: ${operators.size} • Hedef süresi: ${activeMonthsV3()} ay", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricV3("Ortalama", if (withData.isEmpty()) "—" else f1V3(withData.map { it.second.total }.average()), Modifier.weight(1f), Color(0xFFE0F2FE))
                MetricV3("Fotoğraflı Kayıt", records.count { it.photoPath.isNotBlank() }.toString(), Modifier.weight(1f), Color(0xFFEDE9FE))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricV3("Kaçan Hata", f0V3(summaries.sumOf { it.second.escaped }), Modifier.weight(1f), Color(0xFFFEE2E2))
                MetricV3("Yakalanan", f0V3(summaries.sumOf { it.second.caught }), Modifier.weight(1f), Color(0xFFDCFCE7))
            }
        }
        if (operators.isEmpty()) {
            item { InfoV3("Aktif operatör yok", "Ekip sekmesinden yeni operatör ekleyin veya pasif personeli geri alın.") }
        } else {
            item { Text("Personel Performansı", fontSize = 19.sp, fontWeight = FontWeight.Bold) }
            items(summaries, key = { it.first.sicil }) { item ->
                val op = item.first
                val score = item.second
                Card(modifier = Modifier.fillMaxWidth().clickable { openPerson(op) }) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(op.name, fontWeight = FontWeight.Bold)
                            Text("Sicil ${op.sicil} • KY ${f0V3(score.ky)} • Kaizen ${f0V3(score.kaizen)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (score.hasData) {
                            Text(f1V3(score.total), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = scoreColorV3(score.total))
                            Spacer(Modifier.width(8.dp))
                            GradeBadgeV3(score.grade)
                        } else {
                            Text("Veri yok", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryV3(
    records: SnapshotStateList<PerfRecordV3>,
    references: Map<String, String>,
    activeOperators: List<OperatorV3>,
    allOperators: List<OperatorV3>,
    save: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    if (activeOperators.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            InfoV3("Kayıt açılamıyor", "Önce Ekip sekmesinden aktif operatör ekleyin.")
        }
        return
    }

    var operatorSicil by remember(activeOperators.first().sicil) { mutableStateOf(activeOperators.first().sicil) }
    val currentOperator = activeOperators.firstOrNull { it.sicil == operatorSicil } ?: activeOperators.first()
    var type by remember { mutableStateOf(recordTypesV3.first()) }
    var defect by remember { mutableStateOf(defectWeightsV3.keys.first()) }
    var amount by remember { mutableStateOf("1") }
    var machine by remember { mutableStateOf(machinesV3.first()) }
    var part by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var photoPath by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var message by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<PerfRecordV3?>(null) }
    val isError = type == "Kaçan Hata" || type == "Yakalanan Hata"

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val path = saveGalleryV3(context, uri, "record")
            if (path.isNotBlank()) photoPath = path
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val path = saveCameraV3(context, bitmap, "record")
            if (path.isNotBlank()) photoPath = path
        }
    }

    fun resetForm() {
        operatorSicil = activeOperators.first().sicil
        type = recordTypesV3.first()
        defect = defectWeightsV3.keys.first()
        amount = "1"
        machine = machinesV3.first()
        part = ""
        note = ""
        photoPath = ""
        editingId = null
    }

    fun editRecord(record: PerfRecordV3) {
        operatorSicil = activeOperators.firstOrNull { it.sicil == record.operatorSicil }?.sicil ?: activeOperators.first().sicil
        type = record.type
        defect = record.defect.ifBlank { defectWeightsV3.keys.first() }
        amount = if (record.amount % 1.0 == 0.0) record.amount.toInt().toString() else record.amount.toString()
        machine = record.machine.ifBlank { machinesV3.first() }
        part = record.part
        note = record.note
        photoPath = record.photoPath
        editingId = record.id
        message = "✎ Düzenleme modu açık"
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (editingId == null) "Günlük Kayıt" else "Kaydı Düzenle", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        if (editingId != null) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED))) {
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Mevcut kayıt düzenleniyor", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color(0xFF9A3412))
                    TextButton(onClick = { resetForm(); message = "Düzenleme iptal edildi" }) { Text("İPTAL") }
                }
            }
        }

        SelectorV3("Operatör", activeOperators, currentOperator, { "${it.sicil} - ${it.name}" }) { operatorSicil = it.sicil }
        SelectorV3("Kayıt Türü", recordTypesV3, type, { it }) { type = it }
        if (isError) {
            SelectorV3("Hata Türü", defectWeightsV3.keys.toList(), defect, { "$it ×${defectWeightsV3[it]}" }) { defect = it }
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF))) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Hata Örneği • $defect", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(defectHelpV3[defect] ?: "", fontSize = 13.sp)
                    val ref = references[defect].orEmpty()
                    if (ref.isNotBlank()) LocalPhotoV3(ref, 170.dp) else Text("Örnek fotoğraf yok. Hatalar sekmesinden ekleyin.", color = Color(0xFFB45309), fontSize = 12.sp)
                }
            }
        }

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text(if (type == "Mesai") "Saat" else if (type.contains("İzin") || type == "Rapor" || type == "Devamsızlık") "Gün" else "Miktar") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        SelectorV3("Makine", machinesV3, machine, { it }) { machine = it }
        OutlinedTextField(value = part, onValueChange = { part = it }, label = { Text("Model / Parça") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Açıklama / Detay") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

        Card {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Kayıt Fotoğrafı", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                if (photoPath.isNotBlank()) LocalPhotoV3(photoPath, 180.dp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { camera.launch(null) }, modifier = Modifier.weight(1f)) { Text("📷 Kamera") }
                    OutlinedButton(onClick = { gallery.launch("image/*") }, modifier = Modifier.weight(1f)) { Text("▣ Galeri") }
                }
                if (photoPath.isNotBlank()) TextButton(onClick = { photoPath = "" }) { Text("Fotoğrafı kaldır", color = MaterialTheme.colorScheme.error) }
            }
        }

        Button(
            onClick = {
                val number = amount.replace(',', '.').toDoubleOrNull() ?: 0.0
                if (number <= 0.0) {
                    message = "Miktar 0'dan büyük olmalı"
                } else {
                    val index = editingId?.let { id -> records.indexOfFirst { it.id == id } } ?: -1
                    if (index >= 0) {
                        val old = records[index]
                        records[index] = old.copy(
                            operatorSicil = operatorSicil,
                            type = type,
                            defect = if (isError) defect else "",
                            amount = number,
                            machine = machine,
                            part = part.trim(),
                            note = note.trim(),
                            photoPath = photoPath
                        )
                        save()
                        resetForm()
                        message = "✓ Kayıt güncellendi"
                    } else {
                        val now = System.currentTimeMillis()
                        records.add(PerfRecordV3(now, now, operatorSicil, type, if (isError) defect else "", number, machine, part.trim(), note.trim(), photoPath))
                        save()
                        amount = "1"
                        part = ""
                        note = ""
                        photoPath = ""
                        message = "✓ Kayıt kaydedildi"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text(if (editingId == null) "KAYDI KAYDET" else "KAYDI GÜNCELLE", fontWeight = FontWeight.Bold) }

        if (message.isNotBlank()) Text(message, color = if (message.startsWith("✓")) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        HorizontalDivider()
        Text("Son Kayıtlar", fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Text("Son 10 kayıt • düzenleme ve silme açık", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        records.takeLast(10).reversed().forEach { record ->
            RecordCardV3(record, allOperators, onEdit = { editRecord(record) }, onDelete = { deleteTarget = record })
        }
        Spacer(Modifier.height(20.dp))
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Kaydı sil") },
            text = { Text("Bu kayıt kalıcı olarak silinsin mi?") },
            confirmButton = {
                TextButton(onClick = {
                    records.remove(target)
                    save()
                    if (editingId == target.id) resetForm()
                    deleteTarget = null
                    message = "Kayıt silindi"
                }) { Text("SİL", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("VAZGEÇ") } }
        )
    }
}

@Composable
private fun RecordCardV3(record: PerfRecordV3, operators: List<OperatorV3>, onEdit: () -> Unit, onDelete: () -> Unit) {
    val op = operators.firstOrNull { it.sicil == record.operatorSicil }
    val bg = when (record.type) {
        "Kaçan Hata" -> Color(0xFFFFF1F2)
        "Yakalanan Hata" -> Color(0xFFF0FDF4)
        "KY" -> Color(0xFFEFF6FF)
        "Kaizen" -> Color(0xFFFFFBEB)
        else -> Color(0xFFF8FAFC)
    }
    Card(colors = CardDefaults.cardColors(containerColor = bg), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("${op?.name ?: "Sicil ${record.operatorSicil}"} • ${record.type}", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text("Sicil ${record.operatorSicil} • ${dateTextV3(record.timestamp)}${if (op?.active == false) " • PASİF PERSONEL" else ""}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${f0V3(record.amount)} ${unitV3(record.type)}${if (record.defect.isNotBlank()) " • ${record.defect} ×${defectWeightsV3[record.defect] ?: 1.0}" else ""}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (record.machine.isNotBlank()) Text("Makine: ${record.machine}", fontSize = 13.sp)
            if (record.part.isNotBlank()) Text("Model / Parça: ${record.part}", fontSize = 13.sp)
            if (record.note.isNotBlank()) Text("Açıklama: ${record.note}", fontSize = 13.sp)
            if (record.photoPath.isNotBlank()) LocalPhotoV3(record.photoPath, 150.dp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onEdit) { Text("✎ DÜZENLE") }
                TextButton(onClick = onDelete) { Text("SİL", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun DefectLibraryV3(refs: MutableMap<String, String>, saveRefs: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var target by remember { mutableStateOf<String?>(null) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val defect = target
        if (uri != null && defect != null) {
            val path = saveGalleryV3(context, uri, "reference")
            if (path.isNotBlank()) { refs[defect] = path; saveRefs() }
        }
        target = null
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        val defect = target
        if (bitmap != null && defect != null) {
            val path = saveCameraV3(context, bitmap, "reference")
            if (path.isNotBlank()) { refs[defect] = path; saveRefs() }
        }
        target = null
    }

    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Hata Fotoğraf Kütüphanesi", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Her hata için örnek fotoğraf ekleyin. Kayıt ekranında operatöre otomatik gösterilir.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(defectWeightsV3.keys.toList()) { defect ->
            val photo = refs[defect].orEmpty()
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(defect, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text("Katsayı ×${defectWeightsV3[defect]} • ${defectHelpV3[defect] ?: ""}", fontSize = 12.sp)
                    if (photo.isNotBlank()) LocalPhotoV3(photo, 190.dp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { target = defect; camera.launch(null) }, modifier = Modifier.weight(1f)) { Text("📷 Çek") }
                        OutlinedButton(onClick = { target = defect; gallery.launch("image/*") }, modifier = Modifier.weight(1f)) { Text("▣ Galeriden") }
                    }
                    if (photo.isNotBlank()) TextButton(onClick = { refs.remove(defect); saveRefs() }) { Text("Fotoğrafı kaldır", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
private fun PersonV3(records: List<PerfRecordV3>, operators: List<OperatorV3>, selectedSicil: String, onSelect: (OperatorV3) -> Unit, onDelete: (PerfRecordV3) -> Unit) {
    if (operators.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            InfoV3("Aktif personel yok", "Ekip sekmesinden personel ekleyin veya pasif personeli geri alın.")
        }
        return
    }
    val selected = operators.firstOrNull { it.sicil == selectedSicil } ?: operators.first()
    val score = calculateSummaryV3(selected, records)
    val history = records.filter { it.operatorSicil == selected.sicil && inPeriodV3(it) }.sortedByDescending { it.timestamp }
    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SelectorV3("Operatör", operators, selected, { "${it.sicil} - ${it.name}" }, onSelect) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(selected.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Sicil: ${selected.sicil}")
                    }
                    if (score.hasData) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${f1V3(score.total)} / 100", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = scoreColorV3(score.total))
                            GradeBadgeV3(score.grade)
                        }
                    } else Text("Veri yok")
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricV3("KY", f0V3(score.ky), Modifier.weight(1f), Color(0xFFDCFCE7))
                MetricV3("Kaizen", f0V3(score.kaizen), Modifier.weight(1f), Color(0xFFFEF3C7))
                MetricV3("Mesai", "${f0V3(score.overtime)} s", Modifier.weight(1f), Color(0xFFE0E7FF))
            }
        }
        item {
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Puan Bileşenleri", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    ScoreBarV3("Kalite", score.quality, 40.0)
                    ScoreBarV3("KY", score.kyScore, 15.0)
                    ScoreBarV3("Kaizen", score.kaizenScore, 15.0)
                    ScoreBarV3("Devam", score.attendance, 20.0)
                    ScoreBarV3("Mesai", score.overtimeScore, 10.0)
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(14.dp)) {
                    Text("Performans Özeti", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (!score.hasData) Text("Bu dönem için kayıt yok.")
                    else Text("KY ${f0V3(score.ky)}, Kaizen ${f0V3(score.kaizen)}, kaçan hata ${f0V3(score.escaped)}, yakalanan hata ${f0V3(score.caught)}, mesai ${f0V3(score.overtime)} saat, yıllık izin ${f0V3(score.annualLeave)} gün, günlük izin ${f0V3(score.dailyLeave)} gün, rapor ${f0V3(score.report)} gün, devamsızlık ${f0V3(score.absence)} gün. Toplam ${f1V3(score.total)}/100, sınıf ${score.grade}.")
                }
            }
        }
        item { Text("Detaylı Kayıt Geçmişi", fontSize = 19.sp, fontWeight = FontWeight.Bold) }
        if (history.isEmpty()) item { Text("Kayıt yok.") }
        items(history, key = { it.id }) { record ->
            Card {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${record.type}${if (record.defect.isNotBlank()) " • ${record.defect}" else ""}", fontWeight = FontWeight.Bold)
                    Text("${f0V3(record.amount)} ${unitV3(record.type)} • ${dateTextV3(record.timestamp)}", fontSize = 12.sp)
                    if (record.machine.isNotBlank()) Text("Makine: ${record.machine}", fontSize = 12.sp)
                    if (record.part.isNotBlank()) Text("Parça: ${record.part}", fontSize = 12.sp)
                    if (record.note.isNotBlank()) Text("Açıklama: ${record.note}", fontSize = 12.sp)
                    if (record.photoPath.isNotBlank()) LocalPhotoV3(record.photoPath, 130.dp)
                    TextButton(onClick = { onDelete(record) }) { Text("Kaydı sil", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
private fun TeamManagerV3(operators: SnapshotStateList<OperatorV3>, records: List<PerfRecordV3>, onSave: () -> Unit) {
    var sicil by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var removeTarget by remember { mutableStateOf<OperatorV3?>(null) }
    val active = operators.filter { it.active }
    val passive = operators.filter { !it.active }

    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Personel Yönetimi", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Yeni operatör ekleyin, aktif listeden çıkarın veya tekrar geri alın.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Yeni Operatör Ekle", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = sicil,
                        onValueChange = { sicil = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Sicil No") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.uppercase(Locale("tr", "TR")) },
                        label = { Text("Ad Soyad") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Button(onClick = {
                        val s = sicil.trim()
                        val n = name.trim()
                        when {
                            s.isBlank() -> message = "Sicil numarası girin"
                            n.isBlank() -> message = "Ad soyad girin"
                            operators.any { it.sicil == s } -> message = "Bu sicil zaten kayıtlı. Pasifse aşağıdan geri alın"
                            else -> {
                                operators.add(OperatorV3(s, n, true))
                                onSave()
                                sicil = ""
                                name = ""
                                message = "✓ Operatör eklendi"
                            }
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("OPERATÖRÜ EKLE", fontWeight = FontWeight.Bold) }
                    if (message.isNotBlank()) Text(message, color = if (message.startsWith("✓")) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricV3("Aktif", active.size.toString(), Modifier.weight(1f), Color(0xFFDCFCE7))
                MetricV3("Pasif", passive.size.toString(), Modifier.weight(1f), Color(0xFFF3F4F6))
            }
        }
        item { Text("Aktif Operatörler", fontSize = 19.sp, fontWeight = FontWeight.Bold) }
        if (active.isEmpty()) item { Text("Aktif operatör yok.") }
        items(active, key = { it.sicil }) { op ->
            val count = records.count { it.operatorSicil == op.sicil }
            Card {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(op.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("Sicil ${op.sicil} • $count kayıt", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = { removeTarget = op }) { Text("ÇIKAR", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
        if (passive.isNotEmpty()) {
            item {
                Text("Pasif Personel", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("Geçmiş kayıtlar korunur.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(passive, key = { "p_${it.sicil}" }) { op ->
                val count = records.count { it.operatorSicil == op.sicil }
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(op.name, fontWeight = FontWeight.Bold)
                            Text("Sicil ${op.sicil} • $count eski kayıt", fontSize = 12.sp)
                        }
                        Button(onClick = {
                            val index = operators.indexOfFirst { it.sicil == op.sicil }
                            if (index >= 0) operators[index] = op.copy(active = true)
                            onSave()
                            message = "✓ ${op.name} tekrar aktif edildi"
                        }) { Text("GERİ AL") }
                    }
                }
            }
        }
        item { InfoV3("Kayıtlar neden silinmiyor?", "Bir operatörü çıkardığınızda geçmiş performans ve hata kayıtları korunur. Operatör yalnızca yeni kayıt seçimlerinden kaldırılır.") }
    }

    removeTarget?.let { op ->
        val count = records.count { it.operatorSicil == op.sicil }
        AlertDialog(
            onDismissRequest = { removeTarget = null },
            title = { Text("Operatörü çıkar") },
            text = { Text("${op.name} aktif listeden çıkarılsın mı? $count geçmiş kayıt korunacak.") },
            confirmButton = {
                TextButton(onClick = {
                    val index = operators.indexOfFirst { it.sicil == op.sicil }
                    if (index >= 0) operators[index] = op.copy(active = false)
                    onSave()
                    message = "${op.name} pasif personele taşındı"
                    removeTarget = null
                }) { Text("ÇIKAR", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { removeTarget = null }) { Text("VAZGEÇ") } }
        )
    }
}

@Composable
private fun LocalPhotoV3(path: String, height: Dp) {
    var enlarged by remember { mutableStateOf(false) }
    val bitmap = remember(path) { BitmapFactory.decodeFile(path) }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Fotoğraf",
            modifier = Modifier.fillMaxWidth().height(height).clickable { enlarged = true },
            contentScale = ContentScale.Crop
        )
        if (enlarged) {
            Dialog(onDismissRequest = { enlarged = false }) {
                Card(colors = CardDefaults.cardColors(containerColor = Color.Black)) {
                    Column(Modifier.padding(8.dp)) {
                        Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Büyük fotoğraf", modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp, max = 620.dp), contentScale = ContentScale.Fit)
                        TextButton(onClick = { enlarged = false }, modifier = Modifier.align(Alignment.End)) { Text("KAPAT", color = Color.White) }
                    }
                }
            }
        }
    } else {
        Box(Modifier.fillMaxWidth().height(height).background(Color(0xFFE5E7EB)), contentAlignment = Alignment.Center) { Text("Fotoğraf açılamadı", color = Color(0xFF6B7280)) }
    }
}

@Composable
private fun <T> SelectorV3(label: String, options: List<T>, selected: T, display: (T) -> String, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(display(selected), Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
            Text("▼")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(display(option)) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}

@Composable
private fun MetricV3(title: String, value: String, modifier: Modifier, color: Color) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color)) {
        Column(Modifier.padding(12.dp)) {
            Text(title, fontSize = 11.sp, color = Color(0xFF4B5563))
            Text(value, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun InfoV3(title: String, text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GradeBadgeV3(grade: String) {
    val color = when (grade) {
        "A" -> Color(0xFF15803D)
        "B" -> Color(0xFF0369A1)
        "C" -> Color(0xFFB45309)
        else -> Color(0xFFB91C1C)
    }
    Surface(color = color, shape = RoundedCornerShape(9.dp)) {
        Text(grade, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
    }
}

@Composable
private fun ScoreBarV3(title: String, value: Double, maximum: Double) {
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text(title, Modifier.weight(1f))
            Text("${f1V3(value)} / ${f0V3(maximum)}", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(progress = { (value / maximum).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
    }
}

private fun f1V3(value: Double): String = String.format(Locale("tr", "TR"), "%.1f", value)
private fun f0V3(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else f1V3(value)
private fun unitV3(type: String): String = when (type) {
    "Mesai" -> "saat"
    "Yıllık İzin", "Günlük İzin", "Rapor", "Devamsızlık" -> "gün"
    else -> "adet"
}
private fun dateTextV3(timestamp: Long): String = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR")).format(Date(timestamp))
private fun scoreColorV3(score: Double): Color = when {
    score >= 90 -> Color(0xFF15803D)
    score >= 80 -> Color(0xFF0369A1)
    score >= 70 -> Color(0xFFB45309)
    else -> Color(0xFFB91C1C)
}
