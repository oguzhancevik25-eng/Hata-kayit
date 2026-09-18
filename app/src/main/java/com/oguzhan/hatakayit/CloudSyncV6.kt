package com.oguzhan.hatakayit

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class CloudSnapshot4(
    val operators: List<Operator4>,
    val records: List<Record4>,
    val refs: Map<String, String>,
    val machines: List<String>,
    val parts: List<Part4>,
    val scoring: ScoringSettings4
)

object CloudSync4 {
    private const val PREFS = "operator_takip_prefs"
    private const val CODE_KEY = "cloud_sync_code_v1_6"

    private const val BASE_URL = "https://etdhqgrxhicqjkfsoxue.supabase.co"
    private const val API_KEY = "sb_publishable_ECvS4jam6myE7gAeSF7a0Q_viLhocWB"

    fun loadCode(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CODE_KEY, "") ?: ""

    fun saveCode(context: Context, code: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(CODE_KEY, code.trim()).apply()
    }

    fun push(context: Context, code: String, snapshot: CloudSnapshot4): String {
        val body = JSONObject().apply {
            put("p_code", code.trim())
            put("p_state", snapshotToJson(context, snapshot))
        }
        request("operator_sync_push", body)
        return "✓ Buluta gönderildi"
    }

    fun pull(context: Context, code: String): CloudSnapshot4? {
        val body = JSONObject().apply { put("p_code", code.trim()) }
        val raw = request("operator_sync_pull", body)
        val outer = JSONObject(raw)
        if (outer.isNull("state")) return null
        val state = outer.optJSONObject("state") ?: return null
        return jsonToSnapshot(context, state)
    }

