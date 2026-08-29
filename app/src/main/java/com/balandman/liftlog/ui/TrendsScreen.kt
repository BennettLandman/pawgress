@file:OptIn(ExperimentalMaterial3Api::class)

package com.balandman.liftlog.ui

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.balandman.liftlog.data.GymDay
import com.balandman.liftlog.data.LogEntry
import com.balandman.liftlog.data.Machine
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private enum class TrendRange(val label: String) {
    WEEK("This Week"),
    MONTH("This Month"),
    YEAR("This Year"),
    ALL("All Time"),
}

private data class Bucket(val label: String, val totalWeight: Int, val liftCount: Int)

@Composable
fun TrendsScreen(
    machines: List<Machine>,
    log: List<LogEntry>,
    onBack: () -> Unit,
) {
    var range by remember { mutableStateOf(TrendRange.MONTH) }
    val today = GymDay.today()

    val buckets = remember(log, range, today) { buildBuckets(log, range, today) }
    val rangeStart = remember(log, range, today) { rangeStart(log, range, today) }
    val inRange = remember(log, rangeStart) { log.filter { GymDay.dayOf(it.loggedAt) >= rangeStart } }
    val gains = remember(inRange, machines) { biggestGains(inRange, machines) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Trends") },
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
            TrendRange.entries.forEach { option ->
                FilterChip(
                    selected = range == option,
                    onClick = { range = option },
                    label = { Text(option.label) },
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Weight lifted per ${bucketNoun(range)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.height(12.dp))
                        if (buckets.all { it.totalWeight == 0 }) {
                            Text(
                                "Nothing logged in this range yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            BarChart(buckets)
                        }
                        Spacer(Modifier.height(12.dp))
                        val totalWeight = inRange.sumOf { it.weight }
                        Text(
                            "$totalWeight lb total across ${inRange.size} lift" +
                                (if (inRange.size == 1) "" else "s") + ".",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Text(
                    "Biggest gains this range",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
            }

            if (gains.isEmpty()) {
                item {
                    Text(
                        "Keep logging — gains show up once a machine has two or more " +
                            "entries in this range.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(gains) { gain ->
                    GainRow(gain)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun BarChart(buckets: List<Bucket>) {
    val max = (buckets.maxOfOrNull { it.totalWeight } ?: 0).coerceAtLeast(1)
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        buckets.forEach { bucket ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(96.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .width(14.dp)
                            .height(96.dp)
                            .clip(RoundedCornerShape(4.dp)),
                    ) {
                        drawRect(color = trackColor)
                        val fraction = bucket.totalWeight.toFloat() / max.toFloat()
                        val barHeight = size.height * fraction
                        drawRect(
                            color = barColor,
                            topLeft = androidx.compose.ui.geometry.Offset(
                                0f,
                                size.height - barHeight,
                            ),
                            size = androidx.compose.ui.geometry.Size(size.width, barHeight),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    bucket.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                )
            }
        }
    }
}

private data class Gain(val machineName: String, val delta: Int, val from: Int, val to: Int)

@Composable
private fun GainRow(gain: Gain) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(gain.machineName, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${gain.from} lb → ${gain.to} lb",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val positive = gain.delta > 0
        Text(
            text = (if (positive) "+" else "") + "${gain.delta} lb",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = if (positive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.End,
        )
    }
}

// ----------------------------------------------------------------- bucketing

private fun bucketNoun(range: TrendRange): String = when (range) {
    TrendRange.WEEK -> "day"
    TrendRange.MONTH -> "day"
    TrendRange.YEAR -> "month"
    TrendRange.ALL -> "month"
}

private fun rangeStart(log: List<LogEntry>, range: TrendRange, today: LocalDate): LocalDate =
    when (range) {
        TrendRange.WEEK -> today.minusDays(6)
        TrendRange.MONTH -> today.withDayOfMonth(1)
        TrendRange.YEAR -> today.withDayOfYear(1)
        TrendRange.ALL -> log.minOfOrNull { GymDay.dayOf(it.loggedAt) } ?: today
    }

private fun buildBuckets(log: List<LogEntry>, range: TrendRange, today: LocalDate): List<Bucket> {
    val byDay: Map<LocalDate, List<LogEntry>> = log.groupBy { GymDay.dayOf(it.loggedAt) }

    return when (range) {
        TrendRange.WEEK -> {
            val dayLabel = DateTimeFormatter.ofPattern("EEE")
            (6 downTo 0).map { offset ->
                val day = today.minusDays(offset.toLong())
                val entries = byDay[day].orEmpty()
                Bucket(day.format(dayLabel), entries.sumOf { it.weight }, entries.size)
            }
        }

        TrendRange.MONTH -> {
            val start = today.withDayOfMonth(1)
            val days = today.lengthOfMonth()
            (0 until days).map { offset ->
                val day = start.plusDays(offset.toLong())
                val entries = byDay[day].orEmpty()
                val showLabel = offset == 0 || offset == days - 1 || (offset + 1) % 5 == 0
                Bucket(
                    if (showLabel) day.dayOfMonth.toString() else "",
                    entries.sumOf { it.weight },
                    entries.size,
                )
            }
        }

        TrendRange.YEAR -> {
            val monthLabel = DateTimeFormatter.ofPattern("MMM")
            (0 until 12).map { m ->
                val monthStart = today.withDayOfYear(1).plusMonths(m.toLong())
                val monthEnd = monthStart.plusMonths(1)
                val entries = log.filter {
                    val d = GymDay.dayOf(it.loggedAt)
                    d >= monthStart && d < monthEnd
                }
                Bucket(monthStart.format(monthLabel), entries.sumOf { it.weight }, entries.size)
            }
        }

        TrendRange.ALL -> {
            if (log.isEmpty()) return emptyList()
            val firstMonth = log.minOf { GymDay.dayOf(it.loggedAt) }.withDayOfMonth(1)
            val lastMonth = today.withDayOfMonth(1)
            val span = ChronoUnit.MONTHS.between(firstMonth, lastMonth).toInt()
            (0..span).map { m ->
                val monthStart = firstMonth.plusMonths(m.toLong())
                val monthEnd = monthStart.plusMonths(1)
                val entries = log.filter {
                    val d = GymDay.dayOf(it.loggedAt)
                    d >= monthStart && d < monthEnd
                }
                val label = monthStart.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()) +
                    if (monthStart.year != today.year) " '${monthStart.year % 100}" else ""
                Bucket(label, entries.sumOf { it.weight }, entries.size)
            }
        }
    }
}

private fun biggestGains(inRange: List<LogEntry>, machines: List<Machine>): List<Gain> {
    val names = machines.associateBy({ it.id }, { it.name })
    return inRange.groupBy { it.machineId }
        .mapNotNull { (machineId, entries) ->
            if (entries.size < 2) return@mapNotNull null
            val sorted = entries.sortedBy { it.loggedAt }
            val from = sorted.first().weight
            val to = sorted.last().weight
            val delta = to - from
            if (delta == 0) return@mapNotNull null
            Gain(names[machineId] ?: sorted.last().machineName, delta, from, to)
        }
        .sortedByDescending { it.delta }
        .take(6)
}
