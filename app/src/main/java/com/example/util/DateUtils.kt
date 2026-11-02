package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Utility for formatting and parsing dates across the military attendance application.
 * All user-facing dates are formatted as (HARI/BULAN/TAHUN) according to Indonesian standards.
 */
object DateUtils {

    private val INDO_DAYS = arrayOf(
        "MINGGU", // 1 Calendar.SUNDAY
        "SENIN",  // 2 Calendar.MONDAY
        "SELASA", // 3
        "RABU",   // 4
        "KAMIS",  // 5
        "JUMAT",  // 6
        "SABTU"   // 7
    )

    /**
     * Parses dates supporting both internal yyyy-MM-dd and user dd/MM/yyyy formats.
     */
    fun parseDate(dateStr: String): Calendar? {
        if (dateStr.isBlank()) return null
        val patterns = listOf("yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy", "yyyy/MM/dd")
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                sdf.isLenient = false
                val date = sdf.parse(dateStr.trim())
                if (date != null) {
                    val cal = Calendar.getInstance()
                    cal.time = date
                    return cal
                }
            } catch (_: Exception) {}
        }
        return null
    }

    /**
     * Formats date into (HARI/BULAN/TAHUN), e.g.: "SABTU, 31/10/2026"
     */
    fun formatDateHariBulanTahun(dateStr: String): String {
        val cal = parseDate(dateStr) ?: return dateStr
        val dayOfWeekIndex = cal.get(Calendar.DAY_OF_WEEK) - 1
        val dayName = INDO_DAYS.getOrElse(dayOfWeekIndex) { "HARI" }
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)
        return String.format(Locale.getDefault(), "%s, %02d/%02d/%04d", dayName, dayOfMonth, month, year)
    }

    /**
     * Formats date into Day/Month/Year numbers, e.g.: "31/10/2026"
     */
    fun formatDateDmy(dateStr: String): String {
        val cal = parseDate(dateStr) ?: return dateStr
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)
        return String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month, year)
    }

    /**
     * Adjusts date by adding or subtracting days, keeping yyyy-MM-dd internal consistency.
     */
    fun adjustDate(dateStr: String, daysToAdd: Int): String {
        val cal = parseDate(dateStr) ?: Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, daysToAdd)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(cal.time)
    }

    /**
     * Gets today's date in GMT+7 (WIB).
     */
    fun getTodayDateWib(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("GMT+7")
        return sdf.format(Date())
    }
}
