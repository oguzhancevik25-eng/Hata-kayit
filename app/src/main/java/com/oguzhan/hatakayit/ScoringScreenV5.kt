package com.oguzhan.hatakayit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScoringSettingsScreen4(
    settings: ScoringSettings4,
    onSave: (ScoringSettings4) -> Unit
) {
    fun txt(v: Double) = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()

    var qualityMax by remember(settings) { mutableStateOf(txt(settings.qualityMax)) }
    var kyMax by remember(settings) { mutableStateOf(txt(settings.kyMax)) }
    var kaizenMax by remember(settings) { mutableStateOf(txt(settings.kaizenMax)) }
    var attendanceMax by remember(settings) { mutableStateOf(txt(settings.attendanceMax)) }
    var overtimeMax by remember(settings) { mutableStateOf(txt(settings.overtimeMax)) }
    var qualityStart by remember(settings) { mutableStateOf(txt(settings.qualityStart)) }
    var escapedPenalty by remember(settings) { mutableStateOf(txt(settings.escapedPenalty)) }
    var caughtBonus by remember(settings) { mutableStateOf(txt(settings.caughtBonus)) }
    var caughtBonusCap by remember(settings) { mutableStateOf(txt(settings.caughtBonusCap)) }
    var kyTarget by remember(settings) { mutableStateOf(txt(settings.kyMonthlyTarget)) }
    var kaizenTarget by remember(settings) { mutableStateOf(txt(settings.kaizenMonthlyTarget)) }
    var overtimeTarget by remember(settings) { mutableStateOf(txt(settings.overtimeMonthlyTarget)) }
    var absencePenalty by remember(settings) { mutableStateOf(txt(settings.absencePenaltyPerDay)) }
    var gradeA by remember(settings) { mutableStateOf(txt(settings.gradeA)) }
    var gradeB by remember(settings) { mutableStateOf(txt(settings.gradeB)) }
    var gradeC by remember(settings) { mutableStateOf(txt(settings.gradeC)) }
    var message by remember { mutableStateOf("") }

    fun n(s: String): Double? = s.replace(',', '.').toDoubleOrNull()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Puanlama Ayarları", fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text(
            "KY, Kaizen, kalite, devam ve mesainin puan ağırlıklarını kendin belirleyebilirsin. Değişiklik kaydedilince tüm eski kayıtların puanı yeni sisteme göre otomatik yeniden hesaplanır.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Puan Dağılımı", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                NumberField4("Kalite maksimum puan", qualityMax) { qualityMax = it }
                NumberField4("KY maksimum puan", kyMax) { kyMax = it }
                NumberField4("Kaizen maksimum puan", kaizenMax) { kaizenMax = it }
                NumberField4("Devam maksimum puan", attendanceMax) { attendanceMax = it }
                NumberField4("Mesai maksimum puan", overtimeMax) { overtimeMax = it }

                val total = listOf(qualityMax, kyMax, kaizenMax, attendanceMax, overtimeMax)
                    .mapNotNull { n(it) }.sum()
                Text(
                    "Toplam ağırlık: ${f14(total)} puan. Uygulama sonucu yine 100 üzerinden normalize eder.",
                    fontSize = 12.sp,
                    color = Color(0xFF1D4ED8),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Card {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Kalite Puanı", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                NumberField4("Kalite başlangıç puanı", qualityStart) { qualityStart = it }
                NumberField4("Kaçan hata cezası × katsayı", escapedPenalty) { escapedPenalty = it }
                NumberField4("Yakalanan hata bonusu × katsayı", caughtBonus) { caughtBonus = it }
                NumberField4("Yakalanan hata bonus tavanı", caughtBonusCap) { caughtBonusCap = it }
                Text(
                    "Örnek: Çapak katsayısı 1 ve kaçan hata cezası 4 ise 1 adet kaçan çapak kalite puanından 4 puan düşürür.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Aylık Hedefler ve Devamsızlık", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                NumberField4("Aylık KY hedefi (adet)", kyTarget) { kyTarget = it }
                NumberField4("Aylık Kaizen hedefi (adet)", kaizenTarget) { kaizenTarget = it }
                NumberField4("Aylık mesai hedefi (saat)", overtimeTarget) { overtimeTarget = it }
                NumberField4("Devamsızlık cezası / gün", absencePenalty) { absencePenalty = it }
                Text(
                    "Yıllık izin, günlük izin ve rapor bilgi amaçlı takip edilir; varsayılan olarak puan düşürmez.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Sınıf Sınırları", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                NumberField4("A sınıfı alt sınır", gradeA) { gradeA = it }
                NumberField4("B sınıfı alt sınır", gradeB) { gradeB = it }
                NumberField4("C sınıfı alt sınır", gradeC) { gradeC = it }
            }
        }

        Button(
            onClick = {
                val vals = listOf(
                    n(qualityMax), n(kyMax), n(kaizenMax), n(attendanceMax), n(overtimeMax),
                    n(qualityStart), n(escapedPenalty), n(caughtBonus), n(caughtBonusCap),
                    n(kyTarget), n(kaizenTarget), n(overtimeTarget), n(absencePenalty),
                    n(gradeA), n(gradeB), n(gradeC)
                )
                if (vals.any { it == null }) {
                    message = "Tüm alanlara geçerli sayı gir."
                } else {
                    val qMax = n(qualityMax)!!
                    val kyM = n(kyMax)!!
                    val kM = n(kaizenMax)!!
                    val aM = n(attendanceMax)!!
                    val oM = n(overtimeMax)!!
                    val a = n(gradeA)!!
                    val b = n(gradeB)!!
                    val c = n(gradeC)!!

                    when {
                        listOf(qMax, kyM, kM, aM, oM).any { it < 0 } -> message = "Puanlar negatif olamaz."
                        qMax + kyM + kM + aM + oM <= 0 -> message = "Toplam puan ağırlığı 0 olamaz."
                        n(qualityStart)!! < 0 || n(qualityStart)!! > qMax -> message = "Kalite başlangıç puanı 0 ile kalite maksimum puanı arasında olmalı."
                        n(kyTarget)!! <= 0 || n(kaizenTarget)!! <= 0 || n(overtimeTarget)!! <= 0 -> message = "Aylık hedefler 0'dan büyük olmalı."
                        listOf(a, b, c).any { it < 0 || it > 100 } || !(a > b && b > c) ->
                            message = "Sınıf sınırları A > B > C ve 0–100 aralığında olmalı."
                        else -> {
                            val newSettings = ScoringSettings4(
                                qualityMax = qMax,
                                kyMax = kyM,
                                kaizenMax = kM,
                                attendanceMax = aM,
                                overtimeMax = oM,
                                qualityStart = n(qualityStart)!!,
                                escapedPenalty = n(escapedPenalty)!!.coerceAtLeast(0.0),
                                caughtBonus = n(caughtBonus)!!.coerceAtLeast(0.0),
                                caughtBonusCap = n(caughtBonusCap)!!.coerceAtLeast(0.0),
                                kyMonthlyTarget = n(kyTarget)!!,
                                kaizenMonthlyTarget = n(kaizenTarget)!!,
                                overtimeMonthlyTarget = n(overtimeTarget)!!,
                                absencePenaltyPerDay = n(absencePenalty)!!.coerceAtLeast(0.0),
                                gradeA = a,
                                gradeB = b,
                                gradeC = c
                            )
                            onSave(newSettings)
                            message = "✓ Puanlama kaydedildi. Tüm puanlar yeniden hesaplandı."
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            Text("PUANLAMAYI KAYDET", fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = {
                onSave(defaultScoringSettings4)
                message = "✓ Varsayılan puanlama geri yüklendi."
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("VARSAYILANA DÖN")
        }

        if (message.isNotBlank()) {
            Text(
                message,
                fontWeight = FontWeight.SemiBold,
                color = if (message.startsWith("✓")) Color(0xFF15803D) else Color(0xFFB45309)
            )
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun NumberField4(
    label: String,
    value: String,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' }) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}
