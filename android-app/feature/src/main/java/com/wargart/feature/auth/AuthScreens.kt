package com.wargart.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.wargart.core.ui.theme.GoldAccent
import com.wargart.core.ui.theme.NavyPrimary
import com.wargart.core.ui.theme.PureWhite
import com.wargart.core.ui.theme.RoyalBlue
import com.wargart.domain.repository.WargaRtRepository
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    repo: WargaRtRepository,
    onLoginSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isRegisterMode by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyPrimary, RoyalBlue)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Rounded.Apartment, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(56.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("wargaRT", style = MaterialTheme.typography.headlineMedium, color = PureWhite, fontWeight = FontWeight.ExtraBold)
            Text("Administrasi Warga & Keuangan RT/RW", style = MaterialTheme.typography.bodyMedium, color = PureWhite.copy(alpha = 0.8f))
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isRegisterMode) "Daftar Akun Warga" else "Masuk ke Akun",
                        style = MaterialTheme.typography.titleLarge
                    )

                    errorMsg?.let {
                        Surface(color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp)) {
                            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    successMsg?.let {
                        Surface(color = GoldAccent.copy(alpha = 0.18f), shape = RoundedCornerShape(10.dp)) {
                            Text(it, color = NavyPrimary, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    if (isRegisterMode) {
                        OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Nama Lengkap") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("No. WhatsApp") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(if (isRegisterMode) "Username" else "Username atau Email") },
                        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, contentDescription = null)
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (username.isBlank() || password.isBlank()) {
                                errorMsg = "Username dan password wajib diisi."
                                return@Button
                            }
                            loading = true
                            errorMsg = null
                            successMsg = null
                            scope.launch {
                                if (isRegisterMode) {
                                    repo.register(username.trim(), email.trim(), fullName.trim(), phone.trim(), password)
                                        .onSuccess {
                                            successMsg = it
                                            isRegisterMode = false
                                        }
                                        .onFailure { errorMsg = it.message }
                                } else {
                                    repo.login(username.trim(), password)
                                        .onSuccess { onLoginSuccess() }
                                        .onFailure { errorMsg = it.message }
                                }
                                loading = false
                            }
                        },
                        enabled = !loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PureWhite, strokeWidth = 2.dp)
                        else Text(if (isRegisterMode) "Daftar Sekarang" else "Masuk", fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = {
                            isRegisterMode = !isRegisterMode
                            errorMsg = null
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(if (isRegisterMode) "Sudah punya akun? Masuk" else "Warga baru? Daftar akun di sini")
                    }
                }
            }
        }
    }
}
