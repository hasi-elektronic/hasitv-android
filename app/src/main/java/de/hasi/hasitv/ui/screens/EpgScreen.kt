package com.hasielectronic.hasitv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hasielectronic.hasitv.data.model.Channel
import com.hasielectronic.hasitv.data.model.EpgProgram
import com.hasielectronic.hasitv.ui.theme.HasiColors
import com.hasielectronic.hasitv.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EpgScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onPlayChannel: (Channel) -> Unit
) {
    val channels  by vm.allChannels.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val scope     = rememberCoroutineScope()
    val epgUrl    = vm.repo.epgUrl

    // EPG programs per channel (loaded lazily)
    val programsMap = remember { mutableStateMapOf<String, List<EpgProgram>>() }

    LaunchedEffect(channels) {
        channels.take(50).forEach { ch ->
            val id = ch.epgId ?: ch.id
            if (!programsMap.containsKey(id)) {
                programsMap[id] = vm.repo.getProgramsForChannel(id)
            }
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = MaterialTheme.colorScheme.onBackground)
            }
            Text("TV Rehberi", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
            if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = HasiColors.Red, strokeWidth = 3.dp)
            IconButton(onClick = { scope.launch { vm.refreshEpg(force = true) } }) {
                Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.onBackground)
            }
        }

        if (epgUrl.isBlank()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Ayarlar'dan XMLTV URL ekle", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
            return@Column
        }

        // Time header
        val scrollState = rememberScrollState()
        val hourWidth   = 200.dp
        val startTime   = remember {
            Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, -1) }.time
        }

        // Sync horizontal scroll
        Row(Modifier.fillMaxWidth()) {
            // Channel label column
            Spacer(Modifier.width(160.dp))
            // Time labels
            Row(Modifier.horizontalScroll(scrollState)) {
                repeat(6) { h ->
                    val cal = Calendar.getInstance()
                    cal.time = startTime
                    cal.add(Calendar.HOUR_OF_DAY, h)
                    Text(
                        formatTime(cal.time),
                        Modifier.width(hourWidth).padding(horizontal = 8.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        }

        Divider(color = MaterialTheme.colorScheme.outline)

        // Channel rows
        LazyColumn(Modifier.fillMaxSize()) {
            items(channels.take(50)) { channel ->
                val epgId    = channel.epgId ?: channel.id
                val programs = programsMap[epgId] ?: emptyList()
                EpgRow(
                    channel    = channel,
                    programs   = programs,
                    startTime  = startTime,
                    hourWidth  = hourWidth,
                    scrollState= scrollState,
                    onPlay     = { onPlayChannel(channel) }
                )
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            }
        }
    }
}

@Composable
fun EpgRow(
    channel: Channel,
    programs: List<EpgProgram>,
    startTime: Date,
    hourWidth: androidx.compose.ui.unit.Dp,
    scrollState: androidx.compose.foundation.ScrollState,
    onPlay: () -> Unit
) {
    Row(Modifier.height(64.dp)) {
        // Channel label
        Row(
            Modifier.width(160.dp).fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onPlay)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChannelLogo(url = channel.logoUrl, name = channel.name, size = 32.dp)
            Spacer(Modifier.width(8.dp))
            Text(channel.name, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface)
        }

        // Program blocks (horizontally scrollable, synced)
        Box(
            Modifier.weight(1f).fillMaxHeight()
                .horizontalScroll(scrollState)
        ) {
            Row(Modifier.fillMaxHeight()) {
                programs.forEach { prog ->
                    val offsetHours = (prog.startTime - startTime.time) / 3_600_000f
                    val durationHours = (prog.endTime - prog.startTime) / 3_600_000f
                    val blockWidth = (durationHours * hourWidth.value).dp

                    if (blockWidth > 0.dp) {
                        Box(
                            Modifier
                                .width(blockWidth - 2.dp)
                                .fillMaxHeight()
                                .padding(vertical = 4.dp)
                                .background(
                                    if (prog.isLive) HasiColors.Red else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                prog.title,
                                fontSize = 12.sp,
                                fontWeight = if (prog.isLive) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (prog.isLive) Color.White else MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(2.dp))
                    }
                }
            }
        }
    }
}

private fun formatTime(date: Date): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
