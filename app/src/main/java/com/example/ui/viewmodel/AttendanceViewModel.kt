package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AttendanceConstants
import com.example.data.model.AuthSession
import com.example.data.model.CustomStatusItem
import com.example.data.model.Person
import com.example.data.model.ReportFormatType
import com.example.data.model.ReportTemplateConfig
import com.example.data.model.UserRole
import com.example.data.model.WaktuApel
import com.example.data.repository.AttendanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

data class AttendanceUiState(
    val selectedDate: String = "",
    val selectedWaktuApel: WaktuApel = WaktuApel.PAGI,
    val attendanceData: Map<String, List<Person>> = emptyMap(),
    val customStatuses: List<CustomStatusItem> = emptyList(),
    val reportConfig: ReportTemplateConfig = ReportTemplateConfig(),
    val malamSiagaCompanionUnit: String = "TON I", // PIMPINAN, SIMA, TON I, TON II, TON III
    val searchQuery: String = "",
    val selectedPlatoonFilter: String? = null,
    val editingPerson: Pair<String, Person>? = null, // (groupKey, Person)
    val isManagingStatuses: Boolean = false,
    val editingStatusItem: CustomStatusItem? = null,
    val isEditingReportFormat: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val isSyncDialogOpen: Boolean = false,
    val isManagePersonnelOpen: Boolean = false,
    val customAppName: String = "ABSEN KIKAV 2",
    val customAppLogoUri: String? = null,
    val hasApelPhoto: Boolean = false,
    val lastSavedTimestamp: String? = null,
    val hasUnsavedChanges: Boolean = false,
    val userNotification: String? = null,
    val isLoading: Boolean = false,
    // --- Login & Role State (Single Admin Login) ---
    val currentSession: AuthSession? = null,
    val isLoggedIn: Boolean = false,
    val loginError: String? = null,
    val adminUsername: String = "ADMIN"
)

