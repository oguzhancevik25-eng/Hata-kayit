package com.oguzhan.hatakayit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
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

private val periodStartV2: LocalDate = LocalDate.of(2026, 8, 1)
private val periodEndV2: LocalDate = LocalDate.of(2027, 7, 31)

private data class OperatorV2(val sicil: String, val name: String)
private data class PerfRecordV2(
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

private data class SummaryV2(
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

private val operatorsV2 = listOf(
    OperatorV2("6095", "NUSRET BULUT"),
    OperatorV2("614", "LEVENT DOĞUER"),
    OperatorV2("6112", "AHMET SEZER"),
    OperatorV2("3388", "İLYAS ÖZDEMİR"),
    OperatorV2("2921", "GİRAY ÇALIŞIR"),
    OperatorV2("4975", "SEZGİN NALBATÇI"),
    OperatorV2("5828", "EREN YİĞİTOĞLU"),
    OperatorV2("686", "RUHAN SEVİL TEKEOĞLU"),
    OperatorV2("596", "FATİH HENDEKÇİ"),
    OperatorV2("2484", "MESUT MÜHÜRDAROÇ")
)

private val recordTypesV2 = listOf(
    "Kaçan Hata", "Yakalanan Hata", "KY", "Kaizen", "Mesai",
    "Yıllık İzin", "Günlük İzin", "Rapor", "Devamsızlık"
)

private val machinesV2 = listOf("1600T-1", "1600T-2", "1600T-3", "1700T", "850T", "650T")

private val defectWeightsV2 = linkedMapOf(
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

private val defectGuidesV2 = mapOf(
    "Şişme" to "Parça yüzeyinde şişme/kabarma görünümü. Operatör görseldeki bölge ve yüzey farkını kontrol eder.",
    "Felt Eksik" to "Parçada olması gereken feltin bulunmadığı durum. Kontrolde felt var/yok ve doğru pozisyon doğrulanır.",
    "Çapak" to "Kalıp birleşim veya kenar bölgelerinde istenmeyen ince plastik fazlalığı.",
    "Yolluk Kalma" to "Parça üzerinde veya kalıp tarafında yolluk/gate kalıntısının kalması.",
    "Yolluk Yapışması" to "Yolluğun kalıba veya parçaya yapışarak normal ayrılmaması.",
    "Çökme" to "Yüzeyde içeri doğru çökük görünüm. Özellikle kalın/boss bölgelerinde yüzey kontrol edilir.",
    "Eksik" to "Parçanın bir bölgesinin tam dolmaması veya formun eksik oluşması.",
    "İz" to "Standart dışı çizgi, akış, itici, gate veya yüzey izi görülen durum.",
    "Yabancı Madde" to "Parça üzerinde farklı renk/nokta/tanecik veya malzeme dışı görüntü.",
    "Hatalı Setleme" to "Parça/aparat/ayarın standarda uygun yerleştirilmemesi veya seçilmemesi.",
    "Kabarma" to "Yüzeyde lokal kabarıklık veya standart dışı yükselti görünümü.",
    "Beyazlık" to "Parça yüzeyinde beyazlama veya renk kaybı görünümü.",
    "Leke" to "Yüzeyde standart dışı renk, yağ, kir veya iz görünümü.",
    "Deforme" to "Parça geometrisinin standart formdan sapması, eğilme veya şekil bozukluğu.",
    "Diğer" to "Listede olmayan hata. Fotoğraf ve açıklama ile kayıt altına alınır."
)

private object StoreV2 {
    private const val PREFS = "operator_takip_prefs"
    private const val RECORDS_KEY = "records"
    private const val REFERENCE_KEY = "defect_reference_photos"

    fun loadRecords(context: Context): List<PerfRecordV2> {
        return try {
            val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(RECORDS_KEY, "[]") ?: "[]"
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(
                        PerfRecordV2(
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

    fun saveRecords(context: Context, records: List<PerfRecordV2>) {
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
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(RECORDS_KEY, array.toString()).apply()
    }

    fun loadReferencePhotos(context: Context): MutableMap<String, String> {
        return try {
            val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(REFERENCE_KEY, "{}") ?: "{}"
            val obj = JSONObject(raw)
            val result = mutableMapOf<String, String>()
            defectWeightsV2.keys.forEach { defect ->
                val path = obj.optString(defect)
                if (path.isNotBlank()) result[defect] = path
            }
            result
        } catch (_: Exception) {
            mutableMapOf()
        }
    }

    fun saveReferencePhotos(context: Context, photos: Map<String, String>) {
        val obj = JSONObject()
        photos.forEach { (defect, path) -> obj.put(defect, path) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(REFERENCE_KEY, obj.toString()).apply()
    }
}

class MainActivityV2 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OperatorTrackingAppV2() }
    }
}

@Composable
private fun OperatorTrackingAppV2() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val records = remember {
        mutableStateListOf<PerfRecordV2>().apply { addAll(StoreV2.loadRecords(context)) }
    }
    val referencePhotos = remember {
        mutableStateMapOf<String, String>().apply { putAll(StoreV2.loadReferencePhotos(context)) }
    }
    var tab by remember { mutableIntStateOf(0) }
    var selectedOperator by remember { mutableStateOf(operatorsV2.first()) }

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
                        Text("Fotoğraflı Performans Sistemi • 01.08.2026 — 31.07.2027", color = Color.White.copy(alpha = 0.82f), fontSize = 11.sp)
                    }
                }
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Text("⌂") }, label = { Text("Ana") })
                    NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Text("+") }, label = { Text("Kayıt") })
                    NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Text("▣") }, label = { Text("Hatalar") })
                    NavigationBarItem(selected = tab == 3, onClick = { tab = 3 }, icon = { Text("◎") }, label = { Text("Personel") })
                    NavigationBarItem(selected = tab == 4, onClick = { tab = 4 }, icon = { Text("⚙") }, label = { Text("Ayarlar") })
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                when (tab) {
                    0 -> DashboardScreenV2(records) { op -> selectedOperator = op; tab = 3 }
                    1 -> EntryScreenV2(records, referencePhotos) { StoreV2.saveRecords(context, records) }
                    2 -> DefectLibraryScreenV2(referencePhotos) { StoreV2.saveReferencePhotos(context, referencePhotos) }
                    3 -> PersonScreenV2(records, selectedOperator, { selectedOperator = it }) { record ->
                        records.remove(record)
                        StoreV2.saveRecords(context, records)
                    }
                    else -> SettingsScreenV2()
                }
            }
        }
    }
}

