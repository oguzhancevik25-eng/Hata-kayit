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
        setContent { StableBeforeAfterApp() }
    }
}

@Composable
fun StableBeforeAfterApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var beforeUri by remember { mutableStateOf<Uri?>(null) }
    var afterUri by remember { mutableStateOf<Uri?>(null) }
    var result by remember { mutableStateOf<Bitmap?>(null) }
    var vertical by remember { mutableStateOf(false) }
    var beforeText by remember { mutableStateOf("ÖNCESİ") }
    var afterText by remember { mutableStateOf("SONRASI") }
    var status by remember { mutableStateOf("Hazır") }

    val beforePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        beforeUri = uri
        result = null
        status = if (uri != null) "Öncesi fotoğrafı seçildi" else "Seçim iptal edildi"
    }
    val afterPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        afterUri = uri
        result = null
        status = if (uri != null) "Sonrası fotoğrafı seçildi" else "Seçim iptal edildi"
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF0B4F7D),
            secondary = Color(0xFF0E7490),
            tertiary = Color(0xFF15803D),
            background = Color(0xFFF5F7FA)
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Öncesi Sonrası", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("v1.1 • İki fotoğraf seç, tek görsel oluştur ve WhatsApp'tan paylaş.")

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { beforePicker.launch("image/*") }, modifier = Modifier.weight(1f)) {
                        Text(if (beforeUri == null) "ÖNCESİ SEÇ" else "ÖNCESİ ✓")
                    }
                    Button(onClick = { afterPicker.launch("image/*") }, modifier = Modifier.weight(1f)) {
                        Text(if (afterUri == null) "SONRASI SEÇ" else "SONRASI ✓")
                    }
                }

                Card {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Düzen", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = !vertical, onClick = { vertical = false }, label = { Text("Yan yana") })
                            FilterChip(selected = vertical, onClick = { vertical = true }, label = { Text("Alt alta") })
                        }
                        OutlinedTextField(
                            value = beforeText,
                            onValueChange = { beforeText = it.take(30) },
                            label = { Text("Öncesi yazısı") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = afterText,
                            onValueChange = { afterText = it.take(30) },
                            label = { Text("Sonrası yazısı") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Button(
                    onClick = {
                        try {
                            val b = beforeUri?.let { decodeScaledBitmap(context, it, 1800) }
                            val a = afterUri?.let { decodeScaledBitmap(context, it, 1800) }
                            if (b == null || a == null) {
                                status = "İki fotoğrafı da seçmen gerekiyor."
                            } else {
                                result = if (vertical) {
                                    createVerticalStable(b, a, beforeText, afterText)
                                } else {
                                    createSideBySideStable(b, a, beforeText, afterText)
                                }
                                b.recycle()
                                a.recycle()
                                status = "✓ Görsel oluşturuldu"
                            }
                        } catch (e: Exception) {
                            status = "Görsel oluşturulamadı: ${e.message ?: "hata"}"
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("GÖRSELİ OLUŞTUR", fontWeight = FontWeight.Bold)
                }

                result?.let { bmp ->
                    Card {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Öncesi Sonrası",
                            modifier = Modifier.fillMaxWidth().heightIn(min = 250.dp, max = 520.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Button(
                        onClick = {
                            try {
                                saveToGalleryStable(context, bmp)
                                status = "✓ Galeriye kaydedildi"
                            } catch (e: Exception) {
                                status = "Kaydedilemedi: ${e.message ?: "hata"}"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) { Text("GALERİYE KAYDET") }

                    Button(
                        onClick = {
                            try {
                                shareStable(context, bmp)
                                status = "✓ Paylaşım ekranı açıldı"
                            } catch (e: Exception) {
                                status = "Paylaşım açılamadı: ${e.message ?: "hata"}"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("WHATSAPP / PAYLAŞ") }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (status.startsWith("✓")) Color(0xFFF0FDF4) else Color(0xFFFFFBEB)
                    )
                ) {
                    Text(status, modifier = Modifier.padding(12.dp))
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private fun decodeScaledBitmap(context: Context, uri: Uri, maxSide: Int): Bitmap? {
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
    return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
}

private fun createSideBySideStable(before: Bitmap, after: Bitmap, left: String, right: String): Bitmap {
    val photoW = 900
    val photoH = 1200
    val gap = 16
    val header = 120
    val out = Bitmap.createBitmap(photoW * 2 + gap, photoH + header, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    canvas.drawColor(android.graphics.Color.WHITE)
    drawCoverStable(canvas, before, Rect(0, header, photoW, header + photoH))
    drawCoverStable(canvas, after, Rect(photoW + gap, header, photoW * 2 + gap, header + photoH))
    drawTextStable(canvas, left, photoW / 2f, 78f)
    drawTextStable(canvas, right, photoW + gap + photoW / 2f, 78f)
    return out
}

private fun createVerticalStable(before: Bitmap, after: Bitmap, top: String, bottom: String): Bitmap {
    val width = 1500
    val photoH = 950
    val header = 105
    val gap = 14
    val out = Bitmap.createBitmap(width, header + photoH + gap + header + photoH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    canvas.drawColor(android.graphics.Color.WHITE)
    drawTextStable(canvas, top, width / 2f, 70f)
    drawCoverStable(canvas, before, Rect(0, header, width, header + photoH))
    val secondTop = header + photoH + gap
    drawTextStable(canvas, bottom, width / 2f, secondTop + 70f)
    drawCoverStable(canvas, after, Rect(0, secondTop + header, width, secondTop + header + photoH))
    return out
}

private fun drawCoverStable(canvas: Canvas, bitmap: Bitmap, dest: Rect) {
    val srcRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
    val dstRatio = dest.width().toFloat() / dest.height().toFloat()
    val src = if (srcRatio > dstRatio) {
        val newW = (bitmap.height * dstRatio).toInt().coerceAtLeast(1)
        val left = ((bitmap.width - newW) / 2).coerceAtLeast(0)
        Rect(left, 0, (left + newW).coerceAtMost(bitmap.width), bitmap.height)
    } else {
        val newH = (bitmap.width / dstRatio).toInt().coerceAtLeast(1)
        val top = ((bitmap.height - newH) / 2).coerceAtLeast(0)
        Rect(0, top, bitmap.width, (top + newH).coerceAtMost(bitmap.height))
    }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(bitmap, src, dest, paint)
}

private fun drawTextStable(canvas: Canvas, text: String, centerX: Float, baseline: Float) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(11, 79, 125)
        textAlign = Paint.Align.CENTER
        textSize = 54f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText(text.ifBlank { " " }, centerX, baseline, paint)
}

private fun saveToGalleryStable(context: Context, bitmap: Bitmap): Uri {
    val fileName = "oncesi_sonrasi_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/OncesiSonrasi")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: error("Galeri dosyası açılamadı")
    context.contentResolver.openOutputStream(uri)?.use {
        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)) error("JPEG oluşturulamadı")
    } ?: error("Dosya yazılamadı")
    values.clear()
    values.put(MediaStore.Images.Media.IS_PENDING, 0)
    context.contentResolver.update(uri, values, null, null)
    return uri
}

private fun shareStable(context: Context, bitmap: Bitmap) {
    val shareDir = File(context.cacheDir, "share").apply { mkdirs() }
    val file = File(shareDir, "oncesi_sonrasi_${System.currentTimeMillis()}.jpg")
    FileOutputStream(file).use {
        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)) error("Paylaşım resmi oluşturulamadı")
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Fotoğrafı paylaş"))
}
