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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.CustomStatusItem
import com.example.ui.theme.CavalryGold
import com.example.ui.theme.CavalryGreenDark
import com.example.ui.theme.CavalryGreenPrimary
import com.example.ui.theme.CavalryKurangRed

@Composable
fun ManageStatusesDialog(
    customStatuses: List<CustomStatusItem>,
    onRenameOrUpdateStatus: (oldCode: String, newCode: String, newDescription: String) -> Unit,
    onAddNewStatus: (code: String, description: String) -> Unit,
    onDeleteStatus: (code: String) -> Unit,
    onResetToDefault: () -> Unit,
    onDismiss: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<CustomStatusItem?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("manage_statuses_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "KELOLA & KUSTOMISASI KETERANGAN",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CavalryGreenDark
                        )
                        Text(
                            text = "Ganti nama kode, arti keterangan, atau tambah baru",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Bar: Add New + Reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("btn_add_custom_status"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CavalryGreenPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tambah Keterangan",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { showResetConfirm = true },
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("btn_reset_statuses"),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Reset Default",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // List of Statuses
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                        .padding(4.dp)
                ) {
                    items(customStatuses) { status ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (status.isDefault) Color(0xFFF8FAFC) else Color(0xFFFFFBEB)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                if (status.isDefault) Color(0xFFE2E8F0) else CavalryGold.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFFEF2F2))
                                            .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = status.code,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = CavalryKurangRed
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = status.description,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF1E293B)
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { itemToEdit = status },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Ganti Nama",
                                            tint = CavalryGreenPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    if (!status.isDefault || customStatuses.size > 10) {
                                        IconButton(
                                            onClick = { onDeleteStatus(status.code) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Hapus",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CavalryGreenDark)
                ) {
                    Text("Selesai & Tutup", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Modal to Edit/Rename Status
    itemToEdit?.let { status ->
        EditStatusModal(
            statusItem = status,
            onSave = { newCode, newDesc ->
                onRenameOrUpdateStatus(status.code, newCode, newDesc)
                itemToEdit = null
            },
            onDismiss = { itemToEdit = null }
        )
    }

    // Modal to Add New Status
    if (showAddDialog) {
        AddNewStatusModal(
            onAdd = { code, desc ->
                onAddNewStatus(code, desc)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // Confirm Reset Dialog
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset ke Bawaan?", fontWeight = FontWeight.Bold) },
            text = { Text("Semua nama keterangan akan dikembalikan ke 25 kode bawaan standar militer.") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetToDefault()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CavalryKurangRed)
                ) {
                    Text("Ya, Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun EditStatusModal(
    statusItem: CustomStatusItem,
    onSave: (newCode: String, newDescription: String) -> Unit,
    onDismiss: () -> Unit
) {
    var codeInput by remember { mutableStateOf(statusItem.code) }
    var descInput by remember { mutableStateOf(statusItem.description) }
    var errorText by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "GANTI NAMA KETERANGAN",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CavalryGreenDark
                )
                Text(
                    text = "Perubahan kode akan otomatis mengupdate personel yang menggunakan status ini.",
                    fontSize = 10.sp,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = codeInput,
                    onValueChange = {
                        codeInput = it.uppercase()
                        errorText = null
                    },
                    label = { Text("Kode Keterangan (Singkatan)") },
                    placeholder = { Text("cth: ISDD, DINAS, SATGAS") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = descInput,
                    onValueChange = {
                        descInput = it
                        errorText = null
                    },
                    label = { Text("Kepanjangan / Arti Keterangan") },
                    placeholder = { Text("cth: Izin Sedang Dinas Dalam") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                errorText?.let {
                    Text(
                        text = it,
                        color = CavalryKurangRed,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = {
                            val trimmedCode = codeInput.trim()
                            val trimmedDesc = descInput.trim()
                            if (trimmedCode.isBlank()) {
                                errorText = "Kode keterangan tidak boleh kosong"
                            } else {
                                onSave(trimmedCode, trimmedDesc)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CavalryGreenPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Simpan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddNewStatusModal(
    onAdd: (code: String, description: String) -> Unit,
    onDismiss: () -> Unit
) {
    var codeInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "TAMBAH KETERANGAN BARU",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CavalryGreenDark
                )
                Text(
                    text = "Masukkan kode dan arti status dinas/ketidakhadiran baru.",
                    fontSize = 10.sp,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = codeInput,
                    onValueChange = {
                        codeInput = it.uppercase()
                        errorText = null
                    },
                    label = { Text("Kode Keterangan (Singkatan)") },
                    placeholder = { Text("cth: SATGAS, BKO-POL, PIKET") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = descInput,
                    onValueChange = {
                        descInput = it
                        errorText = null
                    },
                    label = { Text("Kepanjangan / Arti Keterangan") },
                    placeholder = { Text("cth: Penugasan Satgas Operasi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                errorText?.let {
                    Text(
                        text = it,
                        color = CavalryKurangRed,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = {
                            val trimmedCode = codeInput.trim()
                            val trimmedDesc = descInput.trim()
                            if (trimmedCode.isBlank()) {
                                errorText = "Kode tidak boleh kosong"
                            } else {
                                onAdd(trimmedCode, trimmedDesc)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CavalryGreenPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Tambahkan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
