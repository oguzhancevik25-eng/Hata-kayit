package com.oguzhan.bilgikutuphanesi

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BilgiKutuphanesiApp() }
    }
}

private val Navy = Color(0xFF0F172A)
private val Blue = Color(0xFF155EEF)
private val Bg = Color(0xFFF4F7FB)
private val Green = Color(0xFF138A5B)
private val Red = Color(0xFFD92D20)
private val Amber = Color(0xFFB54708)

@Composable
fun BilgiKutuphanesiApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("bilgi_kutuphanesi", Context.MODE_PRIVATE) }

    var dark by remember { mutableStateOf(prefs.getBoolean("dark", false)) }
    var fontSize by remember { mutableStateOf(prefs.getFloat("font_size", 16f)) }
    var screen by remember { mutableStateOf("library") }
    var selectedBook by remember { mutableStateOf<Book?>(null) }
    var selectedChapter by remember { mutableStateOf<Chapter?>(null) }
    var search by remember { mutableStateOf("") }
    var favoriteIds by remember {
        mutableStateOf(prefs.getStringSet("favorites", emptySet())?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet())
    }
    var completed by remember { mutableStateOf(prefs.getStringSet("completed", emptySet()) ?: emptySet()) }
    var noteText by remember { mutableStateOf("") }

    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember { TextToSpeech(context) { code -> ttsReady = code == TextToSpeech.SUCCESS } }
    DisposableEffect(Unit) { onDispose { tts.stop(); tts.shutdown() } }

    val colors = if (dark) darkColorScheme(
        primary = Color(0xFF8AB4FF), background = Color(0xFF0B1220), surface = Color(0xFF111A2B),
        onBackground = Color.White, onSurface = Color.White
    ) else lightColorScheme(primary = Blue, background = Bg, surface = Color.White, onBackground = Navy, onSurface = Navy)

    fun saveFavorites(set:Set<Int>) {
        favoriteIds = set
        prefs.edit().putStringSet("favorites", set.map { it.toString() }.toSet()).apply()
    }
    fun saveCompleted(set:Set<String>) {
        completed = set
        prefs.edit().putStringSet("completed", set).apply()
    }

    MaterialTheme(colorScheme = colors) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Surface(shadowElevation = 2.dp) {
                    Row(Modifier.statusBarsPadding().fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (screen != "library") {
                            IconButton(onClick = {
                                when (screen) {
                                    "reader" -> { screen = "book"; selectedChapter = null }
                                    "book" -> { screen = "library"; selectedBook = null }
                                    else -> screen = "library"
                                }
                            }) { Icon(Icons.Default.ArrowBack, null) }
                        } else {
                            Box(Modifier.size(42.dp).background(Blue, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MenuBook, null, tint = Color.White)
                            }
                            Spacer(Modifier.width(10.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                when(screen) {
                                    "book" -> selectedBook?.title ?: "Kitap"
                                    "reader" -> selectedChapter?.title ?: "Okuma"
                                    "tools" -> "Pratik Araçlar"
                                    "favorites" -> "Favoriler"
                                    "settings" -> "Ayarlar"
                                    else -> "Bilgi Kütüphanesi"
                                },
                                fontWeight = FontWeight.Black, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            if (screen == "library") Text("20 kitap • çevrimdışı • not ve sesli okuma", fontSize = 10.sp, color = Color.Gray)
                        }
                        if (screen == "reader") {
                            IconButton(onClick = {
                                if (ttsReady && selectedChapter != null) {
                                    tts.language = Locale("tr","TR")
                                    tts.setSpeechRate(0.9f)
                                    tts.speak(selectedChapter!!.body, TextToSpeech.QUEUE_FLUSH, null, "chapter")
                                }
                            }) { Icon(Icons.Default.VolumeUp, null) }
                            IconButton(onClick = { tts.stop() }) { Icon(Icons.Default.Stop, null) }
                        }
                    }
                }
            },
            bottomBar = {
                if (screen in listOf("library","tools","favorites","settings")) {
                    NavigationBar {
                        listOf(
                            Triple("library","Kütüphane",Icons.Default.LibraryBooks),
                            Triple("tools","Araçlar",Icons.Default.Build),
                            Triple("favorites","Favoriler",Icons.Default.Favorite),
                            Triple("settings","Ayarlar",Icons.Default.Settings)
                        ).forEach { (id,label,icon) ->
                            NavigationBarItem(selected = screen == id, onClick = { screen = id }, icon = { Icon(icon,null) }, label = { Text(label,fontSize=10.sp) })
                        }
                    }
                }
            }
        ) { pad ->
            when (screen) {
                "library" -> LibraryScreen(
                    Modifier.padding(pad), search, { search = it }, favoriteIds,
                    onToggleFav = { id -> saveFavorites(if (id in favoriteIds) favoriteIds - id else favoriteIds + id) },
                    onOpen = { selectedBook = it; screen = "book" }, completed = completed
                )
                "book" -> selectedBook?.let { book ->
                    BookScreen(
                        Modifier.padding(pad), book, favoriteIds.contains(book.id), completed,
                        onToggleFav = { saveFavorites(if (book.id in favoriteIds) favoriteIds - book.id else favoriteIds + book.id) },
                        onOpenChapter = { ch ->
                            selectedChapter = ch
                            noteText = prefs.getString("note_${book.id}_${book.chapters.indexOf(ch)}","") ?: ""
                            screen = "reader"
                        }
                    )
                }
                "reader" -> if (selectedBook != null && selectedChapter != null) {
                    val book = selectedBook!!
                    val ch = selectedChapter!!
                    val idx = book.chapters.indexOf(ch)
                    val key = "${book.id}_$idx"
                    ReaderScreen(
                        Modifier.padding(pad), book, ch, idx, fontSize, key in completed, noteText,
                        onNoteChange = { noteText = it },
                        onSaveNote = { prefs.edit().putString("note_${book.id}_$idx", noteText).apply() },
                        onToggleComplete = { saveCompleted(if (key in completed) completed - key else completed + key) },
                        onPrev = {
                            if (idx > 0) {
                                val ni = idx - 1
                                selectedChapter = book.chapters[ni]
                                noteText = prefs.getString("note_${book.id}_$ni","") ?: ""
                            }
                        },
                        onNext = {
                            if (idx < book.chapters.lastIndex) {
                                val ni = idx + 1
                                selectedChapter = book.chapters[ni]
                                noteText = prefs.getString("note_${book.id}_$ni","") ?: ""
                            }
                        }
                    )
                }
                "tools" -> ToolsScreen(Modifier.padding(pad))
                "favorites" -> FavoritesScreen(Modifier.padding(pad), favoriteIds, onOpen = { selectedBook = it; screen = "book" })
                "settings" -> SettingsScreen(
                    Modifier.padding(pad), dark, fontSize,
                    onDark = { dark = it; prefs.edit().putBoolean("dark", it).apply() },
                    onFont = { fontSize = it; prefs.edit().putFloat("font_size", it).apply() },
                    onResetProgress = { completed = emptySet(); prefs.edit().remove("completed").apply() }
                )
            }
        }
    }
}

