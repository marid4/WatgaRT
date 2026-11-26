package com.wargart.feature.lingkungan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wargart.core.ui.components.*
import com.wargart.core.ui.theme.CrimsonDanger
import com.wargart.core.ui.theme.PureWhite
import com.wargart.core.ui.theme.RoyalBlue
import com.wargart.domain.model.*
import com.wargart.domain.repository.WargaRtRepository
import kotlinx.coroutines.launch

@Composable
fun LingkunganScreen(repo: WargaRtRepository, session: UserSession) {
    val scope = rememberCoroutineScope()
    var tabIndex by remember { mutableIntStateOf(0) }
    var contacts by remember { mutableStateOf<EmergencyContacts?>(null) }
    var reports by remember { mutableStateOf<List<KeamananReport>>(emptyList()) }
    var tamuList by remember { mutableStateOf<List<TamuItem>>(emptyList()) }
    var kegiatanList by remember { mutableStateOf<List<KegiatanItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showTamuDialog by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            loading = true
            repo.getEmergencyContacts().onSuccess { contacts = it }
            repo.getKeamananReports().onSuccess { reports = it }
            repo.getTamu().onSuccess { tamuList = it }
            repo.getKegiatan().onSuccess { kegiatanList = it }
            loading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        floatingActionButton = {
            if (tabIndex == 1) {
                FloatingActionButton(onClick = { showTamuDialog = true }, containerColor = RoyalBlue) {
                    Icon(Icons.Rounded.PersonAdd, contentDescription = "Catat Tamu")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tabIndex) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Keamanan & SOS") })
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Buku Tamu") })
                Tab(selected = tabIndex == 2, onClick = { tabIndex = 2 }, text = { Text("Kegiatan") })
            }

            feedback?.let {
                Text(it, color = RoyalBlue, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium)
            }

            if (loading) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { repeat(4) { SkeletonBox() } }
            } else when (tabIndex) {
                0 -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Button(
                            onClick = {
                                scope.launch {
                                    repo.triggerSos("Lingkungan RT ${contacts?.nomorRt ?: "01"}")
                                        .onSuccess { feedback = it; refresh() }
                                        .onFailure { feedback = it.message }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.Warning, contentDescription = null, tint = PureWhite)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("TOMBOL DARURAT SOS", color = PureWhite, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    contacts?.let { c ->
                        item {
                            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Kontak Darurat Wilayah", style = MaterialTheme.typography.titleMedium)
                                    Text("Ketua RT ${c.nomorRt}: ${c.ketuaRt ?: "-"} (${c.hpKetuaRt ?: "-"})")
                                    Text("Pos Keamanan / Satpam: ${c.hpKeamanan ?: "-"}")
                                    Text("Ketua RW ${c.nomorRw}: ${c.ketuaRw ?: "-"} (${c.hpKetuaRw ?: "-"})")
                                }
                            }
                        }
                    }

                    item { Text("Riwayat Laporan Keamanan", style = MaterialTheme.typography.titleMedium) }
                    items(reports) { r ->
                        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                Text("${r.jenis} • ${r.status}", style = MaterialTheme.typography.labelMedium, color = CrimsonDanger)
                                Text(r.judul, style = MaterialTheme.typography.titleMedium)
                                Text("${r.pelapor} • ${r.createdAt}", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                1 -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (tamuList.isEmpty()) item { EmptyStateView("Belum ada tamu tercatat.") }
                    items(tamuList) { t ->
                        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(t.namaTamu, style = MaterialTheme.typography.titleMedium)
                                    AssistChip(onClick = {}, label = { Text(t.status) })
                                }
                                Text("Tujuan: ${t.tujuanAlamat} • ${t.keperluan}")
                                Text("Masuk: ${t.waktuMasuk} • Nopol: ${t.nomorKendaraan ?: "-"}", style = MaterialTheme.typography.labelMedium)
                                if (t.status == "MASUK") {
                                    OutlinedButton(onClick = {
                                        scope.launch {
                                            repo.checkoutTamu(t.id).onSuccess { feedback = it; refresh() }
                                        }
                                    }) { Text("Catat Tamu Keluar") }
                                }
                            }
                        }
                    }
                }

                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (kegiatanList.isEmpty()) item { EmptyStateView("Belum ada jadwal kegiatan.") }
                    items(kegiatanList) { k ->
                        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(k.kategori, style = MaterialTheme.typography.labelMedium, color = RoyalBlue)
                                Text(k.judul, style = MaterialTheme.typography.titleMedium)
                                Text("${k.lokasi} • ${k.tanggalMulai}")
                                Text("Peserta terdaftar: ${k.totalPeserta}", style = MaterialTheme.typography.labelMedium)
                                if (!k.sudahDaftar) {
                                    Button(onClick = {
                                        scope.launch {
                                            repo.registerKegiatan(k.id).onSuccess { feedback = it; refresh() }
                                        }
                                    }) { Text("Daftar Ikut Kegiatan") }
                                } else {
                                    AssistChip(onClick = {}, label = { Text("Sudah Terdaftar") })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTamuDialog) {
        var nama by remember { mutableStateOf("") }
        var hp by remember { mutableStateOf("") }
        var tujuan by remember { mutableStateOf("") }
        var keperluan by remember { mutableStateOf("") }
        var nopol by remember { mutableStateOf("") }
        FullScreenFormDialog("Catat Buku Tamu Digital", onDismiss = { showTamuDialog = false }) {
            item { OutlinedTextField(nama, { nama = it }, label = { Text("Nama Tamu") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(hp, { hp = it }, label = { Text("No. HP") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(tujuan, { tujuan = it }, label = { Text("Alamat Tujuan / Blok") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(keperluan, { keperluan = it }, label = { Text("Keperluan") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(nopol, { nopol = it }, label = { Text("Nomor Kendaraan") }, modifier = Modifier.fillMaxWidth()) }
            item {
                Button(onClick = {
                    scope.launch {
                        repo.checkinTamu(nama, hp, tujuan, keperluan, nopol)
                            .onSuccess { feedback = it; showTamuDialog = false; refresh() }
                            .onFailure { feedback = it.message }
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Simpan Tamu Masuk") }
            }
        }
    }
}
