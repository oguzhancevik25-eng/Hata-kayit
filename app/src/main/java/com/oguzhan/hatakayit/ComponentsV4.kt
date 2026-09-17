package com.oguzhan.hatakayit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

fun saveCameraPhoto4(context: Context, bitmap: Bitmap, prefix: String): String = try {
    val dir = File(context.filesDir, "operator_photos").apply { mkdirs() }
    val file = File(dir, "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
    file.absolutePath
} catch (_: Exception) { "" }

fun saveGalleryPhoto4(context: Context, uri: Uri, prefix: String): String = try {
    val dir = File(context.filesDir, "operator_photos").apply { mkdirs() }
    val file = File(dir, "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID()}.img")
    val input = context.contentResolver.openInputStream(uri)
    if (input == null) "" else {
        input.use { source -> file.outputStream().use { target -> source.copyTo(target) } }
        file.absolutePath
    }
} catch (_: Exception) { "" }

@Composable
fun LocalPhoto4(path: String, height: Dp) {
    var enlarged by remember { mutableStateOf(false) }
    val bitmap = remember(path) { BitmapFactory.decodeFile(path) }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(), contentDescription = "Fotoğraf",
            modifier = Modifier.fillMaxWidth().height(height).clickable { enlarged = true },
            contentScale = ContentScale.Crop
        )
        if (enlarged) {
            Dialog(onDismissRequest = { enlarged = false }) {
                Card(colors = CardDefaults.cardColors(containerColor = Color.Black)) {
                    Column(Modifier.padding(8.dp)) {
                        Image(
                            bitmap = bitmap.asImageBitmap(), contentDescription = "Büyük fotoğraf",
                            modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp, max = 620.dp), contentScale = ContentScale.Fit
                        )
                        TextButton({ enlarged = false }, Modifier.align(Alignment.End)) { Text("KAPAT", color = Color.White) }
                    }
                }
            }
        }
    } else {
        Box(Modifier.fillMaxWidth().height(height).background(Color(0xFFE5E7EB)), contentAlignment = Alignment.Center) {
            Text("Fotoğraf açılamadı", color = Color(0xFF6B7280))
        }
    }
}

@Composable
fun <T> Selector4(label: String, options: List<T>, selected: T, display: (T) -> String, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(display(selected), Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
            Text("▼")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(display(option)) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}

@Composable
fun Metric4(title: String, value: String, modifier: Modifier, color: Color) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color)) {
        Column(Modifier.padding(12.dp)) {
            Text(title, fontSize = 11.sp, color = Color(0xFF4B5563))
            Text(value, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GradeBadge4(grade: String) {
    val color = when (grade) { "A" -> Color(0xFF15803D); "B" -> Color(0xFF0369A1); "C" -> Color(0xFFB45309); else -> Color(0xFFB91C1C) }
    Surface(color = color, shape = RoundedCornerShape(9.dp)) {
        Text(grade, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
    }
}

@Composable
fun ScoreBar4(title: String, value: Double, maximum: Double) {
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text(title, Modifier.weight(1f)); Text("${f14(value)} / ${i4(maximum)}", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(progress = { (value / maximum).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
    }
}

fun f14(value: Double): String = String.format(Locale("tr", "TR"), "%.1f", value)
fun i4(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else f14(value)
fun unit4(type: String): String = when (type) { "Mesai" -> "saat"; "Yıllık İzin", "Günlük İzin", "Rapor", "Devamsızlık" -> "gün"; else -> "adet" }
fun date4(timestamp: Long): String = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR")).format(Date(timestamp))
fun scoreColor4(score: Double): Color = when { score >= 90 -> Color(0xFF15803D); score >= 80 -> Color(0xFF0369A1); score >= 70 -> Color(0xFFB45309); else -> Color(0xFFB91C1C) }
