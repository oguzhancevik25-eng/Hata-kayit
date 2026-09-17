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

private data class Op2(val sicil: String, val name: String)
private data class Rec2(
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
private data class Sum2(
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

private val start2 = LocalDate.of(2026, 8, 1)
private val end2 = LocalDate.of(2027, 7, 31)
private val ops2 = listOf(
    Op2("6095", "NUSRET BULUT"), Op2("614", "LEVENT DOĞUER"), Op2("6112", "AHMET SEZER"),
    Op2("3388", "İLYAS ÖZDEMİR"), Op2("2921", "GİRAY ÇALIŞIR"), Op2("4975", "SEZGİN NALBATÇI"),
    Op2("5828", "EREN YİĞİTOĞLU"), Op2("686", "RUHAN SEVİL TEKEOĞLU"), Op2("596", "FATİH HENDEKÇİ"),
    Op2("2484", "MESUT MÜHÜRDAROÇ")
)
private val types2 = listOf("Kaçan Hata", "Yakalanan Hata", "KY", "Kaizen", "Mesai", "Yıllık İzin", "Günlük İzin", "Rapor", "Devamsızlık")
private val machines2 = listOf("1600T-1", "1600T-2", "1600T-3", "1700T", "850T", "650T")
private val defects2 = linkedMapOf(
    "Şişme" to 1.5, "Felt Eksik" to 1.5, "Çapak" to 1.0, "Yolluk Kalma" to 3.0,
    "Yolluk Yapışması" to 1.0, "Çökme" to 1.0, "Eksik" to 2.0, "İz" to 1.0,
    "Yabancı Madde" to 1.0, "Hatalı Setleme" to 1.0, "Kabarma" to 1.0,
    "Beyazlık" to 0.3, "Leke" to 0.1, "Deforme" to 0.2, "Diğer" to 1.0
)
private val guides2 = mapOf(
    "Şişme" to "Parça yüzeyinde şişme veya kabarma görünümü.",
    "Felt Eksik" to "Parçada olması gereken feltin bulunmaması veya yanlış pozisyonda olması.",
    "Çapak" to "Kenar/birleşim bölgesinde istenmeyen plastik fazlalığı.",
    "Yolluk Kalma" to "Parça veya kalıp üzerinde yolluk/gate kalıntısı kalması.",
    "Yolluk Yapışması" to "Yolluğun normal ayrılmayıp kalıba veya parçaya yapışması.",
    "Çökme" to "Yüzeyde içeri doğru çökük görünüm.",
    "Eksik" to "Parçanın bir bölgesinin tam dolmaması.",
    "İz" to "Standart dışı çizgi, akış, gate veya itici izi.",
    "Yabancı Madde" to "Farklı renk, nokta veya malzeme dışı görüntü.",
    "Hatalı Setleme" to "Parça/aparat/ayarın standarda uygun setlenmemesi.",
    "Kabarma" to "Yüzeyde lokal kabarıklık veya yükselti.",
    "Beyazlık" to "Parça yüzeyinde beyazlama veya renk kaybı.",
    "Leke" to "Yüzeyde standart dışı renk, kir veya iz.",
    "Deforme" to "Parça geometrisinde eğilme veya şekil bozukluğu.",
    "Diğer" to "Listede olmayan hata; fotoğraf ve açıklama ile kaydedilir."
)

private object Db2 {
    private const val PREF = "operator_takip_prefs"
    private const val KEY = "records"
    private const val REF = "defect_reference_photos"

    fun load(context: Context): List<Rec2> = try {
        val a = JSONArray(context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]")
        buildList {
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                add(Rec2(
                    o.getLong("id"), o.getLong("timestamp"), o.getString("operatorSicil"), o.getString("type"),
                    o.optString("defect"), o.getDouble("amount"), o.optString("machine"), o.optString("part"),
                    o.optString("note"), o.optString("photoPath")
                ))
            }
        }
    } catch (_: Exception) { emptyList() }

    fun save(context: Context, records: List<Rec2>) {
        val a = JSONArray()
        records.forEach { r ->
            a.put(JSONObject().apply {
                put("id", r.id); put("timestamp", r.timestamp); put("operatorSicil", r.operatorSicil); put("type", r.type)
                put("defect", r.defect); put("amount", r.amount); put("machine", r.machine); put("part", r.part)
                put("note", r.note); put("photoPath", r.photoPath)
            })
        }
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY, a.toString()).apply()
    }

    fun loadRefs(context: Context): MutableMap<String, String> = try {
        val o = JSONObject(context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(REF, "{}") ?: "{}")
        mutableMapOf<String, String>().apply { defects2.keys.forEach { d -> o.optString(d).takeIf { it.isNotBlank() }?.let { put(d, it) } } }
    } catch (_: Exception) { mutableMapOf() }

    fun saveRefs(context: Context, refs: Map<String, String>) {
        val o = JSONObject(); refs.forEach { (k, v) -> o.put(k, v) }
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(REF, o.toString()).apply()
    }
}

