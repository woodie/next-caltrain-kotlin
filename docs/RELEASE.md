# Release Runbook — Next Caltrain (Android)

## When to use this

Any time you're publishing a build to the Google Play Store: the very first
submission, or a routine update to an already-live listing.

## Prerequisites and access needed

- **Play Console developer account.** One-time $25 USD fee, a Google payments
  profile (identity verification), and 2-Step Verification enabled on the
  Google account you register with. Sign up at
  https://play.google.com/console/signup. Verification can take a few days.
- **Release keystore + passwords**, in `local.properties` only
  (`RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`,
  `RELEASE_KEY_PASSWORD`). See `docs/COWORK.md` → "Release signing". **If this
  keystore is lost, you can never update this app listing again** — back it up
  somewhere durable outside the repo.
- **12+ people willing to install a test build for 14 consecutive days.**
  First-submission-only Google requirement (see Step 5 below) — line these up
  before you need them, since the clock only starts once they're actively
  testing.
- **Store listing copy** — already drafted in `docs/PLAY_STORE_LISTING.md`
  (name, descriptions, category, content rating expectations, data safety
  answers).
- **Privacy policy** — already live: https://next-caltrain-pwa.appspot.com/privacy.html
- **Screenshots** — `pics/1920_*.png` (3 ready, only 2 required). See
  `docs/COWORK_ADDITIONS.md` "Play Store" section for how these were produced.
- **App icon (512×512 PNG) and feature graphic (1024×500 PNG)** — not yet
  created as of this writing. Needed before the store listing can go live.

## Restoring the keystore on a new machine

`local.properties` is per-machine and gitignored, so a fresh checkout (or a
new dev machine) starts with none of the four release-signing properties
set -- `sdk.dir` may already be there from Android Studio's own setup, but
`RELEASE_STORE_FILE`/`RELEASE_STORE_PASSWORD`/`RELEASE_KEY_ALIAS`/
`RELEASE_KEY_PASSWORD` need to be added by hand from backup:

1. Put the actual `.jks`/`.keystore` file somewhere permanent and *outside*
   any git repo -- e.g. `~/.android-keystores/next-caltrain-release.jks` --
   so it can never end up committed even if `.gitignore` were ever wrong.
2. Add the four lines below to `local.properties` yourself, typed directly
   (don't paste real passwords into a chat/AI session -- an assistant
   helping with this step should tell you where things go, not type your
   secrets in for you):
   ```
   RELEASE_STORE_FILE=/absolute/path/to/next-caltrain-release.jks
   RELEASE_STORE_PASSWORD=<your real password>
   RELEASE_KEY_ALIAS=<your real alias>
   RELEASE_KEY_PASSWORD=<your real password>
   ```
3. Verify: `./gradlew bundleRelease` should succeed and produce
   `app/build/outputs/bundle/release/app-release.aab`. `hasReleaseSigning`
   in `app/build.gradle.kts` gates on all four properties being present --
   if any are missing or wrong, the build still compiles but silently falls
   back to an unsigned/debug-signed `release` build type instead of failing
   loudly, so don't just trust a green build; confirm the signature too:
   ```
   ~/Library/Android/sdk/build-tools/<version>/apksigner verify --print-certs \
     app/build/outputs/bundle/release/app-release.aab
   ```

See `docs/COWORK.md` → "Release signing" for how to generate a *new*
keystore from scratch (only relevant if the backup is genuinely gone, since
losing this file means the existing Play Store listing can never be updated
again under any account).

## One-time account & app setup (skip once done)

