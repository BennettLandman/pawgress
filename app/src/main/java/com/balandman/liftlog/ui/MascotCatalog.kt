package com.balandman.liftlog.ui

import androidx.annotation.DrawableRes
import com.balandman.liftlog.R

/**
 * The motivational cat images live in res/drawable (or res/drawable-nodpi),
 * named mascot_01, mascot_02, and so on, each a transparent-background PNG.
 *
 * Found here by reflection over the generated [R.drawable] fields rather than
 * a hardcoded list — so dropping new mascot_NN files into the project is all
 * it takes to add them; no code change, and no compile error while zero of
 * them exist yet.
 */
object MascotCatalog {

    private val ids: List<Int> by lazy {
        runCatching {
            R.drawable::class.java.fields
                .filter { it.name.startsWith("mascot_") }
                .mapNotNull { field -> runCatching { field.getInt(null) }.getOrNull() }
        }.getOrDefault(emptyList())
    }

    /** mascot_01 -> coach id 1, and so on — a coach's base look before any outfit. */
    private val byNumber: Map<Int, Int> by lazy {
        runCatching {
            R.drawable::class.java.fields
                .mapNotNull { field ->
                    val number = Regex("""^mascot_(\d+)$""").find(field.name)?.groupValues?.get(1)?.toIntOrNull()
                    if (number == null) return@mapNotNull null
                    val resId = runCatching { field.getInt(null) }.getOrNull() ?: return@mapNotNull null
                    number to resId
                }.toMap()
        }.getOrDefault(emptyMap())
    }

    val hasMascots: Boolean get() = ids.isNotEmpty()

    /** A random mascot drawable, or null until the first mascot_NN image exists. */
    @DrawableRes
    fun random(): Int? = ids.randomOrNull()

    /** The specific coach's base portrait (mascot_NN), or null if not supplied. */
    @DrawableRes
    fun forNumber(number: Int): Int? = byNumber[number]
}
