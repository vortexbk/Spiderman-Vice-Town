package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.local.MissionEntity
import com.example.data.local.PlayerProfileEntity
import com.example.domain.model.CityDistrict
import com.example.domain.model.MissionOutcomeSummary
import com.example.domain.model.RooftopBackpack
import com.example.domain.model.SpiderWorldCatalog
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.SpiderCrimson
import com.example.ui.theme.SpiderDeepNavy
import com.example.ui.theme.SpiderMidnight
import com.example.ui.theme.SpiderSenseGold
import com.example.ui.theme.SpiderSurfaceCard
import com.example.ui.theme.SpiderSurfaceElevated
import com.example.ui.theme.TextPureWhite
import com.example.ui.theme.TextSecondaryIce
import com.example.ui.theme.WebCyan

@Composable
fun CityMapScreen(
    heroWorldX: Float,
    missions: List<MissionEntity>,
    profile: PlayerProfileEntity,
    onFastTravel: (CityDistrict) -> Unit,
    onSetBackpackWaypoint: (RooftopBackpack) -> Unit,
    modifier: Modifier = Modifier
) {
    val collectedSet = remember(profile.collectiblesFoundCsv) {
        profile.collectiblesFoundSet()
    }

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
                .testTag("city_map_lazy_column"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.map_header_title),
                        style = MaterialTheme.typography.displaySmall,
                        color = TextPureWhite
                    )
                    Text(
                        text = stringResource(R.string.map_header_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = WebCyan
                    )
                }
            }

            // 5 Districts Fast-Travel & Liberation Cards
            items(SpiderWorldCatalog.districts, key = { it.id }) { district ->
                val districtMissions = missions.filter { it.district == district.name }
                val completedInDistrict = districtMissions.count { it.isCompleted }
                val districtBackpacks = SpiderWorldCatalog.backpacks.filter { it.districtId == district.id }
                val foundBackpacks = districtBackpacks.count { it.id in collectedSet }
                val isHeroHere = heroWorldX >= district.startX && heroWorldX <= district.endX

                DistrictRadarCard(
                    district = district,
                    isHeroHere = isHeroHere,
                    completedMissions = completedInDistrict,
                    totalMissions = districtMissions.size,
                    foundBackpacks = foundBackpacks,
                    totalBackpacks = districtBackpacks.size,
                    onFastTravel = { onFastTravel(district) }
                )
            }

            // Rooftop Spider-Backpacks Tracker
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Backpack,
                        contentDescription = null,
                        tint = CyberGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${stringResource(R.string.map_backpacks_section)} (${collectedSet.size}/${SpiderWorldCatalog.backpacks.size})",
                        style = MaterialTheme.typography.headlineSmall,
                        color = CyberGreen
                    )
                }
            }

            items(SpiderWorldCatalog.backpacks, key = { it.id }) { bp ->
                val isFound = bp.id in collectedSet
                BackpackCollectibleRow(
                    backpack = bp,
                    isFound = isFound,
                    onTrack = { onSetBackpackWaypoint(bp) }
                )
            }
        }
    }
}

