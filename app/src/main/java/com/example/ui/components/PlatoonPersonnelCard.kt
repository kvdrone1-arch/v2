package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import com.example.data.model.AttendanceConstants
import com.example.data.model.CustomStatusItem
import com.example.data.model.Person
import com.example.ui.theme.CavalryHadirCyan
import com.example.ui.theme.CavalryKurangRed

@Composable
fun PlatoonPersonnelCard(
    groupKey: String,
    persons: List<Person>,
    customStatuses: List<CustomStatusItem> = emptyList(),
    onPersonClicked: (Person) -> Unit,
    onMarkAllHadir: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayName = AttendanceConstants.DISPLAY_FULL_NAMES[groupKey] ?: groupKey
    val hadirCount = remember(persons) { persons.count { it.status == "HADIR" } }
    val kurangCount = remember(persons, hadirCount) { persons.size - hadirCount }
    val statusDescMap = remember(customStatuses) {
        customStatuses.associate { it.code to it.description }
    }

    // Group absentees by status (memoized)
    val (nonHadirMap, hadirList) = remember(persons) {
        val nonHadir = mutableMapOf<String, MutableList<Person>>()
        val hadir = mutableListOf<Person>()
        for (p in persons) {
            if (p.status == "HADIR") {
                hadir.add(p)
            } else {
                val list = nonHadir.getOrPut(p.status) { mutableListOf() }
                list.add(p)
            }
        }
        Pair(nonHadir, hadir)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("platoon_card_$groupKey"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Platoon Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFDBE5F1))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = displayName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "Total: ${persons.size} Org  •  Hadir: $hadirCount  •  Kurang: $kurangCount",
                        fontSize = 11.sp,
                        color = if (kurangCount > 0) CavalryKurangRed else Color(0xFF0F766E),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (kurangCount > 0) {
                    OutlinedButton(
                        onClick = onMarkAllHadir,
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("btn_all_hadir_$groupKey"),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 8.dp,
                            vertical = 0.dp
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Hadirkan",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Non-Hadir Personnel Section (Grouped under red status header)
            if (nonHadirMap.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                ) {
                    nonHadirMap.forEach { (status, pList) ->
                        val desc = (statusDescMap[status] ?: AttendanceConstants.DEFAULT_STATUS_DESCRIPTIONS[status] ?: "").trim()
                        val headerLabel = if (desc.isNotBlank() && !desc.equals(status, ignoreCase = true)) {
                            "KETERANGAN: $status ($desc) - ${pList.size} Org"
                        } else {
                            "KETERANGAN: $status - ${pList.size} Org"
                        }
                        // Red military status bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFF1F2))
                                .border(0.5.dp, Color(0xFFFECDD3))
                                .padding(horizontal = 12.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = headerLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = CavalryKurangRed
                            )
                        }

                        // Personnel rows in this status
                        pList.forEachIndexed { index, person ->
                            PersonListItem(
                                index = index + 1,
                                person = person,
                                isHadir = false,
                                onClick = { onPersonClicked(person) }
                            )
                        }
                    }
                }
            }

            // Hadir Section (Blue military status bar)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CavalryHadirCyan)
                    .padding(horizontal = 12.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "HADIR (${hadirList.size} Orang)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }

            // Hadir personnel rows
            if (hadirList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ada personel yang hadir di peleton ini",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    hadirList.forEachIndexed { index, person ->
                        PersonListItem(
                            index = index + 1,
                            person = person,
                            isHadir = true,
                            onClick = { onPersonClicked(person) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonListItem(
    index: Int,
    person: Person,
    isHadir: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("person_row_${person.id}"),
        color = if (isHadir) Color.White else Color(0xFFFEF2F2)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "$index.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    modifier = Modifier.width(26.dp)
                )
                Text(
                    text = person.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isHadir) Color(0xFF0F172A) else CavalryKurangRed
                )
            }

            // Status Badge and Edit Icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isHadir) Color(0xFFE0F2FE) else Color(0xFFFEE2E2)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = person.status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isHadir) Color(0xFF0369A1) else CavalryKurangRed
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Ubah Status",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(Color(0xFFE2E8F0))
    )
}