private fun activeMonthsV2(): Int {
    val today = LocalDate.now()
    val capped = when {
        today.isBefore(periodStartV2) -> periodStartV2
        today.isAfter(periodEndV2) -> periodEndV2
        else -> today
    }
    return (ChronoUnit.MONTHS.between(YearMonth.from(periodStartV2), YearMonth.from(capped)).toInt() + 1).coerceIn(1, 12)
}

private fun dateOfV2(timestamp: Long): LocalDate = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
private fun inPeriodV2(r: PerfRecordV2): Boolean {
    val d = dateOfV2(r.timestamp)
    return !d.isBefore(periodStartV2) && !d.isAfter(periodEndV2)
}

private fun calculateSummaryV2(operator: OperatorV2, all: List<PerfRecordV2>, targetMonths: Int = activeMonthsV2()): SummaryV2 {
    val records = all.filter { it.operatorSicil == operator.sicil && inPeriodV2(it) }
    fun total(type: String) = records.filter { it.type == type }.sumOf { it.amount }
    fun weighted(type: String) = records.filter { it.type == type }.sumOf { it.amount * (defectWeightsV2[it.defect] ?: 1.0) }

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
    val kyScore = min(15.0, ky / (targetMonths * 6.0) * 15.0)
    val kaizenScore = min(15.0, kaizen / targetMonths * 15.0)
    val attendance = max(0.0, 20.0 - absence * 5.0)
    val overtimeScore = min(10.0, overtime / (targetMonths * 10.0) * 10.0)
    val totalScore = quality + kyScore + kaizenScore + attendance + overtimeScore
    val grade = when {
        totalScore >= 90 -> "A"
        totalScore >= 80 -> "B"
        totalScore >= 70 -> "C"
        else -> "D"
    }

    return SummaryV2(
        records.isNotEmpty(), escaped, escapedWeighted, caught, caughtWeighted, ky, kaizen, overtime,
        annualLeave, dailyLeave, report, absence, quality, kyScore, kaizenScore, attendance, overtimeScore,
        if (records.isEmpty()) 0.0 else totalScore, if (records.isEmpty()) "—" else grade
    )
}