private data class SlotLoadResult(
    val loaded: Map<String, List<Person>>,
    val savedAt: String?,
    val companionUnit: String,
    val hasPhoto: Boolean
)

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AttendanceRepository(application.applicationContext)

    private val initialDate = repository.getTodayDateWib()
    private val initialWaktu = WaktuApel.PAGI
    private val initialSession = repository.getCurrentSession()

    private val _uiState = MutableStateFlow(
        AttendanceUiState(
            selectedDate = initialDate,
            selectedWaktuApel = initialWaktu,
            attendanceData = repository.loadAttendance(initialDate, initialWaktu),
            customStatuses = repository.loadCustomStatuses(),
            reportConfig = repository.loadReportConfig(),
            malamSiagaCompanionUnit = repository.getMalamSiagaChosenUnit(initialDate),
            customAppName = repository.getCustomAppName(),
            customAppLogoUri = repository.getCustomAppLogoUri(),
            hasApelPhoto = repository.hasApelPhoto(initialDate, initialWaktu),
            lastSavedTimestamp = repository.getLastSavedTimestamp(initialDate, initialWaktu),
            currentSession = initialSession,
            isLoggedIn = initialSession?.isLoggedIn == true,
            adminUsername = repository.getAdminUsername()
        )
    )
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    fun setDate(newDate: String) {
        if (_uiState.value.selectedDate == newDate) return
        viewModelScope.launch {
            val (loaded, savedAt, companionUnit, hasPhoto) = withContext(Dispatchers.IO) {
                val l = repository.loadAttendance(newDate, _uiState.value.selectedWaktuApel)
                val s = repository.getLastSavedTimestamp(newDate, _uiState.value.selectedWaktuApel)
                val c = repository.getMalamSiagaChosenUnit(newDate)
                val p = repository.hasApelPhoto(newDate, _uiState.value.selectedWaktuApel)
                SlotLoadResult(l, s, c, p)
            }
            _uiState.update {
                it.copy(
                    selectedDate = newDate,
                    attendanceData = loaded,
                    malamSiagaCompanionUnit = companionUnit,
                    hasApelPhoto = hasPhoto,
                    lastSavedTimestamp = savedAt,
                    hasUnsavedChanges = false
                )
            }
        }
    }

    fun setWaktuApel(waktu: WaktuApel) {
        if (_uiState.value.selectedWaktuApel == waktu) return
        viewModelScope.launch {
            val (loaded, savedAt, companionUnit, hasPhoto) = withContext(Dispatchers.IO) {
                val l = repository.loadAttendance(_uiState.value.selectedDate, waktu)
                val s = repository.getLastSavedTimestamp(_uiState.value.selectedDate, waktu)
                val c = repository.getMalamSiagaChosenUnit(_uiState.value.selectedDate)
                val p = repository.hasApelPhoto(_uiState.value.selectedDate, waktu)
                SlotLoadResult(l, s, c, p)
            }
            _uiState.update {
                it.copy(
                    selectedWaktuApel = waktu,
                    attendanceData = loaded,
                    malamSiagaCompanionUnit = companionUnit,
                    hasApelPhoto = hasPhoto,
                    lastSavedTimestamp = savedAt,
                    hasUnsavedChanges = false,
                    selectedPlatoonFilter = null
                )
            }
        }
    }

    // --- Switch Malam Siaga Companion Platoon (KOKI, SIMA, TON I, TON II, TON III) ---
    fun setMalamSiagaCompanionUnit(unitKey: String) {
        if (_uiState.value.malamSiagaCompanionUnit == unitKey) return
        viewModelScope.launch {
            val updatedAttendance = repository.switchMalamSiagaUnit(
                date = _uiState.value.selectedDate,
                newUnitKey = unitKey,
                currentAttendance = _uiState.value.attendanceData
            )
            val displayName = AttendanceConstants.DISPLAY_NAMES[unitKey] ?: unitKey
            _uiState.update {
                it.copy(
                    malamSiagaCompanionUnit = unitKey,
                    attendanceData = updatedAttendance,
                    hasUnsavedChanges = false,
                    selectedPlatoonFilter = null,
                    userNotification = "Peleton Siaga diubah ke $displayName"
                )
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setPlatoonFilter(filter: String?) {
        _uiState.update { it.copy(selectedPlatoonFilter = filter) }
    }

    fun openStatusEditor(groupKey: String, person: Person) {
        _uiState.update { it.copy(editingPerson = Pair(groupKey, person)) }
    }

    fun closeStatusEditor() {
        _uiState.update { it.copy(editingPerson = null) }
    }

    fun updateStatus(groupKey: String, personId: Int, newStatus: String, personName: String? = null) {
        val current = _uiState.value.attendanceData
        val targetName = personName ?: current[groupKey]?.find { it.id == personId }?.name
        val targetId = personId

        // Sinkronkan ke SEMUA grup/peleton di attendanceData jika nama atau ID cocok (misal: REMAJA dan TON I / SIMA)
        val updatedMap = current.mapValues { (_, list) ->
            list.map { p ->
                val matches = (p.id == targetId) || (targetName != null && AttendanceConstants.isSameSoldier(p.name, targetName))
                if (matches) {
                    p.copy(status = newStatus)
                } else {
                    p
                }
            }
        }.toMutableMap()

        _uiState.update {
            it.copy(
                attendanceData = updatedMap,
                editingPerson = null,
                hasUnsavedChanges = true,
                userNotification = "Status ${targetName ?: "personel"} diubah ke $newStatus (Tersinkron di semua data)"
            )
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.saveAttendance(
                    _uiState.value.selectedDate,
                    _uiState.value.selectedWaktuApel,
                    updatedMap
                )
            }
        }
    }

    fun markAllPresentInGroup(groupKey: String) {
        val current = _uiState.value.attendanceData
        val groupList = current[groupKey] ?: return
        val updatedGroup = groupList.map { it.copy(status = "HADIR") }
        val updatedMap = current.toMutableMap()
        updatedMap[groupKey] = updatedGroup

        val name = AttendanceConstants.DISPLAY_NAMES[groupKey] ?: groupKey
        _uiState.update {
            it.copy(
                attendanceData = updatedMap,
                hasUnsavedChanges = true,
                userNotification = "Semua personel $name diset HADIR"
            )
        }

        viewModelScope.launch {
            repository.saveAttendance(
                _uiState.value.selectedDate,
                _uiState.value.selectedWaktuApel,
                updatedMap
            )
        }
    }

    fun markAllPresentEntireUnit() {
        val current = _uiState.value.attendanceData
        val updatedMap = mutableMapOf<String, List<Person>>()
        for ((groupKey, list) in current) {
            updatedMap[groupKey] = list.map { it.copy(status = "HADIR") }
        }

        val countTotal = updatedMap.values.sumOf { it.size }
        _uiState.update {
            it.copy(
                attendanceData = updatedMap,
                hasUnsavedChanges = true,
                userNotification = "Semua Personel ($countTotal Orang) diset HADIR Lengkap!"
            )
        }

        viewModelScope.launch {
            repository.saveAttendance(
                _uiState.value.selectedDate,
                _uiState.value.selectedWaktuApel,
                updatedMap
            )
        }
    }

    // --- Explicit Save Data Feature ---
    fun saveDataExplicitly() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.saveAttendance(
                    _uiState.value.selectedDate,
                    _uiState.value.selectedWaktuApel,
                    _uiState.value.attendanceData
                )
            }
            val timestamp = withContext(Dispatchers.IO) {
                repository.getLastSavedTimestamp(
                    _uiState.value.selectedDate,
                    _uiState.value.selectedWaktuApel
                )
            }
            _uiState.update {
                it.copy(
                    lastSavedTimestamp = timestamp,
                    hasUnsavedChanges = false,
                    userNotification = "✓ Data Apel ${_uiState.value.selectedWaktuApel.label} Berhasil Disimpan!"
                )
            }
        }
    }

    // --- SINGLE SYNC BUTTON WORKFLOW ---
    fun openSyncDialog() {
        _uiState.update { it.copy(isSyncDialogOpen = true) }
    }

    fun closeSyncDialog() {
        _uiState.update { it.copy(isSyncDialogOpen = false) }
    }

    /**
     * EKSEKUSI SINKRONISASI TUNGGAL (HANYA APABILA DIKLIK USER):
     * Menyalin status/keterangan dari Apel Pagi ke Siang, Sore, dan Malam Siaga.
     * Jika tombol ini tidak diklik, data pada masing-masing sesi apel TIDAK AKAN tersinkron.
     */
    fun executeSingleSync() {
        viewModelScope.launch {
            val currentDate = _uiState.value.selectedDate
            val currentSlot = _uiState.value.selectedWaktuApel
            val pagiData = if (currentSlot == WaktuApel.PAGI) _uiState.value.attendanceData else null

            val syncedCount = withContext(Dispatchers.IO) {
                repository.syncPagiToAllSlots(
                    date = currentDate,
                    currentPagiData = pagiData
                )
            }

            // Muat ulang data slot aktif saat ini agar UI langsung diperbarui
            val reloaded = withContext(Dispatchers.IO) {
                repository.loadAttendance(currentDate, currentSlot)
            }
            val timestamp = withContext(Dispatchers.IO) {
                repository.getLastSavedTimestamp(currentDate, currentSlot)
            }

            _uiState.update {
                it.copy(
                    attendanceData = reloaded,
                    lastSavedTimestamp = timestamp,
                    hasUnsavedChanges = false,
                    isSyncDialogOpen = false,
                    userNotification = "✓ Berhasil! Keterangan Apel Pagi diterapkan ke Apel Siang, Sore & Malam Siaga ($syncedCount status disinkronkan)."
                )
            }
        }
    }

    /**
     * SINKRONISASI KETERANGAN APEL PAGI KE SIANG DAN SORE:
     * Menyalin semua keterangan/status hasil edit pada Apel Pagi ke Apel Siang & Apel Sore.
     */
    fun syncPagiToSiangDanSore() {
        viewModelScope.launch {
            val (syncedCount, timestamp) = withContext(Dispatchers.IO) {
                if (_uiState.value.selectedWaktuApel == WaktuApel.PAGI) {
                    repository.saveAttendance(
                        _uiState.value.selectedDate,
                        WaktuApel.PAGI,
                        _uiState.value.attendanceData
                    )
                }
                val count = repository.syncPagiToSiangDanSore(
                    date = _uiState.value.selectedDate,
                    pagiData = if (_uiState.value.selectedWaktuApel == WaktuApel.PAGI) _uiState.value.attendanceData else null
                )
                val ts = repository.getLastSavedTimestamp(
                    _uiState.value.selectedDate,
                    _uiState.value.selectedWaktuApel
                )
                Pair(count, ts)
            }
            _uiState.update {
                it.copy(
                    lastSavedTimestamp = timestamp,
                    hasUnsavedChanges = false,
                    userNotification = "✓ Keterangan Apel Pagi berhasil disinkronkan ke Apel Siang & Sore ($syncedCount status disesuaikan)!"
                )
            }
        }
    }

    /**
     * SINKRONISASI SLOT AKTIF (SIANG / SORE / MALAM SIAGA) DARI APEL PAGI:
     * Menerapkan status/keterangan yang sudah diedit di Apel Pagi ke slot yang sedang dibuka.
     */
    fun syncCurrentSlotFromPagi() {
        viewModelScope.launch {
            val targetSlot = _uiState.value.selectedWaktuApel
            val (updated, timestamp) = withContext(Dispatchers.IO) {
                val up = repository.syncSlotFromPagi(
                    date = _uiState.value.selectedDate,
                    targetSlot = targetSlot,
                    currentSlotAttendance = _uiState.value.attendanceData
                )
                val ts = if (up != null) {
                    repository.getLastSavedTimestamp(_uiState.value.selectedDate, targetSlot)
                } else null
                Pair(up, ts)
            }
            if (updated != null) {
                _uiState.update {
                    it.copy(
                        attendanceData = updated,
                        lastSavedTimestamp = timestamp,
                        hasUnsavedChanges = false,
                        userNotification = "✓ Keterangan Apel ${targetSlot.label} berhasil disinkronkan dari Apel Pagi!"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(userNotification = "Data Apel Pagi belum tersedia untuk disinkronkan")
                }
            }
        }
    }

    fun resetDataForCurrentSlot() {
        val fresh = repository.getInitialAttendanceForSlot(
            _uiState.value.selectedWaktuApel,
            _uiState.value.malamSiagaCompanionUnit
        )
        _uiState.update {
            it.copy(
                attendanceData = fresh,
                hasUnsavedChanges = true,
                userNotification = "Data direset ke formasi awal"
            )
        }
        repository.saveAttendance(
            _uiState.value.selectedDate,
            _uiState.value.selectedWaktuApel,
            fresh
        )
    }

    // --- Custom Status Actions ---
    fun openManageStatuses() {
        _uiState.update { it.copy(isManagingStatuses = true) }
    }

    fun closeManageStatuses() {
        _uiState.update { it.copy(isManagingStatuses = false, editingStatusItem = null) }
    }

    fun renameOrSaveStatus(oldCode: String, newCode: String, newDescription: String) {
        val (updatedStatuses, updatedAttendance) = repository.renameOrUpdateStatus(
            oldCode = oldCode,
            newCode = newCode,
            newDescription = newDescription,
            currentDate = _uiState.value.selectedDate,
            currentWaktuApel = _uiState.value.selectedWaktuApel,
            currentAttendance = _uiState.value.attendanceData
        )
        _uiState.update {
            it.copy(
                customStatuses = updatedStatuses,
                attendanceData = updatedAttendance,
                editingStatusItem = null,
                userNotification = "Keterangan '$newCode' berhasil disimpan"
            )
        }
    }

    fun addNewStatus(code: String, description: String) {
        val updated = repository.addCustomStatus(code, description)
        _uiState.update {
            it.copy(
                customStatuses = updated,
                editingStatusItem = null,
                userNotification = "Keterangan '$code' ditambahkan"
            )
        }
    }

    fun deleteStatus(code: String) {
        val updated = repository.deleteCustomStatus(code)
        _uiState.update {
            it.copy(
                customStatuses = updated,
                userNotification = "Keterangan '$code' dihapus"
            )
        }
    }

    fun resetStatusesToDefault() {
        val updated = repository.resetStatusesToDefault()
        _uiState.update {
            it.copy(
                customStatuses = updated,
                userNotification = "Daftar keterangan dikembalikan ke bawaan"
            )
        }
    }

    // --- Custom Report Template Actions ---
    fun setReportFormatType(type: ReportFormatType) {
        val updated = _uiState.value.reportConfig.copy(formatType = type)
        repository.saveReportConfig(updated)
        _uiState.update { it.copy(reportConfig = updated) }
    }

    fun openEditReportFormat() {
        _uiState.update { it.copy(isEditingReportFormat = true) }
    }

    fun closeEditReportFormat() {
        _uiState.update { it.copy(isEditingReportFormat = false) }
    }

    fun saveReportTemplateConfig(config: ReportTemplateConfig) {
        repository.saveReportConfig(config)
        _uiState.update {
            it.copy(
                reportConfig = config,
                isEditingReportFormat = false,
                userNotification = "Format template laporan berhasil diperbarui"
            )
        }
    }

    fun getFormattedReportText(): String {
        return repository.generateFormattedReport(
            date = _uiState.value.selectedDate,
            waktuApel = _uiState.value.selectedWaktuApel,
            data = _uiState.value.attendanceData,
            customStatuses = _uiState.value.customStatuses,
            config = _uiState.value.reportConfig
        )
    }

    fun getExportJson(): String {
        return repository.getExportJson(_uiState.value.attendanceData)
    }

    fun exportToWordFile(): File {
        return repository.exportToWordFile(
            date = _uiState.value.selectedDate,
            waktuApel = _uiState.value.selectedWaktuApel,
            data = _uiState.value.attendanceData,
            customStatuses = _uiState.value.customStatuses,
            config = _uiState.value.reportConfig
        )
    }

    fun getShareWordIntent(file: File): Intent {
        return repository.createShareWordIntent(file)
    }

    fun getOpenWordIntent(file: File): Intent {
        return repository.createOpenWordIntent(file)
    }

    fun openSettings() {
        _uiState.update { it.copy(isSettingsOpen = true) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(isSettingsOpen = false) }
    }

    // --- File Update: Export and Import Configuration JSON ---
    fun exportConfigJson(): File {
        return repository.exportAppConfigJson(
            currentDate = _uiState.value.selectedDate,
            currentWaktuApel = _uiState.value.selectedWaktuApel,
            currentAttendance = _uiState.value.attendanceData
        )
    }

    fun exportConfigJsonString(): String {
        return repository.exportAppConfigJsonString(
            currentDate = _uiState.value.selectedDate,
            currentWaktuApel = _uiState.value.selectedWaktuApel,
            currentAttendance = _uiState.value.attendanceData
        )
    }

    fun getShareConfigIntent(file: File): Intent {
        return repository.createShareConfigIntent(file)
    }

    fun importConfigFromJson(jsonStr: String): AttendanceRepository.ImportResult {
        val result = repository.importAppConfigFromJson(jsonStr)
        if (result.success) {
            val freshStatuses = repository.loadCustomStatuses()
            val freshReportConfig = repository.loadReportConfig()
            val freshAttendance = repository.loadAttendance(
                _uiState.value.selectedDate,
                _uiState.value.selectedWaktuApel
            )
            val savedAt = repository.getLastSavedTimestamp(
                _uiState.value.selectedDate,
                _uiState.value.selectedWaktuApel
            )
            val freshAppName = repository.getCustomAppName()
            _uiState.update {
                it.copy(
                    customStatuses = freshStatuses,
                    reportConfig = freshReportConfig,
                    attendanceData = freshAttendance,
                    customAppName = freshAppName,
                    lastSavedTimestamp = savedAt,
                    hasUnsavedChanges = false,
                    userNotification = result.message
                )
            }
        } else {
            _uiState.update {
                it.copy(userNotification = result.message)
            }
        }
        return result
    }

    // --- Photo Documentation Feature ---
    fun getApelPhotoFile(): File {
        return repository.getApelPhotoFile(_uiState.value.selectedDate, _uiState.value.selectedWaktuApel)
    }

    fun saveApelPhoto(inputStream: java.io.InputStream): Boolean {
        return try {
            val file = repository.saveApelPhoto(_uiState.value.selectedDate, _uiState.value.selectedWaktuApel, inputStream)
            val success = file.exists() && file.length() > 0
            if (success) {
                _uiState.update { it.copy(hasApelPhoto = true, userNotification = "Foto dokumentasi apel berhasil disimpan!") }
            }
            success
        } catch (e: Exception) {
            false
        }
    }

    fun deleteApelPhoto(): Boolean {
        val success = repository.deleteApelPhoto(_uiState.value.selectedDate, _uiState.value.selectedWaktuApel)
        if (success) {
            _uiState.update { it.copy(hasApelPhoto = false, userNotification = "Foto dokumentasi apel dihapus") }
        }
        return success
    }

    fun getShareReportWithPhotoIntent(reportText: String): Intent {
        val photoFile = if (_uiState.value.hasApelPhoto) getApelPhotoFile() else null
        return repository.createShareReportWithPhotoIntent(reportText, photoFile)
    }

    // --- Personnel Management Feature (Add / Delete with Auto Sync) ---
    fun openManagePersonnel() {
        _uiState.update { it.copy(isManagePersonnelOpen = true) }
    }

    fun closeManagePersonnel() {
        _uiState.update { it.copy(isManagePersonnelOpen = false) }
    }

    fun getAllPlatoonRoster(): Map<String, List<String>> {
        return repository.getPlatoonRoster()
    }

    fun getRemajaRoster(): List<String> {
        return repository.getRemajaRoster()
    }

    fun addMember(groupKey: String, name: String, isRemaja: Boolean) {
        val trimmed = name.trim().uppercase()
        if (trimmed.isBlank()) return

        repository.addMemberToRoster(groupKey, trimmed, isRemaja)

        // Sync into active attendanceData
        val currentAttendance = _uiState.value.attendanceData.toMutableMap()
        if (currentAttendance.containsKey(groupKey)) {
            val list = currentAttendance[groupKey]?.toMutableList() ?: mutableListOf()
            if (list.none { it.name.equals(trimmed, ignoreCase = true) }) {
                val nextId = (currentAttendance.values.flatten().maxOfOrNull { it.id } ?: 0) + 1
                list.add(Person(id = nextId, name = trimmed, status = "HADIR"))
                currentAttendance[groupKey] = list
            }
        }
        if (isRemaja && currentAttendance.containsKey("REMAJA")) {
            val remajaList = currentAttendance["REMAJA"]?.toMutableList() ?: mutableListOf()
            if (remajaList.none { it.name.equals(trimmed, ignoreCase = true) }) {
                val nextId = (currentAttendance.values.flatten().maxOfOrNull { it.id } ?: 0) + 1
                remajaList.add(Person(id = nextId, name = trimmed, status = "HADIR"))
                currentAttendance["REMAJA"] = remajaList
            }
        }

        repository.saveAttendance(
            _uiState.value.selectedDate,
            _uiState.value.selectedWaktuApel,
            currentAttendance
        )

        _uiState.update {
            it.copy(
                attendanceData = currentAttendance,
                userNotification = "Anggota $trimmed berhasil ditambahkan ke $groupKey!"
            )
        }
    }

    fun removeMember(groupKey: String, name: String) {
        val trimmed = name.trim()
        repository.removeMemberFromRoster(groupKey, trimmed)

        // Sync out of active attendanceData
        val currentAttendance = _uiState.value.attendanceData.toMutableMap()
        if (currentAttendance.containsKey(groupKey)) {
            val list = currentAttendance[groupKey]?.filterNot { it.name.equals(trimmed, ignoreCase = true) } ?: emptyList()
            currentAttendance[groupKey] = list
        }
        if (currentAttendance.containsKey("REMAJA")) {
            val remajaList = currentAttendance["REMAJA"]?.filterNot { it.name.equals(trimmed, ignoreCase = true) } ?: emptyList()
            currentAttendance["REMAJA"] = remajaList
        }

        repository.saveAttendance(
            _uiState.value.selectedDate,
            _uiState.value.selectedWaktuApel,
            currentAttendance
        )

        _uiState.update {
            it.copy(
                attendanceData = currentAttendance,
                userNotification = "Anggota $trimmed berhasil dihapus!"
            )
        }
    }

    // --- App Identity & Logo Customization ---
    fun updateAppIdentity(name: String, logoUri: String?) {
        repository.setCustomAppName(name)
        repository.setCustomAppLogoUri(logoUri)
        _uiState.update {
            it.copy(
                customAppName = name,
                customAppLogoUri = logoUri,
                userNotification = "Identitas aplikasi dan logo berhasil diperbarui!"
            )
        }
    }

    fun resetAppIdentity() {
        repository.setCustomAppName("ABSEN KIKAV 2")
        repository.setCustomAppLogoUri(null)
        _uiState.update {
            it.copy(
                customAppName = "ABSEN KIKAV 2",
                customAppLogoUri = null,
                userNotification = "Identitas dan logo direset ke default KIKAV 2!"
            )
        }
    }

    fun clearNotification() {
        _uiState.update { it.copy(userNotification = null, loginError = null) }
    }

    // --- Authentication & Session Management ---
    fun login(usernameInput: String, passwordInput: String): Boolean {
        val session = repository.authenticate(usernameInput, passwordInput)
        if (session != null) {
            repository.saveSession(session)
            _uiState.update {
                it.copy(
                    currentSession = session,
                    isLoggedIn = true,
                    loginError = null,
                    userNotification = "Selamat bertugas, ${session.displayName}!"
                )
            }
            return true
        } else {
            val err = "Username atau Password tidak valid! Silakan periksa kembali akun Anda."
            _uiState.update {
                it.copy(loginError = err)
            }
            return false
        }
    }

    fun changeAdminCredentials(newUsername: String, newPassword: String): Boolean {
        if (newUsername.isBlank() || newPassword.isBlank()) {
            _uiState.update { it.copy(userNotification = "Username dan Password tidak boleh kosong!") }
            return false
        }
        val success = repository.setAdminCredentials(newUsername.trim(), newPassword.trim())
        if (success) {
            _uiState.update {
                it.copy(
                    adminUsername = newUsername.trim(),
                    userNotification = "Username dan Password Admin berhasil diperbarui!"
                )
            }
        } else {
            _uiState.update { it.copy(userNotification = "Gagal memperbarui kredensial.") }
        }
        return success
    }

    fun autoSaveData() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.saveAttendance(
                    _uiState.value.selectedDate,
                    _uiState.value.selectedWaktuApel,
                    _uiState.value.attendanceData
                )
            }
            val timestamp = withContext(Dispatchers.IO) {
                repository.getLastSavedTimestamp(
                    _uiState.value.selectedDate,
                    _uiState.value.selectedWaktuApel
                )
            }
            _uiState.update {
                it.copy(
                    lastSavedTimestamp = timestamp,
                    hasUnsavedChanges = false
                )
            }
        }
    }

    fun logout() {
        // Otomatis simpan data absensi saat keluar
        repository.saveAttendance(
            _uiState.value.selectedDate,
            _uiState.value.selectedWaktuApel,
            _uiState.value.attendanceData
        )
        val timestamp = repository.getLastSavedTimestamp(
            _uiState.value.selectedDate,
            _uiState.value.selectedWaktuApel
        )
        repository.logout()
        _uiState.update {
            it.copy(
                currentSession = null,
                isLoggedIn = false,
                loginError = null,
                lastSavedTimestamp = timestamp,
                hasUnsavedChanges = false,
                userNotification = "✓ Data otomatis tersimpan. Silakan login kembali dengan Username & Password."
            )
        }
    }
}
