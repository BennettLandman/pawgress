@file:OptIn(ExperimentalMaterial3Api::class)

package com.balandman.liftlog.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.balandman.liftlog.R
import com.balandman.liftlog.data.Coach
import com.balandman.liftlog.data.CoachCatalog
import com.balandman.liftlog.data.CoachTheme
import com.balandman.liftlog.data.GymDay
import com.balandman.liftlog.data.outfitKey
import java.time.LocalDate

/**
 * Pick-a-coach and manage-outfits screen. Every coach in [CoachCatalog.ALL]
 * gets a row regardless of art status — a coach with no mascot image yet
 * still shows up with its name and personality, just without a portrait, and
 * every outfit works the same way. Nothing here ever assumes a specific
 * drawable exists.
 */
@Composable
fun CoachScreen(
    pawprintsBalance: Int,
    unlockedCoachIds: Set<Int>,
    selectedCoachId: Int,
    unlockedOutfits: Set<String>,
    equippedOutfits: Map<Int, String>,
    onBack: () -> Unit,
    onSelectCoach: (Int) -> Unit,
    onUnlockCoach: (Int, Int) -> Unit,
    onUnlockOutfit: (Int, CoachTheme, Int) -> Unit,
    onEquipOutfit: (Int, CoachTheme?) -> Unit,
) {
    val today = remember { GymDay.today() }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Coaches") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 16.dp),
                ) {
                    Text("🐾", modifier = Modifier.padding(end = 4.dp))
                    Text("$pawprintsBalance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column {
                    Image(
                        painter = painterResource(R.drawable.all_coaches),
                        contentDescription = "All 20 coaches",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Earn one pawprint per machine, per gym day, and spend them here " +
                            "on new coaches and seasonal outfits.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }

            items(CoachCatalog.ALL, key = { it.id }) { coach ->
                CoachCard(
                    coach = coach,
                    unlocked = coach.id in unlockedCoachIds,
                    selected = coach.id == selectedCoachId,
                    pawprintsBalance = pawprintsBalance,
                    unlockedOutfits = unlockedOutfits,
                    equippedThemeSlug = equippedOutfits[coach.id],
                    today = today,
                    onSelect = { onSelectCoach(coach.id) },
                    onUnlockCoach = { onUnlockCoach(coach.id, coach.unlockCost) },
                    onUnlockOutfit = { theme -> onUnlockOutfit(coach.id, theme, CoachCatalog.OUTFIT_COST) },
                    onEquipOutfit = { theme -> onEquipOutfit(coach.id, theme) },
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun CoachCard(
    coach: Coach,
    unlocked: Boolean,
    selected: Boolean,
    pawprintsBalance: Int,
    unlockedOutfits: Set<String>,
    equippedThemeSlug: String?,
    today: LocalDate,
    onSelect: () -> Unit,
    onUnlockCoach: () -> Unit,
    onUnlockOutfit: (CoachTheme) -> Unit,
    onEquipOutfit: (CoachTheme?) -> Unit,
) {
    var outfitsExpanded by remember { mutableStateOf(false) }
    val portrait = remember(coach.id, equippedThemeSlug, today) {
        CoachArt.current(coach.id, equippedThemeSlug, today)
    }

    // Only themes the player already owns or that are currently in season are
    // shown anywhere in the outfits UI — a theme that's neither isn't
    // purchasable right now and would just be clutter.
    val availableThemes = remember(coach.id, unlockedOutfits, today) {
        CoachTheme.entries.filter { theme ->
            outfitKey(coach.id, theme) in unlockedOutfits || theme.isActiveOn(today)
        }
    }

    // null = base look. Defaults to whatever's currently equipped (if that
    // outfit is still available to preview), otherwise the base look.
    var previewedTheme by remember(coach.id) {
        mutableStateOf(CoachTheme.fromSlug(equippedThemeSlug)?.takeIf { it in availableThemes })
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    if (portrait != null) {
                        Image(painter = painterResource(portrait), contentDescription = coach.name, modifier = Modifier.size(56.dp))
                    } else {
                        Text("🐾")
                    }
                }
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(coach.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    Text(coach.breed, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!unlocked) {
                    Icon(imageVector = Icons.Filled.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(coach.personality, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))

            when {
                !unlocked -> {
                    OutlinedButton(onClick = onUnlockCoach, enabled = pawprintsBalance >= coach.unlockCost, modifier = Modifier.fillMaxWidth()) {
                        Text("Unlock for ${coach.unlockCost} 🐾")
                    }
                }
                selected -> {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                        Text("✓ Selected", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
                else -> {
                    OutlinedButton(onClick = onSelect, modifier = Modifier.fillMaxWidth()) { Text("Select ${coach.name}") }
                }
            }

            if (unlocked) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { outfitsExpanded = !outfitsExpanded }) {
                    Text(if (outfitsExpanded) "Hide outfits" else "Outfits")
                }
                if (outfitsExpanded) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(12.dp))

                    OutfitPreview(
                        coach = coach,
                        theme = previewedTheme,
                        owned = previewedTheme?.let { outfitKey(coach.id, it) in unlockedOutfits } ?: true,
                        equipped = previewedTheme?.slug == equippedThemeSlug,
                        inSeason = previewedTheme?.isActiveOn(today) ?: true,
                        pawprintsBalance = pawprintsBalance,
                        onUnlock = { previewedTheme?.let(onUnlockOutfit) },
                        onToggleEquip = {
                            onEquipOutfit(if (previewedTheme?.slug == equippedThemeSlug) null else previewedTheme)
                        },
                    )

                    Spacer(Modifier.height(12.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            OutfitThumbnail(
                                art = CoachArt.base(coach.id),
                                label = "Base",
                                selected = previewedTheme == null,
                                onClick = { previewedTheme = null },
                            )
                        }
                        items(availableThemes, key = { it.slug }) { theme ->
                            OutfitThumbnail(
                                art = CoachOutfitArt.resFor(coach.id, theme.slug),
                                label = theme.displayName,
                                selected = previewedTheme == theme,
                                onClick = { previewedTheme = theme },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The large portrait + catchphrase for whichever look is currently being previewed. */
@Composable
private fun OutfitPreview(
    coach: Coach,
    theme: CoachTheme?,
    owned: Boolean,
    equipped: Boolean,
    inSeason: Boolean,
    pawprintsBalance: Int,
    onUnlock: () -> Unit,
    onToggleEquip: () -> Unit,
) {
    val art = if (theme != null) CoachOutfitArt.resFor(coach.id, theme.slug) else CoachArt.base(coach.id)
    // The base look gets its own fixed catchphrase too, distinct from
    // coach.personality (that's the third-person descriptor already shown
    // higher up on the card) — this is what the coach actually "says".
    val quote = if (theme != null) {
        CoachOutfitQuotes.quoteFor(coach.id, theme) ?: coach.personality
    } else {
        CoachOutfitQuotes.baseQuoteFor(coach.id) ?: coach.personality
    }

    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(140.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            if (art != null) {
                Image(painter = painterResource(art), contentDescription = coach.name, modifier = Modifier.size(140.dp))
            } else {
                Text("🐾", style = MaterialTheme.typography.headlineLarge)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = theme?.displayName ?: "Base look",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(quote, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(10.dp))

            if (theme != null) {
                when {
                    owned && inSeason -> {
                        FilterChip(selected = equipped, onClick = onToggleEquip, label = { Text(if (equipped) "Equipped" else "Equip") })
                    }
                    owned -> {
                        Text(
                            "Owned — out of season right now",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    inSeason -> {
                        OutlinedButton(onClick = onUnlock, enabled = pawprintsBalance >= CoachCatalog.OUTFIT_COST) {
                            Text("Unlock for ${CoachCatalog.OUTFIT_COST} 🐾")
                        }
                    }
                }
            } else if (equipped) {
                Text("✓ Equipped", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            } else {
                FilterChip(selected = false, onClick = onToggleEquip, label = { Text("Equip") })
            }
        }
    }
}

@Composable
private fun OutfitThumbnail(
    art: Int?,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(12.dp),
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (art != null) {
                Image(painter = painterResource(art), contentDescription = label, modifier = Modifier.size(56.dp))
            } else {
                Text("🐾")
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
