package com.example.domain.model

import com.example.data.local.MissionEntity

object SpiderWorldCatalog {

    const val WORLD_WIDTH = 6000f
    const val MAX_SKY_ALTITUDE = 780f

    val districts: List<CityDistrict> = listOf(
        CityDistrict(
            id = "hells_kitchen",
            name = "Hell's Kitchen",
            subtitle = "Industrial Docks, Water Towers & Fisk Cranes",
            startX = 0f,
            endX = 1200f,
            accentColorHex = 0xFFFF5252,
            landmarkName = "Fisk Construction Tower"
        ),
        CityDistrict(
            id = "midtown",
            name = "Midtown Manhattan",
            subtitle = "Times Square Neon Plaza & Daily Bugle Spire",
            startX = 1200f,
            endX = 2400f,
            accentColorHex = 0xFF00E5FF,
            landmarkName = "Daily Bugle & Empire Spire"
        ),
        CityDistrict(
            id = "chinatown",
            name = "Chinatown & Sanctum",
            subtitle = "Lantern Rooftops & Mystic Bleecker Street",
            startX = 2400f,
            endX = 3600f,
            accentColorHex = 0xFFFFC400,
            landmarkName = "Sanctum Mystic Pagoda"
        ),
        CityDistrict(
            id = "financial",
            name = "Financial District",
            subtitle = "Oscorp Bio-Lab Skyscrapers & Wall Street",
            startX = 3600f,
            endX = 4800f,
            accentColorHex = 0xFF00E676,
            landmarkName = "Oscorp Science Megatower"
        ),
        CityDistrict(
            id = "queens_bridge",
            name = "Queens Suspension Bridge",
            subtitle = "East River High-Tension Cables & Reactor Pier",
            startX = 4800f,
            endX = 6000f,
            accentColorHex = 0xFFD500F9,
            landmarkName = "Queens Bridge Main Pylon"
        )
    )

