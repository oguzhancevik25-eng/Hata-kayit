package com.oguzhan.hatakayit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Entry4(records: SnapshotStateList<Record4>, references: Map<String, String>, operatorsAll: List<Operator4>, save: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activeOperators = operatorsAll.filter { it.active }
    if (activeOperators.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            Card { Text("Önce Ekip sekmesinden en az bir aktif operatör ekleyin.", Modifier.padding(18.dp)) }
        }
        return
    }

    var operator by remember { mutableStateOf(activeOperators.first()) }
    LaunchedEffect(activeOperators.map { it.sicil }) {
        if (activeOperators.none { it.sicil == operator.sicil }) operator = activeOperators.first()
    }
    var type by remember { mutableStateOf(recordTypes4.first()) }
    var defect by remember { mutableStateOf(defectWeights4.keys.first()) }
    var amount by remember { mutableStateOf("1") }
    var machine by remember { mutableStateOf(machines4.first()) }
    var part by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var photoPath by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var message by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<Record4?>(null) }
    val isError = type == "Kaçan Hata" || type == "Yakalanan Hata"

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) saveGalleryPhoto4(context, uri, "record").takeIf { it.isNotBlank() }?.let { photoPath = it }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) saveCameraPhoto4(context, bitmap, "record").takeIf { it.isNotBlank() }?.let { photoPath = it }
    }

    fun resetForm() {
        operator = activeOperators.first(); type = recordTypes4.first(); defect = defectWeights4.keys.first()
        amount = "1"; machine = machines4.first(); part = ""; note = ""; photoPath = ""; editingId = null
    }
    fun editRecord(record: Record4) {
        operator = activeOperators.firstOrNull { it.sicil == record.operatorSicil } ?: activeOperators.first()
        type = record.type; defect = record.defect.ifBlank { defectWeights4.keys.first() }
        amount = if (record.amount % 1.0 == 0.0) record.amount.toInt().toString() else record.amount.toString()
        machine = record.machine.ifBlank { machines4.first() }; part = record.part; note = record.note
        photoPath = record.photoPath; editingId = record.id; message = "✎ Düzenleme modu açık"
    }

    val partChoices = buildList {
        add("— Parça seç —")
        if (part.isNotBlank() && !partMachine4.containsKey(part)) add(part)
        addAll(parts4.map { it.name })
    }
    val selectedPart = if (part.isBlank()) "— Parça seç —" else part

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(if (editingId == null) "Günlük Kayıt" else "Kaydı Düzenle", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        if (editingId != null) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED))) {
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Mevcut kayıt düzenleniyor", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color(0xFF9A3412))
                    TextButton(onClick = { resetForm(); message = "Düzenleme iptal edildi" }) { Text("İPTAL") }
                }
            }
        }

        Selector4("Operatör", activeOperators, operator, { "${it.sicil} - ${it.name}" }) { operator = it }
        Selector4("Kayıt Türü", recordTypes4, type, { it }) { type = it }

        if (isError) {
            Selector4("Hata Türü", defectWeights4.keys.toList(), defect, { "$it  ×${defectWeights4[it]}" }) { defect = it }
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF))) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Hata Örneği • $defect", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(defectHelp4[defect] ?: "", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val reference = references[defect].orEmpty()
                    if (reference.isNotBlank()) LocalPhoto4(reference, 160.dp)
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

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Parça / Kalıp ve Makine", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("Parçayı seçtiğinde makine otomatik gelir.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Selector4("Parça / Kalıp", partChoices, selectedPart, { it }) { choice ->
                    if (choice == "— Parça seç —") part = ""
                    else {
                        part = choice
                        partMachine4[choice]?.let { machine = it }
                    }
                }
                if (part.isNotBlank() && partMachine4.containsKey(part)) {
                    Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(10.dp)) {
                        Text("✓ Otomatik makine: ${partMachine4[part]}", Modifier.fillMaxWidth().padding(10.dp), color = Color(0xFF166534), fontWeight = FontWeight.Bold)
                    }
                }
                Selector4("Makine", machines4, machine, { it }) { machine = it }
                Text("Gerekirse makineyi elle değiştirebilirsin.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), label = { Text("Açıklama / Detay") }, minLines = 3)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Kayıt Fotoğrafı", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                if (photoPath.isNotBlank()) LocalPhoto4(photoPath, 180.dp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ camera.launch(null) }, Modifier.weight(1f)) { Text("📷 Kamera") }
                    OutlinedButton({ gallery.launch("image/*") }, Modifier.weight(1f)) { Text("▣ Galeri") }
                }
                if (photoPath.isNotBlank()) TextButton({ photoPath = "" }) { Text("Fotoğrafı kaldır", color = MaterialTheme.colorScheme.error) }
            }
        }

        Button(
            onClick = {
                val n = amount.replace(',', '.').toDoubleOrNull() ?: 0.0
                if (n <= 0) message = "Miktar 0'dan büyük olmalı"
                else {
                    val index = editingId?.let { id -> records.indexOfFirst { it.id == id } } ?: -1
                    if (index >= 0) {
                        val old = records[index]
                        records[index] = old.copy(
                            operatorSicil = operator.sicil, type = type, defect = if (isError) defect else "",
                            amount = n, machine = machine, part = part, note = note.trim(), photoPath = photoPath
                        )
                        save(); resetForm(); message = "✓ Kayıt güncellendi"
                    } else {
                        val now = System.currentTimeMillis()
                        records.add(Record4(now, now, operator.sicil, type, if (isError) defect else "", n, machine, part, note.trim(), photoPath))
                        save(); amount = "1"; part = ""; machine = machines4.first(); note = ""; photoPath = ""; message = "✓ Kayıt kaydedildi"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text(if (editingId == null) "KAYDI KAYDET" else "KAYDI GÜNCELLE", fontWeight = FontWeight.Bold) }

        if (message.isNotBlank()) Text(message, color = if (message.startsWith("✓")) Color(0xFF15803D) else Color(0xFF4B5563), fontWeight = FontWeight.SemiBold)
        HorizontalDivider(); Text("Son Kayıtlar", fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Text("Son 10 kayıt • düzenle / sil", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        records.takeLast(10).reversed().forEach { r -> RecordCard4(r, operatorsAll, { editRecord(r) }, { deleteTarget = r }) }
        Spacer(Modifier.height(24.dp))
    }

    deleteTarget?.let { r ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Kaydı sil") }, text = { Text("Bu kayıt silinsin mi?") },
            confirmButton = { TextButton({ records.remove(r); save(); if (editingId == r.id) resetForm(); deleteTarget = null; message = "Kayıt silindi" }) { Text("SİL", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton({ deleteTarget = null }) { Text("VAZGEÇ") } }
        )
    }
}

@Composable
fun RecordCard4(record: Record4, operators: List<Operator4>, onEdit: () -> Unit, onDelete: () -> Unit) {
    val op = operators.firstOrNull { it.sicil == record.operatorSicil }
    val bg = when (record.type) {
        "Kaçan Hata" -> Color(0xFFFFF1F2); "Yakalanan Hata" -> Color(0xFFF0FDF4)
        "KY" -> Color(0xFFEFF6FF); "Kaizen" -> Color(0xFFFFFBEB); else -> Color(0xFFF8FAFC)
    }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = bg)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${op?.name ?: record.operatorSicil} • ${record.type}", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Sicil ${record.operatorSicil} • ${date4(record.timestamp)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (record.photoPath.isNotBlank()) Text("📷", fontSize = 20.sp)
            }
            Text("${i4(record.amount)} ${unit4(record.type)}${if (record.defect.isNotBlank()) " • ${record.defect} ×${defectWeights4[record.defect] ?: 1.0}" else ""}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (record.part.isNotBlank()) Text("Parça: ${record.part}", fontSize = 13.sp)
            if (record.machine.isNotBlank()) Text("Makine: ${record.machine}", fontSize = 13.sp)
            if (record.note.isNotBlank()) Text("Açıklama: ${record.note}", fontSize = 13.sp)
            if (record.photoPath.isNotBlank()) LocalPhoto4(record.photoPath, 145.dp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onEdit) { Text("✎ DÜZENLE") }
                TextButton(onDelete) { Text("SİL", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
