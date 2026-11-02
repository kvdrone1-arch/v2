package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.AttendanceConstants
import com.example.data.model.AuthSession
import com.example.data.model.CustomStatusItem
import com.example.data.model.Person
import com.example.data.model.ReportFormatType
import com.example.data.model.ReportTemplateConfig
import com.example.data.model.UserRole
import com.example.data.model.WaktuApel
import com.example.util.DateUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AttendanceRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("absen_apel_prefs_v2", Context.MODE_PRIVATE)

    companion object {
        private const val PREF_CUSTOM_STATUSES = "custom_status_definitions_v4"
        private const val PREF_REPORT_CONFIG = "custom_report_template_config_v1"
        private const val PREF_USER_SESSION = "app_user_session_v1"
        private const val PREF_ADMIN_USERNAME = "custom_admin_username_v1"
        private const val PREF_ADMIN_PASSWORD = "custom_admin_password_v1"
    }

    // --- Authentication & Custom Admin Credentials ---
    fun getAdminUsername(): String {
        return prefs.getString(PREF_ADMIN_USERNAME, "ADMIN") ?: "ADMIN"
    }

    fun getAdminPassword(): String {
        return prefs.getString(PREF_ADMIN_PASSWORD, "ADMIN32") ?: "ADMIN32"
    }

    fun setAdminCredentials(username: String, password: String): Boolean {
        val trimmedUser = username.trim()
        val trimmedPass = password.trim()
        if (trimmedUser.isBlank() || trimmedPass.isBlank()) return false
        prefs.edit()
            .putString(PREF_ADMIN_USERNAME, trimmedUser)
            .putString(PREF_ADMIN_PASSWORD, trimmedPass)
            .apply()
        return true
    }

    fun authenticate(usernameInput: String, passwordInput: String): AuthSession? {
        val userTrim = usernameInput.trim()
        val passTrim = passwordInput.trim()

        val expectedUser = getAdminUsername()
        val expectedPass = getAdminPassword()

        return if (userTrim.equals(expectedUser, ignoreCase = true) && passTrim == expectedPass) {
            AuthSession(
                username = expectedUser,
                role = UserRole.ADMIN,
                displayName = "ADMIN KIKAV 2",
                isLoggedIn = true
            )
        } else {
            null
        }
    }

    fun getCurrentSession(): AuthSession? {
        val json = prefs.getString(PREF_USER_SESSION, null) ?: return null
        return try {
            val obj = JSONObject(json)
            AuthSession(
                username = obj.optString("username", getAdminUsername()),
                role = UserRole.ADMIN,
                displayName = obj.optString("displayName", "ADMIN KIKAV 2"),
                isLoggedIn = obj.optBoolean("isLoggedIn", true)
            )
        } catch (e: Exception) {
            null
        }
    }

    fun saveSession(session: AuthSession?) {
        if (session == null) {
            prefs.edit().remove(PREF_USER_SESSION).apply()
        } else {
            val obj = JSONObject()
            obj.put("username", session.username)
            obj.put("role", UserRole.ADMIN.name)
            obj.put("displayName", session.displayName)
            obj.put("isLoggedIn", session.isLoggedIn)
            prefs.edit().putString(PREF_USER_SESSION, obj.toString()).apply()
        }
    }

    fun logout() {
        saveSession(null)
    }

    fun getTodayDateWib(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("GMT+7")
        return sdf.format(Date())
    }

    fun getCurrentTimeWib(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("GMT+7")
        return "${sdf.format(Date())} WIB"
    }

    // --- Custom Status Management ---
    fun loadCustomStatuses(): List<CustomStatusItem> {
        val jsonStr = prefs.getString(PREF_CUSTOM_STATUSES, null)
        if (jsonStr == null) {
            val defaults = getDefaultStatusItems()
            saveCustomStatuses(defaults)
            return defaults
        }

        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<CustomStatusItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CustomStatusItem(
                        code = obj.getString("code"),
                        description = obj.optString("description", ""),
                        isDefault = obj.optBoolean("isDefault", false)
                    )
                )
            }
            if (list.isEmpty()) {
                val defaults = getDefaultStatusItems()
                saveCustomStatuses(defaults)
                defaults
            } else list
        } catch (e: Exception) {
            val defaults = getDefaultStatusItems()
            saveCustomStatuses(defaults)
            defaults
        }
    }

    private fun getDefaultStatusItems(): List<CustomStatusItem> {
        return AttendanceConstants.DEFAULT_STATUS_LIST.map { code ->
            CustomStatusItem(
                code = code,
                description = "",
                isDefault = true
            )
        }
    }

    fun saveCustomStatuses(statuses: List<CustomStatusItem>) {
        val arr = JSONArray()
        for (item in statuses) {
            val obj = JSONObject()
            obj.put("code", item.code)
            obj.put("description", item.description)
            obj.put("isDefault", item.isDefault)
            arr.put(obj)
        }
        prefs.edit().putString(PREF_CUSTOM_STATUSES, arr.toString()).apply()
    }

    fun renameOrUpdateStatus(
        oldCode: String,
        newCode: String,
        newDescription: String,
        currentDate: String,
        currentWaktuApel: WaktuApel,
        currentAttendance: Map<String, List<Person>>
    ): Pair<List<CustomStatusItem>, Map<String, List<Person>>> {
        val currentStatuses = loadCustomStatuses().toMutableList()
        val index = currentStatuses.indexOfFirst { it.code.equals(oldCode, ignoreCase = true) }
        val formattedCode = newCode.uppercase().trim()
        val formattedDesc = newDescription.trim()

        val updatedItem = CustomStatusItem(
            code = formattedCode,
            description = formattedDesc,
            isDefault = AttendanceConstants.DEFAULT_STATUS_LIST.contains(formattedCode)
        )

        if (index >= 0) {
            currentStatuses[index] = updatedItem
        } else {
            currentStatuses.add(updatedItem)
        }
        saveCustomStatuses(currentStatuses)

        val updatedAttendance = if (!oldCode.equals(newCode, ignoreCase = true)) {
            val updatedMap = mutableMapOf<String, List<Person>>()
            for ((group, persons) in currentAttendance) {
                updatedMap[group] = persons.map { p ->
                    if (p.status.equals(oldCode, ignoreCase = true)) {
                        p.copy(status = formattedCode)
                    } else p
                }
            }
            saveAttendance(currentDate, currentWaktuApel, updatedMap)
            updatedMap
        } else {
            currentAttendance
        }

        return Pair(currentStatuses, updatedAttendance)
    }

    fun addCustomStatus(code: String, description: String): List<CustomStatusItem> {
        val current = loadCustomStatuses().toMutableList()
        val formattedCode = code.uppercase().trim()
        val existingIndex = current.indexOfFirst { it.code.equals(formattedCode, ignoreCase = true) }
        val formattedDesc = description.trim()
        val item = CustomStatusItem(
            code = formattedCode,
            description = formattedDesc,
            isDefault = AttendanceConstants.DEFAULT_STATUS_LIST.contains(formattedCode)
        )
        if (existingIndex >= 0) {
            current[existingIndex] = item
        } else {
            current.add(item)
        }
        saveCustomStatuses(current)
        return current
    }

    fun deleteCustomStatus(code: String): List<CustomStatusItem> {
        val current = loadCustomStatuses().toMutableList()
        current.removeAll { it.code.equals(code, ignoreCase = true) }
        saveCustomStatuses(current)
        return current
    }

    fun resetStatusesToDefault(): List<CustomStatusItem> {
        val defaults = getDefaultStatusItems()
        saveCustomStatuses(defaults)
        return defaults
    }

    // --- Dynamic Greeting for All Report Formats ---
    fun getSalamPembukaForWaktu(waktuApel: WaktuApel, customSalam: String = ""): String {
        val trimmed = customSalam.trim()
        if (trimmed.isNotBlank()) {
            if (trimmed.equals("SELAMAT PAGI KOMANDAN, IZIN MELAPORKAN HASIL APEL PAGI PENGECEKAN PERSONEL", ignoreCase = true) ||
                trimmed.equals("SELAMAT PAGI KOMANDAN,IZIN MELAPORKAN HASIL APEL PAGI PENGECEKAN PERSONEL", ignoreCase = true)) {
                return when (waktuApel) {
                    WaktuApel.PAGI -> "SELAMAT PAGI KOMANDAN, IZIN MELAPORKAN HASIL APEL PAGI PENGECEKAN PERSONEL"
                    WaktuApel.SIANG -> "SELAMAT SIANG KOMANDAN, IZIN MELAPORKAN HASIL APEL SIANG PENGECEKAN PERSONEL"
                    WaktuApel.SORE -> "SELAMAT SORE KOMANDAN, IZIN MELAPORKAN HASIL APEL SORE PENGECEKAN PERSONEL"
                    WaktuApel.MALAM_SIAGA -> "SELAMAT MALAM KOMANDAN, IZIN MELAPORKAN HASIL APEL MALAM SIAGA PENGECEKAN PERSONEL"
                }
            }
            return trimmed
        }
        return when (waktuApel) {
            WaktuApel.PAGI -> "SELAMAT PAGI KOMANDAN, IZIN MELAPORKAN HASIL APEL PAGI PENGECEKAN PERSONEL"
            WaktuApel.SIANG -> "SELAMAT SIANG KOMANDAN, IZIN MELAPORKAN HASIL APEL SIANG PENGECEKAN PERSONEL"
            WaktuApel.SORE -> "SELAMAT SORE KOMANDAN, IZIN MELAPORKAN HASIL APEL SORE PENGECEKAN PERSONEL"
            WaktuApel.MALAM_SIAGA -> "SELAMAT MALAM KOMANDAN, IZIN MELAPORKAN HASIL APEL MALAM SIAGA PENGECEKAN PERSONEL"
        }
    }

    // --- Report Template Configuration Persistence ---
    fun loadReportConfig(): ReportTemplateConfig {
        val jsonStr = prefs.getString(PREF_REPORT_CONFIG, null) ?: return ReportTemplateConfig()
        return try {
            val obj = JSONObject(jsonStr)
            val formatStr = obj.optString("formatType", ReportFormatType.STANDAR_KOMANDO.name)
            val format = try {
                ReportFormatType.valueOf(formatStr)
            } catch (e: Exception) {
                ReportFormatType.STANDAR_KOMANDO
            }
            val ketPa = obj.optString("ketPaJaga", "PA JAGA").trim().ifBlank { "PA JAGA" }
            val pa = obj.optString("namaPaJaga", obj.optString("namaPerwiraPiket", "")).trim()
            val ba = obj.optString("namaBaJaga", obj.optString("namaBintaraPiket", "")).trim()
            val ta = obj.optString("namaTaJaga", "").trim()

            ReportTemplateConfig(
                formatType = format,
                headerKomando = obj.optString("headerKomando", AttendanceConstants.SATUAN_KOMANDO),
                headerSatuan = obj.optString("headerSatuan", AttendanceConstants.SATUAN_UNIT),
                reportTitle = obj.optString("reportTitle", "LAPORAN ABSEN APEL"),
                reportSubtitle = obj.optString("reportSubtitle", "PENGECEKAN PERSONEL KIKAV 2/JRTR"),
                salamPembuka = obj.optString("salamPembuka", ""),
                salamPenutup = obj.optString("salamPenutup", "Demikian dilaporkan, tertanda Piket Siaga KIKAV 2/JRTR."),
                wordHeaderFontSize = obj.optInt("wordHeaderFontSize", 16),
                wordTableFontSize = obj.optDouble("wordTableFontSize", 6.5).toFloat(),
                wordPersonnelFontSize = obj.optDouble("wordPersonnelFontSize", 7.0).toFloat(),
                labelKuatNyata = obj.optString("labelKuatNyata", "Kuat Nyata"),
                labelHadir = obj.optString("labelHadir", "Hadir"),
                labelKurang = obj.optString("labelKurang", "Kurang"),
                customTemplateText = obj.optString("customTemplateText", ""),
                ketPaJaga = ketPa,
                namaPaJaga = if (pa.contains("ARYA PRATAMA", ignoreCase = true)) "" else pa,
                namaBaJaga = if (ba.contains("WAHYU HIDAYAT", ignoreCase = true)) "" else ba,
                namaTaJaga = ta,
                namaPerwiraPiket = pa,
                jabatanPerwiraPiket = ketPa,
                namaBintaraPiket = ba,
                jabatanBintaraPiket = "BA JAGA",
                lokasiTandaTangan = obj.optString("lokasiTandaTangan", "").let { if (it.equals("Ambarawa", ignoreCase = true)) "" else it.trim() }
            )
        } catch (e: Exception) {
            ReportTemplateConfig()
        }
    }

    fun saveReportConfig(config: ReportTemplateConfig) {
        val obj = JSONObject()
        obj.put("formatType", config.formatType.name)
        obj.put("headerKomando", config.headerKomando)
        obj.put("headerSatuan", config.headerSatuan)
        obj.put("reportTitle", config.reportTitle)
        obj.put("reportSubtitle", config.reportSubtitle)
        obj.put("salamPembuka", config.salamPembuka)
        obj.put("salamPenutup", config.salamPenutup)
        obj.put("wordHeaderFontSize", config.wordHeaderFontSize)
        obj.put("wordTableFontSize", config.wordTableFontSize.toDouble())
        obj.put("wordPersonnelFontSize", config.wordPersonnelFontSize.toDouble())
        obj.put("labelKuatNyata", config.labelKuatNyata)
        obj.put("labelHadir", config.labelHadir)
        obj.put("labelKurang", config.labelKurang)
        obj.put("customTemplateText", config.customTemplateText)
        obj.put("ketPaJaga", config.ketPaJaga)
        obj.put("namaPaJaga", config.namaPaJaga)
        obj.put("namaBaJaga", config.namaBaJaga)
        obj.put("namaTaJaga", config.namaTaJaga)
        obj.put("namaPerwiraPiket", config.namaPaJaga)
        obj.put("jabatanPerwiraPiket", config.ketPaJaga)
        obj.put("namaBintaraPiket", config.namaBaJaga)
        obj.put("jabatanBintaraPiket", "BA JAGA")
        obj.put("lokasiTandaTangan", config.lokasiTandaTangan)
        prefs.edit().putString(PREF_REPORT_CONFIG, obj.toString()).apply()
    }

    // --- Platoon and Remaja Personnel Roster Persistence ---
    fun getPlatoonRoster(): Map<String, List<String>> {
        val jsonStr = prefs.getString("custom_platoon_roster_v2", null) ?: return AttendanceConstants.INITIAL_DATA
        return try {
            val obj = JSONObject(jsonStr)
            val map = linkedMapOf<String, List<String>>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val arr = obj.getJSONArray(key)
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    list.add(arr.getString(i))
                }
                map[key] = list
            }
            if (map.isEmpty()) AttendanceConstants.INITIAL_DATA else map
        } catch (e: Exception) {
            AttendanceConstants.INITIAL_DATA
        }
    }

    fun getRemajaRoster(): List<String> {
        val jsonStr = prefs.getString("custom_remaja_roster_v2", null) ?: return AttendanceConstants.REMAJA_DATA
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            if (list.isEmpty()) AttendanceConstants.REMAJA_DATA else list
        } catch (e: Exception) {
            AttendanceConstants.REMAJA_DATA
        }
    }

    fun savePlatoonRoster(roster: Map<String, List<String>>) {
        val obj = JSONObject()
        for ((k, v) in roster) {
            val arr = JSONArray(v)
            obj.put(k, arr)
        }
        prefs.edit().putString("custom_platoon_roster_v2", obj.toString()).apply()
    }

    fun saveRemajaRoster(remaja: List<String>) {
        val arr = JSONArray(remaja)
        prefs.edit().putString("custom_remaja_roster_v2", arr.toString()).apply()
    }

    fun addMemberToRoster(groupKey: String, name: String, isRemaja: Boolean): Boolean {
        val trimmed = name.uppercase().trim()
        if (trimmed.isBlank()) return false

        val currentPlatoons = getPlatoonRoster().toMutableMap()
        val list = (currentPlatoons[groupKey] ?: emptyList()).toMutableList()
        if (!list.contains(trimmed)) {
            list.add(trimmed)
            currentPlatoons[groupKey] = list
            savePlatoonRoster(currentPlatoons)
        }

        if (isRemaja) {
            val remajaList = getRemajaRoster().toMutableList()
            if (!remajaList.contains(trimmed)) {
                remajaList.add(trimmed)
                saveRemajaRoster(remajaList)
            }
        }
        return true
    }

    fun removeMemberFromRoster(groupKey: String, name: String): Boolean {
        val trimmed = name.uppercase().trim()
        val currentPlatoons = getPlatoonRoster().toMutableMap()
        val list = (currentPlatoons[groupKey] ?: emptyList()).toMutableList()
        val removed = list.removeAll { it.equals(trimmed, ignoreCase = true) }
        if (removed) {
            currentPlatoons[groupKey] = list
            savePlatoonRoster(currentPlatoons)
        }

        val remajaList = getRemajaRoster().toMutableList()
        val removedRemaja = remajaList.removeAll { it.equals(trimmed, ignoreCase = true) }
        if (removedRemaja) {
            saveRemajaRoster(remajaList)
        }

        return removed || removedRemaja
    }

    // --- App Custom Identity & Logo ---
    fun getCustomAppName(): String {
        return prefs.getString("custom_app_display_name_v1", "ABSEN KIKAV 2") ?: "ABSEN KIKAV 2"
    }

    fun setCustomAppName(name: String) {
        prefs.edit().putString("custom_app_display_name_v1", name.trim()).apply()
    }

    fun getCustomAppLogoUri(): String? {
        return prefs.getString("custom_app_logo_uri_v1", null)
    }

    fun setCustomAppLogoUri(uriStr: String?) {
        prefs.edit().putString("custom_app_logo_uri_v1", uriStr).apply()
    }

    // --- Photo Documentation for Reports ---
    fun getApelPhotoFile(date: String, waktuApel: WaktuApel): File {
        val photosDir = File(context.filesDir, "apel_photos")
        if (!photosDir.exists()) photosDir.mkdirs()
        val safeWaktu = waktuApel.name
        return File(photosDir, "photo_${date}_${safeWaktu}.jpg")
    }

    fun hasApelPhoto(date: String, waktuApel: WaktuApel): Boolean {
        val file = getApelPhotoFile(date, waktuApel)
        return file.exists() && file.length() > 0
    }

    fun saveApelPhoto(date: String, waktuApel: WaktuApel, inputStream: java.io.InputStream): File {
        val file = getApelPhotoFile(date, waktuApel)
        FileOutputStream(file).use { out ->
            inputStream.copyTo(out)
        }
        return file
    }

    fun deleteApelPhoto(date: String, waktuApel: WaktuApel): Boolean {
        val file = getApelPhotoFile(date, waktuApel)
        return if (file.exists()) file.delete() else true
    }

    fun createShareReportWithPhotoIntent(reportText: String, photoFile: File?): Intent {
        return if (photoFile != null && photoFile.exists()) {
            val uri = getFileUri(photoFile)
            Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, reportText)
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Absen Apel KIKAV 2")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, reportText)
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Absen Apel KIKAV 2")
            }
        }
    }

    // --- Attendance Data Persistence & Apel Malam Siaga Support ---
    fun getMalamSiagaChosenUnit(date: String): String {
        return prefs.getString("${date}_MALAM_SIAGA_chosen_unit", "TON I") ?: "TON I"
    }

    fun setMalamSiagaChosenUnit(date: String, unitKey: String) {
        prefs.edit().putString("${date}_MALAM_SIAGA_chosen_unit", unitKey).apply()
    }

    fun getInitialAttendanceForSlot(waktuApel: WaktuApel, chosenUnit: String = "TON I"): Map<String, List<Person>> {
        val data = linkedMapOf<String, List<Person>>()
        val currentPlatoons = getPlatoonRoster()
        val currentRemaja = getRemajaRoster()

        if (waktuApel == WaktuApel.MALAM_SIAGA) {
            var counter = 1
            // 1. Data REMAJA (Personel Tetap)
            val remajaList = currentRemaja.map { name ->
                Person(id = counter++, name = name, status = "HADIR")
            }
            data["REMAJA"] = remajaList

            // 2. Data Peleton Pilihan Siaga (KOKI, SIMA, TON I, TON II, TON III)
            val companionKey = if (AttendanceConstants.SIAGA_OPTIONS.contains(chosenUnit)) chosenUnit else "TON I"
            val companionNames = currentPlatoons[companionKey]
                ?: currentPlatoons["TON I"]
                ?: AttendanceConstants.INITIAL_DATA["TON I"]!!
            val companionList = companionNames.map { name ->
                Person(id = counter++, name = name, status = "HADIR")
            }
            data[companionKey] = companionList
        } else {
            var counter = 1
            for ((group, names) in currentPlatoons) {
                val list = mutableListOf<Person>()
                for (name in names) {
                    list.add(Person(id = counter++, name = name, status = "HADIR"))
                }
                data[group] = list
            }
        }
        return data
    }

    fun getInitialAttendance(): Map<String, List<Person>> = getInitialAttendanceForSlot(WaktuApel.PAGI)

    private fun storageKey(date: String, waktuApel: WaktuApel): String {
        return "${date}_${waktuApel.name}"
    }

    private fun parsePersonArray(arr: JSONArray): List<Person> {
        val persons = mutableListOf<Person>()
        for (i in 0 until arr.length()) {
            val pObj = arr.getJSONObject(i)
            persons.add(
                Person(
                    id = pObj.optInt("id", i + 1),
                    name = pObj.optString("name", "PERSONEL"),
                    status = pObj.optString("status", "HADIR")
                )
            )
        }
        return persons
    }

    private fun parseAttendanceFromJsonString(
        jsonStr: String,
        waktuApel: WaktuApel,
        chosenUnit: String,
        date: String? = null
    ): Map<String, List<Person>> {
        val result = linkedMapOf<String, List<Person>>()
        val jsonObject = JSONObject(jsonStr)

        if (waktuApel == WaktuApel.MALAM_SIAGA) {
            // 1. Load REMAJA
            if (jsonObject.has("REMAJA")) {
                result["REMAJA"] = parsePersonArray(jsonObject.getJSONArray("REMAJA"))
            } else {
                result["REMAJA"] = AttendanceConstants.REMAJA_DATA.mapIndexed { idx, name ->
                    Person(id = idx + 1, name = name, status = "HADIR")
                }
            }

            // 2. Load chosen companion unit
            var companionUnitKey = chosenUnit
            if (!jsonObject.has(companionUnitKey)) {
                val detected = AttendanceConstants.SIAGA_OPTIONS.firstOrNull { jsonObject.has(it) }
                if (detected != null) {
                    companionUnitKey = detected
                    if (date != null) {
                        setMalamSiagaChosenUnit(date, companionUnitKey)
                    }
                }
            }

            if (jsonObject.has(companionUnitKey)) {
                result[companionUnitKey] = parsePersonArray(jsonObject.getJSONArray(companionUnitKey))
            } else {
                val defaultList = AttendanceConstants.INITIAL_DATA[companionUnitKey] ?: emptyList()
                result[companionUnitKey] = defaultList.mapIndexed { idx, name ->
                    Person(id = 100 + idx + 1, name = name, status = "HADIR")
                }
            }
        } else {
            for (groupKey in AttendanceConstants.INITIAL_DATA.keys) {
                if (jsonObject.has(groupKey)) {
                    result[groupKey] = parsePersonArray(jsonObject.getJSONArray(groupKey))
                } else {
                    val defaultList = AttendanceConstants.INITIAL_DATA[groupKey] ?: emptyList()
                    result[groupKey] = defaultList.mapIndexed { idx, name ->
                        Person(id = idx + 1, name = name, status = "HADIR")
                    }
                }
            }
        }
        return result
    }

    fun loadAttendance(date: String, waktuApel: WaktuApel): Map<String, List<Person>> {
        val key = storageKey(date, waktuApel)
        val chosenUnit = if (waktuApel == WaktuApel.MALAM_SIAGA) getMalamSiagaChosenUnit(date) else ""
        val jsonStr = prefs.getString(key, null)

        if (jsonStr == null) {
            return getInitialAttendanceForSlot(waktuApel, chosenUnit)
        }

        return try {
            parseAttendanceFromJsonString(jsonStr, waktuApel, chosenUnit, date)
        } catch (e: Exception) {
            getInitialAttendanceForSlot(waktuApel, chosenUnit)
        }
    }

    /**
     * SATU-SATUNYA LOGIKA SINKRONISASI UTAMA:
     * Hanya dieksekusi apabila user secara eksplisit menekan tombol SINKRONKAN.
     * Menyalin seluruh status/keterangan personel hasil edit pada APEL PAGI
     * ke dalam APEL SIANG, APEL SORE, dan APEL MALAM SIAGA pada tanggal yang sama.
     */
    fun syncPagiToAllSlots(
        date: String,
        currentPagiData: Map<String, List<Person>>? = null
    ): Int {
        val sourcePagi = currentPagiData ?: loadAttendance(date, WaktuApel.PAGI)
        saveAttendance(date, WaktuApel.PAGI, sourcePagi)

        val statusByName = mutableMapOf<String, String>()
        for ((_, persons) in sourcePagi) {
            for (p in persons) {
                statusByName[p.name] = p.status
            }
        }

        val nonHadirCount = sourcePagi.values.flatten().count { it.status != "HADIR" }

        // 1. Sinkronkan ke APEL SIANG dan APEL SORE
        for (targetSlot in listOf(WaktuApel.SIANG, WaktuApel.SORE)) {
            val targetKey = storageKey(date, targetSlot)
            val jsonStr = prefs.getString(targetKey, null)
            val targetAttendance = if (jsonStr != null) {
                try {
                    parseAttendanceFromJsonString(jsonStr, targetSlot, "", date)
                } catch (e: Exception) {
                    getInitialAttendanceForSlot(targetSlot)
                }
            } else {
                getInitialAttendanceForSlot(targetSlot)
            }

            val updatedMap = linkedMapOf<String, List<Person>>()
            for ((groupKey, persons) in targetAttendance) {
                val updatedPersons = persons.map { person ->
                    val sourceStatus = statusByName[person.name]
                    if (sourceStatus != null) {
                        person.copy(status = sourceStatus)
                    } else {
                        person
                    }
                }
                updatedMap[groupKey] = updatedPersons
            }
            saveAttendance(date, targetSlot, updatedMap)
        }

        // 2. Sinkronkan ke APEL MALAM SIAGA (REMAJA + Peleton Siaga yang dipilih)
        val malamKey = storageKey(date, WaktuApel.MALAM_SIAGA)
        val chosenUnit = getMalamSiagaChosenUnit(date)
        val malamJson = prefs.getString(malamKey, null)
        val malamAttendance = if (malamJson != null) {
            try {
                parseAttendanceFromJsonString(malamJson, WaktuApel.MALAM_SIAGA, chosenUnit, date)
            } catch (e: Exception) {
                getInitialAttendanceForSlot(WaktuApel.MALAM_SIAGA, chosenUnit)
            }
        } else {
            getInitialAttendanceForSlot(WaktuApel.MALAM_SIAGA, chosenUnit)
        }

        val updatedMalamMap = linkedMapOf<String, List<Person>>()
        for ((groupKey, persons) in malamAttendance) {
            val updatedPersons = persons.map { person ->
                val sourceStatus = statusByName[person.name]
                if (sourceStatus != null) {
                    person.copy(status = sourceStatus)
                } else {
                    person
                }
            }
            updatedMalamMap[groupKey] = updatedPersons
        }
        saveAttendance(date, WaktuApel.MALAM_SIAGA, updatedMalamMap)

        return nonHadirCount
    }

    /**
     * SINKRONISASI KETERANGAN:
     * Menyalin semua status/keterangan personel hasil edit pada APEL PAGI
     * ke dalam data APEL SIANG dan APEL SORE pada tanggal yang sama.
     */
    fun syncPagiToSiangDanSore(
        date: String,
        pagiData: Map<String, List<Person>>? = null
    ): Int {
        val sourcePagi = pagiData ?: loadAttendance(date, WaktuApel.PAGI)
        saveAttendance(date, WaktuApel.PAGI, sourcePagi)

        // Mapping nama personel -> status di Apel Pagi
        val statusByName = mutableMapOf<String, String>()
        for ((_, persons) in sourcePagi) {
            for (p in persons) {
                statusByName[p.name] = p.status
            }
        }

        val nonHadirInPagi = sourcePagi.values.flatten().count { it.status != "HADIR" }

        for (targetSlot in listOf(WaktuApel.SIANG, WaktuApel.SORE)) {
            val targetKey = storageKey(date, targetSlot)
            val jsonStr = prefs.getString(targetKey, null)
            val targetAttendance = if (jsonStr != null) {
                try {
                    parseAttendanceFromJsonString(jsonStr, targetSlot, "", date)
                } catch (e: Exception) {
                    getInitialAttendanceForSlot(targetSlot)
                }
            } else {
                getInitialAttendanceForSlot(targetSlot)
            }

            val updatedMap = linkedMapOf<String, List<Person>>()
            for ((groupKey, persons) in targetAttendance) {
                val updatedPersons = persons.map { person ->
                    val sourceStatus = statusByName[person.name]
                    if (sourceStatus != null) {
                        person.copy(status = sourceStatus)
                    } else {
                        person
                    }
                }
                updatedMap[groupKey] = updatedPersons
            }
            saveAttendance(date, targetSlot, updatedMap)
        }

        return nonHadirInPagi
    }

    /**
     * Sinkronkan slot aktif (SIANG, SORE, atau MALAM SIAGA)
     * menggunakan data keterangan yang ada di APEL PAGI.
     */
    fun syncSlotFromPagi(
        date: String,
        targetSlot: WaktuApel,
        currentSlotAttendance: Map<String, List<Person>>? = null
    ): Map<String, List<Person>>? {
        val pagiKey = storageKey(date, WaktuApel.PAGI)
        val pagiJson = prefs.getString(pagiKey, null)
        val sourcePagi = if (pagiJson != null) {
            try {
                parseAttendanceFromJsonString(pagiJson, WaktuApel.PAGI, "", date)
            } catch (e: Exception) {
                loadAttendance(date, WaktuApel.PAGI)
            }
        } else {
            loadAttendance(date, WaktuApel.PAGI)
        }

        val statusByName = mutableMapOf<String, String>()
        for ((_, persons) in sourcePagi) {
            for (p in persons) {
                statusByName[p.name] = p.status
            }
        }

        val targetAttendance = currentSlotAttendance ?: loadAttendance(date, targetSlot)
        val updatedMap = linkedMapOf<String, List<Person>>()

        for ((groupKey, persons) in targetAttendance) {
            val updatedPersons = persons.map { person ->
                val sourceStatus = statusByName[person.name]
                if (sourceStatus != null) {
                    person.copy(status = sourceStatus)
                } else {
                    person
                }
            }
            updatedMap[groupKey] = updatedPersons
        }

        saveAttendance(date, targetSlot, updatedMap)
        return updatedMap
    }

    fun switchMalamSiagaUnit(
        date: String,
        newUnitKey: String,
        currentAttendance: Map<String, List<Person>>
    ): Map<String, List<Person>> {
        setMalamSiagaChosenUnit(date, newUnitKey)
        val updatedMap = linkedMapOf<String, List<Person>>()

        // Keep REMAJA list and statuses as is
        updatedMap["REMAJA"] = currentAttendance["REMAJA"] ?: AttendanceConstants.REMAJA_DATA.mapIndexed { idx, name ->
            Person(id = idx + 1, name = name, status = "HADIR")
        }

        // Add the chosen companion unit (e.g. TON I, TON II, etc.) and sync status from Apel Pagi
        val pagiAttendance = loadAttendance(date, WaktuApel.PAGI)
        val pagiStatusByName = mutableMapOf<String, String>()
        for ((_, pList) in pagiAttendance) {
            for (p in pList) {
                pagiStatusByName[p.name] = p.status
            }
        }

        val defaultList = AttendanceConstants.INITIAL_DATA[newUnitKey] ?: emptyList()
        updatedMap[newUnitKey] = defaultList.mapIndexed { idx, name ->
            val statusFromPagi = pagiStatusByName[name] ?: "HADIR"
            Person(id = 100 + idx + 1, name = name, status = statusFromPagi)
        }

        saveAttendance(date, WaktuApel.MALAM_SIAGA, updatedMap)
        return updatedMap
    }

    fun saveAttendance(date: String, waktuApel: WaktuApel, data: Map<String, List<Person>>) {
        val key = storageKey(date, waktuApel)
        val jsonObject = JSONObject()

        for ((groupKey, persons) in data) {
            val arr = JSONArray()
            for (person in persons) {
                val pObj = JSONObject()
                pObj.put("id", person.id)
                pObj.put("name", person.name)
                pObj.put("status", person.status)
                arr.put(pObj)
            }
            jsonObject.put(groupKey, arr)
        }

        val timestamp = getCurrentTimeWib()
        prefs.edit()
            .putString(key, jsonObject.toString())
            .putString("${key}_saved_at", timestamp)
            .apply()
    }

    fun getLastSavedTimestamp(date: String, waktuApel: WaktuApel): String? {
        val key = storageKey(date, waktuApel)
        return prefs.getString("${key}_saved_at", null)
    }

    fun getExportJson(data: Map<String, List<Person>>): String {
        val jsonObject = JSONObject()
        for ((groupKey, persons) in data) {
            val arr = JSONArray()
            for (person in persons) {
                val pObj = JSONObject()
                pObj.put("id", person.id)
                pObj.put("name", person.name)
                pObj.put("status", person.status)
                arr.put(pObj)
            }
            jsonObject.put(groupKey, arr)
        }
        return jsonObject.toString()
    }

    // --- Formatted Reports ---
    fun generateFormattedReport(
        date: String,
        waktuApel: WaktuApel,
        data: Map<String, List<Person>>,
        customStatuses: List<CustomStatusItem>,
        config: ReportTemplateConfig
    ): String {
        return when (config.formatType) {
            ReportFormatType.STANDAR_KOMANDO -> generateStandarKomandoReport(date, waktuApel, data, customStatuses, config)
            ReportFormatType.RINGKAS_WA -> generateRingkasWaReport(date, waktuApel, data, customStatuses, config)
            ReportFormatType.FORMAL_DINAS -> generateFormalDinasReport(date, waktuApel, data, customStatuses, config)
            ReportFormatType.CUSTOM -> generateCustomReport(date, waktuApel, data, customStatuses, config)
        }
    }

    private fun generateStandarKomandoReport(
        date: String,
        waktuApel: WaktuApel,
        data: Map<String, List<Person>>,
        customStatuses: List<CustomStatusItem>,
        config: ReportTemplateConfig
    ): String {
        val statusDescMap = customStatuses.associate { it.code to it.description }
        val formattedDate = DateUtils.formatDateHariBulanTahun(date)
        val dynamicSalam = getSalamPembukaForWaktu(waktuApel, config.salamPembuka)

        val sb = StringBuilder()
        sb.append("*${config.headerKomando}*\n")
        sb.append("*${config.headerSatuan}*\n\n")
        sb.append("$dynamicSalam\n\n")
        sb.append("*LAPORAN ABSEN APEL ${waktuApel.label}*\n")
        sb.append("*PENGECEKAN PERSONEL KIKAV 2/JRTR*\n")
        sb.append("Hari/Tanggal: $formattedDate\n")
        sb.append("Waktu Apel: ${waktuApel.label}\n\n")

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("*A. REKAPITULASI PERSONEL:*\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")

        var totalAll = 0
        var totalHadir = 0
        var totalKurang = 0
        var rowNum = 1

        for ((groupKey, persons) in data) {
            val groupTotal = persons.size
            val hadirCount = persons.count { it.status == "HADIR" }
            val kurangCount = groupTotal - hadirCount
            val displayName = AttendanceConstants.DISPLAY_NAMES[groupKey] ?: groupKey

            totalAll += groupTotal
            totalHadir += hadirCount
            totalKurang += kurangCount

            val nonHadirMap = persons
                .filter { it.status != "HADIR" }
                .groupBy { it.status }
                .map { "${it.key}: ${it.value.size}" }
                .joinToString(", ")

            val detailSuffix = if (nonHadirMap.isNotEmpty()) " ($nonHadirMap)" else ""
            sb.append("${rowNum++}. *$displayName*: Jml $groupTotal | K $kurangCount | H $hadirCount$detailSuffix\n")
        }

        sb.append("────────────────────────────\n")
        sb.append("*JUMLAH KESELURUHAN:*\n")
        sb.append("• Kuat Nyata : $totalAll Orang\n")
        sb.append("• Hadir      : $totalHadir Orang\n")
        sb.append("• Kurang     : $totalKurang Orang\n\n")

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("*B. RINCIAN PERSONEL KURANG (KETERANGAN):*\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")

        val allKurang = mutableListOf<Triple<String, String, String>>()
        for ((groupKey, persons) in data) {
            val unitName = AttendanceConstants.DISPLAY_NAMES[groupKey] ?: groupKey
            for (p in persons) {
                if (p.status != "HADIR") {
                    allKurang.add(Triple(p.status, unitName, p.name))
                }
            }
        }

        if (allKurang.isEmpty()) {
            sb.append("NIHIL (SEMUA PERSONEL HADIR LENGKAP)\n")
        } else {
            val groupedByStatus = allKurang.groupBy { it.first }
            for ((status, list) in groupedByStatus) {
                val desc = statusDescMap[status] ?: status
                sb.append("\n*• $status ($desc): ${list.size} Orang*\n")
                list.forEachIndexed { idx, item ->
                    sb.append("  ${idx + 1}. ${item.third} (${item.second})\n")
                }
            }
        }

        sb.append("\n${config.salamPenutup}\n")

        // Pejabat Jaga: PA JAGA, BA JAGA, TA JAGA
        val paLabel = config.ketPaJaga.ifBlank { "PA JAGA" }
        val paJaga = config.namaPaJaga.ifBlank { config.namaPerwiraPiket }
        val baJaga = config.namaBaJaga.ifBlank { config.namaBintaraPiket }
        val taJaga = config.namaTaJaga
        if (paJaga.isNotBlank() || baJaga.isNotBlank() || taJaga.isNotBlank() || (paLabel != "PA JAGA" && paLabel.isNotBlank())) {
            sb.append("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
            sb.append("*PEJABAT JAGA:*\n")
            if (paJaga.isNotBlank()) {
                sb.append("• $paLabel : *$paJaga*\n")
            } else if (paLabel != "PA JAGA" && paLabel.isNotBlank()) {
                sb.append("• PA JAGA : *$paLabel*\n")
            }
            if (baJaga.isNotBlank()) {
                sb.append("• BA JAGA : *$baJaga*\n")
            }
            if (taJaga.isNotBlank()) {
                sb.append("• TA JAGA : *$taJaga*\n")
            }
            sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        }

        return sb.toString().trimEnd()
    }

    private fun generateRingkasWaReport(
        date: String,
        waktuApel: WaktuApel,
        data: Map<String, List<Person>>,
        customStatuses: List<CustomStatusItem>,
        config: ReportTemplateConfig
    ): String {
        var totalAll = 0
        var totalHadir = 0
        val nonHadirList = mutableListOf<String>()

        for ((groupKey, persons) in data) {
            totalAll += persons.size
            val unitShort = AttendanceConstants.DISPLAY_NAMES[groupKey] ?: groupKey
            for (p in persons) {
                if (p.status == "HADIR") {
                    totalHadir++
                } else {
                    nonHadirList.add("${p.name} ($unitShort - ${p.status})")
                }
            }
        }
        val totalKurang = totalAll - totalHadir
        val formattedDate = DateUtils.formatDateHariBulanTahun(date)
        val dynamicSalam = getSalamPembukaForWaktu(waktuApel, config.salamPembuka)

        val sb = StringBuilder()
        sb.append("*${config.headerSatuan}*\n")
        sb.append("$dynamicSalam\n\n")
        sb.append("📋 *APEL ${waktuApel.label} KIKAV 2/JRTR* ($formattedDate)\n")
        sb.append("📊 Kuat: *$totalAll* | Hadir: *$totalHadir* | Kurang: *$totalKurang*\n\n")
        if (nonHadirList.isEmpty()) {
            sb.append("✅ *LENGKAP: NIHIL KURANG*")
        } else {
            sb.append("⚠️ *DAFTAR PERSONEL KURANG (${nonHadirList.size} Org):*\n")
            nonHadirList.forEachIndexed { i, item ->
                sb.append("${i + 1}. $item\n")
            }
        }
        sb.append("\n${config.salamPenutup}")

        val paLabel = config.ketPaJaga.ifBlank { "PA JAGA" }
        val paJaga = config.namaPaJaga.ifBlank { config.namaPerwiraPiket }
        val baJaga = config.namaBaJaga.ifBlank { config.namaBintaraPiket }
        val taJaga = config.namaTaJaga
        if (paJaga.isNotBlank() || baJaga.isNotBlank() || taJaga.isNotBlank() || (paLabel != "PA JAGA" && paLabel.isNotBlank())) {
            sb.append("\n\n*PEJABAT JAGA:*")
            if (paJaga.isNotBlank()) {
                sb.append("\n• $paLabel : $paJaga")
            } else if (paLabel != "PA JAGA" && paLabel.isNotBlank()) {
                sb.append("\n• PA JAGA : $paLabel")
            }
            if (baJaga.isNotBlank()) sb.append("\n• BA JAGA : $baJaga")
            if (taJaga.isNotBlank()) sb.append("\n• TA JAGA : $taJaga")
        }
        return sb.toString().trimEnd()
    }

    private fun generateFormalDinasReport(
        date: String,
        waktuApel: WaktuApel,
        data: Map<String, List<Person>>,
        customStatuses: List<CustomStatusItem>,
        config: ReportTemplateConfig
    ): String {
        val formattedDate = DateUtils.formatDateHariBulanTahun(date)
        val dynamicSalam = getSalamPembukaForWaktu(waktuApel, config.salamPembuka)

        val sb = StringBuilder()
        sb.append("*${config.headerKomando}*\n")
        sb.append("*${config.headerSatuan}*\n\n")
        sb.append("$dynamicSalam\n\n")
        sb.append("Perihal : Pengecekan Apel ${waktuApel.label}\n")
        sb.append("Hari/Tgl: $formattedDate\n\n")

        var totalAll = 0
        var totalHadir = 0
        for ((_, persons) in data) {
            totalAll += persons.size
            totalHadir += persons.count { it.status == "HADIR" }
        }
        val totalKurang = totalAll - totalHadir

        sb.append("1. Keadaan Umum Personel:\n")
        sb.append("   a. Kuat Nyata : $totalAll orang\n")
        sb.append("   b. Hadir      : $totalHadir orang\n")
        sb.append("   c. Kurang     : $totalKurang orang\n\n")

        sb.append("2. Rincian per Peleton:\n")
        for ((groupKey, persons) in data) {
            val unitName = AttendanceConstants.DISPLAY_FULL_NAMES[groupKey] ?: groupKey
            val hadir = persons.count { it.status == "HADIR" }
            val kurang = persons.size - hadir
            sb.append("   • $unitName: ${persons.size} org (Hadir: $hadir, Kurang: $kurang)\n")
        }

        sb.append("\n3. Personel Kurang:\n")
        val kurangList = mutableListOf<String>()
        for ((groupKey, persons) in data) {
            val unitShort = AttendanceConstants.DISPLAY_NAMES[groupKey] ?: groupKey
            for (p in persons) {
                if (p.status != "HADIR") {
                    kurangList.add("${p.name} ($unitShort) - Ket: ${p.status}")
                }
            }
        }

        if (kurangList.isEmpty()) {
            sb.append("   Nihil.\n")
        } else {
            kurangList.forEachIndexed { i, k ->
                sb.append("   ${i + 1}. $k\n")
            }
        }

        sb.append("\n${config.salamPenutup}\n\n")

        val formalPaLabel = config.ketPaJaga.ifBlank { "PA JAGA" }
        val paJaga = config.namaPaJaga.ifBlank { config.namaPerwiraPiket }.trim()
        val baJaga = config.namaBaJaga.ifBlank { config.namaBintaraPiket }.trim()
        val taJaga = config.namaTaJaga.trim()
        if (paJaga.isNotBlank() || baJaga.isNotBlank() || taJaga.isNotBlank() || (formalPaLabel != "PA JAGA" && formalPaLabel.isNotBlank())) {
            sb.append("Pejabat Jaga:\n")
            if (paJaga.isNotBlank()) {
                sb.append("1. $formalPaLabel : $paJaga\n")
            } else if (formalPaLabel != "PA JAGA" && formalPaLabel.isNotBlank()) {
                sb.append("1. PA JAGA : $formalPaLabel\n")
            }
            if (baJaga.isNotBlank()) sb.append("2. BA JAGA : $baJaga\n")
            if (taJaga.isNotBlank()) sb.append("3. TA JAGA : $taJaga\n")
        }
        return sb.toString().trimEnd()
    }

    private fun generateCustomReport(
        date: String,
        waktuApel: WaktuApel,
        data: Map<String, List<Person>>,
        customStatuses: List<CustomStatusItem>,
        config: ReportTemplateConfig
    ): String {
        var totalAll = 0
        var totalHadir = 0
        val rekapLines = StringBuilder()
        val allKurang = mutableListOf<Triple<String, String, String>>()
        val statusDescMap = customStatuses.associate { it.code to it.description }

        var rowNum = 1
        for ((groupKey, persons) in data) {
            val groupTotal = persons.size
            val hadirCount = persons.count { it.status == "HADIR" }
            val kurangCount = groupTotal - hadirCount
            val displayName = AttendanceConstants.DISPLAY_NAMES[groupKey] ?: groupKey

            totalAll += groupTotal
            totalHadir += hadirCount

            val nonHadirMap = persons
                .filter { it.status != "HADIR" }
                .groupBy { it.status }
                .map { "${it.key}: ${it.value.size}" }
                .joinToString(", ")

            val detailSuffix = if (nonHadirMap.isNotEmpty()) " ($nonHadirMap)" else ""
            rekapLines.append("${rowNum++}. *$displayName*: Jml $groupTotal | K $kurangCount | H $hadirCount$detailSuffix\n")

            for (p in persons) {
                if (p.status != "HADIR") {
                    allKurang.add(Triple(p.status, displayName, p.name))
                }
            }
        }
        val totalKurang = totalAll - totalHadir

        val rincianKurangSb = StringBuilder()
        if (allKurang.isEmpty()) {
            rincianKurangSb.append("NIHIL (SEMUA HADIR LENGKAP)\n")
        } else {
            val groupedByStatus = allKurang.groupBy { it.first }
            for ((status, list) in groupedByStatus) {
                val desc = statusDescMap[status] ?: status
                rincianKurangSb.append("\n*• $status ($desc): ${list.size} Org*\n")
                list.forEachIndexed { idx, item ->
                    rincianKurangSb.append("  ${idx + 1}. ${item.third} (${item.second})\n")
                }
            }
        }

        val formattedDate = DateUtils.formatDateHariBulanTahun(date)
        val dmyDate = DateUtils.formatDateDmy(date)

        val template = if (config.customTemplateText.isNotBlank()) {
            config.customTemplateText
        } else {
            """
*{KODAM}*
*{SATUAN}*

*LAPORAN ABSEN APEL {WAKTU}*
Hari/Tanggal: {TANGGAL}
Waktu Apel: {WAKTU}

{PEMBUKA}

*A. REKAPITULASI:*
{REKAP}
*TOTAL: Kuat {KUAT_NYATA} | Hadir {HADIR} | Kurang {KURANG}*

*B. RINCIAN KURANG:*
{RINCIAN_KURANG}

{PENUTUP}
            """.trimIndent()
        }

        val dynamicSalam = getSalamPembukaForWaktu(waktuApel, config.salamPembuka)
        val paJaga = config.namaPaJaga.ifBlank { config.namaPerwiraPiket }
        val baJaga = config.namaBaJaga.ifBlank { config.namaBintaraPiket }
        val taJaga = config.namaTaJaga

        var result = template
            .replace("{KODAM}", config.headerKomando)
            .replace("{SATUAN}", config.headerSatuan)
            .replace("{WAKTU}", waktuApel.label)
            .replace("{TANGGAL}", formattedDate)
            .replace("{PEMBUKA}", dynamicSalam)
            .replace("{REKAP}", rekapLines.toString().trimEnd())
            .replace("{KUAT_NYATA}", totalAll.toString())
            .replace("{HADIR}", totalHadir.toString())
            .replace("{KURANG}", totalKurang.toString())
            .replace("{RINCIAN_KURANG}", rincianKurangSb.toString().trimEnd())
            .replace("{PENUTUP}", config.salamPenutup)
            .replace("{PA_JAGA}", paJaga.ifBlank { "(....................)" })
            .replace("{KET_PA_JAGA}", config.ketPaJaga.ifBlank { "PA JAGA" })
            .replace("{BA_JAGA}", baJaga.ifBlank { "(....................)" })
            .replace("{TA_JAGA}", taJaga.ifBlank { "(....................)" })
            .replace("{JABATAN_PAWAS}", config.ketPaJaga.ifBlank { "PA JAGA" })
            .replace("{NAMA_PAWAS}", paJaga.ifBlank { "(....................)" })
            .replace("{JABATAN_BA_PIKET}", "BA JAGA")
            .replace("{NAMA_BA_PIKET}", baJaga.ifBlank { "(....................)" })
            .replace("{LOKASI}", config.lokasiTandaTangan)

        return result.trimEnd()
    }

    // --- Export To Word (.doc) Feature ---
    fun exportToWordFile(
        date: String,
        waktuApel: WaktuApel,
        data: Map<String, List<Person>>,
        customStatuses: List<CustomStatusItem>,
        config: ReportTemplateConfig = loadReportConfig()
    ): File {
        val docsDir = File(context.cacheDir, "docs")
        if (!docsDir.exists()) {
            docsDir.mkdirs()
        }
        val safeWaktuName = waktuApel.label.replace(" ", "_")
        val dmyDate = DateUtils.formatDateDmy(date).replace("/", "-")
        val fileName = "Absen_Apel_${safeWaktuName}_${dmyDate}.doc"
        val wordFile = File(docsDir, fileName)

        val htmlContent = generateWordHtml(date, waktuApel, data, customStatuses, config)
        FileOutputStream(wordFile).use { fos ->
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())) // UTF-8 BOM
            fos.write(htmlContent.toByteArray(Charsets.UTF_8))
        }

        return wordFile
    }

    fun getFileUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun createShareWordIntent(file: File): Intent {
        val uri = getFileUri(file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/msword"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun createOpenWordIntent(file: File): Intent {
        val uri = getFileUri(file)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/msword")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    // --- File Update: Export and Import Application Configuration (.json) ---
    data class ImportResult(
        val success: Boolean,
        val message: String,
        val statusesCount: Int = 0,
        val personnelCount: Int = 0
    )

    fun exportAppConfigJson(
        currentDate: String,
        currentWaktuApel: WaktuApel,
        currentAttendance: Map<String, List<Person>>
    ): File {
        val docsDir = File(context.cacheDir, "docs")
        if (!docsDir.exists()) {
            docsDir.mkdirs()
        }
        val fileName = "Setingan_Absen_JRTR_${currentDate}.json"
        val configFile = File(docsDir, fileName)

        val rootObj = JSONObject()
        rootObj.put("app", "Absen Apel JRTR")
        rootObj.put("version", 2)
        rootObj.put("exportDate", currentDate)
        rootObj.put("exportTime", getCurrentTimeWib())
        rootObj.put("komando", AttendanceConstants.SATUAN_KOMANDO)
        rootObj.put("satuan", AttendanceConstants.SATUAN_UNIT)
        rootObj.put("customAppName", getCustomAppName())

        // 1. Custom Statuses
        val statusesArr = JSONArray()
        for (item in loadCustomStatuses()) {
            val sObj = JSONObject()
            sObj.put("code", item.code)
            sObj.put("description", item.description)
            sObj.put("isDefault", item.isDefault)
            statusesArr.put(sObj)
        }
        rootObj.put("customStatuses", statusesArr)

        // 2. Report Template Config
        val reportConfig = loadReportConfig()
        val reportObj = JSONObject()
        reportObj.put("formatType", reportConfig.formatType.name)
        reportObj.put("headerKomando", reportConfig.headerKomando)
        reportObj.put("headerSatuan", reportConfig.headerSatuan)
        reportObj.put("salamPembuka", reportConfig.salamPembuka)
        reportObj.put("salamPenutup", reportConfig.salamPenutup)
        reportObj.put("customTemplateText", reportConfig.customTemplateText)
        reportObj.put("ketPaJaga", reportConfig.ketPaJaga)
        reportObj.put("namaPaJaga", reportConfig.namaPaJaga)
        reportObj.put("namaBaJaga", reportConfig.namaBaJaga)
        reportObj.put("namaTaJaga", reportConfig.namaTaJaga)
        reportObj.put("namaPerwiraPiket", reportConfig.namaPaJaga)
        reportObj.put("jabatanPerwiraPiket", reportConfig.ketPaJaga)
        reportObj.put("namaBintaraPiket", reportConfig.namaBaJaga)
        reportObj.put("jabatanBintaraPiket", "BA JAGA")
        reportObj.put("lokasiTandaTangan", reportConfig.lokasiTandaTangan)
        rootObj.put("reportConfig", reportObj)

        // 3. Platoons Roster
        val platoonsObj = JSONObject()
        val platoons = getPlatoonRoster()
        for ((group, names) in platoons) {
            val namesArr = JSONArray()
            names.forEach { namesArr.put(it) }
            platoonsObj.put(group, namesArr)
        }
        rootObj.put("platoonRoster", platoonsObj)

        // 4. Remaja Roster
        val remajaArr = JSONArray()
        getRemajaRoster().forEach { remajaArr.put(it) }
        rootObj.put("remajaRoster", remajaArr)

        // 5. Current Attendance Data
        val attendanceObj = JSONObject()
        for ((groupKey, persons) in currentAttendance) {
            val arr = JSONArray()
            for (p in persons) {
                val pObj = JSONObject()
                pObj.put("id", p.id)
                pObj.put("name", p.name)
                pObj.put("status", p.status)
                arr.put(pObj)
            }
            attendanceObj.put(groupKey, arr)
        }
        rootObj.put("activeAttendance", attendanceObj)
        rootObj.put("activeDate", currentDate)
        rootObj.put("activeWaktuApel", currentWaktuApel.name)

        FileOutputStream(configFile).use { fos ->
            fos.write(rootObj.toString(2).toByteArray(Charsets.UTF_8))
        }

        return configFile
    }

    fun createShareConfigIntent(file: File): Intent {
        val uri = getFileUri(file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "*/*" // Broad type to ensure all messaging apps (WhatsApp, Telegram, Drive) can share .json files
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = android.content.ClipData.newRawUri("Config JSON", uri)
            putExtra(Intent.EXTRA_SUBJECT, "Update Setingan Absen KIKAV 2 (${file.name})")
            putExtra(
                Intent.EXTRA_TEXT,
                "File update konfigurasi setingan & daftar personel Absen Apel KIKAV 2/JRTR Kodam XXVI/Mataram."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun exportAppConfigJsonString(
        currentDate: String,
        currentWaktuApel: WaktuApel,
        currentAttendance: Map<String, List<Person>>
    ): String {
        val file = exportAppConfigJson(currentDate, currentWaktuApel, currentAttendance)
        return file.readText(Charsets.UTF_8)
    }

    fun importAppConfigFromJson(rawJsonStr: String): ImportResult {
        return try {
            var sanitized = rawJsonStr.trim()
                .removePrefix("\uFEFF")
                .removePrefix("\u200B")
                .trim()

            // Strip markdown code fences if present (```json ... ``` or ``` ...)
            if (sanitized.contains("```")) {
                sanitized = sanitized.substringAfter("```").substringBeforeLast("```").trim()
                if (sanitized.startsWith("json", ignoreCase = true)) {
                    sanitized = sanitized.substring(4).trim()
                }
            }

            // Normalize curly/smart quotes
            sanitized = sanitized
                .replace('“', '"')
                .replace('”', '"')
                .replace('„', '"')
                .replace('`', '"')

            // Auto-locate outer JSON object { ... } in case extra text or chat prefixes were copied
            val firstBrace = sanitized.indexOf('{')
            val lastBrace = sanitized.lastIndexOf('}')
            if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                sanitized = sanitized.substring(firstBrace, lastBrace + 1)
            }

            val root = JSONObject(sanitized)
            var statusesImported = 0
            var personnelImported = 0

            // 1. Import Custom Statuses
            try {
                if (root.has("customStatuses")) {
                    val arr = root.optJSONArray("customStatuses")
                    if (arr != null) {
                        val list = mutableListOf<CustomStatusItem>()
                        for (i in 0 until arr.length()) {
                            val sObj = arr.optJSONObject(i) ?: continue
                            val code = sObj.optString("code", "").trim().uppercase()
                            if (code.isBlank()) continue
                            val desc = sObj.optString("description", "")
                            val isDef = sObj.optBoolean("isDefault", AttendanceConstants.DEFAULT_STATUS_LIST.contains(code))
                            list.add(CustomStatusItem(code = code, description = desc, isDefault = isDef))
                        }
                        if (list.isNotEmpty()) {
                            saveCustomStatuses(list)
                            statusesImported = list.size
                        }
                    }
                }
            } catch (_: Exception) {}

            // 2. Import Report Config
            try {
                if (root.has("reportConfig")) {
                    val rObj = root.optJSONObject("reportConfig")
                    if (rObj != null) {
                        val format = try {
                            ReportFormatType.valueOf(rObj.optString("formatType", ReportFormatType.STANDAR_KOMANDO.name))
                        } catch (e: Exception) {
                            ReportFormatType.STANDAR_KOMANDO
                        }
                        val ketPa = rObj.optString("ketPaJaga", "PA JAGA").trim().ifBlank { "PA JAGA" }
                        val pa = rObj.optString("namaPaJaga", rObj.optString("namaPerwiraPiket", "")).trim()
                        val ba = rObj.optString("namaBaJaga", rObj.optString("namaBintaraPiket", "")).trim()
                        val ta = rObj.optString("namaTaJaga", "").trim()

                        val cleanNamaPa = if (pa.contains("ARYA PRATAMA", ignoreCase = true)) "" else pa
                        val cleanNamaBa = if (ba.contains("WAHYU HIDAYAT", ignoreCase = true)) "" else ba

                        val config = ReportTemplateConfig(
                            formatType = format,
                            headerKomando = rObj.optString("headerKomando", AttendanceConstants.SATUAN_KOMANDO),
                            headerSatuan = rObj.optString("headerSatuan", AttendanceConstants.SATUAN_UNIT),
                            salamPembuka = rObj.optString("salamPembuka", ""),
                            salamPenutup = rObj.optString("salamPenutup", "Demikian dilaporkan, tertanda Piket Siaga KIKAV 2/JRTR."),
                            customTemplateText = rObj.optString("customTemplateText", ""),
                            ketPaJaga = ketPa,
                            namaPaJaga = cleanNamaPa,
                            namaBaJaga = cleanNamaBa,
                            namaTaJaga = ta,
                            namaPerwiraPiket = cleanNamaPa,
                            jabatanPerwiraPiket = ketPa,
                            namaBintaraPiket = cleanNamaBa,
                            jabatanBintaraPiket = "BA JAGA",
                            lokasiTandaTangan = rObj.optString("lokasiTandaTangan", "").let { if (it.equals("Ambarawa", ignoreCase = true)) "" else it.trim() }
                        )
                        saveReportConfig(config)
                    }
                }
            } catch (_: Exception) {}

            // 3. Import Platoon Roster
            if (root.has("platoonRoster")) {
                val pObj = root.optJSONObject("platoonRoster")
                if (pObj != null) {
                    val rosterMap = linkedMapOf<String, List<String>>()
                    val keys = pObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val arr = pObj.optJSONArray(key)
                        if (arr != null) {
                            val names = mutableListOf<String>()
                            for (i in 0 until arr.length()) {
                                val n = arr.optString(i, "").trim().uppercase()
                                if (n.isNotBlank()) names.add(n)
                            }
                            rosterMap[key] = names
                            personnelImported += names.size
                        }
                    }
                    if (rosterMap.isNotEmpty()) {
                        savePlatoonRoster(rosterMap)
                    }
                }
            }

            // 4. Import Remaja Roster
            if (root.has("remajaRoster")) {
                val rArr = root.optJSONArray("remajaRoster")
                if (rArr != null) {
                    val remajaList = mutableListOf<String>()
                    for (i in 0 until rArr.length()) {
                        val n = rArr.optString(i, "").trim().uppercase()
                        if (n.isNotBlank()) remajaList.add(n)
                    }
                    if (remajaList.isNotEmpty()) {
                        saveRemajaRoster(remajaList)
                    }
                }
            }

            // 5. Import Active Attendance if present
            if (root.has("activeAttendance") && root.has("activeDate") && root.has("activeWaktuApel")) {
                val attObj = root.optJSONObject("activeAttendance")
                val date = root.optString("activeDate", "")
                val waktuStr = root.optString("activeWaktuApel", "")
                if (attObj != null && date.isNotBlank() && waktuStr.isNotBlank()) {
                    val waktu = try { WaktuApel.valueOf(waktuStr) } catch (e: Exception) { WaktuApel.PAGI }
                    val attMap = linkedMapOf<String, List<Person>>()
                    val keys = attObj.keys()
                    while (keys.hasNext()) {
                        val gKey = keys.next()
                        val arr = attObj.optJSONArray(gKey)
                        if (arr != null) {
                            attMap[gKey] = parsePersonArray(arr)
                        }
                    }
                    if (attMap.isNotEmpty()) {
                        saveAttendance(date, waktu, attMap)
                    }
                }
            }

            // 6. Import App Name if present
            if (root.has("customAppName")) {
                val customName = root.optString("customAppName", "").trim()
                if (customName.isNotBlank()) {
                    setCustomAppName(customName)
                }
            }

            ImportResult(
                success = true,
                message = "Setingan Berhasil Diterapkan ($statusesImported keterangan, $personnelImported personel)!",
                statusesCount = statusesImported,
                personnelCount = personnelImported
            )
        } catch (e: Exception) {
            ImportResult(
                success = false,
                message = "Gagal memproses setingan: ${e.message ?: "Format data tidak valid"}"
            )
        }
    }

    private fun generateWordHtml(
        date: String,
        waktuApel: WaktuApel,
        data: Map<String, List<Person>>,
        customStatuses: List<CustomStatusItem>,
        config: ReportTemplateConfig = loadReportConfig()
    ): String {
        val statusCodes = customStatuses.map { it.code }
        val statusCountTotal = statusCodes.size

        // Build Summary Table Rows
        val tbodyRows = StringBuilder()
        var totalAll = 0
        var totalHadir = 0
        var totalKurang = 0
        val statusTotals = mutableMapOf<String, Int>()
        statusCodes.forEach { statusTotals[it] = 0 }

        var rowNum = 1
        for ((groupKey, persons) in data) {
            val groupTotal = persons.size
            val hadirCount = persons.count { it.status == "HADIR" }
            val kurangCount = groupTotal - hadirCount
            val displayName = AttendanceConstants.DISPLAY_NAMES[groupKey] ?: groupKey

            totalAll += groupTotal
            totalHadir += hadirCount
            totalKurang += kurangCount

            val counts = mutableMapOf<String, Int>()
            statusCodes.forEach { counts[it] = 0 }
            for (p in persons) {
                if (p.status != "HADIR") {
                    counts[p.status] = (counts[p.status] ?: 0) + 1
                    statusTotals[p.status] = (statusTotals[p.status] ?: 0) + 1
                }
            }

            tbodyRows.append("<tr>")
            tbodyRows.append("<td style='background-color:#f2f2f2;'>${rowNum++}</td>")
            tbodyRows.append("<td style='text-align:left;font-weight:bold;padding-left:3px;'>$displayName</td>")
            tbodyRows.append("<td style='background-color:#f2f2f2;'>$groupTotal</td>")
            tbodyRows.append("<td style='color:#dc2626;font-weight:bold;background-color:#fff1f2;'>${if (kurangCount > 0) kurangCount else "-"}</td>")
            tbodyRows.append("<td style='color:#0284c7;font-weight:bold;background-color:#f0f9ff;'>${if (hadirCount > 0) hadirCount else "-"}</td>")

            for (st in statusCodes) {
                val cnt = counts[st] ?: 0
                val cellVal = if (cnt > 0) cnt.toString() else ""
                val cellStyle = if (cnt > 0) "color:#dc2626;font-weight:bold;background-color:#fff1f2;" else ""
                tbodyRows.append("<td style='$cellStyle'>$cellVal</td>")
            }
            tbodyRows.append("</tr>\n")
        }

        // Summary Total Row
        val totalRow = StringBuilder()
        totalRow.append("<tr style='background-color:#d9d9d9;font-weight:bold;'>")
        totalRow.append("<td colspan='2' style='text-align:left;padding-left:3px;'>JUMLAH KESELURUHAN</td>")
        totalRow.append("<td>$totalAll</td>")
        totalRow.append("<td style='color:#b91c1c;background-color:#fecdd3;'>$totalKurang</td>")
        totalRow.append("<td style='color:#0369a1;background-color:#bae6fd;'>$totalHadir</td>")
        for (st in statusCodes) {
            val cnt = statusTotals[st] ?: 0
            val cellVal = if (cnt > 0) cnt.toString() else ""
            val cellStyle = if (cnt > 0) "color:#b91c1c;" else ""
            totalRow.append("<td style='$cellStyle'>$cellVal</td>")
        }
        totalRow.append("</tr>\n")

        // Subheader th
        val thKeterangan = StringBuilder()
        for (st in statusCodes) {
            thKeterangan.append("<th>$st</th>")
        }

        // Detail Grid Columns for each Platoon (No lines between names, rapat)
        val detailColumns = StringBuilder()
        for ((groupKey, persons) in data) {
            val displayName = if (groupKey == "PIMPINAN") "ESELON PIMPINAN" else groupKey
            val nonHadirMap = mutableMapOf<String, MutableList<Person>>()
            val hadirList = mutableListOf<Person>()
            for (p in persons) {
                if (p.status == "HADIR") hadirList.add(p)
                else nonHadirMap.getOrPut(p.status) { mutableListOf() }.add(p)
            }

            detailColumns.append("<td style='vertical-align:top;padding:0;border:1px solid black;'>")
            detailColumns.append("<div class='col-header'>$displayName (${persons.size})</div>")

            // Non-Hadir groups
            for ((st, pList) in nonHadirMap) {
                detailColumns.append("<div class='status-group-header'>$st</div>")
                pList.forEachIndexed { idx, p ->
                    detailColumns.append("<p class='person-row' style='color:#b91c1c;font-weight:bold;margin:1.5px 0;padding:0 2px;line-height:8.5pt;font-size:6.5pt;border:none;'>${idx + 1}.${p.name}</p>")
                }
            }

            // Hadir group
            detailColumns.append("<div class='hadir-header'>HADIR (${hadirList.size})</div>")
            hadirList.forEachIndexed { idx, p ->
                detailColumns.append("<p class='person-row' style='color:#000000;margin:1.5px 0;padding:0 2px;line-height:8.5pt;font-size:6.5pt;border:none;'>${idx + 1}.${p.name}</p>")
            }
            detailColumns.append("</td>\n")
        }

        val photoFile = getApelPhotoFile(date, waktuApel)
        val photoHtml = if (photoFile.exists() && photoFile.length() > 0) {
            try {
                val bytes = photoFile.readBytes()
                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                """
                <div style='margin-top:8px;text-align:center;'>
                    <div style='font-weight:bold;font-size:8pt;margin-bottom:3px;'>DOKUMENTASI FOTO APEL:</div>
                    <img src='data:image/jpeg;base64,$base64' style='max-width:320px;max-height:200px;border:1px solid #333;' />
                </div>
                """.trimIndent()
            } catch (e: Exception) { "" }
        } else ""

        val formattedDateDisplay = DateUtils.formatDateHariBulanTahun(date)
        val dynamicSalam = getSalamPembukaForWaktu(waktuApel, config.salamPembuka)
        val paLabel = config.ketPaJaga.ifBlank { "PA JAGA" }
        val paJagaName = if (config.namaPaJaga.isNotBlank()) config.namaPaJaga else if (config.ketPaJaga.isNotBlank() && config.ketPaJaga != "PA JAGA") "(${config.ketPaJaga})" else "(....................................)"
        val baJagaName = (config.namaBaJaga.ifBlank { config.namaBintaraPiket }).ifBlank { "(....................................)" }
        val taJagaName = config.namaTaJaga.ifBlank { "(....................................)" }

        return """
        <html xmlns:o='urn:schemas-microsoft-com:office:office' xmlns:w='urn:schemas-microsoft-com:office:word' xmlns='http://www.w3.org/TR/REC-html40'>
        <head>
          <meta charset='utf-8'>
          <title>Absen Apel KIKAV 2/JRTR</title>
          <!--[if gte mso 9]>
          <xml>
            <w:WordDocument>
              <w:View>Print</w:View>
              <w:Zoom>100</w:Zoom>
              <w:DoNotOptimizeForBrowser/>
            </w:WordDocument>
          </xml>
          <![endif]-->
          <style>
            @page WordSection1 {
                size: 595.3pt 841.9pt; /* A4 Portrait */
                mso-page-orientation: portrait;
                margin: 0.4in 0.35in 0.4in 0.35in;
                mso-header-margin: 0.2in;
                mso-footer-margin: 0.2in;
                mso-paper-source: 0;
            }
            div.WordSection1 { page: WordSection1; }
            body { font-family: Arial, sans-serif; font-size: 7pt; line-height: 1.05; margin: 0; padding: 2px; }
            table { border-collapse: collapse; width: 100%; margin-bottom: 6px; }
            #summaryTable { width: 100%; border-collapse: collapse; table-layout: auto; }
            #summaryTable th, #summaryTable td { border: 1px solid black; padding: 1.5px 1px; text-align: center; vertical-align: middle; font-size: 5.5pt; word-wrap: break-word; }
            #summaryTable th { font-size: 5.5pt; background-color: #d9d9d9; }
            .grid-personnel-table { width: 100%; table-layout: fixed; border-collapse: collapse; margin-top: 6px; }
            .grid-personnel-table td { vertical-align: top; padding: 0; border: 1px solid black; }
            .col-header { background-color: #dbe5f1; font-weight: bold; border-bottom: 1px solid black; padding: 2px; font-size: 6.5pt; text-align: center; }
            .status-group-header { color: #dc2626; font-weight: bold; text-align: left; padding: 1px 2px; border-bottom: 1px solid #ccc; background-color: #f2f2f2; font-size: 5.5pt; }
            .hadir-header { background-color: #00b0f0; color: black; font-weight: bold; text-align: left; padding: 1px 2px; border-bottom: 1px solid black; border-top: 1px solid black; font-size: 5.5pt; }
            .person-row {
                display: block;
                text-align: left;
                padding: 1px 2px !important;
                margin: 1.5px 0px !important;
                line-height: 8.5pt !important;
                mso-line-height-rule: exactly;
                font-size: 6.5pt;
                border: none !important;
                border-bottom: none !important;
            }
            .header-top { font-size: 13pt; font-weight: bold; margin-bottom: 4px; text-align: left; text-transform: uppercase; line-height: 1.15; }
            .header-title { font-size: 9.5pt; font-weight: bold; margin-bottom: 2px; text-align: center; text-transform: uppercase; }
            .header-date { font-size: 8.5pt; font-weight: bold; margin-bottom: 6px; text-align: center; }
            .ttd-table { width: 100%; margin-top: 14px; border: none !important; border-collapse: collapse; }
            .ttd-table td { border: none !important; text-align: center; vertical-align: top; font-size: 7.5pt; }
          </style>
        </head>
        <body>
        <div class="WordSection1">
          <div class="header-top">${config.headerKomando.replace("\n", "<br>")}<br>${config.headerSatuan.replace("\n", "<br>")}</div>
          <div style="font-size:8pt; font-weight:bold; margin-bottom: 4px; text-align: left;">${dynamicSalam.replace("\n", "<br>")}</div>
          <div class="header-title">${config.reportTitle} ${waktuApel.label} ${config.reportSubtitle}</div>
          <div class="header-date">HARI / TANGGAL: ${formattedDateDisplay.uppercase()}</div>

          <!-- Summary Table -->
          <table id="summaryTable">
            <thead>
              <tr style="background-color: #a6a6a6;">
                <th rowspan="2" style="width:20px;">NO</th>
                <th rowspan="2" style="width:55px;">NAMA</th>
                <th rowspan="2" style="width:25px;">JML</th>
                <th colspan="${statusCountTotal + 2}">KETERANGAN</th>
              </tr>
              <tr style="background-color: #d9d9d9;">
                <th style="color:#dc2626;width:24px;">KRG</th>
                <th style="color:#0284c7;width:24px;">HDR</th>
                $thKeterangan
              </tr>
            </thead>
            <tbody>
              $tbodyRows
              $totalRow
            </tbody>
          </table>

          <!-- Personnel Grid per Platoon (Portrait, No lines between names, rapat) -->
          <table class="grid-personnel-table">
            <tr>
              $detailColumns
            </tr>
          </table>

          <!-- Photo Documentation if attached -->
          $photoHtml

          <!-- Signature Block Table (Pejabat Jaga: BA JAGA, TA JAGA, PA JAGA) -->
          <table class="ttd-table">
            <tr>
              <td style="width:33%;">
                <b>BA JAGA</b><br><br><br><br>
                <span style="text-decoration:underline;font-weight:bold;">${baJagaName.replace("\n", "<br>")}</span>
              </td>
              <td style="width:33%;">
                <b>TA JAGA</b><br><br><br><br>
                <span style="text-decoration:underline;font-weight:bold;">${taJagaName.replace("\n", "<br>")}</span>
              </td>
              <td style="width:34%;">
                <b>Mengetahui,</b><br>
                <b>${paLabel.replace("\n", "<br>")}</b><br><br><br>
                <span style="text-decoration:underline;font-weight:bold;">${paJagaName.replace("\n", "<br>")}</span>
              </td>
            </tr>
          </table>
        </div>
        </body>
        </html>
        """.trimIndent()
    }
}
