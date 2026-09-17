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

private data class Operator2(val sicil: String, val name: String)
private data class Record2(
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
private data class Score2(
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

private val periodStart2 = LocalDate.of(2026, 8, 1)
private val periodEnd2 = LocalDate.of(2027, 7, 31)
private val operators2 = listOf(
    Operator2("6095", "NUSRET BULUT"),
    Operator2("614", "LEVENT DOĞUER"),
    Operator2("6112", "AHMET SEZER"),
    Operator2("3388", "İLYAS ÖZDEMİR"),
    Operator2("2921", "GİRAY ÇALIŞIR"),
    Operator2("4975", "SEZGİN NALBATÇI"),
    Operator2("5828", "EREN YİĞİTOĞLU"),
    Operator2("686", "RUHAN SEVİL TEKEOĞLU"),
    Operator2("596", "FATİH HENDEKÇİ"),
    Operator2("2484", "MESUT MÜHÜRDAROÇ")
)
private val recordTypes2 = listOf(
    "Kaçan Hata", "Yakalanan Hata", "KY", "Kaizen", "Mesai",
    "Yıllık İzin", "Günlük İzin", "Rapor", "Devamsızlık"
)
private val machines2 = listOf("1600T-1", "1600T-2", "1600T-3", "1700T", "850T", "650T")
private val defectWeights2 = linkedMapOf(
    "Şişme" to 1.5, "Felt Eksik" to 1.5, "Çapak" to 1.0,
    "Yolluk Kalma" to 3.0, "Yolluk Yapışması" to 1.0, "Çökme" to 1.0,
    "Eksik" to 2.0, "İz" to 1.0, "Yabancı Madde" to 1.0,
    "Hatalı Setleme" to 1.0, "Kabarma" to 1.0, "Beyazlık" to 0.3,
    "Leke" to 0.1, "Deforme" to 0.2, "Diğer" to 1.0
)
private val defectHelp2 = mapOf(
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

private object Store2 {
    private const val PREFS = "operator_takip_prefs"
    private const val RECORDS = "records"
    private const val REFERENCES = "defect_reference_photos"

    fun loadRecords(context: Context): List<Record2> = try {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(RECORDS, "[]") ?: "[]"
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(
                    Record2(
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
    } catch (_: Exception) { emptyList() }

    fun saveRecords(context: Context, records: List<Record2>) {
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

    fun loadReferences(context: Context): MutableMap<String, String> = try {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(REFERENCES, "{}") ?: "{}"
        val obj = JSONObject(raw)
        mutableMapOf<String, String>().apply {
            defectWeights2.keys.forEach { defect ->
                val path = obj.optString(defect)
                if (path.isNotBlank()) put(defect, path)
            }
        }
    } catch (_: Exception) { mutableMapOf() }

    fun saveReferences(context: Context, refs: Map<String, String>) {
        val obj = JSONObject()
        refs.forEach { (defect, path) -> obj.put(defect, path) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(REFERENCES, obj.toString()).apply()
    }
}

class MainActivityV2 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OperatorApp2() }
    }
}

@Composable
private fun OperatorApp2() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val records = remember { mutableStateListOf<Record2>().apply { addAll(Store2.loadRecords(context)) } }
    val refs = remember { mutableStateMapOf<String, String>().apply { putAll(Store2.loadReferences(context)) } }
    var tab by remember { mutableIntStateOf(0) }
    var selectedPerson by remember { mutableStateOf(operators2.first()) }

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
                        Text("Fotoğraflı Performans Sistemi • v1.1", color = Color.White.copy(alpha = .82f), fontSize = 11.sp)
                    }
                }
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Text("⌂") }, label = { Text("Ana") })
                    NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Text("+") }, label = { Text("Kayıt") })
                    NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Text("▣") }, label = { Text("Hatalar") })
                    NavigationBarItem(selected = tab == 3, onClick = { tab = 3 }, icon = { Text("◎") }, label = { Text("Personel") })
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                when (tab) {
                    0 -> Dashboard2(records) { op -> selectedPerson = op; tab = 3 }
                    1 -> Entry2(records, refs) { Store2.saveRecords(context, records) }
                    2 -> DefectLibrary2(refs) { Store2.saveReferences(context, refs) }
                    else -> Person2(records, selectedPerson, onSelect = { selectedPerson = it }, onDelete = {
                        records.remove(it)
                        Store2.saveRecords(context, records)
                    })
                }
            }
        }
    }
}

