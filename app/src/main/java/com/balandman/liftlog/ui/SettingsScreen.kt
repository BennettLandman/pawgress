@file:OptIn(ExperimentalMaterial3Api::class)

package com.balandman.liftlog.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.balandman.liftlog.R
import com.balandman.liftlog.data.Machine
import com.balandman.liftlog.data.MachineCatalog
import com.balandman.liftlog.data.MachineGroup
import com.balandman.liftlog.data.SyncState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val SYNCED_AT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d 'at' h:mm a")

@Composable
fun SettingsScreen(
    machines: List<Machine>,
    syncState: SyncState,
    syncing: Boolean,
    pendingCount: Int,
    onBack: () -> Unit,
    onChooseAccount: () -> Unit,
    onDisconnectGoogle: () -> Unit,
    onSyncNow: () -> Unit,
    onSetVisible: (String, Boolean) -> Unit,
    onSetAllVisible: (Boolean) -> Unit,
    onRename: (String, String) -> Unit,
    onSetIcon: (String, String, Boolean) -> Unit,
    onAddMachine: (String, String, MachineGroup, Boolean) -> Unit,
    onDeleteMachine: (String) -> Unit,
) {
    var editing by remember { mutableStateOf<Machine?>(null) }
    var adding by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                GoogleCard(
                    syncState = syncState,
                    syncing = syncing,
                    pendingCount = pendingCount,
                    onChooseAccount = onChooseAccount,
                    onDisconnect = onDisconnectGoogle,
                    onSyncNow = onSyncNow,
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Machines", style = MaterialTheme.typography.titleMedium)
                    Row {
                        TextButton(onClick = { onSetAllVisible(false) }) { Text("Hide all") }
                        TextButton(onClick = { onSetAllVisible(true) }) { Text("Show all") }
                    }
                }
                Text(
                    "Switch off anything you don't use — hiding keeps its history.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { adding = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Add a machine")
                }
            }

            MachineGroup.entries.forEach { group ->
                val inGroup = machines.filter { it.group == group }.sortedBy { it.sortOrder }
                if (inGroup.isEmpty()) return@forEach

                item(key = "header_${group.name}") {
                    Text(
                        text = group.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                    )
                }

                items(inGroup, key = { it.id }) { machine ->
                    MachineRow(
                        machine = machine,
                        onToggle = { onSetVisible(machine.id, it) },
                        onEdit = { editing = machine },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            item { CreditsSection() }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (adding) {
        MachineDialog(
            title = "Add a machine",
            initialName = "",
            initialIcon = "machine",
            initialGroup = MachineGroup.OTHER,
            initialIllustrated = true,
            canDelete = false,
            onDismiss = { adding = false },
            onSave = { name, icon, group, illustrated ->
                onAddMachine(name, icon, group, illustrated)
                adding = false
            },
            onDelete = {},
        )
    }

    editing?.let { machine ->
        MachineDialog(
            title = "Edit machine",
            initialName = machine.name,
            initialIcon = machine.iconKey,
            initialGroup = machine.group,
            initialIllustrated = machine.illustrated,
            canDelete = machine.custom,
            groupEditable = false,
            onDismiss = { editing = null },
            onSave = { name, icon, _, illustrated ->
                onRename(machine.id, name)
                onSetIcon(machine.id, icon, illustrated)
                editing = null
            },
            onDelete = {
                onDeleteMachine(machine.id)
                editing = null
            },
        )
    }
}

@Composable
private fun GoogleCard(
    syncState: SyncState,
    syncing: Boolean,
    pendingCount: Int,
    onChooseAccount: () -> Unit,
    onDisconnect: () -> Unit,
    onSyncNow: () -> Unit,
) {
    val context = LocalContext.current
    val connectedEmail = syncState.accountEmail
    val profileEmail = syncState.profileEmail
    val lastSync = syncState.lastSyncAt

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Google account", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))

            when {
                // Nobody has ever signed in on this phone.
                profileEmail == null -> {
                    Text(
                        "Sign in and the app creates a spreadsheet in your own Google " +
                            "Drive, then mirrors every lift into it. Everything works " +
                            "without signing in — the log just stays on this phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onChooseAccount, enabled = !syncing) {
                        Text("Sign in with Google")
                    }
                }

                // A profile is on screen but syncing is switched off.
                connectedEmail == null -> {
                    Text(
                        profileEmail,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "Not syncing. This profile's history is still on the phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onChooseAccount, enabled = !syncing) {
                            Text("Sign back in")
                        }
                    }
                }

                else -> {
                    Text(
                        connectedEmail,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = when {
                            syncing -> "Syncing…"
                            pendingCount > 0 -> "$pendingCount lift(s) waiting to upload"
                            lastSync != null -> "Last synced " + SYNCED_AT.format(
                                Instant.ofEpochMilli(lastSync).atZone(ZoneId.systemDefault())
                            )

                            else -> "Not synced yet"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    syncState.lastError?.let { error ->
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onSyncNow, enabled = !syncing) { Text("Sync now") }
                        syncState.spreadsheetUrl?.let { url ->
                            OutlinedButton(onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                            }) { Text("Open sheet") }
                        }
                    }
                    Row {
                        TextButton(onClick = onChooseAccount, enabled = !syncing) {
                            Text("Switch account")
                        }
                        TextButton(onClick = onDisconnect) { Text("Stop syncing") }
                    }
                }
            }

            if (profileEmail != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (syncState.profileCount > 1) {
                        "${syncState.profileCount} accounts have used this phone. Each one " +
                            "keeps its own machines, weights, history and spreadsheet."
                    } else {
                        "Each account that signs in gets its own machines, weights, " +
                            "history and spreadsheet."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MachineRow(
    machine: Machine,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MachineArt(iconKey = machine.iconKey, size = 34.dp, illustrated = machine.illustrated)
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(machine.name, style = MaterialTheme.typography.bodyLarge)
            machine.lastWeight?.let {
                Text(
                    "$it lb",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onEdit) {
            Icon(
                Icons.Filled.Edit,
                contentDescription = "Edit ${machine.name}",
                modifier = Modifier.size(20.dp),
            )
        }
        Switch(checked = machine.visible, onCheckedChange = onToggle)
    }
}

@Composable
private fun MachineDialog(
    title: String,
    initialName: String,
    initialIcon: String,
    initialGroup: MachineGroup,
    initialIllustrated: Boolean,
    canDelete: Boolean,
    groupEditable: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (String, String, MachineGroup, Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var icon by remember { mutableStateOf(initialIcon) }
    var group by remember { mutableStateOf(initialGroup) }
    var illustrated by remember { mutableStateOf(initialIllustrated) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (groupEditable) {
                    Spacer(Modifier.height(12.dp))
                    Text("Group", style = MaterialTheme.typography.labelMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                    ) {
                        MachineGroup.entries.forEach { option ->
                            FilterChip(
                                selected = group == option,
                                onClick = { group = option },
                                label = { Text(option.label) },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Illustrated artwork", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "Off shows the classic line icon instead.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = illustrated && MachineIcons.artFor(icon) != null,
                        onCheckedChange = { illustrated = it },
                        enabled = MachineIcons.artFor(icon) != null,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text("Icon", style = MaterialTheme.typography.labelMedium)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(48.dp),
                    modifier = Modifier.height(180.dp).padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(MachineCatalog.ICON_KEYS, key = { it }) { key ->
                        val selected = key == icon
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp),
                                )
                                .clickable { icon = key },
                            contentAlignment = Alignment.Center,
                        ) {
                            MachineArt(
                                iconKey = key,
                                size = 36.dp,
                                illustrated = illustrated || MachineIcons.artFor(key) == null,
                            )
                        }
                    }
                }

                if (canDelete) {
                    TextButton(onClick = onDelete) {
                        Text("Delete machine", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, icon, group, illustrated) },
                enabled = name.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/**
 * The exact credits copy Bennett wrote, shown once at the bottom of settings —
 * below the photo he supplied, never reworded.
 */
@Composable
private fun CreditsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(bottom = 20.dp),
        )

        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(R.drawable.credits_photo),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .clip(RoundedCornerShape(20.dp)),
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "Created by Orinda & Bennett Landman",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Made with cats, curiosity, and an unreasonable amount of code.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            "Built with the help of Claude and OpenAI Codex.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Version 1.0",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            "Your data stays yours.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "This app does not collect or store user data in a central database. " +
                "No sign-in is required. If you choose to sign in, your app data is " +
                "stored in a Google Sheet associated with your Google account.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "© 2026 Orinda & Bennett Landman. All rights reserved.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