private fun saveBitmapV2(context: Context, bitmap: Bitmap, prefix: String): String = try {
    val dir = File(context.filesDir, "operator_photos").apply { mkdirs() }
    val file = File(dir, "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
    file.absolutePath
} catch (_: Exception) { "" }

private fun copyUriToInternalV2(context: Context, uri: android.net.Uri, prefix: String): String = try {
    val dir = File(context.filesDir, "operator_photos").apply { mkdirs() }
    val file = File(dir, "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID()}.img")
    context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { output -> input.copyTo(output) } } ?: return ""
    file.absolutePath
} catch (_: Exception) { "" }

@Composable
private fun DashboardScreenV2(records: List<PerfRecordV2>, openPerson: (OperatorV2) -> Unit) {
    val summaries = operatorsV2.map { it to calculateSummaryV2(it, records) }
    val withData = summaries.filter { it.second.hasData }
    val average = if (withData.isEmpty()) 0.0 else withData.map { it.second.total }.average()
    val totalKy = summaries.sumOf { it.second.ky }
    val totalKaizen = summaries.sumOf { it.second.kaizen }
    val escaped = summaries.sumOf { it.second.escaped }
    val caught = summaries.sumOf { it.second.caught }
    val photoCount = records.count { it.photoPath.isNotBlank() }

    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Yönetici Dashboard", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Aktif hedef süresi: ${activeMonthsV2()} ay", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCardV2("Ortalama", fmtV2(average), Modifier.weight(1f), Color(0xFFE0F2FE))
                MetricCardV2("Fotoğraflı Kayıt", photoCount.toString(), Modifier.weight(1f), Color(0xFFEDE9FE))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCardV2("Toplam KY", fmt0V2(totalKy), Modifier.weight(1f), Color(0xFFDCFCE7))
                MetricCardV2("Toplam Kaizen", fmt0V2(totalKaizen), Modifier.weight(1f), Color(0xFFFEF3C7))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCardV2("Kaçan Hata", fmt0V2(escaped), Modifier.weight(1f), Color(0xFFFEE2E2))
                MetricCardV2("Yakalanan", fmt0V2(caught), Modifier.weight(1f), Color(0xFFDCFCE7))
            }
        }
        item { Text("Personel Performansı", fontSize = 19.sp, fontWeight = FontWeight.Bold) }
        items(summaries) { (op, s) ->
            Card(modifier = Modifier.fillMaxWidth().clickable { openPerson(op) }, colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(op.name, fontWeight = FontWeight.Bold)
                        Text("Sicil ${op.sicil} • KY ${fmt0V2(s.ky)} • Kaizen ${fmt0V2(s.kaizen)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (s.hasData) {
                        Text(fmtV2(s.total), fontSize = 23.sp, fontWeight = FontWeight.Bold, color = scoreColorV2(s.total))
                        Spacer(Modifier.width(8.dp))
                        GradeBadgeV2(s.grade)
                    } else Text("Veri yok", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun EntryScreenV2(records: SnapshotStateList<PerfRecordV2>, referencePhotos: Map<String, String>, onSaved: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var operator by remember { mutableStateOf(operatorsV2.first()) }
    var type by remember { mutableStateOf(recordTypesV2.first()) }
    var defect by remember { mutableStateOf(defectWeightsV2.keys.first()) }
    var amount by remember { mutableStateOf("1") }
    var machine by remember { mutableStateOf(machinesV2.first()) }
    var part by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var photoPath by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var deleteCandidate by remember { mutableStateOf<PerfRecordV2?>(null) }
    val isError = type == "Kaçan Hata" || type == "Yakalanan Hata"

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) copyUriToInternalV2(context, uri, "record").takeIf { it.isNotBlank() }?.let { photoPath = it }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) saveBitmapV2(context, bitmap, "record").takeIf { it.isNotBlank() }?.let { photoPath = it }
    }

    fun clearForm() {
        operator = operatorsV2.first(); type = recordTypesV2.first(); defect = defectWeightsV2.keys.first(); amount = "1"
        machine = machinesV2.first(); part = ""; note = ""; photoPath = ""; editingId = null
    }
    fun startEdit(r: PerfRecordV2) {
        operator = operatorsV2.firstOrNull { it.sicil == r.operatorSicil } ?: operatorsV2.first()
        type = r.type; defect = if (r.defect.isNotBlank()) r.defect else defectWeightsV2.keys.first()
        amount = if (r.amount % 1.0 == 0.0) r.amount.toInt().toString() else r.amount.toString()
        machine = if (r.machine.isNotBlank()) r.machine else machinesV2.first(); part = r.part; note = r.note; photoPath = r.photoPath
        editingId = r.id; message = "Düzenleme modu: bilgileri değiştirip güncelleyin."
        scope.launch { scrollState.animateScrollTo(0) }
    }

    Column(Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (editingId == null) "Günlük Kayıt" else "Kaydı Düzenle", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        if (editingId != null) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("✎ Mevcut kayıt düzenleniyor", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color(0xFF9A3412))
                    TextButton(onClick = { clearForm(); message = "Düzenleme iptal edildi." }) { Text("İPTAL") }
                }
            }
        }
        DropdownSelectorV2("Operatör", operatorsV2, operator, { "${it.sicil} - ${it.name}" }) { operator = it }
        DropdownSelectorV2("Kayıt Türü", recordTypesV2, type, { it }) { type = it }
        if (isError) {
            DropdownSelectorV2("Hata Türü", defectWeightsV2.keys.toList(), defect, { "$it  ×${defectWeightsV2[it]}" }) { defect = it }
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Hata Örneği • $defect", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(defectGuidesV2[defect] ?: "", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val ref = referencePhotos[defect].orEmpty()
                    if (ref.isNotBlank()) ExpandableLocalPhotoV2(ref, 170.dp)
                    else Text("Bu hata için henüz örnek fotoğraf eklenmedi. Hatalar sekmesinden ekleyebilirsiniz.", color = Color(0xFFB45309), fontSize = 12.sp)
                }
            }
        }
        OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text(if (type == "Mesai") "Saat" else if (type.contains("İzin") || type == "Rapor" || type == "Devamsızlık") "Gün" else "Miktar") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)
        DropdownSelectorV2("Makine", machinesV2, machine, { it }) { machine = it }
        OutlinedTextField(value = part, onValueChange = { part = it }, label = { Text("Model / Parça") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Açıklama / Detay") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Kayıt Fotoğrafı", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("Gerçek hatayı/olayı fotoğrafla kayıt altına alın.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (photoPath.isNotBlank()) ExpandableLocalPhotoV2(photoPath, 180.dp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { cameraLauncher.launch(null) }, modifier = Modifier.weight(1f)) { Text("📷 Kamera") }
                    OutlinedButton(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) { Text("▣ Galeri") }
                }
                if (photoPath.isNotBlank()) TextButton(onClick = { photoPath = "" }) { Text("Fotoğrafı kaldır", color = MaterialTheme.colorScheme.error) }
            }
        }

        Button(onClick = {
            val n = amount.replace(',', '.').toDoubleOrNull() ?: 0.0
            if (n <= 0.0) message = "Miktar 0'dan büyük olmalı."
            else {
                val idx = editingId?.let { id -> records.indexOfFirst { it.id == id } } ?: -1
                if (idx >= 0) {
                    val old = records[idx]
                    records[idx] = old.copy(operatorSicil = operator.sicil, type = type, defect = if (isError) defect else "", amount = n, machine = machine, part = part.trim(), note = note.trim(), photoPath = photoPath)
                    onSaved(); clearForm(); message = "✓ Kayıt güncellendi"
                } else {
                    records.add(PerfRecordV2(System.currentTimeMillis(), System.currentTimeMillis(), operator.sicil, type, if (isError) defect else "", n, machine, part.trim(), note.trim(), photoPath))
                    onSaved(); message = "✓ Kayıt kaydedildi"; note = ""; part = ""; amount = "1"; photoPath = ""
                }
            }
        }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(if (editingId == null) "KAYDI KAYDET" else "KAYDI GÜNCELLE", fontWeight = FontWeight.Bold) }

        if (message.isNotBlank()) Text(message, color = if (message.startsWith("✓")) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        HorizontalDivider()
        Text("Son Kayıtlar", fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Text("Yanlış kayıtları buradan düzenleyebilir veya silebilirsiniz.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        records.takeLast(10).reversed().forEach { r -> DetailedRecordCardV2(r, { startEdit(r) }, { deleteCandidate = r }) }
        Spacer(Modifier.height(24.dp))
    }

    deleteCandidate?.let { candidate ->
        AlertDialog(onDismissRequest = { deleteCandidate = null }, title = { Text("Kaydı sil") }, text = { Text("Bu kayıt silinsin mi?") }, confirmButton = {
            TextButton(onClick = { records.remove(candidate); onSaved(); if (editingId == candidate.id) clearForm(); deleteCandidate = null; message = "Kayıt silindi." }) { Text("SİL", color = MaterialTheme.colorScheme.error) }
        }, dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("VAZGEÇ") } })
    }
}

