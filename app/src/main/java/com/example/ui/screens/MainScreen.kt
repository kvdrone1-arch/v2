package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AttendanceConstants
import com.example.data.model.Person
import com.example.data.model.UserRole
import com.example.data.model.WaktuApel
import com.example.ui.components.AppSettingsDialog
import com.example.ui.components.ManagePersonnelDialog
import com.example.ui.components.MilitaryHeader
import com.example.ui.components.PlatoonPersonnelCard
import com.example.ui.components.ReportExportScreen
import com.example.ui.components.StatusSelectionDialog
import com.example.ui.components.SummaryDashboardCards
import com.example.ui.components.SummaryTable
import com.example.ui.components.WebViewScreen
import com.example.ui.theme.CavalryGold
import com.example.ui.theme.CavalryGreenDark
import com.example.ui.theme.CavalryGreenPrimary
import com.example.ui.theme.CavalryRed
import com.example.ui.viewmodel.AttendanceViewModel

enum class MainNavigationTab(val title: String, val icon: ImageVector) {
    PENGECEKAN("Pengecekan", Icons.Default.FactCheck),
    REKAP("Rekapitulasi", Icons.Default.TableChart),
    LAPORAN("Laporan & Word", Icons.Default.Description),
    WEB_SERVER("Server Web", Icons.Default.CloudSync)
}