class MainActivityV2 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App2() }
    }
}

@Composable
private fun App2() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val records = remember { mutableStateListOf<Rec2>().apply { addAll(Db2.load(context)) } }
    val refs = remember { mutableStateMapOf<String, String>().apply { putAll(Db2.loadRefs(context)) } }
    var tab by remember { mutableIntStateOf(0) }
    var person by remember { mutableStateOf(ops2.first()) }
    val scheme = lightColorScheme(primary = Color(0xFF0B4F7D), secondary = Color(0xFF0E7490), tertiary = Color(0xFF15803D), error = Color(0xFFB91C1C), background = Color(0xFFF5F7FA))

    MaterialTheme(colorScheme = scheme) {
        Scaffold(
            topBar = {
                Surface(color = MaterialTheme.colorScheme.primary, shadowElevation = 3.dp) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Operatör Takip", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text("Fotoğraflı Performans Sistemi • v1.1", color = Color.White.copy(alpha = .82f), fontSize = 11.sp)
                    }
                }
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(tab == 0, { tab = 0 }, { Text("⌂") }, label = { Text("Ana") })
                    NavigationBarItem(tab == 1, { tab = 1 }, { Text("+") }, label = { Text("Kayıt") })
                    NavigationBarItem(tab == 2, { tab = 2 }, { Text("▣") }, label = { Text("Hatalar") })
                    NavigationBarItem(tab == 3, { tab = 3 }, { Text("◎") }, label = { Text("Personel") })
                    NavigationBarItem(tab == 4, { tab = 4 }, { Text("⚙") }, label = { Text("Ayarlar") })
                }
            }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                when (tab) {
                    0 -> Dashboard2(records) { person = it; tab = 3 }
                    1 -> Entry2(records, refs) { Db2.save(context, records) }
                    2 -> Library2(refs) { Db2.saveRefs(context, refs) }
                    3 -> Person2(records, person, { person = it }) { records.remove(it); Db2.save(context, records) }
                    else -> Settings2()
                }
            }
        }
    }
}

