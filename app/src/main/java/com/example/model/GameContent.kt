package com.example.model

object GameContent {
    val vehicles = listOf(
        VehicleSpec(
            id = "supercar_apex",
            name = "Apex V12 (Lambo Supercar)",
            category = "Italian Supercar (V12 AWD)",
            description = "Razor-sharp wedge aerodynamics, scissor doors, screaming 770 HP naturally aspirated V12. Built for alpine carving.",
            topSpeedKmh = 355f,
            acceleration0To100 = 2.8f,
            handling = 0.96f,
            braking = 0.94f,
            unlockKm = 0f, // Unlocked initially
            defaultColor = 0, // Giallo Yellow
            engineSoundPitchMultiplier = 1.35f
        ),
        VehicleSpec(
            id = "coupe_bavaria",
            name = "Bavaria M-Sport (Coupe)",
            category = "Luxury Drift Coupe",
            description = "Twin-turbo straight-six rear-wheel drive. Balanced 50:50 weight distribution for tight mountain switchbacks.",
            topSpeedKmh = 295f,
            acceleration0To100 = 3.8f,
            handling = 0.88f,
            braking = 0.85f,
            unlockKm = 0f,
            defaultColor = 5, // Riviera Blue
            engineSoundPitchMultiplier = 1.05f
        ),
        VehicleSpec(
            id = "sedan_apex",
            name = "Apex Cruiser (Sedan)",
            category = "Executive Sedan",
            description = "Smooth aerodynamic highway cruiser. Exceptional endurance, plush ride, and responsive handling.",
            topSpeedKmh = 240f,
            acceleration0To100 = 5.2f,
            handling = 0.78f,
            braking = 0.80f,
            unlockKm = 0f,
            defaultColor = 1, // Alpine White
            engineSoundPitchMultiplier = 0.95f
        ),
        VehicleSpec(
            id = "suv_summit",
            name = "Summit 4x4 (SUV)",
            category = "Mountain Offroad SUV",
            description = "High ground clearance, permanent AWD, and commanding view over mountain valleys and rivers.",
            topSpeedKmh = 230f,
            acceleration0To100 = 5.6f,
            handling = 0.72f,
            braking = 0.76f,
            unlockKm = 0f,
            defaultColor = 4, // Forest Green
            engineSoundPitchMultiplier = 0.9f
        ),
        VehicleSpec(
            id = "sports_phantom",
            name = "Phantom GT (Sports Car)",
            category = "Track-Honed GT",
            description = "Lightweight track-honed chassis, quick steering ratio, and intoxicating high-RPM exhaust note.",
            topSpeedKmh = 315f,
            acceleration0To100 = 3.3f,
            handling = 0.92f,
            braking = 0.89f,
            unlockKm = 0f,
            defaultColor = 3, // Sunset Orange
            engineSoundPitchMultiplier = 1.2f
        ),
        VehicleSpec(
            id = "sedan_monarch",
            name = "Monarch V8 (Grand Tourer)",
            category = "Luxury Grand Tourer",
            description = "Quiet grand touring refinement. Long wheelbase gliding over mountain passes with effortless V8 torque.",
            topSpeedKmh = 270f,
            acceleration0To100 = 4.3f,
            handling = 0.82f,
            braking = 0.82f,
            unlockKm = 0f,
            defaultColor = 2, // Midnight Black
            engineSoundPitchMultiplier = 1.0f
        )
    )

    val paintColors = listOf(
        PaintColor("Giallo Orion (Lambo Yellow)", 0xFFFFD600, floatArrayOf(1.0f, 0.84f, 0.0f)),
        PaintColor("Rosso Corsa (Sport Red)", 0xFFD32F2F, floatArrayOf(0.85f, 0.12f, 0.12f)),
        PaintColor("Verde Mantis (Lambo Green)", 0xFF00E676, floatArrayOf(0.0f, 0.90f, 0.35f)),
        PaintColor("Arancio Atlas (Lambo Orange)", 0xFFFF6D00, floatArrayOf(1.0f, 0.42f, 0.0f)),
        PaintColor("Nero Nemesis (Matte Black)", 0xFF121214, floatArrayOf(0.08f, 0.08f, 0.09f)),
        PaintColor("Bianco Monocerus (Alpine White)", 0xFFF5F7FA, floatArrayOf(0.96f, 0.97f, 0.98f)),
        PaintColor("Blu Cepheus (Electric Blue)", 0xFF00B0FF, floatArrayOf(0.0f, 0.69f, 1.0f)),
        PaintColor("Matte Titanium (Gunmetal)", 0xFF616161, floatArrayOf(0.38f, 0.38f, 0.38f))
    )

    val liveries = listOf(
        LiveryOption("Pure Paint", "Clean, factory finish"),
        LiveryOption("Dual Racing Stripes", "Twin bold racing lines over hood, roof and deck"),
        LiveryOption("Mountain Trail Decal", "Topographic elevation lines along the side sills"),
        LiveryOption("Speed Carbon", "Carbon weave bonnet, side aero fins and wing"),
        LiveryOption("Cyber Camo", "Subtle geometric low-poly mountain pattern")
    )

    val wheelDesigns = listOf(
        WheelOption("Star Sport 5-Spoke", "Classic lightweight five-spoke alloy"),
        WheelOption("Multi-Spoke Mesh", "Race-inspired forged mesh pattern"),
        WheelOption("Deep-Dish Concave", "Aggressive staggered stance with exposed lip"),
        WheelOption("Aero Blade", "Directional aerodynamic aero disc styling")
    )

    val interiorTrims = listOf(
        InteriorOption("Ebony Black Leather", floatArrayOf(0.12f, 0.12f, 0.14f)),
        InteriorOption("Cognac Tan Leather", floatArrayOf(0.60f, 0.38f, 0.20f)),
        InteriorOption("Crimson Sport Accents", floatArrayOf(0.70f, 0.15f, 0.18f)),
        InteriorOption("Alpine Cream Leather", floatArrayOf(0.85f, 0.82f, 0.76f))
    )

    val characters = listOf(
        CharacterSpec(
            id = "male_alex",
            name = "Alex",
            gender = "Male",
            description = "Alpine enthusiast, avid mountain driver and outdoor explorer.",
            outfitColor = 0xFF1976D2
        ),
        CharacterSpec(
            id = "female_elena",
            name = "Elena",
            gender = "Female",
            description = "Road-trip photographer and mountain pass rally enthusiast.",
            outfitColor = 0xFFE91E63
        )
    )

    val defaultRadioStations = listOf(
        RadioStation("Alpine Chill FM", "Relaxing acoustic & ambient mountain melodies"),
        RadioStation("Mountain Synthwave", "80s retro sunset driving rhythms"),
        RadioStation("High Pass Acoustic", "Organic fingerstyle guitar & tranquil nature chords"),
        RadioStation("Valley Lo-Fi Beats", "Calm, mellow beats for scenic cruising")
    )
}

data class PaintColor(val name: String, val argbColor: Long, val rgbFloats: FloatArray)
data class LiveryOption(val name: String, val description: String)
data class WheelOption(val name: String, val description: String)
data class InteriorOption(val name: String, val rgbFloats: FloatArray)
data class RadioStation(val title: String, val subtitle: String)
