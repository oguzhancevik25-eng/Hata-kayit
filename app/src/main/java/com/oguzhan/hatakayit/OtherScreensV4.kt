package com.oguzhan.hatakayit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import java.util.Locale

@Composable
fun Dashboard4(records: List<Record4>, operators: List<Operator4>, openPerson: (Operator4) -> Unit) {
    val summaries = operators.map { it to calculate4(it, records) }
    val withData = summaries.filter { it.second.hasData }
    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Yönetici Dashboard", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("01.08.2026 – 31.07.2027 • ${parts4.size} kayıtlı parça", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric4("Ortalama", if (withData.isEmpty()) "—" else f14(withData.map { it.second.total }.average()), Modifier.weight(1f), Color(0xFFE0F2FE))
                Metric4("Fotoğraflı Kayıt", records.count { it.photoPath.isNotBlank() }.toString(), Modifier.weight(1f), Color(0xFFEDE9FE))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric4("Kaçan Hata", i4(summaries.sumOf { it.second.escaped }), Modifier.weight(1f), Color(0xFFFEE2E2))
                Metric4("Yakalanan", i4(summaries.sumOf { it.second.caught }), Modifier.weight(1f), Color(0xFFDCFCE7))
            }
        }
        item { Text("Personel Performansı", fontSize = 19.sp, fontWeight = FontWeight.Bold) }
        items(summaries) { (op, score) ->
            Card(modifier = Modifier.fillMaxWidth().clickable { openPerson(op) }) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(op.name, fontWeight = FontWeight.Bold)
                        Text("Sicil ${op.sicil} • KY ${i4(score.ky)} • Kaizen ${i4(score.kaizen)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (score.hasData) {
                        Text(f14(score.total), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = scoreColor4(score.total))
                        Spacer(Modifier.width(8.dp)); GradeBadge4(score.grade)
                    } else Text("Veri yok", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun DefectLibrary4(refs: MutableMap<String, String>, saveRefs: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var targetDefect by remember { mutableStateOf<String?>(null) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val target = targetDefect
        if (uri != null && target != null) saveGalleryPhoto4(context, uri, "reference").takeIf { it.isNotBlank() }?.let { refs[target] = it; saveRefs() }
        targetDefect = null
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        val target = targetDefect
        if (bitmap != null && target != null) saveCameraPhoto4(context, bitmap, "reference").takeIf { it.isNotBlank() }?.let { refs[target] = it; saveRefs() }
        targetDefect = null
    }

    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Hata Fotoğraf Kütüphanesi", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Operatör hata seçtiğinde bu örnek fotoğrafı görür.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(defectWeights4.keys.toList()) { defect ->
            val photo = refs[defect].orEmpty()
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(defect, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text("Katsayı ×${defectWeights4[defect]} • ${defectHelp4[defect]}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (photo.isNotBlank()) LocalPhoto4(photo, 180.dp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton({ targetDefect = defect; camera.launch(null) }, Modifier.weight(1f)) { Text("📷 Çek") }
                        OutlinedButton({ targetDefect = defect; gallery.launch("image/*") }, Modifier.weight(1f)) { Text("▣ Galeri") }
                    }
                    if (photo.isNotBlank()) TextButton({ refs.remove(defect); saveRefs() }) { Text("Fotoğrafı kaldır", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
fun Person4(records: List<Record4>, operators: List<Operator4>, selectedSicil: String, onSelect: (Operator4) -> Unit, onDelete: (Record4) -> Unit) {
    if (operators.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) { Text("Aktif personel yok") }
        return
    }
    val selected = operators.firstOrNull { it.sicil == selectedSicil } ?: operators.first()
    val score = calculate4(selected, records)
    val history = records.filter { it.operatorSicil == selected.sicil && inPeriod4(it) }.sortedByDescending { it.timestamp }
    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Selector4("Operatör", operators, selected, { "${it.sicil} - ${it.name}" }, onSelect) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(selected.name, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text("Sicil ${selected.sicil}") }
                    if (score.hasData) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${f14(score.total)} / 100", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = scoreColor4(score.total)); GradeBadge4(score.grade)
                        }
                    } else Text("Veri yok")
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric4("KY", i4(score.ky), Modifier.weight(1f), Color(0xFFDCFCE7))
                Metric4("Kaizen", i4(score.kaizen), Modifier.weight(1f), Color(0xFFFEF3C7))
                Metric4("Mesai", "${i4(score.overtime)} s", Modifier.weight(1f), Color(0xFFE0E7FF))
            }
        }
        item {
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Puan Bileşenleri", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    ScoreBar4("Kalite", score.quality, 40.0); ScoreBar4("KY", score.kyScore, 15.0)
                    ScoreBar4("Kaizen", score.kaizenScore, 15.0); ScoreBar4("Devam", score.attendance, 20.0); ScoreBar4("Mesai", score.overtimeScore, 10.0)
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(14.dp)) {
                    Text("Performans Özeti", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (!score.hasData) Text("Bu dönem için kayıt yok")
                    else Text("KY ${i4(score.ky)}, Kaizen ${i4(score.kaizen)}, kaçan hata ${i4(score.escaped)}, yakalanan hata ${i4(score.caught)}, mesai ${i4(score.overtime)} saat, yıllık izin ${i4(score.annualLeave)} gün, günlük izin ${i4(score.dailyLeave)} gün, rapor ${i4(score.report)} gün, devamsızlık ${i4(score.absence)} gün. Toplam ${f14(score.total)}/100, sınıf ${score.grade}.")
                }
            }
        }
        item { Text("Detaylı Kayıt Geçmişi", fontSize = 19.sp, fontWeight = FontWeight.Bold) }
        if (history.isEmpty()) item { Text("Kayıt yok", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(history, key = { it.id }) { r ->
            Card {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${r.type}${if (r.defect.isNotBlank()) " • ${r.defect}" else ""}", fontWeight = FontWeight.Bold)
                    Text("${i4(r.amount)} ${unit4(r.type)} • ${date4(r.timestamp)}", fontSize = 12.sp)
                    if (r.part.isNotBlank()) Text("Parça: ${r.part}", fontSize = 12.sp)
                    if (r.machine.isNotBlank()) Text("Makine: ${r.machine}", fontSize = 12.sp)
                    if (r.note.isNotBlank()) Text("Açıklama: ${r.note}", fontSize = 12.sp)
                    if (r.photoPath.isNotBlank()) LocalPhoto4(r.photoPath, 125.dp)
                    TextButton({ onDelete(r) }) { Text("Kaydı sil", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
fun TeamManager4(operators: SnapshotStateList<Operator4>, records: List<Record4>, onSave: () -> Unit) {
    var sicil by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val active = operators.filter { it.active }
    val inactive = operators.filter { !it.active }

    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Ekip Yönetimi", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Operatör ekle, aktif listeden çıkar veya geri al. Eski kayıtlar korunur.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Yeni Operatör", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(sicil, { sicil = it.filter { ch -> ch.isDigit() } }, Modifier.fillMaxWidth(), label = { Text("Sicil") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Ad Soyad") }, singleLine = true)
                    Button(onClick = {
                        val s = sicil.trim(); val n = name.trim().uppercase(Locale("tr", "TR"))
                        when {
                            s.isBlank() || n.isBlank() -> message = "Sicil ve ad soyad gir"
                            operators.any { it.sicil == s } -> message = "Bu sicil zaten kayıtlı"
                            else -> { operators.add(Operator4(s, n, true)); onSave(); sicil = ""; name = ""; message = "✓ Operatör eklendi" }
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("OPERATÖR EKLE") }
                    if (message.isNotBlank()) Text(message, color = if (message.startsWith("✓")) Color(0xFF15803D) else Color(0xFFB45309))
                }
            }
        }
        item { Text("Aktif Personel (${active.size})", fontSize = 19.sp, fontWeight = FontWeight.Bold) }
        items(active, key = { it.sicil }) { op ->
            val count = records.count { it.operatorSicil == op.sicil }
            Card {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(op.name, fontWeight = FontWeight.Bold); Text("Sicil ${op.sicil} • $count kayıt", fontSize = 12.sp) }
                    OutlinedButton({
                        val i = operators.indexOfFirst { it.sicil == op.sicil }
                        if (i >= 0) { operators[i] = op.copy(active = false); onSave(); message = "${op.name} pasife alındı" }
                    }) { Text("ÇIKAR") }
                }
            }
        }
        if (inactive.isNotEmpty()) {
            item { Text("Pasif Personel (${inactive.size})", fontSize = 19.sp, fontWeight = FontWeight.Bold) }
            items(inactive, key = { "inactive_${it.sicil}" }) { op ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(op.name, fontWeight = FontWeight.Bold); Text("Sicil ${op.sicil} • kayıtları korunuyor", fontSize = 12.sp) }
                        OutlinedButton({
                            val i = operators.indexOfFirst { it.sicil == op.sicil }
                            if (i >= 0) { operators[i] = op.copy(active = true); onSave(); message = "✓ ${op.name} geri alındı" }
                        }) { Text("GERİ AL") }
                    }
                }
            }
        }
    }
}
