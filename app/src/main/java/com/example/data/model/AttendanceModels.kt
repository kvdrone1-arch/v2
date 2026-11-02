package com.example.data.model

data class Person(
    val id: Int,
    val name: String,
    val status: String
)

enum class WaktuApel(val label: String) {
    PAGI("PAGI"),
    SIANG("SIANG"),
    SORE("SORE"),
    MALAM_SIAGA("MALAM SIAGA")
}

data class CustomStatusItem(
    val code: String,
    val description: String,
    val isDefault: Boolean = true
)

enum class ReportFormatType(val title: String) {
    STANDAR_KOMANDO("Standar Komando"),
    RINGKAS_WA("Ringkas WhatsApp"),
    FORMAL_DINAS("Formal Dinas"),
    CUSTOM("Format Kustom")
}

data class ReportTemplateConfig(
    val formatType: ReportFormatType = ReportFormatType.STANDAR_KOMANDO,
    val headerKomando: String = AttendanceConstants.SATUAN_KOMANDO,
    val headerSatuan: String = AttendanceConstants.SATUAN_UNIT,
    val reportTitle: String = "LAPORAN ABSEN APEL",
    val reportSubtitle: String = "PENGECEKAN PERSONEL KIKAV 2/JRTR",
    val salamPembuka: String = "SELAMAT PAGI KOMANDAN, IZIN MELAPORKAN HASIL APEL PAGI PENGECEKAN PERSONEL",
    val salamPenutup: String = "Demikian dilaporkan, tertanda Piket Siaga KIKAV 2/JRTR.",
    val wordHeaderFontSize: Int = 16,
    val wordTableFontSize: Float = 6.5f,
    val wordPersonnelFontSize: Float = 7.0f,
    val labelKuatNyata: String = "Kuat Nyata",
    val labelHadir: String = "Hadir",
    val labelKurang: String = "Kurang",
    val customTemplateText: String = "",
    val ketPaJaga: String = "PA JAGA",
    val namaPaJaga: String = "",
    val namaBaJaga: String = "",
    val namaTaJaga: String = "",
    val namaPerwiraPiket: String = "",
    val jabatanPerwiraPiket: String = "",
    val namaBintaraPiket: String = "",
    val jabatanBintaraPiket: String = "",
    val lokasiTandaTangan: String = ""
)

enum class UserRole(val displayName: String, val levelTitle: String) {
    ADMIN("ADMIN (KOMANDO)", "AKSES PENUH KOMANDO")
}

data class AuthSession(
    val username: String,
    val role: UserRole = UserRole.ADMIN,
    val displayName: String = "ADMIN KIKAV 2",
    val isLoggedIn: Boolean = false
)

object AttendanceConstants {
    const val GAS_WEB_APP_URL =
        "https://script.google.com/macros/s/AKfycbxCctZPRAGGnvfFnEUUtxNezDAcqE2fhE7efAIh_1vr6Nk9vdtNvhjWmw3pK915Oxc_/exec"

    const val SATUAN_KOMANDO = "KOMANDO DAERAH MILITER XXVI/MATARAM"
    const val SATUAN_UNIT = "KOMPI KAVALERI 2/JAYENG RATA TOH RAGA"
    const val APEL_TITLE = "PENGECEKAN PERSONEL KIKAV 2/JRTR"

    // Regular Platoons Data (PAGI, SIANG, SORE)
    val INITIAL_DATA = linkedMapOf(
        "PIMPINAN" to listOf(
            "DANKI", "TEGUH", "RANGGA", "RAHMAT", "SANDA", "GALIH", "HERU"
        ),
        "SIMA" to listOf(
            "KHOIRUL", "DEDI", "IWAN F", "EKO WBU", "KLEOVIN", "DAFFA", "KRISWAN",
            "ANGGIT", "ALDI", "SUGENG", "YOGA RA", "RIFAI", "LAKSONO", "GALANG",
            "RENDY", "VALENTIKO", "SUHANDA", "VIRA", "SASONGKO", "PRAYUDHA", "DANI",
            "BOYKE", "LILIK", "JONI", "TRIANDI", "FAHRIL", "JALIL", "SARDAN",
            "DANDI", "AKBAR", "BAYU S", "YOFA"
        ),
        "TON I" to listOf(
            "RIKI S", "DIMAS", "ADI PRAS", "LUTFI", "DANTON1", "BAHRUL", "HABIB",
            "GERRAD", "OKKY", "HENDRI", "LUKMAN", "YUSUF", "KRISKI", "DIKI",
            "SINURAT", "BIMO", "FREDI", "FERLI", "RAFLY", "YULIANTO", "VITO", "EDI"
        ),
        "TON II" to listOf(
            "FARID", "FENDI", "AGUNG", "PIRMAN", "HENDRA", "BIMA", "GIGIH",
            "DANTON2", "RENO", "ZEGA", "FAISOL", "ELI", "RIZKI", "VERI",
            "HUDA", "RUDY", "MAULANA", "PUTRA", "ZAENAL", "TARMIZI", "YOGI",
            "TEGAR", "WAHYU"
        ),
        "TON III" to listOf(
            "REZA", "SOFYAN", "KOLIANTO", "ARIS", "ISMAIL", "FERIKA", "ROMA",
            "FAUZI", "LATIF", "FAJAR", "NAZIR", "FANDI", "BAGAS", "RAHMAN",
            "M YOGA", "PRABOWO", "ADITYA", "WAFI", "SAGALA", "RISALDI"
        ),
        "LF" to listOf(
            "ALFIUS"
        )
    )

