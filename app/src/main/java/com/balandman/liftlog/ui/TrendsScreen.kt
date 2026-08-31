@file:OptIn(ExperimentalMaterial3Api::class)

package com.balandman.liftlog.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.balandman.liftlog.data.GymDay
import com.balandman.liftlog.data.LogEntry
import com.balandman.liftlog.data.Machine
import com.balandman.liftlog.data.MachineGroup
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

/**
 * All ▸ a single body area ▸ a single machine. Drilling down narrows which
 * machines feed the overlay line chart below; the bar chart and gains list
 * stay scoped to the range picker above, not this.
 */
private sealed interface TrendScope {
    data object All : TrendScope
    data class Area(val group: MachineGroup) : TrendScope
    data class Single(val machineId: String, val machineName: String, val group: MachineGroup) : TrendScope
}

private data class Bucket(val label: String, val totalWeight: Int, val liftCount: Int)

/** One point on the overlay chart: how far above/below this machine's own starting weight. */
private data class SeriesPoint(val loggedAt: Long, val delta: Int)

private data class MachineSeries(
    val machineId: String,
    val name: String,
    val color: Color,
    val points: List<SeriesPoint>,
)

/** Distinct hues, cycled by index — separate from [com.balandman.liftlog.ui.theme.GroupColors], which labels an area rather than an individual machine. */
private val CHART_PALETTE = listOf(
    Color(0xFFB96756), Color(0xFF6E7F76), Color(0xFF978DAE), Color(0xFFC9A227),
    Color(0xFF4C7B8F), Color(0xFF9C5B8C), Color(0xFF5B8C5A), Color(0xFFB5533C),
    Color(0xFF3E6B8A), Color(0xFF8C7A3E), Color(0xFF6B5B95), Color(0xFF4E8C7A),
)

@Composable
fun TrendsScreen(
    machines: List<Machine>,
    log: List<LogEntry>,
    onBack: () -> Unit,
) {
    var range by remember { mutableStateOf(TrendRange.MONTH) }
    var scope by remember { mutableStateOf<TrendScope>(TrendScope.All) }
    val today = GymDay.today()

    // Every time series on this screen is scoped to currently-visible machines
    // by default — a hidden machine's history doesn't disappear, it's just not
    // what "how am I trending" means day to day.
    val visibleMachines = remember(machines) { machines.filter { it.visible } }
    val visibleIds = remember(visibleMachines) { visibleMachines.map { it.id }.toHashSet() }
    val visibleLog = remember(log, visibleIds) { log.filter { it.machineId in visibleIds } }

    val buckets = remember(visibleLog, range, today) { buildBuckets(visibleLog, range, today) }
    val rangeStart = remember(visibleLog, range, today) { rangeStart(visibleLog, range, today) }
    val inRange = remember(visibleLog, rangeStart) {
        visibleLog.filter { GymDay.dayOf(it.loggedAt) >= rangeStart }
    }
    val gains = remember(inRange, visibleMachines) { biggestGains(inRange, visibleMachines) }

    val scopedMachines = remember(visibleMachines, scope) {
        when (val s = scope) {
            TrendScope.All -> visibleMachines
            is TrendScope.Area -> visibleMachines.filter { it.group == s.group }
            is TrendScope.Single -> visibleMachines.filter { it.id == s.machineId }
        }
    }
    val series = remember(inRange, scopedMachines) { buildSeries(inRange, scopedMachines) }

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
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Relative progress, this range",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            "Each line starts at its machine's own weight at the start of " +
                                "this range, so a 5 lb gain on one machine and a 50 lb gain " +
                                "on another are easy to compare.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        ScopePicker(
                            scope = scope,
                            machines = visibleMachines,
                            onScopeChange = { scope = it },
                        )
                        Spacer(Modifier.height(12.dp))
                        if (series.isEmpty()) {
                            Text(
                                "Nothing with two or more entries in this range yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            OverlayLineChart(series)
                            Spacer(Modifier.height(12.dp))
                            ChartLegend(series)
                        }
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

/** All ▸ area ▸ machine, as a row of chips that grows a second row once an area is picked. */
@Composable
private fun ScopePicker(
    scope: TrendScope,
    machines: List<Machine>,
    onScopeChange: (TrendScope) -> Unit,
) {
    val groupsPresent = MachineGroup.entries.filter { g -> machines.any { it.group == g } }
    val activeGroup = when (scope) {
        is TrendScope.Area -> scope.group
        is TrendScope.Single -> scope.group
        TrendScope.All -> null
    }

    Column {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip(
                selected = scope == TrendScope.All,
                onClick = { onScopeChange(TrendScope.All) },
                label = { Text("All") },
            )
            groupsPresent.forEach { group ->
                FilterChip(
                    selected = activeGroup == group,
                    onClick = { onScopeChange(TrendScope.Area(group)) },
                    label = { Text(group.label) },
                )
            }
        }

        if (activeGroup != null) {
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                machines.filter { it.group == activeGroup }.sortedBy { it.sortOrder }.forEach { machine ->
                    val selected = (scope as? TrendScope.Single)?.machineId == machine.id
                    FilterChip(
                        selected = selected,
                        onClick = {
                            onScopeChange(TrendScope.Single(machine.id, machine.name, machine.group))
                        },
                        label = { Text(machine.name) },
                    )
                }
            }
        }
    }
}

