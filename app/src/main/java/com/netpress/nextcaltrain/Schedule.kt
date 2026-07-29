package com.netpress.nextcaltrain

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URI

class ScheduleError(message: String) : Exception(message)

class ScheduleHttpResult(val statusCode: Int, val body: String)

// Test seam matching huck's ScanHttpClient interface -- lets ScheduleSpec fake server responses
// (status codes, bodies) without a real network call. JdkHttpScheduleHttpClient below is the real
// implementation. This briefly used java.net.http.HttpClient to match huck's own
// JdkHttpScanHttpClient (a desktop JVM app), but that class doesn't exist on Android's runtime at
// all -- confirmed via a real NoClassDefFoundError on-device (java.net.http is a desktop/server
// JDK API, never shipped as part of Android's libcore, at any API level). Reverted to
// HttpURLConnection, wrapped behind this same interface so the test seam is unaffected.
interface ScheduleHttpClient {
    fun get(url: URI): ScheduleHttpResult
}

class JdkHttpScheduleHttpClient(
    private val connectTimeoutMs: Int = CONNECT_TIMEOUT_MS,
    private val readTimeoutMs: Int = READ_TIMEOUT_MS,
) : ScheduleHttpClient {
    override fun get(url: URI): ScheduleHttpResult {
        val connection = url.toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = connectTimeoutMs
        connection.readTimeout = readTimeoutMs
        connection.requestMethod = "GET"
        return try {
            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
            ScheduleHttpResult(statusCode, body)
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 30_000
    }
}

// Holds the parsed schedule.json data; uses Android's built-in JSONObject, no third-party JSON lib.
data class Schedule(
    val specialDates: Map<String, Int>,
    val northStops: List<String>,
    val southStops: List<String>,
    val northWeekday: Map<String, List<Int?>>,
    val northWeekend: Map<String, List<Int?>>,
    val northHoliday: Map<String, List<Int?>>,
    val southWeekday: Map<String, List<Int?>>,
    val southWeekend: Map<String, List<Int?>>,
    val southHoliday: Map<String, List<Int?>>,
    val scheduleDate: Long?,
) {
    companion object {
        // Defaults to prod; override via `scheduleUrl=` in local.properties (see build.gradle.kts).
        private val REMOTE_URL = BuildConfig.SCHEDULE_URL

        // internal (not private) so tests can check the real cache filename directly.
        internal const val CACHE_FILE = "schedule.json"

        // internal (not private) so tests can key a fake SharedPreferences the same way as production.
        internal const val PREFS_NAME = "nextcaltrain"
        internal const val KEY_LAST_FETCH_MS = "lastFetchMs"

        fun loadCached(context: Context): Schedule? {
            val file = File(context.filesDir, CACHE_FILE)
            if (!file.exists()) return null
            return try {
                val json = JSONObject(file.readText())
                val schedule = fromJson(json)
                if (schedule.isValid) schedule else null
            } catch (e: Exception) {
                null
            }
        }

        // True if the last successful fetch landed on today's schedule-day (2am boundary); skips redundant fetches.
        fun fetchedToday(context: Context, nowMillis: Long = System.currentTimeMillis()): Boolean {
            val last = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(KEY_LAST_FETCH_MS, -1L)
            if (last < 0) return false
            return GoodTimes.scheduleDateFor(last) == GoodTimes.scheduleDateFor(nowMillis)
        }

        private fun markFetched(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_FETCH_MS, System.currentTimeMillis())
                .apply()
        }

        suspend fun fetchFromNetwork(
            context: Context,
            httpClient: ScheduleHttpClient = JdkHttpScheduleHttpClient(),
        ): Schedule = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val result = httpClient.get(URI(REMOTE_URL))
            if (result.statusCode != 200) {
                throw ScheduleError("The server responded with status ${result.statusCode}.")
            }
            val json = JSONObject(result.body)
            val schedule = fromJson(json)
            if (!schedule.isValid) {
                throw ScheduleError("The server sent back schedule data that didn't validate.")
            }
            File(context.filesDir, CACHE_FILE).writeText(result.body)
            markFetched(context)
            schedule
        }

        private fun fromJson(json: JSONObject): Schedule {
            fun parseStops(key: String): List<String> {
                val arr = json.getJSONArray(key)
                return List(arr.length()) { arr.getString(it) }
            }

            fun parseTable(key: String): Map<String, List<Int?>> {
                val obj = json.getJSONObject(key)
                val map = mutableMapOf<String, List<Int?>>()
                for (trainId in obj.keys()) {
                    val arr = obj.getJSONArray(trainId)
                    map[trainId] = List(arr.length()) { i ->
                        if (arr.isNull(i)) null else arr.getInt(i)
                    }
                }
                return map
            }

            fun parseSpecialDates(): Map<String, Int> {
                if (!json.has("specialDates")) return emptyMap()
                val obj = json.getJSONObject("specialDates")
                val map = mutableMapOf<String, Int>()
                for (date in obj.keys()) map[date] = obj.getInt(date)
                return map
            }

            return Schedule(
                specialDates = parseSpecialDates(),
                northStops = parseStops("northStops"),
                southStops = parseStops("southStops"),
                northWeekday = parseTable("northWeekday"),
                northWeekend = parseTable("northWeekend"),
                northHoliday = parseTable("northHoliday"),
                southWeekday = parseTable("southWeekday"),
                southWeekend = parseTable("southWeekend"),
                southHoliday = parseTable("southHoliday"),
                scheduleDate = if (json.has("scheduleDate")) json.getLong("scheduleDate") else null,
            )
        }
    }

    val isValid: Boolean
        get() {
            if (northStops.isEmpty() || southStops.isEmpty()) return false
            val northTables = listOf(northWeekday, northWeekend, northHoliday)
            val southTables = listOf(southWeekday, southWeekend, southHoliday)
            for (table in northTables) {
                for ((_, times) in table) {
                    if (times.size != northStops.size) return false
                }
            }
            for (table in southTables) {
                for ((_, times) in table) {
                    if (times.size != southStops.size) return false
                }
            }
            return true
        }
}
