package com.oguzhan.kaizenform

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class MainActivity : ComponentActivity() {
    private val prefs by lazy { getSharedPreferences("kaizen_form", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF155EEF),
                    secondary = Color(0xFF087A55),
                    background = Color(0xFFF4F7FB),
                    surface = Color.White
                )
            ) { KaizenApp() }
        }
    }

    @Composable
    private fun KaizenApp() {
        var templateUriText by remember { mutableStateOf(prefs.getString("template_uri", "") ?: "") }
        var sicil by remember { mutableStateOf("2709") }
        var adSoyad by remember { mutableStateOf("Oğuzhan ÇEVİK") }
        var fabrika by remember { mutableStateOf("Fabrika: TBT 1") }
        var bolum by remember { mutableStateOf("Bölümü: I-MNF") }
        var birim by remember { mutableStateOf("Birimi-Hat: INJECTION") }
        var vardiya by remember { mutableStateOf("MAVİ") }
        var amir by remember { mutableStateOf("Amir Adı-Soyadı:BİROL SEZGİN") }
        var tur by remember { mutableStateOf("uygulanmis") }
        var icerik by remember { mutableStateOf("3-Kalite") }
        var baslik by remember { mutableStateOf("") }
        val bugun = remember { SimpleDateFormat("dd.MM.yyyy", Locale("tr","TR")).format(Date()) }
        var formTarih by remember { mutableStateOf(bugun) }
        var kaizenTarih by remember { mutableStateOf(bugun) }
        var once by remember { mutableStateOf("") }
        var sonra by remember { mutableStateOf("") }
        var beforeUri by remember { mutableStateOf<Uri?>(null) }
        var afterUri by remember { mutableStateOf<Uri?>(null) }
        var beforePreview by remember { mutableStateOf<Bitmap?>(null) }
        var afterPreview by remember { mutableStateOf<Bitmap?>(null) }
        var status by remember { mutableStateOf("Hazır") }
        var busy by remember { mutableStateOf(false) }
        var pendingBeforeCamera by remember { mutableStateOf<Uri?>(null) }
        var pendingAfterCamera by remember { mutableStateOf<Uri?>(null) }

        val templatePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
                templateUriText = uri.toString()
                prefs.edit().putString("template_uri", templateUriText).apply()
                status = "Orijinal Kaizen Excel şablonu seçildi"
            }
        }
        val beforeGallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) { beforeUri = uri; beforePreview = loadPreview(uri) }
        }
        val afterGallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) { afterUri = uri; afterPreview = loadPreview(uri) }
        }
        val beforeCamera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
            if (ok && pendingBeforeCamera != null) { beforeUri = pendingBeforeCamera; beforePreview = loadPreview(pendingBeforeCamera!!) }
        }
        val afterCamera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
            if (ok && pendingAfterCamera != null) { afterUri = pendingAfterCamera; afterPreview = loadPreview(pendingAfterCamera!!) }
        }

        fun buildAndDeliver(share: Boolean) {
            if (templateUriText.isBlank()) {
                status = "Önce orijinal Excel şablonunu bir kez seç"
                templatePicker.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/octet-stream"))
                return
            }
            if (baslik.isBlank()) {
                Toast.makeText(this, "Öneri konu başlığını yaz.", Toast.LENGTH_SHORT).show()
                return
            }
            busy = true
            status = "Excel hazırlanıyor…"
            val data = KaizenFormData(sicil, adSoyad, fabrika, bolum, birim, vardiya, amir, tur, icerik, baslik, formTarih, kaizenTarih, once, sonra)
            Thread {
                try {
                    val bytes = XlsxKaizenBuilder(this).build(Uri.parse(templateUriText), data, beforeUri, afterUri)
                    val name = sanitizeFileName(baslik) + " - Kaizen.xlsx"
                    if (share) {
                        val file = File(cacheDir, "share/" + name)
                        file.parentFile?.mkdirs()
                        file.writeBytes(bytes)
                        runOnUiThread {
                            busy = false
                            status = "Excel hazır • paylaşım ekranı açılıyor"
                            shareXlsx(file)
                        }
                    } else {
                        saveToDownloads(bytes, name)
                        runOnUiThread {
                            busy = false
                            status = "Excel İndirilenler klasörüne kaydedildi"
                            Toast.makeText(this, "Excel İndirilenler'e kaydedildi.", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        busy = false
                        status = "Hata: " + (e.message ?: "Excel oluşturulamadı")
                        Toast.makeText(this, status, Toast.LENGTH_LONG).show()
                    }
                }
            }.start()
        }

        Scaffold(
            containerColor = Color(0xFFF4F7FB),
            bottomBar = {
                Surface(shadowElevation = 10.dp) {
                    Row(Modifier.navigationBarsPadding().fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(enabled = !busy, onClick = { buildAndDeliver(false) }, modifier = Modifier.weight(1f).height(52.dp)) {
                            Icon(Icons.Default.Download, null); Spacer(Modifier.width(6.dp)); Text("Excel İndir")
                        }
                        Button(
                            enabled = !busy, onClick = { buildAndDeliver(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF087A55)),
                            modifier = Modifier.weight(1.65f).height(52.dp)
                        ) {
                            Icon(Icons.Default.Share, null); Spacer(Modifier.width(6.dp))
                            Text(if (busy) "Hazırlanıyor…" else "Excel Oluştur ve WhatsApp")
                        }
                    }
                }
            }
        ) { pad ->
            Column(
                Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Kaizen Form Mobil PRO", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(5.dp))
                        Text("Orijinal fabrika Excel şablonunu kullanır • Ön/arka A4 ayarlarını korur • Fotoğraf ekler • Excel'i WhatsApp'a paylaşır",
                            color = Color(0xFFD0D5DD), fontSize = 12.sp, lineHeight = 18.sp)
                    }
                }
                StatusCard(status, templateUriText.isNotBlank(), busy)

                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        SectionTitle("0", "Orijinal Excel Şablonu")
                        Text(
                            if (templateUriText.isBlank()) "İlk kullanımda sadece 1 kez “YENİ KAİZEN FORMU - BOŞ (3).xlsx” dosyasını seç."
                            else "Şablon kayıtlı. Sonraki kullanımlarda tekrar seçmen gerekmez.",
                            fontSize = 12.sp, color = Color.Gray
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { templatePicker.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/octet-stream")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FolderOpen, null); Spacer(Modifier.width(6.dp))
                            Text(if (templateUriText.isBlank()) "ŞABLONU SEÇ" else "ŞABLONU DEĞİŞTİR")
                        }
                    }
                }

                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionTitle("1", "Çalışan Bilgileri")
                        AppTextField("Çalışan Sicil No", sicil) { sicil = it }
                        AppTextField("Ad Soyad", adSoyad) { adSoyad = it }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f)) { AppTextField("Fabrika", fabrika) { fabrika = it } }
                            Box(Modifier.weight(1f)) { AppTextField("Bölümü", bolum) { bolum = it } }
                        }
                        AppTextField("Birimi-Hat", birim) { birim = it }
                        DropdownField("Vardiya", vardiya, listOf("MAVİ","KIRMIZI","BEYAZ","YEŞİL","GECE","GÜNDÜZ")) { vardiya = it }
                        AppTextField("Amir Adı-Soyadı", amir) { amir = it }
                    }
                }

                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionTitle("2", "Öneri Türü ve İçeriği")
                        Text("Öneri Türü", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChoiceButton("A. Uygulanmış", tur == "uygulanmis", Modifier.weight(1f)) { tur = "uygulanmis" }
                            ChoiceButton("B. Öneri kaizen", tur == "oneri", Modifier.weight(1f)) { tur = "oneri" }
                        }
                        Text("Öneri İçeriği", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        val opts = listOf("1-Maliyet","2-Verimlilik-Adam Saat","3-Kalite","4-Güvenlik/İşçi Sağlığı","5-Çevre/Atık-Enerji","6-Alan (m²)","7-VA önerisi","8-Diğer")
                        opts.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { opt -> ChoiceButton(opt, icerik == opt, Modifier.weight(1f)) { icerik = opt } }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionTitle("3", "Kaizen Başlığı ve Tarihler")
                        AppTextField("Öneri Konu Başlığı", baslik) { baslik = it }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f)) { AppTextField("Form Tarihi", formTarih) { formTarih = it } }
                            Box(Modifier.weight(1f)) { AppTextField("Kaizen Tarihi", kaizenTarih) { kaizenTarih = it } }
                        }
                    }
                }

                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionTitle("4", "Öncesi / Sonrası Açıklaması")
                        AppTextField("Kaizen Öncesi Açıklama", once, 5) { once = it }
                        AppTextField("Kaizen Sonrası Açıklama", sonra, 5) { sonra = it }
                    }
                }

                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionTitle("5", "Çizim / Fotoğraf")
                        PhotoPickerCard("ÖNCESİ", beforePreview,
                            onGallery = { beforeGallery.launch("image/*") },
                            onCamera = {
                                val uri = createCameraUri("before")
                                pendingBeforeCamera = uri
                                beforeCamera.launch(uri)
                            }
                        )
                        PhotoPickerCard("SONRASI", afterPreview,
                            onGallery = { afterGallery.launch("image/*") },
                            onCamera = {
                                val uri = createCameraUri("after")
                                pendingAfterCamera = uri
                                afterCamera.launch(uri)
                            }
                        )
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF3)), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Çıktı mantığı", fontWeight = FontWeight.Black, color = Color(0xFF067647))
                        Text(
                            "Uygulama sıfırdan Excel çizmez. Seçtiğin orijinal dosyayı kopyalar ve yalnızca gerekli hücreleri/fotoğrafları doldurur. " +
                                "Ön sayfa, Değerlendirme Çalışma Tab. arka sayfası ve yazdırma ayarları dosyada korunur.",
                            fontSize = 11.sp, lineHeight = 17.sp, color = Color(0xFF067647)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    @Composable
    private fun StatusCard(text: String, templateReady: Boolean, busy: Boolean) {
        val c = when { busy -> Color(0xFFFFFAEB); templateReady -> Color(0xFFECFDF3); else -> Color(0xFFFFF4ED) }
        Card(colors = CardDefaults.cardColors(containerColor = c), shape = RoundedCornerShape(14.dp)) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Icon(if (templateReady) Icons.Default.CheckCircle else Icons.Default.Info, null, tint = if (templateReady) Color(0xFF087A55) else Color(0xFFB54708))
                Spacer(Modifier.width(8.dp)); Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    @Composable private fun SectionTitle(n: String, text: String) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(28.dp).background(Color(0xFF155EEF), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Text(n, color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
            Spacer(Modifier.width(8.dp)); Text(text, fontSize = 17.sp, fontWeight = FontWeight.Black)
        }
    }

    @Composable private fun AppTextField(label: String, value: String, minLines: Int = 1, onChange: (String) -> Unit) {
        OutlinedTextField(value, onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), minLines = minLines, maxLines = if (minLines > 1) 8 else 1)
    }

    @Composable private fun ChoiceButton(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
        Surface(
            modifier.clickable(onClick = onClick),
            color = if (selected) Color(0xFFEEF4FF) else Color.White,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (selected) Color(0xFF155EEF) else Color(0xFFD0D5DD))
        ) {
            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (selected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank, null, tint = if (selected) Color(0xFF155EEF) else Color.Gray)
                Spacer(Modifier.width(6.dp)); Text(text, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }

    @Composable private fun DropdownField(label: String, value: String, options: List<String>, onChange: (String) -> Unit) {
        var expanded by remember { mutableStateOf(false) }
        Box {
            OutlinedTextField(value, {}, readOnly = true, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) })
            Box(Modifier.matchParentSize().clickable { expanded = true })
            DropdownMenu(expanded, { expanded = false }) {
                options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onChange(option); expanded = false }) }
            }
        }
    }

    @Composable private fun PhotoPickerCard(title: String, bitmap: Bitmap?, onGallery: () -> Unit, onCamera: () -> Unit) {
        Surface(color = Color(0xFFF9FAFB), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFFD0D5DD))) {
            Column(Modifier.padding(10.dp)) {
                Text(title, fontWeight = FontWeight.Black, fontSize = 12.sp); Spacer(Modifier.height(7.dp))
                Box(Modifier.fillMaxWidth().height(190.dp).background(Color.White, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    if (bitmap == null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddPhotoAlternate, null, tint = Color.Gray, modifier = Modifier.size(42.dp))
                            Text("Fotoğraf seçilmedi", fontSize = 11.sp, color = Color.Gray)
                        }
                    } else {
                        androidx.compose.foundation.Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    }
                }
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onCamera, modifier = Modifier.weight(1f)) { Icon(Icons.Default.PhotoCamera, null); Spacer(Modifier.width(4.dp)); Text("Kamera") }
                    OutlinedButton(onClick = onGallery, modifier = Modifier.weight(1f)) { Icon(Icons.Default.PhotoLibrary, null); Spacer(Modifier.width(4.dp)); Text("Galeri") }
                }
            }
        }
    }

    private fun loadPreview(uri: Uri): Bitmap? = try { contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } } catch (_: Exception) { null }

    private fun createCameraUri(prefix: String): Uri {
        val dir = File(cacheDir, "camera").apply { mkdirs() }
        val f = File(dir, prefix + "_" + System.currentTimeMillis() + ".jpg")
        return FileProvider.getUriForFile(this, packageName + ".fileprovider", f)
    }

    private fun shareXlsx(file: File) {
        val uri = FileProvider.getUriForFile(this, packageName + ".fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Doldurulmuş Kaizen Excel formu")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Excel'i paylaş"))
    }

    private fun saveToDownloads(bytes: ByteArray, fileName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Kaizen Form")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: throw IOException("İndirilenler klasörü açılamadı")
            contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: throw IOException("Excel kaydedilemedi")
            values.clear(); values.put(MediaStore.Downloads.IS_PENDING, 0); contentResolver.update(uri, values, null, null)
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!dir.exists()) dir.mkdirs()
            File(dir, fileName).writeBytes(bytes)
        }
    }

    private fun sanitizeFileName(s: String): String = s.replace(Regex("""[\\/:*?"<>|]+"""), "_").replace(Regex("""\s+"""), " ").trim().take(70)
}