private fun activeMonths2(): Int {
    val today = LocalDate.now()
    val capped = when { today.isBefore(start2) -> start2; today.isAfter(end2) -> end2; else -> today }
    return (ChronoUnit.MONTHS.between(YearMonth.from(start2), YearMonth.from(capped)).toInt() + 1).coerceIn(1, 12)
}
private fun inPeriod2(r: Rec2): Boolean {
    val d = Instant.ofEpochMilli(r.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    return !d.isBefore(start2) && !d.isAfter(end2)
}
private fun summary2(op: Op2, all: List<Rec2>): Sum2 {
    val rs = all.filter { it.operatorSicil == op.sicil && inPeriod2(it) }
    fun t(type: String) = rs.filter { it.type == type }.sumOf { it.amount }
    fun w(type: String) = rs.filter { it.type == type }.sumOf { it.amount * (defects2[it.defect] ?: 1.0) }
    val esc = t("Kaçan Hata"); val caught = t("Yakalanan Hata"); val ky = t("KY"); val kaizen = t("Kaizen"); val ot = t("Mesai"); val abs = t("Devamsızlık")
    val q = (35.0 + min(w("Yakalanan Hata") * .5, 5.0) - w("Kaçan Hata") * 4.0).coerceIn(0.0, 40.0)
    val kyp = min(15.0, ky / (activeMonths2() * 6.0) * 15.0)
    val kp = min(15.0, kaizen / activeMonths2() * 15.0)
    val att = max(0.0, 20.0 - abs * 5.0)
    val otp = min(10.0, ot / (activeMonths2() * 10.0) * 10.0)
    val total = q + kyp + kp + att + otp
    val grade = when { total >= 90 -> "A"; total >= 80 -> "B"; total >= 70 -> "C"; else -> "D" }
    return Sum2(rs.isNotEmpty(), esc, caught, ky, kaizen, ot, t("Yıllık İzin"), t("Günlük İzin"), t("Rapor"), abs, q, kyp, kp, att, otp, if (rs.isEmpty()) 0.0 else total, if (rs.isEmpty()) "—" else grade)
}

private fun saveBitmap2(context: Context, b: Bitmap, prefix: String): String {
    return try {
        val dir = File(context.filesDir, "operator_photos").apply { mkdirs() }
        val f = File(dir, "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
        f.outputStream().use { b.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        f.absolutePath
    } catch (_: Exception) { "" }
}
private fun copyUri2(context: Context, uri: android.net.Uri, prefix: String): String {
    return try {
        val dir = File(context.filesDir, "operator_photos").apply { mkdirs() }
        val f = File(dir, "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID()}.img")
        val input = context.contentResolver.openInputStream(uri) ?: return ""
        input.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
        f.absolutePath
    } catch (_: Exception) { "" }
}

@Composable
private fun Dashboard2(records: List<Rec2>, open: (Op2) -> Unit) {
    val sums = ops2.map { it to summary2(it, records) }
    val data = sums.filter { it.second.hasData }
    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Yönetici Dashboard", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric2("Ortalama", if (data.isEmpty()) "—" else f2(data.map { it.second.total }.average()), Modifier.weight(1f), Color(0xFFE0F2FE))
                Metric2("Fotoğraflı Kayıt", records.count { it.photoPath.isNotBlank() }.toString(), Modifier.weight(1f), Color(0xFFEDE9FE))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric2("Kaçan Hata", i2(sums.sumOf { it.second.escaped }), Modifier.weight(1f), Color(0xFFFEE2E2))
                Metric2("Yakalanan", i2(sums.sumOf { it.second.caught }), Modifier.weight(1f), Color(0xFFDCFCE7))
            }
        }
        item { Text("Personel", fontSize = 19.sp, fontWeight = FontWeight.Bold) }
        items(sums) { (op, s) ->
            Card(Modifier.fillMaxWidth().clickable { open(op) }) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(op.name, fontWeight = FontWeight.Bold); Text("Sicil ${op.sicil} • KY ${i2(s.ky)} • Kaizen ${i2(s.kaizen)}", fontSize = 12.sp) }
                    if (s.hasData) { Text(f2(s.total), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = scoreColor2(s.total)); Spacer(Modifier.width(8.dp)); Grade2(s.grade) } else Text("Veri yok")
                }
            }
        }
    }
}

