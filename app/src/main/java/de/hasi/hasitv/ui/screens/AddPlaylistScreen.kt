package com.hasielectronic.hasitv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hasielectronic.hasitv.data.model.Playlist
import com.hasielectronic.hasitv.data.model.PlaylistType
import com.hasielectronic.hasitv.data.parser.XtreamConfig
import com.hasielectronic.hasitv.data.parser.XtreamService
import com.hasielectronic.hasitv.ui.theme.HasiColors
import com.hasielectronic.hasitv.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun AddPlaylistScreen(vm: MainViewModel, onBack: () -> Unit) {
    var selectedType   by remember { mutableStateOf(PlaylistType.M3U_URL) }
    var name           by remember { mutableStateOf("") }
    var m3uUrl         by remember { mutableStateOf("") }
    var xtreamHost     by remember { mutableStateOf("") }
    var xtreamUser     by remember { mutableStateOf("") }
    var xtreamPass     by remember { mutableStateOf("") }
    var isValidating   by remember { mutableStateOf(false) }
    var errorMsg       by remember { mutableStateOf<String?>(null) }
    val scope          = rememberCoroutineScope()

    val isValid = name.isNotBlank() && when (selectedType) {
        PlaylistType.M3U_URL  -> m3uUrl.isNotBlank()
        PlaylistType.XTREAM   -> xtreamHost.isNotBlank() && xtreamUser.isNotBlank() && xtreamPass.isNotBlank()
        PlaylistType.M3U_LOCAL -> false
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = MaterialTheme.colorScheme.onBackground)
            }
            Text("Playlist Ekle", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground)
        }

        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // Name
            OutlinedTextField(
                value         = name,
                onValueChange = { name = it },
                label         = { Text("Playlist Adı") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                shape         = RoundedCornerShape(10.dp)
            )

            // Type selector
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(PlaylistType.M3U_URL to "M3U URL", PlaylistType.XTREAM to "Xtream").forEach { (type, label) ->
                    val selected = selectedType == type
                    Button(
                        onClick = { selectedType = type },
                        colors  = ButtonDefaults.buttonColors(
                            containerColor = if (selected) HasiColors.Red else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor   = if (selected) androidx.compose.ui.graphics.Color.White
                                             else MaterialTheme.colorScheme.onSurface
                        ),
                        shape   = RoundedCornerShape(8.dp)
                    ) { Text(label, fontSize = 13.sp) }
                }
            }

            // M3U URL fields
            if (selectedType == PlaylistType.M3U_URL) {
                OutlinedTextField(
                    value         = m3uUrl,
                    onValueChange = { m3uUrl = it },
                    label         = { Text("M3U URL") },
                    placeholder   = { Text("https://example.com/playlist.m3u") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    shape         = RoundedCornerShape(10.dp)
                )
            }

            // Xtream fields
            if (selectedType == PlaylistType.XTREAM) {
                OutlinedTextField(
                    value = xtreamHost, onValueChange = { xtreamHost = it },
                    label = { Text("Sunucu (http://host:port)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = xtreamUser, onValueChange = { xtreamUser = it },
                    label = { Text("Kullanıcı Adı") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = xtreamPass, onValueChange = { xtreamPass = it },
                    label = { Text("Şifre") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Error
            errorMsg?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            // Submit button
            Button(
                onClick = {
                    scope.launch {
                        isValidating = true
                        errorMsg     = null
                        try {
                            val playlist = Playlist(
                                id              = UUID.randomUUID().toString(),
                                name            = name,
                                type            = selectedType,
                                m3uUrl          = m3uUrl.takeIf { selectedType == PlaylistType.M3U_URL },
                                xtreamHost      = xtreamHost.takeIf { selectedType == PlaylistType.XTREAM },
                                xtreamUsername  = xtreamUser.takeIf { selectedType == PlaylistType.XTREAM },
                                xtreamPassword  = xtreamPass.takeIf { selectedType == PlaylistType.XTREAM }
                            )

                            // Quick validation
                            if (selectedType == PlaylistType.XTREAM) {
                                val ok = XtreamService.verify(XtreamConfig(xtreamHost, xtreamUser, xtreamPass))
                                if (!ok) throw Exception("Geçersiz kullanıcı adı veya şifre")
                            }

                            vm.addPlaylist(playlist)
                            onBack()
                        } catch (e: Exception) {
                            errorMsg = e.message
                        }
                        isValidating = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled  = isValid && !isValidating,
                colors   = ButtonDefaults.buttonColors(containerColor = HasiColors.Red),
                shape    = RoundedCornerShape(10.dp)
            ) {
                if (isValidating) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Doğrulanıyor...")
                } else {
                    Text("Ekle", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