data class KaizenFormData(
    val sicil: String, val adSoyad: String, val fabrika: String, val bolum: String, val birim: String,
    val vardiya: String, val amir: String, val tur: String, val icerik: String, val baslik: String,
    val formTarih: String, val kaizenTarih: String, val once: String, val sonra: String
)

class XlsxKaizenBuilder(private val context: Context) {
    fun build(templateUri: Uri, data: KaizenFormData, before: Uri?, after: Uri?): ByteArray {
        val files = readZip(templateUri)
        val sheetPath = "xl/worksheets/sheet1.xml"
        var sheet = files[sheetPath]?.toString(Charsets.UTF_8) ?: error("Orijinal şablonda ön sayfa bulunamadı")

        val values = linkedMapOf(
            "B7" to data.sicil, "H7" to data.adSoyad, "S7" to data.fabrika, "Y7" to data.bolum, "AI7" to data.birim,
            "S8" to ("Vardiya: " + data.vardiya), "Y8" to data.amir,
            "E10" to if (data.tur == "uygulanmis") " A. ☑ Uygulanmış" else " A. □ Uygulanmış",
            "E12" to if (data.tur == "oneri") " B. ☑ Öneri kaizen*" else " B. □ Öneri kaizen*",
            "H13" to data.baslik, "AL13" to data.formTarih, "AL14" to data.kaizenTarih,
            "B18" to data.once, "W18" to data.sonra
        )
        values.forEach { entry -> sheet = patchCell(sheet, entry.key, entry.value) }
        files[sheetPath] = sheet.toByteArray(Charsets.UTF_8)

        val drawingPath = "xl/drawings/drawing1.xml"
        var drawing = files[drawingPath]?.toString(Charsets.UTF_8) ?: error("Şablonun çizim katmanı bulunamadı")
        val options = listOf("1-Maliyet","2-Verimlilik-Adam Saat","3-Kalite","4-Güvenlik/İşçi Sağlığı","5-Çevre/Atık-Enerji","6-Alan (m²)","7-VA önerisi","8-Diğer")
        options.forEach { opt ->
            if (opt == data.icerik) {
                val old = "<a:t>" + xmlEscape(opt) + "</a:t>"
                val checked = "<a:t>☑ " + xmlEscape(opt) + "</a:t>"
                if (drawing.contains(old)) drawing = drawing.replace(old, checked)
            }
        }

        val relPath = "xl/drawings/_rels/drawing1.xml.rels"
        var rels = files[relPath]?.toString(Charsets.UTF_8) ?: error("Şablon fotoğraf ilişkileri bulunamadı")
        var nextRid = nextRid(rels)
        var nextNv = nextNvId(drawing)

        fun addPhoto(uri: Uri?, label: String, fromCol: Int, toCol: Int) {
            if (uri == null) return
            val img = processImage(uri)
            val mediaName = "kaizen_" + label + "_" + System.currentTimeMillis() + "_" + nextRid + ".jpg"
            files["xl/media/" + mediaName] = img
            val rid = "rId" + nextRid
            nextRid++
            rels = rels.replace("</Relationships>",
                "<Relationship Id=\"" + rid + "\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" Target=\"../media/" + mediaName + "\"/></Relationships>")
            drawing = drawing.replace("</xdr:wsDr>", pictureAnchor(rid, nextNv++, label, fromCol, 25, toCol, 38) + "</xdr:wsDr>")
        }

        addPhoto(before, "Kaizen_Oncesi", 1, 22)
        addPhoto(after, "Kaizen_Sonrasi", 22, 46)
        files[drawingPath] = drawing.toByteArray(Charsets.UTF_8)
        files[relPath] = rels.toByteArray(Charsets.UTF_8)

        val ctPath = "[Content_Types].xml"
        var ct = files[ctPath]?.toString(Charsets.UTF_8) ?: error("Excel içerik tipleri bulunamadı")
        if (!ct.contains("Extension=\"jpg\"", ignoreCase = true)) {
            ct = ct.replace("</Types>", "<Default Extension=\"jpg\" ContentType=\"image/jpeg\"/></Types>")
            files[ctPath] = ct.toByteArray(Charsets.UTF_8)
        }
        return writeZip(files)
    }

