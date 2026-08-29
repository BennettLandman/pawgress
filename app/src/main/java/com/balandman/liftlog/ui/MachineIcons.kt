package com.balandman.liftlog.ui

import androidx.annotation.DrawableRes
import com.balandman.liftlog.R

/** Maps a machine's stored icon key to its vector drawable. */
object MachineIcons {

    private val BY_KEY: Map<String, Int> = mapOf(
        "ab_crunch" to R.drawable.ic_m_ab_crunch,
        "assist_dip_chin" to R.drawable.ic_m_assist_dip_chin,
        "back_extension" to R.drawable.ic_m_back_extension,
        "barbell" to R.drawable.ic_m_barbell,
        "bench_press" to R.drawable.ic_m_bench_press,
        "biceps_curl" to R.drawable.ic_m_biceps_curl,
        "cable" to R.drawable.ic_m_cable,
        "standing_calf" to R.drawable.ic_m_standing_calf,
        "chest_press" to R.drawable.ic_m_chest_press,
        "dip" to R.drawable.ic_m_dip,
        "dumbbell" to R.drawable.ic_m_dumbbell,
        "farmers_carry" to R.drawable.ic_m_farmers_carry,
        "fixed_pulldown" to R.drawable.ic_m_fixed_pulldown,
        "hip_and_glute" to R.drawable.ic_m_hip_and_glute,
        "hack_squat" to R.drawable.ic_m_hack_squat,
        "hip_abduction" to R.drawable.ic_m_hip_abduction,
        "hip_adduction" to R.drawable.ic_m_hip_adduction,
        "horizontal_calf" to R.drawable.ic_m_horizontal_calf,
        "incline_press" to R.drawable.ic_m_incline_press,
        "kettlebell" to R.drawable.ic_m_kettlebell,
        "lat_pulldown" to R.drawable.ic_m_lat_pulldown,
        "lateral_raise" to R.drawable.ic_m_lateral_raise,
        "leg_curl" to R.drawable.ic_m_leg_curl,
        "leg_extension" to R.drawable.ic_m_leg_extension,
        "leg_lift" to R.drawable.ic_m_leg_lift,
        "seated_leg_press" to R.drawable.ic_m_seated_leg_press,
        "machine" to R.drawable.ic_m_machine,
        "pec_fly" to R.drawable.ic_m_pec_fly,
        "pec_fly_rear_delt" to R.drawable.ic_m_pec_fly_rear_delt,
        "plate" to R.drawable.ic_m_plate,
        "preacher_curl" to R.drawable.ic_m_preacher_curl,
        "pullup" to R.drawable.ic_m_pullup,
        "rear_delt" to R.drawable.ic_m_rear_delt,
        "run" to R.drawable.ic_m_run,
        "seated_leg_curl" to R.drawable.ic_m_seated_leg_curl,
        "seated_row" to R.drawable.ic_m_seated_row,
        "shoulder_press" to R.drawable.ic_m_shoulder_press,
        "shrug" to R.drawable.ic_m_shrug,
        "squat" to R.drawable.ic_m_squat,
        "tbar_row" to R.drawable.ic_m_tbar_row,
        "torso_rotation" to R.drawable.ic_m_torso_rotation,
        "triceps_extension" to R.drawable.ic_m_triceps_extension,
        "triceps_pushdown" to R.drawable.ic_m_triceps_pushdown,
        "woodchop" to R.drawable.ic_m_woodchop,
    )

    @DrawableRes
    fun resFor(key: String): Int = BY_KEY[key] ?: R.drawable.ic_m_dumbbell

    /**
     * Full-color illustrations, keyed the same way as [BY_KEY]. Not every key
     * has one — a key without an entry here just falls back to its line icon,
     * which is how a brand-new custom icon key behaves until art exists for it.
     */
    private val ART_BY_KEY: Map<String, Int> = mapOf(
        "ab_crunch" to R.drawable.art_ab_crunch,
        "assist_dip_chin" to R.drawable.art_assist_dip_chin,
        "back_extension" to R.drawable.art_back_extension,
        "barbell" to R.drawable.art_barbell,
        "bench_press" to R.drawable.art_bench_press,
        "biceps_curl" to R.drawable.art_biceps_curl,
        "cable" to R.drawable.art_cable,
        "standing_calf" to R.drawable.art_standing_calf,
        "chest_press" to R.drawable.art_chest_press,
        "dip" to R.drawable.art_dip,
        "dumbbell" to R.drawable.art_dumbbell,
        "farmers_carry" to R.drawable.art_farmers_carry,
        "fixed_pulldown" to R.drawable.art_fixed_pulldown,
        "hip_and_glute" to R.drawable.art_hip_and_glute,
        "hack_squat" to R.drawable.art_hack_squat,
        "hip_abduction" to R.drawable.art_hip_abduction,
        "hip_adduction" to R.drawable.art_hip_adduction,
        "horizontal_calf" to R.drawable.art_horizontal_calf,
        "incline_press" to R.drawable.art_incline_press,
        "kettlebell" to R.drawable.art_kettlebell,
        "lat_pulldown" to R.drawable.art_lat_pulldown,
        "lateral_raise" to R.drawable.art_lateral_raise,
        "leg_curl" to R.drawable.art_leg_curl,
        "leg_extension" to R.drawable.art_leg_extension,
        "leg_lift" to R.drawable.art_leg_lift,
        "seated_leg_press" to R.drawable.art_seated_leg_press,
        "machine" to R.drawable.art_machine,
        "pec_fly" to R.drawable.art_pec_fly,
        "pec_fly_rear_delt" to R.drawable.art_pec_fly_rear_delt,
        "plate" to R.drawable.art_plate,
        "preacher_curl" to R.drawable.art_preacher_curl,
        "pullup" to R.drawable.art_pullup,
        "rear_delt" to R.drawable.art_rear_delt,
        "run" to R.drawable.art_run,
        "seated_leg_curl" to R.drawable.art_seated_leg_curl,
        "seated_row" to R.drawable.art_seated_row,
        "shoulder_press" to R.drawable.art_shoulder_press,
        "shrug" to R.drawable.art_shrug,
        "shuttle_run" to R.drawable.art_shuttle_run,
        "squat" to R.drawable.art_squat,
        "tbar_row" to R.drawable.art_tbar_row,
        "torso_rotation" to R.drawable.art_torso_rotation,
        "triceps_extension" to R.drawable.art_triceps_extension,
        "triceps_pushdown" to R.drawable.art_triceps_pushdown,
        "woodchop" to R.drawable.art_woodchop,
    )

    /** The illustrated artwork for a key, or null when only the line icon exists. */
    @DrawableRes
    fun artFor(key: String): Int? = ART_BY_KEY[key]
}
