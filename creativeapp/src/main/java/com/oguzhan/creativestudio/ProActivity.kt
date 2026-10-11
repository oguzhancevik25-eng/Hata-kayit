package com.oguzhan.creativestudio

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.speech.tts.TextToSpeech
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class RefMedia(val uri: Uri, val name: String, val mime: String, val size: Long)

class ProActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ProApp() }
    }
}

private val P = Color(0xFF6D3DEB)
private val BG = Color(0xFFF4F6FA)
private val INK = Color(0xFF101828)

@Composable
fun ProApp() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("creative_pro_v2", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()

    var tab by remember { mutableStateOf("Studio") }
    var apiKey by remember { mutableStateOf(prefs.getString("gemini_key", "") ?: "") }
    var model by remember { mutableStateOf(prefs.getString("text_model", "gemini-2.5-flash") ?: "gemini-2.5-flash") }
    var imageModel by remember { mutableStateOf(prefs.getString("image_model", "gemini-nano-banana-2.1") ?: "gemini-nano-banana-2.1") }

    var media by remember { mutableStateOf<List<RefMedia>>(emptyList()) }
    var prompt by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("Şarkı") }
    var style by remember { mutableStateOf("Duygusal / Sinematik") }
    var result by remember { mutableStateOf("") }
    var suno by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    var bookIdea by remember { mutableStateOf("") }
    var bookTitle by remember { mutableStateOf("") }
    var outline by remember { mutableStateOf("") }
    var characters by remember { mutableStateOf("") }
    var chapterNo by remember { mutableStateOf("1") }
    var chapterText by remember { mutableStateOf("") }

    var coverPrompt by remember { mutableStateOf("") }
    var cover by remember { mutableStateOf<ByteArray?>(null) }

    var pendingPdf by remember { mutableStateOf<ByteArray?>(null) }
    var pendingImage by remember { mutableStateOf<ByteArray?>(null) }

    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val room = 10 - media.size
        val add = uris.take(room).mapNotNull { proMediaInfo(ctx, it) }
        media = (media + add).take(10)
        status = media.size.toString() + "/10 medya hazır"
    }

    val pdfSaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val b = pendingPdf
        if (uri != null && b != null) {
            runCatching { ctx.contentResolver.openOutputStream(uri)?.use { it.write(b) } }
            status = "PDF kaydedildi."
        }
        pendingPdf = null
    }

    val imageSaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val b = pendingImage
        if (uri != null && b != null) {
            runCatching { ctx.contentResolver.openOutputStream(uri)?.use { it.write(b) } }
            status = "Kapak kaydedildi."
        }
        pendingImage = null
    }

    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember {
        TextToSpeech(ctx) { code -> ttsReady = code == TextToSpeech.SUCCESS }
    }
    DisposableEffect(Unit) { onDispose { tts.stop(); tts.shutdown() } }

    fun requireKey(onReady: () -> Unit) {
        if (apiKey.isBlank()) {
            status = "Gemini API anahtarı gerekli. Ayarlar bölümünden bir kez bağla."
            tab = "Ayarlar"
        } else onReady()
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = P, background = BG, surface = Color.White)) {
        Scaffold(
            containerColor = BG,
            topBar = {
                Surface(shadowElevation = 2.dp) {
                    Column(Modifier.statusBarsPadding().padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(46.dp).background(P, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AutoAwesome, null, tint = Color.White)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Creative Studio Pro 2", fontWeight = FontWeight.Black, fontSize = 19.sp, color = INK)
                                Text("Şarkı • Şiir • Hikaye • Kitap • Kapak • Ses", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            listOf("Studio", "Kitap", "Kapak", "Ayarlar").forEach { t ->
                                FilterChip(selected = tab == t, onClick = { tab = t }, label = { Text(t, fontSize = 10.sp) })
                            }
                        }
                    }
                }
            }
        ) { pad ->
            when (tab) {
                "Studio" -> LazyColumn(Modifier.padding(pad).fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { HeroCard("Yaratıcı Stüdyo", "Prompt yaz veya en fazla 10 fotoğraf/video yükle. Referansları analiz edip profesyonel içerik üretir.") }
                    item {
                        Card(shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.padding(15.dp)) {
                                Text("Ne üretelim?", fontWeight = FontWeight.Black, fontSize = 18.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    listOf("Şarkı", "Şiir", "Hikaye", "Kitap Metni").forEach { t ->
                                        FilterChip(selected = mode == t, onClick = { mode = t }, label = { Text(t, fontSize = 10.sp) })
                                    }
                                }
                                OutlinedTextField(prompt, { prompt = it }, Modifier.fillMaxWidth(), label = { Text("Konu / prompt") }, minLines = 4)
                                OutlinedTextField(style, { style = it }, Modifier.fillMaxWidth(), label = { Text("Stil / duygu") })
                            }
                        }
                    }
                    item {
                        Card(shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.padding(15.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Referans Medya", fontWeight = FontWeight.Black)
                                        Text("Fotoğraf + video toplam 10 adet", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Button(onClick = { mediaPicker.launch(arrayOf("image/*", "video/*")) }, enabled = media.size < 10) {
                                        Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text("Ekle")
                                    }
                                }
                                media.forEachIndexed { i, m ->
                                    Row(Modifier.fillMaxWidth().padding(top = 7.dp).background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp)).padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(if (m.mime.startsWith("video")) Icons.Default.Videocam else Icons.Default.Image, null, tint = P)
                                        Spacer(Modifier.width(8.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(m.name, maxLines = 1, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            Text(proFormatSize(m.size), fontSize = 10.sp, color = Color.Gray)
                                        }
                                        IconButton(onClick = { media = media.toMutableList().also { it.removeAt(i) } }) { Icon(Icons.Default.Close, null) }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Button(
                            onClick = {
                                requireKey {
                                    if (prompt.isBlank() && media.isEmpty()) status = "Konu yaz veya medya ekle."
                                    else scope.launch {
                                        busy = true; status = "Üretiliyor..."
                                        try {
                                            result = GeminiV2.generateWithMedia(ctx, apiKey, model, mode, prompt, style, media)
                                            status = "Hazır."
                                        } catch (e: Exception) { status = "Hata: " + (e.message ?: "") }
                                        busy = false
                                    }
                                }
                            },
                            enabled = !busy, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)
                        ) {
                            if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White) else Icon(Icons.Default.AutoAwesome, null)
                            Spacer(Modifier.width(7.dp)); Text(if (busy) "ÜRETİLİYOR..." else "YAPAY ZEKA İLE ÜRET", fontWeight = FontWeight.Black)
                        }
                        if (status.isNotBlank()) Text(status, fontSize = 12.sp, color = P, modifier = Modifier.padding(top = 5.dp))
                    }
                    if (result.isNotBlank()) item {
                        Card(shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.padding(15.dp)) {
                                Text("Sonuç", fontWeight = FontWeight.Black, fontSize = 18.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    AssistChip(onClick = {
                                        if (ttsReady) {
                                            tts.language = Locale("tr", "TR")
                                            tts.setSpeechRate(0.92f)
                                            tts.speak(result, TextToSpeech.QUEUE_FLUSH, null, "creative")
                                            status = "Seslendirme başladı."
                                        }
                                    }, label = { Text("Seslendir") }, leadingIcon = { Icon(Icons.Default.VolumeUp, null) })
                                    AssistChip(onClick = { tts.stop() }, label = { Text("Durdur") }, leadingIcon = { Icon(Icons.Default.Stop, null) })
                                    AssistChip(onClick = {
                                        pendingPdf = makePdf("Creative Studio • " + mode, result)
                                        pdfSaver.launch("Creative_" + mode.replace(" ", "_") + ".pdf")
                                    }, label = { Text("PDF") }, leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) })
                                }
                                if (mode == "Şarkı") {
                                    Button(onClick = {
                                        requireKey {
                                            scope.launch {
                                                busy = true
                                                try {
                                                    suno = GeminiV2.text(apiKey, model, "Aşağıdaki şarkı için Suno'ya doğrudan yapıştırılacak profesyonel STYLE PROMPT üret. Tür, BPM, vokal, duygu, enstrüman, prodüksiyon ve mix karakterini ver. Sadece style prompt yaz:\n\n" + result)
                                                    status = "Suno promptu hazır."
                                                } catch (e: Exception) { status = "Hata: " + (e.message ?: "") }
                                                busy = false
                                            }
                                        }
                                    }) { Icon(Icons.Default.MusicNote, null); Spacer(Modifier.width(5.dp)); Text("SUNO PROMPT ÜRET") }
                                }
                                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                                Text(result, fontSize = 14.sp, lineHeight = 22.sp)
                            }
                        }
                    }
                    if (suno.isNotBlank()) item { ResultCard("SUNO STYLE PROMPT", suno) }
                }

                "Kitap" -> LazyColumn(Modifier.padding(pad).fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { HeroCard("Kitap Fabrikası", "Fikir → kitap planı → karakter dosyası → bölüm bölüm yazım → PDF.") }
                    item {
                        Card(shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.padding(15.dp)) {
                                OutlinedTextField(bookIdea, { bookIdea = it }, Modifier.fillMaxWidth(), label = { Text("Kitap fikri") }, minLines = 3)
                                OutlinedTextField(bookTitle, { bookTitle = it }, Modifier.fillMaxWidth(), label = { Text("Kitap adı") })
                                Button(onClick = {
                                    requireKey {
                                        scope.launch {
                                            busy = true; status = "Kitap planı hazırlanıyor..."
                                            try {
                                                outline = GeminiV2.text(apiKey, model, "Profesyonel kitap editörü gibi davran. Fikir: " + bookIdea + ". 12 bölümlük kitap planı hazırla. Başlık öner, arka kapak özeti ve numaralı bölüm planı ver. Dramaturji güçlü ve bölümler birbirine bağlı olsun.")
                                                if (bookTitle.isBlank()) bookTitle = outline.lineSequence().firstOrNull()?.replace("#", "")?.take(70) ?: "Kitap"
                                                status = "Plan hazır."
                                            } catch (e: Exception) { status = "Hata: " + (e.message ?: "") }
                                            busy = false
                                        }
                                    }
                                }, Modifier.fillMaxWidth(), enabled = !busy) { Text("1 • KİTAP PLANI OLUŞTUR") }
                            }
                        }
                    }
                    if (outline.isNotBlank()) item { ResultCard("Kitap Planı", outline) }
                    item {
                        Button(onClick = {
                            requireKey {
                                scope.launch {
                                    busy = true; status = "Karakterler oluşturuluyor..."
                                    try {
                                        characters = GeminiV2.text(apiKey, model, "Kitap fikri: " + bookIdea + "\nPlan:\n" + outline + "\nAna ve yardımcı karakterler için ad, yaş, görünüş, geçmiş, amaç, korku, iç çatışma, konuşma biçimi, ilişkiler ve gelişim arkı içeren ayrıntılı KARAKTER DOSYASI oluştur.")
                                        status = "Karakter dosyası hazır."
                                    } catch (e: Exception) { status = "Hata: " + (e.message ?: "") }
                                    busy = false
                                }
                            }
                        }, Modifier.fillMaxWidth(), enabled = !busy) { Text("2 • KARAKTER DOSYASI OLUŞTUR") }
                    }
                    if (characters.isNotBlank()) item { ResultCard("Karakter Dosyası", characters) }
                    item {
                        Card(shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.padding(15.dp)) {
                                OutlinedTextField(chapterNo, { chapterNo = it }, Modifier.fillMaxWidth(), label = { Text("Yazılacak bölüm no") })
                                Button(onClick = {
                                    requireKey {
                                        if (outline.isBlank()) status = "Önce kitap planını oluştur."
                                        else scope.launch {
                                            busy = true; status = chapterNo + ". bölüm yazılıyor..."
                                            try {
                                                chapterText = GeminiV2.text(apiKey, model, "Kitap: " + bookTitle + "\nFikir: " + bookIdea + "\nPlan:\n" + outline + "\nKarakterler:\n" + characters + "\n\n" + chapterNo + ". BÖLÜMÜ tam metin olarak yaz. Sahneleme ve diyalog kullan, özetleme yapma, tutarlılığı koru. Yalnızca bölüm metnini ver.")
                                                status = chapterNo + ". bölüm hazır."
                                            } catch (e: Exception) { status = "Hata: " + (e.message ?: "") }
                                            busy = false
                                        }
                                    }
                                }, Modifier.fillMaxWidth(), enabled = !busy) { Text("3 • BU BÖLÜMÜ YAZ") }
                            }
                        }
                    }
                    if (chapterText.isNotBlank()) item { ResultCard(chapterNo + ". Bölüm", chapterText) }
                    item {
                        Button(onClick = {
                            val full = bookTitle + "\n\nKİTAP PLANI\n" + outline + "\n\nKARAKTER DOSYASI\n" + characters + "\n\nBÖLÜM " + chapterNo + "\n" + chapterText
                            pendingPdf = makePdf(if (bookTitle.isBlank()) "Kitap" else bookTitle, full)
                            pdfSaver.launch((if (bookTitle.isBlank()) "Kitap" else bookTitle.replace(" ", "_")) + ".pdf")
                        }, Modifier.fillMaxWidth()) { Icon(Icons.Default.PictureAsPdf, null); Spacer(Modifier.width(5.dp)); Text("KİTABI PDF OLARAK ÇIKAR") }
                    }
                }

                "Kapak" -> LazyColumn(Modifier.padding(pad).fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { HeroCard("Kapak Stüdyosu", "Gemini görsel modeli ile gerçek kitap kapağı üret.") }
                    item {
                        Card(shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.padding(15.dp)) {
                                OutlinedTextField(coverPrompt, { coverPrompt = it }, Modifier.fillMaxWidth(), label = { Text("Kapak konsepti") }, minLines = 4)
                                Button(onClick = {
                                    requireKey {
                                        if (coverPrompt.isBlank()) status = "Kapak açıklaması yaz."
                                        else scope.launch {
                                            busy = true; status = "Kapak üretiliyor..."
                                            try {
                                                cover = GeminiV2.image(apiKey, imageModel, "Profesyonel kitap kapağı, dikey 2:3, sinematik ve yayın kalitesinde. Üzerinde rastgele yazı üretme; başlık için temiz alan bırak. Konsept: " + coverPrompt)
                                                status = "Kapak hazır."
                                            } catch (e: Exception) { status = "Hata: " + (e.message ?: "") }
                                            busy = false
                                        }
                                    }
                                }, Modifier.fillMaxWidth(), enabled = !busy) { Icon(Icons.Default.Image, null); Spacer(Modifier.width(5.dp)); Text(if (busy) "ÜRETİLİYOR..." else "KAPAK ÜRET") }
                                if (status.isNotBlank()) Text(status, fontSize = 12.sp, color = P, modifier = Modifier.padding(top = 5.dp))
                            }
                        }
                    }
                    if (cover != null) item {
                        Card(shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                val bmp = remember(cover) { BitmapFactory.decodeByteArray(cover, 0, cover!!.size) }
                                if (bmp != null) Image(bmp.asImageBitmap(), null, Modifier.fillMaxWidth())
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = {
                                    pendingImage = cover
                                    imageSaver.launch("Creative_Studio_Kapak.png")
                                }, Modifier.fillMaxWidth()) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(5.dp)); Text("KAPAĞI KAYDET") }
                            }
                        }
                    }
                }

                "Ayarlar" -> LazyColumn(Modifier.padding(pad).fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { HeroCard("Gemini Bağlantısı", "API anahtarı yalnızca bu telefonda saklanır. Ortak bir gizli anahtarı APK içine gömmek güvenli değildir.") }
                    item {
                        Card(shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.padding(15.dp)) {
                                OutlinedTextField(apiKey, { apiKey = it }, Modifier.fillMaxWidth(), label = { Text("Gemini API Key") }, visualTransformation = PasswordVisualTransformation())
                                OutlinedTextField(model, { model = it }, Modifier.fillMaxWidth(), label = { Text("Metin modeli") })
                                OutlinedTextField(imageModel, { imageModel = it }, Modifier.fillMaxWidth(), label = { Text("Görsel modeli") })
                                Button(onClick = {
                                    prefs.edit().putString("gemini_key", apiKey.trim()).putString("text_model", model.trim()).putString("image_model", imageModel.trim()).apply()
                                    status = "Gemini bağlantısı kaydedildi."
                                }, Modifier.fillMaxWidth()) { Text("BAĞLANTIYI KAYDET") }
                                OutlinedButton(onClick = {
                                    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey"))) }
                                }, Modifier.fillMaxWidth()) { Icon(Icons.Default.OpenInBrowser, null); Spacer(Modifier.width(5.dp)); Text("API ANAHTARI OLUŞTUR") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeroCard(title: String, sub: String) {
    Card(colors = CardDefaults.cardColors(containerColor = INK), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text(title, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(5.dp))
            Text(sub, color = Color(0xFFD7DFEC), fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
fun ResultCard(title: String, text: String) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.Black, fontSize = 17.sp, color = INK)
            Spacer(Modifier.height(8.dp))
            Text(text, fontSize = 13.sp, lineHeight = 21.sp)
        }
    }
}

object GeminiV2 {
    suspend fun text(key: String, model: String, prompt: String): String = withContext(Dispatchers.IO) {
        val parts = JSONArray().put(JSONObject().put("text", prompt))
        callText(key, model, parts)
    }

    suspend fun generateWithMedia(ctx: Context, key: String, model: String, mode: String, prompt: String, style: String, media: List<RefMedia>): String = withContext(Dispatchers.IO) {
        val instruction = "Profesyonel yaratıcı yazarlık stüdyosu gibi davran. Tür: " + mode + ". Stil: " + style + ". Kullanıcı isteği: " + prompt + ". Yüklenen medya varsa atmosfer, nesneler, duygu, renk ve hikaye ipuçlarını analiz ederek özgün içerikte kullan; gerçek kişilerin kimliğini tahmin etme. " + if (mode == "Şarkı") "Kıta, nakarat ve gerekiyorsa bridge kullan; söylenebilir ve özgün yaz." else "Güçlü yapı, doğal dil ve özgünlük kullan."
        val parts = JSONArray().put(JSONObject().put("text", instruction))
        var total = 0L
        media.take(10).forEach { m ->
            if (m.size in 1..8_000_000 && total + m.size <= 18_000_000) {
                val bytes = ctx.contentResolver.openInputStream(m.uri)?.use { it.readBytes() } ?: ByteArray(0)
                if (bytes.isNotEmpty()) {
                    total += bytes.size
                    parts.put(JSONObject().put("inlineData", JSONObject().put("mimeType", m.mime).put("data", Base64.encodeToString(bytes, Base64.NO_WRAP))))
                }
            }
        }
        callText(key, model, parts)
    }

    private fun callText(key: String, model: String, parts: JSONArray): String {
        val body = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", parts)))
            .put("generationConfig", JSONObject().put("temperature", 0.9).put("maxOutputTokens", 8192))
        val raw = request("https://generativelanguage.googleapis.com/v1beta/models/" + model.trim() + ":generateContent", key, body)
        val json = JSONObject(raw)
        val cand = json.optJSONArray("candidates") ?: error("Yanıt üretilemedi.")
        if (cand.length() == 0) error("Boş yanıt.")
        val arr = cand.getJSONObject(0).getJSONObject("content").getJSONArray("parts")
        val out = StringBuilder()
        for (i in 0 until arr.length()) out.append(arr.getJSONObject(i).optString("text"))
        return out.toString().trim()
    }

    suspend fun image(key: String, model: String, prompt: String): ByteArray = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
            .put("generationConfig", JSONObject().put("responseModalities", JSONArray().put("IMAGE")))
        val raw = request("https://generativelanguage.googleapis.com/v1/models/" + model.trim() + ":generateContent", key, body)
        val arr = JSONObject(raw).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts")
        for (i in 0 until arr.length()) {
            val inline = arr.getJSONObject(i).optJSONObject("inlineData")
            if (inline != null && inline.optString("data").isNotBlank()) {
                return@withContext Base64.decode(inline.optString("data"), Base64.DEFAULT)
            }
        }
        error("Görsel yanıtı alınamadı.")
    }

    private fun request(url: String, key: String, body: JSONObject): String {
        val c = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 30000
            readTimeout = 180000
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("x-goog-api-key", key.trim())
        }
        c.outputStream.use { it.write(body.toString().toByteArray()) }
        val code = c.responseCode
        val raw = (if (code in 200..299) c.inputStream else c.errorStream).bufferedReader().use { it.readText() }
        if (code !in 200..299) {
            val msg = runCatching { JSONObject(raw).optJSONObject("error")?.optString("message") }.getOrNull() ?: raw.take(240)
            error("API " + code + ": " + msg)
        }
        return raw
    }
}

fun proMediaInfo(ctx: Context, uri: Uri): RefMedia? = try {
    var name = "Medya"
    var size = 0L
    ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
        if (c.moveToFirst()) {
            val n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val s = c.getColumnIndex(OpenableColumns.SIZE)
            if (n >= 0) name = c.getString(n) ?: name
            if (s >= 0) size = c.getLong(s)
        }
    }
    RefMedia(uri, name, ctx.contentResolver.getType(uri) ?: "application/octet-stream", size)
} catch (_: Exception) { null }

