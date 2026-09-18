package com.oguzhan.hatakayit

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
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
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val XLSX_MIME7 = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

@Composable
fun ExcelExportScreen4(
    records: List<Record4>,
    operators: List<Operator4>,
    machines: List<String>,
    parts: List<Part4>,
    refs: Map<String, String>,
    settings: ScoringSettings4
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val reportTypes = listOf(
        "Ana Panel",
        "Aylık Analiz",
        "Personel Performans",
        "Kayıtlar",
        "Hata Kütüphanesi",
        "Yönetim",
        "Puanlama",
        "Tüm Rapor"
    )
    var reportType by remember { mutableStateOf("Ana Panel") }
    val months = remember {
        buildList {
            var m = YearMonth.from(periodStart4)
            val end = YearMonth.from(periodEnd4)
            while (!m.isAfter(end)) {
                add(m)
                m = m.plusMonths(1)
            }
        }
    }
    val now = YearMonth.now()
    var selectedMonth by remember { mutableStateOf(if (now in months) now else months.first()) }
    var selectedPerson by remember { mutableStateOf("ALL") }
    var pendingBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingName by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var lastUri by remember { mutableStateOf<Uri?>(null) }

    val createDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(XLSX_MIME7)
    ) { uri ->
        val bytes = pendingBytes
        if (uri == null || bytes == null) {
            status = "Excel kaydetme iptal edildi."
            return@rememberLauncherForActivityResult
        }
        try {
            context.contentResolver.openOutputStream(uri, "w")?.use { it.write(bytes) }
                ?: error("Dosya açılamadı")
            lastUri = uri
            status = "✓ Excel dosyası telefona kaydedildi."
        } catch (e: Exception) {
            status = "Excel kaydedilemedi: ${e.message ?: "bilinmeyen hata"}"
        } finally {
            pendingBytes = null
        }
    }

    fun buildAndSave() {
        try {
            val safeMonth = "${selectedMonth.year}_${"%02d".format(selectedMonth.monthValue)}"
            val sheets = when (reportType) {
                "Ana Panel" -> listOf(ExcelReports7.dashboard(records, operators, settings))
                "Aylık Analiz" -> listOf(ExcelReports7.monthly(records, operators, settings, selectedMonth))
                "Personel Performans" -> listOf(ExcelReports7.personnel(records, operators, settings, selectedPerson))
                "Kayıtlar" -> listOf(ExcelReports7.records(records, operators))
                "Hata Kütüphanesi" -> listOf(ExcelReports7.defects(refs))
                "Yönetim" -> listOf(ExcelReports7.management(operators, machines, parts))
                "Puanlama" -> listOf(ExcelReports7.scoring(settings))
                else -> ExcelReports7.all(records, operators, machines, parts, refs, settings, selectedMonth)
            }

            pendingBytes = XlsxWriter7.build(sheets)
            pendingName = when (reportType) {
                "Ana Panel" -> "Operator_Takip_Ana_Panel.xlsx"
                "Aylık Analiz" -> "Operator_Takip_Aylik_$safeMonth.xlsx"
                "Personel Performans" -> {
                    val suffix = if (selectedPerson == "ALL") "Tum_Personel" else "Sicil_$selectedPerson"
                    "Operator_Takip_Personel_$suffix.xlsx"
                }
                "Kayıtlar" -> "Operator_Takip_Kayitlar.xlsx"
                "Hata Kütüphanesi" -> "Operator_Takip_Hata_Kutuphanesi.xlsx"
                "Yönetim" -> "Operator_Takip_Yonetim.xlsx"
                "Puanlama" -> "Operator_Takip_Puanlama.xlsx"
                else -> "Operator_Takip_Tum_Rapor_$safeMonth.xlsx"
            }
            status = "Dosya hazır. Kaydedilecek yeri seç."
            createDocument.launch(pendingName)
        } catch (e: Exception) {
            status = "Excel oluşturulamadı: ${e.message ?: "bilinmeyen hata"}"
        }
    }

    fun shareLast() {
        val uri = lastUri ?: return
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = XLSX_MIME7
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Excel dosyasını gönder"))
        } catch (e: Exception) {
            status = "Paylaşma açılamadı: ${e.message ?: "bilinmeyen hata"}"
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Excel Rapor Merkezi", fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text(
            "Uygulamadaki istediğin bölümü Excel dosyasına dönüştür. Dosya telefona .xlsx olarak kaydolur; bilgisayara gönderebilir ve yazıcıdan çıktı alabilirsin.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("1. Excel'e çevrilecek sayfa", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Selector4("Rapor", reportTypes, reportType, { it }) { reportType = it }

                if (reportType == "Aylık Analiz" || reportType == "Tüm Rapor") {
                    Selector4(
                        "Ay",
                        months,
                        selectedMonth,
                        { monthLabelExport7(it) }
                    ) { selectedMonth = it }
                }

                if (reportType == "Personel Performans") {
                    val people = listOf("ALL") + operators.map { it.sicil }
                    Selector4(
                        "Personel",
                        people,
                        selectedPerson,
                        { value ->
                            if (value == "ALL") "Tüm Personel"
                            else operators.find { it.sicil == value }?.let { "${it.sicil} - ${it.name}" } ?: value
                        }
                    ) { selectedPerson = it }
                }
            }
        }

        Card {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("2. Excel dosyasını oluştur", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Button(
                    onClick = { buildAndSave() },
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    Text("EXCEL'E DÖNÜŞTÜR VE TELEFONA KAYDET", fontWeight = FontWeight.Bold)
                }
                Text(
                    "Kaydet ekranı açılınca Downloads / İndirilenler klasörünü seçebilirsin. Dosya gerçek .xlsx formatındadır.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (status.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (status.startsWith("✓")) Color(0xFFF0FDF4) else Color(0xFFFFFBEB)
                )
            ) {
                Text(
                    status,
                    modifier = Modifier.padding(14.dp),
                    color = if (status.startsWith("✓")) Color(0xFF166534) else Color(0xFF92400E),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (lastUri != null) {
            Button(
                onClick = { shareLast() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
            ) {
                Text("BİLGİSAYARA / WHATSAPP / DRIVE İLE PAYLAŞ")
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Yazdırma için hazır", fontWeight = FontWeight.Bold)
                Text(
                    "Excel çıktıları sayfa genişliğine sığacak şekilde ayarlanır. Bilgisayarda Excel'den Dosya → Yazdır diyerek doğrudan çıktı alabilirsin.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Rapor seçenekleri", fontWeight = FontWeight.Bold)
                Text(
                    "Ana Panel • Aylık Analiz • Personel Performans • Kayıtlar • Hata Kütüphanesi • Yönetim • Puanlama • Tüm Rapor",
                    fontSize = 12.sp
                )
                Text(
                    "Tüm Rapor seçeneği bunların hepsini tek Excel dosyasında ayrı sayfalar olarak oluşturur.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

private fun monthLabelExport7(month: YearMonth): String {
    val tr = Locale("tr", "TR")
    return month.format(DateTimeFormatter.ofPattern("MMMM yyyy", tr))
        .replaceFirstChar { it.uppercase(tr) }
}
