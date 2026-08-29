@file:OptIn(ExperimentalMaterial3Api::class)

package com.balandman.liftlog.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.balandman.liftlog.data.GymDay
import com.balandman.liftlog.data.LogEntry
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

private enum class FactsRange(val label: String) {
    TODAY("Today"),
    WEEK("This Week"),
    MONTH("This Month"),
    ALL("All Time"),
}

@Composable
fun FunFactsScreen(
    log: List<LogEntry>,
    onBack: () -> Unit,
) {
    var range by remember { mutableStateOf(FactsRange.TODAY) }
    val today = GymDay.today()
    val filtered = remember(log, range, today) { filterByRange(log, range, today) }
    val streak = remember(log, today) { currentStreak(log, today) }

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
