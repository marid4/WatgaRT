package com.wargart.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wargart.core.ui.components.*
import com.wargart.core.ui.theme.*
import com.wargart.core.util.Formatters
import com.wargart.domain.model.DashboardSummary
import com.wargart.domain.model.ResourceState
import com.wargart.domain.model.UserSession
import com.wargart.domain.repository.WargaRtRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    repo: WargaRtRepository,
    session: UserSession,
    onNavigate: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<ResourceState<DashboardSummary>>(ResourceState.Loading) }
    var refreshing by remember { mutableStateOf(false) }

    fun load() {
        scope.launch {
            if (!refreshing) state = ResourceState.Loading
            repo.getDashboard()
                .onSuccess { state = ResourceState.Success(it) }
                .onFailure { state = ResourceState.Error(it.message ?: "Gagal memuat data dashboard") }
            refreshing = false
        }
    }

    LaunchedEffect(Unit) { load() }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            refreshing = true
            load()
        }
    ) {
        when (val s = state) {
            is ResourceState.Loading -> {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SkeletonBox(height = 140.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SkeletonBox(modifier = Modifier.weight(1f), height = 96.dp)
                        SkeletonBox(modifier = Modifier.weight(1f), height = 96.dp)
                    }
                    SkeletonBox(height = 180.dp)
                }
            }
            is ResourceState.Error -> ErrorStateView(s.message, onRetry = { load() })
            is ResourceState.Success -> {
                val d = s.data
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Brush.linearGradient(listOf(NavyPrimary, RoyalBlue)))
                                .padding(20.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Halo, ${session.fullName}", color = PureWhite, style = MaterialTheme.typography.titleMedium)
                                        Text(session.roleName, color = GoldAccent, style = MaterialTheme.typography.labelMedium)
                                    }
                                    IconButton(onClick = { onNavigate("notifikasi") }) {
                                        Icon(Icons.Rounded.Notifications, contentDescription = "Notifikasi", tint = PureWhite)
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Total Kas RT Terkini", color = PureWhite.copy(alpha = 0.78f), style = MaterialTheme.typography.labelMedium)
                                Text(Formatters.rupiah(d.totalKas), color = PureWhite, style = MaterialTheme.typography.headlineMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Tagihan belum dibayar: ${d.tagihanBelumLunasCount} (${Formatters.rupiah(d.tagihanBelumLunasNominal)})",
                                    color = GoldAccent,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            StatCard("Total Warga", "${d.totalWarga} Jiwa", Icons.Rounded.Groups, RoyalBlue, Modifier.weight(1f))
                            StatCard("Kepala Keluarga", "${d.totalKk} KK", Icons.Rounded.Home, GoldAccent, Modifier.weight(1f))
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            StatCard("Laki-laki / Perempuan", "${d.totalLaki} / ${d.totalPerempuan}", Icons.Rounded.Wc, EmeraldSuccess, Modifier.weight(1f))
                            StatCard("Warga Aktif", "${d.totalAktif} Aktif", Icons.Rounded.VerifiedUser, RoyalBlueLight, Modifier.weight(1f))
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Grafik Pemasukan & Pengeluaran Kas", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(12.dp))
                                val bars = d.grafikKas.map {
                                    CashChartBar(it.bulan.takeLast(5), it.pemasukan.toFloat(), it.pengeluaran.toFloat())
                                }
                                if (bars.isEmpty()) {
                                    Text("Belum ada riwayat transaksi kas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    CashFlowBarChart(bars)
                                }
                            }
                        }
                    }

                    item {
                        Text("Akses Cepat Layanan", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            QuickMenuChip("Surat", Icons.Rounded.Description, Modifier.weight(1f)) { onNavigate("layanan") }
                            QuickMenuChip("Iuran", Icons.Rounded.Payments, Modifier.weight(1f)) { onNavigate("keuangan") }
                            QuickMenuChip("SOS", Icons.Rounded.Warning, Modifier.weight(1f)) { onNavigate("lingkungan") }
                            QuickMenuChip("Warga", Icons.Rounded.Badge, Modifier.weight(1f)) { onNavigate("warga") }
                        }
                    }

                    item {
                        Text("Pengumuman & Aktivitas Terbaru", style = MaterialTheme.typography.titleMedium)
                    }
                    if (d.aktivitasTerbaru.isEmpty()) {
                        item { EmptyStateView("Belum ada pengumuman terbaru.") }
                    } else {
                        items(d.aktivitasTerbaru) { ann ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { onNavigate("layanan") },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(ann.kategori, style = MaterialTheme.typography.labelMedium, color = RoyalBlue)
                                    Text(ann.judul, style = MaterialTheme.typography.titleMedium)
                                    Text(ann.publishedAt, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickMenuChip(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 6.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = RoyalBlue)
            Spacer(modifier = Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
