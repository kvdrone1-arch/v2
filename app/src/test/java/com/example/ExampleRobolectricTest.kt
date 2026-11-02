package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AttendanceConstants
import com.example.data.model.ReportFormatType
import com.example.data.model.ReportTemplateConfig
import com.example.data.model.WaktuApel
import com.example.data.repository.AttendanceRepository
import com.example.ui.viewmodel.AttendanceViewModel
import androidx.core.content.FileProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Before
    fun resetFileProviderCache() {
        try {
            val field = FileProvider::class.java.getDeclaredField("sCache")
            field.isAccessible = true
            (field.get(null) as? MutableMap<*, *>)?.clear()
        } catch (_: Exception) {}
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ABSEN KIKAV 2", appName)
    }

    @Test
    fun `verify initial personnel count equals 105`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = AttendanceRepository(context)
        val initial = repo.getInitialAttendance()

        var total = 0
        for ((_, list) in initial) {
            total += list.size
        }
        assertEquals(105, total)
        assertEquals(7, initial["PIMPINAN"]?.size)
        assertEquals(32, initial["SIMA"]?.size)
        assertEquals(22, initial["TON I"]?.size)
        assertEquals(23, initial["TON II"]?.size)
        assertEquals(20, initial["TON III"]?.size)
        assertEquals(1, initial["LF"]?.size)
    }

    @Test
    fun `verify attendance save and load persistence with timestamp`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = AttendanceRepository(context)
        val initial = repo.getInitialAttendance().toMutableMap()
        val statuses = repo.loadCustomStatuses()

        // Change HERU to ISDD
        val pimpinan = initial["PIMPINAN"]!!.map {
            if (it.name == "HERU") it.copy(status = "ISDD") else it
        }
        initial["PIMPINAN"] = pimpinan

        val testDate = "2026-10-29"
        val testWaktu = WaktuApel.PAGI
        repo.saveAttendance(testDate, testWaktu, initial)

        val loaded = repo.loadAttendance(testDate, testWaktu)
        val heru = loaded["PIMPINAN"]?.find { it.name == "HERU" }
        assertNotNull(heru)
        assertEquals("ISDD", heru?.status)

        // Verify last saved timestamp
        val savedAt = repo.getLastSavedTimestamp(testDate, testWaktu)
        assertNotNull(savedAt)
        assertTrue(savedAt!!.contains("WIB"))

        // Generate report text
        val report = repo.generateFormattedReport(testDate, testWaktu, loaded, statuses, ReportTemplateConfig())
        assertTrue(report.contains("ISDD"))
        assertTrue(report.contains("HERU"))
    }

    @Test
    fun `verify custom report formats`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = AttendanceRepository(context)
        val initial = repo.getInitialAttendance()
        val statuses = repo.loadCustomStatuses()

        // Ringkas WA
        val ringkasConfig = ReportTemplateConfig(formatType = ReportFormatType.RINGKAS_WA)
        val ringkasText = repo.generateFormattedReport("2026-10-29", WaktuApel.PAGI, initial, statuses, ringkasConfig)
        assertTrue(ringkasText.contains("APEL PAGI KIKAV 2/JRTR"))
        assertTrue(ringkasText.contains("Kuat:"))

        // Formal Dinas
        val formalConfig = ReportTemplateConfig(formatType = ReportFormatType.FORMAL_DINAS)
        val formalText = repo.generateFormattedReport("2026-10-29", WaktuApel.PAGI, initial, statuses, formalConfig)
        assertTrue(formalText.contains("Keadaan Umum Personel"))

        // Custom template
        val customConfig = ReportTemplateConfig(
            formatType = ReportFormatType.CUSTOM,
            headerSatuan = "KIKAV 2 YOGYAKARTA",
            customTemplateText = "SATUAN: {SATUAN} - TOTAL: {KUAT_NYATA} - HADIR: {HADIR}"
        )
        val customText = repo.generateFormattedReport("2026-10-29", WaktuApel.PAGI, initial, statuses, customConfig)
        assertEquals("SATUAN: KIKAV 2 YOGYAKARTA - TOTAL: 105 - HADIR: 105", customText.trim())
    }

    @Test
    fun `verify export to word doc has no dashed lines and tight spacing`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = AttendanceRepository(context)
        val attendance = repo.getInitialAttendance()
        val statuses = repo.loadCustomStatuses()

        val wordFile = repo.exportToWordFile(
            date = "2026-10-29",
            waktuApel = WaktuApel.PAGI,
            data = attendance,
            customStatuses = statuses
        )

        assertTrue("Word file should exist", wordFile.exists())
        assertTrue("Word file should not be empty", wordFile.length() > 0)
        assertTrue("Word file extension should be .doc", wordFile.name.endsWith(".doc"))

        val content = wordFile.readText(Charsets.UTF_8)
        assertTrue("Contains Word namespace", content.contains("schemas-microsoft-com:office:word"))
        assertTrue("Contains summary table", content.contains("summaryTable"))
        assertTrue("Contains unit name", content.contains("KIKAV 2/JRTR"))

        // Verify requirement: Word export in Portrait format
        assertTrue("Should be A4 Portrait width and height", content.contains("size: 595.3pt 841.9pt"))
        assertTrue("Should specify portrait orientation", content.contains("mso-page-orientation: portrait"))

        // Verify signature table block
        assertTrue("Should contain ttd-table for duty officers", content.contains("ttd-table"))
        assertTrue("Should contain HARI / TANGGAL header", content.contains("HARI / TANGGAL"))

        // Verify requirement: HILANGKAN GARIS-GARIS PADA TABEL NAMA
        assertFalse("Should not contain dashed border lines between names", content.contains("0.5px dashed"))
        assertTrue("Should contain border: none for person-row", content.contains("border: none"))

        // Verify FileProvider URI creation
        val uri = repo.getFileUri(wordFile)
        assertNotNull(uri)
        assertTrue(uri.toString().startsWith("content://"))
    }

    @Test
    fun `verify apel malam siaga has remaja 43 and customizable companion unit`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = AttendanceRepository(context)

        // 1. Verify Remaja data has exactly 43 members
        assertEquals(43, AttendanceConstants.REMAJA_DATA.size)
        assertTrue(AttendanceConstants.REMAJA_DATA.contains("DAFFA"))
        assertTrue(AttendanceConstants.REMAJA_DATA.contains("YOGI"))
        assertTrue(AttendanceConstants.REMAJA_DATA.contains("ELY"))
        assertTrue(AttendanceConstants.REMAJA_DATA.contains("ZAENAL"))

        val testDate = "2026-10-29"
        val testWaktu = WaktuApel.MALAM_SIAGA

        // 2. Default companion unit is TON I
        val defaultCompanion = repo.getMalamSiagaChosenUnit(testDate)
        assertEquals("TON I", defaultCompanion)

        val initialMalamSiaga = repo.loadAttendance(testDate, testWaktu)
        assertTrue(initialMalamSiaga.containsKey("REMAJA"))
        assertTrue(initialMalamSiaga.containsKey("TON I"))
        assertEquals(43, initialMalamSiaga["REMAJA"]?.size)
        assertEquals(22, initialMalamSiaga["TON I"]?.size)
        val totalInitial = (initialMalamSiaga["REMAJA"]?.size ?: 0) + (initialMalamSiaga["TON I"]?.size ?: 0)
        assertEquals(65, totalInitial)

        // 3. Switch companion unit to KOKI (PIMPINAN)
        val switchedAttendance = repo.switchMalamSiagaUnit(testDate, "PIMPINAN", initialMalamSiaga)
        assertEquals("PIMPINAN", repo.getMalamSiagaChosenUnit(testDate))
        assertTrue(switchedAttendance.containsKey("REMAJA"))
        assertTrue(switchedAttendance.containsKey("PIMPINAN"))
        assertFalse(switchedAttendance.containsKey("TON I"))
        assertEquals(43, switchedAttendance["REMAJA"]?.size)
        assertEquals(7, switchedAttendance["PIMPINAN"]?.size)
        val totalSwitched = (switchedAttendance["REMAJA"]?.size ?: 0) + (switchedAttendance["PIMPINAN"]?.size ?: 0)
        assertEquals(50, totalSwitched)

        // 4. Save and generate report for Malam Siaga
        repo.saveAttendance(testDate, testWaktu, switchedAttendance)
        val statuses = repo.loadCustomStatuses()
        val report = repo.generateFormattedReport(testDate, testWaktu, switchedAttendance, statuses, ReportTemplateConfig())
        assertTrue(report.contains("APEL MALAM SIAGA"))
        assertTrue(report.contains("REMAJA"))
        assertTrue(report.contains("KOKI"))
        assertTrue(report.contains("Kuat Nyata : 50"))
    }

    @Test
    fun `verify synchronization of edited statuses only happens when explicitly triggered`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = AttendanceRepository(context)
        val syncDate = "2026-10-31"

        // 1. Setup Apel Pagi with edited statuses
        val pagiData = repo.getInitialAttendance().toMutableMap()

        // Edit HERU to ISDD in PIMPINAN
        pagiData["PIMPINAN"] = pagiData["PIMPINAN"]!!.map {
            if (it.name == "HERU") it.copy(status = "ISDD") else it
        }
        // Edit DAFFA to DL and SUGENG to CT in SIMA
        pagiData["SIMA"] = pagiData["SIMA"]!!.map {
            when (it.name) {
                "DAFFA" -> it.copy(status = "DL")
                "SUGENG" -> it.copy(status = "CT")
                else -> it
            }
        }
        // Edit DIMAS to SAKIT in TON I
        pagiData["TON I"] = pagiData["TON I"]!!.map {
            if (it.name == "DIMAS") it.copy(status = "SAKIT") else it
        }

        repo.saveAttendance(syncDate, WaktuApel.PAGI, pagiData)

        // 2. BEFORE clicking sync: verify APEL SIANG and SORE are NOT synchronized
        val unsyncedSiang = repo.loadAttendance(syncDate, WaktuApel.SIANG)
        assertEquals("HADIR", unsyncedSiang["PIMPINAN"]?.find { it.name == "HERU" }?.status)
        assertEquals("HADIR", unsyncedSiang["SIMA"]?.find { it.name == "DAFFA" }?.status)
        assertEquals("HADIR", unsyncedSiang["SIMA"]?.find { it.name == "SUGENG" }?.status)
        assertEquals("HADIR", unsyncedSiang["TON I"]?.find { it.name == "DIMAS" }?.status)

        val unsyncedSore = repo.loadAttendance(syncDate, WaktuApel.SORE)
        assertEquals("HADIR", unsyncedSore["PIMPINAN"]?.find { it.name == "HERU" }?.status)

        // 3. Perform explicit synchronization to Siang and Sore (as if user clicked SINKRONKAN)
        val syncedCount = repo.syncPagiToSiangDanSore(syncDate, pagiData)
        assertTrue("Synced count should be at least 4", syncedCount >= 4)

        // 4. AFTER clicking sync: verify APEL SIANG and SORE now have synchronized statuses
        val siangData = repo.loadAttendance(syncDate, WaktuApel.SIANG)
        assertEquals("ISDD", siangData["PIMPINAN"]?.find { it.name == "HERU" }?.status)
        assertEquals("DL", siangData["SIMA"]?.find { it.name == "DAFFA" }?.status)
        assertEquals("CT", siangData["SIMA"]?.find { it.name == "SUGENG" }?.status)
        assertEquals("SAKIT", siangData["TON I"]?.find { it.name == "DIMAS" }?.status)

        val soreData = repo.loadAttendance(syncDate, WaktuApel.SORE)
        assertEquals("ISDD", soreData["PIMPINAN"]?.find { it.name == "HERU" }?.status)
        assertEquals("DL", soreData["SIMA"]?.find { it.name == "DAFFA" }?.status)
        assertEquals("CT", soreData["SIMA"]?.find { it.name == "SUGENG" }?.status)
        assertEquals("SAKIT", soreData["TON I"]?.find { it.name == "DIMAS" }?.status)

        // 5. Test slot-specific sync (syncSlotFromPagi) for fresh date:
        val freshDate = "2026-11-01"
        val freshPagi = repo.getInitialAttendance().toMutableMap()
        freshPagi["PIMPINAN"] = freshPagi["PIMPINAN"]!!.map {
            if (it.name == "DANKI") it.copy(status = "PAM") else it
        }
        repo.saveAttendance(freshDate, WaktuApel.PAGI, freshPagi)

        // Loading SIANG before sync action: DANKI must NOT be PAM (remains default HADIR)
        val beforeSyncSlot = repo.loadAttendance(freshDate, WaktuApel.SIANG)
        assertEquals("HADIR", beforeSyncSlot["PIMPINAN"]?.find { it.name == "DANKI" }?.status)

        // Now trigger explicit sync for this slot
        repo.syncSlotFromPagi(freshDate, WaktuApel.SIANG)
        val afterSyncSlot = repo.loadAttendance(freshDate, WaktuApel.SIANG)
        assertEquals("PAM", afterSyncSlot["PIMPINAN"]?.find { it.name == "DANKI" }?.status)
    }

    @Test
    fun `verify single sync button synchronizes Pagi to Siang Sore and Malam Siaga only when clicked`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = AttendanceRepository(context)
        val testDate = "2026-11-05"

        // 1. Edit Pagi
        val pagiData = repo.getInitialAttendance().toMutableMap()
        pagiData["SIMA"] = pagiData["SIMA"]!!.map {
            if (it.name == "DAFFA") it.copy(status = "DL") else it
        }
        pagiData["TON I"] = pagiData["TON I"]!!.map {
            if (it.name == "DIMAS") it.copy(status = "SAKIT") else it
        }
        pagiData["PIMPINAN"] = pagiData["PIMPINAN"]!!.map {
            if (it.name == "HERU") it.copy(status = "ISDD") else it
        }
        repo.saveAttendance(testDate, WaktuApel.PAGI, pagiData)

        // 2. BEFORE clicking sync: ALL slots must remain independent (NOT synced)
        val siangBefore = repo.loadAttendance(testDate, WaktuApel.SIANG)
        val soreBefore = repo.loadAttendance(testDate, WaktuApel.SORE)
        val malamBefore = repo.loadAttendance(testDate, WaktuApel.MALAM_SIAGA)

        assertEquals("HADIR", siangBefore["SIMA"]?.find { it.name == "DAFFA" }?.status)
        assertEquals("HADIR", soreBefore["TON I"]?.find { it.name == "DIMAS" }?.status)
        assertEquals("HADIR", malamBefore["REMAJA"]?.find { it.name == "DAFFA" }?.status)
        assertEquals("HADIR", malamBefore["TON I"]?.find { it.name == "DIMAS" }?.status)

        // 3. User clicks the single sync button -> syncPagiToAllSlots is executed
        val syncedCount = repo.syncPagiToAllSlots(testDate, pagiData)
        assertTrue("Synced count should reflect edited statuses", syncedCount >= 3)

        // 4. AFTER clicking single sync: SIANG, SORE, and MALAM_SIAGA are updated
        val siangAfter = repo.loadAttendance(testDate, WaktuApel.SIANG)
        val soreAfter = repo.loadAttendance(testDate, WaktuApel.SORE)
        val malamAfter = repo.loadAttendance(testDate, WaktuApel.MALAM_SIAGA)

        assertEquals("DL", siangAfter["SIMA"]?.find { it.name == "DAFFA" }?.status)
        assertEquals("SAKIT", siangAfter["TON I"]?.find { it.name == "DIMAS" }?.status)
        assertEquals("ISDD", siangAfter["PIMPINAN"]?.find { it.name == "HERU" }?.status)

        assertEquals("DL", soreAfter["SIMA"]?.find { it.name == "DAFFA" }?.status)
        assertEquals("SAKIT", soreAfter["TON I"]?.find { it.name == "DIMAS" }?.status)
        assertEquals("ISDD", soreAfter["PIMPINAN"]?.find { it.name == "HERU" }?.status)

        // Malam Siaga: DAFFA is in REMAJA, DIMAS is in TON I
        assertEquals("DL", malamAfter["REMAJA"]?.find { it.name == "DAFFA" }?.status)
        assertEquals("SAKIT", malamAfter["TON I"]?.find { it.name == "DIMAS" }?.status)
    }

    @Test
    fun `verify default admin login and custom credentials update`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = AttendanceRepository(context)

        // Verify default username and password
        assertEquals("ADMIN", repo.getAdminUsername())
        assertEquals("ADMIN32", repo.getAdminPassword())

        // Default login succeeds
        val sessionDefault = repo.authenticate("ADMIN", "ADMIN32")
        assertNotNull(sessionDefault)
        assertEquals(com.example.data.model.UserRole.ADMIN, sessionDefault?.role)

        // Invalid password fails
        val sessionInvalid = repo.authenticate("ADMIN", "WRONG_PASSWORD")
        assertTrue(sessionInvalid == null)

        // Change custom credentials
        val updateSuccess = repo.setAdminCredentials("KOMANDAN", "KIKAV2026")
        assertTrue(updateSuccess)
        assertEquals("KOMANDAN", repo.getAdminUsername())
        assertEquals("KIKAV2026", repo.getAdminPassword())

        // Old credentials no longer work
        val oldLogin = repo.authenticate("ADMIN", "ADMIN32")
        assertTrue(oldLogin == null)

        // New credentials authenticate successfully
        val newLogin = repo.authenticate("KOMANDAN", "KIKAV2026")
        assertNotNull(newLogin)
        assertEquals("KOMANDAN", newLogin?.username)

        // Reset back to ADMIN / ADMIN32 for test cleanliness
        repo.setAdminCredentials("ADMIN", "ADMIN32")
    }

    @Test
    fun `verify export and import config with WhatsApp markdown and text sanitization`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = AttendanceRepository(context)

        // Generate export text
        val testDate = "2026-10-31"
        val testWaktu = WaktuApel.PAGI
        val testAttendance = repo.getInitialAttendance()
        val exportedJson = repo.exportAppConfigJsonString(testDate, testWaktu, testAttendance)
        assertTrue(exportedJson.contains("customStatuses"))
        assertTrue(exportedJson.contains("reportConfig"))
        assertTrue(exportedJson.contains("version"))

        // Test 1: Direct clean JSON import
        val resultClean = repo.importAppConfigFromJson(exportedJson)
        assertTrue(resultClean.success)

        // Test 2: WhatsApp formatted with markdown triple backticks and message preamble
        val whatsappPasted = """
            Berikut ini konfigurasi pengaturan aplikasi Absen Kikav 2:
            ```json
            $exportedJson
            ```
            Silakan diimpor ke aplikasi ya!
        """.trimIndent()

        val resultPasted = repo.importAppConfigFromJson(whatsappPasted)
        assertTrue(resultPasted.success)
        assertTrue(resultPasted.message.contains("berhasil", ignoreCase = true))

        // Test 3: Verify share intent creation
        val configFile = repo.exportAppConfigJson(testDate, testWaktu, testAttendance)
        assertTrue(configFile.exists())
        val shareIntent = repo.createShareConfigIntent(configFile)
        assertEquals(android.content.Intent.ACTION_SEND, shareIntent.action)
        assertEquals("*/*", shareIntent.type)
        assertTrue(shareIntent.hasExtra(android.content.Intent.EXTRA_STREAM))
    }

    @Test
    fun `verify custom duty officers PA JAGA BA JAGA TA JAGA and dynamic salam pembuka without bottom date`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = AttendanceRepository(context)
        val initial = repo.getInitialAttendance()
        val statuses = repo.loadCustomStatuses()

        // 1. Verify dynamic salam pembuka for all WaktuApel
        val configDefault = ReportTemplateConfig()
        val reportPagi = repo.generateFormattedReport("2026-10-31", WaktuApel.PAGI, initial, statuses, configDefault)
        assertTrue(reportPagi.contains("SELAMAT PAGI KOMANDAN, IZIN MELAPORKAN HASIL APEL PAGI PENGECEKAN PERSONEL"))

        val reportSiang = repo.generateFormattedReport("2026-10-31", WaktuApel.SIANG, initial, statuses, configDefault)
        assertTrue(reportSiang.contains("SELAMAT SIANG KOMANDAN, IZIN MELAPORKAN HASIL APEL SIANG PENGECEKAN PERSONEL"))

        val reportSore = repo.generateFormattedReport("2026-10-31", WaktuApel.SORE, initial, statuses, configDefault)
        assertTrue(reportSore.contains("SELAMAT SORE KOMANDAN, IZIN MELAPORKAN HASIL APEL SORE PENGECEKAN PERSONEL"))

        val reportMalam = repo.generateFormattedReport("2026-10-31", WaktuApel.MALAM_SIAGA, initial, statuses, configDefault)
        assertTrue(reportMalam.contains("SELAMAT MALAM KOMANDAN, IZIN MELAPORKAN HASIL APEL MALAM SIAGA PENGECEKAN PERSONEL"))

        // Requirement: NO bottom date line in reports
        assertFalse(reportPagi.endsWith("31/10/2026"))
        assertFalse(reportPagi.contains("Magelang, 31/10/2026"))

        // 2. Configure custom officers: PA JAGA, BA JAGA, TA JAGA
        val customConfig = ReportTemplateConfig(
            formatType = ReportFormatType.STANDAR_KOMANDO,
            namaPaJaga = "LETDA KAV SURYA DHARMA",
            namaBaJaga = "SERKA AGUS PRASETYO",
            namaTaJaga = "PRAKA BUDI SANTOSO"
        )
        repo.saveReportConfig(customConfig)

        // 3. Generate report text and verify custom officers appear
        val reportCustom = repo.generateFormattedReport("2026-10-31", WaktuApel.MALAM_SIAGA, initial, statuses, customConfig)
        assertTrue(reportCustom.contains("PEJABAT JAGA:"))
        assertTrue(reportCustom.contains("PA JAGA") && reportCustom.contains("LETDA KAV SURYA DHARMA"))
        assertTrue(reportCustom.contains("BA JAGA") && reportCustom.contains("SERKA AGUS PRASETYO"))
        assertTrue(reportCustom.contains("TA JAGA") && reportCustom.contains("PRAKA BUDI SANTOSO"))

        // Verify Word file generation contains PA JAGA, BA JAGA, TA JAGA
        val wordFile = repo.exportToWordFile("2026-10-31", WaktuApel.MALAM_SIAGA, initial, statuses)
        val wordContent = wordFile.readText(Charsets.UTF_8)
        assertTrue(wordContent.contains("PA JAGA"))
        assertTrue(wordContent.contains("BA JAGA"))
        assertTrue(wordContent.contains("TA JAGA"))
        assertTrue(wordContent.contains("LETDA KAV SURYA DHARMA"))
        assertTrue(wordContent.contains("SERKA AGUS PRASETYO"))
        assertTrue(wordContent.contains("PRAKA BUDI SANTOSO"))

        // 4. Export config to JSON and verify officers are included
        val exportedJson = repo.exportAppConfigJsonString("2026-10-31", WaktuApel.MALAM_SIAGA, initial)
        assertTrue(exportedJson.contains("LETDA KAV SURYA DHARMA"))
        assertTrue(exportedJson.contains("SERKA AGUS PRASETYO"))
        assertTrue(exportedJson.contains("PRAKA BUDI SANTOSO"))

        // 5. Reset config to default: verify names are cleared
        repo.saveReportConfig(ReportTemplateConfig())
        val resetConfig = repo.loadReportConfig()
        assertEquals("", resetConfig.namaPaJaga)
        assertEquals("", resetConfig.namaBaJaga)
        assertEquals("", resetConfig.namaTaJaga)

        // 6. Import the exported JSON and verify custom officers are restored
        val importResult = repo.importAppConfigFromJson(exportedJson)
        assertTrue(importResult.success)
        val restoredConfig = repo.loadReportConfig()
        assertEquals("LETDA KAV SURYA DHARMA", restoredConfig.namaPaJaga)
        assertEquals("SERKA AGUS PRASETYO", restoredConfig.namaBaJaga)
        assertEquals("PRAKA BUDI SANTOSO", restoredConfig.namaTaJaga)

        // Clean up
        repo.saveReportConfig(ReportTemplateConfig())
    }

    @Test
    fun `verify date formatting across reports is (HARI, BULAN, TAHUN) and no bottom date line`() {
        val testDate = "2026-10-31" // Saturday
        val formatted = com.example.util.DateUtils.formatDateHariBulanTahun(testDate)
        assertEquals("SABTU, 31/10/2026", formatted)

        val dmy = com.example.util.DateUtils.formatDateDmy(testDate)
        assertEquals("31/10/2026", dmy)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = AttendanceRepository(context)
        val initial = repo.getInitialAttendance()
        val statuses = repo.loadCustomStatuses()

        // Standar Komando report has Hari/Tanggal: SABTU, 31/10/2026 in header block
        val report = repo.generateFormattedReport(testDate, WaktuApel.PAGI, initial, statuses, ReportTemplateConfig())
        assertTrue(report.contains("Hari/Tanggal: SABTU, 31/10/2026"))

        // When duty officers are empty, no example duty officer text should appear
        assertFalse(report.contains("LETTU KAV ARYA PRATAMA"))
        assertFalse(report.contains("SERKA WAHYU HIDAYAT"))
        assertFalse(report.contains("PEJABAT JAGA:"))
        assertFalse(report.contains("Ambarawa"))

        // Requirement: NO bottom date line
        assertFalse(report.trim().endsWith("31/10/2026"))
    }

    @Test
    fun `verify Pejabat Piket format with ketPaJaga custom when PA JAGA absent and bawaan BA JAGA, TA JAGA`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = AttendanceRepository(context)
        val initial = repo.getInitialAttendance()
        val statuses = repo.loadCustomStatuses()

        // 1. Case: PA JAGA is absent/custom label (e.g. "NIHIL" or "PAWAS"), with custom BA and TA names
        val absentConfig = ReportTemplateConfig(
            ketPaJaga = "NIHIL",
            namaPaJaga = "",
            namaBaJaga = "SERKA AGUS PRASETYO",
            namaTaJaga = "PRAKA BUDI SANTOSO"
        )
        val reportAbsent = repo.generateFormattedReport("2026-10-31", WaktuApel.PAGI, initial, statuses, absentConfig)
        assertTrue(reportAbsent.contains("PA JAGA") && reportAbsent.contains("NIHIL"))
        assertTrue(reportAbsent.contains("BA JAGA") && reportAbsent.contains("SERKA AGUS PRASETYO"))
        assertTrue(reportAbsent.contains("TA JAGA") && reportAbsent.contains("PRAKA BUDI SANTOSO"))

        // 2. Case: PA JAGA present with custom name and label
        val presentConfig = ReportTemplateConfig(
            ketPaJaga = "PA JAGA",
            namaPaJaga = "LETDA KAV SURYA DHARMA",
            namaBaJaga = "SERKA AGUS PRASETYO",
            namaTaJaga = "PRAKA BUDI SANTOSO"
        )
        val reportPresent = repo.generateFormattedReport("2026-10-31", WaktuApel.PAGI, initial, statuses, presentConfig)
        assertTrue(reportPresent.contains("PA JAGA") && reportPresent.contains("LETDA KAV SURYA DHARMA"))
        assertTrue(reportPresent.contains("BA JAGA") && reportPresent.contains("SERKA AGUS PRASETYO"))
        assertTrue(reportPresent.contains("TA JAGA") && reportPresent.contains("PRAKA BUDI SANTOSO"))

        // 3. Verify Word file export carries the PA JAGA label and BA/TA fixed labels
        val wordFile = repo.exportToWordFile("2026-10-31", WaktuApel.PAGI, initial, statuses, presentConfig)
        val wordContent = wordFile.readText(Charsets.UTF_8)
        assertTrue(wordContent.contains("PA JAGA"))
        assertTrue(wordContent.contains("BA JAGA"))
        assertTrue(wordContent.contains("TA JAGA"))
        assertTrue(wordContent.contains("LETDA KAV SURYA DHARMA"))

        // 4. Verify Export & Import JSON includes ketPaJaga
        repo.saveReportConfig(absentConfig)
        val jsonConfig = repo.exportAppConfigJsonString("2026-10-31", WaktuApel.PAGI, initial)
        assertTrue(jsonConfig.contains("\"ketPaJaga\": \"NIHIL\""))

        // Clear and restore
        repo.saveReportConfig(ReportTemplateConfig())
        val importResult = repo.importAppConfigFromJson(jsonConfig)
        assertTrue(importResult.success)
        val restored = repo.loadReportConfig()
        assertEquals("NIHIL", restored.ketPaJaga)
        assertEquals("", restored.namaPaJaga)
        assertEquals("SERKA AGUS PRASETYO", restored.namaBaJaga)
        assertEquals("PRAKA BUDI SANTOSO", restored.namaTaJaga)

        // Reset config
        repo.saveReportConfig(ReportTemplateConfig())
    }

    @Test
    fun `verify multi group status synchronization across entire unit in AttendanceViewModel`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = AttendanceViewModel(app)

        // Find a soldier in current attendanceData
        val initialMap = vm.uiState.value.attendanceData
        assertFalse(initialMap.isEmpty())

        // Choose a soldier that can appear in multiple groups or test across groups
        val firstGroupKey = initialMap.keys.first()
        val firstPerson = initialMap[firstGroupKey]!!.first()

        // Update their status
        vm.updateStatus(firstGroupKey, firstPerson.id, "ISDD", firstPerson.name)

        val updatedMap = vm.uiState.value.attendanceData
        var matchCount = 0
        for ((_, list) in updatedMap) {
            for (p in list) {
                if (p.name.equals(firstPerson.name, ignoreCase = true) || p.id == firstPerson.id) {
                    assertEquals("ISDD", p.status)
                    matchCount++
                }
            }
        }
        assertTrue("Matching person should have their status updated", matchCount >= 1)
    }

    @Test
    fun `verify multiline Enter support in report formats and Word export`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = AttendanceRepository(context)
        val initial = repo.getInitialAttendance()
        val statuses = repo.loadCustomStatuses()

        // Report config with multiple lines (Enters) in header, salam, and duty officers
        val multilineConfig = ReportTemplateConfig(
            headerSatuan = "KOMPI KAVALERI 2/JRTR\nKODAM XXVI/MATARAM",
            salamPembuka = "SELAMAT PAGI KOMANDAN\nIZIN MELAPORKAN HASIL APEL",
            salamPenutup = "Demikian kami laporkan.\nMohon petunjuk lebih lanjut.",
            namaPaJaga = "LETDA KAV SURYA DHARMA\nNRP 1118001234",
            namaBaJaga = "SERKA AGUS PRASETYO\nBATON 1",
            namaTaJaga = "PRAKA BUDI SANTOSO\nTA MOTORIS"
        )

        // 1. Standar Komando report text contains the newlines
        val reportText = repo.generateFormattedReport("2026-11-01", WaktuApel.PAGI, initial, statuses, multilineConfig)
        assertTrue(reportText.contains("KOMPI KAVALERI 2/JRTR\nKODAM XXVI/MATARAM"))
        assertTrue(reportText.contains("SELAMAT PAGI KOMANDAN\nIZIN MELAPORKAN HASIL APEL"))
        assertTrue(reportText.contains("LETDA KAV SURYA DHARMA\nNRP 1118001234"))

        // 2. Word export (.doc) converts the newlines to HTML <br> tags
        val wordFile = repo.exportToWordFile("2026-11-01", WaktuApel.PAGI, initial, statuses, multilineConfig)
        val wordHtml = wordFile.readText(Charsets.UTF_8)
        assertTrue(wordHtml.contains("KOMPI KAVALERI 2/JRTR<br>KODAM XXVI/MATARAM"))
        assertTrue(wordHtml.contains("LETDA KAV SURYA DHARMA<br>NRP 1118001234"))
        assertTrue(wordHtml.contains("SERKA AGUS PRASETYO<br>BATON 1"))
        assertTrue(wordHtml.contains("PRAKA BUDI SANTOSO<br>TA MOTORIS"))
    }

    @Test
    fun `verify normalized soldier matching and REMAJA alignment`() {
        // Verify aliases match
        assertTrue(AttendanceConstants.isSameSoldier("BOYKE", "BOYKEY"))
        assertTrue(AttendanceConstants.isSameSoldier("SOFYAN", "SOFIAN"))
        assertTrue(AttendanceConstants.isSameSoldier("ELI", "ELY"))
        assertTrue(AttendanceConstants.isSameSoldier("KOLIANTO", "KHOLI"))
        assertTrue(AttendanceConstants.isSameSoldier("  DAFFA  ", "daffa"))

        // Verify REMAJA_DATA has exactly 43 soldiers
        assertEquals(43, AttendanceConstants.REMAJA_DATA.size)
        assertTrue(AttendanceConstants.REMAJA_DATA.contains("BOYKEY"))
        assertTrue(AttendanceConstants.REMAJA_DATA.contains("SOFIAN"))
        assertTrue(AttendanceConstants.REMAJA_DATA.contains("ELY"))
        assertTrue(AttendanceConstants.REMAJA_DATA.contains("KHOLI"))
        assertTrue(AttendanceConstants.REMAJA_DATA.contains("DAFFA"))
    }
}