@Composable
private fun Entry2(records: SnapshotStateList<Rec2>, refs: Map<String, String>, save: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scroll = rememberScrollState(); val scope = rememberCoroutineScope()
    var op by remember { mutableStateOf(ops2.first()) }; var type by remember { mutableStateOf(types2.first()) }; var defect by remember { mutableStateOf(defects2.keys.first()) }
    var amount by remember { mutableStateOf("1") }; var machine by remember { mutableStateOf(machines2.first()) }; var part by remember { mutableStateOf("") }; var note by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf("") }; var editing by remember { mutableStateOf<Long?>(null) }; var msg by remember { mutableStateOf("") }; var deleting by remember { mutableStateOf<Rec2?>(null) }
    val isError = type == "Kaçan Hata" || type == "Yakalanan Hata"
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { u -> if (u != null) copyUri2(context, u, "record").takeIf { it.isNotBlank() }?.let { photo = it } }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { b -> if (b != null) saveBitmap2(context, b, "record").takeIf { it.isNotBlank() }?.let { photo = it } }

    fun reset() { op = ops2.first(); type = types2.first(); defect = defects2.keys.first(); amount = "1"; machine = machines2.first(); part = ""; note = ""; photo = ""; editing = null }
    fun edit(r: Rec2) {
        op = ops2.firstOrNull { it.sicil == r.operatorSicil } ?: ops2.first(); type = r.type; defect = r.defect.ifBlank { defects2.keys.first() }
        amount = if (r.amount % 1.0 == 0.0) r.amount.toInt().toString() else r.amount.toString(); machine = r.machine.ifBlank { machines2.first() }; part = r.part; note = r.note; photo = r.photoPath; editing = r.id
        msg = "Düzenleme modu açık."; scope.launch { scroll.animateScrollTo(0) }
    }

    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (editing == null) "Günlük Kayıt" else "Kaydı Düzenle", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        if (editing != null) Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED))) { Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) { Text("✎ Kayıt düzenleniyor", Modifier.weight(1f), fontWeight = FontWeight.Bold); TextButton({ reset(); msg = "Düzenleme iptal edildi." }) { Text("İPTAL") } } }
        Select2("Operatör", ops2, op, { "${it.sicil} - ${it.name}" }) { op = it }
        Select2("Kayıt Türü", types2, type, { it }) { type = it }
        if (isError) {
            Select2("Hata Türü", defects2.keys.toList(), defect, { "$it ×${defects2[it]}" }) { defect = it }
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF))) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Hata Örneği • $defect", fontWeight = FontWeight.Bold, fontSize = 17.sp); Text(guides2[defect] ?: "", fontSize = 13.sp)
                    refs[defect]?.takeIf { it.isNotBlank() }?.let { Photo2(it, 170.dp) } ?: Text("Örnek fotoğraf yok. Hatalar sekmesinden ekleyin.", color = Color(0xFFB45309), fontSize = 12.sp)
                }
            }
        }
        OutlinedTextField(amount, { amount = it }, Modifier.fillMaxWidth(), label = { Text(if (type == "Mesai") "Saat" else if (type.contains("İzin") || type == "Rapor" || type == "Devamsızlık") "Gün" else "Miktar") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
        Select2("Makine", machines2, machine, { it }) { machine = it }
        OutlinedTextField(part, { part = it }, Modifier.fillMaxWidth(), label = { Text("Model / Parça") }, singleLine = true)
        OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), label = { Text("Açıklama / Detay") }, minLines = 3)
        Card { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Kayıt Fotoğrafı", fontWeight = FontWeight.Bold, fontSize = 17.sp); Text("Gerçek hatayı/olayı fotoğrafla kayıt altına alın.", fontSize = 12.sp)
            if (photo.isNotBlank()) Photo2(photo, 180.dp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ camera.launch(null) }, Modifier.weight(1f)) { Text("📷 Kamera") }; OutlinedButton({ gallery.launch("image/*") }, Modifier.weight(1f)) { Text("▣ Galeri") }
            }
            if (photo.isNotBlank()) TextButton({ photo = "" }) { Text("Fotoğrafı kaldır", color = MaterialTheme.colorScheme.error) }
        } }
        Button({
            val n = amount.replace(',', '.').toDoubleOrNull() ?: 0.0
            if (n <= 0) msg = "Miktar 0'dan büyük olmalı."
            else {
                val idx = editing?.let { id -> records.indexOfFirst { it.id == id } } ?: -1
                if (idx >= 0) { val old = records[idx]; records[idx] = old.copy(operatorSicil = op.sicil, type = type, defect = if (isError) defect else "", amount = n, machine = machine, part = part.trim(), note = note.trim(), photoPath = photo); save(); reset(); msg = "✓ Kayıt güncellendi" }
                else { records.add(Rec2(System.currentTimeMillis(), System.currentTimeMillis(), op.sicil, type, if (isError) defect else "", n, machine, part.trim(), note.trim(), photo)); save(); msg = "✓ Kayıt kaydedildi"; amount = "1"; part = ""; note = ""; photo = "" }
            }
        }, Modifier.fillMaxWidth().height(52.dp)) { Text(if (editing == null) "KAYDI KAYDET" else "KAYDI GÜNCELLE", fontWeight = FontWeight.Bold) }
        if (msg.isNotBlank()) Text(msg, color = if (msg.startsWith("✓")) Color(0xFF15803D) else Color(0xFF4B5563), fontWeight = FontWeight.SemiBold)
        HorizontalDivider(); Text("Son Kayıtlar", fontSize = 21.sp, fontWeight = FontWeight.Bold); Text("Düzenle veya sil. Son 10 kayıt tüm detaylarıyla gösterilir.", fontSize = 12.sp)
        records.takeLast(10).reversed().forEach { r -> RecordCard2(r, { edit(r) }, { deleting = r }) }
        Spacer(Modifier.height(24.dp))
    }
    deleting?.let { r -> AlertDialog({ deleting = null }, { Text("Kaydı sil") }, { Text("Bu kayıt silinsin mi?") }, confirmButton = { TextButton({ records.remove(r); save(); deleting = null; if (editing == r.id) reset(); msg = "Kayıt silindi." }) { Text("SİL", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton({ deleting = null }) { Text("VAZGEÇ") } }) }
}

@Composable
private fun RecordCard2(r: Rec2, edit: () -> Unit, delete: () -> Unit) {
    val op = ops2.firstOrNull { it.sicil == r.operatorSicil }
    val bg = when (r.type) { "Kaçan Hata" -> Color(0xFFFFF1F2); "Yakalanan Hata" -> Color(0xFFF0FDF4); "Kaizen" -> Color(0xFFFFFBEB); "KY" -> Color(0xFFEFF6FF); else -> Color(0xFFF8FAFC) }
    Card(colors = CardDefaults.cardColors(containerColor = bg)) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row { Column(Modifier.weight(1f)) { Text("${op?.name ?: r.operatorSicil} • ${r.type}", fontWeight = FontWeight.Bold, fontSize = 17.sp); Text("Sicil ${r.operatorSicil} • ${date2(r.timestamp)}", fontSize = 11.sp) }; if (r.photoPath.isNotBlank()) Text("📷", fontSize = 20.sp) }
        Text("${i2(r.amount)} ${unit2(r.type)}${if (r.defect.isNotBlank()) " • Hata: ${r.defect} ×${defects2[r.defect] ?: 1.0}" else ""}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        if (r.machine.isNotBlank()) Text("Makine: ${r.machine}", fontSize = 13.sp); if (r.part.isNotBlank()) Text("Model / Parça: ${r.part}", fontSize = 13.sp); if (r.note.isNotBlank()) Text("Açıklama: ${r.note}", fontSize = 13.sp)
        if (r.photoPath.isNotBlank()) Photo2(r.photoPath, 150.dp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(edit) { Text("✎ DÜZENLE") }; TextButton(delete) { Text("SİL", color = MaterialTheme.colorScheme.error) } }
    } }
}