/**
 * One line per machine, each rebased to its own starting weight in this range
 * so a small machine's progress and a big machine's progress read on the same
 * scale. A dashed zero line marks "same as range start".
 */
@Composable
private fun OverlayLineChart(series: List<MachineSeries>) {
    val trackColor = MaterialTheme.colorScheme.outlineVariant

    val minTime = series.minOf { it.points.first().loggedAt }
    val maxTime = series.maxOf { it.points.last().loggedAt }
    val timeSpan = (maxTime - minTime).coerceAtLeast(1L).toFloat()

    val minDelta = (series.minOf { s -> s.points.minOf { it.delta } }).coerceAtMost(0)
    val maxDelta = (series.maxOf { s -> s.points.maxOf { it.delta } }).coerceAtLeast(0)
    val deltaSpan = (maxDelta - minDelta).coerceAtLeast(1).toFloat()

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
    ) {
        val zeroY = size.height - ((0 - minDelta).toFloat() / deltaSpan) * size.height
        drawLine(
            color = trackColor,
            start = Offset(0f, zeroY),
            end = Offset(size.width, zeroY),
            strokeWidth = 1.dp.toPx(),
        )

        series.forEach { s ->
            if (s.points.size < 2) return@forEach
            val path = Path()
            s.points.forEachIndexed { index, point ->
                val x = ((point.loggedAt - minTime).toFloat() / timeSpan) * size.width
                val y = size.height - ((point.delta - minDelta).toFloat() / deltaSpan) * size.height
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = s.color,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

@Composable
private fun ChartLegend(series: List<MachineSeries>) {
    Column {
        series.forEach { s ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(s.color, CircleShape),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    s.name,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                val latest = s.points.last().delta
                Text(
                    text = (if (latest > 0) "+" else "") + "$latest lb",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = if (latest > 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
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

/** One series per machine with 2+ entries in range, rebased to its first entry in range. */
private fun buildSeries(inRange: List<LogEntry>, machines: List<Machine>): List<MachineSeries> {
    return machines.mapIndexedNotNull { index, machine ->
        val entries = inRange.filter { it.machineId == machine.id }.sortedBy { it.loggedAt }
        if (entries.size < 2) return@mapIndexedNotNull null
        val base = entries.first().weight
        MachineSeries(
            machineId = machine.id,
            name = machine.name,
            color = CHART_PALETTE[index % CHART_PALETTE.size],
            points = entries.map { SeriesPoint(it.loggedAt, it.weight - base) },
        )
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
