package com.balandman.liftlog.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
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
 * combinations.
 *
 * Every icon key has a hand-drawn line-icon fallback ([MachineIcons.resFor]); a
 * growing number also have a full-color illustration ([MachineIcons.artFor]).
 * [illustrated] is the per-machine choice between them — off (or a key with no
 * illustration yet) always falls back to the line icon, so the original icon set
 * never stops being a real option.
 */
@Composable
fun MachineArt(
    iconKey: String,
    size: Dp,
    modifier: Modifier = Modifier,
    illustrated: Boolean = true,
) {
    val artRes = if (illustrated) MachineIcons.artFor(iconKey) else null

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * CORNER_RATIO))
            .background(MachineArtChip),
        contentAlignment = Alignment.Center,
    ) {
        if (artRes != null) {
            // Full-color illustration: already drawn on this exact backdrop, so
            // it fills the chip edge to edge with no tint of its own.
            Image(
                painter = painterResource(artRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                painter = painterResource(MachineIcons.resFor(iconKey)),
                contentDescription = null,
                tint = MachineArtInk,
                modifier = Modifier.size(size * GLYPH_RATIO),
            )
        }
    }
}

private const val CORNER_RATIO = 0.28f

/** Leaves the margin the line icons are drawn with. */
private const val GLYPH_RATIO = 0.72f