    val buildings: List<Building> = listOf(
        // Hell's Kitchen (0 .. 1200)
        Building(1, "hells_kitchen", 40f, 130f, 250f, RoofType.FLAT, 0xFF1A1E36, 0xFFFFB74D, "DOCKS 42"),
        Building(2, "hells_kitchen", 200f, 145f, 340f, RoofType.CRANE, 0xFF231F38, 0xFFFF5252, "FISK CORP"),
        Building(3, "hells_kitchen", 380f, 135f, 290f, RoofType.FLAT, 0xFF19223B, 0xFFFFCC80),
        Building(4, "hells_kitchen", 545f, 160f, 430f, RoofType.SPIRE, 0xFF251A38, 0xFFFF5252, "KINGPIN"),
        Building(5, "hells_kitchen", 735f, 140f, 310f, RoofType.NEON_BILLBOARD, 0xFF182039, 0xFF80D8FF, "BOXING GYM"),
        Building(6, "hells_kitchen", 905f, 130f, 360f, RoofType.CRANE, 0xFF202542, 0xFFFF8A80),
        Building(7, "hells_kitchen", 1060f, 120f, 280f, RoofType.FLAT, 0xFF171E35, 0xFFFFE082),

        // Midtown Manhattan (1200 .. 2400)
        Building(8, "midtown", 1210f, 150f, 390f, RoofType.NEON_BILLBOARD, 0xFF142447, 0xFF00E5FF, "DAILY BUGLE"),
        Building(9, "midtown", 1390f, 165f, 470f, RoofType.NEON_BILLBOARD, 0xFF1B2B54, 0xFFFF4081, "TIMES SQUARE"),
        Building(10, "midtown", 1585f, 140f, 350f, RoofType.FLAT, 0xFF162242, 0xFF80F2FF, "BROADWAY"),
        Building(11, "midtown", 1755f, 180f, 540f, RoofType.SPIRE, 0xFF1F3263, 0xFF00E5FF, "EMPIRE SPIRE"),
        Building(12, "midtown", 1965f, 145f, 400f, RoofType.NEON_BILLBOARD, 0xFF15254A, 0xFFFFD740, "RADIO CITY"),
        Building(13, "midtown", 2140f, 130f, 330f, RoofType.FLAT, 0xFF1A2950, 0xFF40C4FF),
        Building(14, "midtown", 2290f, 110f, 370f, RoofType.SPIRE, 0xFF142140, 0xFF80D8FF),

        // Chinatown & Sanctum (2400 .. 3600)
        Building(15, "chinatown", 2425f, 145f, 310f, RoofType.PAGODA, 0xFF2B1B38, 0xFFFFD54F, "GOLDEN DRAGON"),
        Building(16, "chinatown", 2600f, 155f, 380f, RoofType.PAGODA, 0xFF331E42, 0xFFFFAB40),
        Building(17, "chinatown", 2785f, 175f, 460f, RoofType.PAGODA, 0xFF28193D, 0xFFE040FB, "SANCTUM"),
        Building(18, "chinatown", 2990f, 140f, 320f, RoofType.NEON_BILLBOARD, 0xFF231A38, 0xFFFFD740, "DIM SUM NEON"),
        Building(19, "chinatown", 3160f, 155f, 410f, RoofType.PAGODA, 0xFF2E1E45, 0xFFFF80AB),
        Building(20, "chinatown", 3345f, 130f, 290f, RoofType.FLAT, 0xFF1F1835, 0xFFFFE57F),
        Building(21, "chinatown", 3495f, 100f, 340f, RoofType.PAGODA, 0xFF271B3B, 0xFFFFC400),

        // Financial District (3600 .. 4800)
        Building(22, "financial", 3620f, 155f, 440f, RoofType.SPIRE, 0xFF102A3A, 0xFF00E676, "WALL ST"),
        Building(23, "financial", 3805f, 140f, 370f, RoofType.FLAT, 0xFF153042, 0xFF69F0AE),
        Building(24, "financial", 3975f, 190f, 550f, RoofType.SPIRE, 0xFF0E2F3D, 0xFF00E676, "OSCORP TOWER"),
        Building(25, "financial", 4195f, 150f, 420f, RoofType.NEON_BILLBOARD, 0xFF13283B, 0xFF00E5FF, "ALCHEMAX"),
        Building(26, "financial", 4375f, 160f, 480f, RoofType.SPIRE, 0xFF183147, 0xFF69F0AE, "EXCHANGE"),
        Building(27, "financial", 4565f, 130f, 350f, RoofType.FLAT, 0xFF122436, 0xFF80CBC4),
        Building(28, "financial", 4710f, 95f, 300f, RoofType.FLAT, 0xFF102030, 0xFF64FFDA),

        // Queens Suspension Bridge (4800 .. 6000)
        Building(29, "queens_bridge", 4830f, 150f, 500f, RoofType.BRIDGE_TOWER, 0xFF261E47, 0xFFE040FB, "WEST PYLON"),
        Building(30, "queens_bridge", 5020f, 150f, 240f, RoofType.FLAT, 0xFF1A1836, 0xFF00E5FF, "BRIDGE DECK A"),
        Building(31, "queens_bridge", 5210f, 160f, 260f, RoofType.CRANE, 0xFF1E1B3D, 0xFF00E676, "REACTOR SPAN"),
        Building(32, "queens_bridge", 5410f, 150f, 240f, RoofType.FLAT, 0xFF1A1836, 0xFF00E5FF, "BRIDGE DECK B"),
        Building(33, "queens_bridge", 5600f, 155f, 510f, RoofType.BRIDGE_TOWER, 0xFF261E47, 0xFFE040FB, "EAST PYLON"),
        Building(34, "queens_bridge", 5795f, 155f, 330f, RoofType.NEON_BILLBOARD, 0xFF192240, 0xFFFFC400, "QUEENS PLAZA")
    )

    val backpacks: List<RooftopBackpack> = listOf(
        RooftopBackpack("bp_1", "First Web-Shooter Prototype", "Webbed to the Fisk Construction crane in Hell's Kitchen.", "hells_kitchen", 272f, 360f),
        RooftopBackpack("bp_2", "Nelson & Murdock Business Card", "Found atop the Kingpin Spire in Hell's Kitchen.", "hells_kitchen", 625f, 450f),
        RooftopBackpack("bp_3", "Daily Bugle Press Pass", "Left on J. Jonah Jameson's rooftop balcony in Midtown.", "midtown", 1285f, 410f),
        RooftopBackpack("bp_4", "Empire State Physics Notebook", "Stashed at the very peak of Empire Spire.", "midtown", 1845f, 560f),
        RooftopBackpack("bp_5", "Bleecker Sanctum Amulet Sketch", "Hidden atop the Sanctum Pagoda in Chinatown.", "chinatown", 2872f, 480f),
        RooftopBackpack("bp_6", "Aunt May's Dumpling Receipt", "Webbed to the Golden Dragon roof in Chinatown.", "chinatown", 2497f, 330f),
        RooftopBackpack("bp_7", "Oscorp Bio-Cable Sample", "Secured at the summit of Oscorp Megatower.", "financial", 4070f, 570f),
        RooftopBackpack("bp_8", "Alchemax Chrono-Cell", "Perched on the Exchange Spire in the Financial District.", "financial", 4455f, 500f),
        RooftopBackpack("bp_9", "Queens Science Fair Ribbon", "Tied to the West Suspension Pylon overlooking the East River.", "queens_bridge", 4905f, 520f),
        RooftopBackpack("bp_10", "Homemade Goggles Lens", "Hidden high on the East Pylon of Queens Bridge.", "queens_bridge", 5677f, 530f)
    )

