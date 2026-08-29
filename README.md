# Pawgress

An Android app for tracking how much weight you lift on each machine. No reps, no
sets, no rest timers — one number per machine per gym day.

**To build and install it, read [SETUP.md](SETUP.md).**

---

## How it works

**The grid.** One tile per machine, showing its icon, the last weight you lifted on
it, and its name. The weight is deliberately huge — it has to be readable at arm's
length while you are standing next to the machine.

**Logging.** Tap a tile. A sheet slides up with the previous weight already dialed
in and a wide **Confirm 130 lb** button, so the common case is one tap. If today is
different, `−5` / `+5` are large circular buttons, and a slider handles bigger jumps.
The range is 10–300 lb in 5 lb steps.

**Done state.** Once logged, the tile turns filled with a checkmark. Everything
resets at **4am**, so a late-night session still counts as that evening's workout
and nothing flips over underneath you mid-workout. Rest days simply pass untouched.

**Correcting.** Tap a completed tile to change the weight or undo it entirely. If
the entry had already been uploaded, the stale row is removed from the spreadsheet
rather than left sitting next to the corrected one.

**Machines.** 36 come preloaded, 22 of them visible from the start. Settings lets
you hide any of them (history is kept), rename them to match your gym's naming,
change their icon, and add your own.

**Google Sheets.** Optional. Sign in and the app creates a spreadsheet called
*Pawgress* in your own Drive, then mirrors every lift into it, one row per lift:
`Date | Time | Exercise | Weight (lb) | Entry ID`. The app is fully usable signed
out — sync is a mirror, never the source of truth. Lifts logged offline queue up
and go on the next sync.

**Profiles.** Every Google account that signs in gets its own everything: its own
machine grid, its own weights and history, and its own spreadsheet in its own
Drive. Signing in as someone else switches the whole app over; signing back
returns you to exactly what you left. Two people can share a phone without ever
seeing each other's numbers. Whatever you set up *before* signing in is adopted
by the first account to sign in, so nothing configured early is lost.

---

## Design decisions worth knowing

**No database.** All state is one small JSON file in the app's private storage
(`LiftRepository`). A gym log is a few thousand rows over years; Room would have
been more machinery than the problem deserves, and it kept the build free of
annotation processing.

**Authorization, not "sign in".** Asking for the Drive scope already makes Google
pick an account and show consent, which is everything the app needs. Adding
Credential Manager on top would have meant registering a second OAuth client for no
extra capability.

**`drive.file` scope, not `drive`.** The app can only touch files it created
itself. It cannot see anything else in your Drive, and no amount of bugs in it
could.

**The account is never taken on trust.** Every sync resolves the signed-in address
from the token Google just issued, not from anything stored, and the profile is
selected from that. It is the one thing standing between two users of the same
phone and one person's lifts landing in the other's Drive.

**Background syncs cannot switch users.** If a background refresh comes back
holding a different account than the grid on screen, it gives up rather than
swapping the whole app out from under someone mid-workout. Only a deliberate
action — signing in, or Switch account — is allowed to change profiles.

**The system account picker.** Google's authorization client silently reuses
whichever account already consented, which is right for a background refresh and
useless for switching users. The app launches Android's own account chooser first,
which needs no permission, then authorizes that specific account.

**Fixed palette, not Material You.** Dynamic color could wash out the difference
between a done tile and a fresh one, and that distinction is the whole point of the
screen.

**Icons drawn by hand.** All 44 pictograms are vector paths written for this app,
checked at tile size for legibility, and distinct enough to tell apart at a glance —
particularly the pairs that matter, like hip abduction versus adduction. They are
placeholders for commissioned artwork; each sits on a fixed cream backdrop
(`MachineArt`) so one drawing works on a fresh tile, a done tile, and in both
light and dark mode without needing variants.

---

## Layout

```
app/src/main/
├── AndroidManifest.xml
├── res/drawable/ic_m_*.xml        44 machine pictograms
└── java/com/balandman/liftlog/
    ├── MainActivity.kt            entry point, screens, consent + picker results
    ├── LiftLogApp.kt              holds the repository
    ├── data/
    │   ├── Models.kt              Machine, LogEntry, Profile, weight rules
    │   ├── GymDay.kt              the 4am-to-4am day
    │   ├── MachineCatalog.kt      the 36 preloaded machines
    │   └── LiftRepository.kt      profiles + JSON persistence
    ├── sync/
    │   ├── GoogleAuth.kt          account authorization
    │   ├── AccountPicker.kt       the system account chooser
    │   ├── SheetsApi.kt           Sheets REST calls
    │   └── SyncManager.kt         which profile, what to push, and when
    └── ui/
        ├── MainScreen.kt          the grid
        ├── LogSheet.kt            the logging sheet
        ├── SettingsScreen.kt      machines + account
        ├── MainViewModel.kt
        ├── MachineArt.kt          artwork on its fixed backdrop
        ├── MachineIcons.kt        icon key → drawable
        └── theme/Theme.kt
```

The saved file (`liftlog.json` in private storage) is a map of profiles keyed by
account address plus which one is active, so adding a person costs one more entry
and touches nothing else.

## Things you might want to change

| Change | Where |
| --- | --- |
| Weight range or step size | `Weights` in `data/Models.kt` |
| When the day resets | `GymDay.RESET_HOUR` |
| Preloaded machines | `MachineCatalog.SEEDS` |
| Colors | `ui/theme/Theme.kt` |
| Spreadsheet columns | `HEADER` and `toRow()` in `sync/SheetsApi.kt` |
| How long before an auto-sync fires | `AUTO_SYNC_DELAY_MS` in `ui/MainViewModel.kt` |

Adding a machine to `MachineCatalog` and reinstalling merges it into an existing
install rather than resetting anything, so the catalog can grow safely.

---

## Known caveats

- Built without ever being compiled — the machine this was written on had no
  access to Google's Maven servers or the Android SDK. The code was reviewed
  carefully and structurally checked, but expect the first build to surface a
  fixable mistake or two.
- The Gradle wrapper `.jar` is not included, for the same reason. See SETUP.md.
- `AuthorizationRequest.Builder.setAccount()` in `GoogleAuth` is the one API here
  I could not verify against a compiler. If the build objects to it, deleting that
  one line still leaves everything working — the app just loses the ability to
  target a specific account, so switching users gets less reliable.
- Sync is one-directional. Editing the spreadsheet by hand will not change what
  the app shows, and the app may overwrite assumptions you make there.
- Profiles are separated for privacy, not secured. Anyone holding the unlocked
  phone can switch to another profile and read its grid; the data sits in the
  app's private storage, unencrypted, like any single-user app's would.
