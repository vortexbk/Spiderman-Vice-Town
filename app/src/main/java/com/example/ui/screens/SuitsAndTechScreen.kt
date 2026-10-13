package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.PlayerProfileEntity
import com.example.domain.model.SpiderSuit
import com.example.domain.model.SpiderWorldCatalog
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.SpiderCrimson
import com.example.ui.theme.SpiderMidnight
import com.example.ui.theme.SpiderSenseGold
import com.example.ui.theme.SpiderSurfaceCard
import com.example.ui.theme.SpiderSurfaceElevated
import com.example.ui.theme.TextPureWhite
import com.example.ui.theme.TextSecondaryIce
import com.example.ui.theme.WebCyan

private data class TechUpgradeItem(
    val key: String,
    val title: String,
    val subtitle: String,
    val currentLevel: Int,
    val baseCost: Int
)

@Composable
fun SuitsAndTechScreen(
    profile: PlayerProfileEntity,
    onUnlockOrEquipSuit: (SpiderSuit) -> Unit,
    onUpgradeTech: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val unlockedSet = remember(profile.unlockedSuitsCsv) {
        profile.unlockedSuitsSet()
    }

    val upgrades = listOf(
        TechUpgradeItem(
            key = "swing_speed",
            title = "Micro-Gyro Web Pendulum",
            subtitle = "Boosts web-swing acceleration and Web-Zip launch velocity (+12% per level).",
            currentLevel = profile.webSwingSpeedLevel,
            baseCost = 30
        ),
        TechUpgradeItem(
            key = "combat_damage",
            title = "Bio-Kinetic Strike Gloves",
            subtitle = "Amplifies Web-Strike melee combo damage and impact web force (+18% per level).",
            currentLevel = profile.combatDamageLevel,
            baseCost = 35
        ),
        TechUpgradeItem(
            key = "web_capacity",
            title = "Pressurized Web Cartridges",
            subtitle = "Increases high-tensile web fluid capacity and rapid mid-air synthesis.",
            currentLevel = profile.webShooterCapacityLevel,
            baseCost = 30
        ),
        TechUpgradeItem(
            key = "spider_sense",
            title = "Neuro-Optic Spider-Sense",
            subtitle = "Expands enemy attack detection range and counter-dodge window (+15% per level).",
            currentLevel = profile.spiderSenseRangeLevel,
            baseCost = 40
        )
    )

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
                .testTag("suits_tech_lazy_column"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Armory Header & Tech Tokens Wallet
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SpiderSurfaceCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, SpiderSenseGold.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.suits_header_title),
                                    style = MaterialTheme.typography.displaySmall,
                                    color = TextPureWhite
                                )
                                Text(
                                    text = stringResource(R.string.suits_header_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondaryIce
                                )
                            }
                            Surface(
                                color = SpiderSurfaceElevated,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.border(1.dp, SpiderSenseGold, RoundedCornerShape(14.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = SpiderSenseGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${profile.techTokens} TOKENS",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SpiderSenseGold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val xpProgress = (profile.xp % 400) / 400f
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "SPIDER-HERO LEVEL ${profile.level}",
                                style = MaterialTheme.typography.labelLarge,
                                color = WebCyan
                            )
                            Text(
                                text = "${profile.xp} TOTAL XP",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondaryIce
                            )
                        }
                        LinearProgressIndicator(
                            progress = { xpProgress },
                            color = WebCyan,
                            trackColor = SpiderSurfaceElevated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }

            // 2. Spider-Suits Collection
            items(SpiderWorldCatalog.suits, key = { it.id }) { suit ->
                val isUnlocked = suit.id in unlockedSet
                val isEquipped = profile.equippedSuitId == suit.id
                SpiderSuitCard(
                    suit = suit,
                    isUnlocked = isUnlocked,
                    isEquipped = isEquipped,
                    canAfford = profile.techTokens >= suit.tokenCost,
                    onAction = { onUnlockOrEquipSuit(suit) }
                )
            }

            // 3. Web-Shooter & Suit Tech Upgrades Header
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = WebCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.upgrades_section_title).uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = WebCyan
                    )
                }
            }

            items(upgrades, key = { it.key }) { item ->
                val cost = item.baseCost * item.currentLevel
                TechUpgradeCard(
                    item = item,
                    cost = cost,
                    canAfford = profile.techTokens >= cost,
                    onUpgrade = { onUpgradeTech(item.key, cost) }
                )
            }
        }
    }
}

