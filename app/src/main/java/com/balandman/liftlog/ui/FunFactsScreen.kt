@file:OptIn(ExperimentalMaterial3Api::class)

package com.balandman.liftlog.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.balandman.liftlog.data.CoachCatalog
import com.balandman.liftlog.data.GymDay
import com.balandman.liftlog.data.LogEntry
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

private enum class FactsRange(val label: String) {
    TODAY("Today"),
    WEEK("This Week"),
    MONTH("This Month"),
    // "All Time" doesn't fit the chip on one line next to the others — the
    // infinity symbol says the same thing and always fits.
    ALL("∞"),
}

@Composable
fun FunFactsScreen(
    log: List<LogEntry>,
    selectedCoachId: Int,
    equippedOutfits: Map<Int, String>,
    onBack: () -> Unit,
) {
    var range by remember { mutableStateOf(FactsRange.TODAY) }
    val today = GymDay.today()
    val filtered = remember(log, range, today) { filterByRange(log, range, today) }
    val streak = remember(log, today) { currentStreak(log, today) }

    // The selected coach fully replaces the random mascot here — its own
    // portrait (base look, or its current seasonal outfit if one's equipped
    // and in season) and its own voice. Randomized once per visit to this
    // screen, not on every recomposition, so re-entering "Fun Facts" is what
    // earns a fresh line.
    val mascotDrawable = remember(selectedCoachId, equippedOutfits, today) {
        CoachArt.current(selectedCoachId, equippedOutfits[selectedCoachId], today)
    }
    val coachName = remember(selectedCoachId) { CoachCatalog.byId(selectedCoachId)?.name }
    val saying = remember(log, selectedCoachId) {
        CoachVoice.randomSaying(selectedCoachId, MotivationStats.from(log, today))
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Fun Facts") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FactsRange.entries.forEach { option ->
                FilterChip(
                    selected = range == option,
                    onClick = { range = option },
                    label = { Text(option.label) },
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                MascotCard(mascotDrawable = mascotDrawable, coachName = coachName, saying = saying)
            }

            item {
                StreakCard(streak)
            }

            if (filtered.isEmpty()) {
                item {
                    FactCard(
                        headline = "Not a whisker of activity " +
                            (if (range == FactsRange.ALL) "yet" else "here") + ".",
                        detail = "Log a lift and the fun facts will pounce right in.",
                    )
                }
            } else {
                items(buildFacts(filtered, range)) { fact ->
                    FactCard(headline = fact.first, detail = fact.second)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

/**
 * The motivational cat — the selected coach, once one has been picked on the
 * Coach screen. Portrait on the left (once its art exists) with its name
 * underneath, its line in a cartoon speech bubble on the right, tail pointing
 * back at the portrait. With no portrait, there's nowhere to anchor a name
 * under, so the bubble just takes the full width on its own — the tail is a
 * nice touch, not something the layout depends on.
 */
@Composable
private fun MascotCard(mascotDrawable: Int?, coachName: String?, saying: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (mascotDrawable != null) {
                // 50% bigger than the original 120dp portrait, now anchored to
                // the left so the speech bubble has somewhere to point at.
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(mascotDrawable),
                        contentDescription = coachName ?: "Motivational cat",
                        modifier = Modifier.size(180.dp),
                    )
                    if (coachName != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = coachName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
            }
            SpeechBubble(modifier = Modifier.weight(1f)) {
                Text(
                    text = saying,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

// -------------------------------------------------------------- speech bubble

private val BUBBLE_CORNER_RADIUS = 16.dp
private val BUBBLE_TAIL_WIDTH = 10.dp
private val BUBBLE_TAIL_HEIGHT = 18.dp
private val MASCOT_BUBBLE_SHAPE =
    SpeechBubbleShape(cornerRadius = BUBBLE_CORNER_RADIUS, tailWidth = BUBBLE_TAIL_WIDTH, tailHeight = BUBBLE_TAIL_HEIGHT)

/** A rounded, outlined panel styled like a cartoon speech bubble. */
@Composable
private fun SpeechBubble(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .clip(MASCOT_BUBBLE_SHAPE)
            .background(MaterialTheme.colorScheme.surface)
            .border(width = 1.5.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = MASCOT_BUBBLE_SHAPE)
            .padding(start = BUBBLE_TAIL_WIDTH + 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
    ) {
        content()
    }
}

/**
 * A rounded rectangle with a small triangular tail on its left edge, centered
 * vertically — the classic cartoon speech-bubble outline, pointing back
 * toward whatever is speaking.
 */
private class SpeechBubbleShape(
    private val cornerRadius: Dp,
    private val tailWidth: Dp,
    private val tailHeight: Dp,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val radius = with(density) { cornerRadius.toPx() }
        val tailW = with(density) { tailWidth.toPx() }
        val tailH = with(density) { tailHeight.toPx() }
        val bodyLeft = tailW

        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    left = bodyLeft,
                    top = 0f,
                    right = size.width,
                    bottom = size.height,
                    cornerRadius = CornerRadius(radius, radius),
                )
            )
            val tailCenterY = size.height / 2f
            moveTo(bodyLeft, tailCenterY - tailH / 2f)
            lineTo(0f, tailCenterY)
            lineTo(bodyLeft, tailCenterY + tailH / 2f)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
private fun StreakCard(streakDays: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (streakDays > 0) "🔥" else "🐾",
                fontSize = 28.sp,
                modifier = Modifier.padding(end = 12.dp),
            )
            Column {
                Text(
                    text = if (streakDays > 0) {
                        "$streakDays-day prowl streak"
                    } else {
                        "No streak yet"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = if (streakDays > 0) {
                        "Consecutive gym days with at least one lift logged."
                    } else {
                        "Log something today to start one."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun FactCard(headline: String, detail: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(headline, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ------------------------------------------------------------------- facts

private fun buildFacts(entries: List<LogEntry>, range: FactsRange): List<Pair<String, String>> {
    val totalLifts = entries.size
    val totalWeight = entries.sumOf { it.weight }
    val distinctMachines = entries.map { it.machineId }.distinct().size
    val heaviest = entries.maxByOrNull { it.weight }
    val favorite = entries.groupingBy { it.machineName }.eachCount().maxByOrNull { it.value }

    val facts = mutableListOf<Pair<String, String>>()

    facts += "🐾 $totalLifts pounce${if (totalLifts == 1) "" else "s"} logged" to
        "That's $totalLifts confirmed lift${if (totalLifts == 1) "" else "s"} ${rangePhrase(range)}."

    facts += "💪 $totalWeight lb hauled" to
        "Total weight across every machine ${rangePhrase(range)} — one paw-print per set, no reps counted."

    if (favorite != null) {
        facts += "🎯 Favorite scratching post: ${favorite.key}" to
            "Logged ${favorite.value} time${if (favorite.value == 1) "" else "s"} ${rangePhrase(range)}."
    }

    if (heaviest != null) {
        facts += "🏆 Heaviest catch: ${heaviest.weight} lb" to
            "On ${heaviest.machineName}, the top lift ${rangePhrase(range)}."
    }

    facts += "🐈 $distinctMachines machine${if (distinctMachines == 1) "" else "s"} worked" to
        "Different pieces of equipment touched ${rangePhrase(range)}."

    return facts
}

private fun rangePhrase(range: FactsRange): String = when (range) {
    FactsRange.TODAY -> "today"
    FactsRange.WEEK -> "this week"
    FactsRange.MONTH -> "this month"
    FactsRange.ALL -> "of all time"
}

// ------------------------------------------------------------------ ranges

private fun filterByRange(log: List<LogEntry>, range: FactsRange, today: LocalDate): List<LogEntry> {
    if (range == FactsRange.ALL) return log
    val weekFields = WeekFields.of(Locale.getDefault())

    return log.filter { entry ->
        val day = GymDay.dayOf(entry.loggedAt)
        when (range) {
            FactsRange.TODAY -> day == today
            FactsRange.WEEK ->
                day.get(weekFields.weekBasedYear()) == today.get(weekFields.weekBasedYear()) &&
                    day.get(weekFields.weekOfWeekBasedYear()) == today.get(weekFields.weekOfWeekBasedYear())
            FactsRange.MONTH -> day.year == today.year && day.month == today.month
            FactsRange.ALL -> true
        }
    }
}

/** Consecutive gym days, ending today or yesterday, with at least one lift. */
private fun currentStreak(log: List<LogEntry>, today: LocalDate): Int {
    val loggedDays = log.map { GymDay.dayOf(it.loggedAt) }.toHashSet()
    if (loggedDays.isEmpty()) return 0

    var cursor = if (today in loggedDays) today else today.minusDays(1)
    if (cursor !in loggedDays) return 0

    var streak = 0
    while (cursor in loggedDays) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}
