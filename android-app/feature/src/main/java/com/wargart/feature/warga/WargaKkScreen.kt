package com.wargart.feature.warga

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
import com.wargart.core.ui.theme.GoldAccent
import com.wargart.core.ui.theme.RoyalBlue
import com.wargart.core.util.Formatters
import com.wargart.domain.model.KeluargaItem
import com.wargart.domain.model.UserSession
import com.wargart.domain.model.WargaItem
import com.wargart.domain.repository.WargaRtRepository
import kotlinx.coroutines.launch

@Composable
fun WargaKkScreen(repo: WargaRtRepository, session: UserSession) {
    val scope = rememberCoroutineScope()
    var tabIndex by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }
    var wargaList by remember { mutableStateOf<List<WargaItem>>(emptyList()) }
    var kkList by remember { mutableStateOf<List<KeluargaItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showAddWarga by remember { mutableStateOf(false) }
    var showAddKk by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            loading = true
            repo.getWarga(search).onSuccess { wargaList = it }
            repo.getKeluarga().onSuccess { kkList = it }
            loading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        floatingActionButton = {
            if (Formatters.isAdminRole(session.roleCode)) {
                FloatingActionButton(
                    onClick = { if (tabIndex == 0) showAddWarga = true else showAddKk = true },
                    containerColor = RoyalBlue
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Tambah")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tabIndex) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Data Warga") })
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Kartu Keluarga") })
            }

            feedback?.let {
                Text(it, color = RoyalBlue, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium)
            }

            if (tabIndex == 0) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it; refresh() },
                    placeholder = { Text("Cari nama atau alamat warga...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    singleLine = true
                )
            }

            if (loading) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(4) { SkeletonBox() }
                }
            } else if (tabIndex == 0) {
                if (wargaList.isEmpty()) EmptyStateView("Belum ada data warga.")
                else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(wargaList) { w ->
                        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(w.namaLengkap, style = MaterialTheme.typography.titleMedium)
                                    AssistChip(onClick = {}, label = { Text(w.statusDomisili) })
                                }
                                Text("NIK: ${w.nikMasked} • ${if (w.jenisKelamin == "L") "Laki-laki" else "Perempuan"}", style = MaterialTheme.typography.bodyMedium)
                                Text(w.alamatLengkap, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Pekerjaan: ${w.pekerjaan ?: "-"} • WA: ${w.noWhatsapp ?: "-"}", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            } else {
                if (kkList.isEmpty()) EmptyStateView("Belum ada data Kartu Keluarga.")
                else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(kkList) { k ->
                        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                Text(k.kepalaKeluargaNama, style = MaterialTheme.typography.titleMedium)
                                Text("No. KK: ${k.noKkMasked}", color = GoldAccent, fontWeight = FontWeight.SemiBold)
                                Text("${k.alamat} (Blok ${k.blokRumah ?: "-"})", style = MaterialTheme.typography.bodyMedium)
                                Text("Status: ${k.statusHunian} • ${k.jumlahAnggota} Anggota", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddWarga) {
        var nik by remember { mutableStateOf("") }
        var nama by remember { mutableStateOf("") }
        var tempat by remember { mutableStateOf("") }
        var tgl by remember { mutableStateOf("1990-01-01") }
        var jk by remember { mutableStateOf("L") }
        var alamat by remember { mutableStateOf("") }
        var wa by remember { mutableStateOf("") }
        var pekerjaan by remember { mutableStateOf("") }

        FullScreenFormDialog("Tambah Data Warga", onDismiss = { showAddWarga = false }) {
            item { OutlinedTextField(nik, { nik = it }, label = { Text("NIK (16 Digit)") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(nama, { nama = it }, label = { Text("Nama Lengkap") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(tempat, { tempat = it }, label = { Text("Tempat Lahir") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(tgl, { tgl = it }, label = { Text("Tanggal Lahir (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth()) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = jk == "L", onClick = { jk = "L" }, label = { Text("Laki-laki") })
                    FilterChip(selected = jk == "P", onClick = { jk = "P" }, label = { Text("Perempuan") })
                }
            }
            item { OutlinedTextField(alamat, { alamat = it }, label = { Text("Alamat Lengkap") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(wa, { wa = it }, label = { Text("No. WhatsApp") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(pekerjaan, { pekerjaan = it }, label = { Text("Pekerjaan") }, modifier = Modifier.fillMaxWidth()) }
            item {
                Button(
                    onClick = {
                        scope.launch {
                            repo.createWarga(nik, nama, tempat, tgl, jk, alamat, wa, pekerjaan)
                                .onSuccess { feedback = it; showAddWarga = false; refresh() }
                                .onFailure { feedback = it.message }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Simpan Data Warga") }
            }
        }
    }

    if (showAddKk) {
        var noKk by remember { mutableStateOf("") }
        var kepala by remember { mutableStateOf("") }
        var alamat by remember { mutableStateOf("") }
        var blok by remember { mutableStateOf("") }

        FullScreenFormDialog("Tambah Kartu Keluarga", onDismiss = { showAddKk = false }) {
            item { OutlinedTextField(noKk, { noKk = it }, label = { Text("Nomor KK (16 Digit)") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(kepala, { kepala = it }, label = { Text("Nama Kepala Keluarga") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(alamat, { alamat = it }, label = { Text("Alamat") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(blok, { blok = it }, label = { Text("Blok / No. Rumah") }, modifier = Modifier.fillMaxWidth()) }
            item {
                Button(
                    onClick = {
                        scope.launch {
                            repo.createKeluarga(noKk, kepala, alamat, blok)
                                .onSuccess { feedback = it; showAddKk = false; refresh() }
                                .onFailure { feedback = it.message }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Simpan Kartu Keluarga") }
            }
        }
    }
}
