package com.balandman.liftlog.sync

import com.balandman.liftlog.data.LogEntry
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class SheetsException(val code: Int, message: String) : IOException(message) {
    val isAuthExpired: Boolean get() = code == 401
    val isMissing: Boolean get() = code == 404
}

data class SpreadsheetRef(val id: String, val url: String)

/**
 * Thin wrapper over the Sheets REST API. Every call blocks, so callers must be
 * on a background dispatcher.
 */
class SheetsApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {

    // ------------------------------------------------------------------ account

    fun userEmail(token: String): String? {
        val body = get("https://www.googleapis.com/oauth2/v3/userinfo", token)
        return JSONObject(body).optString("email").takeIf { it.isNotEmpty() }
    }

    // -------------------------------------------------------------- spreadsheet

    fun createLogSpreadsheet(token: String, title: String = "Pawgress"): SpreadsheetRef {
        val payload = JSONObject().apply {
            put("properties", JSONObject().put("title", title))
            put(
                "sheets",
                JSONArray().put(
                    JSONObject().put(
                        "properties",
                        JSONObject().put("title", SHEET_TITLE).put("index", 0)
                    )
                )
            )
        }
        val body = post("https://sheets.googleapis.com/v4/spreadsheets", token, payload)
        val json = JSONObject(body)
        val id = json.optString("spreadsheetId")
        if (id.isEmpty()) throw SheetsException(-1, "Sheets did not return a spreadsheet id.")
        val url = json.optString("spreadsheetUrl")
            .takeIf { it.isNotEmpty() }
            ?: "https://docs.google.com/spreadsheets/d/$id/edit"

        appendValues(token, id, listOf(HEADER))
        runCatching { formatHeader(token, id) }   // cosmetic only
        return SpreadsheetRef(id, url)
    }

    fun spreadsheetExists(token: String, spreadsheetId: String): Boolean = try {
        get(
            "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId?fields=spreadsheetId",
            token
        )
        true
    } catch (e: SheetsException) {
        if (e.isMissing) false else throw e
    }

    // --------------------------------------------------------------------- rows

    fun appendEntries(token: String, spreadsheetId: String, entries: List<LogEntry>) {
        if (entries.isEmpty()) return
        appendValues(token, spreadsheetId, entries.map { it.toRow() })
    }

    /**
     * Removes rows for entries that were undone or corrected after they had
     * already been pushed. Rows are matched on the Entry ID column, and deleted
     * one at a time because each deletion shifts everything below it.
     */
    fun deleteEntries(
        token: String,
        spreadsheetId: String,
        entryIds: List<String>,
    ): List<String> {
        if (entryIds.isEmpty()) return emptyList()
        val gid = sheetGid(token, spreadsheetId) ?: return emptyList()
        val removed = mutableListOf<String>()

        for (entryId in entryIds) {
            val rowIndex = findRowIndex(token, spreadsheetId, entryId)
            if (rowIndex == null) {
                // Already gone, or never made it up — either way, stop tracking it.
                removed += entryId
                continue
            }
            val payload = JSONObject().put(
                "requests",
                JSONArray().put(
                    JSONObject().put(
                        "deleteDimension",
                        JSONObject().put(
                            "range",
                            JSONObject()
                                .put("sheetId", gid)
                                .put("dimension", "ROWS")
                                .put("startIndex", rowIndex)
                                .put("endIndex", rowIndex + 1)
                        )
                    )
                )
            )
            post(
                "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId:batchUpdate",
                token,
                payload
            )
            removed += entryId
        }
        return removed
    }

    // ------------------------------------------------------------------ internals

    private fun appendValues(token: String, spreadsheetId: String, rows: List<List<String>>) {
        val values = JSONArray()
        rows.forEach { row ->
            val jsonRow = JSONArray()
            row.forEach { jsonRow.put(it) }
            values.put(jsonRow)
        }
        val range = URLEncoder.encode("$SHEET_TITLE!A1", "UTF-8")
        post(
            "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/$range:append" +
                "?valueInputOption=USER_ENTERED&insertDataOption=INSERT_ROWS",
            token,
            JSONObject().put("values", values)
        )
    }

