package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.MissionEntity
import com.example.data.local.PlayerProfileEntity
import com.example.domain.model.MissionCategory
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.MysticPurple
import com.example.ui.theme.SpiderCrimson
import com.example.ui.theme.SpiderMidnight
import com.example.ui.theme.SpiderSenseGold
import com.example.ui.theme.SpiderSurfaceCard
import com.example.ui.theme.SpiderSurfaceElevated
import com.example.ui.theme.TextPureWhite
import com.example.ui.theme.TextSecondaryIce
import com.example.ui.theme.WebCyan

@Composable
fun MissionsHubScreen(
    missions: List<MissionEntity>,
    profile: PlayerProfileEntity,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    onLaunchMission: (MissionEntity) -> Unit,
    onSetWaypoint: (MissionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredMissions = missions.filter { mission ->
        when (selectedFilter) {
            "STORY_BOSS" -> mission.category == MissionCategory.STORY.name || mission.category == MissionCategory.BOSS.name
            "PATROL_RESCUE" -> mission.category == MissionCategory.CRIME_PATROL.name || mission.category == MissionCategory.STEALTH_RESCUE.name
            "TRIALS" -> mission.category == MissionCategory.TIME_TRIAL.name
            else -> true
        }
    }

    val completedCount = missions.count { it.isCompleted }
    val totalCount = missions.size.coerceAtLeast(1)
    val bossMission = missions.firstOrNull { it.category == MissionCategory.BOSS.name }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpiderMidnight),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .testTag("missions_lazy_column"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Hero Banner & Campaign Liberation Summary
            item {
                CampaignHeroBannerCard(
                    completedCount = completedCount,
                    totalCount = totalCount,
                    profile = profile
                )
            }

            // 2. Boss Threat Spotlight Card
            if (bossMission != null) {
                item {
                    BossThreatSpotlightCard(
                        bossMission = bossMission,
                        onLaunchBoss = { onLaunchMission(bossMission) }
                    )
                }
            }

            // 3. Category Filter Row
            item {
                val filters = listOf(
                    "ALL" to stringResource(R.string.missions_filter_all),
                    "STORY_BOSS" to stringResource(R.string.missions_filter_story),
                    "PATROL_RESCUE" to stringResource(R.string.missions_filter_patrol),
                    "TRIALS" to stringResource(R.string.missions_filter_trial)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filters) { (key, label) ->
                        val isSelected = selectedFilter == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectFilter(key) },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SpiderCrimson,
                                selectedLabelColor = TextPureWhite,
                                containerColor = SpiderSurfaceCard,
                                labelColor = TextSecondaryIce
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("mission_filter_$key")
                        )
                    }
                }
            }

            // 4. Mission Cards
            items(filteredMissions, key = { it.id }) { mission ->
                MissionOperationCard(
                    mission = mission,
                    onLaunch = { onLaunchMission(mission) },
                    onWaypoint = { onSetWaypoint(mission) }
                )
            }
        }
    }
}

@Composable
private fun CampaignHeroBannerCard(
    completedCount: Int,
    totalCount: Int,
    profile: PlayerProfileEntity
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SpiderSurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, WebCyan.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(190.dp)) {
            Image(
                painter = painterResource(id = R.drawable.img_city_hero_banner_1791736611807),
                contentDescription = stringResource(R.string.missions_header_title),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                SpiderMidnight.copy(alpha = 0.25f),
                                SpiderMidnight.copy(alpha = 0.92f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = stringResource(R.string.missions_header_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = TextPureWhite
                )
                Text(
                    text = stringResource(R.string.missions_header_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = WebCyan
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CITY LIBERATION: $completedCount/$totalCount OPS",
                        style = MaterialTheme.typography.labelLarge,
                        color = SpiderSenseGold
                    )
                    Text(
                        text = "CRIMES STOPPED: ${profile.totalCrimesStopped} • SWUNG: ${profile.totalDistanceSwungMeters}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryIce
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { completedCount.toFloat() / totalCount.toFloat() },
                    color = CyberGreen,
                    trackColor = SpiderSurfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

@Composable
private fun BossThreatSpotlightCard(
    bossMission: MissionEntity,
    onLaunchBoss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SpiderSurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, CyberGreen.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
            .testTag("boss_spotlight_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_villain_threat_1791736631464),
                contentDescription = bossMission.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, CyberGreen, RoundedCornerShape(14.dp))
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = CyberGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "OMEGA BOSS SHOWDOWN",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberGreen
                    )
                }
                Text(
                    text = bossMission.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPureWhite
                )
                Text(
                    text = bossMission.objectiveSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryIce
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onLaunchBoss,
                    colors = ButtonDefaults.buttonColors(containerColor = SpiderCrimson),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("launch_boss_spotlight_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "CONFRONT DR. CYBER-STORM",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun MissionOperationCard(
    mission: MissionEntity,
    onLaunch: () -> Unit,
    onWaypoint: () -> Unit
) {
    val category = runCatching { MissionCategory.valueOf(mission.category) }
        .getOrDefault(MissionCategory.CRIME_PATROL)

    val badgeColor = when (category) {
        MissionCategory.BOSS -> CyberGreen
        MissionCategory.STORY -> SpiderCrimson
        MissionCategory.TIME_TRIAL -> WebCyan
        MissionCategory.STEALTH_RESCUE -> MysticPurple
        MissionCategory.CRIME_PATROL -> SpiderSenseGold
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SpiderSurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (mission.isCompleted) CyberGreen.copy(alpha = 0.55f) else badgeColor.copy(alpha = 0.45f),
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("mission_card_${mission.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = badgeColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.border(1.dp, badgeColor, RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = category.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = mission.district.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = WebCyan
                    )
                }

                if (mission.isCompleted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = stringResource(R.string.mission_completed_badge),
                            tint = CyberGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "★".repeat(mission.starsEarned.coerceIn(1, 3)),
                            style = MaterialTheme.typography.labelLarge,
                            color = SpiderSenseGold
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(mission.difficulty) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = SpiderSenseGold,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = mission.title,
                style = MaterialTheme.typography.headlineSmall,
                color = TextPureWhite
            )

            Text(
                text = mission.description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryIce
            )

            Surface(
                color = SpiderSurfaceElevated,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = mission.objectiveSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPureWhite,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+${mission.xpReward} XP • ⚡${mission.techTokensReward}",
                        style = MaterialTheme.typography.labelMedium,
                        color = SpiderSenseGold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onWaypoint,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("waypoint_btn_${mission.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = WebCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.mission_locate_on_map),
                        style = MaterialTheme.typography.labelMedium,
                        color = WebCyan
                    )
                }

                Button(
                    onClick = onLaunch,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (mission.isCompleted) SpiderSurfaceElevated else SpiderCrimson
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("launch_mission_btn_${mission.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (mission.isCompleted) {
                            stringResource(R.string.mission_replay_btn)
                        } else {
                            stringResource(R.string.mission_launch_btn)
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
