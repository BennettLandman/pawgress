@file:OptIn(ExperimentalMaterial3Api::class)

package com.balandman.liftlog.ui

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.balandman.liftlog.R
import com.balandman.liftlog.data.GymDay
import com.balandman.liftlog.data.Machine
import com.balandman.liftlog.data.MachineGroup
import com.balandman.liftlog.ui.theme.LocalTileColors
import java.time.format.DateTimeFormatter

private val DATE_HEADER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d")

/** Body-area order for the main grid — the same order Settings groups by. */
private val GROUP_ORDER = listOf(
    MachineGroup.UPPER,
    MachineGroup.CORE,
    MachineGroup.LOWER,
    MachineGroup.OTHER,
)

@Composable
fun MainScreen(
    machines: List<Machine>,
    syncing: Boolean,
    onOpenSettings: () -> Unit,
    onOpenFunFacts: () -> Unit,
    onOpenTrends: () -> Unit,
    onSyncNow: () -> Unit,
    onTapMachine: (Machine) -> Unit,
    modifier: Modifier = Modifier,
) {
    val doneCount = machines.count { GymDay.isToday(it.lastLoggedAt) }

    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Pawgress",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.clickable(onClick = onOpenFunFacts),
                        )
                        IconButton(onClick = onOpenTrends, modifier = Modifier.size(30.dp)) {
                            Icon(
                                painter = painterResource(R.drawable.ic_trend_graph),
                                contentDescription = "Trends",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        text = buildSubtitle(doneCount, machines.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            actions = {
                if (syncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp).padding(end = 4.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    IconButton(onClick = onSyncNow) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Sync to Google Sheets")
                    }
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
        )

        if (machines.isEmpty()) {
            EmptyState(onOpenSettings)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 108.dp),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                // A subtle label per body area, shown only when more than one
                // area is on screen — with a single group it would just be
                // repeating what the grid already makes obvious.
                val groupsPresent = GROUP_ORDER.filter { g -> machines.any { it.group == g } }
                val showHeaders = groupsPresent.size > 1

                GROUP_ORDER.forEach { group ->
                    val inGroup = machines.filter { it.group == group }.sortedBy { it.sortOrder }
                    if (inGroup.isEmpty()) return@forEach

                    if (showHeaders) {
                        item(
                            key = "header_${group.name}",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            Text(
                                text = group.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                letterSpacing = 1.2.sp,
                                modifier = Modifier.padding(
                                    start = 4.dp,
                                    top = if (group == groupsPresent.first()) 4.dp else 14.dp,
                                    bottom = 2.dp,
                                ),
                            )
                        }
                    }

                    items(inGroup, key = { it.id }) { machine ->
                        MachineTile(
                            machine = machine,
                            done = GymDay.isToday(machine.lastLoggedAt),
                            onClick = { onTapMachine(machine) },
                        )
                    }
                }
            }
        }
    }
}

private fun buildSubtitle(done: Int, total: Int): String {
    val date = GymDay.today().format(DATE_HEADER)
    return if (done == 0) date else "$date  ·  $done of $total done"
}

/**
 * The weight is the hero: it has to be readable at arm's length, standing next
 * to the machine, without leaning in.
 */
@Composable
private fun MachineTile(
    machine: Machine,
    done: Boolean,
    onClick: () -> Unit,
) {
    val tiles = LocalTileColors.current
    val shape = RoundedCornerShape(18.dp)

    val container = if (done) tiles.done else tiles.fresh
    val contentColor = if (done) tiles.onDone else tiles.onFresh
    val numberColor = if (done) tiles.doneNumber else tiles.freshNumber
    val borderColor = if (done) tiles.doneBorder else Color.Transparent

    Box(
        modifier = Modifier
            .height(154.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(container)
            .border(width = if (done) 2.dp else 0.dp, color = borderColor, shape = shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            MachineArt(iconKey = machine.iconKey, size = 56.dp, illustrated = machine.illustrated)

            Spacer(Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = machine.lastWeight?.toString() ?: "—",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                    ),
                    color = numberColor,
                )
                if (machine.lastWeight != null) {
                    Text(
                        text = " lb",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }

            Text(
                text = machine.name,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                minLines = 2,
                lineHeight = 13.sp,
                fontSize = 11.sp,
            )
        }

        if (done) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Done today",
                tint = tiles.doneBadge,
                modifier = Modifier.size(20.dp).align(Alignment.TopEnd),
            )
        }
    }
}

@Composable
private fun EmptyState(onOpenSettings: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Every machine is hidden",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Open settings to choose the machines you actually use.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            androidx.compose.material3.Button(onClick = onOpenSettings) {
                Text("Open settings")
            }
        }
    }
}
