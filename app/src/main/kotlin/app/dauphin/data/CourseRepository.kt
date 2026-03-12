package app.dauphin.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.dauphin.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "course_prefs")

class CourseRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val client = OkHttpClient()

    private val COURSE_DATA_KEY = stringPreferencesKey("course_data")
    private val COOKIES_KEY = stringPreferencesKey("asp_net_cookies")

    val cookiesFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[COOKIES_KEY]
    }

    suspend fun saveCookies(cookies: String) {
        context.dataStore.edit { preferences ->
            preferences[COOKIES_KEY] = cookies
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(COOKIES_KEY)
            preferences.remove(COURSE_DATA_KEY)
        }
    }

    suspend fun getCourseData(): CourseResponse? = withContext(Dispatchers.IO) {
        val cookies = cookiesFlow.first() ?: return@withContext null

        val url = "https://ilifeapp.az.tku.edu.tw/api/stu/course"

        Log.d("CourseRepository", "Fetching from URL: $url")

        return@withContext try {
            val remoteData = fetchFromRemote(url, cookies)
            if (remoteData != null) {
                val mappedData = CourseResponse(stuelelist = groupRawToItems(remoteData))
                Log.d("CourseRepository", "Successfully fetched ${mappedData.stuelelist.size} grouped classes")
                saveToLocal(mappedData)
                mappedData
            } else {
                Log.w("CourseRepository", "Remote fetch returned null, falling back to local")
                fetchFromLocal()
            }
        } catch (e: Exception) {
            Log.e("CourseRepository", "Error fetching remote data", e)
            fetchFromLocal()
        }
    }

    private fun fetchFromRemote(url: String, cookies: String): List<RawCourseItem>? {
        val request = Request.Builder()
            .url(url)
            .addHeader("Cookie", cookies)
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("CourseRepository", "HTTP Error: ${response.code}")
                    return null
                }
                val body = response.body?.string() ?: return null
                Log.d("CourseRepository", "Response body: $body")
                json.decodeFromString<List<RawCourseItem>>(body)
            }
        } catch (e: IOException) {
            Log.e("CourseRepository", "Network error", e)
            null
        }
    }

    private fun groupRawToItems(rawItems: List<RawCourseItem>): List<CourseItem> {
        // Filter out empty courses and group by name and week
        return rawItems.filter { it.ch_cos_name.isNotBlank() }
            .groupBy { it.ch_cos_name + it.weekno }
            .map { (_, sessions) ->
                val first = sessions.first()
                val sortedSno = sessions.map { it.sessno }.sorted()
                
                CourseItem(
                    ch_cos_name = first.ch_cos_name,
                    en_cos_name = first.en_cos_name,
                    time_plase = "${first.week}${sortedSno.joinToString(",")}",
                    seat_no = first.seatno,
                    teach_name = first.teach_name,
                    teach_name_en = first.teach_name_en,
                    note = first.note,
                    week = first.weekno,
                    sess1 = sortedSno.getOrNull(0) ?: "",
                    sess2 = sortedSno.getOrNull(1) ?: "",
                    sess3 = sortedSno.getOrNull(2) ?: "",
                    cos_no = "", // Not available in new API
                    cos_ele_seq = "", // Not available in new API
                    remark = "",
                    room = first.room,
                    timePlase = TimePlaseInfo(
                        week = first.weekno,
                        sesses = sortedSno,
                        room = first.room
                    )
                )
            }
    }

    private suspend fun saveToLocal(data: CourseResponse) {
        val serialized = json.encodeToString(data)
        context.dataStore.edit { preferences ->
            preferences[COURSE_DATA_KEY] = serialized
        }
    }

    private suspend fun fetchFromLocal(): CourseResponse? {
        val serialized = context.dataStore.data.map { it[COURSE_DATA_KEY] }.first()
        return if (serialized != null) {
            try {
                json.decodeFromString<CourseResponse>(serialized)
            } catch (e: Exception) {
                Log.e("CourseRepository", "Error decoding local data", e)
                null
            }
        } else {
            null
        }
    }
}
