package com.balandman.liftlog.ui

import androidx.annotation.DrawableRes
import com.balandman.liftlog.data.CoachTheme
import com.balandman.liftlog.data.GymDay
import java.time.LocalDate

/**
 * Resolves what a coach actually looks like right now: the seasonal outfit if
 * one is equipped, currently in season, and its art has been supplied — the
 * plain base portrait otherwise. Every fallback step is graceful, so this
 * never crashes or shows nothing just because an outfit image hasn't been
 * drawn yet.
 */
object CoachArt {

    /** The coach's un-costumed look, or null until its mascot_NN image exists. */
    @DrawableRes
    fun base(coachId: Int): Int? = MascotCatalog.forNumber(coachId)

    /**
     * The coach's current look. [equippedThemeSlug] is whatever the profile has
     * stored for this coach (from [com.balandman.liftlog.data.Profile.equippedOutfits]),
     * independent of season — this is where that preference actually gets
     * gated by the calendar and by whether the art exists.
     */
    @DrawableRes
    fun current(
        coachId: Int,
        equippedThemeSlug: String?,
        today: LocalDate = GymDay.today(),
    ): Int? {
        val theme = CoachTheme.fromSlug(equippedThemeSlug)
        if (theme != null && theme.isActiveOn(today)) {
            CoachOutfitArt.resFor(coachId, theme.slug)?.let { return it }
        }
        return base(coachId)
    }
}
