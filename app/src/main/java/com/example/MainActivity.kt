package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.SpiderWorldCatalog
import com.example.ui.screens.CityMapScreen
import com.example.ui.screens.MissionOutcomeDialog
import com.example.ui.screens.MissionsHubScreen
import com.example.ui.screens.OpenWorldScreen
import com.example.ui.screens.SuitsAndTechScreen
import com.example.ui.theme.SpiderCityTheme
import com.example.ui.theme.SpiderCrimson
import com.example.ui.theme.SpiderDeepNavy
import com.example.ui.theme.SpiderMidnight
import com.example.ui.theme.SpiderSurfaceElevated
import com.example.ui.theme.TextPureWhite
import com.example.ui.theme.TextSecondaryIce
import com.example.ui.theme.WebCyan
import com.example.ui.viewmodel.GameTab
import com.example.ui.viewmodel.SpiderGameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpiderCityTheme {
                SpiderCityApp()
            }
        }
    }
}

private data class NavDestination(
    val tab: GameTab,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun SpiderCityApp(
    viewModel: SpiderGameViewModel = viewModel(
        factory = SpiderGameViewModel.provideFactory(LocalContext.current)
    )
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val missions by viewModel.missions.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val missionFilter by viewModel.missionFilter.collectAsStateWithLifecycle()
    val missionOutcome by viewModel.missionOutcome.collectAsStateWithLifecycle()
    val worldState by viewModel.worldState.collectAsStateWithLifecycle()

    val equippedSuit = SpiderWorldCatalog.suits.firstOrNull { it.id == profile.equippedSuitId }
        ?: SpiderWorldCatalog.suits.first()

    // Ensure system Back returns to the Open World from any secondary tab
    BackHandler(enabled = selectedTab != GameTab.OPEN_WORLD) {
        viewModel.selectTab(GameTab.OPEN_WORLD)
    }

    val destinations = listOf(
        NavDestination(
            tab = GameTab.OPEN_WORLD,
            labelRes = R.string.nav_open_world,
            selectedIcon = Icons.Filled.Explore,
            unselectedIcon = Icons.Outlined.Explore,
            testTag = "nav_tab_open_world"
        ),
        NavDestination(
            tab = GameTab.MISSIONS,
            labelRes = R.string.nav_missions,
            selectedIcon = Icons.Filled.Assignment,
            unselectedIcon = Icons.Outlined.Assignment,
            testTag = "nav_tab_missions"
        ),
        NavDestination(
            tab = GameTab.SUITS_TECH,
            labelRes = R.string.nav_suits_tech,
            selectedIcon = Icons.Filled.Shield,
            unselectedIcon = Icons.Outlined.Shield,
            testTag = "nav_tab_suits_tech"
        ),
        NavDestination(
            tab = GameTab.CITY_MAP,
            labelRes = R.string.nav_city_map,
            selectedIcon = Icons.Filled.Map,
            unselectedIcon = Icons.Outlined.Map,
            testTag = "nav_tab_city_map"
        )
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 700.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = SpiderMidnight,
            bottomBar = {
                if (!isExpandedScreen) {
                    NavigationBar(
                        containerColor = SpiderDeepNavy,
                        contentColor = TextPureWhite
                    ) {
                        destinations.forEach { dest ->
                            val isSelected = selectedTab == dest.tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { viewModel.selectTab(dest.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = {
                                    Text(text = stringResource(dest.labelRes))
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TextPureWhite,
                                    selectedTextColor = WebCyan,
                                    indicatorColor = SpiderCrimson,
                                    unselectedIconColor = TextSecondaryIce,
                                    unselectedTextColor = TextSecondaryIce
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    NavigationRail(
                        containerColor = SpiderDeepNavy,
                        contentColor = TextPureWhite
                    ) {
                        destinations.forEach { dest ->
                            val isSelected = selectedTab == dest.tab
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = { viewModel.selectTab(dest.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = {
                                    Text(text = stringResource(dest.labelRes))
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = TextPureWhite,
                                    selectedTextColor = WebCyan,
                                    indicatorColor = SpiderCrimson,
                                    unselectedIconColor = TextSecondaryIce,
                                    unselectedTextColor = TextSecondaryIce
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                when (selectedTab) {
                    GameTab.OPEN_WORLD -> {
                        OpenWorldScreen(
                            worldState = worldState,
                            profile = profile,
                            equippedSuit = equippedSuit,
                            missions = missions,
                            onStepPhysics = viewModel::stepPhysics,
                            onMoveDirection = viewModel::setMoveDirection,
                            onStartSwing = viewModel::startWebSwing,
                            onReleaseSwing = viewModel::releaseWebSwing,
                            onWebZip = viewModel::triggerWebZip,
                            onWebStrike = viewModel::triggerWebStrike,
                            onWebShot = viewModel::triggerWebShot,
                            onDodgeOrTrick = viewModel::triggerDodgeOrTrick,
                            onTriggerUltimate = viewModel::triggerSuitUltimate,
                            onLaunchMission = viewModel::launchMission,
                            onAbortMission = viewModel::abortActiveMission,
                            onCycleTimeOfDay = viewModel::cycleTimeOfDay,
                            onTogglePause = viewModel::togglePause,
                            onOpenMissionsTab = { viewModel.selectTab(GameTab.MISSIONS) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    GameTab.MISSIONS -> {
                        MissionsHubScreen(
                            missions = missions,
                            profile = profile,
                            selectedFilter = missionFilter,
                            onSelectFilter = viewModel::setMissionFilter,
                            onLaunchMission = viewModel::launchMission,
                            onSetWaypoint = { mission ->
                                viewModel.setWaypoint(mission.targetWorldX, mission.title)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    GameTab.SUITS_TECH -> {
                        SuitsAndTechScreen(
                            profile = profile,
                            onUnlockOrEquipSuit = viewModel::unlockOrEquipSuit,
                            onUpgradeTech = viewModel::upgradeTech,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    GameTab.CITY_MAP -> {
                        CityMapScreen(
                            heroWorldX = worldState.heroX,
                            missions = missions,
                            profile = profile,
                            onFastTravel = viewModel::fastTravelToDistrict,
                            onSetBackpackWaypoint = { bp ->
                                viewModel.setWaypoint(bp.worldX, bp.name)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            missionOutcome?.let { outcome ->
                MissionOutcomeDialog(
                    outcome = outcome,
                    onDismissToOpenWorld = viewModel::dismissMissionOutcome,
                    onRetryMission = {
                        viewModel.dismissMissionOutcome()
                        viewModel.startMissionById(outcome.missionId)
                    }
                )
            }
        }
    }
}