**Landmine**: don't assume an old developer account is still usable just
because you remember creating one. Google auto-closes accounts for
inactivity — either (a) created over a year ago with no app ever submitted
for review, or (b) all published apps under 1,000 combined lifetime installs
*and* contact info unverified *and* no Play Console activity in 180 days.
Closure **cannot be reversed**, and the $25 registration fee is **not
refunded**. We hit this directly: an existing account showed "Account
closed... due to inactivity, and can't be reactivated" with "more information
sent to the account owner's email address" — check that email for
account-specific next steps before doing anything else. The fix is simply to
register a new account (new $25 fee); the closure email confirmed creating a
new account is fine ("You can create a new account if you do decide to
publish apps on Google Play").

**Sub-landmine — developer name reuse**: developer names are globally unique
across all of Play Console, not per-account. If the old account hasn't
actually finished deleting yet (the email only says "shortly," no exact
timing), trying to register the *same* developer name on the new account may
still get rejected as taken. Developer name can be changed anytime after
registration though, so the fix is: register with a close variant now to get
unblocked, then rename to the preferred name later once the old account is
confirmed gone.

1. Create the Play Console developer account (link above). Choose **Personal**
   account type (not Organization — that needs a D-U-N-S number and can take
   up to 30 days). Have ready:
   - Developer name (display name on the listing — can be anything, doesn't
     need to match your legal name)
   - Legal name + legal address (taken from the linked Google payments
     profile; used for identity verification)
   - A contact email + contact phone number (private — Google uses these to
     reach you, not shown to users)
   - A developer email address (shown publicly on the Play Store listing)
   - A government-issued photo ID, for identity verification
   - A credit/debit card for the one-time $25 fee
   - **Heads up**: once verified, Google displays your **legal name and
     country** publicly on the Play Store listing — there's no way to keep
     that private on a personal account. Verification itself takes a few
     hours up to 2 business days.
   - **Hard gate**: you cannot create an app entry (Step 2 below) until
     identity verification finishes — it's not just a pre-publish check, it
     blocks Play Console from letting you start. Nothing to do here but
     wait; there's no skip-ahead path.
2. Play Console → **Create app**. Name: "Next Caltrain" (from
   `docs/PLAY_STORE_LISTING.md`). App, Free, default language English (US).
3. Work through the **"Set up your app" / App content checklist**:
   - Privacy policy URL (above).
   - Ads declaration: **No ads**.
   - Data safety form: **No data collected** — per the notes in
     `docs/PLAY_STORE_LISTING.md` (no accounts, analytics, ads, or tracking;
     the only network call is an anonymous fetch of the public
     `schedule.json`, identical for every user).
   - Content rating questionnaire: answer per actual app behavior; given no
     data collection and no objectionable content, this should land on
     **Everyone**, matching the listing doc.
   - Target audience: general audience, **not** "designed for children" (Play
     has stricter rules for that path and this app doesn't need it).
   - Government app / COVID-19 contact tracing: No.
4. **Main store listing**: paste in app name, short description, full
   description from `docs/PLAY_STORE_LISTING.md`. Category: Travel & Local
   (Play Console's current UI may only allow one category — if so, drop the
   "Maps & Navigation" secondary). Upload icon, feature graphic, and the
   screenshots from `pics/`.

## Step-by-step release procedure (every release)

1. **Bump the version** in `app/build.gradle.kts` (skip for the very first
   release, which already starts at `versionCode = 1` / `versionName = "1.0"`):
   ```kotlin
   versionCode = <previous + 1>   // must strictly increase, every release
   versionName = "<human-readable, e.g. 1.1>"
   ```
2. **Build the App Bundle — not the APK.** Play Store requires `.aab`:
   ```bash
   ./gradlew bundleRelease
   ```
   Output: `app/build/outputs/bundle/release/app-release.aab`.

   Note: `./gradlew assembleRelease` (what was run previously) builds an
   `.apk` at `app/build/outputs/apk/release/app-release.apk`. That's fine for
   sideloading or manual testing, but it is **not** what gets uploaded to Play
   Console — use `bundleRelease` for that.
3. **Sanity-check the signature** (optional, mirrors how the APK was
   verified):
   ```bash
   ~/Library/Android/sdk/build-tools/<version>/apksigner verify --print-certs \
     app/build/outputs/bundle/release/app-release.aab
   ```
   If your `apksigner` version doesn't support `.aab` directly, verify the
   sibling `app-release.apk` from `assembleRelease` instead — both come from
   the same `signingConfig`.
4. **First release only — closed testing gate.** New developer accounts can't
   publish to Production directly. Play Console → **Testing → Closed
   testing** → create a track → upload the `.aab` → add release notes → add
   at least 12 tester emails → publish to testers. Google requires those 12
   testers to have the app actively installed for **14 consecutive days**
   before the Production track unlocks. (Internal testing is available too,
   for your own quick sanity checks, but it doesn't count toward this
   requirement — only Closed testing does.)
5. **Subsequent releases** (or promoting out of closed testing once the gate
   clears): Play Console → the relevant track (e.g. Production) → **Create
   new release** → upload the `.aab` → release notes → review → roll out.
   Consider a staged rollout (e.g. start at 20%) rather than 100% immediately,
   so a bad release can be halted before everyone gets it.
6. **Submit for review.** Typically 1–3 days; can take longer for new
   accounts or after policy-sensitive changes.
7. Once approved, confirm the listing is live and install from the real Play
   Store on a device as a final smoke test.

## Rollback steps

- Play Store has no built-in "revert to previous binary" button. If a release
  is still rolling out (not yet at 100%), go to the track's **Manage
  releases** and **halt the rollout** — that's the fastest stop.
- If a bad release already reached 100% of users, the fix is a **new build
  with a higher `versionCode`**, not a revert. Staged rollouts (Step 5) exist
  specifically to make this less likely.
- Internal/closed testing tracks can be used to verify a hotfix before
  pushing it to Production.

## Escalation / known dead ends

- **Lost keystore** → there is no recovery path; you cannot push further
  updates to this listing under any account. This is the single highest-risk
  failure mode in this whole process — see `docs/COWORK.md` "Release
  signing" for where the backup should live.
- **Account suspension or policy strikes** → Play Console Help Center /
  appeal form inside Play Console itself; nothing in this repo can help with
  that.
- The iOS sibling app (`next-caltrain-swift`) releases to the App Store
  independently — not a blocker for Android releases.

## References

- `docs/COWORK.md` — release signing / keystore setup
- `docs/PLAY_STORE_LISTING.md` — listing copy and data safety answers
- `docs/COWORK_ADDITIONS.md` — screenshot workaround status, emulator notes
- `pics/1920_*.png` — current screenshots ready for upload