@Composable
private fun Library2(refs: MutableMap<String, String>, changed: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current; var target by remember { mutableStateOf<String?>(null) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { u -> val d = target; if (u != null && d != null) copyUri2(context, u, "reference").takeIf { it.isNotBlank() }?.let { refs[d] = it; changed() }; target = null }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { b -> val d = target; if (b != null && d != null) saveBitmap2(context, b, "reference").takeIf { it.isNotBlank() }?.let { refs[d] = it; changed() }; target = null }
    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Hata Fotoğraf Kütüphanesi", fontSize = 24.sp, fontWeight = FontWeight.Bold); Text("Her hata için örnek fotoğraf yükleyin. Hata seçildiğinde operatöre otomatik gösterilir.", fontSize = 13.sp) }
        items(defects2.keys.toList()) { d ->
            Card { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row { Column(Modifier.weight(1f)) { Text(d, fontSize = 19.sp, fontWeight = FontWeight.Bold); Text("Katsayı ×${defects2[d]}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary) }; if (refs[d].orEmpty().isNotBlank()) Text("✓ Fotoğraf var", color = Color(0xFF15803D), fontSize = 12.sp) }
                Text(guides2[d] ?: "", fontSize = 13.sp); refs[d]?.takeIf { it.isNotBlank() }?.let { Photo2(it, 190.dp) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton({ target = d; camera.launch(null) }, Modifier.weight(1f)) { Text("📷 Çek") }; OutlinedButton({ target = d; gallery.launch("image/*") }, Modifier.weight(1f)) { Text("▣ Galeriden") } }
                if (refs[d].orEmpty().isNotBlank()) TextButton({ refs.remove(d); changed() }) { Text("Fotoğrafı kaldır", color = MaterialTheme.colorScheme.error) }
            } }
        }
    }
}

@Composable
private fun Person2(records: List<Rec2>, selected: Op2, select: (Op2) -> Unit, delete: (Rec2) -> Unit) {
    val s = summary2(selected, records); val own = records.filter { it.operatorSicil == selected.sicil && inPeriod2(it) }.sortedByDescending { it.timestamp }
    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Select2("Operatör", ops2, selected, { "${it.sicil} - ${it.name}" }) { select(it) } }
        item { Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(selected.name, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text("Sicil: ${selected.sicil}") }; if (s.hasData) { Text("${f2(s.total)} / 100", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = scoreColor2(s.total)); Spacer(Modifier.width(8.dp)); Grade2(s.grade) } else Text("Veri yok") } } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Metric2("KY", i2(s.ky), Modifier.weight(1f), Color(0xFFDCFCE7)); Metric2("Kaizen", i2(s.kaizen), Modifier.weight(1f), Color(0xFFFEF3C7)); Metric2("Mesai", "${i2(s.overtime)} s", Modifier.weight(1f), Color(0xFFE0E7FF)) } }
        item { Card { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { Text("Puan Bileşenleri", fontWeight = FontWeight.Bold, fontSize = 18.sp); Score2("Kalite", s.quality, 40.0); Score2("KY", s.kyScore, 15.0); Score2("Kaizen", s.kaizenScore, 15.0); Score2("Devam", s.attendance, 20.0); Score2("Mesai", s.overtimeScore, 10.0) } } }
        item { Card { Column(Modifier.padding(14.dp)) { Text("Performans Özeti", fontWeight = FontWeight.Bold, fontSize = 18.sp); if (!s.hasData) Text("Kayıt yok.") else Text("KY ${i2(s.ky)}, Kaizen ${i2(s.kaizen)}, kaçan ${i2(s.escaped)}, yakalanan ${i2(s.caught)}, mesai ${i2(s.overtime)} saat, yıllık izin ${i2(s.annualLeave)} gün, günlük izin ${i2(s.dailyLeave)} gün, rapor ${i2(s.report)} gün, devamsızlık ${i2(s.absence)} gün. Puan ${f2(s.total)}/100, sınıf ${s.grade}.") } } }
        item { Text("Detaylı Kayıt Geçmişi", fontSize = 19.sp, fontWeight = FontWeight.Bold) }
        items(own, key = { it.id }) { r -> Card { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text("${r.type}${if (r.defect.isNotBlank()) " • ${r.defect}" else ""}", fontWeight = FontWeight.Bold); Text("${i2(r.amount)} ${unit2(r.type)} • ${date2(r.timestamp)}", fontSize = 12.sp); if (r.machine.isNotBlank()) Text("Makine: ${r.machine}", fontSize = 12.sp); if (r.part.isNotBlank()) Text("Parça: ${r.part}", fontSize = 12.sp); if (r.note.isNotBlank()) Text("Açıklama: ${r.note}", fontSize = 12.sp); if (r.photoPath.isNotBlank()) Photo2(r.photoPath, 130.dp); TextButton({ delete(r) }) { Text("Kaydı sil", color = MaterialTheme.colorScheme.error) } } } }
    }
}

