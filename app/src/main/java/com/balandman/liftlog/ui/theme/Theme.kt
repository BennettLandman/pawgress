package com.balandman.liftlog.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// "Lavender and Sage Training Club": clay as the action color, sage for a fresh
// tile, lavender for a finished one, on soft ivory.
//
// A fixed palette rather than Material You dynamic color: the done state has to
// stay unmistakable, and a wallpaper-derived scheme could wash it out.

private val LightColors = lightColorScheme(
    primary = Color(0xFFB96756),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF1D5D0),
    onPrimaryContainer = Color(0xFF46180F),
    secondary = Color(0xFF978DAE),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE7E1ED),
    onSecondaryContainer = Color(0xFF2E2739),
    tertiary = Color(0xFF6E7F76),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE8EEE9),
    onTertiaryContainer = Color(0xFF222E28),
    background = Color(0xFFFCFAF6),
    onBackground = Color(0xFF2A322E),
    surface = Color(0xFFFCFAF6),
    onSurface = Color(0xFF2A322E),
    surfaceVariant = Color(0xFFE8EEE9),
    onSurfaceVariant = Color(0xFF4C574F),
    outline = Color(0xFF7C8880),
    outlineVariant = Color(0xFFD3DDD6),
    error = Color(0xFFA03A2C),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF7DDD8),
    onErrorContainer = Color(0xFF3F0D06),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFDFA091),
    onPrimary = Color(0xFF46180F),
    primaryContainer = Color(0xFF8B4839),
    onPrimaryContainer = Color(0xFFF1D5D0),
    secondary = Color(0xFFC6BBDD),
    onSecondary = Color(0xFF322A45),
    secondaryContainer = Color(0xFF4A415C),
    onSecondaryContainer = Color(0xFFE7E1ED),
    tertiary = Color(0xFFA7BDB2),
    onTertiary = Color(0xFF14291F),
    tertiaryContainer = Color(0xFF2F413A),
    onTertiaryContainer = Color(0xFFC3D6CB),
    background = Color(0xFF18201D),
    onBackground = Color(0xFFE3E8E4),
    surface = Color(0xFF18201D),
    onSurface = Color(0xFFE3E8E4),
    surfaceVariant = Color(0xFF2C3733),
    onSurfaceVariant = Color(0xFFC2CEC7),
    outline = Color(0xFF8A968E),
    outlineVariant = Color(0xFF3E4A45),
    error = Color(0xFFEFB0A4),
    onError = Color(0xFF5A1A10),
    errorContainer = Color(0xFF7A2E22),
    onErrorContainer = Color(0xFFF7DDD8),
)

/**
 * Tile colors are their own thing rather than borrowed Material roles, because
 * fresh and done are a *hue* shift here — sage to lavender — and neither maps
 * onto "primary container" in a way that would survive someone editing the
 * scheme later.
 */
@Immutable
data class TileColors(
    val fresh: Color,
    val onFresh: Color,
    val freshNumber: Color,
    val done: Color,
    val onDone: Color,
    val doneNumber: Color,
    val doneBorder: Color,
    /** The check badge: the one saturated mark on an otherwise muted grid. */
    val doneBadge: Color,
    val onDoneBadge: Color,
)

private val LightTiles = TileColors(
    fresh = Color(0xFFE8EEE9),
    onFresh = Color(0xFF4C574F),
    freshNumber = Color(0xFF2A322E),
    done = Color(0xFFE7E1ED),
    onDone = Color(0xFF4A4358),
    doneNumber = Color(0xFF2E2739),
    doneBorder = Color(0xFF978DAE),
    doneBadge = Color(0xFFB96756),
    onDoneBadge = Color(0xFFFFFFFF),
)

private val DarkTiles = TileColors(
    fresh = Color(0xFF27332E),
    onFresh = Color(0xFFB6C3BB),
    freshNumber = Color(0xFFE3E8E4),
    done = Color(0xFF2F2A3A),
    onDone = Color(0xFFCDC4DE),
    doneNumber = Color(0xFFEFE9F5),
    doneBorder = Color(0xFFA99EC2),
    doneBadge = Color(0xFFDFA091),
    onDoneBadge = Color(0xFF46180F),
)

val LocalTileColors = staticCompositionLocalOf { LightTiles }

/**
 * Fixed backdrop and ink for machine artwork, identical in both themes — see the
 * note on [com.balandman.liftlog.ui.MachineArt]. Any illustration supplied later
 * should be drawn on exactly this ivory.
 */
val MachineArtChip = Color(0xFFFAF7F0)
val MachineArtInk = Color(0xFF34403B)

@Composable
fun LiftLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalTileColors provides if (darkTheme) DarkTiles else LightTiles) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            content = content,
        )
    }
}
