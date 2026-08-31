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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.balandman.liftlog.data.Difficulty
import com.balandman.liftlog.data.GymDay
import com.balandman.liftlog.data.Machine
import com.balandman.liftlog.data.Weights
import com.balandman.liftlog.ui.theme.DifficultyColors
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

/**
 * Confirming the previous weight is one tap on the biggest target on screen;
 * changing it is one tap per 5 lb, or a drag for a bigger jump.
 */
@Composable
fun LogSheet(
    machine: Machine,
    onDismiss: () -> Unit,
    onConfirm: (Int, Difficulty?) -> Unit,
    onUndo: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val doneToday = GymDay.isToday(machine.lastLoggedAt)

    var weight by remember(machine.id) {
        mutableIntStateOf(machine.lastWeight ?: Weights.DEFAULT)
    }
    var difficulty by remember(machine.id) {
        mutableStateOf<Difficulty?>(null)
    }

    fun close(after: () -> Unit = {}) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            after()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The whole reason to open this sheet is to look at the machine and
            // confirm it's the right one — the art gets to be the hero here,
            // not a small label next to the name.
            MachineArt(
                iconKey = machine.iconKey,
                size = 192.dp,
                illustrated = machine.illustrated,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = machine.name,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )

            Text(
                text = statusLine(machine, doneToday),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepButton(
                    label = "−5",
                    enabled = weight > Weights.MIN,
                    onClick = { weight = Weights.clamp(weight - Weights.STEP) },
                )

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = weight.toString(),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 60.sp,
                        ),
                    )
                    Text(
                        text = " lb",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }

                StepButton(
                    label = "+5",
                    enabled = weight < Weights.MAX,
                    onClick = { weight = Weights.clamp(weight + Weights.STEP) },
                )
            }

            Slider(
                value = weight.toFloat(),
                onValueChange = { weight = Weights.clamp(it.roundToInt()) },
                valueRange = Weights.MIN.toFloat()..Weights.MAX.toFloat(),
                steps = STEP_COUNT,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${Weights.MIN}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${Weights.MAX}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "How did it feel? (optional)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            DifficultyPicker(
                selected = difficulty,
                onSelect = { difficulty = if (difficulty == it) null else it },
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { close { onConfirm(weight, difficulty) } },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                Text(
                    text = if (weight == machine.lastWeight) "Confirm $weight lb" else "Log $weight lb",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            if (doneToday) {
                TextButton(onClick = { close { onUndo() } }) {
                    Text("Undo today's entry")
                }
            } else {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private fun statusLine(machine: Machine, doneToday: Boolean): String {
    val at = machine.lastLoggedAt
    val previous = machine.lastWeight
    return when {
        at == null || previous == null -> "No weight recorded yet"
        doneToday -> "Logged today at " +
            TIME_FORMAT.format(Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()))

        else -> "Last time: $previous lb on " +
            LAST_DATE_FORMAT.format(Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()))
    }
}

private val LAST_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")

/** Discrete stops the slider can land on, between the two endpoints. */
private val STEP_COUNT = ((Weights.MAX - Weights.MIN) / Weights.STEP) - 1

/**
 * Five color-coded pills, one per [Difficulty]. Tapping the one already
 * selected clears it — the rating is optional, and there should always be an
 * easy way back to "didn't say".
 */
@Composable
private fun DifficultyPicker(
    selected: Difficulty?,
    onSelect: (Difficulty) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Difficulty.entries.forEach { level ->
            val color = DifficultyColors.forName(level.name) ?: MaterialTheme.colorScheme.outline
            val isSelected = level == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clickable { onSelect(level) }
                    .background(
                        color = if (isSelected) color else color.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .border(
                        width = if (isSelected) 0.dp else 1.dp,
                        color = color.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = abbreviate(level),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = if (isSelected) bestOnColor(color) else color,
                )
            }
        }
    }
}

private fun abbreviate(level: Difficulty): String = when (level) {
    Difficulty.VERY_EASY -> "V.Easy"
    Difficulty.EASY -> "Easy"
    Difficulty.ABOUT_RIGHT -> "OK"
    Difficulty.HARD -> "Hard"
    Difficulty.VERY_HARD -> "V.Hard"
}

/** Cheap luminance check so the pill label stays legible on any fixed data color. */
private fun bestOnColor(background: Color): Color {
    val luminance = 0.299f * background.red + 0.587f * background.green + 0.114f * background.blue
    return if (luminance > 0.6f) Color(0xFF2A322E) else Color.White
}

@Composable
private fun StepButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        // The default button padding is wider than a 76dp circle can hold.
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(76.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
