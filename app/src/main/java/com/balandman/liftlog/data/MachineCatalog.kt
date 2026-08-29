package com.balandman.liftlog.data

/**
 * The machines the app ships with.
 *
 * The first 22 are the circuit Bennett actually uses, named the way the machines
 * themselves are labelled, and they are visible on first launch. The rest are
 * common equipment that starts hidden — switch any of them on from Settings.
 * Hiding never deletes a machine or its history.
 */
object MachineCatalog {

    private data class Seed(
        val id: String,
        val name: String,
        val icon: String,
        val group: MachineGroup,
        val visible: Boolean = true,
    )

    private val SEEDS = listOf(
        // ------------------------------------------------ the circuit: upper body
        Seed("assist_dip_chin", "Assist Dip/Chin", "assist_dip_chin", MachineGroup.UPPER),
        Seed("biceps_curl", "Biceps Curl", "biceps_curl", MachineGroup.UPPER),
        Seed("chest_press", "Chest Press", "chest_press", MachineGroup.UPPER),
        Seed("fixed_pulldown", "Fixed Pulldown", "fixed_pulldown", MachineGroup.UPPER),
        Seed("lat_pulldown", "Lat Pulldown", "lat_pulldown", MachineGroup.UPPER),
        Seed("lateral_raise", "Lateral Raise", "lateral_raise", MachineGroup.UPPER),
        Seed("pec_fly", "Pectoral Fly", "pec_fly", MachineGroup.UPPER),
        Seed(
            "pec_fly_rear_delt", "Pectoral Fly/Rear Deltoid",
            "pec_fly_rear_delt", MachineGroup.UPPER
        ),
        Seed("seated_row", "Seated Row", "seated_row", MachineGroup.UPPER),
        Seed("shoulder_press", "Shoulder Press", "shoulder_press", MachineGroup.UPPER),
        Seed("triceps_extension", "Triceps Extension", "triceps_extension", MachineGroup.UPPER),

        // ------------------------------------------------------ the circuit: core
        Seed("ab_crunch", "Abdominal Crunch", "ab_crunch", MachineGroup.CORE),
        Seed("back_extension", "Back Extension", "back_extension", MachineGroup.CORE),

        // ------------------------------------------------ the circuit: lower body
        Seed("hip_abduction", "Hip Abduction", "hip_abduction", MachineGroup.LOWER),
        Seed("hip_adduction", "Hip Adduction", "hip_adduction", MachineGroup.LOWER),
        Seed("hip_and_glute", "Hip and Glute", "hip_and_glute", MachineGroup.LOWER),
        Seed("horizontal_calf", "Horizontal Calf", "horizontal_calf", MachineGroup.LOWER),
        Seed("leg_curl", "Leg Curl", "leg_curl", MachineGroup.LOWER),
        Seed("leg_extension", "Leg Extension", "leg_extension", MachineGroup.LOWER),
        Seed("seated_leg_curl", "Seated Leg Curl", "seated_leg_curl", MachineGroup.LOWER),
        Seed("seated_leg_press", "Seated Leg Press", "seated_leg_press", MachineGroup.LOWER),
        Seed("standing_calf", "Standing Calf", "standing_calf", MachineGroup.LOWER),

        // ----------------------------------------- available but hidden by default
        Seed("bench_press", "Bench Press", "bench_press", MachineGroup.UPPER, visible = false),
        Seed(
            "incline_press", "Incline Chest Press",
            "incline_press", MachineGroup.UPPER, visible = false
        ),
        Seed(
            "triceps_pushdown", "Triceps Pushdown",
            "triceps_pushdown", MachineGroup.UPPER, visible = false
        ),
        Seed("preacher_curl", "Preacher Curl", "preacher_curl", MachineGroup.UPPER, visible = false),
        Seed("tbar_row", "T-Bar Row", "tbar_row", MachineGroup.UPPER, visible = false),
        Seed("rear_delt", "Rear Deltoid Fly", "rear_delt", MachineGroup.UPPER, visible = false),
        Seed("shrug", "Shrug", "shrug", MachineGroup.UPPER, visible = false),
        Seed("pullup", "Assisted Pull-Up", "pullup", MachineGroup.UPPER, visible = false),
        Seed("dip", "Dip", "dip", MachineGroup.UPPER, visible = false),

        Seed("leg_lift", "Leg Lift", "leg_lift", MachineGroup.CORE, visible = false),
        Seed("torso_rotation", "Torso Rotation", "torso_rotation", MachineGroup.CORE, visible = false),
        Seed("woodchop", "Cable Woodchop", "woodchop", MachineGroup.CORE, visible = false),

        Seed("hack_squat", "Hack Squat", "hack_squat", MachineGroup.LOWER, visible = false),
        Seed("smith_squat", "Smith Machine Squat", "squat", MachineGroup.LOWER, visible = false),

        Seed("farmers_carry", "Farmer's Carry", "farmers_carry", MachineGroup.OTHER, visible = false),
    )

    /** Every icon a custom machine can pick from. */
    val ICON_KEYS: List<String> = listOf(
        "dumbbell", "barbell", "kettlebell", "cable", "plate", "machine", "run",
        "assist_dip_chin", "bench_press", "chest_press", "dip", "fixed_pulldown",
        "incline_press", "lat_pulldown", "lateral_raise", "pec_fly",
        "pec_fly_rear_delt", "preacher_curl", "pullup", "rear_delt", "seated_row",
        "shoulder_press", "shrug", "tbar_row", "triceps_extension",
        "triceps_pushdown", "biceps_curl",
        "standing_calf", "hip_and_glute", "hack_squat", "hip_abduction",
        "hip_adduction", "horizontal_calf", "leg_curl", "leg_extension",
        "seated_leg_press", "seated_leg_curl", "squat",
        "ab_crunch", "back_extension", "leg_lift", "torso_rotation", "woodchop",
        "farmers_carry",
    )

    fun defaults(): List<Machine> = SEEDS.mapIndexed { index, seed ->
        Machine(
            id = seed.id,
            name = seed.name,
            iconKey = seed.icon,
            group = seed.group,
            visible = seed.visible,
            custom = false,
            sortOrder = index,
        )
    }

    /**
     * Machines added to the catalog after a user's install already exists get
     * merged in on launch, so an app update never silently drops new equipment.
     * They arrive hidden, so an update never rearranges the grid either.
     */
    fun mergeNewSeeds(existing: List<Machine>): List<Machine> {
        val known = existing.map { it.id }.toSet()
        val additions = defaults().filter { it.id !in known }
        if (additions.isEmpty()) return existing
        val nextOrder = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1
        return existing + additions.mapIndexed { i, m ->
            m.copy(sortOrder = nextOrder + i, visible = false)
        }
    }
}