@Composable
private fun Settings2() { LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { item { Text("Ayarlar & Puanlama", fontSize = 24.sp, fontWeight = FontWeight.Bold) }; item { Card { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { Text("Sürüm 1.1", fontWeight = FontWeight.Bold, fontSize = 18.sp); Text("• Hata kütüphanesine örnek fotoğraf"); Text("• Kayıta kamera/galeri fotoğrafı"); Text("• Son kayıtlardan düzenleme ve silme"); Text("• Fotoğrafı dokunarak büyütme"); Text("• Eski kayıtlar korunur") } } }; item { Card { Column(Modifier.padding(14.dp)) { Text("100 Puan Modeli", fontWeight = FontWeight.Bold); Text("Kalite 40 • KY 15 • Kaizen 15 • Devam 20 • Mesai 10") } } } } }

@Composable
private fun Photo2(path: String, h: androidx.compose.ui.unit.Dp) {
    var big by remember { mutableStateOf(false) }; val b = remember(path) { BitmapFactory.decodeFile(path) }
    if (b != null) {
        Image(b.asImageBitmap(), "Fotoğraf", Modifier.fillMaxWidth().height(h).clickable { big = true }, contentScale = ContentScale.Crop)
        if (big) Dialog({ big = false }) { Card(colors = CardDefaults.cardColors(containerColor = Color.Black)) { Column(Modifier.padding(8.dp)) { Image(b.asImageBitmap(), "Büyük fotoğraf", Modifier.fillMaxWidth().heightIn(min = 280.dp, max = 620.dp), contentScale = ContentScale.Fit); TextButton({ big = false }, Modifier.align(Alignment.End)) { Text("KAPAT", color = Color.White) } } } }
    } else Box(Modifier.fillMaxWidth().height(h).background(Color(0xFFE5E7EB)), contentAlignment = Alignment.Center) { Text("Fotoğraf açılamadı") }
}