fun proFormatSize(v: Long): String = when {
    v >= 1024 * 1024 -> "%.1f MB".format(v / 1024.0 / 1024.0)
    v >= 1024 -> "%.0f KB".format(v / 1024.0)
    else -> v.toString() + " B"
}

fun makePdf(title: String, text: String): ByteArray {
    val pdf = PdfDocument()
    val titlePaint = Paint().apply { textSize = 20f; isFakeBoldText = true; isAntiAlias = true }
    val bodyPaint = Paint().apply { textSize = 11f; isAntiAlias = true }
    val lines = wrap(text, bodyPaint, 500f)
    var index = 0
    var pageNo = 1
    while (index < lines.size || pageNo == 1) {
        val info = PdfDocument.PageInfo.Builder(595, 842, pageNo).create()
        val page = pdf.startPage(info)
        var y = 55f
        page.canvas.drawText(title, 45f, y, titlePaint)
        y += 30f
        while (index < lines.size && y < 800f) {
            page.canvas.drawText(lines[index], 45f, y, bodyPaint)
            y += 16f
            index++
        }
        pdf.finishPage(page)
        pageNo++
    }
    val out = ByteArrayOutputStream()
    pdf.writeTo(out)
    pdf.close()
    return out.toByteArray()
}

fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
    val out = mutableListOf<String>()
    text.lines().forEach { paragraph ->
        if (paragraph.isBlank()) { out.add(""); return@forEach }
        var line = ""
        paragraph.split(Regex("\\s+")).forEach { word ->
            val test = if (line.isBlank()) word else line + " " + word
            if (paint.measureText(test) <= maxWidth) line = test
            else { if (line.isNotBlank()) out.add(line); line = word }
        }
        if (line.isNotBlank()) out.add(line)
    }
    return out
}