@Composable
fun MainScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.userNotification) {
        uiState.userNotification?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotification()
        }
    }

    // 1. If not logged in, show LoginScreen (Single Admin Login)
    if (!uiState.isLoggedIn) {
        LoginScreen(
            customAppName = uiState.customAppName,
            customAppLogoUri = uiState.customAppLogoUri,
            loginError = uiState.loginError,
            onLogin = { u, p -> viewModel.login(u, p) },
            modifier = modifier
        )
        return
    }

    // Auto-save attendance data when leaving or backgrounding the application
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_PAUSE) {
                viewModel.autoSaveData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val availableTabs = listOf(
        MainNavigationTab.PENGECEKAN,
        MainNavigationTab.REKAP,
        MainNavigationTab.LAPORAN
    )

    val isAdmin = true

    var selectedTabKey by remember { mutableIntStateOf(0) }
    val currentTab = availableTabs.getOrElse(selectedTabKey) { availableTabs.first() }
    var showConfirmSetAllHadirDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = CavalryGreenDark,
                contentColor = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                availableTabs.forEachIndexed { index, tab ->
                    val isSelected = selectedTabKey == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTabKey = index },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CavalryGreenDark,
                            selectedTextColor = Color(0xFFFEF08A),
                            indicatorColor = CavalryGold,
                            unselectedIconColor = Color(0xFFE2E8F0),
                            unselectedTextColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}")
                    )
                }

                // Tombol KELUAR di baris bar bawah (Menyimpan data otomatis dan kembali ke login)
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        viewModel.logout()
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Keluar Sistem",
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Keluar",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CavalryRed,
                        selectedTextColor = Color(0xFFFCA5A5),
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = Color(0xFFFCA5A5),
                        unselectedTextColor = Color(0xFFFCA5A5)
                    ),
                    modifier = Modifier.testTag("nav_item_logout")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF3F6F4))
        ) {
            // Sticky Military Header for Absen/Rekap
            if (currentTab == MainNavigationTab.PENGECEKAN || currentTab == MainNavigationTab.REKAP) {
                MilitaryHeader(
                    selectedDate = uiState.selectedDate,
                    selectedWaktuApel = uiState.selectedWaktuApel,
                    lastSavedTimestamp = uiState.lastSavedTimestamp,
                    hasUnsavedChanges = uiState.hasUnsavedChanges,
                    appName = uiState.customAppName,
                    customLogoUri = uiState.customAppLogoUri,
                    onSaveDataClicked = {
                        viewModel.saveDataExplicitly()
                    },
                    onDateSelected = { viewModel.setDate(it) },
                    onWaktuApelSelected = { viewModel.setWaktuApel(it) },
                    onSyncClicked = {
                        viewModel.openSyncDialog()
                    }
                )
            }

            // Tab Content Dispatcher (Only 4 core tabs remain: Pengecekan, Rekap, Laporan, Web Server)
            when (currentTab) {
                MainNavigationTab.PENGECEKAN -> {
                    PengecekanTabContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        isAdmin = isAdmin,
                        onRequestMarkAllPresent = { showConfirmSetAllHadirDialog = true }
                    )
                }
                MainNavigationTab.REKAP -> {
                    RekapTabContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        isAdmin = isAdmin,
                        onRequestMarkAllPresent = { showConfirmSetAllHadirDialog = true }
                    )
                }
                MainNavigationTab.LAPORAN -> {
                    ReportExportScreen(
                        reportText = viewModel.getFormattedReportText(),
                        jsonText = viewModel.getExportJson(),
                        date = uiState.selectedDate,
                        waktuApel = uiState.selectedWaktuApel,
                        reportConfig = uiState.reportConfig,
                        hasReportPhoto = uiState.hasApelPhoto,
                        reportPhotoFile = viewModel.getApelPhotoFile(),
                        onFormatTypeSelected = { viewModel.setReportFormatType(it) },
                        onOpenCustomReportDialog = { viewModel.openSettings() },
                        onGenerateWordFile = { viewModel.exportToWordFile() },
                        onGetShareWordIntent = { viewModel.getShareWordIntent(it) },
                        onGetOpenWordIntent = { viewModel.getOpenWordIntent(it) },
                        onAttachPhoto = { uri ->
                            try {
                                val stream = context.contentResolver.openInputStream(uri)
                                if (stream != null) {
                                    viewModel.saveApelPhoto(stream)
                                }
                            } catch (e: Exception) {
                                // Handled
                            }
                        },
                        onRemovePhoto = { viewModel.deleteApelPhoto() },
                        onGetShareReportIntent = { textToShare ->
                            viewModel.getShareReportWithPhotoIntent(textToShare)
                        },
                        onOpenShareSettings = { viewModel.openSettings() },
                        onOpenImportSettings = { viewModel.openSettings() }
                    )
                }
                MainNavigationTab.WEB_SERVER -> {
                    WebViewScreen(
                        url = AttendanceConstants.GAS_WEB_APP_URL
                    )
                }
            }
        }

        // Active Status Selection Dialog
        uiState.editingPerson?.let { (groupKey, person) ->
            StatusSelectionDialog(
                groupKey = groupKey,
                person = person,
                customStatuses = uiState.customStatuses,
                onStatusSelected = { newStatus ->
                    viewModel.updateStatus(groupKey, person.id, newStatus, person.name)
                },
                onManageStatusesClicked = {
                    viewModel.openSettings()
                },
                onDismiss = {
                    viewModel.closeStatusEditor()
                }
            )
        }

        // Unified App Settings Dialog
        if (uiState.isSettingsOpen) {
            AppSettingsDialog(
                customStatuses = uiState.customStatuses,
                reportConfig = uiState.reportConfig,
                customAppName = uiState.customAppName,
                customAppLogoUri = uiState.customAppLogoUri,
                adminUsername = uiState.adminUsername,
                onChangeAdminCredentials = { u, p ->
                    viewModel.changeAdminCredentials(u, p)
                },
                onExportConfig = { viewModel.exportConfigJson() },
                onExportConfigText = { viewModel.exportConfigJsonString() },
                onGetShareIntent = { viewModel.getShareConfigIntent(it) },
                onImportConfigJson = { viewModel.importConfigFromJson(it) },
                onRenameOrUpdateStatus = { oldCode, newCode, newDesc ->
                    viewModel.renameOrSaveStatus(oldCode, newCode, newDesc)
                },
                onAddNewStatus = { code, desc ->
                    viewModel.addNewStatus(code, desc)
                },
                onDeleteStatus = { code ->
                    viewModel.deleteStatus(code)
                },
                onResetStatusesToDefault = {
                    viewModel.resetStatusesToDefault()
                },
                onSaveReportConfig = { newConfig ->
                    viewModel.saveReportTemplateConfig(newConfig)
                },
                onSaveIdentity = { name, logoUri ->
                    viewModel.updateAppIdentity(name, logoUri)
                },
                onResetIdentity = {
                    viewModel.resetAppIdentity()
                },
                onLogout = {
                    viewModel.logout()
                },
                onDismiss = {
                    viewModel.closeSettings()
                }
            )
        }

        // Manage Personnel Dialog (Add & Remove Members)
        if (uiState.isManagePersonnelOpen) {
            ManagePersonnelDialog(
                platoonRoster = viewModel.getAllPlatoonRoster(),
                remajaRoster = viewModel.getRemajaRoster(),
                onAddMember = { groupKey, name, isRemaja ->
                    viewModel.addMember(groupKey, name, isRemaja)
                },
                onRemoveMember = { groupKey, name ->
                    viewModel.removeMember(groupKey, name)
                },
                onDismiss = {
                    viewModel.closeManagePersonnel()
                }
            )
        }

        // Single Sync Confirmation Dialog
        if (uiState.isSyncDialogOpen) {
            AlertDialog(
                onDismissRequest = { viewModel.closeSyncDialog() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = Color(0xFF0F766E),
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text(
                        text = "Sinkronisasi Keterangan Apel",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Apakah Anda ingin menyinkronkan seluruh keterangan/status personel hasil edit di APEL PAGI ke APEL SIANG, APEL SORE, dan APEL MALAM SIAGA pada tanggal ${com.example.util.DateUtils.formatDateHariBulanTahun(uiState.selectedDate)}?",
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = Color(0xFFF0FDF4),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                        ) {
                            Text(
                                text = "LOGIKA SINKRONISASI: Data tidak akan tersinkron otomatis sebelum Anda menekan tombol 'Ya, Sinkronkan' ini.",
                                color = Color(0xFF166534),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.executeSingleSync() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0F766E),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("btn_confirm_sync")
                    ) {
                        Text("Ya, Sinkronkan Sekarang", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { viewModel.closeSyncDialog() },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("btn_cancel_sync")
                    ) {
                        Text("Batal")
                    }
                }
            )
        }

        // Confirmation dialog for "Setel Semua Hadir Lengkap"
        if (showConfirmSetAllHadirDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmSetAllHadirDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = CavalryGreenPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text(
                        text = "Konfirmasi Setel Semua Hadir",
                        fontWeight = FontWeight.Bold,
                        color = CavalryGreenDark,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Text(
                        text = "Apakah Anda yakin ingin menyetel SELURUH personel satuan menjadi HADIR LENGKAP? Data ketidakhadiran sebelumnya akan direset ke Hadir.",
                        fontSize = 12.5.sp,
                        color = Color(0xFF334155)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showConfirmSetAllHadirDialog = false
                            viewModel.markAllPresentEntireUnit()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CavalryGreenPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("btn_confirm_set_all_hadir")
                    ) {
                        Text("Ya, Setel Hadir Lengkap", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showConfirmSetAllHadirDialog = false },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("btn_cancel_set_all_hadir")
                    ) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

@Composable
private fun PengecekanTabContent(
    uiState: com.example.ui.viewmodel.AttendanceUiState,
    viewModel: AttendanceViewModel,
    isAdmin: Boolean,
    onRequestMarkAllPresent: () -> Unit
) {
    val filterScrollState = rememberScrollState()
    val companionScrollState = rememberScrollState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("pengecekan_list")
    ) {
        // Special Banner for APEL MALAM SIAGA: Choose companion unit (1 of KOKI, SIMA, TON I, TON II, TON III)
        if (uiState.selectedWaktuApel == WaktuApel.MALAM_SIAGA) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("card_malam_siaga_selector"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    border = BorderStroke(1.dp, Color(0xFFFBBF24))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "FORMASI APEL MALAM SIAGA",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "1. Data REMAJA (43 Personel Tetap)\n2. Pilih salah satu Peleton Siaga Pendamping:",
                            fontSize = 11.sp,
                            color = Color(0xFF78350F),
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(companionScrollState),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AttendanceConstants.SIAGA_OPTIONS.forEach { optionKey ->
                                val isSelected = uiState.malamSiagaCompanionUnit == optionKey
                                val displayName = AttendanceConstants.DISPLAY_NAMES[optionKey] ?: optionKey
                                val memberCount = AttendanceConstants.INITIAL_DATA[optionKey]?.size ?: 0

                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isAdmin) {
                                            viewModel.setMalamSiagaCompanionUnit(optionKey)
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = "$displayName ($memberCount)",
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CavalryGreenPrimary,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFFFFFBEB),
                                        labelColor = Color(0xFF92400E)
                                    ),
                                    modifier = Modifier.testTag("chip_siaga_$optionKey")
                                )
                            }
                        }
                    }
                }
            }
        }

        // Dashboard Cards
        item {
            SummaryDashboardCards(
                attendanceData = uiState.attendanceData,
                onMarkAllPresent = {
                    if (isAdmin) {
                        onRequestMarkAllPresent()
                    }
                }
            )
        }

        // Search Bar & Filter Chips + Manage Statuses button + Save status banner
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("search_input"),
                    placeholder = {
                        Text(
                            text = "Cari nama personel...",
                            fontSize = 11.5.sp,
                            color = Color(0xFF94A3B8)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Cari",
                            tint = CavalryGreenPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Hapus",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = CavalryGreenPrimary,
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                if (isAdmin) {
                    Spacer(modifier = Modifier.height(6.dp))

                    // Action Buttons: 1 Tombol Kelola Anggota & 1 Tombol Pengaturan
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { viewModel.openManagePersonnel() },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("btn_manage_members"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E3A8A),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.GroupAdd,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF93C5FD)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "KELOLA ANGGOTA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Button(
                            onClick = { viewModel.openSettings() },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("btn_app_settings"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0F766E),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Pengaturan",
                                tint = Color(0xFF5EEAD4),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PENGATURAN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Platoon Filter Chips (Dynamic for Regular vs Malam Siaga)
                val activeKeys = uiState.attendanceData.keys.toList()
                val totalPersonnel = uiState.attendanceData.values.sumOf { it.size }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(filterScrollState),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = uiState.selectedPlatoonFilter == null,
                        onClick = { viewModel.setPlatoonFilter(null) },
                        label = { Text("SEMUA ($totalPersonnel)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CavalryGreenPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFFE2E8F0),
                            labelColor = Color(0xFF0F172A)
                        ),
                        modifier = Modifier.testTag("chip_filter_all")
                    )

                    activeKeys.forEach { key ->
                        val count = uiState.attendanceData[key]?.size ?: 0
                        val isSelected = uiState.selectedPlatoonFilter == key
                        val label = AttendanceConstants.DISPLAY_NAMES[key] ?: key

                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setPlatoonFilter(if (isSelected) null else key) },
                            label = { Text("$label ($count)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CavalryGreenPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFFE2E8F0),
                                labelColor = Color(0xFF0F172A)
                            ),
                            modifier = Modifier.testTag("chip_filter_$key")
                        )
                    }
                }
            }
        }

        // Filtered Platoon Cards (When searching, inspect all groups across entire unit with trimmed & tokenized search)
        val cleanQuery = uiState.searchQuery.trim()
        val queryTokens = if (cleanQuery.isNotBlank()) {
            cleanQuery.split("\\s+".toRegex()).filter { it.isNotBlank() }
        } else emptyList()

        val totalMatches = if (queryTokens.isEmpty()) 0 else {
            uiState.attendanceData.values.sumOf { list ->
                list.count { person ->
                    queryTokens.all { token ->
                        val normToken = AttendanceConstants.normalizeMilitaryName(token)
                        val normPerson = AttendanceConstants.normalizeMilitaryName(person.name)
                        person.name.contains(token, ignoreCase = true) ||
                        normPerson.contains(normToken, ignoreCase = true) ||
                        person.status.contains(token, ignoreCase = true)
                    }
                }
            }
        }

        if (queryTokens.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (totalMatches > 0) Color(0xFFEFF6FF) else Color(0xFFFEF2F2)
                    ),
                    border = BorderStroke(1.dp, if (totalMatches > 0) Color(0xFF93C5FD) else Color(0xFFFECACA))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (totalMatches > 0) Icons.Default.Search else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (totalMatches > 0) Color(0xFF1D4ED8) else Color(0xFFDC2626),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (totalMatches > 0) {
                                "Hasil pencarian \"$cleanQuery\": $totalMatches personel ditemukan di semua peleton"
                            } else {
                                "Tidak ditemukan personel dengan nama \"$cleanQuery\""
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (totalMatches > 0) Color(0xFF1E40AF) else Color(0xFFB91C1C)
                        )
                    }
                }
            }
        }

        val groupsToDisplay = if (queryTokens.isNotEmpty()) {
            uiState.attendanceData.keys.toList()
        } else if (uiState.selectedPlatoonFilter != null) {
            listOf(uiState.selectedPlatoonFilter!!)
        } else {
            uiState.attendanceData.keys.toList()
        }

        items(groupsToDisplay, key = { it }) { groupKey ->
            val persons = uiState.attendanceData[groupKey] ?: emptyList()
            val filteredPersons = if (queryTokens.isEmpty()) {
                persons
            } else {
                persons.filter { person ->
                    queryTokens.all { token ->
                        val normToken = AttendanceConstants.normalizeMilitaryName(token)
                        val normPerson = AttendanceConstants.normalizeMilitaryName(person.name)
                        person.name.contains(token, ignoreCase = true) ||
                        normPerson.contains(normToken, ignoreCase = true) ||
                        person.status.contains(token, ignoreCase = true)
                    }
                }
            }

            if (filteredPersons.isNotEmpty() || queryTokens.isEmpty()) {
                PlatoonPersonnelCard(
                    groupKey = groupKey,
                    persons = filteredPersons,
                    customStatuses = uiState.customStatuses,
                    onPersonClicked = { person ->
                        if (isAdmin) {
                            viewModel.openStatusEditor(groupKey, person)
                        } else {
                            viewModel.clearNotification()
                        }
                    },
                    onMarkAllHadir = {
                        if (isAdmin) {
                            viewModel.markAllPresentInGroup(groupKey)
                        }
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun RekapTabContent(
    uiState: com.example.ui.viewmodel.AttendanceUiState,
    viewModel: AttendanceViewModel,
    isAdmin: Boolean,
    onRequestMarkAllPresent: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            SummaryDashboardCards(
                attendanceData = uiState.attendanceData,
                onMarkAllPresent = {
                    if (isAdmin) {
                        onRequestMarkAllPresent()
                    }
                }
            )
        }
        item {
            SummaryTable(
                attendanceData = uiState.attendanceData,
                customStatuses = uiState.customStatuses
            )
        }
        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
