# Getting Pawgress onto your phone

Three stages: **build it**, **tell Google about it**, **install it**. Budget about
45 minutes the first time, most of it waiting on downloads.

Requires Android 8.0 or newer on the phone.

---

## Stage 1 — Build

### The easy path: Android Studio

1. Download Android Studio from <https://developer.android.com/studio> and install
   it. On first launch let it run the setup wizard — that is what installs the
   Android SDK, and everything later depends on it.

2. **File → Open**, choose `~/dev/LiftLog`, and click Open.

3. Android Studio will start a Gradle sync. Two prompts are likely:

   - *"Gradle wrapper not found"* or a dialog asking where to get Gradle. Choose
     **Gradle wrapper** / the default option and let it download. (I could not
     ship the wrapper's `.jar` — the machine I built this on has no access to
     Gradle's servers.)
   - *"Install missing SDK platform"* or similar — accept it.

4. Sync takes a few minutes the first time; it is downloading Gradle, the Android
   SDK platform, and the libraries. When the status bar goes quiet with no red
   errors, you are built.

If sync fails, the error text in the **Build** panel is the thing to send me.

### The terminal path, if you would rather

You still need the Android SDK, which realistically means installing Android
Studio anyway. Once it exists:

```bash
brew install gradle
cd ~/dev/LiftLog
gradle wrapper --gradle-version 8.9   # one time, creates ./gradlew
./gradlew assembleDebug
```

The APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

---

## Stage 2 — Tell Google about the app

Sign-in and the Sheets sync will not work until this is done. The app itself runs
fine without it, so you can skip ahead, install, and come back.

Nothing from this stage gets pasted into the code — Google matches Android apps by
package name plus signing fingerprint, so there is no client ID to copy anywhere.

### 2a. Your signing fingerprint

Already extracted from your built APK, so there is nothing to run:

```
SHA-1:   5E:19:F2:05:D3:FF:4C:F1:9D:AC:2C:9F:69:B8:49:5E:CE:3F:34:0D
Package: com.balandman.liftlog
```

It comes from the standard Android debug keystore (`CN=Android Debug`, valid until
2056), which lives on this Mac and does not change between builds. Build on a
different machine or switch to a release keystore and you will need to register
that new fingerprint too — to read it off any APK:

```bash
keytool -list -v -alias androiddebugkey \
  -keystore ~/.android/debug.keystore \
  -storepass android -keypass android | grep SHA1
```

### 2b. Create a Google Cloud project

1. Go to <https://console.cloud.google.com> and sign in with the Google account
   whose Drive should hold the spreadsheet.
2. Project dropdown at the top → **New Project** → name it `Pawgress` → Create,
   then make sure it is the selected project.

### 2c. Enable the Sheets API

**APIs & Services → Library** → search *Google Sheets API* → **Enable**.

That is the only API needed. (The app asks for a Drive scope, but never calls the
Drive API — it creates and writes the spreadsheet through Sheets.)

### 2d. Configure the consent screen

Under **APIs & Services → OAuth consent screen** (newer consoles call this
**Google Auth Platform**):

1. User type **External** → Create.
2. App name `Pawgress`, user support email and developer contact email = your own
   address. Save and continue.
3. **Scopes**: you can leave this empty — the app requests its scopes at runtime.
4. **Test users**: add your own Gmail address. This matters. An app in Testing
   mode only works for accounts listed here.
5. Leave the app in **Testing**. Publishing would trigger Google's verification
   review, which is pointless for an app only you will ever run.

### 2e. Create the Android OAuth client

**APIs & Services → Credentials → Create credentials → OAuth client ID**:

- Application type: **Android**
- Name: anything, e.g. `Pawgress debug`
- Package name: `com.balandman.liftlog`
- SHA-1 certificate fingerprint: the value from step 2a

Create it. There is nothing to download or copy back.

---

## Stage 3 — Install on the phone

### Over USB (best, and gives you one-tap rebuilds later)

1. On the phone: **Settings → About phone**, tap **Build number** seven times to
   unlock Developer options.
2. **Settings → System → Developer options → USB debugging**, turn it on.
3. Plug the phone into the Mac. A prompt appears on the phone — **Allow USB
   debugging**, and tick "always allow".
4. In Android Studio, pick the phone from the device dropdown in the toolbar and
   press **Run** (the green triangle).

### By file, if you would rather not use USB

Build the APK (`./gradlew assembleDebug`), get
`app/build/outputs/apk/debug/app-debug.apk` onto the phone — Drive, email, AirDroid,
whatever — then tap it in the phone's Files app. Android will ask permission to
install from that app the first time; grant it and tap Install.

---

## First run

The grid comes up with your 22 machines, all showing `—` because nothing has been
logged yet. Another 14 are preloaded but hidden.

1. Tap the **gear** → **Sign in with Google** → pick your account and approve.
   The app creates a spreadsheet called **Pawgress** in the root of your Drive.
2. Back on the grid, tap a machine, set the weight, **Log**. It appears in the
   sheet within a few seconds.
3. Tap the gear again to hide anything you do not use, rename machines to match
   your gym's labels, or switch on one of the hidden ones. Hiding keeps history.

The sheet's columns are `Date | Time | Exercise | Weight (lb) | Entry ID`. The last
column is how the app finds a row again when you undo or correct an entry — leave
it alone and ignore it when charting.

### More than one person on the same phone

Each Google account gets its own profile: its own machine grid, its own weights and
history, and its own spreadsheet in its own Drive. **Switch account** in the gear
screen opens Android's account chooser and swaps the whole app over; signing back
in returns you to exactly what you left. Nobody's lifts ever appear in anyone
else's sheet.

Two things follow from this. Whatever you set up before signing in is adopted by
the first account that signs in, so early configuration is never lost. And every
account that will use the app has to be added to the **test users** list in Stage
2d — otherwise Google blocks sign-in for it.

---

## When something goes wrong

**"Google rejected this app's OAuth setup (error 10)"**
The fingerprint or package name in the OAuth client does not match this build.
Re-run the `keytool` command and compare it character by character with what is in
the console. This is by far the most common failure, and it is always a typo or a
fingerprint from a different keystore.

**"The Google Sheets API is not enabled for this project yet"**
Stage 2c. It can also take a couple of minutes to take effect after enabling.

**Sign-in works but nothing appears in the sheet**
Check the gear screen — the account card shows the last sync time, how many lifts
are queued, and the last error verbatim.

**"Access blocked: Pawgress has not completed the Google verification process"**
Your account is not in the test users list. Stage 2d, step 4.

**Everything logs fine but never syncs**
Expected when you are signed out or offline. Entries queue on the phone and go up
on the next successful sync; nothing is lost.

**Anything else**
Send me the exact error text, plus the Build panel output if it is a build failure.