private fun activeMonths2(): Int {
    val today = LocalDate.now()
    val capped = when {
        today.isBefore(periodStart2) -> periodStart2
        today.isAfter(periodEnd2) -> periodEnd2
        else -> today
    }
    return (ChronoUnit.MONTHS.between(YearMonth.from(periodStart2), YearMonth.from(capped)).toInt() + 1).coerceIn(1, 12)
}

private fun inPeriod2(record: Record2): Boolean {
    val date = Instant.ofEpochMilli(record.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    return !date.isBefore(periodStart2) && !date.isAfter(periodEnd2)
}

private fun calculate2(op: Operator2, all: List<Record2>): Score2 {
    val rows = all.filter { it.operatorSicil == op.sicil && inPeriod2(it) }
    fun sum(type: String) = rows.filter { it.type == type }.sumOf { it.amount }
    fun weighted(type: String) = rows.filter { it.type == type }.sumOf { it.amount * (defectWeights2[it.defect] ?: 1.0) }

    val escaped = sum("Kaçan Hata")
    val caught = sum("Yakalanan Hata")
    val ky = sum("KY")
    val kaizen = sum("Kaizen")
    val overtime = sum("Mesai")
    val absence = sum("Devamsızlık")
    val months = activeMonths2()
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
    return Score2(
        hasData = rows.isNotEmpty(), escaped = escaped, caught = caught, ky = ky, kaizen = kaizen,
        overtime = overtime, annualLeave = sum("Yıllık İzin"), dailyLeave = sum("Günlük İzin"),
        report = sum("Rapor"), absence = absence, quality = quality, kyScore = kyScore,
        kaizenScore = kaizenScore, attendance = attendance, overtimeScore = overtimeScore,
        total = if (rows.isEmpty()) 0.0 else total, grade = if (rows.isEmpty()) "—" else grade
    )
}

private fun saveCameraPhoto2(context: Context, bitmap: Bitmap, prefix: String): String = try {
    val dir = File(context.filesDir, "operator_photos").apply { mkdirs() }
    val file = File(dir, "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
    file.absolutePath
} catch (_: Exception) { "" }

private fun saveGalleryPhoto2(context: Context, uri: Uri, prefix: String): String {
    return try {
        val dir = File(context.filesDir, "operator_photos").apply { mkdirs() }
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID()}.img")
        val input = context.contentResolver.openInputStream(uri) ?: return ""
        input.use { source -> file.outputStream().use { target -> source.copyTo(target) } }
        file.absolutePath
    } catch (_: Exception) { "" }
}

@Composable
private fun Dashboard2(records: List<Record2>, openPerson: (Operator2) -> Unit) {
    val summaries = operators2.map { it to calculate2(it, records) }
    val withData = summaries.filter { it.second.hasData }
    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Yönetici Dashboard", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("01.08.2026 – 31.07.2027 • Aktif hedef ${activeMonths2()} ay", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric2("Ortalama Puan", if (withData.isEmpty()) "—" else format1(withData.map { it.second.total }.average()), Modifier.weight(1f), Color(0xFFE0F2FE))
                Metric2("Fotoğraflı Kayıt", records.count { it.photoPath.isNotBlank() }.toString(), Modifier.weight(1f), Color(0xFFEDE9FE))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric2("Kaçan Hata", format0(summaries.sumOf { it.second.escaped }), Modifier.weight(1f), Color(0xFFFEE2E2))
                Metric2("Yakalanan", format0(summaries.sumOf { it.second.caught }), Modifier.weight(1f), Color(0xFFDCFCE7))
            }
        }
        item { Text("Personel Performansı", fontSize = 19.sp, fontWeight = FontWeight.Bold) }
        items(summaries) { (op, score) ->
            Card(modifier = Modifier.fillMaxWidth().clickable { openPerson(op) }) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(op.name, fontWeight = FontWeight.Bold)
                        Text("Sicil ${op.sicil} • KY ${format0(score.ky)} • Kaizen ${format0(score.kaizen)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (score.hasData) {
                        Text(format1(score.total), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = scoreColor2(score.total))
                        Spacer(Modifier.width(8.dp))
                        GradeBadge2(score.grade)
                    } else Text("Veri yok", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun Entry2(records: SnapshotStateList<Record2>, references: Map<String, String>, save: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scroll = rememberScrollState()
    var operator by remember { mutableStateOf(operators2.first()) }
    var type by remember { mutableStateOf(recordTypes2.first()) }
    var defect by remember { mutableStateOf(defectWeights2.keys.first()) }
    var amount by remember { mutableStateOf("1") }
    var machine by remember { mutableStateOf(machines2.first()) }
    var part by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var photoPath by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var message by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<Record2?>(null) }
    val isError = type == "Kaçan Hata" || type == "Yakalanan Hata"

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val path = saveGalleryPhoto2(context, uri, "record")
            if (path.isNotBlank()) photoPath = path
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val path = saveCameraPhoto2(context, bitmap, "record")
            if (path.isNotBlank()) photoPath = path
        }
    }

    fun clearForm() {
        operator = operators2.first()
        type = recordTypes2.first()
        defect = defectWeights2.keys.first()
        amount = "1"
        machine = machines2.first()
        part = ""
        note = ""
        photoPath = ""
        editingId = null
    }

    fun loadForEdit(record: Record2) {
        operator = operators2.firstOrNull { it.sicil == record.operatorSicil } ?: operators2.first()
        type = record.type
        defect = record.defect.ifBlank { defectWeights2.keys.first() }
        amount = if (record.amount % 1.0 == 0.0) record.amount.toInt().toString() else record.amount.toString()
        machine = record.machine.ifBlank { machines2.first() }
        part = record.part
        note = record.note
        photoPath = record.photoPath
        editingId = record.id
        message = "✎ Düzenleme modu açık."
    }

    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (editingId == null) "Günlük Kayıt" else "Kaydı Düzenle", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        if (editingId != null) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED))) {
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Mevcut kayıt düzenleniyor", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color(0xFF9A3412))
                    TextButton(onClick = { clearForm(); message = "Düzenleme iptal edildi." }) { Text("İPTAL") }
                }
            }
        }

        Selector2("Operatör", operators2, operator, { "${it.sicil} - ${it.name}" }) { operator = it }
        Selector2("Kayıt Türü", recordTypes2, type, { it }) { type = it }

        if (isError) {
            Selector2("Hata Türü", defectWeights2.keys.toList(), defect, { "$it  ×${defectWeights2[it]}" }) { defect = it }
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF))) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Hata Örneği • $defect", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(defectHelp2[defect] ?: "", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val reference = references[defect].orEmpty()
                    if (reference.isNotBlank()) LocalPhoto2(reference, 170.dp)
                    else Text("Bu hata için örnek fotoğraf yok. Hatalar sekmesinden ekleyin.", color = Color(0xFFB45309), fontSize = 12.sp)
                }
            }
        }

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(if (type == "Mesai") "Saat" else if (type.contains("İzin") || type == "Rapor" || type == "Devamsızlık") "Gün" else "Miktar") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )
        Selector2("Makine", machines2, machine, { it }) { machine = it }
        OutlinedTextField(value = part, onValueChange = { part = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Model / Parça") }, singleLine = true)
        OutlinedTextField(value = note, onValueChange = { note = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Açıklama / Detay") }, minLines = 3)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Kayıt Fotoğrafı", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("Gerçek hatayı veya olayı fotoğrafla kayıt altına alın.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (photoPath.isNotBlank()) LocalPhoto2(photoPath, 180.dp)
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
                if (number <= 0) {
                    message = "Miktar 0'dan büyük olmalı."
                } else {
                    val index = editingId?.let { id -> records.indexOfFirst { it.id == id } } ?: -1
                    if (index >= 0) {
                        val old = records[index]
                        records[index] = old.copy(
                            operatorSicil = operator.sicil,
                            type = type,
                            defect = if (isError) defect else "",
                            amount = number,
                            machine = machine,
                            part = part.trim(),
                            note = note.trim(),
                            photoPath = photoPath
                        )
                        save()
                        clearForm()
                        message = "✓ Kayıt güncellendi"
                    } else {
                        val now = System.currentTimeMillis()
                        records.add(Record2(now, now, operator.sicil, type, if (isError) defect else "", number, machine, part.trim(), note.trim(), photoPath))
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
        Text("Son 10 kayıt tüm detaylarıyla gösterilir. Yanlış kaydı Düzenle ile düzeltin.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        records.takeLast(10).reversed().forEach { record ->
            RecordCard2(record, onEdit = { loadForEdit(record) }, onDelete = { deleteTarget = record })
        }
        Spacer(Modifier.height(24.dp))
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
                    if (editingId == target.id) clearForm()
                    deleteTarget = null
                    message = "Kayıt silindi."
                }) { Text("SİL", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("VAZGEÇ") } }
        )
    }
}

@Composable
private fun RecordCard2(record: Record2, onEdit: () -> Unit, onDelete: () -> Unit) {
    val op = operators2.firstOrNull { it.sicil == record.operatorSicil }
    val background = when (record.type) {
        "Kaçan Hata" -> Color(0xFFFFF1F2)
        "Yakalanan Hata" -> Color(0xFFF0FDF4)
        "KY" -> Color(0xFFEFF6FF)
        "Kaizen" -> Color(0xFFFFFBEB)
        else -> Color(0xFFF8FAFC)
    }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = background)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${op?.name ?: record.operatorSicil} • ${record.type}", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Sicil ${record.operatorSicil} • ${dateText2(record.timestamp)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (record.photoPath.isNotBlank()) Text("📷", fontSize = 20.sp)
            }
            Text("${format0(record.amount)} ${unit2(record.type)}${if (record.defect.isNotBlank()) " • Hata: ${record.defect} ×${defectWeights2[record.defect] ?: 1.0}" else ""}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (record.machine.isNotBlank()) Text("Makine: ${record.machine}", fontSize = 13.sp)
            if (record.part.isNotBlank()) Text("Model / Parça: ${record.part}", fontSize = 13.sp)
            if (record.note.isNotBlank()) Text("Açıklama: ${record.note}", fontSize = 13.sp)
            if (record.photoPath.isNotBlank()) LocalPhoto2(record.photoPath, 150.dp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onEdit) { Text("✎ DÜZENLE") }
                TextButton(onClick = onDelete) { Text("SİL", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun DefectLibrary2(refs: MutableMap<String, String>, saveRefs: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var targetDefect by remember { mutableStateOf<String?>(null) }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val target = targetDefect
        if (uri != null && target != null) {
            val path = saveGalleryPhoto2(context, uri, "reference")
            if (path.isNotBlank()) { refs[target] = path; saveRefs() }
        }
        targetDefect = null
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        val target = targetDefect
        if (bitmap != null && target != null) {
            val path = saveCameraPhoto2(context, bitmap, "reference")
            if (path.isNotBlank()) { refs[target] = path; saveRefs() }
        }
        targetDefect = null
    }

    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Hata Fotoğraf Kütüphanesi", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Her hata için örnek fotoğraf ekleyin. Kayıtta hata seçildiğinde operatöre bu görsel gösterilir.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(defectWeights2.keys.toList()) { defect ->
            val photo = refs[defect].orEmpty()
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(defect, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            Text("Katsayı ×${defectWeights2[defect]}", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                        }
                        if (photo.isNotBlank()) Text("✓ Fotoğraf var", color = Color(0xFF15803D), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Text(defectHelp2[defect] ?: "", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (photo.isNotBlank()) LocalPhoto2(photo, 190.dp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { targetDefect = defect; camera.launch(null) }, modifier = Modifier.weight(1f)) { Text("📷 Çek") }
                        OutlinedButton(onClick = { targetDefect = defect; gallery.launch("image/*") }, modifier = Modifier.weight(1f)) { Text("▣ Galeriden") }
                    }
                    if (photo.isNotBlank()) TextButton(onClick = { refs.remove(defect); saveRefs() }) { Text("Örnek fotoğrafı kaldır", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
private fun Person2(records: List<Record2>, selected: Operator2, onSelect: (Operator2) -> Unit, onDelete: (Record2) -> Unit) {
    val score = calculate2(selected, records)
    val history = records.filter { it.operatorSicil == selected.sicil && inPeriod2(it) }.sortedByDescending { it.timestamp }
    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Selector2("Operatör", operators2, selected, { "${it.sicil} - ${it.name}" }, onSelect) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(selected.name, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text("Sicil: ${selected.sicil}") }
                    if (score.hasData) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${format1(score.total)} / 100", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = scoreColor2(score.total))
                            GradeBadge2(score.grade)
                        }
                    } else Text("Veri yok")
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric2("KY", format0(score.ky), Modifier.weight(1f), Color(0xFFDCFCE7))
                Metric2("Kaizen", format0(score.kaizen), Modifier.weight(1f), Color(0xFFFEF3C7))
                Metric2("Mesai", "${format0(score.overtime)} s", Modifier.weight(1f), Color(0xFFE0E7FF))
            }
        }
        item {
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Puan Bileşenleri", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    ScoreBar2("Kalite", score.quality, 40.0)
                    ScoreBar2("KY", score.kyScore, 15.0)
                    ScoreBar2("Kaizen", score.kaizenScore, 15.0)
                    ScoreBar2("Devam", score.attendance, 20.0)
                    ScoreBar2("Mesai", score.overtimeScore, 10.0)
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(14.dp)) {
                    Text("Performans Özeti", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (!score.hasData) Text("Bu dönem için kayıt yok.")
                    else Text("KY ${format0(score.ky)}, Kaizen ${format0(score.kaizen)}, kaçan hata ${format0(score.escaped)}, yakalanan hata ${format0(score.caught)}, mesai ${format0(score.overtime)} saat, yıllık izin ${format0(score.annualLeave)} gün, günlük izin ${format0(score.dailyLeave)} gün, rapor ${format0(score.report)} gün, devamsızlık ${format0(score.absence)} gün. Toplam ${format1(score.total)}/100, sınıf ${score.grade}.")
                }
            }
        }
        item { Text("Detaylı Kayıt Geçmişi", fontSize = 19.sp, fontWeight = FontWeight.Bold) }
        if (history.isEmpty()) item { Text("Kayıt yok.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(history, key = { it.id }) { record ->
            Card {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${record.type}${if (record.defect.isNotBlank()) " • ${record.defect}" else ""}", fontWeight = FontWeight.Bold)
                    Text("${format0(record.amount)} ${unit2(record.type)} • ${dateText2(record.timestamp)}", fontSize = 12.sp)
                    if (record.machine.isNotBlank()) Text("Makine: ${record.machine}", fontSize = 12.sp)
                    if (record.part.isNotBlank()) Text("Parça: ${record.part}", fontSize = 12.sp)
                    if (record.note.isNotBlank()) Text("Açıklama: ${record.note}", fontSize = 12.sp)
                    if (record.photoPath.isNotBlank()) LocalPhoto2(record.photoPath, 130.dp)
                    TextButton(onClick = { onDelete(record) }) { Text("Kaydı sil", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
private fun LocalPhoto2(path: String, height: Dp) {
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
private fun <T> Selector2(label: String, options: List<T>, selected: T, display: (T) -> String, onSelect: (T) -> Unit) {
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
private fun Metric2(title: String, value: String, modifier: Modifier, color: Color) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color)) {
        Column(Modifier.padding(12.dp)) {
            Text(title, fontSize = 11.sp, color = Color(0xFF4B5563))
            Text(value, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun GradeBadge2(grade: String) {
    val color = when (grade) { "A" -> Color(0xFF15803D); "B" -> Color(0xFF0369A1); "C" -> Color(0xFFB45309); else -> Color(0xFFB91C1C) }
    Surface(color = color, shape = RoundedCornerShape(9.dp)) {
        Text(grade, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
    }
}

@Composable
private fun ScoreBar2(title: String, value: Double, maximum: Double) {
    Column {
        Row(Modifier.fillMaxWidth()) { Text(title, Modifier.weight(1f)); Text("${format1(value)} / ${format0(maximum)}", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(progress = { (value / maximum).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
    }
}

private fun format1(value: Double): String = String.format(Locale("tr", "TR"), "%.1f", value)
private fun format0(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else format1(value)
private fun unit2(type: String): String = when (type) { "Mesai" -> "saat"; "Yıllık İzin", "Günlük İzin", "Rapor", "Devamsızlık" -> "gün"; else -> "adet" }
private fun dateText2(timestamp: Long): String = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR")).format(Date(timestamp))
private fun scoreColor2(score: Double): Color = when { score >= 90 -> Color(0xFF15803D); score >= 80 -> Color(0xFF0369A1); score >= 70 -> Color(0xFFB45309); else -> Color(0xFFB91C1C) }
