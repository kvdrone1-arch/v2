package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AttendanceConstants
import com.example.data.model.ReportTemplateConfig
import com.example.ui.theme.CavalryGold
import com.example.ui.theme.CavalryGreenDark
import com.example.ui.theme.CavalryGreenPrimary

@Composable
fun CustomReportDialog(
    currentConfig: ReportTemplateConfig,
    onSaveConfig: (ReportTemplateConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var komandoInput by remember { mutableStateOf(currentConfig.headerKomando) }
    var satuanInput by remember { mutableStateOf(currentConfig.headerSatuan) }
    var pembukaInput by remember { mutableStateOf(currentConfig.salamPembuka) }
    var penutupInput by remember { mutableStateOf(currentConfig.salamPenutup) }
    var customTemplateInput by remember {
        mutableStateOf(
            if (currentConfig.customTemplateText.isNotBlank()) currentConfig.customTemplateText
            else """
*{KODAM}*
*{SATUAN}*

*LAPORAN ABSEN APEL {WAKTU}*
Tanggal: {TANGGAL}
Waktu Apel: {WAKTU}

{PEMBUKA}

*A. REKAPITULASI:*
{REKAP}
*TOTAL: Kuat {KUAT_NYATA} | Hadir {HADIR} | Kurang {KURANG}*

*B. RINCIAN KURANG:*
{RINCIAN_KURANG}

{PENUTUP}
            """.trimIndent()
        )
    }

    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("custom_report_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "KUSTOMISASI FORMAT LAPORAN",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CavalryGreenDark
                        )
                        Text(
                            text = "Atur teks pembuka, penutup, kop komando, atau struktur laporan",
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

                Spacer(modifier = Modifier.height(12.dp))

                // Field: Header Komando
                OutlinedTextField(
                    value = komandoInput,
                    onValueChange = { komandoInput = it },
                    label = { Text("Kop Komando Daerah Militer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Field: Header Satuan
                OutlinedTextField(
                    value = satuanInput,
                    onValueChange = { satuanInput = it },
                    label = { Text("Nama Satuan / Batalyon / Kompi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Field: Salam Pembuka
                OutlinedTextField(
                    value = pembukaInput,
                    onValueChange = { pembukaInput = it },
                    label = { Text("Salam / Kalimat Pembuka") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Field: Salam Penutup
                OutlinedTextField(
                    value = penutupInput,
                    onValueChange = { penutupInput = it },
                    label = { Text("Salam / Tanda Tangan Penutup") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Placeholder Hints Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFEF3C7))
                        .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Text(
                            text = "Variabel Template yang Tersedia:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = "{KODAM}, {SATUAN}, {WAKTU}, {TANGGAL}, {PEMBUKA}, {REKAP}, {KUAT_NYATA}, {HADIR}, {KURANG}, {RINCIAN_KURANG}, {PENUTUP}",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF78350F)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Custom Raw Template editor
                Text(
                    text = "Susunan Template Format Kustom:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = customTemplateInput,
                    onValueChange = { customTemplateInput = it },
                    minLines = 6,
                    maxLines = 10,
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            komandoInput = AttendanceConstants.SATUAN_KOMANDO
                            satuanInput = AttendanceConstants.SATUAN_UNIT
                            pembukaInput = "Selamat pagi/siang/sore Komandan, izin melaporkan hasil apel pengecekan personel:"
                            penutupInput = "Demikian dilaporkan, tertanda Piket KIKAV 2/JRTR."
                            customTemplateInput = ""
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Default", fontSize = 11.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onDismiss) {
                            Text("Batal")
                        }
                        Button(
                            onClick = {
                                val newConfig = currentConfig.copy(
                                    headerKomando = komandoInput.trim(),
                                    headerSatuan = satuanInput.trim(),
                                    salamPembuka = pembukaInput.trim(),
                                    salamPenutup = penutupInput.trim(),
                                    customTemplateText = customTemplateInput.trim()
                                )
                                onSaveConfig(newConfig)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CavalryGreenPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simpan Format", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
