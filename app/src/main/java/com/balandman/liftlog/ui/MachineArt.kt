package com.balandman.liftlog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.balandman.liftlog.ui.theme.MachineArtChip
import com.balandman.liftlog.ui.theme.MachineArtInk

/**
 * A machine's artwork on its own fixed cream backdrop.
 *
 * The backdrop is deliberately *not* a theme color. Tiles change color when an
 * exercise is done, and the whole grid inverts in dark mode — pinning the artwork
 * to one constant background means a single drawing reads identically in all four
 * combinations, and full-color illustrations can be dropped in later without
 * needing a light and a dark variant of each.
 */
@Composable
fun MachineArt(
    iconKey: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * CORNER_RATIO))
            .background(MachineArtChip),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(MachineIcons.resFor(iconKey)),
            contentDescription = null,
            tint = MachineArtInk,
            modifier = Modifier.size(size * GLYPH_RATIO),
        )
    }
}

private const val CORNER_RATIO = 0.28f

/** Leaves the margin the illustrations are specced to include. */
private const val GLYPH_RATIO = 0.72f
