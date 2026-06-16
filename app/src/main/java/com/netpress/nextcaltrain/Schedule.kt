package com.netpress.nextcaltrain

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Holds the parsed schedule.json data. Mirrors the iOS Schedule struct.
 * Uses Android's built-in JSONObject rather than a third-party library
 * to keep dependencies minimal.
 */
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
        private const val REMOTE_URL = "https://next-caltrain-pwa.appspot.com/schedule.json"
        private const val CACHE_FILE = "schedule.json"
        private const val FETCH_TIMEOUT_MS = 10_000
        private const val PREFS_NAME = "nextcaltrain"
        private const val KEY_LAST_FETCH_MS = "lastFetchMs"

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

        /** True if the last successful network fetch landed on today's schedule-day
         * (2am boundary, see GoodTimes.scheduleDateFor). Used to skip redundant
         * network calls once we already have today's data. */
        fun fetchedToday(context: Context): Boolean {
            val last = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(KEY_LAST_FETCH_MS, -1L)
            if (last < 0) return false
            return GoodTimes.scheduleDateFor(last) == GoodTimes.scheduleDateFor(System.currentTimeMillis())
        }

        private fun markFetched(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_FETCH_MS, System.currentTimeMillis())
                .apply()
        }

        suspend fun fetchFromNetwork(context: Context): Schedule {
            return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val url = URL(REMOTE_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = FETCH_TIMEOUT_MS
                connection.readTimeout = FETCH_TIMEOUT_MS
                try {
                    if (connection.responseCode != 200) {
                        throw Exception("HTTP ${connection.responseCode}")
                    }
                    val text = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(text)
                    val schedule = fromJson(json)
                    if (!schedule.isValid) throw Exception("Invalid schedule data")
                    File(context.filesDir, CACHE_FILE).writeText(text)
                    markFetched(context)
                    schedule
                } finally {
                    connection.disconnect()
                }
            }
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
