package com.wargart.feature.keuangan

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
import com.wargart.core.ui.theme.EmeraldSuccess
import com.wargart.core.ui.theme.RoyalBlue
import com.wargart.core.util.Formatters
import com.wargart.domain.model.*
import com.wargart.domain.repository.WargaRtRepository
import kotlinx.coroutines.launch

@Composable
fun KeuanganIuranScreen(repo: WargaRtRepository, session: UserSession) {
    val scope = rememberCoroutineScope()
    var tabIndex by remember { mutableIntStateOf(0) }
    var saldoKas by remember { mutableDoubleStateOf(0.0) }
    var kasList by remember { mutableStateOf<List<KasTransaction>>(emptyList()) }
    var tagihanList by remember { mutableStateOf<List<TagihanItem>>(emptyList()) }
    var pembayaranList by remember { mutableStateOf<List<PembayaranItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showAddKas by remember { mutableStateOf(false) }
    var payDialogTagihan by remember { mutableStateOf<TagihanItem?>(null) }
    var activePaymentResult by remember { mutableStateOf<PembayaranItem?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            loading = true
            repo.getKasSummary().onSuccess { (saldo, txs) -> saldoKas = saldo; kasList = txs }
            repo.getTagihan().onSuccess { tagihanList = it }
            repo.getPembayaran().onSuccess { pembayaranList = it }
            loading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        floatingActionButton = {
            if (tabIndex == 0 && Formatters.isAdminRole(session.roleCode)) {
                FloatingActionButton(onClick = { showAddKas = true }, containerColor = RoyalBlue) {
                    Icon(Icons.Rounded.Add, contentDescription = "Catat Kas")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tabIndex) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Kas RT") })
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Tagihan Iuran") })
                Tab(selected = tabIndex == 2, onClick = { tabIndex = 2 }, text = { Text("Pembayaran") })
            }

            message?.let {
                Text(it, color = RoyalBlue, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium)
            }

            if (loading) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(4) { SkeletonBox() }
                }
            } else when (tabIndex) {
                0 -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        StatCard("Saldo Kas RT", Formatters.rupiah(saldoKas), Icons.Rounded.AccountBalanceWallet, EmeraldSuccess)
                    }
                    items(kasList) { tx ->
                        val isIn = tx.jenis == "PEMASUKAN"
                        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tx.judul, style = MaterialTheme.typography.titleMedium)
                                    Text("${tx.kategori} • ${tx.tanggalTransaksi}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    (if (isIn) "+" else "-") + Formatters.rupiah(tx.nominal),
                                    color = if (isIn) EmeraldSuccess else CrimsonDanger,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                1 -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (tagihanList.isEmpty()) item { EmptyStateView("Belum ada tagihan iuran.") }
                    items(tagihanList) { t ->
                        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(t.namaIuran, style = MaterialTheme.typography.titleMedium)
                                    AssistChip(onClick = {}, label = { Text(t.status) })
                                }
                                Text("${t.kepalaKeluargaNama} • Periode ${t.periodeBulan}/${t.periodeTahun}", style = MaterialTheme.typography.bodyMedium)
                                Text("Nominal: ${Formatters.rupiah(t.totalTagihan)} • Jatuh tempo: ${t.tanggalJatuhTempo}", style = MaterialTheme.typography.labelMedium)
                                if (t.status == "BELUM_LUNAS") {
                                    Button(onClick = { payDialogTagihan = t }, modifier = Modifier.fillMaxWidth()) {
                                        Text("Bayar Sekarang (QRIS / VA / Transfer)")
                                    }
                                }
                            }
                        }
                    }
                }

                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (pembayaranList.isEmpty()) item { EmptyStateView("Belum ada riwayat pembayaran.") }
                    items(pembayaranList) { p ->
                        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(p.orderId, style = MaterialTheme.typography.titleMedium)
                                    AssistChip(onClick = {}, label = { Text(p.status) })
                                }
                                Text("${p.namaIuran} • ${p.metodePembayaran}", style = MaterialTheme.typography.bodyMedium)
                                Text("Total: ${Formatters.rupiah(p.totalBayar)}", fontWeight = FontWeight.Bold)
                                p.vaNumber?.let { Text("Nomor VA: $it", color = RoyalBlue, style = MaterialTheme.typography.labelMedium) }
                                if (Formatters.isAdminRole(session.roleCode) && p.status == "MENUNGGU_VERIFIKASI") {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                repo.verifyPembayaran(p.id, true)
                                                    .onSuccess { message = it; refresh() }
                                                    .onFailure { message = it.message }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Verifikasi Lunas") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    payDialogTagihan?.let { t ->
        var metode by remember { mutableStateOf("QRIS") }
        var channel by remember { mutableStateOf("QRIS") }
        FullScreenFormDialog("Pembayaran Tagihan ${t.nomorTagihan}", onDismiss = { payDialogTagihan = null }) {
            item { Text("${t.namaIuran} • ${Formatters.rupiah(t.totalTagihan)}", style = MaterialTheme.typography.titleMedium) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = metode == "QRIS", onClick = { metode = "QRIS"; channel = "QRIS" }, label = { Text("QRIS Instan") })
                    FilterChip(selected = metode == "VIRTUAL_ACCOUNT", onClick = { metode = "VIRTUAL_ACCOUNT"; channel = "BCA" }, label = { Text("Virtual Account BCA (+Rp 2.500)") })
                    FilterChip(selected = metode == "TRANSFER_MANUAL", onClick = { metode = "TRANSFER_MANUAL"; channel = "TRANSFER_BANK" }, label = { Text("Transfer Bank Manual") })
                }
            }
            item {
                Button(
                    onClick = {
                        scope.launch {
                            repo.createPembayaran(t.id, metode, channel)
                                .onSuccess { activePaymentResult = it; payDialogTagihan = null; refresh() }
                                .onFailure { message = it.message; payDialogTagihan = null }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Buat Instruksi Pembayaran") }
            }
        }
    }

    activePaymentResult?.let { p ->
        AlertDialog(
            onDismissRequest = { activePaymentResult = null },
            confirmButton = { TextButton(onClick = { activePaymentResult = null }) { Text("Tutup") } },
            title = { Text("Instruksi Pembayaran") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Order ID: ${p.orderId}")
                    Text("Total: ${Formatters.rupiah(p.totalBayar)}")
                    p.vaNumber?.let { Text("Nomor Virtual Account: $it", fontWeight = FontWeight.Bold) }
                    p.qrisPayload?.let { Text("Kode QRIS: ${it.take(36)}...", style = MaterialTheme.typography.bodySmall) }
                    Text("Status akan otomatis diperbarui setelah pembayaran terkonfirmasi backend.")
                }
            }
        )
    }

    if (showAddKas) {
        var jenis by remember { mutableStateOf("PEMASUKAN") }
        var kategori by remember { mutableStateOf("IURAN_WARGA") }
        var judul by remember { mutableStateOf("") }
        var nominal by remember { mutableStateOf("") }
        var deskripsi by remember { mutableStateOf("") }
        FullScreenFormDialog("Catat Transaksi Kas RT", onDismiss = { showAddKas = false }) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = jenis == "PEMASUKAN", onClick = { jenis = "PEMASUKAN" }, label = { Text("Pemasukan") })
                    FilterChip(selected = jenis == "PENGELUARAN", onClick = { jenis = "PENGELUARAN" }, label = { Text("Pengeluaran") })
                }
            }
            item { OutlinedTextField(kategori, { kategori = it }, label = { Text("Kategori") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(judul, { judul = it }, label = { Text("Judul Transaksi") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(nominal, { nominal = it }, label = { Text("Nominal (Rp)") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(deskripsi, { deskripsi = it }, label = { Text("Deskripsi") }, modifier = Modifier.fillMaxWidth()) }
            item {
                Button(
                    onClick = {
                        scope.launch {
                            repo.createKasTransaction(jenis, kategori, judul, nominal.toDoubleOrNull() ?: 0.0, deskripsi)
                                .onSuccess { message = it; showAddKas = false; refresh() }
                                .onFailure { message = it.message }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Simpan Transaksi") }
            }
        }
    }
}
