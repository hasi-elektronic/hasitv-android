package com.hasielectronic.hasitv.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.hasielectronic.hasitv.data.model.EpgProgram
import com.hasielectronic.hasitv.ui.theme.HasiColors
import com.hasielectronic.hasitv.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(vm: MainViewModel, onBack: () -> Unit) {
    val channel        by vm.currentChannel.collectAsStateWithLifecycle()
    val currentProgram by vm.currentProgram.collectAsStateWithLifecycle()
    val nextProgram    by vm.nextProgram.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var showOverlay by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var hasError    by remember { mutableStateOf(false) }
    var retryCount  by remember { mutableIntStateOf(0) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    // Load stream
    LaunchedEffect(channel) {
        channel?.let { ch ->
            hasError = false
            isBuffering = true
            val mediaItem = MediaItem.fromUri(ch.url)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }
    }

    // Player listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) { hasError = false }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                isBuffering = false
                if (retryCount < 3) {
                    retryCount++
                    exoPlayer.prepare()
                } else {
                    hasError = true
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Auto-hide overlay after 5s
    LaunchedEffect(showOverlay) {
        if (showOverlay) {
            delay(5000)
            showOverlay = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { showOverlay = !showOverlay }
    ) {
        // ── Video surface ────────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player           = exoPlayer
                    useController    = false
                    layoutParams     = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── Buffering indicator ──────────────────────────────────
        if (isBuffering) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = HasiColors.Red, strokeWidth = 4.dp, modifier = Modifier.size(56.dp))
            }
        }

        // ── Error state ──────────────────────────────────────────
        if (hasError) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ErrorOutline, null, Modifier.size(64.dp), tint = Color.White.copy(alpha = 0.7f))
                    Spacer(Modifier.height(16.dp))
                    Text("Yayın yüklenemedi", color = Color.White, fontSize = 20.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        hasError = false; retryCount = 0
                        exoPlayer.prepare()
                    }, colors = ButtonDefaults.buttonColors(containerColor = HasiColors.Red)) {
                        Text("Tekrar Dene")
                    }
                }
            }
        }

        // ── Overlay ──────────────────────────────────────────────
        AnimatedVisibility(
            visible = showOverlay,
            enter   = fadeIn(),
            exit    = fadeOut()
        ) {
            Box(Modifier.fillMaxSize()) {
                // Gradient
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent,
                                   Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                )

                // Top bar
                Row(
                    Modifier.fillMaxWidth().padding(24.dp).align(Alignment.TopStart),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(channel?.name ?: "", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(channel?.group ?: "", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    }
                }

                // Bottom: EPG info
                Column(
                    Modifier.fillMaxWidth().align(Alignment.BottomStart).padding(24.dp)
                ) {
                    currentProgram?.let { prog ->
                        EpgOverlayCard(current = prog, next = nextProgram)
                    }
                }
            }
        }
    }
}

// ─── EPG Overlay Card ────────────────────────────────────────────
@Composable
fun EpgOverlayCard(current: EpgProgram, next: EpgProgram?) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.65f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            // Current
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(HasiColors.Red, androidx.compose.foundation.shape.CircleShape))
                Spacer(Modifier.width(8.dp))
                Text("ŞIMDI", fontSize = 11.sp, color = HasiColors.Red, fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp)
            }
            Text(current.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            current.description?.let {
                Text(it, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp,
                    maxLines = 2, modifier = Modifier.padding(top = 4.dp))
            }
            // Progress bar
            LinearProgressIndicator(
                progress   = { current.progressFraction },
                modifier   = Modifier.fillMaxWidth().padding(top = 8.dp).height(3.dp),
                color      = HasiColors.Red,
                trackColor = Color.White.copy(alpha = 0.2f)
            )

            // Next
            next?.let {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowForward, null, Modifier.size(14.dp),
                        tint = Color.White.copy(alpha = 0.6f))
                    Spacer(Modifier.width(6.dp))
                    Text("Sonraki: ${it.title}", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("• ${formatTime(it.startTime)}", color = Color.White.copy(alpha = 0.45f), fontSize = 12.sp)
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
