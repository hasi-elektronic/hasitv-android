package de.hasi.hasitv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import de.hasi.hasitv.data.model.Channel
import de.hasi.hasitv.ui.theme.HasiColors
import de.hasi.hasitv.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onPlayChannel: (Channel) -> Unit,
    onOpenEpg: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val channels     by vm.filteredChannels.collectAsStateWithLifecycle()
    val groups       by vm.groups.collectAsStateWithLifecycle()
    val selectedGroup by vm.selectedGroup.collectAsStateWithLifecycle()
    val recentList   by vm.recentChannels.collectAsStateWithLifecycle()
    val isLoading    by vm.isLoading.collectAsStateWithLifecycle()
    val playlists    by vm.playlists.collectAsStateWithLifecycle()
    val searchQuery  by vm.searchQuery.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (playlists.isNotEmpty()) vm.refreshAll()
    }

    Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // ── Sidebar ─────────────────────────────────────────────
        Column(
            Modifier
                .width(220.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 24.dp)
        ) {
            // App logo
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = HasiColors.Red, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text("HasiTV", fontWeight = FontWeight.Bold, fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface)
            }

            Divider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline)

            // Top nav
            SideNavItem("TV Rehberi", Icons.Default.CalendarToday) { onOpenEpg() }
            SideNavItem("Ayarlar", Icons.Default.Settings) { onOpenSettings() }

            Divider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline)

            Text("GRUPLAR", Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                letterSpacing = 1.2.sp)

            LazyColumn {
                items(groups) { group ->
                    GroupItem(
                        title      = group,
                        isSelected = group == selectedGroup,
                        count      = when (group) {
                            "All"          -> vm.allChannels.value.size
                            "⭐ Favoriler" -> vm.allChannels.value.count { it.isFavorite }
                            else           -> vm.allChannels.value.count { it.group == group }
                        }
                    ) { vm.setGroup(group) }
                }
            }
        }

        // ── Main content ─────────────────────────────────────────
        Column(Modifier.weight(1f).fillMaxHeight()) {

            // Top bar
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value         = searchQuery,
                    onValueChange = vm::setSearch,
                    modifier      = Modifier.weight(1f).height(52.dp),
                    placeholder   = { Text("Kanal ara...") },
                    leadingIcon   = { Icon(Icons.Default.Search, null) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(10.dp)
                )
                if (isLoading) {
                    Spacer(Modifier.width(16.dp))
                    CircularProgressIndicator(Modifier.size(28.dp), color = HasiColors.Red, strokeWidth = 3.dp)
                }
            }

            // Recently watched row
            if (recentList.isNotEmpty() && searchQuery.isBlank()) {
                Text("Son İzlenenler", Modifier.padding(horizontal = 24.dp),
                    fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground)
                LazyRow(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recentList.take(10)) { ch ->
                        RecentChannelChip(ch) { onPlayChannel(ch) }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Empty state
            if (channels.isEmpty() && !isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Tv, null, Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (playlists.isEmpty()) "Playlist ekle" else "Kanal bulunamadı",
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                // Channel grid
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(180.dp),
                    Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(channels) { channel ->
                        ChannelCard(
                            channel    = channel,
                            onPlay     = { onPlayChannel(channel) },
                            onFavorite = { vm.toggleFavorite(channel) }
                        )
                    }
                }
            }
        }
    }
}

// ─── Sidebar items ────────────────────────────────────────────────
@Composable
fun SideNavItem(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Spacer(Modifier.width(10.dp))
        Text(title, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun GroupItem(title: String, isSelected: Boolean, count: Int, onClick: () -> Unit) {
    val bg = if (isSelected) HasiColors.Red.copy(alpha = 0.12f) else Color.Transparent
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title, Modifier.weight(1f),
            fontSize    = 14.sp,
            fontWeight  = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color       = if (isSelected) HasiColors.Red else MaterialTheme.colorScheme.onSurface,
            maxLines    = 1,
            overflow    = TextOverflow.Ellipsis
        )
        Text("$count", fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
    }
}

// ─── Channel Card ─────────────────────────────────────────────────
@Composable
fun ChannelCard(channel: Channel, onPlay: () -> Unit, onFavorite: () -> Unit) {
    var focused by remember { mutableStateOf(false) }

    Card(
        Modifier
            .aspectRatio(1.3f)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onPlay),
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (focused) MaterialTheme.colorScheme.surfaceVariant
                             else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(if (focused) 8.dp else 2.dp)
    ) {
        Box(Modifier.fillMaxSize().padding(12.dp)) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ChannelLogo(url = channel.logoUrl, name = channel.name, size = 56.dp)
                Spacer(Modifier.height(8.dp))
                Text(channel.name, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(channel.group, fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            // Favorite icon
            IconButton(
                onClick  = onFavorite,
                modifier = Modifier.align(Alignment.TopEnd).size(28.dp)
            ) {
                Icon(
                    if (channel.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    null,
                    tint = if (channel.isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─── Channel Logo ─────────────────────────────────────────────────
@Composable
fun ChannelLogo(url: String?, name: String, size: androidx.compose.ui.unit.Dp) {
    if (url != null) {
        AsyncImage(
            model              = url,
            contentDescription = name,
            modifier           = Modifier.size(size).clip(RoundedCornerShape(8.dp)),
            contentScale       = ContentScale.Fit
        )
    } else {
        Box(
            Modifier.size(size).clip(RoundedCornerShape(8.dp)).background(HasiColors.Red.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                name.take(2).uppercase(),
                fontSize   = (size.value * 0.35f).sp,
                fontWeight = FontWeight.Bold,
                color      = HasiColors.Red
            )
        }
    }
}

// ─── Recent chip ──────────────────────────────────────────────────
@Composable
fun RecentChannelChip(channel: Channel, onClick: () -> Unit) {
    Surface(
        onClick   = onClick,
        shape     = RoundedCornerShape(8.dp),
        color     = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            ChannelLogo(url = channel.logoUrl, name = channel.name, size = 28.dp)
            Spacer(Modifier.width(8.dp))
            Text(channel.name, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