    private fun request(function: String, body: JSONObject): String {
        val conn = (URL("$BASE_URL/rest/v1/rpc/$function").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12000
            readTimeout = 20000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("apikey", API_KEY)
        }
        return try {
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val msg = try { JSONObject(text).optString("message", text) } catch (_: Exception) { text }
                throw IllegalStateException(
                    when {
                        msg.contains("invalid sync code", ignoreCase = true) -> "Senkron kodu yanlış"
                        msg.isBlank() -> "Bulut bağlantı hatası ($code)"
                        else -> msg
                    }
                )
            }
            text
        } finally {
            conn.disconnect()
        }
    }

    private fun snapshotToJson(context: Context, s: CloudSnapshot4): JSONObject = JSONObject().apply {
        put("schemaVersion", 1)
        put("operators", JSONArray().apply {
            s.operators.forEach { op ->
                put(JSONObject().apply {
                    put("sicil", op.sicil)
                    put("name", op.name)
                    put("active", op.active)
                })
            }
        })
        put("records", JSONArray().apply {
            s.records.forEach { r ->
                put(JSONObject().apply {
                    put("id", r.id)
                    put("timestamp", r.timestamp)
                    put("operatorSicil", r.operatorSicil)
                    put("type", r.type)
                    put("defect", r.defect)
                    put("amount", r.amount)
                    put("machine", r.machine)
                    put("part", r.part)
                    put("note", r.note)
                    put("photoData", fileToData(r.photoPath))
                })
            }
        })
        put("refs", JSONObject().apply {
            s.refs.forEach { (defect, path) -> put(defect, fileToData(path)) }
        })
        put("machines", JSONArray().apply { s.machines.forEach { put(it) } })
        put("parts", JSONArray().apply {
            s.parts.forEach { p ->
                put(JSONObject().apply {
                    put("name", p.name)
                    put("machine", p.machine)
                })
            }
        })
        put("scoring", scoringToJson(s.scoring))
    }

    private fun jsonToSnapshot(context: Context, o: JSONObject): CloudSnapshot4 {
        val operators = mutableListOf<Operator4>()
        val opA = o.optJSONArray("operators") ?: JSONArray()
        for (i in 0 until opA.length()) {
            val x = opA.getJSONObject(i)
            operators += Operator4(x.optString("sicil"), x.optString("name"), x.optBoolean("active", true))
        }

        val records = mutableListOf<Record4>()
        val recA = o.optJSONArray("records") ?: JSONArray()
        for (i in 0 until recA.length()) {
            val x = recA.getJSONObject(i)
            val photoData = x.optString("photoData").ifBlank { x.optString("photo") }
            records += Record4(
                id = x.optLong("id"),
                timestamp = x.optLong("timestamp"),
                operatorSicil = x.optString("operatorSicil"),
                type = x.optString("type"),
                defect = x.optString("defect"),
                amount = x.optDouble("amount", 1.0),
                machine = x.optString("machine"),
                part = x.optString("part"),
                note = x.optString("note"),
                photoPath = dataToFile(context, photoData, "record")
            )
        }

        val refs = mutableMapOf<String, String>()
        val refO = o.optJSONObject("refs") ?: JSONObject()
        refO.keys().forEach { key ->
            val value = refO.optString(key)
            dataToFile(context, value, "ref").takeIf { it.isNotBlank() }?.let { refs[key] = it }
        }

        val machines = mutableListOf<String>()
        val machineA = o.optJSONArray("machines") ?: JSONArray()
        for (i in 0 until machineA.length()) machines += machineA.optString(i)

        val parts = mutableListOf<Part4>()
        val partA = o.optJSONArray("parts") ?: JSONArray()
        for (i in 0 until partA.length()) {
            val x = partA.getJSONObject(i)
            parts += Part4(x.optString("name"), x.optString("machine"))
        }

        val scoring = jsonToScoring(o.optJSONObject("scoring"))

        return CloudSnapshot4(
            operators = operators.ifEmpty { defaultOperators4 },
            records = records,
            refs = refs,
            machines = machines.ifEmpty { defaultMachines4 },
            parts = parts.ifEmpty { defaultParts4 },
            scoring = scoring
        )
    }

    private fun scoringToJson(s: ScoringSettings4) = JSONObject().apply {
        put("qualityMax", s.qualityMax)
        put("kyMax", s.kyMax)
        put("kaizenMax", s.kaizenMax)
        put("attendanceMax", s.attendanceMax)
        put("overtimeMax", s.overtimeMax)
        put("qualityStart", s.qualityStart)
        put("escapedPenalty", s.escapedPenalty)
        put("caughtBonus", s.caughtBonus)
        put("caughtBonusCap", s.caughtBonusCap)
        put("kyMonthlyTarget", s.kyMonthlyTarget)
        put("kaizenMonthlyTarget", s.kaizenMonthlyTarget)
        put("overtimeMonthlyTarget", s.overtimeMonthlyTarget)
        put("absencePenaltyPerDay", s.absencePenaltyPerDay)
        put("gradeA", s.gradeA)
        put("gradeB", s.gradeB)
        put("gradeC", s.gradeC)
    }

    private fun jsonToScoring(o: JSONObject?): ScoringSettings4 {
        if (o == null) return defaultScoringSettings4
        return ScoringSettings4(
            qualityMax = o.optDouble("qualityMax", 40.0),
            kyMax = o.optDouble("kyMax", 15.0),
            kaizenMax = o.optDouble("kaizenMax", 15.0),
            attendanceMax = o.optDouble("attendanceMax", 20.0),
            overtimeMax = o.optDouble("overtimeMax", 10.0),
            qualityStart = o.optDouble("qualityStart", 35.0),
            escapedPenalty = o.optDouble("escapedPenalty", 4.0),
            caughtBonus = o.optDouble("caughtBonus", 0.5),
            caughtBonusCap = o.optDouble("caughtBonusCap", 5.0),
            kyMonthlyTarget = o.optDouble("kyMonthlyTarget", 6.0),
            kaizenMonthlyTarget = o.optDouble("kaizenMonthlyTarget", 1.0),
            overtimeMonthlyTarget = o.optDouble("overtimeMonthlyTarget", 10.0),
            absencePenaltyPerDay = o.optDouble("absencePenaltyPerDay", 5.0),
            gradeA = o.optDouble("gradeA", 90.0),
            gradeB = o.optDouble("gradeB", 80.0),
            gradeC = o.optDouble("gradeC", 70.0)
        )
    }

    private fun fileToData(path: String): String {
        if (path.isBlank()) return ""
        return try {
            val f = File(path)
            if (!f.exists()) return ""
            "data:image/jpeg;base64," + Base64.encodeToString(f.readBytes(), Base64.NO_WRAP)
        } catch (_: Exception) { "" }
    }

    private fun dataToFile(context: Context, data: String, prefix: String): String {
        if (data.isBlank() || !data.contains("base64,")) return ""
        return try {
            val b64 = data.substringAfter("base64,")
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            val hash = MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it) }
            val dir = File(context.filesDir, "operator_photos").apply { mkdirs() }
            val file = File(dir, "cloud_${prefix}_${hash.take(24)}.jpg")
            if (!file.exists()) file.writeBytes(bytes)
            file.absolutePath
        } catch (_: Exception) { "" }
    }
}