@Composable
private fun <T> Select2(label: String, options: List<T>, selected: T, text: (T) -> String, set: (T) -> Unit) {
    var exp by remember { mutableStateOf(false) }; Column { Text(label, fontSize = 12.sp); OutlinedButton({ exp = true }, Modifier.fillMaxWidth()) { Text(text(selected), Modifier.weight(1f)); Text("▼") }; DropdownMenu(exp, { exp = false }) { options.forEach { o -> DropdownMenuItem({ Text(text(o)) }, { set(o); exp = false }) } } }
}
@Composable private fun Metric2(t: String, v: String, m: Modifier, c: Color) { Card(m, colors = CardDefaults.cardColors(containerColor = c)) { Column(Modifier.padding(12.dp)) { Text(t, fontSize = 11.sp); Text(v, fontSize = 19.sp, fontWeight = FontWeight.Bold) } } }
@Composable private fun Grade2(g: String) { val c = when (g) { "A" -> Color(0xFF15803D); "B" -> Color(0xFF0369A1); "C" -> Color(0xFFB45309); else -> Color(0xFFB91C1C) }; Surface(color = c, shape = RoundedCornerShape(9.dp)) { Text(g, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) } }
@Composable private fun Score2(t: String, v: Double, mx: Double) { Column { Row(Modifier.fillMaxWidth()) { Text(t, Modifier.weight(1f)); Text("${f2(v)} / ${i2(mx)}", fontWeight = FontWeight.Bold) }; LinearProgressIndicator(progress = { (v / mx).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth()) } }
private fun f2(v: Double) = String.format(Locale("tr", "TR"), "%.1f", v)
private fun i2(v: Double) = if (v % 1.0 == 0.0) v.toInt().toString() else f2(v)
private fun unit2(t: String) = when (t) { "Mesai" -> "saat"; "Yıllık İzin", "Günlük İzin", "Rapor", "Devamsızlık" -> "gün"; else -> "adet" }
private fun date2(ts: Long) = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR")).format(Date(ts))
private fun scoreColor2(v: Double) = when { v >= 90 -> Color(0xFF15803D); v >= 80 -> Color(0xFF0369A1); v >= 70 -> Color(0xFFB45309); else -> Color(0xFFB91C1C) }
