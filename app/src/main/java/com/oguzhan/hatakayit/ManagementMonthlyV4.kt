package com.oguzhan.hatakayit

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

@Composable
fun ManagementHub4(
    operators: SnapshotStateList<Operator4>,
    records: List<Record4>,
    machines: SnapshotStateList<String>,
    parts: SnapshotStateList<Part4>,
    saveOperators: () -> Unit,
    saveMachines: () -> Unit,
    saveParts: () -> Unit
) {
    var section by remember { mutableStateOf("Ekip") }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (section == "Ekip") Button({ section = "Ekip" }) { Text("Ekip") }
            else OutlinedButton({ section = "Ekip" }) { Text("Ekip") }
            if (section == "Makine") Button({ section = "Makine" }) { Text("Makineler") }
            else OutlinedButton({ section = "Makine" }) { Text("Makineler") }
            if (section == "Parça") Button({ section = "Parça" }) { Text("Parça / Kalıp") }
            else OutlinedButton({ section = "Parça" }) { Text("Parça / Kalıp") }
        }
        HorizontalDivider()
        Box(Modifier.weight(1f)) {
            when (section) {
                "Ekip" -> TeamManager4(operators, records, saveOperators)
                "Makine" -> MachineManager4(machines, parts, saveMachines)
                else -> PartManager4(parts, machines, saveParts)
            }
        }
    }
}

@Composable
private fun MachineManager4(
    machines: SnapshotStateList<String>,
    parts: List<Part4>,
    save: () -> Unit
) {
    var newMachine by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Makine Yönetimi", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Makine ekleyebilir veya listeden çıkarabilirsin. Eski kayıtlar silinmez.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Yeni Makine", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(newMachine, { newMachine = it }, Modifier.fillMaxWidth(), label = { Text("Makine adı") }, singleLine = true)
                    Button({
                        val name = newMachine.trim().uppercase(Locale("tr", "TR"))
                        when {
                            name.isBlank() -> message = "Makine adı gir"
                            machines.any { it.equals(name, true) } -> message = "Bu makine zaten kayıtlı"
                            else -> {
                                machines.add(name); save(); newMachine = ""; message = "✓ Makine eklendi"
                            }
                        }
                    }, Modifier.fillMaxWidth()) { Text("MAKİNE EKLE") }
                    if (message.isNotBlank()) Text(message, color = if (message.startsWith("✓")) Color(0xFF15803D) else Color(0xFFB45309))
                }
            }
        }
        item { Text("Kayıtlı Makineler (${machines.size})", fontSize = 19.sp, fontWeight = FontWeight.Bold) }
        items(machines, key = { it }) { machine ->
            val linked = parts.count { it.machine == machine }
            Card {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(machine, fontWeight = FontWeight.Bold)
                        Text("$linked parça bağlı", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton({
                        when {
                            machines.size <= 1 -> message = "En az 1 makine kalmalı"
                            linked > 0 -> message = "$machine makinesine $linked parça bağlı. Önce bu parçaları sil veya başka makineyle yeniden ekle."
                            else -> { machines.remove(machine); save(); message = "✓ $machine çıkarıldı" }
                        }
                    }) { Text("ÇIKAR") }
                }
            }
        }
    }
}

@Composable
private fun PartManager4(
    parts: SnapshotStateList<Part4>,
    machines: List<String>,
    save: () -> Unit
) {
    val availableMachines = machines.ifEmpty { defaultMachines4 }
    var partName by remember { mutableStateOf("") }
    var selectedMachine by remember { mutableStateOf(availableMachines.first()) }
    var message by remember { mutableStateOf("") }
    LaunchedEffect(availableMachines) {
        if (selectedMachine !in availableMachines) selectedMachine = availableMachines.first()
    }

    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Parça / Kalıp Yönetimi", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Parçayı hangi makinede ürettiğini tanımla. Kayıt ekranında makine otomatik seçilir.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Yeni Parça / Kalıp", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(partName, { partName = it }, Modifier.fillMaxWidth(), label = { Text("Parça / Kalıp adı") }, singleLine = true)
                    Selector4("Makine", availableMachines, selectedMachine, { it }) { selectedMachine = it }
                    Button({
                        val name = partName.trim()
                        when {
                            name.isBlank() -> message = "Parça adı gir"
                            parts.any { it.name.equals(name, true) } -> message = "Bu parça zaten kayıtlı"
                            else -> {
                                parts.add(Part4(name, selectedMachine)); save(); partName = ""; message = "✓ Parça eklendi"
                            }
                        }
                    }, Modifier.fillMaxWidth()) { Text("PARÇA EKLE") }
                    if (message.isNotBlank()) Text(message, color = if (message.startsWith("✓")) Color(0xFF15803D) else Color(0xFFB45309))
                }
            }
        }
        item { Text("Kayıtlı Parçalar (${parts.size})", fontSize = 19.sp, fontWeight = FontWeight.Bold) }
        items(parts, key = { it.name }) { part ->
            Card {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(part.name, fontWeight = FontWeight.Bold)
                        Text("Makine: ${part.machine}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton({ parts.remove(part); save(); message = "✓ ${part.name} çıkarıldı" }) { Text("ÇIKAR") }
                }
            }
        }
    }
}