@Composable
fun LibraryScreen(modifier:Modifier, search:String, onSearch:(String)->Unit, favoriteIds:Set<Int>, onToggleFav:(Int)->Unit, onOpen:(Book)->Unit, completed:Set<String>) {
    val filtered = LibraryData.books.filter {
        search.isBlank() || it.title.contains(search,true) || it.category.contains(search,true) ||
        it.chapters.any { ch -> ch.title.contains(search,true) || ch.body.contains(search,true) }
    }
    LazyColumn(modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Navy), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Kendi Eğitim Kütüphanen", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Enjeksiyon, diksiyon, konuşma, tekerleme, insan gelişimi ve iş hayatı dahil 20 kitap.", color = Color(0xFFD7E0EF), fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HeroStat("20","Kitap")
                        HeroStat(LibraryData.books.sumOf { it.chapters.size }.toString(),"Bölüm")
                        HeroStat(completed.size.toString(),"Tamam")
                    }
                }
            }
        }
        item {
            OutlinedTextField(search,onSearch,Modifier.fillMaxWidth(),leadingIcon={Icon(Icons.Default.Search,null)},label={Text("Kitap veya konu ara")},singleLine=true)
        }
        items(filtered) { book -> BookCard(book, favoriteIds.contains(book.id), completed, onToggleFav, onOpen) }
    }
}

