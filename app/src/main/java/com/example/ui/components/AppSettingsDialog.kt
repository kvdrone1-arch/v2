package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AttendanceConstants
import com.example.data.model.CustomStatusItem
import com.example.data.model.ReportFormatType
import com.example.data.model.ReportTemplateConfig
import com.example.data.repository.AttendanceRepository
import com.example.ui.theme.CavalryGold
import com.example.ui.theme.CavalryGreenDark
import com.example.ui.theme.CavalryGreenPrimary
import com.example.ui.theme.CavalryKurangRed
import java.io.File
import java.io.FileOutputStream

@Composable
fun AppSettingsDialog(
    customStatuses: List<CustomStatusItem>,
    reportConfig: ReportTemplateConfig,
    customAppName: String,
    customAppLogoUri: String?,
    adminUsername: String = "ADMIN",
    onChangeAdminCredentials: (String, String) -> Boolean = { _, _ -> true },
    onLogout: () -> Unit = {},
    onExportConfig: () -> File,
    onExportConfigText: () -> String = { "" },
    onGetShareIntent: (File) -> Intent,
    onImportConfigJson: (String) -> AttendanceRepository.ImportResult,
    onRenameOrUpdateStatus: (oldCode: String, newCode: String, newDescription: String) -> Unit,
    onAddNewStatus: (code: String, description: String) -> Unit,
    onDeleteStatus: (code: String) -> Unit,
    onResetStatusesToDefault: () -> Unit,
    onSaveReportConfig: (ReportTemplateConfig) -> Unit,
    onSaveIdentity: (name: String, logoUri: String?) -> Unit,
    onResetIdentity: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Akun Admin", "File Update", "Keterangan", "Format", "Nama & Logo")
    var importResultDialog by remember { mutableStateOf<AttendanceRepository.ImportResult?>(null) }

    // Universal Android File Picker for JSON configuration
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader(Charsets.UTF_8).readText()
                }?.trim()?.removePrefix("\uFEFF")
                if (!jsonString.isNullOrBlank()) {
                    val result = onImportConfigJson(jsonString)
                    importResultDialog = result
                } else {
                    importResultDialog = AttendanceRepository.ImportResult(
                        success = false,
                        message = "File kosong atau tidak dapat dibaca dari penyimpanan perangkat."
                    )
                }
            } catch (e: Exception) {
                importResultDialog = AttendanceRepository.ImportResult(
                    success = false,
                    message = "Gagal membaca file: ${e.message}"
                )
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
                .testTag("app_settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Top Header with Close Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PENGATURAN APLIKASI",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = CavalryGreenDark
                        )
                        Text(
                            text = AttendanceConstants.SATUAN_KOMANDO,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F766E)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).testTag("btn_close_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color(0xFF475569)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Tabs within Settings
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color(0xFFF1F5F9),
                    contentColor = CavalryGreenDark,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = CavalryGreenDark,
                            height = 3.dp
                        )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTabIndex == index) CavalryGreenDark else Color(0xFF64748B)
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Content
                Box(modifier = Modifier.weight(1f, fill = false)) {
                    when (selectedTabIndex) {
                        0 -> AkunAdminTab(
                            context = context,
                            currentUsername = adminUsername,
                            onChangeCredentials = onChangeAdminCredentials,
                            onLogout = onLogout
                        )
                        1 -> FileUpdateTab(
                            context = context,
                            onExportConfig = onExportConfig,
                            onExportConfigText = onExportConfigText,
                            onGetShareIntent = onGetShareIntent,
                            onImportJsonText = { jsonStr ->
                                val res = onImportConfigJson(jsonStr)
                                importResultDialog = res
                            },
                            onPickFile = {
                                filePickerLauncher.launch("*/*")
                            }
                        )
                        2 -> KeteranganApelTab(
                            customStatuses = customStatuses,
                            onRenameOrUpdateStatus = onRenameOrUpdateStatus,
                            onAddNewStatus = onAddNewStatus,
                            onDeleteStatus = onDeleteStatus,
                            onResetStatusesToDefault = onResetStatusesToDefault
                        )
                        3 -> FormatLaporanTab(
                            currentConfig = reportConfig,
                            onSaveReportConfig = onSaveReportConfig
                        )
                        4 -> IdentitasAppTab(
                            context = context,
                            currentAppName = customAppName,
                            currentLogoUri = customAppLogoUri,
                            onSaveIdentity = onSaveIdentity,
                            onResetIdentity = onResetIdentity
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("btn_done_settings"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CavalryGreenDark)
                ) {
                    Text("Tutup Pengaturan", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    importResultDialog?.let { res ->
        AlertDialog(
            onDismissRequest = { importResultDialog = null },
            icon = {
                Icon(
                    imageVector = if (res.success) Icons.Default.CheckCircle else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (res.success) Color(0xFF16A34A) else Color(0xFFDC2626),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = if (res.success) "Setingan Berhasil Diterapkan" else "Gagal Menerapkan Setingan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = res.message,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    if (res.success && (res.statusesCount > 0 || res.personnelCount > 0)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Keterangan Absen: ${res.statusesCount}\n• Personel Diperbarui: ${res.personnelCount}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF166534)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { importResultDialog = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (res.success) Color(0xFF16A34A) else Color(0xFFDC2626)
                    )
                ) {
                    Text("OK, Mengerti")
                }
            }
        )
    }
}

@Composable
private fun FileUpdateTab(
    context: Context,
    onExportConfig: () -> File,
    onExportConfigText: () -> String,
    onGetShareIntent: (File) -> Intent,
    onImportJsonText: (String) -> Unit,
    onPickFile: () -> Unit
) {
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current
    var showPasteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Notice Info Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
            border = BorderStroke(1.dp, Color(0xFFBBF7D0))
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Gunakan fitur ini untuk saling membagikan & memperbarui daftar personel, keterangan dinas, dan setingan antar HP petugas piket apel.",
                    fontSize = 11.sp,
                    color = Color(0xFF166534),
                    lineHeight = 15.sp
                )
            }
        }

        // 1. BAGIKAN SETINGAN (EXPORT)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F766E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "1. BAGIKAN SETINGAN",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Kirim file JSON via WhatsApp atau Salin Teks ke Clipboard",
                            fontSize = 10.5.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Button Bagikan File
                Button(
                    onClick = {
                        try {
                            val file = onExportConfig()
                            val shareIntent = onGetShareIntent(file)
                            val chooser = Intent.createChooser(shareIntent, "Bagikan File Setingan via:")
                            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(chooser)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Gagal membagikan: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("btn_share_config_file"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Bagikan File Setingan (.json)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Button Salin Teks JSON
                OutlinedButton(
                    onClick = {
                        try {
                            val jsonText = onExportConfigText()
                            if (jsonText.isNotBlank()) {
                                clipboardManager.setText(AnnotatedString(jsonText))
                                Toast.makeText(context, "Teks setingan JSON berhasil disalin ke Clipboard!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Gagal membuat teks setingan", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Gagal menyalin: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("btn_copy_config_text"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF0F766E)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Salin Teks Setingan (JSON)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F766E)
                    )
                }
            }
        }

        // 2. TERIMA SETINGAN (IMPORT)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF16A34A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "2. TERIMA SETINGAN",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Pilih file .json atau tempel teks setingan yang diterima",
                            fontSize = 10.5.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Button Pilih File
                Button(
                    onClick = onPickFile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("btn_import_config_file"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pilih File (.json) dari HP",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Button Tempel Teks JSON
                OutlinedButton(
                    onClick = { showPasteDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("btn_paste_config_text"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF16A34A)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tempel & Terapkan Teks Setingan",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                }
            }
        }
    }

    if (showPasteDialog) {
        PasteJsonConfigDialog(
            onApply = { jsonStr ->
                onImportJsonText(jsonStr)
                showPasteDialog = false
            },
            onDismiss = { showPasteDialog = false }
        )
    }
}

@Composable
private fun PasteJsonConfigDialog(
    onApply: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Tempel Teks Setingan JSON",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Tempel (paste) kode JSON setingan yang dikirim dari WhatsApp atau HP lain di bawah ini:",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedButton(
                    onClick = {
                        val clip = clipboardManager.getText()?.text
                        if (!clip.isNullOrBlank()) {
                            textInput = clip.trim()
                            Toast.makeText(context, "Teks berhasil ditempel dari Clipboard!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Clipboard kosong", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF16A34A)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tempel Otomatis dari Clipboard",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                }

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("{\"customStatuses\":[...]}", fontSize = 11.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    maxLines = 8
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (textInput.isNotBlank()) {
                        onApply(textInput.trim())
                    }
                },
                enabled = textInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
            ) {
                Text("Terapkan Sekarang")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun KeteranganApelTab(
    customStatuses: List<CustomStatusItem>,
    onRenameOrUpdateStatus: (oldCode: String, newCode: String, newDescription: String) -> Unit,
    onAddNewStatus: (code: String, description: String) -> Unit,
    onDeleteStatus: (code: String) -> Unit,
    onResetStatusesToDefault: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<CustomStatusItem?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("btn_add_keterangan_settings"),
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
                    .testTag("btn_reset_keterangan_settings"),
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
                    text = "Reset Bawaan",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Status List without automatic custom text
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                .padding(4.dp)
        ) {
            items(customStatuses) { status ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(0.5.dp, Color(0xFFE2E8F0))
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
                            Text(
                                text = status.description,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E293B)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { itemToEdit = status },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Ganti",
                                    tint = CavalryGreenPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            if (!status.isDefault || customStatuses.size > 10) {
                                IconButton(
                                    onClick = { onDeleteStatus(status.code) },
                                    modifier = Modifier.size(30.dp)
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
    }

    // Edit modal
    itemToEdit?.let { status ->
        EditStatusModalClean(
            statusItem = status,
            onSave = { newCode, newDesc ->
                onRenameOrUpdateStatus(status.code, newCode, newDesc)
                itemToEdit = null
            },
            onDismiss = { itemToEdit = null }
        )
    }

    // Add modal
    if (showAddDialog) {
        AddNewStatusModalClean(
            onAdd = { code, desc ->
                onAddNewStatus(code, desc)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // Reset Confirm
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset ke Bawaan?", fontWeight = FontWeight.Bold) },
            text = { Text("Semua keterangan akan dikembalikan ke 25 kode bawaan standar dinas militer.") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetStatusesToDefault()
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
private fun EditStatusModalClean(
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
                    text = "GANTI KODE & ARTI KETERANGAN",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CavalryGreenDark
                )
                Text(
                    text = "Arti keterangan diatur manual sesuai dinas militer.",
                    fontSize = 10.5.sp,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = codeInput,
                    onValueChange = {
                        codeInput = it.uppercase()
                        errorText = null
                    },
                    label = { Text("Kode Singkatan") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = descInput,
                    onValueChange = {
                        descInput = it
                        errorText = null
                    },
                    label = { Text("Arti Keterangan") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
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
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
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
private fun AddNewStatusModalClean(
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
                    text = "TAMBAH KETERANGAN APEL",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CavalryGreenDark
                )
                Text(
                    text = "Masukkan kode dan arti status dinas/ketidakhadiran baru.",
                    fontSize = 10.5.sp,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = codeInput,
                    onValueChange = {
                        codeInput = it.uppercase()
                        errorText = null
                    },
                    label = { Text("Kode Singkatan") },
                    placeholder = { Text("cth: SATGAS, BKO-POL") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = descInput,
                    onValueChange = {
                        descInput = it
                        errorText = null
                    },
                    label = { Text("Arti Keterangan") },
                    placeholder = { Text("cth: Tugas Operasi Pengamanan") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
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
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
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

@Composable
private fun FormatLaporanTab(
    currentConfig: ReportTemplateConfig,
    onSaveReportConfig: (ReportTemplateConfig) -> Unit
) {
    val scrollState = rememberScrollState()
    var selectedFormat by remember { mutableStateOf(currentConfig.formatType) }
    var headerKomando by remember { mutableStateOf(currentConfig.headerKomando) }
    var headerSatuan by remember { mutableStateOf(currentConfig.headerSatuan) }
    var salamPembuka by remember { mutableStateOf(currentConfig.salamPembuka) }
    var salamPenutup by remember { mutableStateOf(currentConfig.salamPenutup) }
    var customTemplateText by remember { mutableStateOf(currentConfig.customTemplateText) }
    var lokasiTandaTangan by remember { mutableStateOf(currentConfig.lokasiTandaTangan) }
    var ketPaJaga by remember { mutableStateOf(currentConfig.ketPaJaga.ifBlank { "PA JAGA" }) }
    var namaPaJaga by remember { mutableStateOf(currentConfig.namaPaJaga) }
    var namaBaJaga by remember { mutableStateOf(currentConfig.namaBaJaga) }
    var namaTaJaga by remember { mutableStateOf(currentConfig.namaTaJaga) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
            .verticalScroll(scrollState)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "PILIH FORMAT LAPORAN:",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ReportFormatType.values().forEach { format ->
                val isSelected = selectedFormat == format
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFormat = format },
                    label = { Text(format.title, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CavalryGreenPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // --- Card Format Pejabat Piket (PA JAGA, BA JAGA, TA JAGA) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "👮 FORMAT PEJABAT PIKET",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    color = CavalryGreenDark
                )
                Text(
                    text = "Format pejabat piket laporan. Mendukung penekanan tombol ENTER untuk baris baru:",
                    fontSize = 10.sp,
                    color = Color(0xFF475569)
                )

                OutlinedTextField(
                    value = ketPaJaga,
                    onValueChange = { ketPaJaga = it },
                    label = { Text("Keterangan PA JAGA (Kustom jika tidak ada)") },
                    placeholder = { Text("Bawaan: PA JAGA (atau isi NIHIL / DIWAKILKAN)") },
                    singleLine = false,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_ket_pa_jaga")
                )

                OutlinedTextField(
                    value = namaPaJaga,
                    onValueChange = { namaPaJaga = it },
                    label = { Text("Pangkat & Nama PA JAGA (Kustom)") },
                    placeholder = { Text("Pangkat & Nama Lengkap PA JAGA (bisa di-enter)") },
                    singleLine = false,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_nama_pa_jaga")
                )

                OutlinedTextField(
                    value = namaBaJaga,
                    onValueChange = { namaBaJaga = it },
                    label = { Text("BA JAGA (Bawaan) - Pangkat & Nama") },
                    placeholder = { Text("Pangkat & Nama BA JAGA (bisa di-enter)") },
                    singleLine = false,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_nama_ba_jaga")
                )

                OutlinedTextField(
                    value = namaTaJaga,
                    onValueChange = { namaTaJaga = it },
                    label = { Text("TA JAGA (Bawaan) - Pangkat & Nama") },
                    placeholder = { Text("Pangkat & Nama TA JAGA (bisa di-enter)") },
                    singleLine = false,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_nama_ta_jaga")
                )
            }
        }

        OutlinedTextField(
            value = headerKomando,
            onValueChange = { headerKomando = it },
            label = { Text("Header Komando (Bisa di-Enter)") },
            singleLine = false,
            maxLines = 3,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = headerSatuan,
            onValueChange = { headerSatuan = it },
            label = { Text("Header Satuan (Bisa di-Enter)") },
            singleLine = false,
            maxLines = 3,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = salamPembuka,
            onValueChange = { salamPembuka = it },
            label = { Text("Salam Pembuka Laporan (Bisa di-Enter)") },
            placeholder = { Text("Kosongkan untuk salam otomatis sesuai waktu apel") },
            singleLine = false,
            minLines = 2,
            maxLines = 6,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = salamPenutup,
            onValueChange = { salamPenutup = it },
            label = { Text("Salam Penutup Laporan (Bisa di-Enter)") },
            singleLine = false,
            minLines = 2,
            maxLines = 6,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Custom Template Text Area (shown when CUSTOM is selected)
        if (selectedFormat == ReportFormatType.CUSTOM) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                border = BorderStroke(1.dp, Color(0xFF93C5FD))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "📝 TEMPLATE FORMAT KUSTOM",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D4ED8)
                    )
                    Text(
                        text = "Gunakan tag: {SATUAN}, {TANGGAL}, {WAKTU}, {PEMBUKA}, {KUAT_NYATA}, {HADIR}, {KURANG}, {RINCIAN_KURANG}, {PENUTUP}, {KET_PA_JAGA}, {PA_JAGA}, {BA_JAGA}, {TA_JAGA}. Tekan ENTER untuk baris baru.",
                        fontSize = 10.sp,
                        color = Color(0xFF1E40AF),
                        lineHeight = 14.sp
                    )

                    OutlinedTextField(
                        value = customTemplateText,
                        onValueChange = { customTemplateText = it },
                        label = { Text("Susunan Teks Laporan Kustom (Bisa di-Enter)") },
                        placeholder = { Text("Tulis susunan laporan kustom di sini...") },
                        singleLine = false,
                        minLines = 5,
                        maxLines = 20,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("input_custom_template_text")
                    )
                }
            }
        }

        Button(
            onClick = {
                val updated = currentConfig.copy(
                    formatType = selectedFormat,
                    headerKomando = headerKomando.trim(),
                    headerSatuan = headerSatuan.trim(),
                    salamPembuka = salamPembuka.trim(),
                    salamPenutup = salamPenutup.trim(),
                    customTemplateText = customTemplateText,
                    lokasiTandaTangan = lokasiTandaTangan.trim(),
                    ketPaJaga = ketPaJaga.trim().ifBlank { "PA JAGA" },
                    namaPaJaga = namaPaJaga.trim(),
                    namaBaJaga = namaBaJaga.trim(),
                    namaTaJaga = namaTaJaga.trim()
                )
                onSaveReportConfig(updated)
                Toast.makeText(context, "Format & Pejabat Piket berhasil disimpan!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .testTag("btn_save_format_settings"),
            colors = ButtonDefaults.buttonColors(containerColor = CavalryGreenPrimary),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Simpan Format & Pejabat Piket", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun IdentitasAppTab(
    context: Context,
    currentAppName: String,
    currentLogoUri: String?,
    onSaveIdentity: (name: String, logoUri: String?) -> Unit,
    onResetIdentity: () -> Unit
) {
    var appNameInput by remember { mutableStateOf(currentAppName) }
    var selectedLogoUri by remember { mutableStateOf(currentLogoUri) }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val logoFile = File(context.filesDir, "custom_app_logo.png")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(logoFile).use { output ->
                        input.copyTo(output)
                    }
                }
                selectedLogoUri = Uri.fromFile(logoFile).toString()
                Toast.makeText(context, "Logo baru dipilih!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal memproses gambar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "LOGO APLIKASI SAAT INI",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = CavalryGreenDark
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Logo Preview
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .border(2.dp, CavalryGold, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val uri = selectedLogoUri
                    val customBitmap = remember(uri) {
                        if (!uri.isNullOrBlank()) {
                            try {
                                val file = if (uri.startsWith("file://")) File(Uri.parse(uri).path ?: "") else File(uri)
                                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                            } catch (e: Exception) { null }
                        } else null
                    }

                    if (customBitmap != null) {
                        Image(
                            bitmap = customBitmap.asImageBitmap(),
                            contentDescription = "Logo Aplikasi",
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Image(
                            painter = painterResource(id = com.example.R.drawable.logo_kikav2),
                            contentDescription = "Logo KIKAV 2 Bawaan",
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            logoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("btn_change_logo"),
                        colors = ButtonDefaults.buttonColors(containerColor = CavalryGreenPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pilih Logo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            selectedLogoUri = null
                            appNameInput = "ABSEN KIKAV 2"
                            onResetIdentity()
                            Toast.makeText(context, "Logo dan nama direset ke bawaan KIKAV 2", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("btn_reset_logo"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", fontSize = 11.sp)
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "NAMA & JUDUL APLIKASI",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = CavalryGreenDark
                )

                OutlinedTextField(
                    value = appNameInput,
                    onValueChange = { appNameInput = it },
                    label = { Text("Nama Aplikasi") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_app_name")
                )

                Button(
                    onClick = {
                        val nameToSave = if (appNameInput.isNotBlank()) appNameInput.trim() else "ABSEN KIKAV 2"
                        onSaveIdentity(nameToSave, selectedLogoUri)
                        Toast.makeText(context, "Nama dan Logo Aplikasi berhasil disimpan!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("btn_save_identity"),
                    colors = ButtonDefaults.buttonColors(containerColor = CavalryGreenPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("SIMPAN NAMA & LOGO APLIKASI", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun AkunAdminTab(
    context: Context,
    currentUsername: String,
    onChangeCredentials: (newUsername: String, newPassword: String) -> Boolean,
    onLogout: () -> Unit
) {
    var newUsername by remember { mutableStateOf(currentUsername) }
    var newPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "GANTI KREDENSIAL LOGIN ADMIN",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Black,
                    color = CavalryGreenDark
                )
                Text(
                    text = "Atur username dan password kustom untuk hak akses login Admin. Simpan dan ingat kredensial baru Anda.",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 15.sp
                )

                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Username Aktif: $currentUsername",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                OutlinedTextField(
                    value = newUsername,
                    onValueChange = { newUsername = it },
                    label = { Text("Username Baru") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_new_admin_username")
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Password Baru") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("input_new_admin_password")
                )

                Button(
                    onClick = {
                        if (newUsername.isBlank() || newPassword.isBlank()) {
                            Toast.makeText(context, "Username dan Password tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                        } else {
                            val success = onChangeCredentials(newUsername.trim(), newPassword.trim())
                            if (success) {
                                Toast.makeText(context, "Username & Password berhasil diubah!", Toast.LENGTH_LONG).show()
                                newPassword = ""
                            } else {
                                Toast.makeText(context, "Gagal mengubah kredensial", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_save_admin_credentials"),
                    colors = ButtonDefaults.buttonColors(containerColor = CavalryGreenPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("SIMPAN USERNAME & PASSWORD BARU", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("btn_logout_settings"),
                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Keluar dari Akun Admin", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