@Composable
private fun DistrictRadarCard(
    district: CityDistrict,
    isHeroHere: Boolean,
    completedMissions: Int,
    totalMissions: Int,
    foundBackpacks: Int,
    totalBackpacks: Int,
    onFastTravel: () -> Unit
) {
    val accent = Color(district.accentColorHex)
    val totalTasks = (totalMissions + totalBackpacks).coerceAtLeast(1)
    val liberationRatio = (completedMissions + foundBackpacks).toFloat() / totalTasks.toFloat()

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SpiderSurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isHeroHere) 2.dp else 1.dp,
                color = if (isHeroHere) WebCyan else accent.copy(alpha = 0.55f),
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("district_card_${district.id}")
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = district.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPureWhite
                    )
                }
                if (isHeroHere) {
                    Surface(
                        color = WebCyan.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.border(1.dp, WebCyan, RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = "CURRENT SECTOR",
                            style = MaterialTheme.typography.labelSmall,
                            color = WebCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Text(
                text = "${district.subtitle} • Landmark: ${district.landmarkName}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryIce
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${stringResource(R.string.map_liberation_label)}: ${(liberationRatio * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = SpiderSenseGold
                )
                Text(
                    text = "Ops: $completedMissions/$totalMissions • Backpacks: $foundBackpacks/$totalBackpacks",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPureWhite
                )
            }

            LinearProgressIndicator(
                progress = { liberationRatio },
                color = accent,
                trackColor = SpiderSurfaceElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Button(
                onClick = onFastTravel,
                colors = ButtonDefaults.buttonColors(containerColor = SpiderSurfaceElevated),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("fast_travel_btn_${district.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.map_fast_travel_btn),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPureWhite
                )
            }
        }
    }
}

@Composable
private fun BackpackCollectibleRow(
    backpack: RooftopBackpack,
    isFound: Boolean,
    onTrack: () -> Unit
) {
    Surface(
        color = SpiderSurfaceCard,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isFound) CyberGreen.copy(alpha = 0.6f) else WebCyan.copy(alpha = 0.25f),
                RoundedCornerShape(14.dp)
            )
            .testTag("backpack_row_${backpack.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isFound) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = CyberGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = backpack.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isFound) CyberGreen else TextPureWhite
                    )
                }
                Text(
                    text = backpack.loreHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryIce
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (!isFound) {
                OutlinedButton(
                    onClick = onTrack,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("track_backpack_btn_${backpack.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = CyberGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "LOCATE",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberGreen
                    )
                }
            } else {
                Text(
                    text = "+${backpack.tokenReward} ⚡",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyberGreen
                )
            }
        }
    }
}

@Composable
fun MissionOutcomeDialog(
    outcome: MissionOutcomeSummary,
    onDismissToOpenWorld: () -> Unit,
    onRetryMission: () -> Unit
) {
    Dialog(onDismissRequest = onDismissToOpenWorld) {
        Surface(
            color = SpiderDeepNavy,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    color = if (outcome.isVictory) SpiderSenseGold else SpiderCrimson,
                    shape = RoundedCornerShape(24.dp)
                )
                .testTag("mission_outcome_dialog")
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (outcome.isVictory) {
                        stringResource(R.string.mission_victory_title)
                    } else {
                        stringResource(R.string.mission_failed_title)
                    },
                    style = MaterialTheme.typography.displaySmall,
                    color = if (outcome.isVictory) SpiderSenseGold else SpiderCrimson,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = outcome.missionTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPureWhite,
                    textAlign = TextAlign.Center
                )

                if (outcome.isVictory) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(3) { idx ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (idx < outcome.starsEarned) SpiderSenseGold else SpiderSurfaceElevated,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Surface(
                        color = SpiderSurfaceCard,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "FINAL SCORE: ${outcome.score}",
                                style = MaterialTheme.typography.labelLarge,
                                color = WebCyan
                            )
                            Text(
                                text = "CLEAR TIME: ${outcome.timeTakenSeconds}s",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPureWhite
                            )
                            Text(
                                text = "REWARDS: +${outcome.xpEarned} XP • +⚡${outcome.techTokensEarned} TECH TOKENS",
                                style = MaterialTheme.typography.labelLarge,
                                color = CyberGreen
                            )
                        }
                    }
                } else {
                    Text(
                        text = outcome.failureReason ?: "Mission failed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryIce,
                        textAlign = TextAlign.Center
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onRetryMission,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("retry_mission_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = WebCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.mission_retry_btn),
                            style = MaterialTheme.typography.labelMedium,
                            color = WebCyan
                        )
                    }

                    Button(
                        onClick = onDismissToOpenWorld,
                        colors = ButtonDefaults.buttonColors(containerColor = SpiderCrimson),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("continue_patrol_dialog_button")
                    ) {
                        Text(
                            text = stringResource(R.string.mission_continue_patrol),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
