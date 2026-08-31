@file:OptIn(ExperimentalMaterial3Api::class)

package com.balandman.liftlog.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                    Text(
                        "$pawprintsBalance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "Earn one pawprint per machine, per gym day, and spend them here " +
                        "on new coaches and seasonal outfits.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
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

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    if (portrait != null) {
                        Image(
                            painter = painterResource(portrait),
                            contentDescription = coach.name,
                            modifier = Modifier.size(56.dp),
                        )
                    } else {
                        Text("🐾")
                    }
                }
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        coach.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        coach.breed,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!unlocked) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Locked",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(coach.personality, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))

            when {
                !unlocked -> {
                    OutlinedButton(
                        onClick = onUnlockCoach,
                        enabled = pawprintsBalance >= coach.unlockCost,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Unlock for ${coach.unlockCost} 🐾")
                    }
                }

                selected -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp),
                    ) {
                        Text(
                            "✓ Selected",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                else -> {
                    OutlinedButton(onClick = onSelect, modifier = Modifier.fillMaxWidth()) {
                        Text("Select ${coach.name}")
                    }
                }
            }

            if (unlocked) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { outfitsExpanded = !outfitsExpanded }) {
                    Text(if (outfitsExpanded) "Hide outfits" else "Outfits")
                }
                if (outfitsExpanded) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(8.dp))
                    CoachTheme.entries.forEach { theme ->
                        OutfitRow(
                            theme = theme,
                            owned = outfitKey(coach.id, theme) in unlockedOutfits,
                            equipped = equippedThemeSlug == theme.slug,
                            inSeason = theme.isActiveOn(today),
                            pawprintsBalance = pawprintsBalance,
                            onUnlock = { onUnlockOutfit(theme) },
                            onToggleEquip = { onEquipOutfit(if (equippedThemeSlug == theme.slug) null else theme) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OutfitRow(
    theme: CoachTheme,
    owned: Boolean,
    equipped: Boolean,
    inSeason: Boolean,
    pawprintsBalance: Int,
    onUnlock: () -> Unit,
    onToggleEquip: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(theme.displayName, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = when {
                    owned && inSeason -> "In season now"
                    owned -> "Owned — back in season ${seasonWindowLabel(theme)}"
                    inSeason -> "In season now"
                    else -> "In season ${seasonWindowLabel(theme)}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        when {
            owned && inSeason -> {
                FilterChip(
                    selected = equipped,
                    onClick = onToggleEquip,
                    label = { Text(if (equipped) "Equipped" else "Equip") },
                )
            }
            owned -> {
                // Owned but out of season: nothing to toggle right now — the
                // equipped preference (if any) is preserved silently and the
                // art will reappear on its own once the window comes back.
            }
            inSeason -> {
                OutlinedButton(
                    onClick = onUnlock,
                    enabled = pawprintsBalance >= CoachCatalog.OUTFIT_COST,
                ) {
                    Text("${CoachCatalog.OUTFIT_COST} 🐾")
                }
            }
            else -> {
                // Not owned and not in season: nothing purchasable right now.
            }
        }
    }
}

private fun seasonWindowLabel(theme: CoachTheme): String {
    // Re-derive a display window from the theme's active check would need the
    // private start/end fields, so instead each theme's slug maps to the same
    // human-readable window shown when the plan was designed. Kept here
    // rather than on the enum so the enum stays focused on the date logic.
    return when (theme) {
        CoachTheme.NEW_YEAR -> "Jan 1–7"
        CoachTheme.VALENTINE -> "Feb 1–14"
        CoachTheme.SPRING -> "Mar 15–Apr 15"
        CoachTheme.SUMMER -> "Jun 1–Aug 31"
        CoachTheme.BACK_TO_SCHOOL -> "Aug 15–Sep 15"
        CoachTheme.HALLOWEEN -> "Oct 1–31"
        CoachTheme.THANKSGIVING -> "Nov 1–30"
        CoachTheme.WINTER_HOLIDAY -> "Dec 1–25"
    }
}
