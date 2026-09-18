package com.oguzhan.hatakayit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CloudSyncScreen4(
    syncCode: String,
    status: String,
    busy: Boolean,
    onSaveCode: (String) -> Unit,
    onSyncNow: () -> Unit
) {
    var code by remember(syncCode) { mutableStateOf(syncCode) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Telefon + PC Senkron", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            "Aynı senkron kodunu telefonda ve bilgisayarda bir kez kaydet. Bundan sonra kayıtlar yaklaşık 15 saniyede bir ortak bulut veritabanıyla eşitlenir.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Senkron Kodu", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Kod") },
                    singleLine = true
                )
                Button(
                    onClick = { onSaveCode(code.trim()) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy && code.isNotBlank()
                ) { Text("KODU KAYDET") }
            }
        }

        Card {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Bulut Durumu", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (status.isBlank()) "Henüz senkron yapılmadı." else status,
                    color = if (status.startsWith("✓")) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onSyncNow,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy && syncCode.isNotBlank()
                ) {
                    Text(if (busy) "SENKRON YAPILIYOR..." else "ŞİMDİ SENKRONİZE ET")
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4))) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Neler ortak olacak?", fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                Text("Operatörler • kayıtlar • hata fotoğrafları • makineler • parçalar • puanlama ayarları", fontSize = 12.sp)
            }
        }

        Text(
            "İnternet yoksa uygulamayı kullanmaya devam edebilirsin. İnternet geldiğinde yeniden senkron yapılır.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
