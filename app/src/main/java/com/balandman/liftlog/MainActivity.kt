package com.balandman.liftlog

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.balandman.liftlog.sync.AccountPicker
import com.balandman.liftlog.ui.CoachScreen
import com.balandman.liftlog.ui.FunFactsScreen
import com.balandman.liftlog.ui.LogSheet
import com.balandman.liftlog.ui.MainScreen
import com.balandman.liftlog.ui.MainViewModel
import com.balandman.liftlog.ui.SettingsScreen
import com.balandman.liftlog.ui.TrendsScreen
import com.balandman.liftlog.ui.theme.LiftLogTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var consentLauncher: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var accountLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Google hands back a PendingIntent for the consent screen; this is where
        // its result comes home.
        consentLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            viewModel.onConsentResult(this, result.resultCode == RESULT_OK, result.data)
        }

        // The system account chooser, used whenever the user is deliberately
        // picking or changing which Google account the app is working as.
        accountLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            viewModel.onAccountChosen(
                this,
                if (result.resultCode == RESULT_OK) AccountPicker.accountFrom(result.data) else null,
            )
        }

        setContent {
            LiftLogTheme {
                AppRoot(
                    viewModel = viewModel,
                    onLaunchConsent = { pendingIntent: PendingIntent ->
                        consentLauncher.launch(IntentSenderRequest.Builder(pendingIntent).build())
                    },
                    onLaunchAccountPicker = { accountLauncher.launch(AccountPicker.intent()) },
                )
            }
        }
    }
}

@Composable
private fun AppRoot(
    viewModel: MainViewModel,
    onLaunchConsent: (PendingIntent) -> Unit,
    onLaunchAccountPicker: () -> Unit,
) {
    val allMachines by viewModel.machines.collectAsStateWithLifecycle()
    val visibleMachines by viewModel.visibleMachines.collectAsStateWithLifecycle()
    val log by viewModel.log.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val syncing by viewModel.syncing.collectAsStateWithLifecycle()
    val pendingCount by viewModel.pendingCount.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val consentRequest by viewModel.consentRequest.collectAsStateWithLifecycle()
    val accountPickerRequest by viewModel.accountPickerRequest.collectAsStateWithLifecycle()
    val pawprintsBalance by viewModel.pawprintsBalance.collectAsStateWithLifecycle()
    val unlockedCoachIds by viewModel.unlockedCoachIds.collectAsStateWithLifecycle()
    val selectedCoachId by viewModel.selectedCoachId.collectAsStateWithLifecycle()
    val unlockedOutfits by viewModel.unlockedOutfits.collectAsStateWithLifecycle()
    val equippedOutfits by viewModel.equippedOutfits.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // "main" | "settings" | "funfacts" | "trends" | "coach" — a plain string
    // survives process death via rememberSaveable with no custom Saver to write.
    var screen by rememberSaveable { mutableStateOf("main") }
    var sheetMachineId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(consentRequest) {
        consentRequest?.let {
            onLaunchConsent(it)
            viewModel.consentLaunched()
        }
    }

    LaunchedEffect(accountPickerRequest) {
        if (accountPickerRequest) {
            onLaunchAccountPicker()
            viewModel.accountPickerLaunched()
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    BackHandler(enabled = screen != "main") { screen = "main" }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (screen) {
                "settings" -> SettingsScreen(
                    machines = allMachines,
                    syncState = syncState,
                    syncing = syncing,
                    pendingCount = pendingCount,
                    onBack = { screen = "main" },
                    onChooseAccount = viewModel::chooseAccount,
                    onDisconnectGoogle = viewModel::disconnect,
                    onSyncNow = { viewModel.syncNow(context) },
                    onSetVisible = viewModel::setVisible,
                    onSetAllVisible = viewModel::setAllVisible,
                    onRename = viewModel::rename,
                    onSetIcon = viewModel::setIcon,
                    onSetGroup = viewModel::setGroup,
                    onAddMachine = viewModel::addMachine,
                    onDeleteMachine = viewModel::deleteMachine,
                    onResetToday = viewModel::resetToday,
                    onFullReset = viewModel::fullReset,
                    onRestoreFromSheet = { viewModel.restoreFromSheet(context) },
                    // DEBUG ONLY — remove along with the button in Settings before release.
                    onDebugGrantPawprints = viewModel::debugGrantPawprints,
                )

                "funfacts" -> FunFactsScreen(
                    log = log,
                    selectedCoachId = selectedCoachId,
                    equippedOutfits = equippedOutfits,
                    onBack = { screen = "main" },
                )

                "trends" -> TrendsScreen(
                    machines = allMachines,
                    log = log,
                    onBack = { screen = "main" },
                )

                "coach" -> CoachScreen(
                    pawprintsBalance = pawprintsBalance,
                    unlockedCoachIds = unlockedCoachIds,
                    selectedCoachId = selectedCoachId,
                    unlockedOutfits = unlockedOutfits,
                    equippedOutfits = equippedOutfits,
                    onBack = { screen = "main" },
                    onSelectCoach = viewModel::selectCoach,
                    onUnlockCoach = viewModel::unlockCoach,
                    onUnlockOutfit = viewModel::unlockOutfit,
                    onEquipOutfit = viewModel::equipOutfit,
                )

                else -> MainScreen(
                    machines = visibleMachines,
                    pawprintsBalance = pawprintsBalance,
                    onOpenSettings = { screen = "settings" },
                    onOpenFunFacts = { screen = "funfacts" },
                    onOpenTrends = { screen = "trends" },
                    onOpenCoach = { screen = "coach" },
                    onTapMachine = { sheetMachineId = it.id },
                )
            }
        }
    }

    // Read from the full list so the sheet reflects edits made while it is open.
    val sheetMachine = sheetMachineId?.let { id -> allMachines.firstOrNull { it.id == id } }
    if (sheetMachine != null) {
        LogSheet(
            machine = sheetMachine,
            onDismiss = { sheetMachineId = null },
            onConfirm = { weight, difficulty -> viewModel.logLift(sheetMachine.id, weight, difficulty) },
            onUndo = { viewModel.undoToday(sheetMachine.id) },
        )
    }
}
