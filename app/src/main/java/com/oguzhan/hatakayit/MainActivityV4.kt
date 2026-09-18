package com.oguzhan.hatakayit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivityV4 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OperatorApp4() }
    }
}

@Composable
fun OperatorApp4() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val records = remember { mutableStateListOf<Record4>().apply { addAll(Store4.loadRecords(context)) } }
    val refs = remember { mutableStateMapOf<String, String>().apply { putAll(Store4.loadReferences(context)) } }
    val operators = remember { mutableStateListOf<Operator4>().apply { addAll(Store4.loadOperators(context)) } }
    val machines = remember { mutableStateListOf<String>().apply { addAll(Store4.loadMachines(context)) } }
    val parts = remember { mutableStateListOf<Part4>().apply { addAll(Store4.loadParts(context)) } }
    var scoringSettings by remember { mutableStateOf(loadScoringSettings4(context)) }

    var syncCode by remember { mutableStateOf(CloudSync4.loadCode(context)) }
    var syncStatus by remember { mutableStateOf("") }
    var syncBusy by remember { mutableStateOf(false) }
    var cloudDirty by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var tab by remember { mutableIntStateOf(0) }
    var selectedSicil by remember { mutableStateOf(operators.firstOrNull { it.active }?.sicil ?: "") }

    fun currentSnapshot() = CloudSnapshot4(
        operators = operators.toList(),
        records = records.toList(),
        refs = refs.toMap(),
        machines = machines.toList(),
        parts = parts.toList(),
        scoring = scoringSettings
    )

    fun saveAllLocal(snapshot: CloudSnapshot4) {
        operators.clear(); operators.addAll(snapshot.operators)
        records.clear(); records.addAll(snapshot.records)
        refs.clear(); refs.putAll(snapshot.refs)
        machines.clear(); machines.addAll(snapshot.machines)
        parts.clear(); parts.addAll(snapshot.parts)
        scoringSettings = snapshot.scoring

        Store4.saveOperators(context, operators)
        Store4.saveRecords(context, records)
        Store4.saveReferences(context, refs)
        Store4.saveMachines(context, machines)
        Store4.saveParts(context, parts)
        saveScoringSettings4(context, scoringSettings)

        if (selectedSicil.isBlank() || operators.none { it.sicil == selectedSicil && it.active }) {
            selectedSicil = operators.firstOrNull { it.active }?.sicil ?: ""
        }
    }

    fun markRecordsSaved() {
        Store4.saveRecords(context, records)
        cloudDirty = true
    }
    fun markRefsSaved() {
        Store4.saveReferences(context, refs)
        cloudDirty = true
    }
    fun markOperatorsSaved() {
        Store4.saveOperators(context, operators)
        cloudDirty = true
        if (selectedSicil.isBlank() || operators.none { it.sicil == selectedSicil && it.active }) {
            selectedSicil = operators.firstOrNull { it.active }?.sicil ?: ""
        }
    }
    fun markMachinesSaved() {
        Store4.saveMachines(context, machines)
        cloudDirty = true
    }
    fun markPartsSaved() {
        Store4.saveParts(context, parts)
        cloudDirty = true
    }

    suspend fun syncOnce() {
        if (syncCode.isBlank() || syncBusy) return
        syncBusy = true
        try {
            if (cloudDirty) {
                val snapshot = currentSnapshot()
                withContext(Dispatchers.IO) { CloudSync4.push(context, syncCode, snapshot) }
                cloudDirty = false
                syncStatus = "✓ Değişiklikler buluta gönderildi"
            } else {
                val remote = withContext(Dispatchers.IO) { CloudSync4.pull(context, syncCode) }
                if (remote == null) {
                    val snapshot = currentSnapshot()
                    withContext(Dispatchers.IO) { CloudSync4.push(context, syncCode, snapshot) }
                    syncStatus = "✓ İlk veri buluta yüklendi"
                } else {
                    saveAllLocal(remote)
                    syncStatus = "✓ Telefon ve PC verileri güncel"
                }
            }
        } catch (e: Exception) {
            syncStatus = e.message ?: "Senkron hatası"
        } finally {
            syncBusy = false
        }
    }

    LaunchedEffect(syncCode) {
        if (syncCode.isBlank()) return@LaunchedEffect
        syncOnce()
        while (isActive) {
            delay(15000)
            syncOnce()
        }
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF0B4F7D), secondary = Color(0xFF0E7490), tertiary = Color(0xFF15803D),
            error = Color(0xFFB91C1C), background = Color(0xFFF5F7FA)
        )
    ) {
        Scaffold(
            topBar = {
                Surface(color = MaterialTheme.colorScheme.primary, shadowElevation = 3.dp) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp)) {
                        Text("Operatör Takip", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text("Excel Rapor Merkezi • v1.7", color = Color.White.copy(alpha = .82f), fontSize = 11.sp)
                    }
                }
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(tab == 0, { tab = 0 }, { Text("⌂") }, label = { Text("Ana") })
                    NavigationBarItem(tab == 1, { tab = 1 }, { Text("+") }, label = { Text("Kayıt") })
                    NavigationBarItem(tab == 2, { tab = 2 }, { Text("▣") }, label = { Text("Hata") })
                    NavigationBarItem(tab == 3, { tab = 3 }, { Text("◎") }, label = { Text("Kişi") })
                    NavigationBarItem(tab == 4, { tab = 4 }, { Text("▥") }, label = { Text("Aylık") })
                    NavigationBarItem(tab == 5, { tab = 5 }, { Text("⚙") }, label = { Text("Ayar") })
                    NavigationBarItem(tab == 6, { tab = 6 }, { Text("Σ") }, label = { Text("Puan") })
                    NavigationBarItem(tab == 7, { tab = 7 }, { Text("XLS") }, label = { Text("Excel") })
                }
            }
        ) { padding ->
            val active = operators.filter { it.active }
            Box(Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                when (tab) {
                    0 -> Dashboard4(records, active, scoringSettings) { op -> selectedSicil = op.sicil; tab = 3 }
                    1 -> Entry4(records, refs, operators, machines, parts) { markRecordsSaved() }
                    2 -> DefectLibrary4(refs) { markRefsSaved() }
                    3 -> Person4(records, active, selectedSicil, scoringSettings, onSelect = { selectedSicil = it.sicil }, onDelete = {
                        records.remove(it); markRecordsSaved()
                    })
                    4 -> MonthlyAnalytics4(records, active, scoringSettings)
                    5 -> ManagementHub4(
                        operators = operators,
                        records = records,
                        machines = machines,
                        parts = parts,
                        saveOperators = { markOperatorsSaved() },
                        saveMachines = { markMachinesSaved() },
                        saveParts = { markPartsSaved() },
                        syncCode = syncCode,
                        syncStatus = syncStatus,
                        syncBusy = syncBusy,
                        saveSyncCode = { code ->
                            syncCode = code.trim()
                            CloudSync4.saveCode(context, syncCode)
                            syncStatus = if (syncCode.isBlank()) "Senkron kapatıldı" else "Kod kaydedildi, bağlantı kuruluyor..."
                        },
                        syncNow = { scope.launch { syncOnce() } }
                    )
                    6 -> ScoringSettingsScreen4(scoringSettings) { updated ->
                        scoringSettings = updated
                        saveScoringSettings4(context, updated)
                        cloudDirty = true
                    }
                    else -> ExcelExportScreen4(
                        records = records,
                        operators = operators,
                        machines = machines,
                        parts = parts,
                        refs = refs,
                        settings = scoringSettings
                    )
                }
            }
        }
    }
}
