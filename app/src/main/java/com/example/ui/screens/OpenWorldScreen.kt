package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.MissionEntity
import com.example.data.local.PlayerProfileEntity
import com.example.domain.model.SpiderSuit
import com.example.domain.model.SpiderWorldCatalog
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.SpiderCrimson
import com.example.ui.theme.SpiderDeepNavy
import com.example.ui.theme.SpiderMidnight
import com.example.ui.theme.SpiderSenseGold
import com.example.ui.theme.TextPureWhite
import com.example.ui.theme.TextSecondaryIce
import com.example.ui.theme.WebCyan
import com.example.ui.viewmodel.OpenWorldUiState
import kotlin.math.hypot

@Composable
fun OpenWorldScreen(
    worldState: OpenWorldUiState,
    profile: PlayerProfileEntity,
    equippedSuit: SpiderSuit,
    missions: List<MissionEntity>,
    onStepPhysics: (Float) -> Unit,
    onMoveDirection: (Boolean) -> Unit,
    onStartSwing: (Float?, Float?) -> Unit,
    onReleaseSwing: () -> Unit,
    onWebZip: (Float?, Float?) -> Unit,
    onWebStrike: () -> Unit,
    onWebShot: () -> Unit,
    onDodgeOrTrick: () -> Unit,
    onTriggerUltimate: () -> Unit,
    onLaunchMission: (MissionEntity) -> Unit,
    onAbortMission: () -> Unit,
    onCycleTimeOfDay: () -> Unit,
    onTogglePause: () -> Unit,
    onOpenMissionsTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    var animationTick by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(worldState.isPaused) {
        var lastNanos = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0.008f, 0.05f)
            lastNanos = now
            animationTick += dt
            onStepPhysics(dt)
        }
    }

    val collectedSet = remember(profile.collectiblesFoundCsv) {
        profile.collectiblesFoundSet()
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Interactive Open-World City Skyline & Physics Canvas
        OpenWorldCityCanvas(
            worldState = worldState,
            equippedSuit = equippedSuit,
            missions = missions,
            collectedBackpacks = collectedSet,
            animationTick = animationTick,
            onCanvasSkySwingPress = { wx, wy -> onStartSwing(wx, wy) },
            onCanvasSkyRelease = onReleaseSwing,
            onCanvasDoubleTapZip = { wx, wy -> onWebZip(wx, wy) }
        )

        // 2. Top Tactical HUD & Minimap Radar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TopTelemetryBar(
                worldState = worldState,
                profile = profile,
                equippedSuit = equippedSuit,
                onCycleTimeOfDay = onCycleTimeOfDay,
                onTogglePause = onTogglePause
            )

            ManhattanMinimapRadar(
                worldState = worldState,
                missions = missions,
                collectedBackpacks = collectedSet,
                onOpenMissionsTab = onOpenMissionsTab
            )

            // Active Mission Banner OR Free-Roam Nearby Mission Beacon Banner
            val activeMission = worldState.activeMission
            if (activeMission != null) {
                ActiveMissionHudCard(
                    missionRuntime = activeMission,
                    remainingEnemies = worldState.enemies.size,
                    remainingTargets = worldState.objectiveTargets.count { !it.isCompleted },
                    onAbort = onAbortMission
                )
            } else {
                worldState.nearbyMissionBeacon?.let { nearbyMission ->
                    NearbyBeaconPromptCard(
                        mission = nearbyMission,
                        onEngage = { onLaunchMission(nearbyMission) }
                    )
                }
            }

            // Spider-Sense Alert Banner
            AnimatedVisibility(
                visible = worldState.isSpiderSenseActive,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = SpiderSenseGold.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("spider_sense_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = SpiderMidnight,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.hud_spider_sense_alert),
                            style = MaterialTheme.typography.labelLarge,
                            color = SpiderMidnight
                        )
                    }
                }
            }
        }

        // 3. Bottom Arcade Web-Swinging & Combat Controls
        BottomSpiderControls(
            worldState = worldState,
            equippedSuit = equippedSuit,
            onMoveDirection = onMoveDirection,
            onStartSwing = { onStartSwing(null, null) },
            onReleaseSwing = onReleaseSwing,
            onWebZip = { onWebZip(null, null) },
            onWebStrike = onWebStrike,
            onWebShot = onWebShot,
            onDodgeOrTrick = onDodgeOrTrick,
            onTriggerUltimate = onTriggerUltimate,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun TopTelemetryBar(
    worldState: OpenWorldUiState,
    profile: PlayerProfileEntity,
    equippedSuit: SpiderSuit,
    onCycleTimeOfDay: () -> Unit,
    onTogglePause: () -> Unit
) {
    val speedMetersPerSec = (hypot(worldState.heroVx, worldState.heroVy) * 0.12f).toInt()
    val altitudeMeters = (worldState.heroY * 0.25f).toInt()

    Surface(
        color = SpiderMidnight.copy(alpha = 0.84f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WebCyan.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // District & Suit Badge
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(worldState.currentDistrict.accentColorHex))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = worldState.currentDistrict.name.uppercase(),
                            style = MaterialTheme.typography.titleSmall,
                            color = WebCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LVL ${profile.level} • ⚡${profile.techTokens}",
                            style = MaterialTheme.typography.labelMedium,
                            color = SpiderSenseGold
                        )
                    }
                    Text(
                        text = "${equippedSuit.name} • $speedMetersPerSec m/s • ${altitudeMeters}m ALT",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryIce
                    )
                }

                // Combo Counter Pill
                if (worldState.comboCount > 0) {
                    Surface(
                        color = SpiderCrimson,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = "x${worldState.comboCount} COMBO",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPureWhite,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onCycleTimeOfDay,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("cycle_time_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.NightsStay,
                        contentDescription = stringResource(R.string.btn_toggle_time_of_day),
                        tint = SpiderSenseGold
                    )
                }

                IconButton(
                    onClick = onTogglePause,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("pause_game_button")
                ) {
                    Icon(
                        imageVector = if (worldState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = stringResource(R.string.btn_pause_game),
                        tint = WebCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // HP, Web Fluid, and Focus Ultimate Bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TelemetryMeter(
                    label = "HP ${worldState.heroHealth}",
                    progress = worldState.heroHealth / 100f,
                    color = SpiderCrimson,
                    modifier = Modifier.weight(1f)
                )
                TelemetryMeter(
                    label = "WEB ${worldState.webFluid.toInt()}%",
                    progress = worldState.webFluid / 100f,
                    color = WebCyan,
                    modifier = Modifier.weight(1f)
                )
                TelemetryMeter(
                    label = if (worldState.focusMeter >= 100f) "ULT READY!" else "FOCUS ${worldState.focusMeter.toInt()}%",
                    progress = worldState.focusMeter / 100f,
                    color = SpiderSenseGold,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TelemetryMeter(
    label: String,
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            color = color,
            trackColor = color.copy(alpha = 0.2f),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
    }
}

@Composable
private fun ManhattanMinimapRadar(
    worldState: OpenWorldUiState,
    missions: List<MissionEntity>,
    collectedBackpacks: Set<String>,
    onOpenMissionsTab: () -> Unit
) {
    Surface(
        color = SpiderDeepNavy.copy(alpha = 0.82f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .border(1.dp, WebCyan.copy(alpha = 0.28f), RoundedCornerShape(10.dp))
            .clickable(onClick = onOpenMissionsTab)
            .testTag("minimap_radar_bar")
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val midY = h * 0.5f

                // District dividers
                for (i in 1..4) {
                    val dx = w * (i / 5f)
                    drawLine(
                        color = Color.White.copy(alpha = 0.2f),
                        start = Offset(dx, 4f),
                        end = Offset(dx, h - 4f),
                        strokeWidth = 2f
                    )
                }

                // Uncollected Backpacks (Green dots)
                for (bp in SpiderWorldCatalog.backpacks) {
                    if (bp.id in collectedBackpacks) continue
                    val bx = (bp.worldX / SpiderWorldCatalog.WORLD_WIDTH) * w
                    drawCircle(
                        color = CyberGreen,
                        radius = 3.5f,
                        center = Offset(bx, midY + 5f)
                    )
                }

                // Mission Beacons (Gold dots)
                for (m in missions) {
                    val mx = (m.targetWorldX / SpiderWorldCatalog.WORLD_WIDTH) * w
                    drawCircle(
                        color = if (m.isCompleted) WebCyan else SpiderSenseGold,
                        radius = 4.5f,
                        center = Offset(mx, midY - 4f)
                    )
                }

                // Active Enemies (Red blips)
                for (e in worldState.enemies) {
                    val ex = (e.x / SpiderWorldCatalog.WORLD_WIDTH) * w
                    drawCircle(
                        color = SpiderCrimson,
                        radius = if (e.type.isBoss) 6.5f else 4f,
                        center = Offset(ex, midY)
                    )
                }

                // Spider-Hero Live Radar Blip
                val hx = (worldState.heroX / SpiderWorldCatalog.WORLD_WIDTH) * w
                drawCircle(
                    color = Color.White,
                    radius = 7f,
                    center = Offset(hx, midY)
                )
                drawCircle(
                    color = SpiderCrimson,
                    radius = 5f,
                    center = Offset(hx, midY)
                )
            }
        }
    }
}

@Composable
private fun ActiveMissionHudCard(
    missionRuntime: com.example.domain.model.ActiveMissionRuntime,
    remainingEnemies: Int,
    remainingTargets: Int,
    onAbort: () -> Unit
) {
    Surface(
        color = SpiderDeepNavy.copy(alpha = 0.90f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, SpiderSenseGold, RoundedCornerShape(12.dp))
            .testTag("active_mission_hud_card")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ACTIVE OP: ${missionRuntime.title.uppercase()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = SpiderSenseGold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "⏱ ${missionRuntime.timeRemainingSeconds.toInt()}s",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (missionRuntime.timeRemainingSeconds < 20f) SpiderCrimson else WebCyan
                    )
                }
                Text(
                    text = missionRuntime.currentInstruction,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPureWhite
                )
                Text(
                    text = "Threats Left: $remainingEnemies • Objectives Left: $remainingTargets • Score: ${missionRuntime.score}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondaryIce
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onAbort,
                colors = ButtonDefaults.buttonColors(containerColor = SpiderCrimson.copy(alpha = 0.85f)),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("abort_mission_button")
            ) {
                Text(
                    text = stringResource(R.string.btn_abort_mission),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun NearbyBeaconPromptCard(
    mission: MissionEntity,
    onEngage: () -> Unit
) {
    Surface(
        color = SpiderDeepNavy.copy(alpha = 0.92f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, WebCyan, RoundedCornerShape(12.dp))
            .testTag("nearby_mission_prompt_card")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.hud_mission_beacon_nearby),
                    style = MaterialTheme.typography.labelSmall,
                    color = WebCyan
                )
                Text(
                    text = mission.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPureWhite
                )
                Text(
                    text = mission.objectiveSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryIce
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onEngage,
                colors = ButtonDefaults.buttonColors(containerColor = SpiderCrimson),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("engage_nearby_mission_button")
            ) {
                Text(
                    text = stringResource(R.string.btn_start_nearby_mission),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun BottomSpiderControls(
    worldState: OpenWorldUiState,
    equippedSuit: SpiderSuit,
    onMoveDirection: (Boolean) -> Unit,
    onStartSwing: () -> Unit,
    onReleaseSwing: () -> Unit,
    onWebZip: () -> Unit,
    onWebStrike: () -> Unit,
    onWebShot: () -> Unit,
    onDodgeOrTrick: () -> Unit,
    onTriggerUltimate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Ultimate Power Ready Bar when Focus == 100%
        AnimatedVisibility(
            visible = worldState.focusMeter >= 100f,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Button(
                onClick = onTriggerUltimate,
                colors = ButtonDefaults.buttonColors(containerColor = SpiderSenseGold),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("ultimate_power_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = null,
                    tint = SpiderMidnight
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${stringResource(R.string.btn_ultimate)}: ${equippedSuit.ultimateName.uppercase()}",
                    style = MaterialTheme.typography.labelLarge,
                    color = SpiderMidnight,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Left Hand: Direction & Spider-Sense Dodge / Aerial Trick
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = if (worldState.isSpiderSenseActive) SpiderSenseGold else SpiderDeepNavy.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .border(
                            width = 1.5.dp,
                            color = if (worldState.isSpiderSenseActive) TextPureWhite else WebCyan.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable(onClick = onDodgeOrTrick)
                        .testTag("dodge_trick_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = stringResource(R.string.btn_dodge),
                            tint = if (worldState.isSpiderSenseActive) SpiderMidnight else WebCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (worldState.isSpiderSenseActive) "COUNTER DODGE!" else stringResource(R.string.btn_dodge),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (worldState.isSpiderSenseActive) SpiderMidnight else TextPureWhite
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = SpiderDeepNavy.copy(alpha = 0.88f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .size(56.dp)
                            .border(1.dp, WebCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .clickable { onMoveDirection(false) }
                            .testTag("move_left_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Move Left",
                                tint = TextPureWhite
                            )
                        }
                    }

                    Surface(
                        color = SpiderDeepNavy.copy(alpha = 0.88f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .size(56.dp)
                            .border(1.dp, WebCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .clickable { onMoveDirection(true) }
                            .testTag("move_right_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Move Right",
                                tint = TextPureWhite
                            )
                        }
                    }
                }
            }

            // Right Hand: Web-Swing, Web-Zip, Web-Shot, Strike
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionPillButton(
                        label = stringResource(R.string.btn_web_shot),
                        icon = Icons.Default.GpsFixed,
                        containerColor = SpiderDeepNavy.copy(alpha = 0.9f),
                        borderColor = WebCyan,
                        onClick = onWebShot,
                        testTag = "web_shot_button"
                    )
                    ActionPillButton(
                        label = stringResource(R.string.btn_web_zip),
                        icon = Icons.Default.Explore,
                        containerColor = SpiderDeepNavy.copy(alpha = 0.9f),
                        borderColor = SpiderSenseGold,
                        onClick = onWebZip,
                        testTag = "web_zip_button"
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionPillButton(
                        label = stringResource(R.string.btn_web_strike),
                        icon = Icons.Default.Bolt,
                        containerColor = SpiderCrimson.copy(alpha = 0.92f),
                        borderColor = TextPureWhite.copy(alpha = 0.6f),
                        onClick = onWebStrike,
                        testTag = "web_strike_button"
                    )

                    // Primary Hold-or-Tap Web Swing Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = if (worldState.isSwinging) {
                                        listOf(WebCyan, Color(0xFF1E56F0))
                                    } else {
                                        listOf(SpiderCrimson, Color(0xFFB71C1C))
                                    }
                                )
                            )
                            .border(2.dp, WebCyan, RoundedCornerShape(16.dp))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        onStartSwing()
                                        tryAwaitRelease()
                                        onReleaseSwing()
                                    }
                                )
                            }
                            .padding(horizontal = 18.dp)
                            .testTag("hold_swing_button")
                    ) {
                        Text(
                            text = if (worldState.isSwinging) "SWINGING!" else stringResource(R.string.btn_swing_hold),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (worldState.isSwinging) SpiderMidnight else TextPureWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionPillButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .height(52.dp)
            .minimumInteractiveComponentSize()
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = borderColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = TextPureWhite
            )
        }
    }
}
