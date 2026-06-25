# Git Workflow & Release Process

Mirrors the Swift sibling's `docs/GIT_WORKFLOW.md`, adapted for Gradle/Play
Store. If the two ever drift, treat that as a bug — keep them in sync.

## Branching strategy

This project uses a simplified Git Flow model:

- **`main`** — production-ready code, always deployable to Play Store. Every
  commit should pass `./test.sh` (also enforced by `.github/workflows/CI.yml`
  on every push/PR) and be ready for release.
- **Work branches** — all code changes (features, bug fixes, UI polish) happen on
  a branch named after the GitHub issue. Branch from `main`, merge back via
  pull request, then delete the branch.
- **Docs** — documentation updates (`docs/`) commit directly to `main`, no branch needed.

## Branch naming convention

Always tie a branch to a GitHub issue:

```
bugfix/8-reset-button-stuck-on
feature/29-append-tomorrow-trips
hotfix/33-fix-crash-on-launch
```

Pattern: `{type}/{issue-number}-{short-description}`

- `bugfix/` — fixes a reported bug (closes a GitHub issue)
- `feature/` — adds new functionality
- `hotfix/` — urgent fix for a production issue

## Feature / bugfix workflow

1. **Create a branch** from `main`, tied to the issue number:
   ```bash
   git checkout main && git pull
   git checkout -b bugfix/8-reset-button-stuck-on
   ```

2. **Make changes** — edit files, run `./gradlew installDebug` + launch on
   emulator/device to test interactively (see `docs/COWORK.md` "Install and
   run on emulator").

3. **Run tests** before committing:
   ```bash
   ./test.sh
   ```
   All tests must pass.

4. **Lint** to catch style issues:
   ```bash
   ktlint
   ```

5. **Commit** with a message that references the issue:
   ```bash
   git add app/src/main/java/com/netpress/nextcaltrain/HomeScreen.kt
   git commit -m "Fix reset button stuck on (fixes #8)"
   ```

6. **Push** and open a pull request on GitHub:
   ```bash
   git push -u origin bugfix/8-reset-button-stuck-on
   ```
   `.github/workflows/CI.yml` runs `./gradlew clean test` automatically on
   the PR.

7. **Merge** via GitHub PR, then clean up:
   ```bash
   git checkout main && git pull
   git branch -d bugfix/8-reset-button-stuck-on
   ```
   (Delete the remote branch via GitHub's "Delete branch" button after merging,
   or with `git push origin --delete bugfix/8-reset-button-stuck-on`.)

## Hotfix workflow (urgent production bugs)

Same as above but use `hotfix/` prefix and merge immediately without waiting
for extended review:

```bash
git checkout main && git pull
git checkout -b hotfix/33-fix-crash-on-launch
# ... fix, test, commit ...
git push -u origin hotfix/33-fix-crash-on-launch
# merge PR immediately, then:
git checkout main && git pull
git branch -d hotfix/33-fix-crash-on-launch
```

After merging a hotfix, **re-release to the Play Store** (see below).

## Testing a branch before merging

1. **Unit tests**:
   ```bash
   ./test.sh
   ```

2. **Emulator/device**:
   ```bash
   ./gradlew installDebug
   ~/Library/Android/sdk/platform-tools/adb shell am start -n com.netpress.nextcaltrain/.MainActivity
   ```

3. **Edge cases** — if the change touches schedule logic, routing, or time
   calculations, test with debug overrides (see `GoodTimes.kt`'s
   `debugOverrideMinutes`/`debugOverrideDotw`, same convention as the Swift
   sibling's `docs/ROLLOVER_NOTES.md`):
   - Test South County no-service behavior (Friday evening → Saturday).
   - Test schedule type cycling (weekday ↔ weekend ↔ holiday).

4. **Crash logs**:
   ```bash
   ~/Library/Android/sdk/platform-tools/adb logcat -d | grep -A 20 "FATAL EXCEPTION"
   ```

## Schedule updates (publish new `schedule.json`)

Same as the Swift sibling — the app fetches `schedule.json` from the network
at startup, so updating the published schedule does **not** require a Play
Store release. Follow `next-caltrain-pwa/docs/PUBLISHING.md`; don't duplicate
those steps here.

## Play Store release workflow

This is the git-side complement to `docs/RELEASE.md`'s full release runbook
(signing, closed-testing gate, Play Console steps) — see that doc for
everything past step 3 below.

1. **Merge all branches** to `main` and ensure `./test.sh` passes.

2. **Update version numbers** in `app/build.gradle.kts`:
   ```kotlin
   versionCode = <previous + 1>   // must strictly increase, every release
   versionName = "<human-readable, e.g. 1.1>"
   ```
   `versionName` is the tag-facing version (mirrors the Swift sibling's
   `CFBundleShortVersionString`); `versionCode` is the upload-facing build
   number (mirrors `CFBundleVersion`) — Play Console rejects an upload whose
   `versionCode` doesn't strictly increase, just like App Store Connect does
   for `CFBundleVersion`.

3. **Commit and tag**:
   ```bash
   git add -A
   git commit -m "Release 1.1"
   git tag -a v1.1 -m "Version 1.1 (versionCode 2) release"
   git push origin main --tags
   ```
   Tag name is always `v<versionName>` — no exceptions — so a tag maps 1:1 to
   what Play Console shows for that listing. Put the `versionCode` in the
   annotated message (not the tag name) since multiple uploads can share one
   `versionName` if a build is rejected and resubmitted without a
   user-facing version change.

4. **Build and upload** — see `docs/RELEASE.md` "Step-by-step release
   procedure" starting at `./gradlew bundleRelease`.

5. **Submit for review** in Play Console and monitor status.

## Regression testing checklist

Before releasing:

- [ ] `./test.sh` passes.
- [ ] `ktlint` has no errors.
- [ ] `./gradlew installDebug` works on emulator and a real device.
- [ ] Schedule loads correctly (check About screen).
- [ ] Countdown updates every second; weekday/weekend detection correct.
- [ ] South County Friday evening edge case verified.
- [ ] Dark mode and light mode both look correct.
- [ ] App works offline with a cached schedule.
