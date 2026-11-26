package com.wargart.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wargart.core.ui.components.OfflineStatusBanner
import com.wargart.core.ui.theme.WargaRtTheme
import com.wargart.feature.auth.LoginScreen
import com.wargart.feature.dashboard.DashboardScreen
import com.wargart.feature.keuangan.KeuanganIuranScreen
import com.wargart.feature.layanan.LayananScreen
import com.wargart.feature.lingkungan.LingkunganScreen
import com.wargart.feature.settings.NotifikasiScreen
import com.wargart.feature.settings.SettingsScreen
import com.wargart.feature.warga.WargaKkScreen
import kotlinx.coroutines.launch

private data class NavTab(val route: String, val label: String, val icon: ImageVector)

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as WargaRtApp
        val repo = app.repository
        val networkMonitor = app.networkMonitor

        setContent {
            val darkMode by repo.darkModeFlow.collectAsState(initial = false)
            val session by repo.sessionFlow.collectAsState(initial = null)
            val isOnline by networkMonitor.isOnlineFlow.collectAsState(initial = true)
            val pendingSync by repo.pendingSyncCountFlow.collectAsState(initial = 0)

            WargaRtTheme(darkTheme = darkMode) {
                val activeSession = session
                if (activeSession == null) {
                    LoginScreen(repo = repo, onLoginSuccess = {})
                } else {
                    val navController = rememberNavController()
                    val drawerState = rememberDrawerState(DrawerValue.Closed)
                    val scope = rememberCoroutineScope()
                    val backStack by navController.currentBackStackEntryAsState()
                    val currentRoute = backStack?.destination?.route ?: "dashboard"

                    val tabs = listOf(
                        NavTab("dashboard", "Beranda", Icons.Rounded.Dashboard),
                        NavTab("warga", "Warga", Icons.Rounded.Groups),
                        NavTab("keuangan", "Keuangan", Icons.Rounded.AccountBalanceWallet),
                        NavTab("layanan", "Layanan", Icons.Rounded.Description),
                        NavTab("lingkungan", "Keamanan", Icons.Rounded.Security)
                    )

                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("wargaRT", style = MaterialTheme.typography.headlineMedium)
                                    Text("${activeSession.fullName} (${activeSession.roleName})", style = MaterialTheme.typography.bodyMedium)
                                }
                                HorizontalDivider()
                                tabs.forEach { t ->
                                    NavigationDrawerItem(
                                        label = { Text(t.label) },
                                        selected = currentRoute == t.route,
                                        icon = { Icon(t.icon, contentDescription = null) },
                                        onClick = {
                                            navController.navigate(t.route) { launchSingleTop = true }
                                            scope.launch { drawerState.close() }
                                        }
                                    )
                                }
                                NavigationDrawerItem(
                                    label = { Text("Notifikasi") },
                                    selected = currentRoute == "notifikasi",
                                    icon = { Icon(Icons.Rounded.Notifications, contentDescription = null) },
                                    onClick = {
                                        navController.navigate("notifikasi") { launchSingleTop = true }
                                        scope.launch { drawerState.close() }
                                    }
                                )
                                NavigationDrawerItem(
                                    label = { Text("Pengaturan") },
                                    selected = currentRoute == "settings",
                                    icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                                    onClick = {
                                        navController.navigate("settings") { launchSingleTop = true }
                                        scope.launch { drawerState.close() }
                                    }
                                )
                            }
                        }
                    ) {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = { Text("wargaRT") },
                                    navigationIcon = {
                                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                            Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                                        }
                                    },
                                    actions = {
                                        IconButton(onClick = { navController.navigate("notifikasi") { launchSingleTop = true } }) {
                                            Icon(Icons.Rounded.Notifications, contentDescription = "Notifikasi")
                                        }
                                        IconButton(onClick = { navController.navigate("settings") { launchSingleTop = true } }) {
                                            Icon(Icons.Rounded.Settings, contentDescription = "Pengaturan")
                                        }
                                    }
                                )
                            },
                            bottomBar = {
                                NavigationBar {
                                    tabs.forEach { tab ->
                                        NavigationBarItem(
                                            selected = currentRoute == tab.route,
                                            onClick = {
                                                navController.navigate(tab.route) {
                                                    popUpTo("dashboard") { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                                            label = { Text(tab.label) }
                                        )
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                                OfflineStatusBanner(isOnline = isOnline, pendingSyncCount = pendingSync)
                                NavHost(navController = navController, startDestination = "dashboard", modifier = Modifier.weight(1f)) {
                                    composable("dashboard") {
                                        DashboardScreen(repo, activeSession) { route ->
                                            navController.navigate(route) { launchSingleTop = true }
                                        }
                                    }
                                    composable("warga") { WargaKkScreen(repo, activeSession) }
                                    composable("keuangan") { KeuanganIuranScreen(repo, activeSession) }
                                    composable("layanan") { LayananScreen(repo, activeSession) }
                                    composable("lingkungan") { LingkunganScreen(repo, activeSession) }
                                    composable("notifikasi") { NotifikasiScreen(repo) }
                                    composable("settings") {
                                        SettingsScreen(repo, activeSession, onLoggedOut = {})
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
