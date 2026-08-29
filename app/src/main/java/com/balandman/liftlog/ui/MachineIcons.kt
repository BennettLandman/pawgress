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
}