    private fun findRowIndex(token: String, spreadsheetId: String, entryId: String): Int? {
        val range = URLEncoder.encode("$SHEET_TITLE!E:E", "UTF-8")
        val body = get(
            "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/$range",
            token
        )
        val rows = JSONObject(body).optJSONArray("values") ?: return null
        for (i in 0 until rows.length()) {
            val cell = rows.optJSONArray(i)?.optString(0).orEmpty()
            if (cell == entryId) return i
        }
        return null
    }

    private fun sheetGid(token: String, spreadsheetId: String): Int? {
        val body = get(
            "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId" +
                "?fields=sheets(properties(sheetId,title))",
            token
        )
        val sheets = JSONObject(body).optJSONArray("sheets") ?: return null
        for (i in 0 until sheets.length()) {
            val props = sheets.optJSONObject(i)?.optJSONObject("properties") ?: continue
            if (props.optString("title") == SHEET_TITLE) return props.optInt("sheetId")
        }
        return null
    }

    private fun formatHeader(token: String, spreadsheetId: String) {
        val gid = sheetGid(token, spreadsheetId) ?: return
        val requests = JSONArray()
            .put(
                JSONObject().put(
                    "repeatCell",
                    JSONObject()
                        .put(
                            "range",
                            JSONObject().put("sheetId", gid).put("startRowIndex", 0)
                                .put("endRowIndex", 1)
                        )
                        .put(
                            "cell",
                            JSONObject().put(
                                "userEnteredFormat",
                                JSONObject().put(
                                    "textFormat",
                                    JSONObject().put("bold", true)
                                )
                            )
                        )
                        .put("fields", "userEnteredFormat.textFormat.bold")
                )
            )
            .put(
                JSONObject().put(
                    "updateSheetProperties",
                    JSONObject()
                        .put(
                            "properties",
                            JSONObject().put("sheetId", gid).put(
                                "gridProperties",
                                JSONObject().put("frozenRowCount", 1)
                            )
                        )
                        .put("fields", "gridProperties.frozenRowCount")
                )
            )
        post(
            "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId:batchUpdate",
            token,
            JSONObject().put("requests", requests)
        )
    }

    private fun get(url: String, token: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        return execute(request)
    }

    private fun post(url: String, token: String, payload: JSONObject): String {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .post(payload.toString().toRequestBody(JSON_MEDIA))
            .build()
        return execute(request)
    }

    private fun execute(request: Request): String = client.newCall(request).execute().use { response ->
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            throw SheetsException(response.code, describe(response.code, body))
        }
        body
    }

    private fun describe(code: Int, body: String): String {
        val apiMessage = runCatching {
            JSONObject(body).optJSONObject("error")?.optString("message")
        }.getOrNull().orEmpty()

        return when (code) {
            401 -> "Google sign-in expired."
            403 -> if (apiMessage.contains("has not been used", ignoreCase = true) ||
                apiMessage.contains("disabled", ignoreCase = true)
            ) {
                "The Google Sheets API is not enabled for this project yet. See SETUP.md step 3."
            } else {
                "Google refused the request: $apiMessage"
            }

            404 -> "The Pawgress spreadsheet no longer exists."
            else -> if (apiMessage.isNotEmpty()) apiMessage else "Sheets request failed ($code)."
        }
    }

    companion object {
        private const val SHEET_TITLE = "Log"
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
        private val HEADER = listOf("Date", "Time", "Exercise", "Weight (lb)", "Entry ID")

        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        private fun LogEntry.toRow(): List<String> {
            val moment = Instant.ofEpochMilli(loggedAt).atZone(ZoneId.systemDefault())
            return listOf(
                DATE_FORMAT.format(moment),
                TIME_FORMAT.format(moment),
                machineName,
                weight.toString(),
                id,
            )
        }
    }
}