@Composable
private fun SpiderSuitCard(
    suit: SpiderSuit,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    canAfford: Boolean,
    onAction: () -> Unit
) {
    val primaryColor = Color(suit.primaryColorHex)
    val secondaryColor = Color(suit.secondaryColorHex)
    val eyeGlow = Color(suit.eyeGlowHex)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SpiderSurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isEquipped) 2.dp else 1.dp,
                color = if (isEquipped) CyberGreen else primaryColor.copy(alpha = 0.65f),
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("suit_card_${suit.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Custom Spider-Mask Avatar Badge
                Surface(
                    color = SpiderMidnight,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .size(68.dp)
                        .border(1.5.dp, primaryColor, RoundedCornerShape(16.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width * 0.5f
                        val cy = size.height * 0.52f
                        drawCircle(color = secondaryColor.copy(alpha = 0.35f), radius = 26f, center = Offset(cx, cy))
                        drawCircle(color = primaryColor, radius = 20f, center = Offset(cx, cy))
                        drawLine(
                            color = eyeGlow,
                            start = Offset(cx - 11f, cy - 4f),
                            end = Offset(cx - 3f, cy + 1f),
                            strokeWidth = 5f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = eyeGlow,
                            start = Offset(cx + 11f, cy - 4f),
                            end = Offset(cx + 3f, cy + 1f),
                            strokeWidth = 5f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = suit.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPureWhite
                        )
                        if (isEquipped) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = CyberGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.suit_equipped_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CyberGreen
                                )
                            }
                        }
                    }
                    Text(
                        text = suit.tagline,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryIce
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "SPEED ${(suit.speedBonus * 100).toInt()}% • DAMAGE ${(suit.damageMultiplier * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = WebCyan
                    )
                }
            }

            // Suit Ultimate Box
            Surface(
                color = SpiderSurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = SpiderSenseGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "ULTIMATE: ${suit.ultimateName.uppercase()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = SpiderSenseGold
                        )
                        Text(
                            text = suit.ultimateDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPureWhite
                        )
                    }
                }
            }

            if (!isEquipped) {
                Button(
                    onClick = onAction,
                    enabled = isUnlocked || canAfford,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isUnlocked) WebCyan else SpiderCrimson
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("suit_action_btn_${suit.id}")
                ) {
                    Icon(
                        imageVector = if (isUnlocked) Icons.Default.CheckCircle else Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = if (isUnlocked) SpiderMidnight else TextPureWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isUnlocked) {
                            stringResource(R.string.suit_equip_btn)
                        } else {
                            "${stringResource(R.string.suit_craft_btn)} (⚡ ${suit.tokenCost} TOKENS)"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isUnlocked) SpiderMidnight else TextPureWhite
                    )
                }
            }
        }
    }
}

@Composable
private fun TechUpgradeCard(
    item: TechUpgradeItem,
    cost: Int,
    canAfford: Boolean,
    onUpgrade: () -> Unit
) {
    val isMaxed = item.currentLevel >= 5
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SpiderSurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WebCyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .testTag("upgrade_card_${item.key}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${item.title} (LVL ${item.currentLevel}/5)",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPureWhite
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryIce
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { item.currentLevel / 5f },
                    color = if (isMaxed) CyberGreen else SpiderSenseGold,
                    trackColor = SpiderSurfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onUpgrade,
                enabled = !isMaxed && canAfford,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMaxed) CyberGreen else SpiderCrimson
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("upgrade_btn_${item.key}")
            ) {
                Text(
                    text = if (isMaxed) {
                        stringResource(R.string.upgrade_max_level)
                    } else {
                        "${stringResource(R.string.upgrade_enhance_btn)} ⚡$cost"
                    },
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
