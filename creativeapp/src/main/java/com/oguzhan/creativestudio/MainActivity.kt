package com.oguzhan.creativestudio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MediaItem(val uri: Uri, val name: String, val mime: String, val size: Long)
data class SavedProject(val title: String, val type: String, val text: String, val date: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CreativeStudioApp() }
    }
}

private val Bg = Color(0xFFF3F5F8)
private val Ink = Color(0xFF101828)
private val Primary = Color(0xFF6D3DEB)

@Composable
fun CreativeStudioApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("creative_studio_pro", Context.MODE_PRIVATE) }
    var tab by remember { mutableStateOf("Ana Sayfa") }
    var apiKey by remember { mutableStateOf(prefs.getString("api_key", "") ?: "") }
    var model by remember { mutableStateOf(prefs.getString("model", "gemini-2.5-flash") ?: "gemini-2.5-flash") }
    var type by remember { mutableStateOf("Şarkı") }
    var prompt by remember { mutableStateOf("") }
    var style by remember { mutableStateOf("Duygusal / Sinematik") }
    var language by remember { mutableStateOf("Türkçe") }
    var length by remember { mutableStateOf("Orta") }
    var audience by remember { mutableStateOf("Genel") }
    var includeTitle by remember { mutableStateOf(true) }
    var media by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var output by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var projects by remember { mutableStateOf(loadProjects(prefs)) }
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val room = 10 - media.size
        val picked = uris.take(room).mapNotNull { uri ->
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            mediaInfo(context, uri)
        }
        media = (media + picked).take(10)
        status = if (uris.size > room) "En fazla 10 medya eklenebilir." else ""
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = Primary, background = Bg, surface = Color.White)) {
        Scaffold(
            containerColor = Bg,
            topBar = {
                Surface(shadowElevation = 2.dp, color = Color.White) {
                    Column(Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(44.dp).background(Primary, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AutoAwesome, null, tint = Color.White)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Creative Studio Pro", fontWeight = FontWeight.Black, fontSize = 19.sp, color = Ink)
                                Text("Şarkı • Şiir • Hikaye • Kitap • Medya referansı", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Ana Sayfa","Oluştur","Projeler","Ayarlar").forEach { t ->
                                FilterChip(selected = tab == t, onClick = { tab = t }, label = { Text(t, fontSize = 11.sp) })
                            }
                        }
                    }
                }
            }
        ) { pad ->
            when (tab) {
                "Ana Sayfa" -> HomeScreen(Modifier.padding(pad), projects.size) { type = it; tab = "Oluştur" }
                "Oluştur" -> CreateScreen(
                    Modifier.padding(pad), type, { type = it }, prompt, { prompt = it }, style, { style = it },
                    language, { language = it }, length, { length = it }, audience, { audience = it },
                    includeTitle, { includeTitle = it }, media, onPick = { picker.launch(arrayOf("image/*","video/*")) },
                    onRemove = { i -> media = media.toMutableList().also { it.removeAt(i) } },
                    output = output, loading = loading, status = status,
                    onGenerate = {
                        if (prompt.isBlank() && media.isEmpty()) {
                            status = "Bir konu yaz veya fotoğraf/video ekle."
                        } else if (apiKey.isBlank()) {
                            status = "Önce Ayarlar bölümünden Gemini API anahtarını gir."
                            tab = "Ayarlar"
                        } else {
                            loading = true; status = "Yapay zeka içerik üretiyor..."
                            scope.launch {
                                try {
                                    output = GeminiClient.generate(context, apiKey, model, type, prompt, style, language, length, audience, includeTitle, media)
                                    status = "İçerik hazır."
                                } catch (e: Exception) {
                                    status = "Hata: " + (e.message ?: "Bilinmeyen hata")
                                } finally { loading = false }
                            }
                        }
                    },
                    onCopy = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Creative Studio", output))
                        status = "Metin kopyalandı."
                    },
                    onShare = {
                        if (output.isNotBlank()) {
                            val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, output) }
                            context.startActivity(Intent.createChooser(intent, "Paylaş"))
                        }
                    },
                    onSave = {
                        if (output.isNotBlank()) {
                            val title = output.lineSequence().firstOrNull()?.take(60)?.ifBlank { "$type Projesi" } ?: "$type Projesi"
                            val p = SavedProject(title, type, output, SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr")).format(Date()))
                            projects = listOf(p) + projects
                            saveProjects(prefs, projects)
                            status = "Proje kaydedildi."
                        }
                    }
                )
                "Projeler" -> ProjectsScreen(Modifier.padding(pad), projects,
                    onOpen = { output = it.text; type = it.type; tab = "Oluştur" },
                    onDelete = { idx -> projects = projects.toMutableList().also { it.removeAt(idx) }; saveProjects(prefs, projects) }
                )
                "Ayarlar" -> SettingsScreen(Modifier.padding(pad), apiKey, { apiKey = it }, model, { model = it }) {
                    prefs.edit().putString("api_key", apiKey.trim()).putString("model", model.trim()).apply()
                    status = "Ayarlar kaydedildi."
                }
            }
        }
    }
}