@Composable fun HeroStat(v:String,l:String) {
    Box(Modifier.background(Color(0xFF1F2A44), RoundedCornerShape(12.dp)).padding(horizontal=12.dp,vertical=8.dp)) {
        Column { Text(v,color=Color.White,fontWeight=FontWeight.Black,fontSize=18.sp); Text(l,color=Color(0xFFC7D2E2),fontSize=9.sp) }
    }
}

@Composable
fun BookCard(book:Book, favorite:Boolean, completed:Set<String>, onToggleFav:(Int)->Unit, onOpen:(Book)->Unit) {
    val done = book.chapters.indices.count { "${book.id}_$it" in completed }
    Card(modifier = Modifier.fillMaxWidth().clickable { onOpen(book) }, shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(54.dp).background(bookColor(book.id), RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                Text(book.id.toString(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(book.title, fontWeight = FontWeight.Black, fontSize = 15.sp, maxLines=2, overflow=TextOverflow.Ellipsis)
                Text(book.subtitle, fontSize = 11.sp, color = Color.Gray, maxLines=2, overflow=TextOverflow.Ellipsis)
                Spacer(Modifier.height(5.dp))
                Text("${book.category} • ${book.chapters.size} bölüm • $done tamamlandı", fontSize = 10.sp, color = Blue)
            }
            IconButton(onClick = { onToggleFav(book.id) }) {
                Icon(if(favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if(favorite) Red else Color.Gray)
            }
        }
    }
}

@Composable
fun BookScreen(modifier:Modifier, book:Book, favorite:Boolean, completed:Set<String>, onToggleFav:()->Unit, onOpenChapter:(Chapter)->Unit) {
    val done = book.chapters.indices.count { "${book.id}_$it" in completed }
    val pct = if(book.chapters.isEmpty()) 0f else done.toFloat()/book.chapters.size
    LazyColumn(modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = bookColor(book.id)), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Text("${book.id}. KİTAP",color=Color.White,fontSize=11.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                        IconButton(onClick=onToggleFav) { Icon(if(favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,null,tint=Color.White) }
                    }
                    Text(book.title,color=Color.White,fontSize=25.sp,fontWeight=FontWeight.Black)
                    Spacer(Modifier.height(6.dp))
                    Text(book.subtitle,color=Color.White.copy(alpha=.85f),fontSize=12.sp)
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(progress={pct},Modifier.fillMaxWidth(),color=Color.White,trackColor=Color.White.copy(alpha=.2f))
                    Spacer(Modifier.height(5.dp))
                    Text("$done / ${book.chapters.size} bölüm tamamlandı",color=Color.White,fontSize=10.sp)
                }
            }
        }
        item { Text("Bölümler",fontWeight=FontWeight.Black,fontSize=19.sp) }
        items(book.chapters) { ch ->
            val idx = book.chapters.indexOf(ch)
            val key = "${book.id}_$idx"
            Card(Modifier.fillMaxWidth().clickable { onOpenChapter(ch) },shape=RoundedCornerShape(15.dp)) {
                Row(Modifier.padding(13.dp),verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(34.dp).background(if(key in completed) Green else Color(0xFFE9EEF7),RoundedCornerShape(10.dp)),contentAlignment=Alignment.Center) {
                        if(key in completed) Icon(Icons.Default.Check,null,tint=Color.White,modifier=Modifier.size(18.dp))
                        else Text((idx+1).toString(),fontWeight=FontWeight.Bold,color=Navy)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(ch.title,Modifier.weight(1f),fontWeight=FontWeight.SemiBold,fontSize=13.sp)
                    Icon(Icons.Default.ChevronRight,null)
                }
            }
        }
    }
}

