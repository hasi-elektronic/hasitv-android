package com.hasielectronic.hasitv.ui.screens

import com.hasielectronic.hasitv.data.model.PlaylistType

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hasielectronic.hasitv.data.model.Playlist
import com.hasielectronic.hasitv.ui.theme.HasiColors
import com.hasielectronic.hasitv.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    vm: MainViewModel,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onBack: () -> Unit,
    onAddPlaylist: () -> Unit
) {
    val playlists     by vm.playlists.collectAsStateWithLifecycle()
    val scope         = rememberCoroutineScope()
    var epgUrl        by remember { mutableStateOf(vm.repo.epgUrl) }
    var showDeleteDlg by remember { mutableStateOf<Playlist?>(null) }
    var showClearDlg  by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Header
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = MaterialTheme.colorScheme.onBackground)
            }
            Text("Ayarlar", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground)
        }

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // ── Playlists ─────────────────────────────────────────
            item {
                SettingsSection(title = "Playlist'ler") {
                    playlists.forEach { pl ->
                        PlaylistRow(pl,
                            onDelete = { showDeleteDlg = pl },
                            onRefresh = { scope.launch { vm.repo.refreshPlaylist(pl) } }
                        )
                    }
                    Button(
                        onClick = onAddPlaylist,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = HasiColors.Red),
                        shape    = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Playlist Ekle")
                    }
                }
            }

            // ── EPG ───────────────────────────────────────────────
            item {
                SettingsSection(title = "EPG / TV Rehberi") {
                    Text("XMLTV URL", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value         = epgUrl,
                        onValueChange = { epgUrl = it },
                        modifier      = Modifier.fillMaxWidth(),
                        placeholder   = { Text("https://example.com/epg.xml") },
                        singleLine    = true,
                        shape         = RoundedCornerShape(10.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            vm.repo.epgUrl = epgUrl
                            scope.launch { vm.refreshEpg(force = true) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape    = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.width(8.dp))
                        Text("Kaydet & Yenile", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            // ── Appearance ────────────────────────────────────────
            item {
                SettingsSection(title = "Görünüm") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (darkTheme) Icons.Default.Bedtime else Icons.Default.WbSunny,
                            null, tint = HasiColors.Red
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(if (darkTheme) "Karanlık Tema" else "Aydınlık Tema",
                            Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                        Switch(
                            checked         = darkTheme,
                            onCheckedChange = { onToggleTheme() },
                            colors          = SwitchDefaults.colors(checkedThumbColor = HasiColors.Red,
                                checkedTrackColor = HasiColors.Red.copy(alpha = 0.3f))
                        )
                    }
                }
            }

            // ── About ─────────────────────────────────────────────
            item {
                SettingsSection(title = "Hakkında") {
                    InfoRow("Uygulama", "HasiTV")
                    InfoRow("Versiyon", "1.0.0")
                    InfoRow("Geliştirici", "Hasi Elektronic")
                    InfoRow("Yüklü Kanal", "${vm.allChannels.value.size}")
                }
            }

            // ── Danger ────────────────────────────────────────────
            item {
                OutlinedButton(
                    onClick  = { showClearDlg = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border   = ButtonDefaults.outlinedButtonBorder.copy()
                ) {
                    Icon(Icons.Default.Delete, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Tüm Verileri Temizle")
                }
            }
        }
    }

    // Delete playlist dialog
    showDeleteDlg?.let { pl ->
        AlertDialog(
            onDismissRequest = { showDeleteDlg = null },
            title   = { Text("Playlist Sil") },
            text    = { Text("\"${pl.name}\" silinsin mi? Kanallar da kaldırılır.") },
            confirmButton = {
                TextButton(onClick = { vm.deletePlaylist(pl); showDeleteDlg = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Sil")
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDlg = null }) { Text("İptal") } }
        )
    }

    // Clear all dialog
    if (showClearDlg) {
        AlertDialog(
            onDismissRequest = { showClearDlg = false },
            title   = { Text("Tüm Verileri Temizle") },
            text    = { Text("Tüm playlist, kanal ve önbellek silinecek. Geri alınamaz.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        vm.repo.epgLastFetched = 0L
                    }
                    showClearDlg = false
                }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Temizle")
                }
            },
            dismissButton = { TextButton(onClick = { showClearDlg = false }) { Text("İptal") } }
        )
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(title.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            letterSpacing = 1.2.sp, modifier = Modifier.padding(bottom = 12.dp))
        content()
    }
}

@Composable
fun PlaylistRow(playlist: Playlist, onDelete: () -> Unit, onRefresh: () -> Unit) {
    val subtitle = when (playlist.type) {
        PlaylistType.M3U_URL   -> playlist.m3uUrl ?: ""
        PlaylistType.M3U_LOCAL -> "Yerel dosya"
        PlaylistType.XTREAM    -> playlist.xtreamHost ?: ""
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.List, null, Modifier.size(20.dp), tint = HasiColors.Red)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(playlist.name, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), maxLines = 1)
        }
        Text("${playlist.channelCount} kanal", fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        IconButton(onClick = onRefresh, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Refresh, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Delete, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 14.sp)
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
    }
}