    // Data 1 Apel Malam Siaga: REMAJA (43 Personel)
    val REMAJA_DATA = listOf(
        "DAFFA", "ANGGIT", "ALDI", "LUTFI", "REZA", "SOFIAN", "KHOLI", "DIMAS",
        "FENDI", "AGUNG", "KLEOVIN", "HENDRA", "ROMA", "ALFIUS", "HABIB", "BOYKEY",
        "RANGGA", "GIGIH", "VALENTIKO", "ADI PRAS", "SUGENG", "TARMIZI", "LUKMAN",
        "GALANG", "RAFLY", "FAISOL", "LAKSONO", "FAHRIL", "RIZKI", "GERRAD",
        "TEGAR", "RENDY", "PIRMAN", "AKBAR", "FANDI", "BAGAS", "ZEGA", "WAFI",
        "ELY", "ZAENAL", "SARDAN", "YOFA", "YOGI"
    )

    // Data 2 Apel Malam Siaga: Pilihan Peleton Siaga (Pilih Salah Satu dari 5 Satuan)
    val SIAGA_OPTIONS = listOf(
        "PIMPINAN", // KOKI
        "SIMA",     // SIMA
        "TON I",    // TON I
        "TON II",   // TON II
        "TON III"   // TON III
    )

    val DISPLAY_NAMES = mapOf(
        "REMAJA" to "REMAJA",
        "PIMPINAN" to "KOKI",
        "SIMA" to "SIMA",
        "TON I" to "TON I",
        "TON II" to "TON II",
        "TON III" to "TON III",
        "LF" to "LF"
    )

    val DISPLAY_FULL_NAMES = mapOf(
        "REMAJA" to "PERSONEL REMAJA (REMAJA)",
        "PIMPINAN" to "ESELON PIMPINAN (KOKI)",
        "SIMA" to "PELETON MARKAS (SIMA)",
        "TON I" to "PELETON I (TON I)",
        "TON II" to "PELETON II (TON II)",
        "TON III" to "PELETON III (TON III)",
        "LF" to "LUAR FORMASI (LF)"
    )

    // 22 Keterangan standar bawaan (Keterangan bersifat custom, tanpa arti otomatis)
    val DEFAULT_STATUS_LIST = listOf(
        "DD", "ISDD", "DL", "CT", "BP", "BKO", "DISERSI", "IZIN", "DIK", "LN",
        "DKT", "HUT", "ADC", "PAM", "KODAM", "BPTIH", "BPMIL",
        "GAR", "KANTOR", "SUS", "PROV", "SAKIT"
    )

    // Tanpa arti keterangan otomatis - bersifat custom murni
    val DEFAULT_STATUS_DESCRIPTIONS = emptyMap<String, String>()

    val STATUS_LIST: List<String> get() = DEFAULT_STATUS_LIST
    val STATUS_DESCRIPTIONS: Map<String, String> get() = DEFAULT_STATUS_DESCRIPTIONS

    fun normalizeMilitaryName(name: String): String {
        val clean = name.replace("\\s+".toRegex(), " ").trim().uppercase()
        return when (clean) {
            "BOYKEY", "BOYKE" -> "BOYKE"
            "SOFIAN", "SOFYAN" -> "SOFYAN"
            "ELY", "ELI" -> "ELI"
            "KHOLI", "KOLIANTO" -> "KOLIANTO"
            else -> clean
        }
    }

    fun isSameSoldier(name1: String?, name2: String?): Boolean {
        if (name1.isNullOrBlank() || name2.isNullOrBlank()) return false
        val n1 = normalizeMilitaryName(name1)
        val n2 = normalizeMilitaryName(name2)
        return n1 == n2
    }
}
