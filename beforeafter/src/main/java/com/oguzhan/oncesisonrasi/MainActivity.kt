package com.oguzhan.oncesisonrasi

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

data class PhotoSlot12(
    val index: Int,
    val uri: Uri? = null,
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotation: Float = 0f,
    val caption: String = "",
    val dateText: String = "",
    val autoDate: Boolean = true
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BeforeAfterProApp() }
    }
}

@Composable
fun BeforeAfterProApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var photoCount by remember { mutableIntStateOf(2) }
    var layoutMode by remember { mutableStateOf("Çiftler") }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var showDate by remember { mutableStateOf(true) }
    var showLogo by remember { mutableStateOf(true) }
    var globalTitle by remember { mutableStateOf("ÖNCESİ / SONRASI") }
    var result by remember { mutableStateOf<Bitmap?>(null) }
    var status by remember { mutableStateOf("Hazır") }
    val slots = remember { mutableStateListOf<PhotoSlot12>() }

    LaunchedEffect(photoCount) {
        resizeSlots(slots, photoCount)
        if (selectedIndex >= photoCount) selectedIndex = photoCount - 1
        result = null
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && selectedIndex in slots.indices) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) { }

            try {
                val probe = decodeScaledBitmap12(context, uri, 1200)
                    ?: error("Bu fotoğraf biçimi açılamadı")
                probe.recycle()

                val detected = detectPhotoDate(context, uri)
                val old = slots[selectedIndex]
                slots[selectedIndex] = old.copy(
                    uri = uri,
                    dateText = detected,
                    scale = 1f,
                    offsetX = 0f,
                    offsetY = 0f,
                    rotation = 0f
                )
                status = "✓ Fotoğraf ${selectedIndex + 1} yüklendi ve görüntülendi"
                result = null
            } catch (e: Exception) {
                status = "Fotoğraf yükleme hatası: ${e.message ?: "dosya açılamadı"}"
            }
        }
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFFD71920),
            secondary = Color(0xFF111827),
            tertiary = Color(0xFF15803D),
            background = Color(0xFFF6F7F9),
            surface = Color.White
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BrandHeader12()
                Text("Öncesi Sonrası Pro", fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Text(
                    "2, 4, 6 veya 8 fotoğrafla karşılaştırma hazırla. Her fotoğrafı ayrı ayrı büyüt, küçült, kaydır, döndür; açıklama ve tarih ekle.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("1. Fotoğraf sayısı", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(2,4,6,8).forEach { count ->
                                FilterChip(
                                    selected = photoCount == count,
                                    onClick = { photoCount = count },
                                    label = { Text("${count}'li") }
                                )
                            }
                        }

                        Text("Düzen", fontWeight = FontWeight.SemiBold)
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Çiftler","Alt Alta","Kolaj").forEach { mode ->
                                FilterChip(
                                    selected = layoutMode == mode,
                                    onClick = { layoutMode = mode; result = null },
                                    label = { Text(mode) }
                                )
                            }
                        }
                    }
                }

                Card {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("2. Fotoğrafları ekle", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        PhotoGrid12(
                            context = context,
                            slots = slots,
                            selectedIndex = selectedIndex,
                            onSelect = { selectedIndex = it },
                            onPick = {
                                selectedIndex = it
                                picker.launch(arrayOf("image/*"))
                            }
                        )
                    }
                }

                if (selectedIndex in slots.indices) {
                    val selected = slots[selectedIndex]
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFB))) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "3. Fotoğraf ${selectedIndex + 1} ayarları • ${labelForIndex(selectedIndex)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Button(
                                onClick = { picker.launch(arrayOf("image/*")) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(if (selected.uri == null) "FOTOĞRAF SEÇ" else "FOTOĞRAFI DEĞİŞTİR") }

                            if (selected.uri != null) {
                                val editorBitmap = remember(selected.uri) {
                                    decodeScaledBitmap12(context, selected.uri, 1600)
                                }
                                val transformState = rememberTransformableState { zoomChange, panChange, rotationChange ->
                                    val cur = slots[selectedIndex]
                                    slots[selectedIndex] = cur.copy(
                                        scale = (cur.scale * zoomChange).coerceIn(1f, 4f),
                                        offsetX = (cur.offsetX + panChange.x / 450f).coerceIn(-1f, 1f),
                                        offsetY = (cur.offsetY + panChange.y / 450f).coerceIn(-1f, 1f),
                                        rotation = (cur.rotation + rotationChange).coerceIn(-180f, 180f)
                                    )
                                    result = null
                                }

                                if (editorBitmap != null) {
                                    Text(
                                        "Fotoğrafın üzerinde iki parmakla yakınlaştır • sürükle • döndür",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Box(
                                        Modifier.fillMaxWidth().height(360.dp)
                                            .clip(MaterialTheme.shapes.medium)
                                            .background(Color(0xFF111827)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            bitmap = editorBitmap.asImageBitmap(),
                                            contentDescription = "Düzenlenen fotoğraf",
                                            modifier = Modifier.fillMaxSize()
                                                .graphicsLayer(
                                                    scaleX = selected.scale,
                                                    scaleY = selected.scale,
                                                    translationX = selected.offsetX * 180f,
                                                    translationY = selected.offsetY * 180f,
                                                    rotationZ = selected.rotation
                                                )
                                                .transformable(transformState),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                } else {
                                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE4E6))) {
                                        Text(
                                            "Fotoğraf okunamadı. FOTOĞRAFI DEĞİŞTİR ile yeniden seç.",
                                            modifier = Modifier.padding(12.dp),
                                            color = Color(0xFFB91C1C),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Text("Yakınlaştırma: ${"%.1f".format(Locale.US, selected.scale)}x", fontSize = 12.sp)
                            Slider(
                                value = selected.scale,
                                onValueChange = {
                                    slots[selectedIndex] = selected.copy(scale = it)
                                    result = null
                                },
                                valueRange = 1f..3f
                            )

                            Text("Sağa / sola kaydır", fontSize = 12.sp)
                            Slider(
                                value = selected.offsetX,
                                onValueChange = {
                                    slots[selectedIndex] = slots[selectedIndex].copy(offsetX = it)
                                    result = null
                                },
                                valueRange = -1f..1f
                            )

                            Text("Yukarı / aşağı kaydır", fontSize = 12.sp)
                            Slider(
                                value = selected.offsetY,
                                onValueChange = {
                                    slots[selectedIndex] = slots[selectedIndex].copy(offsetY = it)
                                    result = null
                                },
                                valueRange = -1f..1f
                            )

                            Text("Döndürme: ${selected.rotation.toInt()}°", fontSize = 12.sp)
                            Slider(
                                value = selected.rotation,
                                onValueChange = {
                                    slots[selectedIndex] = slots[selectedIndex].copy(rotation = it)
                                    result = null
                                },
                                valueRange = -180f..180f
                            )

                            OutlinedTextField(
                                value = selected.caption,
                                onValueChange = {
                                    slots[selectedIndex] = slots[selectedIndex].copy(caption = it.take(100))
                                    result = null
                                },
                                label = { Text("Fotoğraf altı açıklama") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = selected.autoDate,
                                    onCheckedChange = {
                                        slots[selectedIndex] = slots[selectedIndex].copy(
                                            autoDate = it,
                                            dateText = if (it && selected.uri != null) detectPhotoDate(context, selected.uri) else selected.dateText
                                        )
                                        result = null
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Tarihi fotoğraftan otomatik algıla")
                            }

                            OutlinedTextField(
                                value = selected.dateText,
                                onValueChange = {
                                    slots[selectedIndex] = slots[selectedIndex].copy(dateText = it.take(20), autoDate = false)
                                    result = null
                                },
                                label = { Text("Tarih damgası") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        slots[selectedIndex] = selected.copy(
                                            scale = 1f, offsetX = 0f, offsetY = 0f, rotation = 0f
                                        )
                                        result = null
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text("KONUMU SIFIRLA") }
                                OutlinedButton(
                                    onClick = {
                                        slots[selectedIndex] = selected.copy(uri = null, caption = "", dateText = "")
                                        result = null
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text("FOTOĞRAFI SİL") }
                            }
                        }
                    }
                }

                Card {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("4. Başlık ve damgalar", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        OutlinedTextField(
                            value = globalTitle,
                            onValueChange = { globalTitle = it.take(50); result = null },
                            label = { Text("Üst başlık") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = showLogo, onCheckedChange = { showLogo = it; result = null })
                            Spacer(Modifier.width(8.dp))
                            Text("Toyota Boshoku Türkiye logosunu göster")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = showDate, onCheckedChange = { showDate = it; result = null })
                            Spacer(Modifier.width(8.dp))
                            Text("Fotoğraflarda tarih damgasını göster")
                        }
                    }
                }

                Button(
                    onClick = {
                        try {
                            if (slots.take(photoCount).any { it.uri == null }) {
                                status = "Önce ${photoCount} fotoğrafın tamamını ekle."
                            } else {
                                result?.recycle()
                                result = createComposite12(
                                    context = context,
                                    slots = slots.take(photoCount),
                                    layoutMode = layoutMode,
                                    globalTitle = globalTitle,
                                    showLogo = showLogo,
                                    showDate = showDate
                                )
                                status = "✓ Öncesi / Sonrası görseli hazır"
                            }
                        } catch (e: Exception) {
                            status = "Görsel oluşturulamadı: ${e.message ?: "hata"}"
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(58.dp)
                ) { Text("PROFESYONEL GÖRSELİ OLUŞTUR", fontWeight = FontWeight.Bold) }

                result?.let { bmp ->
                    Card {
                        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Sonuç Önizleme", fontWeight = FontWeight.Bold)
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Sonuç",
                                modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp, max = 650.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    Button(
                        onClick = {
                            try {
                                saveToGallery12(context, bmp)
                                status = "✓ Galeriye kaydedildi"
                            } catch (e: Exception) {
                                status = "Kaydedilemedi: ${e.message ?: "hata"}"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
                    ) { Text("GALERİYE KAYDET") }

                    Button(
                        onClick = {
                            try {
                                share12(context, bmp, whatsappOnly = true)
                                status = "✓ WhatsApp paylaşımı açıldı"
                            } catch (_: Exception) {
                                try {
                                    share12(context, bmp, whatsappOnly = false)
                                    status = "✓ Paylaşım ekranı açıldı"
                                } catch (e: Exception) {
                                    status = "Paylaşım açılamadı: ${e.message ?: "hata"}"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("WHATSAPP'TAN GÖNDER") }

                    OutlinedButton(
                        onClick = {
                            try {
                                share12(context, bmp, whatsappOnly = false)
                            } catch (e: Exception) {
                                status = "Paylaşım açılamadı: ${e.message ?: "hata"}"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("DİĞER UYGULAMALARLA PAYLAŞ") }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (status.startsWith("✓")) Color(0xFFF0FDF4) else Color(0xFFFFFBEB)
                    )
                ) {
                    Text(status, Modifier.padding(12.dp))
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun BrandHeader12() {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.toyota_boshoku_symbol),
            contentDescription = "Toyota Boshoku Türkiye logosu",
            modifier = Modifier.width(72.dp).height(42.dp),
            contentScale = ContentScale.Fit
        )
        Column {
            Text("TOYOTA BOSHOKU TÜRKİYE", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("Öncesi / Sonrası Görsel Hazırlama", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PhotoGrid12(
    context: Context,
    slots: List<PhotoSlot12>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onPick: (Int) -> Unit
) {
    val rows = slots.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { slot ->
                    Card(
                        modifier = Modifier.weight(1f).clickable { onSelect(slot.index) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedIndex == slot.index) Color(0xFFFFE8E8) else Color.White
                        )
                    ) {
                        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "${slot.index + 1}. ${labelForIndex(slot.index)}",
                                fontWeight = FontWeight.Bold,
                                color = if (labelForIndex(slot.index) == "ÖNCESİ") Color(0xFFD71920) else Color(0xFF166534)
                            )
                            if (slot.uri != null) {
                                val bmp = remember(slot.uri) { decodeScaledBitmap12(context, slot.uri, 900) }
                                if (bmp != null) {
                                    Box(
                                        Modifier.fillMaxWidth().height(180.dp).clip(MaterialTheme.shapes.small)
                                            .background(Color(0xFF111827)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize().graphicsLayer(
                                                scaleX = slot.scale,
                                                scaleY = slot.scale,
                                                translationX = slot.offsetX * 85f,
                                                translationY = slot.offsetY * 85f,
                                                rotationZ = slot.rotation
                                            ),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                } else {
                                    Box(
                                        Modifier.fillMaxWidth().height(180.dp)
                                            .background(Color(0xFFFFE4E6)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Fotoğraf açılamadı\nTekrar seç", color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(slot.dateText.ifBlank { "Tarih yok" }, fontSize = 10.sp)
                                Text(
                                    if (slot.caption.isBlank()) "Açıklama eklenebilir" else slot.caption,
                                    fontSize = 10.sp,
                                    maxLines = 2
                                )
                            } else {
                                Button(onClick = { onPick(slot.index) }, modifier = Modifier.fillMaxWidth().height(145.dp)) {
                                    Text("FOTOĞRAF EKLE")
                                }
                            }
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private fun resizeSlots(slots: SnapshotStateList<PhotoSlot12>, count: Int) {
    while (slots.size < count) slots.add(PhotoSlot12(index = slots.size))
    while (slots.size > count) slots.removeAt(slots.lastIndex)
    for (i in slots.indices) if (slots[i].index != i) slots[i] = slots[i].copy(index = i)
}

private fun labelForIndex(index: Int): String = if (index % 2 == 0) "ÖNCESİ" else "SONRASI"

private fun detectPhotoDate(context: Context, uri: Uri): String {
    val outFormat = SimpleDateFormat("dd.MM.yyyy", Locale("tr","TR"))
    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val exif = ExifInterface(input)
            val raw = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
            if (!raw.isNullOrBlank()) {
                val parser = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
                parser.parse(raw)?.let { return outFormat.format(it) }
            }
        }
    } catch (_: Exception) { }

    try {
        val projection = arrayOf(MediaStore.Images.Media.DATE_TAKEN)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                if (idx >= 0) {
                    val millis = cursor.getLong(idx)
                    if (millis > 0) return outFormat.format(Date(millis))
                }
            }
        }
    } catch (_: Exception) { }

    return outFormat.format(Date())
}

private fun decodeScaledBitmap12(context: Context, uri: Uri, maxSide: Int): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val w = info.size.width
                val h = info.size.height
                val longest = max(w, h).coerceAtLeast(1)
                if (longest > maxSide) {
                    val ratio = maxSide.toFloat() / longest.toFloat()
                    decoder.setTargetSize(
                        (w * ratio).toInt().coerceAtLeast(1),
                        (h * ratio).toInt().coerceAtLeast(1)
                    )
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            val longest = max(bounds.outWidth, bounds.outHeight)
            while (longest / sample > maxSide * 2) sample *= 2
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        }
    } catch (_: Exception) {
        try {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) {
            null
        }
    }
}

private fun createComposite12(
    context: Context,
    slots: List<PhotoSlot12>,
    layoutMode: String,
    globalTitle: String,
    showLogo: Boolean,
    showDate: Boolean
): Bitmap {
    val gap = 18
    val outer = 34
    val headerH = 150
    val infoH = 110

    val columns = when (layoutMode) {
        "Alt Alta" -> 1
        "Kolaj" -> if (slots.size >= 6) 2 else 2
        else -> 2
    }
    val cellW = if (columns == 1) 1500 else 850
    val photoH = when {
        layoutMode == "Kolaj" && slots.size >= 6 -> 620
        layoutMode == "Kolaj" -> 720
        columns == 1 -> 900
        else -> 780
    }
    val rows = (slots.size + columns - 1) / columns
    val width = outer * 2 + columns * cellW + (columns - 1) * gap
    val height = outer * 2 + headerH + rows * (photoH + infoH) + (rows - 1) * gap

    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    canvas.drawColor(android.graphics.Color.WHITE)

    val brandLogo = BitmapFactory.decodeResource(context.resources, R.drawable.toyota_boshoku_symbol)
    drawGlobalHeader12(canvas, width, globalTitle, showLogo, brandLogo)

    slots.forEachIndexed { idx, slot ->
        val row = idx / columns
        val col = idx % columns
        val left = outer + col * (cellW + gap)
        val top = outer + headerH + row * (photoH + infoH + gap)
        val rect = Rect(left, top, left + cellW, top + photoH)
        val bmp = slot.uri?.let { decodeScaledBitmap12(context, it, 2400) }
            ?: error("Fotoğraf ${idx + 1} okunamadı")
        drawAdjustedCover12(canvas, bmp, rect, slot.scale, slot.offsetX, slot.offsetY, slot.rotation)
        bmp.recycle()
        drawInfo12(canvas, left, top + photoH, cellW, infoH, slot, showDate, showLogo, brandLogo)
    }
    brandLogo?.recycle()

    return output
}

private fun drawGlobalHeader12(
    canvas: Canvas,
    width: Int,
    title: String,
    showLogo: Boolean,
    logo: Bitmap?
) {
    if (showLogo && logo != null) {
        val logoRect = Rect(45, 34, 145, 104)
        canvas.drawBitmap(logo, null, logoRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }.let { canvas.drawText("TOYOTA BOSHOKU TÜRKİYE", 165f, 82f, it) }
    }
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(17,24,39)
        textSize = 48f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.RIGHT
    }.let { canvas.drawText(title.ifBlank { "ÖNCESİ / SONRASI" }, width - 45f, 86f, it) }
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(229,231,235)
        strokeWidth = 3f
    }.let { canvas.drawLine(35f, 122f, width - 35f, 122f, it) }
}

private fun drawAdjustedCover12(
    canvas: Canvas,
    bitmap: Bitmap,
    dest: Rect,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    rotation: Float
) {
    canvas.save()
    canvas.clipRect(dest)
    val centerX = dest.exactCenterX()
    val centerY = dest.exactCenterY()
    canvas.rotate(rotation, centerX, centerY)

    val baseScale = max(dest.width().toFloat() / bitmap.width, dest.height().toFloat() / bitmap.height)
    val finalScale = baseScale * scale.coerceIn(1f, 3f)
    val drawW = bitmap.width * finalScale
    val drawH = bitmap.height * finalScale
    val maxShiftX = max(0f, (drawW - dest.width()) / 2f)
    val maxShiftY = max(0f, (drawH - dest.height()) / 2f)
    val cx = centerX + offsetX.coerceIn(-1f,1f) * maxShiftX
    val cy = centerY + offsetY.coerceIn(-1f,1f) * maxShiftY
    val dst = RectF(cx - drawW/2f, cy - drawH/2f, cx + drawW/2f, cy + drawH/2f)
    canvas.drawBitmap(bitmap, null, dst, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    canvas.restore()
}

private fun drawInfo12(
    canvas: Canvas,
    left: Int,
    top: Int,
    width: Int,
    height: Int,
    slot: PhotoSlot12,
    showDate: Boolean,
    showLogo: Boolean,
    logo: Bitmap?
) {
    var labelX = left + 14f
    if (showLogo && logo != null) {
        val logoRect = Rect(left + 12, top + 10, left + 68, top + 50)
        canvas.drawBitmap(logo, null, logoRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }.let { canvas.drawText("TOYOTA BOSHOKU TÜRKİYE", left + 76f, top + 36f, it) }
        labelX = left + 14f
    }

    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(17,24,39)
        textSize = 28f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }.let { canvas.drawText(labelForIndex(slot.index), labelX, top + 82f, it) }

    if (showDate) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(75,85,99)
            textSize = 24f
            textAlign = Paint.Align.RIGHT
        }.let { canvas.drawText(slot.dateText, left + width - 12f, top + 82f, it) }
    }

    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(55,65,81)
        textSize = 23f
    }.let {
        val text = slot.caption.ifBlank { " " }
        canvas.drawText(text.take(70), left + 150f, top + 82f, it)
    }
}

private fun saveToGallery12(context: Context, bitmap: Bitmap): Uri {
    val name = "oncesi_sonrasi_pro_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/OncesiSonrasi")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: error("Galeri dosyası oluşturulamadı")
    context.contentResolver.openOutputStream(uri)?.use {
        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 96, it)) error("JPEG oluşturulamadı")
    } ?: error("Dosya açılamadı")
    values.clear()
    values.put(MediaStore.Images.Media.IS_PENDING, 0)
    context.contentResolver.update(uri, values, null, null)
    return uri
}

private fun share12(context: Context, bitmap: Bitmap, whatsappOnly: Boolean) {
    val dir = File(context.cacheDir, "share").apply { mkdirs() }
    val file = File(dir, "oncesi_sonrasi_pro_${System.currentTimeMillis()}.jpg")
    FileOutputStream(file).use {
        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 96, it)) error("Paylaşım görseli oluşturulamadı")
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (whatsappOnly) setPackage("com.whatsapp")
    }
    context.startActivity(if (whatsappOnly) intent else Intent.createChooser(intent, "Fotoğrafı paylaş"))
}