@Composable
fun ReaderScreen(modifier:Modifier, book:Book, chapter:Chapter, index:Int, fontSize:Float, completed:Boolean, note:String, onNoteChange:(String)->Unit, onSaveNote:()->Unit, onToggleComplete:()->Unit, onPrev:()->Unit, onNext:()->Unit) {
    LazyColumn(modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(book.title,fontSize=11.sp,color=Blue,fontWeight=FontWeight.Bold); Text(chapter.title,fontSize=24.sp,fontWeight=FontWeight.Black,lineHeight=30.sp) }
        chapter.visual?.let { visual -> item { VisualPanel(visual) } }
        item { Card(shape=RoundedCornerShape(18.dp)) { Text(chapter.body,Modifier.padding(17.dp),fontSize=fontSize.sp,lineHeight=(fontSize*1.55f).sp) } }
        item {
            Card(shape=RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("Kendi Notum",fontWeight=FontWeight.Black)
                    OutlinedTextField(note,onNoteChange,Modifier.fillMaxWidth(),minLines=3,placeholder={Text("Bu bölümden öğrendiğini veya sahadaki örneğini yaz...")})
                    Button(onClick=onSaveNote,Modifier.fillMaxWidth()){Icon(Icons.Default.Save,null);Spacer(Modifier.width(5.dp));Text("NOTU KAYDET")}
                }
            }
        }
        item {
            Button(onClick=onToggleComplete,Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=if(completed) Green else Blue)) {
                Icon(if(completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,null);Spacer(Modifier.width(6.dp))
                Text(if(completed) "BÖLÜM TAMAMLANDI" else "OKUDUM • TAMAMLANDI İŞARETLE")
            }
        }
        item {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick=onPrev,enabled=index>0,modifier=Modifier.weight(1f)){Icon(Icons.Default.ArrowBack,null);Text("Önceki")}
                OutlinedButton(onClick=onNext,enabled=index<book.chapters.lastIndex,modifier=Modifier.weight(1f)){Text("Sonraki");Icon(Icons.Default.ArrowForward,null)}
            }
        }
    }
}

@Composable
fun VisualPanel(type:String) {
    Card(colors=CardDefaults.cardColors(containerColor=Color(0xFFEEF4FF)),shape=RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text("GÖRSEL ANLATIM",fontWeight=FontWeight.Black,color=Blue,fontSize=11.sp); Spacer(Modifier.height(9.dp))
            when(type) {
                "injection_cycle" -> Flow(listOf("Hammadde","Plastikleştirme","Dolum","V→P","Holding","Soğutma","İtici"))
                "machine_parts" -> { Flow(listOf("Huni","Vida/Kovan","Nozül","Kalıp","Kilitleme","İtici")); Text("Malzeme akışı ve makine hareketi senkron çalışır.",fontSize=11.sp,modifier=Modifier.padding(top=8.dp)) }
                "mold_map" -> Flow(listOf("Sabit Plaka","Cavity","Gate","Core","İtici","Hareketli Plaka"))
                "valve_gate" -> { Flow(listOf("Gate 1 Aç","Akış İlerle","Gate 2 Aç","Birleşme","Gate 3 Aç","Dolum Sonu")); Text("Zamanlama, birleşme noktasını ve lokal gerilimi değiştirir.",fontSize=11.sp,modifier=Modifier.padding(top=8.dp)) }
                "vp" -> Flow(listOf("Hız Kontrol","%90-98 Dolum","V→P","Basınç Kontrol","Holding"))
                "holding" -> Flow(listOf("Kavite Dolu","Holding","Gate Donma","Basınç Kes","Soğutma"))
                "defect_flash" -> DefectVisual("ÇAPAK","Aşırı basınç / kapanma / parting line",Red)
                "defect_short" -> DefectVisual("EKSİK","Yetersiz akış / erken V→P / gate",Amber)
                "defect_white" -> DefectVisual("BEYAZLIK","Lokal gerilim / akış birleşmesi / valf gate",Color(0xFF667085))
                "cycle" -> Flow(listOf("Dolum","Holding","Soğutma","Kalıp Aç","Robot","Kalıp Kapa"))
                "breath" -> Flow(listOf("Rahat Nefes Al","Omuz Sabit","Alt Kaburga Genişle","Yavaş Ver","Cümleye Başla"))
                else -> Text("Bu bölüm için adım adım görsel akış.",fontSize=12.sp)
            }
        }
    }
}