private fun monthsInPeriod4(): List<YearMonth> {
    val start = YearMonth.from(periodStart4)
    val end = YearMonth.from(periodEnd4)
    val result = mutableListOf<YearMonth>()
    var m = start
    while (!m.isAfter(end)) { result.add(m); m = m.plusMonths(1) }
    return result
}

private fun monthLabel4(month: YearMonth): String = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("tr", "TR"))).replaceFirstChar { it.uppercase(Locale("tr", "TR")) }

@Composable
fun MonthlyAnalytics4(records: List<Record4>, operators: List<Operator4>) {
    if (operators.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Aktif personel yok") }
        return
    }
    val months = remember { monthsInPeriod4() }
    val now = YearMonth.now()
    var selectedMonth by remember { mutableStateOf(if (now in months) now else months.first()) }
    var trendOperator by remember { mutableStateOf(operators.first()) }
    LaunchedEffect(operators.map { it.sicil }) {
        if (operators.none { it.sicil == trendOperator.sicil }) trendOperator = operators.first()
    }

    val monthScores = operators.map { it to calculateMonth4(it, records, selectedMonth) }
    val monthRecords = records.filter { monthOf4(it) == selectedMonth && operators.any { op -> op.sicil == it.operatorSicil } }

    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Aylık Operatör Analizi", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Her ay operatörlerin KY, Kaizen, hata, mesai, izin ve performansını gör.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { Selector4("Ay", months, selectedMonth, { monthLabel4(it) }) { selectedMonth = it } }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric4("Kayıt", monthRecords.size.toString(), Modifier.weight(1f), Color(0xFFE0F2FE))
                Metric4("Kaçan", i4(monthScores.sumOf { it.second.escaped }), Modifier.weight(1f), Color(0xFFFEE2E2))
                Metric4("Yakalanan", i4(monthScores.sumOf { it.second.caught }), Modifier.weight(1f), Color(0xFFDCFCE7))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("12 Aylık Puan Grafiği", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Selector4("Operatör", operators, trendOperator, { "${it.sicil} - ${it.name}" }) { trendOperator = it }
                    YearScoreChart4(trendOperator, records, months)
                }
            }
        }
        item { Text("${monthLabel4(selectedMonth)} • Operatörler", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        items(monthScores, key = { it.first.sicil }) { (op, score) ->
            val rows = recordsForMonth4(records, op.sicil, selectedMonth)
            Card(colors = CardDefaults.cardColors(containerColor = if (score.hasData) Color.White else Color(0xFFF8FAFC))) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(op.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Sicil ${op.sicil} • ${rows.size} kayıt", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (score.hasData) {
                            Text("${f14(score.total)}/100", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = scoreColor4(score.total))
                            Spacer(Modifier.width(6.dp)); GradeBadge4(score.grade)
                        } else Text("Veri yok", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (score.hasData) {
                        ActivityBars4(score)
                        Text(
                            "KY ${i4(score.ky)} • Kaizen ${i4(score.kaizen)} • Yakalanan ${i4(score.caught)} • Kaçan ${i4(score.escaped)} • Mesai ${i4(score.overtime)} saat",
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Yıllık izin ${i4(score.annualLeave)} gün • Günlük izin ${i4(score.dailyLeave)} gün • Rapor ${i4(score.report)} gün • Devamsızlık ${i4(score.absence)} gün",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun ActivityBars4(score: Score4) {
    val values = listOf(
        Triple("KY", score.ky, Color(0xFF2563EB)),
        Triple("KZ", score.kaizen, Color(0xFFF59E0B)),
        Triple("YK", score.caught, Color(0xFF16A34A)),
        Triple("KH", score.escaped, Color(0xFFDC2626)),
        Triple("M", score.overtime, Color(0xFF7C3AED))
    )
    val maxValue = max(1.0, values.maxOf { it.second })
    Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
        values.forEach { (label, value, color) ->
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Text(i4(value), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Box(
                    Modifier.width(22.dp)
                        .height(max(4.0, 78.0 * value / maxValue).dp)
                        .background(color, RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                )
                Spacer(Modifier.height(3.dp)); Text(label, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun YearScoreChart4(op: Operator4, records: List<Record4>, months: List<YearMonth>) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).height(155.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom
    ) {
        months.forEach { month ->
            val score = calculateMonth4(op, records, month)
            val value = if (score.hasData) score.total.coerceIn(0.0, 100.0) else 0.0
            Column(Modifier.width(42.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Text(if (score.hasData) f14(value) else "—", fontSize = 9.sp)
                Spacer(Modifier.height(3.dp))
                Box(
                    Modifier.width(24.dp)
                        .height(max(3.0, 108.0 * value / 100.0).dp)
                        .background(if (score.hasData) scoreColor4(value) else Color(0xFFE5E7EB), RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                )
                Spacer(Modifier.height(3.dp))
                Text(month.format(DateTimeFormatter.ofPattern("MMM", Locale("tr", "TR"))), fontSize = 9.sp)
            }
        }
    }
}