    private fun readZip(uri: Uri): LinkedHashMap<String, ByteArray> {
        val map = LinkedHashMap<String, ByteArray>()
        val input = context.contentResolver.openInputStream(uri) ?: error("Excel şablonu açılamadı")
        ZipInputStream(BufferedInputStream(input)).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                if (!entry.isDirectory) {
                    val out = ByteArrayOutputStream()
                    val buf = ByteArray(16384)
                    while (true) {
                        val n = zis.read(buf)
                        if (n <= 0) break
                        out.write(buf, 0, n)
                    }
                    map[entry.name] = out.toByteArray()
                }
                zis.closeEntry()
            }
        }
        if (!map.containsKey("xl/workbook.xml") || !map.containsKey("xl/worksheets/sheet1.xml")) error("Seçilen dosya geçerli Kaizen Excel (.xlsx) şablonu değil")
        return map
    }

    private fun writeZip(files: LinkedHashMap<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(BufferedOutputStream(out)).use { zos ->
            files.forEach { entry ->
                zos.putNextEntry(ZipEntry(entry.key))
                zos.write(entry.value)
                zos.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun patchCell(xml: String, addr: String, value: String): String {
        val escaped = xmlEscape(value)
        val a = Regex.escape(addr)
        val full = Regex("<c\\b([^>]*\\br=\"" + a + "\"[^>]*)>([\\s\\S]*?)</c>")
        val self = Regex("<c\\b([^>]*\\br=\"" + a + "\"[^>]*)/>")
        fun make(attrs: String): String {
            val cleaned = attrs.replace(Regex("\\s+t=\"[^\"]*\""), "")
            return "<c" + cleaned + " t=\"inlineStr\"><is><t xml:space=\"preserve\">" + escaped + "</t></is></c>"
        }
        val fm = full.find(xml)
        if (fm != null) return xml.replaceRange(fm.range, make(fm.groupValues[1]))
        val sm = self.find(xml)
        if (sm != null) return xml.replaceRange(sm.range, make(sm.groupValues[1]))
        error("Şablonda hedef hücre bulunamadı: " + addr)
    }

    private fun xmlEscape(s: String): String = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")

    private fun nextRid(rels: String): Int {
        var max = 0
        Regex("Id=\"rId(\\d+)\"").findAll(rels).forEach { max = maxOf(max, it.groupValues[1].toInt()) }
        return max + 1
    }

    private fun nextNvId(drawing: String): Int {
        var max = 0
        Regex("<xdr:cNvPr\\b[^>]*id=\"(\\d+)\"").findAll(drawing).forEach { max = maxOf(max, it.groupValues[1].toInt()) }
        return max + 1
    }

    private fun pictureAnchor(rid: String, nvId: Int, name: String, fromCol: Int, fromRow: Int, toCol: Int, toRow: Int): String {
        return "<xdr:twoCellAnchor editAs=\"oneCell\">" +
            "<xdr:from><xdr:col>" + fromCol + "</xdr:col><xdr:colOff>47625</xdr:colOff><xdr:row>" + fromRow + "</xdr:row><xdr:rowOff>47625</xdr:rowOff></xdr:from>" +
            "<xdr:to><xdr:col>" + toCol + "</xdr:col><xdr:colOff>0</xdr:colOff><xdr:row>" + toRow + "</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:to>" +
            "<xdr:pic><xdr:nvPicPr><xdr:cNvPr id=\"" + nvId + "\" name=\"" + xmlEscape(name) + "\"/><xdr:cNvPicPr><a:picLocks noChangeAspect=\"1\"/></xdr:cNvPicPr></xdr:nvPicPr>" +
            "<xdr:blipFill><a:blip r:embed=\"" + rid + "\"/><a:stretch><a:fillRect/></a:stretch></xdr:blipFill>" +
            "<xdr:spPr><a:xfrm/><a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom><a:ln><a:noFill/></a:ln></xdr:spPr></xdr:pic><xdr:clientData/></xdr:twoCellAnchor>"
    }

    private fun processImage(uri: Uri): ByteArray {
        val src = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: error("Fotoğraf açılamadı")
        val targetW = 1500
        val targetH = 900
        val outBmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(outBmp)
        canvas.drawColor(android.graphics.Color.WHITE)
        val scale = minOf((targetW - 24f) / src.width, (targetH - 24f) / src.height)
        val dw = src.width * scale
        val dh = src.height * scale
        val left = (targetW - dw) / 2f
        val top = (targetH - dh) / 2f
        canvas.drawBitmap(src, null, android.graphics.RectF(left, top, left + dw, top + dh), null)
        val out = ByteArrayOutputStream()
        outBmp.compress(Bitmap.CompressFormat.JPEG, 82, out)
        src.recycle()
        outBmp.recycle()
        return out.toByteArray()
    }
}
