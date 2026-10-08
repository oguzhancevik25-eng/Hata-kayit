package com.oguzhan.oncesisonrasi

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BeforeAfterApp() }
    }
}

enum class LayoutMode { SIDE_BY_SIDE, TOP_BOTTOM }

@Composable
fun BeforeAfterApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var beforeUri by remember { mutableStateOf<Uri?>(null) }
    var afterUri by remember { mutableStateOf<Uri?>(null) }
    var beforeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var afterBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var outputBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var beforeLabel by remember { mutableStateOf("ÖNCESİ") }
    var afterLabel by remember { mutableStateOf("SONRASI") }
    var title by remember { mutableStateOf("") }
    var layoutMode by remember { mutableStateOf(LayoutMode.SIDE_BY_SIDE) }
    var message by remember { mutableStateOf("") }

    val beforePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        beforeUri = uri
        beforeBitmap = uri?.let { loadBitmap(context, it) }
        outputBitmap = null
    }
    val afterPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        afterUri = uri
        afterBitmap = uri?.let { loadBitmap(context, it) }
        outputBitmap = null
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF0B4F7D),
            secondary = Color(0xFF0E7490),
            tertiary = Color(0xFF15803D),
            background = Color(0xFFF5F7FA)
        )
    ) {
        Scaffold(
            topBar = {
                Surface(color = MaterialTheme.colorScheme.primary) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Öncesi Sonrası", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        Text("Fotoğrafları birleştir • kaydet • WhatsApp'ta paylaş", color = Color.White.copy(alpha = .85f), fontSize = 12.sp)
                    }
                }
            }
        ) { pad ->
            Column(
                Modifier
                    .padding(pad)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("1. Fotoğrafları seç", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PhotoPickCard(
                                Modifier.weight(1f),
                                "Öncesi",
                                beforeBitmap,
                                { beforePicker.launch("image/*") }
                            )
                            PhotoPickCard(
                                Modifier.weight(1f),
                                "Sonrası",
                                afterBitmap,
                                { afterPicker.launch("image/*") }
                            )
                        }
                    }
                }

                Card {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("2. Görünümü ayarla", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Başlık (isteğe bağlı)") },
                            singleLine = true
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = beforeLabel,
                                onValueChange = { beforeLabel = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("Sol / Üst yazı") },
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = afterLabel,
                                onValueChange = { afterLabel = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("Sağ / Alt yazı") },
                                singleLine = true
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = layoutMode == LayoutMode.SIDE_BY_SIDE,
                                onClick = { layoutMode = LayoutMode.SIDE_BY_SIDE; outputBitmap = null },
                                label = { Text("Yan yana") }
                            )
                            FilterChip(
                                selected = layoutMode == LayoutMode.TOP_BOTTOM,
                                onClick = { layoutMode = LayoutMode.TOP_BOTTOM; outputBitmap = null },
                                label = { Text("Alt alta") }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        val b = beforeBitmap
                        val a = afterBitmap
                        if (b == null || a == null) {
                            message = "Önce iki fotoğrafı da seç."
                        } else {
                            outputBitmap = createBeforeAfter(
                                before = b,
                                after = a,
                                beforeLabel = beforeLabel.ifBlank { "ÖNCESİ" },
                                afterLabel = afterLabel.ifBlank { "SONRASI" },
                                title = title,
                                mode = layoutMode
                            )
                            message = "✓ Görsel hazır."
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    Text("GÖRSELİ OLUŞTUR", fontWeight = FontWeight.Bold)
                }

                outputBitmap?.let { result ->
                    Card {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Önizleme", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Image(
                                bitmap = result.asImageBitmap(),
                                contentDescription = "Öncesi sonrası çıktı",
                                modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 520.dp),
                                contentScale = ContentScale.Fit
                            )

                            Button(
                                onClick = {
                                    val uri = saveToGallery(context, result)
                                    message = if (uri != null) "✓ Galeriye kaydedildi." else "Kaydetme başarısız."
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
                            ) {
                                Text("GALERİYE KAYDET")
                            }

                            Button(
                                onClick = {
                                    val uri = saveForShare(context, result)
                                    if (uri != null) {
                                        shareToWhatsApp(context, uri)
                                        message = "✓ WhatsApp paylaşımı açıldı."
                                    } else {
                                        message = "Paylaşım dosyası oluşturulamadı."
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                            ) {
                                Text("WHATSAPP'TA PAYLAŞ", color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    beforeUri = null
                                    afterUri = null
                                    beforeBitmap = null
                                    afterBitmap = null
                                    outputBitmap = null
                                    title = ""
                                    message = ""
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("YENİ ÇALIŞMA")
                            }
                        }
                    }
                }

                if (message.isNotBlank()) {
                    Text(
                        message,
                        color = if (message.startsWith("✓")) Color(0xFF15803D) else Color(0xFFB45309),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Nasıl kullanılır?", fontWeight = FontWeight.Bold)
                        Text("Öncesi fotoğrafını seç → Sonrası fotoğrafını seç → Görseli Oluştur → WhatsApp'ta Paylaş.", fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun PhotoPickCard(
    modifier: Modifier,
    label: String,
    bitmap: Bitmap?,
    onPick: () -> Unit
) {
    Card(modifier = modifier) {
        Column(
            Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .background(Color(0xFFE8EEF4), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap == null) {
                    Text("$label fotoğrafı\nseçilmedi", color = Color(0xFF687386))
                } else {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = label,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Button(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
                Text(if (bitmap == null) "$label SEÇ" else "$label DEĞİŞTİR")
            }
        }
    }
}

private fun loadBitmap(context: Context, uri: Uri): Bitmap? = try {
    if (Build.VERSION.SDK_INT >= 28) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }
} catch (_: Exception) {
    null
}

private fun createBeforeAfter(
    before: Bitmap,
    after: Bitmap,
    beforeLabel: String,
    afterLabel: String,
    title: String,
    mode: LayoutMode
): Bitmap {
    val target = if (mode == LayoutMode.SIDE_BY_SIDE) {
        createSideBySide(before, after, beforeLabel, afterLabel, title)
    } else {
        createTopBottom(before, after, beforeLabel, afterLabel, title)
    }
    return target
}

private fun createSideBySide(
    before: Bitmap,
    after: Bitmap,
    beforeLabel: String,
    afterLabel: String,
    title: String
): Bitmap {
    val width = 1600
    val titleH = if (title.isBlank()) 0 else 120
    val labelH = 95
    val imageH = 1000
    val height = titleH + labelH + imageH
    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    canvas.drawColor(android.graphics.Color.WHITE)

    var y = 0
    if (title.isNotBlank()) {
        drawCenteredText(canvas, title, width / 2f, 72f, 52f, android.graphics.Color.rgb(11,79,125))
        y += titleH
    }

    drawLabelBlock(canvas, 0, y, width / 2, labelH, beforeLabel, android.graphics.Color.rgb(11,79,125))
    drawLabelBlock(canvas, width / 2, y, width / 2, labelH, afterLabel, android.graphics.Color.rgb(21,128,61))
    y += labelH

    drawCover(canvas, before, Rect(0, y, width / 2, y + imageH))
    drawCover(canvas, after, Rect(width / 2, y, width, y + imageH))

    val p = Paint().apply { color = android.graphics.Color.WHITE; strokeWidth = 8f }
    canvas.drawLine(width / 2f, y.toFloat(), width / 2f, height.toFloat(), p)
    return result
}

private fun createTopBottom(
    before: Bitmap,
    after: Bitmap,
    beforeLabel: String,
    afterLabel: String,
    title: String
): Bitmap {
    val width = 1200
    val titleH = if (title.isBlank()) 0 else 120
    val labelH = 90
    val imageH = 760
    val height = titleH + (labelH + imageH) * 2
    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    canvas.drawColor(android.graphics.Color.WHITE)

    var y = 0
    if (title.isNotBlank()) {
        drawCenteredText(canvas, title, width / 2f, 72f, 48f, android.graphics.Color.rgb(11,79,125))
        y += titleH
    }

    drawLabelBlock(canvas, 0, y, width, labelH, beforeLabel, android.graphics.Color.rgb(11,79,125))
    y += labelH
    drawCover(canvas, before, Rect(0, y, width, y + imageH))
    y += imageH

    drawLabelBlock(canvas, 0, y, width, labelH, afterLabel, android.graphics.Color.rgb(21,128,61))
    y += labelH
    drawCover(canvas, after, Rect(0, y, width, y + imageH))
    return result
}

private fun drawLabelBlock(
    canvas: Canvas,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    text: String,
    color: Int
) {
    val bg = Paint().apply { this.color = color }
    canvas.drawRect(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat(), bg)
    drawCenteredText(
        canvas,
        text,
        x + width / 2f,
        y + height / 2f + 18f,
        42f,
        android.graphics.Color.WHITE
    )
}

private fun drawCenteredText(canvas: Canvas, text: String, x: Float, y: Float, size: Float, color: Int) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(text, x, y, paint)
}

private fun drawCover(canvas: Canvas, bitmap: Bitmap, dst: Rect) {
    val srcRatio = bitmap.width.toFloat() / bitmap.height
    val dstRatio = dst.width().toFloat() / dst.height()

    val src = if (srcRatio > dstRatio) {
        val neededWidth = (bitmap.height * dstRatio).toInt()
        val left = (bitmap.width - neededWidth) / 2
        Rect(left, 0, left + neededWidth, bitmap.height)
    } else {
        val neededHeight = (bitmap.width / dstRatio).toInt()
        val top = (bitmap.height - neededHeight) / 2
        Rect(0, top, bitmap.width, top + neededHeight)
    }
    canvas.drawBitmap(bitmap, src, dst, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
}

private fun saveToGallery(context: Context, bitmap: Bitmap): Uri? {\n    return try {
    val name = "Oncesi_Sonrasi_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/OncesiSonrasi")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
    resolver.openOutputStream(uri)?.use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 94, out)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }
    uri
} catch (_: Exception) {
    null
}

private fun saveForShare(context: Context, bitmap: Bitmap): Uri? = try {
    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
    val file = File(dir, "oncesi_sonrasi.jpg")
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 94, it) }
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
} catch (_: Exception) {
    null
}

private fun shareToWhatsApp(context: Context, uri: Uri) {
    val direct = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        setPackage("com.whatsapp")
    }
    try {
        context.startActivity(direct)
    } catch (_: Exception) {
        val fallback = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(fallback, "Görseli paylaş"))
        Toast.makeText(context, "WhatsApp bulunamadı; paylaşım menüsü açıldı.", Toast.LENGTH_SHORT).show()
    }
}
