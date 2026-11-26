package com.wargart.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wargart.core.ui.components.EmptyStateView
import com.wargart.core.ui.theme.CrimsonDanger
import com.wargart.core.ui.theme.RoyalBlue
import com.wargart.domain.model.NotifikasiItem
import com.wargart.domain.model.UserSession
import com.wargart.domain.repository.WargaRtRepository
import kotlinx.coroutines.launch

@Composable
fun NotifikasiScreen(repo: WargaRtRepository) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<NotifikasiItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        repo.getNotifikasi().onSuccess { list = it }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Notifikasi", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = {
                scope.launch {
                    repo.markNotifikasiRead(null)
                    repo.getNotifikasi().onSuccess { list = it }
                }
            }) { Text("Tandai Semua Dibaca") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (list.isEmpty()) EmptyStateView("Belum ada notifikasi.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(list) { n ->
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Text("${n.tipe} • ${n.createdAt}", style = MaterialTheme.typography.labelMedium, color = RoyalBlue)
                        Text(n.judul, style = MaterialTheme.typography.titleMedium)
                        Text(n.pesan, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    repo: WargaRtRepository,
    session: UserSession,
    onLoggedOut: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val darkMode by repo.darkModeFlow.collectAsState(initial = false)
    val currentUrl by repo.apiBaseUrlFlow.collectAsState(initial = "")
    val pendingSync by repo.pendingSyncCountFlow.collectAsState(initial = 0)
    var urlInput by remember(currentUrl) { mutableStateOf(currentUrl) }
    var message by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(session.fullName, style = MaterialTheme.typography.titleLarge)
                    Text("${session.roleName} • ${session.email}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Mode Gelap (Dark Mode)", style = MaterialTheme.typography.titleMedium)
                        Text("Sesuaikan tampilan tema aplikasi", style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(checked = darkMode, onCheckedChange = { scope.launch { repo.setDarkMode(it) } })
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Sinkronisasi Data Offline", style = MaterialTheme.typography.titleMedium)
                    Text("Antrean pengajuan tertunda: $pendingSync item")
                    Button(onClick = {
                        scope.launch {
                            val synced = repo.syncPendingOfflineActions()
                            message = "$synced data offline berhasil disinkronkan ke server."
                        }
                    }) { Text("Sinkronkan Sekarang") }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Konfigurasi URL Backend API", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = urlInput, onValueChange = { urlInput = it }, label = { Text("Base API URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Button(onClick = {
                        scope.launch {
                            repo.setApiBaseUrl(urlInput.trim())
                            message = "URL API diperbarui."
                        }
                    }) { Text("Simpan URL API") }
                }
            }
        }

        message?.let {
            item { Text(it, color = RoyalBlue, style = MaterialTheme.typography.labelMedium) }
        }

        item {
            Button(
                onClick = {
                    scope.launch {
                        repo.logout()
                        onLoggedOut()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Keluar dari Akun")
            }
        }
    }
}