@Composable
private fun DetailedRecordCardV2(r: PerfRecordV2, onEdit: () -> Unit, onDelete: () -> Unit) {
    val op = operatorsV2.firstOrNull { it.sicil == r.operatorSicil }
    val bg = when (r.type) { "Kaçan Hata" -> Color(0xFFFFF1F2); "Yakalanan Hata" -> Color(0xFFF0FDF4); "Kaizen" -> Color(0xFFFFFBEB); "KY" -> Color(0xFFEFF6FF); else -> Color(0xFFF8FAFC) }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = bg)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${op?.name ?: r.operatorSicil} • ${r.type}", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("Sicil: ${r.operatorSicil} • ${formatDateV2(r.timestamp)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (r.photoPath.isNotBlank()) Text("📷", fontSize = 21.sp)
            }
            Text("${fmt0V2(r.amount)} ${unitForV2(r.type)}${if (r.defect.isNotBlank()) " • Hata: ${r.defect} (×${defectWeightsV2[r.defect] ?: 1.0})" else ""}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (r.machine.isNotBlank()) Text("Makine: ${r.machine}", fontSize = 13.sp)
            if (r.part.isNotBlank()) Text("Model / Parça: ${r.part}", fontSize = 13.sp)
            if (r.note.isNotBlank()) Text("Açıklama: ${r.note}", fontSize = 13.sp)
            if (r.photoPath.isNotBlank()) ExpandableLocalPhotoV2(r.photoPath, 150.dp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onEdit) { Text("✎ DÜZENLE") }
                TextButton(onClick = onDelete) { Text("SİL", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun DefectLibraryScreenV2(referencePhotos: MutableMap<String, String>, onChanged: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var targetDefect by remember { mutableStateOf<String?>(null) }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val defect = targetDefect
        if (uri != null && defect != null) copyUriToInternalV2(context, uri, "reference").takeIf { it.isNotBlank() }?.let { referencePhotos[defect] = it; onChanged() }
        targetDefect = null
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        val defect = targetDefect
        if (bitmap != null && defect != null) saveBitmapV2(context, bitmap, "reference").takeIf { it.isNotBlank() }?.let { referencePhotos[defect] = it; onChanged() }
        targetDefect = null
    }

    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Hata Fotoğraf Kütüphanesi", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Her hata için örnek fotoğraf yükleyin. Operatör kayıt ekranında hata seçtiğinde bu fotoğraf otomatik gösterilir.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        items(defectWeightsV2.keys.toList()) { defect ->
            val path = referencePhotos[defect].orEmpty()
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(defect, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            Text("Hata katsayısı: ×${defectWeightsV2[defect]}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        if (path.isNotBlank()) Text("✓ Fotoğraf var", color = Color(0xFF15803D), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(defectGuidesV2[defect] ?: "", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (path.isNotBlank()) ExpandableLocalPhotoV2(path, 190.dp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { targetDefect = defect; cameraLauncher.launch(null) }, modifier = Modifier.weight(1f)) { Text("📷 Çek") }
                        OutlinedButton(onClick = { targetDefect = defect; galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) { Text("▣ Galeriden") }
                    }
                    if (path.isNotBlank()) TextButton(onClick = { referencePhotos.remove(defect); onChanged() }) { Text("Örnek fotoğrafı kaldır", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
private fun PersonScreenV2(records: List<PerfRecordV2>, selected: OperatorV2, select: (OperatorV2) -> Unit, delete: (PerfRecordV2) -> Unit) {
    val s = calculateSummaryV2(selected, records)
    val own = records.filter { it.operatorSicil == selected.sicil && inPeriodV2(it) }.sortedByDescending { it.timestamp }
    val components = listOf("Kalite" to (s.qualityScore / 40.0), "KY" to (s.kyScore / 15.0), "Kaizen" to (s.kaizenScore / 15.0), "Devam" to (s.attendanceScore / 20.0), "Mesai" to (s.overtimeScore / 10.0))
    val strongest = components.maxByOrNull { it.second }?.first ?: "—"
    val development = components.minByOrNull { it.second }?.first ?: "—"

    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { DropdownSelectorV2("Operatör", operatorsV2, selected, { "${it.sicil} - ${it.name}" }) { select(it) } }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(selected.name, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text("Sicil: ${selected.sicil}") }
                    if (s.hasData) Column(horizontalAlignment = Alignment.End) { Text("${fmtV2(s.total)} / 100", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = scoreColorV2(s.total)); GradeBadgeV2(s.grade) } else Text("Veri yok")
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCardV2("KY", fmt0V2(s.ky), Modifier.weight(1f), Color(0xFFDCFCE7)); MetricCardV2("Kaizen", fmt0V2(s.kaizen), Modifier.weight(1f), Color(0xFFFEF3C7)); MetricCardV2("Mesai", "${fmt0V2(s.overtime)} s", Modifier.weight(1f), Color(0xFFE0E7FF))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCardV2("Kaçan", fmt0V2(s.escaped), Modifier.weight(1f), Color(0xFFFEE2E2)); MetricCardV2("Yakalanan", fmt0V2(s.caught), Modifier.weight(1f), Color(0xFFDCFCE7)); MetricCardV2("Devamsızlık", fmt0V2(s.absence), Modifier.weight(1f), Color(0xFFF3F4F6))
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Puan Bileşenleri", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    ScoreRowV2("Kalite", s.qualityScore, 40.0); ScoreRowV2("KY", s.kyScore, 15.0); ScoreRowV2("Kaizen", s.kaizenScore, 15.0); ScoreRowV2("Devam", s.attendanceScore, 20.0); ScoreRowV2("Mesai / Katılım", s.overtimeScore, 10.0)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCardV2("Güçlü Alan", if (s.hasData) strongest else "—", Modifier.weight(1f), Color(0xFFDCFCE7)); MetricCardV2("Gelişim Alanı", if (s.hasData) development else "—", Modifier.weight(1f), Color(0xFFFFE4E6))
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("Performans Görüşmesi Özeti", fontSize = 18.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp))
                    if (!s.hasData) Text("Bu dönem için henüz kayıt bulunmuyor.") else Text("${selected.name}; KY ${fmt0V2(s.ky)}, Kaizen ${fmt0V2(s.kaizen)}, kaçan hata ${fmt0V2(s.escaped)}, yakalanan hata ${fmt0V2(s.caught)}, mesai ${fmt0V2(s.overtime)} saat, yıllık izin ${fmt0V2(s.annualLeave)} gün, günlük izin ${fmt0V2(s.dailyLeave)} gün, rapor ${fmt0V2(s.report)} gün ve devamsızlık ${fmt0V2(s.absence)} gün. Toplam performans puanı ${fmtV2(s.total)}/100, sınıfı ${s.grade}.")
                }
            }
        }
        item { Text("Detaylı Kayıt Geçmişi", fontSize = 19.sp, fontWeight = FontWeight.Bold) }
        if (own.isEmpty()) item { Text("Kayıt yok.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(own, key = { it.id }) { r ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${r.type}${if (r.defect.isNotBlank()) " • ${r.defect}" else ""}", fontWeight = FontWeight.Bold)
                    Text("${fmt0V2(r.amount)} ${unitForV2(r.type)} • ${formatDateV2(r.timestamp)}", fontSize = 12.sp)
                    if (r.machine.isNotBlank()) Text("Makine: ${r.machine}", fontSize = 12.sp)
                    if (r.part.isNotBlank()) Text("Parça: ${r.part}", fontSize = 12.sp)
                    if (r.note.isNotBlank()) Text("Açıklama: ${r.note}", fontSize = 12.sp)
                    if (r.photoPath.isNotBlank()) ExpandableLocalPhotoV2(r.photoPath, 130.dp)
                    TextButton(onClick = { delete(r) }) { Text("Kaydı sil", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreenV2() {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Ayarlar & Puanlama", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Sürüm 1.1 • Fotoğraflı Kayıt", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("• Hata kütüphanesine standart örnek fotoğraf ekleme"); Text("• Kayıt sırasında kamera veya galeriden fotoğraf"); Text("• Son kayıtlardan düzenleme ve silme"); Text("• Detaylı kayıt kartları ve fotoğraf büyütme"); Text("• Eski sürümdeki kayıtlar korunur")
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("100 Puan Modeli", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Kalite 40 • KY 15 • Kaizen 15 • Devam 20 • Mesai/Katılım 10")
                    Text("Kalite başlangıcı 35 puandır. Kaçan ağırlıklı hata ×4 ceza, yakalanan ağırlıklı hata ×0,5 bonus verir (bonus tavanı 5).")
                    Text("Aylık KY hedefi 6, Kaizen hedefi 1, mesai hedefi 10 saattir. Devamsızlık her gün 5 puan düşürür.")
                    Text("Yıllık/günlük izin ve rapor bilgi amaçlı takip edilir; doğrudan puan düşürmez.")
                }
            }
        }
    }
}

@Composable
private fun ExpandableLocalPhotoV2(path: String, height: androidx.compose.ui.unit.Dp) {
    var expanded by remember { mutableStateOf(false) }
    val bitmap = remember(path) { if (path.isNotBlank()) BitmapFactory.decodeFile(path) else null }
    if (bitmap != null) {
        Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Fotoğraf", modifier = Modifier.fillMaxWidth().height(height).clickable { expanded = true }, contentScale = ContentScale.Crop)
        if (expanded) Dialog(onDismissRequest = { expanded = false }) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.Black), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp)) {
                    Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Büyük fotoğraf", modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp, max = 620.dp), contentScale = ContentScale.Fit)
                    TextButton(onClick = { expanded = false }, modifier = Modifier.align(Alignment.End)) { Text("KAPAT", color = Color.White) }
                }
            }
        }
    } else Box(Modifier.fillMaxWidth().height(height).background(Color(0xFFE5E7EB)), contentAlignment = Alignment.Center) { Text("Fotoğraf açılamadı", color = Color(0xFF6B7280)) }
}

