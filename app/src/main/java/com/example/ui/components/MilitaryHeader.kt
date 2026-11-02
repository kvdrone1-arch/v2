package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceConstants
import com.example.data.model.WaktuApel
import com.example.ui.theme.CavalryGold
import com.example.ui.theme.CavalryGreenDark
import com.example.ui.theme.CavalryGreenPrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import java.io.File

@Composable
fun MilitaryHeader(
    selectedDate: String,
    selectedWaktuApel: WaktuApel,
    lastSavedTimestamp: String?,
    hasUnsavedChanges: Boolean,
    appName: String = "ABSEN KIKAV 2",
    customLogoUri: String? = null,
    onSaveDataClicked: () -> Unit,
    onDateSelected: (String) -> Unit,
    onWaktuApelSelected: (WaktuApel) -> Unit,
    onSyncClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("military_header"),
        color = CavalryGreenDark,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Military Command Header Line with KIKAV 2 Logo Emblem (matching Untitled-1.jpg)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0C2415))
                    .border(1.dp, Color(0xFF1E4829), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                // Emblem
                Box(
                    modifier = Modifier
                        .size(46.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val customBitmap = remember(customLogoUri) {
                        if (!customLogoUri.isNullOrBlank()) {
                            try {
                                val file = if (customLogoUri.startsWith("file://")) File(Uri.parse(customLogoUri).path ?: "") else File(customLogoUri)
                                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                            } catch (e: Exception) { null }
                        } else null
                    }

                    if (customBitmap != null) {
                        Image(
                            bitmap = customBitmap.asImageBitmap(),
                            contentDescription = "Logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Image(
                            painter = painterResource(id = com.example.R.drawable.logo_kikav2),
                            contentDescription = "Logo Kikav 2",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "KOMANDO DAERAH MILITER XXVI/MATARAM",
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.2.sp,
                        maxLines = 1
                    )
                    Text(
                        text = "KOMPI KAVALERI 2/JRTR",
                        color = Color(0xFFFACC15), // Bold bright gold matching reference
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle + Explicit Save Data Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ABSEN APEL ${selectedWaktuApel.label}",
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )

                    // Saved Status Indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        if (hasUnsavedChanges) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(RoundedCornerShape(3.5.dp))
                                    .background(Color(0xFFFBBF24))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Ada perubahan belum disimpan",
                                fontSize = 9.5.sp,
                                color = Color(0xFFFEF08A),
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4ADE80),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (lastSavedTimestamp != null) "Tersimpan ($lastSavedTimestamp)" else "Data Tersimpan di HP",
                                fontSize = 9.5.sp,
                                color = Color(0xFF86EFAC),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Action Buttons: 1 Single Sync Button + 1 Single Save Button
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // SATU-SATUNYA TOMBOL SINKRONISASI DI SELURUH APLIKASI
                    Button(
                        onClick = onSyncClicked,
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("btn_single_sync"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0F766E),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sinkronkan",
                            tint = Color(0xFF5EEAD4),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SINKRONKAN",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // SATU-SATUNYA TOMBOL SIMPAN DATA DI SELURUH APLIKASI
                    Button(
                        onClick = onSaveDataClicked,
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("btn_save_data"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasUnsavedChanges) Color(0xFF16A34A) else Color(0xFF1E5631),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "SIMPAN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Waktu Apel Selector Segmented Controls (PAGI, SIANG, SORE, MALAM SIAGA)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF10281C), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF2E4839), RoundedCornerShape(8.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                WaktuApel.values().forEach { waktu ->
                    val isSelected = waktu == selectedWaktuApel
                    Box(
                        modifier = Modifier
                            .weight(if (waktu == WaktuApel.MALAM_SIAGA) 1.25f else 1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) CavalryGreenPrimary else Color.Transparent
                            )
                            .clickable { onWaktuApelSelected(waktu) }
                            .padding(vertical = 7.dp, horizontal = 2.dp)
                            .testTag("tab_waktu_${waktu.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = waktu.label,
                            color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                            fontSize = if (waktu == WaktuApel.MALAM_SIAGA) 9.5.sp else 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Date Navigation Row with DatePickerDialog
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF14241C), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF2E4839), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        val newDate = adjustDate(selectedDate, -1)
                        onDateSelected(newDate)
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Hari Sebelumnya",
                        tint = Color.White
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            showDatePicker(context, selectedDate, onDateSelected)
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("date_picker_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Pilih Tanggal",
                        tint = CavalryGold,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TANGGAL: ${com.example.util.DateUtils.formatDateHariBulanTahun(selectedDate)}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = {
                        val newDate = com.example.util.DateUtils.adjustDate(selectedDate, 1)
                        onDateSelected(newDate)
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Hari Berikutnya",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

private fun adjustDate(dateStr: String, daysToAdd: Int): String {
    return com.example.util.DateUtils.adjustDate(dateStr, daysToAdd)
}

private fun showDatePicker(
    context: android.content.Context,
    currentDateStr: String,
    onDateSelected: (String) -> Unit
) {
    val cal = Calendar.getInstance()
    try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsed = sdf.parse(currentDateStr)
        if (parsed != null) {
            cal.time = parsed
        }
    } catch (_: Exception) {}

    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val formatted = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            onDateSelected(formatted)
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}
