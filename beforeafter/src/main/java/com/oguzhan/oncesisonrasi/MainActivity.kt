package com.oguzhan.oncesisonrasi

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import java.util.Date
import java.util.Locale
import kotlin.math.max

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BeforeAfterApp() }
    }
}

@Composable
fun BeforeAfterApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var beforeUri by remember { mutableStateOf<Uri?>(null) }
    var afterUri by remember { mutableStateOf<Uri?>(null) }
    var result by remember { mutableStateOf<Bitmap?>(null) }
    var layout by remember { mutableStateOf("Yan Yana") }
    var titleBefore by remember { mutableStateOf("ÖNCESİ") }
    var titleAfter by remember { mutableStateOf("SONRASI") }
    var status by remember { mutableStateOf("") }

    val beforePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        beforeUri = it
        result = null
    }
    val afterPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        afterUri = it
        result = null
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF0B4F7D),
            secondary = Color(0xFF0E7490),
            tertiary = Color(0xFF15803D),
            background = Color(0xFFF5F7FA)
        )
    ) {
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Öncesi Sonrası", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("İki fotoğraf seç, tek görsel oluştur, telefona kaydet veya WhatsApp'tan paylaş.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button({ beforePicker.launch("image/*") }, modifier = Modifier.weight(1f)) { Text("ÖNCESİ SEÇ") }
                Button({ afterPicker.launch("image/*") }, modifier = Modifier.weight(1f)) { Text("SONRASI SEÇ") }
            }

            if (beforeUri != null || afterUri != null) {
                Row(Modifier.fillMaxWidth().height(170.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UriPreview(context, beforeUri, "Öncesi", Modifier.weight(1f))
                    UriPreview(context, afterUri, "Sonrası", Modifier.weight(1f))
                }
            }

            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Düzen", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = layout == "Yan Yana", onClick = { layout = "Yan Yana" }, label = { Text("Yan Yana") })
                        FilterChip(selected = layout == "Alt Alta", onClick = { layout = "Alt Alta" }, label = { Text("Alt Alta") })
                    }
                    OutlinedTextField(titleBefore, { titleBefore = it }, label = { Text("Öncesi yazısı") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(titleAfter, { titleAfter = it }, label = { Text("Sonrası yazısı") }, modifier = Modifier.fillMaxWidth())
                }
            }

            Button(
                onClick = {
                    val b = beforeUri?.let { loadBitmap(context, it) }
                    val a = afterUri?.let { loadBitmap(context, it) }
                    if (b == null || a == null) {
                        status = "Önce iki fotoğrafı da seç."
                    } else {
                        result = if (layout == "Yan Yana") {
                            createSideBySide(b, a, titleBefore, titleAfter)
                        } else {
                            createVertical(b, a, titleBefore, titleAfter)
                        }
                        status = "✓ Görsel hazır."
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) { Text("GÖRSELİ OLUŞTUR", fontWeight = FontWeight.Bold) }

            result?.let { bmp ->
                Card {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Öncesi Sonrası Sonuç",
                        modifier = Modifier.fillMaxWidth().heightIn(min = 260.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            try {
                                saveToGallery(context, bmp)
                                status = "✓ Galeriye kaydedildi."
                            } catch (e: Exception) {
                                status = "Kaydedilemedi: ${e.message}"
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) { Text("GALERİYE KAYDET") }

                    Button(
                        onClick = {
                            try {
                                shareImage(context, bmp)
                                status = "✓ Paylaşım ekranı açıldı."
                            } catch (e: Exception) {
                                status = "Paylaşım açılamadı: ${e.message}"
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("WHATSAPP / PAYLAŞ") }
                }
            }

            if (status.isNotBlank()) {
                Text(status, color = if (status.startsWith("✓")) Color(0xFF15803D) else Color(0xFFB45309), fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun UriPreview(context: Context, uri: Uri?, label: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.fillMaxSize().padding(8.dp)) {
            Text(label, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            val bmp = remember(uri) { uri?.let { loadBitmap(context, it) } }
            if (bmp != null) {
                Image(bitmap = bmp.asImageBitmap(), contentDescription = label, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize().background(Color(0xFFE5E7EB)))
            }
        }
    }
}

private fun loadBitmap(context: Context, uri: Uri): Bitmap? = try {
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
} catch (_: Exception) { null }

private fun createSideBySide(before: Bitmap, after: Bitmap, leftText: String, rightText: String): Bitmap {
    val targetH = 1400
    val gap = 12
    val header = 120
    val halfW = 1000
    val out = Bitmap.createBitmap(halfW * 2 + gap, targetH + header, Bitmap.Config.ARGB_8888)
    val c = Canvas(out)
    c.drawColor(Color.WHITE.hashCode())
    drawCover(c, before, Rect(0, header, halfW, header + targetH))
    drawCover(c, after, Rect(halfW + gap, header, halfW * 2 + gap, header + targetH))
    drawLabel(c, leftText, halfW / 2f, 76f)
    drawLabel(c, rightText, halfW + gap + halfW / 2f, 76f)
    return out
}

private fun createVertical(before: Bitmap, after: Bitmap, topText: String, bottomText: String): Bitmap {
    val targetW = 1600
    val photoH = 1050
    val header = 110
    val gap = 12
    val out = Bitmap.createBitmap(targetW, header + photoH + gap + header + photoH, Bitmap.Config.ARGB_8888)
    val c = Canvas(out)
    c.drawColor(Color.WHITE.hashCode())
    drawLabel(c, topText, targetW / 2f, 72f)
    drawCover(c, before, Rect(0, header, targetW, header + photoH))
    val secondHeaderTop = header + photoH + gap
    drawLabel(c, bottomText, targetW / 2f, secondHeaderTop + 72f)
    drawCover(c, after, Rect(0, secondHeaderTop + header, targetW, secondHeaderTop + header + photoH))
    return out
}

private fun drawCover(canvas: Canvas, bitmap: Bitmap, dest: Rect) {
    val srcRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
    val dstRatio = dest.width().toFloat() / dest.height().toFloat()
    val src = if (srcRatio > dstRatio) {
        val newW = (bitmap.height * dstRatio).toInt()
        val left = (bitmap.width - newW) / 2
        Rect(left, 0, left + newW, bitmap.height)
    } else {
        val newH = (bitmap.width / dstRatio).toInt()
        val top = (bitmap.height - newH) / 2
        Rect(0, top, bitmap.width, top + newH)
    }
    canvas.drawBitmap(bitmap, src, dest, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
}

private fun drawLabel(canvas: Canvas, text: String, centerX: Float, baselineY: Float) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(11, 79, 125)
        textSize = 54f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(text.ifBlank { " " }, centerX, baselineY, paint)
}

private fun saveToGallery(context: Context, bitmap: Bitmap): Uri {
    val name = "oncesi_sonrasi_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/OncesiSonrasi")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: error("Galeri dosyası oluşturulamadı")
    context.contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        ?: error("Dosya açılamadı")
    values.clear()
    values.put(MediaStore.Images.Media.IS_PENDING, 0)
    context.contentResolver.update(uri, values, null, null)
    return uri
}

private fun shareImage(context: Context, bitmap: Bitmap) {
    val dir = File(context.cacheDir, "share").apply { mkdirs() }
    val file = File(dir, "oncesi_sonrasi.jpg")
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val whatsapp = Intent(send).apply { setPackage("com.whatsapp") }
    try {
        context.startActivity(whatsapp)
    } catch (_: Exception) {
        context.startActivity(Intent.createChooser(send, "Fotoğrafı paylaş"))
    }
}
