package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceConstants
import com.example.data.model.CustomStatusItem
import com.example.data.model.Person
import com.example.ui.theme.CavalryHadirCyan
import com.example.ui.theme.CavalryKurangRed

@Composable
fun SummaryTable(
    attendanceData: Map<String, List<Person>>,
    customStatuses: List<CustomStatusItem>,
    modifier: Modifier = Modifier
) {
    val horizontalScrollState = rememberScrollState()
    val statusCodes = remember(customStatuses) { customStatuses.map { it.code } }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .testTag("summary_table_card"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = "TABEL REKAPITULASI ABSEN APEL",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Scrollable Matrix Table
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScrollState)
            ) {
                Column(modifier = Modifier.border(1.dp, Color(0xFF000000))) {
                    // Header Row 1: NO, SATUAN, JML, KETERANGAN
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF1E293B))
                            .height(28.dp)
                    ) {
                        TableCell("NO", width = 36, isHeader = true, textColor = Color.White)
                        TableCell("SATUAN", width = 80, isHeader = true, textColor = Color.White)
                        TableCell("JML", width = 45, isHeader = true, textColor = Color.White)
                        TableCell("KETERANGAN", width = 42 * (statusCodes.size + 2), isHeader = true, textColor = Color.White)
                    }

                    // Header Row 2: KRG, HDR, statusCodes
                    Row(
                        modifier = Modifier
                            .background(Color(0xFFE2E8F0))
                            .height(26.dp)
                    ) {
                        TableCell("", width = 36, isHeader = true, textColor = Color(0xFF0F172A))
                        TableCell("", width = 80, isHeader = true, textColor = Color(0xFF0F172A))
                        TableCell("", width = 45, isHeader = true, textColor = Color(0xFF0F172A))
                        TableCell("KRG", width = 42, isHeader = true, textColor = CavalryKurangRed)
                        TableCell("HDR", width = 42, isHeader = true, textColor = Color(0xFF0284C7))
                        statusCodes.forEach { status ->
                            TableCell(status, width = 42, isHeader = true, textColor = Color(0xFF0F172A))
                        }
                    }

                    // Body Rows
                    var totalAll = 0
                    var totalHadir = 0
                    var totalKurang = 0
                    val statusTotals = mutableMapOf<String, Int>()
                    statusCodes.forEach { statusTotals[it] = 0 }

                    var rowIndex = 1
                    for ((groupKey, persons) in attendanceData) {
                        val groupTotal = persons.size
                        val hadirCount = persons.count { it.status == "HADIR" }
                        val kurangCount = groupTotal - hadirCount
                        val displayName = AttendanceConstants.DISPLAY_NAMES[groupKey] ?: groupKey

                        totalAll += groupTotal
                        totalHadir += hadirCount
                        totalKurang += kurangCount

                        val statusCounts = mutableMapOf<String, Int>()
                        statusCodes.forEach { statusCounts[it] = 0 }
                        for (p in persons) {
                            if (p.status != "HADIR" && statusCounts.containsKey(p.status)) {
                                statusCounts[p.status] = (statusCounts[p.status] ?: 0) + 1
                                statusTotals[p.status] = (statusTotals[p.status] ?: 0) + 1
                            }
                        }

                        val isEven = rowIndex % 2 == 0
                        Row(
                            modifier = Modifier
                                .background(if (isEven) Color(0xFFF8FAFC) else Color.White)
                                .height(26.dp)
                        ) {
                            TableCell((rowIndex++).toString(), width = 36, isHeader = false)
                            TableCell(displayName, width = 80, isHeader = false, isBold = true)
                            TableCell(groupTotal.toString(), width = 45, isHeader = false, isBold = true)
                            TableCell(
                                text = if (kurangCount > 0) kurangCount.toString() else "-",
                                width = 42,
                                isHeader = false,
                                isBold = true,
                                textColor = if (kurangCount > 0) CavalryKurangRed else Color(0xFF64748B),
                                bgColor = if (kurangCount > 0) Color(0xFFFFF1F2) else null
                            )
                            TableCell(
                                text = if (hadirCount > 0) hadirCount.toString() else "-",
                                width = 42,
                                isHeader = false,
                                isBold = true,
                                textColor = if (hadirCount > 0) Color(0xFF0369A1) else Color(0xFF64748B),
                                bgColor = if (hadirCount > 0) Color(0xFFF0F9FF) else null
                            )

                            statusCodes.forEach { status ->
                                val count = statusCounts[status] ?: 0
                                TableCell(
                                    text = if (count > 0) count.toString() else "",
                                    width = 42,
                                    isHeader = false,
                                    isBold = count > 0,
                                    textColor = if (count > 0) CavalryKurangRed else Color.Black,
                                    bgColor = if (count > 0) Color(0xFFFEF2F2) else null
                                )
                            }
                        }
                    }

                    // Total Row (JUMLAH KESELURUHAN)
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF1E293B))
                            .height(28.dp)
                    ) {
                        TableCell("JUMLAH KESELURUHAN", width = 36 + 80, isHeader = true, textColor = Color.White)
                        TableCell(totalAll.toString(), width = 45, isHeader = true, textColor = Color.White)
                        TableCell(
                            text = totalKurang.toString(),
                            width = 42,
                            isHeader = true,
                            textColor = Color(0xFFEF4444),
                            bgColor = Color(0xFFFEE2E2)
                        )
                        TableCell(
                            text = totalHadir.toString(),
                            width = 42,
                            isHeader = true,
                            textColor = Color(0xFF0284C7),
                            bgColor = Color(0xFFE0F2FE)
                        )

                        statusCodes.forEach { status ->
                            val count = statusTotals[status] ?: 0
                            TableCell(
                                text = if (count > 0) count.toString() else "",
                                width = 42,
                                isHeader = true,
                                textColor = if (count > 0) Color(0xFFF87171) else Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TableCell(
    text: String,
    width: Int,
    isHeader: Boolean = false,
    isBold: Boolean = false,
    textColor: Color = Color.Black,
    bgColor: Color? = null
) {
    Box(
        modifier = Modifier
            .width(width.dp)
            .border(0.5.dp, Color(0xFF475569))
            .then(if (bgColor != null) Modifier.background(bgColor) else Modifier)
            .padding(horizontal = 2.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = if (isHeader) 9.sp else 10.sp,
            fontWeight = if (isHeader || isBold) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