@Composable
fun HomeScreen(modifier: Modifier, projectCount: Int, onStart: (String)->Unit) {
    LazyColumn(modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Ink), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Fikrini profesyonel içeriğe dönüştür", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    Text("Prompt yaz veya 10 adede kadar fotoğraf/video yükle. Referanslardan ilham alarak şarkı, şiir, hikaye veya kitap üret.", color = Color(0xFFD9E1EE), fontSize = 13.sp)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("Kayıtlı Proje", projectCount.toString(), Modifier.weight(1f))
                Stat("Medya Limiti", "10", Modifier.weight(1f))
            }
        }
        item { Text("Hızlı Başlat", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Ink) }
        items(listOf("Şarkı","Şiir","Hikaye","Kitap")) { t ->
            Card(Modifier.fillMaxWidth().clickable { onStart(t) }, shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(when(t){"Şarkı"->Icons.Default.MusicNote;"Şiir"->Icons.Default.EditNote;"Hikaye"->Icons.Default.AutoStories;else->Icons.Default.MenuBook}, null, tint=Primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(when(t){"Şarkı"->"Kıta, nakarat, köprü ve stil";"Şiir"->"Duygu, ölçü ve edebi ton";"Hikaye"->"Karakter, sahne, olay örgüsü";else->"Başlık, bölüm planı ve uzun anlatı"}, color=Color.Gray, fontSize=12.sp)
                    }
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }
    }
}

@Composable fun Stat(label:String,value:String,modifier:Modifier){Card(modifier,shape=RoundedCornerShape(16.dp)){Column(Modifier.padding(14.dp)){Text(value,fontSize=24.sp,fontWeight=FontWeight.Black,color=Primary);Text(label,fontSize=11.sp,color=Color.Gray)}}}