    val suits: List<SpiderSuit> = listOf(
        SpiderSuit(
            id = "classic_crimson",
            name = "Advanced Crimson Suit",
            tagline = "Balanced white-spider tactical weave with 360° Web Blossom.",
            primaryColorHex = 0xFFE62429,
            secondaryColorHex = 0xFF1E56F0,
            eyeGlowHex = 0xFFFFFFFF,
            webColorHex = 0xFFE8F8FF,
            ultimateName = "Web Blossom Barrage",
            ultimateDescription = "Unleashes a 360° spiral of high-tensile impact webs that instantly binds and damages all nearby enemies.",
            speedBonus = 1.0f,
            damageMultiplier = 1.0f,
            tokenCost = 0
        ),
        SpiderSuit(
            id = "symbiote_obsidian",
            name = "Symbiote Obsidian Suit",
            tagline = "Aggressive bio-organic armor with devastating Tendril Surge.",
            primaryColorHex = 0xFF1C1F2B,
            secondaryColorHex = 0xFF374785,
            eyeGlowHex = 0xFFE0F7FA,
            webColorHex = 0xFFB39DDB,
            ultimateName = "Symbiote Tendril Surge",
            ultimateDescription = "Erupts dark bio-tendrils across the rooftop, dealing massive heavy damage and knocking back bosses.",
            speedBonus = 1.12f,
            damageMultiplier = 1.35f,
            tokenCost = 80
        ),
        SpiderSuit(
            id = "iron_spider_nano",
            name = "Iron-Spider Nano Armor",
            tagline = "Stark nanotech crimson-gold plating with 4 Waldo Mech Arms.",
            primaryColorHex = 0xFFC62828,
            secondaryColorHex = 0xFFFFC400,
            eyeGlowHex = 0xFF00E5FF,
            webColorHex = 0xFFFFE082,
            ultimateName = "Waldo Repulsor Blitz",
            ultimateDescription = "Deploys 4 golden titanium spider-arms that fire synchronized plasma beams at every threat in range.",
            speedBonus = 1.18f,
            damageMultiplier = 1.45f,
            tokenCost = 140
        ),
        SpiderSuit(
            id = "neon_noir_2099",
            name = "Cyber-Spider 2099",
            tagline = "Nueva York futuristic stealth weave with anti-gravity thrusters.",
            primaryColorHex = 0xFF0D254C,
            secondaryColorHex = 0xFFFF1744,
            eyeGlowHex = 0xFFFF5252,
            webColorHex = 0xFF00E5FF,
            ultimateName = "Chrono EMP Shockwave",
            ultimateDescription = "Detonates a high-frequency cybernetic pulse that disables drone shields and stuns all enemies for 6 seconds.",
            speedBonus = 1.28f,
            damageMultiplier = 1.30f,
            tokenCost = 190
        ),
        SpiderSuit(
            id = "velocity_scarlet",
            name = "Velocity Hyper-Suit",
            tagline = "Micro-gyros for extreme pendulum swing speeds and kinetic strikes.",
            primaryColorHex = 0xFFD50000,
            secondaryColorHex = 0xFF00E5FF,
            eyeGlowHex = 0xFF80F2FF,
            webColorHex = 0xFF00E5FF,
            ultimateName = "Hyper-Sonic Chain Strike",
            ultimateDescription = "Blurs across the skyline at Mach-1, chaining instant kinetic takedowns across every active target.",
            speedBonus = 1.40f,
            damageMultiplier = 1.50f,
            tokenCost = 250
        )
    )