@Composable
private fun <T> DropdownSelectorV2(label: String, options: List<T>, selected: T, text: (T) -> String, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(4.dp))
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(text(selected), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface); Text("▼") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(text(option)) }, onClick = { onSelect(option); expanded = false }) }
        }
    }
}

@Composable
private fun MetricCardV2(title: String, value: String, modifier: Modifier, color: Color) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = color)) {
        Column(Modifier.padding(12.dp)) { Text(title, fontSize = 11.sp, color = Color(0xFF4B5563)); Text(value, fontSize = 19.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun GradeBadgeV2(grade: String) {
    val color = when (grade) { "A" -> Color(0xFF15803D); "B" -> Color(0xFF0369A1); "C" -> Color(0xFFB45309); "D" -> Color(0xFFB91C1C); else -> Color.Gray }
    Surface(color = color, shape = RoundedCornerShape(9.dp)) { Text(grade, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) }
}

@Composable
private fun ScoreRowV2(label: String, value: Double, maxValue: Double) {
    Column {
        Row(Modifier.fillMaxWidth()) { Text(label, Modifier.weight(1f)); Text("${fmtV2(value)} / ${fmt0V2(maxValue)}", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(4.dp)); LinearProgressIndicator(progress = { (value / maxValue).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
    }
}

private fun fmtV2(v: Double): String = String.format(Locale("tr", "TR"), "%.1f", v)
private fun fmt0V2(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else String.format(Locale("tr", "TR"), "%.1f", v)
private fun unitForV2(type: String): String = when (type) { "Mesai" -> "saat"; "Yıllık İzin", "Günlük İzin", "Rapor", "Devamsızlık" -> "gün"; else -> "adet" }
private fun formatDateV2(timestamp: Long): String = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR")).format(Date(timestamp))
private fun scoreColorV2(score: Double): Color = when { score >= 90 -> Color(0xFF15803D); score >= 80 -> Color(0xFF0369A1); score >= 70 -> Color(0xFFB45309); else -> Color(0xFFB91C1C) }
