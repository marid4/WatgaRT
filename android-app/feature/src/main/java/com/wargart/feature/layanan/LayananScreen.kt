package com.wargart.feature.layanan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wargart.core.ui.components.*
import com.wargart.core.ui.theme.RoyalBlue
import com.wargart.core.util.Formatters
import com.wargart.domain.model.*
import com.wargart.domain.repository.WargaRtRepository
import kotlinx.coroutines.launch

@Composable
fun LayananScreen(repo: WargaRtRepository, session: UserSession) {
    val scope = rememberCoroutineScope()
    var tabIndex by remember { mutableIntStateOf(0) }
    var templates by remember { mutableStateOf<List<SuratTemplate>>(emptyList()) }
    var suratList by remember { mutableStateOf<List<SuratItem>>(emptyList()) }
    var pengumumanList by remember { mutableStateOf<List<PengumumanItem>>(emptyList()) }
    var pengaduanList by remember { mutableStateOf<List<PengaduanItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            loading = true
            repo.getSuratTemplates().onSuccess { templates = it }
            repo.getSuratList().onSuccess { suratList = it }
            repo.getPengumuman().onSuccess { pengumumanList = it }
            repo.getPengaduan().onSuccess { pengaduanList = it }
            loading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        floatingActionButton = {
            val canAdd = tabIndex != 1 || Formatters.isAdminRole(session.roleCode)
            if (canAdd) {
                FloatingActionButton(onClick = { showCreateDialog = true }, containerColor = RoyalBlue) {
                    Icon(Icons.Rounded.Add, contentDescription = "Buat")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tabIndex) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Surat") })
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Pengumuman") })
                Tab(selected = tabIndex == 2, onClick = { tabIndex = 2 }, text = { Text("Pengaduan") })
            }

            feedback?.let {
                Text(it, color = RoyalBlue, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium)
            }

            if (loading) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { repeat(4) { SkeletonBox() } }
            } else when (tabIndex) {
                0 -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (suratList.isEmpty()) item { EmptyStateView("Belum ada pengajuan surat.") }
                    items(suratList) { s ->
                        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(s.jenisSurat, style = MaterialTheme.typography.titleMedium)
                                    AssistChip(onClick = {}, label = { Text(s.status) })
                                }
                                Text("Pemohon: ${s.namaLengkap}")
                                Text("Keperluan: ${s.keperluan}", style = MaterialTheme.typography.bodyMedium)
                                s.nomorSurat?.let { Text("Nomor: $it", color = RoyalBlue, style = MaterialTheme.typography.labelMedium) }
                                if (Formatters.isAdminRole(session.roleCode) && s.status == "DIAJUKAN") {
                                    Button(onClick = {
                                        scope.launch {
                                            repo.approveSurat(s.id, true)
                                                .onSuccess { feedback = it; refresh() }
                                                .onFailure { feedback = it.message }
                                        }
                                    }) { Text("Setujui & Terbitkan Nomor") }
                                }
                            }
                        }
                    }
                }

                1 -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (pengumumanList.isEmpty()) item { EmptyStateView("Belum ada pengumuman.") }
                    items(pengumumanList) { p ->
                        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${p.scope} • ${p.kategori}", style = MaterialTheme.typography.labelMedium, color = RoyalBlue)
                                Text(p.judul, style = MaterialTheme.typography.titleMedium)
                                Text(p.isi, style = MaterialTheme.typography.bodyMedium)
                                Text(p.publishedAt, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (pengaduanList.isEmpty()) item { EmptyStateView("Belum ada pengaduan warga.") }
                    items(pengaduanList) { a ->
                        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(a.kodeTiket, style = MaterialTheme.typography.labelMedium, color = RoyalBlue)
                                    AssistChip(onClick = {}, label = { Text(a.status) })
                                }
                                Text(a.judul, style = MaterialTheme.typography.titleMedium)
                                Text(a.deskripsi, style = MaterialTheme.typography.bodyMedium)
                                a.tanggapanAdmin?.let { Text("Tanggapan Admin: $it", color = RoyalBlue, style = MaterialTheme.typography.labelMedium) }
                                if (Formatters.isAdminRole(session.roleCode) && a.status != "SELESAI") {
                                    OutlinedButton(onClick = {
                                        scope.launch {
                                            repo.respondPengaduan(a.id, "SELESAI", "Telah ditindaklanjuti oleh pengurus RT.")
                                                .onSuccess { feedback = it; refresh() }
                                        }
                                    }) { Text("Tandai Selesai") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        when (tabIndex) {
            0 -> {
                var selectedTemplate by remember { mutableStateOf(templates.firstOrNull()?.id ?: 1L) }
                var keperluan by remember { mutableStateOf("") }
                FullScreenFormDialog("Ajukan Surat Pengantar / Keterangan", onDismiss = { showCreateDialog = false }) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            templates.forEach { t ->
                                FilterChip(selected = selectedTemplate == t.id, onClick = { selectedTemplate = t.id }, label = { Text(t.namaSurat) })
                            }
                        }
                    }
                    item { OutlinedTextField(keperluan, { keperluan = it }, label = { Text("Keperluan Surat") }, modifier = Modifier.fillMaxWidth()) }
                    item {
                        Button(onClick = {
                            scope.launch {
                                repo.createSurat(selectedTemplate, keperluan)
                                    .onSuccess { feedback = it; showCreateDialog = false; refresh() }
                                    .onFailure { feedback = it.message }
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Kirim Pengajuan") }
                    }
                }
            }
            1 -> {
                var judul by remember { mutableStateOf("") }
                var isi by remember { mutableStateOf("") }
                var kategori by remember { mutableStateOf("UMUM") }
                FullScreenFormDialog("Buat Pengumuman Baru", onDismiss = { showCreateDialog = false }) {
                    item { OutlinedTextField(judul, { judul = it }, label = { Text("Judul Pengumuman") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(kategori, { kategori = it }, label = { Text("Kategori (UMUM/KERJA_BAKTI/DARURAT)") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(isi, { isi = it }, label = { Text("Isi Pengumuman") }, modifier = Modifier.fillMaxWidth(), minLines = 4) }
                    item {
                        Button(onClick = {
                            scope.launch {
                                repo.createPengumuman(judul, isi, kategori, "RT")
                                    .onSuccess { feedback = it; showCreateDialog = false; refresh() }
                                    .onFailure { feedback = it.message }
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Publikasikan") }
                    }
                }
            }
            else -> {
                var kategori by remember { mutableStateOf("FASILITAS") }
                var judul by remember { mutableStateOf("") }
                var deskripsi by remember { mutableStateOf("") }
                var lokasi by remember { mutableStateOf("") }
                FullScreenFormDialog("Buat Pengaduan Warga", onDismiss = { showCreateDialog = false }) {
                    item { OutlinedTextField(kategori, { kategori = it }, label = { Text("Kategori (FASILITAS/KEAMANAN/KEBERSIHAN/SOSIAL)") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(judul, { judul = it }, label = { Text("Judul Laporan") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(lokasi, { lokasi = it }, label = { Text("Lokasi Kejadian") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(deskripsi, { deskripsi = it }, label = { Text("Deskripsi Lengkap") }, modifier = Modifier.fillMaxWidth(), minLines = 3) }
                    item {
                        Button(onClick = {
                            scope.launch {
                                repo.createPengaduan(kategori, judul, deskripsi, lokasi, "SEDANG")
                                    .onSuccess { feedback = it; showCreateDialog = false; refresh() }
                                    .onFailure { feedback = it.message }
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Kirim Pengaduan") }
                    }
                }
            }
        }
    }
}