@Composable
fun CreateScreen(
    modifier: Modifier, type:String,onType:(String)->Unit,prompt:String,onPrompt:(String)->Unit,
    style:String,onStyle:(String)->Unit,language:String,onLanguage:(String)->Unit,length:String,onLength:(String)->Unit,
    audience:String,onAudience:(String)->Unit,includeTitle:Boolean,onIncludeTitle:(Boolean)->Unit,
    media:List<MediaItem>,onPick:()->Unit,onRemove:(Int)->Unit,output:String,loading:Boolean,status:String,
    onGenerate:()->Unit,onCopy:()->Unit,onShare:()->Unit,onSave:()->Unit
){
    LazyColumn(modifier.fillMaxSize().padding(14.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Card(shape=RoundedCornerShape(20.dp)) { Column(Modifier.padding(15.dp)) {
                Text("Ne üretmek istiyorsun?",fontWeight=FontWeight.Black,fontSize=18.sp)
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp), modifier=Modifier.fillMaxWidth()) {
                    listOf("Şarkı","Şiir","Hikaye","Kitap").forEach { t -> FilterChip(selected=type==t,onClick={onType(t)},label={Text(t,fontSize=11.sp)}) }
                }
                OutlinedTextField(prompt,onPrompt,Modifier.fillMaxWidth(),label={Text("Konu / prompt")},placeholder={Text("Örn: Gece vardiyasında hayallerini bırakmayan bir insan...")},minLines=4)
                OptionRow("Stil",style,listOf("Duygusal / Sinematik","Rap / Pop","Slow","Epik","Romantik","Gerilim","Çocuk","Bilim Kurgu"),onStyle)
                OptionRow("Dil",language,listOf("Türkçe","İngilizce","Almanca"),onLanguage)
                OptionRow("Uzunluk",length,listOf("Kısa","Orta","Uzun","Çok Uzun"),onLength)
                OptionRow("Hedef Kitle",audience,listOf("Genel","Çocuk","Genç","Yetişkin"),onAudience)
                Row(verticalAlignment=Alignment.CenterVertically){Checkbox(includeTitle,onCheckedChange=onIncludeTitle);Text("Başlık da üret")}
            }}
        }
        item {
            Card(shape=RoundedCornerShape(20.dp)) { Column(Modifier.padding(15.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Referans Medya",fontWeight=FontWeight.Black,fontSize=18.sp);Text("Fotoğraf ve video toplam en fazla 10 adet",fontSize=11.sp,color=Color.Gray)};Button(onClick=onPick,enabled=media.size<10){Icon(Icons.Default.Add,null);Text("Ekle")}}
                Spacer(Modifier.height(6.dp))
                if(media.isEmpty()) Text("Henüz medya eklenmedi. İstersen sadece prompt ile de üretebilirsin.",fontSize=12.sp,color=Color.Gray)
                media.forEachIndexed { i,m -> MediaRow(i,m,onRemove) }
            }}
        }
        item {
            Button(onClick=onGenerate,modifier=Modifier.fillMaxWidth().height(54.dp),enabled=!loading,shape=RoundedCornerShape(16.dp)){
                if(loading) CircularProgressIndicator(Modifier.size(22.dp),strokeWidth=2.dp,color=Color.White) else Icon(Icons.Default.AutoAwesome,null)
                Spacer(Modifier.width(8.dp));Text(if(loading)"ÜRETİLİYOR..." else "YAPAY ZEKA İLE ÜRET",fontWeight=FontWeight.Black)
            }
            if(status.isNotBlank()) Text(status,fontSize=12.sp,color=if(status.startsWith("Hata"))Color.Red else Primary,modifier=Modifier.padding(top=6.dp))
        }
        if(output.isNotBlank()) item {
            Card(shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(15.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){Text("Sonuç",fontWeight=FontWeight.Black,fontSize=18.sp,modifier=Modifier.weight(1f));IconButton(onClick=onCopy){Icon(Icons.Default.ContentCopy,null)};IconButton(onClick=onShare){Icon(Icons.Default.Share,null)};IconButton(onClick=onSave){Icon(Icons.Default.BookmarkAdd,null)}}
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))
                Text(output,fontSize=14.sp,lineHeight=22.sp)
            }}
        }
    }
}

@Composable
fun MediaRow(index:Int,m:MediaItem,onRemove:(Int)->Unit){
    Row(Modifier.fillMaxWidth().padding(vertical=6.dp).background(Color(0xFFF8F9FB),RoundedCornerShape(12.dp)).padding(10.dp),verticalAlignment=Alignment.CenterVertically){
        Icon(if(m.mime.startsWith("video"))Icons.Default.Videocam else Icons.Default.Image,null,tint=Primary)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)){Text(m.name,maxLines=1,overflow=TextOverflow.Ellipsis,fontWeight=FontWeight.SemiBold,fontSize=12.sp);Text((if(m.mime.startsWith("video"))"Video" else "Fotoğraf")+" • "+formatSize(m.size),fontSize=10.sp,color=Color.Gray)}
        IconButton(onClick={onRemove(index)}){Icon(Icons.Default.Close,null)}
    }
}