    val initialMissions: List<MissionEntity> = listOf(
        MissionEntity(
            id = "m1_times_square_heist",
            title = "Times Square Shock Heist",
            district = "Midtown Manhattan",
            category = MissionCategory.CRIME_PATROL.name,
            description = "The Shock-Gang has ambushed an armored Oscorp transport right under the Times Square billboards. Swing in and web them up!",
            objectiveSummary = "Defeat 5 Armed Heist Thugs & Shock Enforcers in Midtown.",
            difficulty = 1,
            xpReward = 250,
            techTokensReward = 45,
            targetWorldX = 1470f,
            targetWorldY = 470f,
            isCompleted = false,
            bestScore = 0,
            bestTimeSeconds = 0,
            starsEarned = 0
        ),
        MissionEntity(
            id = "m2_skyline_express",
            title = "Skyline Pendulum Trial",
            district = "Midtown Manhattan",
            category = MissionCategory.TIME_TRIAL.name,
            description = "Test your web-swinging momentum! Swing and web-zip through 7 holographic aerial checkpoints from Hell's Kitchen to Chinatown.",
            objectiveSummary = "Pass through 7 Aerial Holo-Rings before time runs out.",
            difficulty = 2,
            xpReward = 300,
            techTokensReward = 55,
            targetWorldX = 1100f,
            targetWorldY = 440f,
            isCompleted = false,
            bestScore = 0,
            bestTimeSeconds = 0,
            starsEarned = 0
        ),
        MissionEntity(
            id = "m3_fisk_tower_raid",
            title = "Fisk Tower Sonic Disarm",
            district = "Hell's Kitchen",
            category = MissionCategory.STORY.name,
            description = "Kingpin's mercenaries planted 4 seismic resonators across Hell's Kitchen rooftops while guarding them with heavy enforcers.",
            objectiveSummary = "Disarm 4 Sonic Bombs and defeat 4 Fisk Enforcers.",
            difficulty = 2,
            xpReward = 380,
            techTokensReward = 65,
            targetWorldX = 625f,
            targetWorldY = 430f,
            isCompleted = false,
            bestScore = 0,
            bestTimeSeconds = 0,
            starsEarned = 0
        ),
        MissionEntity(
            id = "m4_chinatown_rescue",
            title = "Sanctum Rooftop Rescue",
            district = "Chinatown & Sanctum",
            category = MissionCategory.STEALTH_RESCUE.name,
            description = "Runaway hover-drones have trapped 4 civilians in containment pods along the Chinatown pagoda roofs. Zip in and free them!",
            objectiveSummary = "Rescue 4 Civilian Pods & destroy 4 Hunter Drones.",
            difficulty = 3,
            xpReward = 420,
            techTokensReward = 75,
            targetWorldX = 2870f,
            targetWorldY = 460f,
            isCompleted = false,
            bestScore = 0,
            bestTimeSeconds = 0,
            starsEarned = 0
        ),
        MissionEntity(
            id = "m5_oscorp_drone_swarm",
            title = "Oscorp Sky-Hunter Swarm",
            district = "Financial District",
            category = MissionCategory.CRIME_PATROL.name,
            description = "Rogue security algorithms launched an elite squadron of 6 airborne Hunter-Drones around Oscorp Tower. Use Web-Zip and Impact Webs!",
            objectiveSummary = "Destroy 6 Airborne Hunter-Drones over the Financial District.",
            difficulty = 3,
            xpReward = 460,
            techTokensReward = 80,
            targetWorldX = 4070f,
            targetWorldY = 550f,
            isCompleted = false,
            bestScore = 0,
            bestTimeSeconds = 0,
            starsEarned = 0
        ),
        MissionEntity(
            id = "m6_mystic_portal_rift",
            title = "Bleecker Dimensional Rifts",
            district = "Chinatown & Sanctum",
            category = MissionCategory.TIME_TRIAL.name,
            description = "Unstable dimensional rifts are opening above the Sanctum! Swing at high altitude to seal 6 aerial rifts before they destabilize.",
            objectiveSummary = "Seal 6 High-Altitude Mystic Rifts within 75 seconds.",
            difficulty = 4,
            xpReward = 500,
            techTokensReward = 90,
            targetWorldX = 2785f,
            targetWorldY = 520f,
            isCompleted = false,
            bestScore = 0,
            bestTimeSeconds = 0,
            starsEarned = 0
        ),
        MissionEntity(
            id = "m7_midtown_rhino_convoy",
            title = "Armored Rhino-Mech Rampage",
            district = "Midtown Manhattan",
            category = MissionCategory.STORY.name,
            description = "A heavily armored Rhino-Mech exosuit and its escort squad are tearing through Empire Spire plaza! Watch your Spider-Sense and strike back!",
            objectiveSummary = "Defeat the Armored Rhino-Mech and 3 Shock Escorts.",
            difficulty = 4,
            xpReward = 600,
            techTokensReward = 110,
            targetWorldX = 1845f,
            targetWorldY = 540f,
            isCompleted = false,
            bestScore = 0,
            bestTimeSeconds = 0,
            starsEarned = 0
        ),
        MissionEntity(
            id = "m8_boss_cyber_octo",
            title = "Boss: Dr. Cyber-Storm on Queens Bridge",
            district = "Queens Suspension Bridge",
            category = MissionCategory.BOSS.name,
            description = "Dr. Cyber-Storm has seized the Queens Suspension Bridge with his four neon-green plasma tentacles and drone swarm! Web his core and save New York!",
            objectiveSummary = "Defeat Dr. Cyber-Storm and his 3 Reactor Drones on Queens Bridge.",
            difficulty = 5,
            xpReward = 1000,
            techTokensReward = 180,
            targetWorldX = 5290f,
            targetWorldY = 380f,
            isCompleted = false,
            bestScore = 0,
            bestTimeSeconds = 0,
            starsEarned = 0
        )
    )

    fun districtAt(worldX: Float): CityDistrict {
        return districts.firstOrNull { worldX >= it.startX && worldX <= it.endX }
            ?: districts.first()
    }
}