@Composable
fun Flow(items:List<String>) {
    Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
        items.forEachIndexed { i, s ->
            Row(verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.background(if(i%2==0) Blue else Green,RoundedCornerShape(10.dp)).padding(horizontal=10.dp,vertical=7.dp)) {
                    Text(s,color=Color.White,fontSize=11.sp,fontWeight=FontWeight.Bold)
                }
                if(i<items.lastIndex) { Spacer(Modifier.width(6.dp)); Icon(Icons.Default.ArrowDownward,null,tint=Color.Gray,modifier=Modifier.size(16.dp)) }
            }
        }
    }
}

@Composable
fun DefectVisual(name:String, cause:String, color:Color) {
    Row(verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(76.dp).background(color,RoundedCornerShape(16.dp)),contentAlignment=Alignment.Center) { Text(name,color=Color.White,fontWeight=FontWeight.Black,fontSize=12.sp) }
        Spacer(Modifier.width(12.dp))
        Column { Text("Muhtemel odak",fontWeight=FontWeight.Bold,fontSize=11.sp); Text(cause,fontSize=12.sp); Text("Önce bölgeyi ve akış yönünü doğrula.",fontSize=11.sp,color=Color.Gray) }
    }
}

@Composable
fun ToolsScreen(modifier:Modifier) {
    var defect by remember { mutableStateOf(LibraryData.injectionDefects.keys.first()) }
    var expanded by remember { mutableStateOf(false) }
    var tongue by remember { mutableStateOf(LibraryData.tongueTwisters.first()) }
    LazyColumn(modifier.fillMaxSize().padding(14.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { Text("Pratik Araçlar",fontSize=23.sp,fontWeight=FontWeight.Black) }
        item {
            Card(shape=RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(15.dp)) {
                    Text("Enjeksiyon Hata Çözüm Asistanı",fontWeight=FontWeight.Black,fontSize=17.sp)
                    Text("Hata seç → hızlı kontrol sırasını gör.",fontSize=11.sp,color=Color.Gray)
                    Box {
                        OutlinedButton(onClick={expanded=true},Modifier.fillMaxWidth()){Text(defect,Modifier.weight(1f));Icon(Icons.Default.ArrowDropDown,null)}
                        DropdownMenu(expanded,{expanded=false}) { LibraryData.injectionDefects.keys.forEach { d -> DropdownMenuItem(text={Text(d)},onClick={defect=d;expanded=false}) } }
                    }
                    Card(colors=CardDefaults.cardColors(containerColor=Color(0xFFF0F7FF)),shape=RoundedCornerShape(14.dp)) {
                        Text(LibraryData.injectionDefects[defect]?:"",Modifier.padding(13.dp),fontSize=13.sp,lineHeight=20.sp)
                    }
                }
            }
        }
        item {
            Card(shape=RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(15.dp)) {
                    Text("Tekerleme Antrenmanı",fontWeight=FontWeight.Black,fontSize=17.sp)
                    Text(tongue,fontSize=16.sp,lineHeight=24.sp,modifier=Modifier.padding(vertical=12.dp))
                    Button(onClick={tongue=LibraryData.tongueTwisters[Random.nextInt(LibraryData.tongueTwisters.size)]},Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Shuffle,null);Spacer(Modifier.width(6.dp));Text("YENİ TEKERLEME")
                    }
                }
            }
        }
        item {
            Card(shape=RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(15.dp)) {
                    Text("10 Dakikalık Diksiyon Rutini",fontWeight=FontWeight.Black,fontSize=17.sp)
                    listOf("2 dk nefes","2 dk çene-dudak-dil","2 dk tekerleme","2 dk sesli okuma","2 dk serbest anlatım + kayıt").forEach {
                        Row(Modifier.padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.CheckCircle,null,tint=Green,modifier=Modifier.size(17.dp));Spacer(Modifier.width(7.dp));Text(it,fontSize=12.sp)}
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(modifier:Modifier, favoriteIds:Set<Int>, onOpen:(Book)->Unit) {
    val list = LibraryData.books.filter { it.id in favoriteIds }
    LazyColumn(modifier.fillMaxSize().padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item { Text("Favori Kitaplar",fontSize=23.sp,fontWeight=FontWeight.Black) }
        if(list.isEmpty()) item { Text("Henüz favori kitap eklemedin.",color=Color.Gray) }
        items(list) { book ->
            Card(Modifier.fillMaxWidth().clickable{onOpen(book)},shape=RoundedCornerShape(16.dp)) {
                Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite,null,tint=Red); Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)){Text(book.title,fontWeight=FontWeight.Bold);Text(book.category,fontSize=10.sp,color=Color.Gray)}
                    Icon(Icons.Default.ChevronRight,null)
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(modifier:Modifier,dark:Boolean,fontSize:Float,onDark:(Boolean)->Unit,onFont:(Float)->Unit,onResetProgress:()->Unit) {
    LazyColumn(modifier.fillMaxSize().padding(14.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Card(shape=RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(15.dp)) {
                    Text("Okuma Ayarları",fontWeight=FontWeight.Black,fontSize=18.sp)
                    Row(verticalAlignment=Alignment.CenterVertically) { Text("Koyu tema",Modifier.weight(1f)); Switch(dark,onCheckedChange=onDark) }
                    Text("Yazı boyutu: ${fontSize.toInt()}",fontSize=12.sp)
                    Slider(value=fontSize,onValueChange=onFont,valueRange=13f..24f,steps=10)
                }
            }
        }
        item {
            Card(shape=RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(15.dp)) {
                    Text("Öğrenme Sistemi",fontWeight=FontWeight.Black,fontSize=18.sp)
                    Text("Tamamlanan bölümler, favoriler ve bölüm notların cihazda saklanır.",fontSize=12.sp,color=Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick=onResetProgress,Modifier.fillMaxWidth()) { Icon(Icons.Default.RestartAlt,null);Spacer(Modifier.width(5.dp));Text("OKUMA İLERLEMESİNİ SIFIRLA") }
                }
            }
        }
        item {
            Card(colors=CardDefaults.cardColors(containerColor=Color(0xFFFFFAEB)),shape=RoundedCornerShape(18.dp)) {
                Text("Sağlık ve konuşma bölümleri eğitim amaçlıdır. Süregelen veya belirgin bir sorun için ilgili sağlık uzmanından kişisel değerlendirme alın.",Modifier.padding(14.dp),fontSize=11.sp,color=Amber)
            }
        }
    }
}

fun bookColor(id:Int):Color = when(id%6) {
    0 -> Color(0xFF6941C6)
    1 -> Color(0xFF155EEF)
    2 -> Color(0xFF0E9384)
    3 -> Color(0xFFB54708)
    4 -> Color(0xFFD92D20)
    else -> Color(0xFF475467)
}
