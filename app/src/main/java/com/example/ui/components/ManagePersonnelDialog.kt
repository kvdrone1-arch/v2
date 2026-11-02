package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AttendanceConstants
import com.example.ui.theme.CavalryGold
import com.example.ui.theme.CavalryGreenDark
import com.example.ui.theme.CavalryGreenPrimary
import com.example.ui.theme.CavalryKurangRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePersonnelDialog(
    platoonRoster: Map<String, List<String>>,
    remajaRoster: List<String>,
    onAddMember: (groupKey: String, name: String, isRemaja: Boolean) -> Unit,
    onRemoveMember: (groupKey: String, name: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    // Tambah Anggota State
    val platoonKeys = listOf("PIMPINAN", "KOKI", "SIMA", "TON I", "TON II", "TON III")
    var selectedPlatoonForAdd by remember { mutableStateOf("TON I") }
    var inputName by remember { mutableStateOf("") }
    var isAlsoRemaja by remember { mutableStateOf(false) }

    // Hapus Anggota State
    var searchQuery by remember { mutableStateOf("") }
    var filterPlatoonForDelete by remember { mutableStateOf("SEMUA") }
    var memberToDelete by remember { mutableStateOf<Pair<String, String>?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, CavalryGold, RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CavalryGreenDark)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "KELOLA PERSONEL SATUAN",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = CavalryGold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Tambah & Hapus Anggota (Sinkron Otomatis)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFF1F5F9)
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("close_manage_personnel")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Tabs: Tambah vs Hapus
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF1E293B),
                    contentColor = CavalryGold,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = CavalryGold,
                            height = 3.dp
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (selectedTab == 0) CavalryGold else Color.Gray
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "TAMBAH ANGGOTA",
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTab == 0) Color.White else Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PersonRemove,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (selectedTab == 1) Color(0xFFEF4444) else Color.Gray
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "HAPUS ANGGOTA",
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTab == 1) Color.White else Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    )
                }

                // Content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    if (selectedTab == 0) {
                        // --- TAB 1: TAMBAH ANGGOTA ---
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "1. Pilih Peleton / Bagian:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Platoon Selector Chips
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(platoonKeys) { key ->
                                    val isSelected = selectedPlatoonForAdd == key
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedPlatoonForAdd = key },
                                        label = {
                                            Text(
                                                text = AttendanceConstants.DISPLAY_NAMES[key] ?: key,
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CavalryGreenDark,
                                            selectedLabelColor = CavalryGold,
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "2. Nama Lengkap & Pangkat:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            OutlinedTextField(
                                value = inputName,
                                onValueChange = { inputName = it },
                                placeholder = { Text("Nama Lengkap & Pangkat") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_new_member_name"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = CavalryGold,
                                    unfocusedBorderColor = Color.Gray
                                )
                            )

                            // Checkbox Remaja
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = isAlsoRemaja,
                                    onCheckedChange = { isAlsoRemaja = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF16A34A))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Column {
                                    Text(
                                        text = "Daftarkan juga sebagai Tamtama Remaja (REMAJA)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Akan otomatis tampil di daftar Apel Malam Siaga",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Button(
                                onClick = {
                                    if (inputName.isNotBlank()) {
                                        onAddMember(selectedPlatoonForAdd, inputName.trim(), isAlsoRemaja)
                                        inputName = ""
                                        isAlsoRemaja = false
                                    }
                                },
                                enabled = inputName.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_confirm_add_member"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CavalryGreenDark,
                                    contentColor = CavalryGold
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SIMPAN ANGGOTA KE $selectedPlatoonForAdd",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        // --- TAB 2: HAPUS ANGGOTA ---
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Search Box
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Cari nama personel...") },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear")
                                        }
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = CavalryGold,
                                    unfocusedBorderColor = Color.Gray
                                )
                            )

                            // Platoon Filter Row
                            val deleteFilterOptions = listOf("SEMUA") + platoonKeys + listOf("REMAJA")
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(deleteFilterOptions) { opt ->
                                    val isSelected = filterPlatoonForDelete == opt
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { filterPlatoonForDelete = opt },
                                        label = { Text(opt, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = if (opt == "REMAJA") Color(0xFFD97706) else CavalryGreenDark,
                                            selectedLabelColor = if (opt == "REMAJA") Color.White else CavalryGold
                                        )
                                    )
                                }
                            }

                            // Build personnel items to display
                            val itemsToShow = remember(platoonRoster, remajaRoster, filterPlatoonForDelete, searchQuery) {
                                val list = mutableListOf<Pair<String, String>>() // (groupKey, name)
                                if (filterPlatoonForDelete == "SEMUA") {
                                    for ((key, names) in platoonRoster) {
                                        for (name in names) {
                                            list.add(Pair(key, name))
                                        }
                                    }
                                } else if (filterPlatoonForDelete == "REMAJA") {
                                    for (name in remajaRoster) {
                                        list.add(Pair("REMAJA", name))
                                    }
                                } else {
                                    val names = platoonRoster[filterPlatoonForDelete] ?: emptyList()
                                    for (name in names) {
                                        list.add(Pair(filterPlatoonForDelete, name))
                                    }
                                }

                                if (searchQuery.isNotBlank()) {
                                    list.filter { it.second.contains(searchQuery.trim(), ignoreCase = true) }
                                } else {
                                    list
                                }
                            }

                            Text(
                                text = "Ditemukan: ${itemsToShow.size} personel",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(itemsToShow) { (groupKey, name) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        if (groupKey == "REMAJA") Color(0xFFD97706) else CavalryGreenDark,
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = AttendanceConstants.DISPLAY_NAMES[groupKey] ?: groupKey,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (groupKey == "REMAJA") Color.White else CavalryGold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = name,
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        IconButton(
                                            onClick = { memberToDelete = Pair(groupKey, name) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Hapus $name",
                                                tint = Color(0xFFEF4444)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Alert Dialog for Deletion
    memberToDelete?.let { (groupKey, name) ->
        AlertDialog(
            onDismissRequest = { memberToDelete = null },
            title = {
                Text(
                    text = "Konfirmasi Hapus Anggota",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDC2626)
                )
            },
            text = {
                Text("Apakah Anda yakin ingin menghapus $name dari data $groupKey? Tindakan ini akan langsung mensinkronkan daftar hadir aktif.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveMember(groupKey, name)
                        memberToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Ya, Hapus", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}
