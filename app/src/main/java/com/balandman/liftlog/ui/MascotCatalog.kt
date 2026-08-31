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

    val hasMascots: Boolean get() = ids.isNotEmpty()

    /** A random mascot drawable, or null until the first mascot_NN image exists. */
    @DrawableRes
    fun random(): Int? = ids.randomOrNull()
}