@Composable
fun OptionRow(label:String,value:String,items:List<String>,onChange:(String)->Unit){
    var open by remember { mutableStateOf(false) }
    Column { Text(label,fontSize=11.sp,fontWeight=FontWeight.Bold,color=Color.Gray,modifier=Modifier.padding(top=10.dp,bottom=4.dp));Box{
        OutlinedButton(onClick={open=true},modifier=Modifier.fillMaxWidth()){Text(value,modifier=Modifier.weight(1f));Icon(Icons.Default.ArrowDropDown,null)}
        DropdownMenu(expanded=open,onDismissRequest={open=false}){items.forEach{DropdownMenuItem(text={Text(it)},onClick={onChange(it);open=false})}}
    }}
}

@Composable
fun ProjectsScreen(modifier:Modifier,projects:List<SavedProject>,onOpen:(SavedProject)->Unit,onDelete:(Int)->Unit){
    LazyColumn(modifier.fillMaxSize().padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{Text("Projelerim",fontSize=22.sp,fontWeight=FontWeight.Black)}
        if(projects.isEmpty()) item{Card{Text("Henüz kayıtlı proje yok.",Modifier.padding(18.dp),color=Color.Gray)}}
        itemsIndexed(projects){i,p->Card(Modifier.fillMaxWidth().clickable{onOpen(p)},shape=RoundedCornerShape(16.dp)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Description,null,tint=Primary);Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(p.title,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(p.type+" • "+p.date,fontSize=11.sp,color=Color.Gray)};IconButton(onClick={onDelete(i)}){Icon(Icons.Default.DeleteOutline,null,tint=Color.Red)}}}}
    }
}

@Composable
fun SettingsScreen(modifier:Modifier,apiKey:String,onApiKey:(String)->Unit,model:String,onModel:(String)->Unit,onSave:()->Unit){
    LazyColumn(modifier.fillMaxSize().padding(14.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Card(shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(16.dp)){Text("Yapay Zeka Bağlantısı",fontWeight=FontWeight.Black,fontSize=19.sp);Text("Anahtar yalnızca bu cihazda saklanır.",fontSize=12.sp,color=Color.Gray);OutlinedTextField(apiKey,onApiKey,Modifier.fillMaxWidth(),label={Text("Gemini API Key")},visualTransformation=androidx.compose.ui.text.input.PasswordVisualTransformation());OutlinedTextField(model,onModel,Modifier.fillMaxWidth(),label={Text("Model adı")});Spacer(Modifier.height(8.dp));Button(onClick=onSave,Modifier.fillMaxWidth()){Text("AYARLARI KAYDET")}}}}
        item{Card(shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(16.dp)){Text("Medya kullanımı",fontWeight=FontWeight.Bold);Text("Fotoğraf ve küçük videolar üretim isteğine referans olarak eklenebilir. Çok büyük medya dosyaları güvenli limit nedeniyle atlanır.",fontSize=12.sp,color=Color.Gray)}}}
    }
}

