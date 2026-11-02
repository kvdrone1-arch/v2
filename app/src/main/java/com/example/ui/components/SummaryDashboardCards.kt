package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Person
import com.example.ui.theme.CavalryHadirCyan
import com.example.ui.theme.CavalryKurangRed

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SummaryDashboardCards(
    attendanceData: Map<String, List<Person>>,
    onMarkAllPresent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (totalPersonnel, totalHadir, statusBreakdown) = remember(attendanceData) {
        var total = 0
        var hadir = 0
        val breakdown = mutableMapOf<String, Int>()

        for ((_, persons) in attendanceData) {
            total += persons.size
            for (p in persons) {
                if (p.status == "HADIR") {
                    hadir++
                } else {
                    breakdown[p.status] = (breakdown[p.status] ?: 0) + 1
                }
            }
        }
        Triple(total, hadir, breakdown)
    }

    val totalKurang = totalPersonnel - totalHadir
    val hadirPercentage = if (totalPersonnel > 0) {
        (totalHadir.toFloat() / totalPersonnel.toFloat())
    } else 1f

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("summary_dashboard_card"),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Total
                MetricItem(
                    label = "KUAT NYATA",
                    value = totalPersonnel.toString(),
                    color = Color(0xFF1E293B),
                    bgColor = Color(0xFFF1F5F9),
                    icon = Icons.Default.Group,
                    modifier = Modifier.weight(1f)
                )

                // Hadir
                MetricItem(
                    label = "HADIR",
                    value = totalHadir.toString(),
                    color = Color(0xFF0369A1),
                    bgColor = Color(0xFFE0F2FE),
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )

                // Kurang
                MetricItem(
                    label = "KURANG",
                    value = totalKurang.toString(),
                    color = if (totalKurang > 0) CavalryKurangRed else Color(0xFF15803D),
                    bgColor = if (totalKurang > 0) Color(0xFFFEE2E2) else Color(0xFFDCFCE7),
                    icon = Icons.Default.Warning,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Persentase Kehadiran",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${(hadirPercentage * 100).toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hadirPercentage >= 0.95f) Color(0xFF15803D) else Color(0xFFD97706)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { hadirPercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = CavalryHadirCyan,
                trackColor = Color(0xFFE2E8F0)
            )

            // Non-Hadir status pills if any
            if (statusBreakdown.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Rincian Keterangan:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569)
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    statusBreakdown.forEach { (code, count) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFEF2F2))
                                .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$code: $count",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CavalryKurangRed
                            )
                        }
                    }
                }
            }

            // Quick Action Row
            if (totalKurang > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = onMarkAllPresent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .testTag("btn_mark_all_present"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFFECFDF5),
                        contentColor = Color(0xFF065F46)
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Setel Semua Hadir Lengkap ($totalPersonnel Org)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color,
    bgColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = color.copy(alpha = 0.85f)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}
