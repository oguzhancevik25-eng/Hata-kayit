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
    var tab by remember { mutableIntStateOf(0) }
    var selectedSicil by remember { mutableStateOf(operators.firstOrNull { it.active }?.sicil ?: "") }

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
                        Text("Aylık Grafik + Parça/Makine Yönetimi • v1.4", color = Color.White.copy(alpha = .82f), fontSize = 11.sp)
                    }
                }
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(tab == 0, { tab = 0 }, { Text("⌂") }, label = { Text("Ana") })
                    NavigationBarItem(tab == 1, { tab = 1 }, { Text("+") }, label = { Text("Kayıt") })
                    NavigationBarItem(tab == 2, { tab = 2 }, { Text("▣") }, label = { Text("Hatalar") })
                    NavigationBarItem(tab == 3, { tab = 3 }, { Text("◎") }, label = { Text("Personel") })
                    NavigationBarItem(tab == 4, { tab = 4 }, { Text("▥") }, label = { Text("Aylık") })
                    NavigationBarItem(tab == 5, { tab = 5 }, { Text("⚙") }, label = { Text("Yönetim") })
                }
            }
        ) { padding ->
            val active = operators.filter { it.active }
            Box(Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                when (tab) {
                    0 -> Dashboard4(records, active) { op -> selectedSicil = op.sicil; tab = 3 }
                    1 -> Entry4(records, refs, operators, machines, parts) { Store4.saveRecords(context, records) }
                    2 -> DefectLibrary4(refs) { Store4.saveReferences(context, refs) }
                    3 -> Person4(records, active, selectedSicil, onSelect = { selectedSicil = it.sicil }, onDelete = {
                        records.remove(it); Store4.saveRecords(context, records)
                    })
                    4 -> MonthlyAnalytics4(records, active)
                    else -> ManagementHub4(
                        operators = operators,
                        records = records,
                        machines = machines,
                        parts = parts,
                        saveOperators = {
                            Store4.saveOperators(context, operators)
                            if (selectedSicil.isBlank() || operators.none { it.sicil == selectedSicil && it.active }) {
                                selectedSicil = operators.firstOrNull { it.active }?.sicil ?: ""
                            }
                        },
                        saveMachines = { Store4.saveMachines(context, machines) },
                        saveParts = { Store4.saveParts(context, parts) }
                    )
                }
            }
        }
    }
}