object GeminiClient {
    suspend fun generate(
        context: Context, apiKey:String, model:String, type:String, prompt:String, style:String, language:String,
        length:String, audience:String, includeTitle:Boolean, media:List<MediaItem>
    ): String = withContext(Dispatchers.IO) {
        val instruction = buildInstruction(type,prompt,style,language,length,audience,includeTitle,media)
        val parts = JSONArray().put(JSONObject().put("text",instruction))
        var totalBytes = 0L
        media.take(10).forEach { item ->
            if(item.size in 1..8_000_000 && totalBytes + item.size <= 18_000_000){
                val bytes = context.contentResolver.openInputStream(item.uri)?.use { it.readBytes() } ?: ByteArray(0)
                if(bytes.isNotEmpty()){
                    totalBytes += bytes.size
                    parts.put(JSONObject().put("inlineData",JSONObject().put("mimeType",item.mime).put("data",Base64.encodeToString(bytes,Base64.NO_WRAP))))
                }
            }
        }
        val body = JSONObject()
            .put("contents",JSONArray().put(JSONObject().put("role","user").put("parts",parts)))
            .put("generationConfig",JSONObject().put("temperature",0.9).put("maxOutputTokens",8192))
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/"+model.trim()+":generateContent?key="+apiKey.trim()
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod="POST";doOutput=true;connectTimeout=30000;readTimeout=120000;setRequestProperty("Content-Type","application/json; charset=UTF-8")
        }
        conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val code=conn.responseCode
        val raw=(if(code in 200..299)conn.inputStream else conn.errorStream).bufferedReader().use{it.readText()}
        if(code !in 200..299) throw IllegalStateException("API $code: "+raw.take(300))
        val json=JSONObject(raw)
        val candidates=json.optJSONArray("candidates") ?: throw IllegalStateException("Yanıt üretilemedi.")
        if(candidates.length()==0) throw IllegalStateException("Boş yanıt.")
        val outParts=candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts")
        buildString { for(i in 0 until outParts.length()) append(outParts.getJSONObject(i).optString("text")) }.trim()
    }

    private fun buildInstruction(type:String,prompt:String,style:String,language:String,length:String,audience:String,includeTitle:Boolean,media:List<MediaItem>):String {
        val extra = when(type){
            "Şarkı" -> "Kıta, nakarat, gerekiyorsa pre-chorus ve bridge kullan. Sözleri doğal, özgün ve söylenebilir yaz. Sonunda müzik üretim aracı için STYLE etiketi ver."
            "Şiir" -> "İmge, ritim, duygu ve tutarlı bir edebi ses kullan. Klişelerden kaçın."
            "Hikaye" -> "Güçlü açılış, karakter amacı, çatışma, dönüm noktası ve tatmin edici final oluştur."
            else -> "Kitap formatında profesyonel başlık, kısa arka kapak özeti, içindekiler/bölüm planı ve mümkün olduğunca uzun, tutarlı bölüm metinleri üret. Karakter ve olay sürekliliğini koru."
        }
        return """
Sen profesyonel bir yaratıcı yazarlık stüdyosusun.
Üretim türü: $type
Dil: $language
Stil/Ton: $style
Uzunluk: $length
Hedef kitle: $audience
Başlık üret: ${if(includeTitle)"Evet" else "Hayır"}
Kullanıcı isteği: ${prompt.ifBlank{"Yüklenen medya dosyalarını ana ilham kaynağı olarak kullan."}}
Yüklenen referans sayısı: ${media.size}. Referans görseller/video varsa atmosfer, nesneler, duygu, renk, sahne ve hikaye ipuçlarını analiz ederek esere yansıt; kişilerin kimliğini tahmin etme.
$extra
Çıktıyı doğrudan kullanıma hazır ver. Açıklama yapma; eserin kendisini yaz.
""".trimIndent()
    }
}

fun mediaInfo(context:Context,uri:Uri):MediaItem?{
    return try{
        var name="Medya";var size=0L
        context.contentResolver.query(uri,null,null,null,null)?.use{c->
            if(c.moveToFirst()){val ni=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);val si=c.getColumnIndex(OpenableColumns.SIZE);if(ni>=0)name=c.getString(ni)?:name;if(si>=0)size=c.getLong(si)}
        }
        val mime=context.contentResolver.getType(uri)?:"application/octet-stream"
        MediaItem(uri,name,mime,size)
    }catch(e:Exception){null}
}
fun formatSize(v:Long):String=when{v>=1024*1024->"%.1f MB".format(v/1024.0/1024.0);v>=1024->"%.0f KB".format(v/1024.0);else->"$v B"}

fun loadProjects(prefs:android.content.SharedPreferences):List<SavedProject>{
    return try{
        val arr=JSONArray(prefs.getString("projects","[]"))
        List(arr.length()){i->val o=arr.getJSONObject(i);SavedProject(o.optString("title"),o.optString("type"),o.optString("text"),o.optString("date"))}
    }catch(e:Exception){emptyList()}
}
fun saveProjects(prefs:android.content.SharedPreferences,items:List<SavedProject>){
    val arr=JSONArray();items.take(50).forEach{p->arr.put(JSONObject().put("title",p.title).put("type",p.type).put("text",p.text).put("date",p.date))}
    prefs.edit().putString("projects",arr.toString()).apply()
}
