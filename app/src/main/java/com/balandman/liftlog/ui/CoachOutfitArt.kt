package com.balandman.liftlog.ui

import androidx.annotation.DrawableRes
import com.balandman.liftlog.R

/**
 * Seasonal outfit portraits live in res/drawable (or res/drawable-nodpi), one
 * full-replacement image per coach per theme, named
 * `coach_look_<coachId two digits>_<themeSlug>` — e.g. `coach_look_07_halloween`.
 *
 * Found by reflection over the generated [R.drawable] fields, exactly like
 * [MascotCatalog], so any coach+theme combination the user hasn't drawn yet is
 * simply absent — no compile-time reference, no crash, nothing to update in
 * code as art gets added incrementally.
 */
object CoachOutfitArt {

    private val NAME_PATTERN = Regex("""^coach_look_(\d+)_([a-z]+)$""")

    /** "coachId:themeSlug" -> drawable resource id. */
    private val byKey: Map<String, Int> by lazy {
        runCatching {
            R.drawable::class.java.fields
                .mapNotNull { field ->
                    val match = NAME_PATTERN.find(field.name) ?: return@mapNotNull null
                    val coachId = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
                    val slug = match.groupValues[2]
                    val resId = runCatching { field.getInt(null) }.getOrNull() ?: return@mapNotNull null
                    "$coachId:$slug" to resId
                }.toMap()
        }.getOrDefault(emptyMap())
    }

    /** The outfit portrait for this coach+theme, or null if that art hasn't been supplied. */
    @DrawableRes
    fun resFor(coachId: Int, themeSlug: String): Int? = byKey["$coachId:$themeSlug"]

    /** True once at least one outfit portrait exists for this coach, any theme. */
    fun hasAny(coachId: Int): Boolean = byKey.keys.any { it.startsWith("$coachId:") }
}
