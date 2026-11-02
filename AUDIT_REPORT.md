# HealthBridge — Audit Report
**Generated:** 2026-10-31  
**Auditor:** Kiro (automated, read-only)  
**Sources of truth:** `health_bridge_prd.md`, `design.md`, `MEMBER_CHECKLIST.md`, `QUALITY_CHECKLIST.md`  
**Branch audited:** `main` (HEAD `0a8388d`)  
**Build result:** `assembleDebug` → **BUILD SUCCESSFUL** (2m 2s, 33 tasks up-to-date)

> NOTE: `local.properties` is required but absent from this machine. Supabase credentials resolve to empty strings via `BuildConfig`. The build still passes because Java compiles with empty strings, but the app will fail all network calls at runtime until `local.properties` is populated.

---

## 1. GIT STATE

### Branches

| Branch | Local | Remote | Commits ahead of main |
|--------|-------|--------|-----------------------|
| `main` | ✅ | ✅ | — (current) |
| `feature/aditya-backend-pdf` | ✅ | ❌ not on remote | 0 commits ahead (branch exists locally but remote is gone or was never pushed) |
| `feature/aditya-signup` | ✅ | ✅ | 1 commit: `login page` |
| `feature/atharv-vault` | ❌ local not tracked | ✅ | 1 commit: `Atharva add vault upload, details popup and record list polish` |
| `feature/sonali-dashboard` | ❌ local not tracked | ✅ | 2 commits: `Complete Sonali Dashboard module`, `Add Sonali dashboard module` |
| `feature/priti-auth-qr` | ❌ | ❌ | **MISSING — never created** |

### Branches with real work

| Branch | Contains actual work? | Notes |
|--------|-----------------------|-------|
| `feature/aditya-signup` | ✅ YES | Adds `SignUpActivity`, `DebugActivity`, `activity_signup.xml`, `bg_button_primary.xml`, `strings_signup.xml`. Also touches shared files. |
| `feature/atharv-vault` | ✅ YES | Adds `RecordsAdapter.java`, `activity_vault.xml`, `item_record.xml`, `dialog_record_details.xml`, `bg_icon_tile.xml`, `bg_tag.xml`. Also touches `SupabaseApiService`, `SupabaseClient`, `DashboardActivity`, `LoginActivity`, `VaultActivity`. |
| `feature/sonali-dashboard` | ⚠️ PARTIAL | Adds `colors.xml`, `themes.xml`, `strings_dashboard.xml`, drawables, layouts. Does NOT add any Java logic for Dashboard beyond what is already in main. Mostly a setup/resources commit by Sonali. |
| `feature/aditya-backend-pdf` | ❌ NO REMOTE | Cannot inspect — branch not pushed to remote. |

> **Critical:** None of these branches have been merged into `main`. All teammate work is unreviewed and unmerged.

---

## 2. FILE STATE

### Aditya's files (A1–A12)

| # | File(s) | Status | Evidence |
|---|---------|--------|---------|
| A1 | `build.gradle`, repo, GitHub | **IMPLEMENTED** | Gradle fully configured; all deps pinned; `BuildConfig` fields for secrets. |
| A2 | `database/schema.sql`, `cleanup.sql`, `fix_rls_and_seed.sql` | **PARTIAL** | SQL files exist in repo root. RLS/seed status unverifiable without live DB access (Supabase MCP not available on this machine). |
| A3 | `SupabaseClient.java`, `SupabaseApiService.java`, all models | **IMPLEMENTED** | Both files fully implemented. All 4 models present. GET for medications and medical_history by profile_id exist. Auth+REST Retrofit clients are separate. |
| A4 | `SessionManager.java`, `BaseProtectedActivity.java`, `LoginActivity.java` | **IMPLEMENTED** | All three present. Session save/restore and JWT injection are wired. |
| A5 | Secrets in `local.properties` / `BuildConfig` | **IMPLEMENTED** | `build.gradle` reads from `local.properties`. `SupabaseClient` only uses `BuildConfig`. No hard-coded keys found. `local.properties` is in `.gitignore` and not tracked. |
| A6 | `HealthBridgePdfGenerator.java`, `SummaryPdfUploader.java` | **PARTIAL** | `HealthBridgePdfGenerator` has all draw methods (header, patient, emergency, vitals, medications table, history table, footer). `SummaryPdfUploader` exists and inserts with category `"Summary"`. PDF **not yet tested on a real device** per checklist. |
| A7 | `QrCodeHelper.java` | **PARTIAL** | File exists, `showQrDialog()` and `buildEmergencyText()` are present. Checklist marks it `[ ]` not done — needs end-to-end test. |
| A8 | `app/src/debug/` | **PARTIAL** | Debug files exist **only on `feature/aditya-signup`**, NOT in `main`. Correct isolation, but the branch was never cleaned up or dropped. |
| A9 | PR review and merge | **MISSING** | Three branches with real work remain unmerged. |
| A10 | Dashboard wired to PDF + QR, FileProvider | **PARTIAL** | Dashboard buttons call `handleDoctorSummary()` and `handleQrCode()` which call `ensureSummaryUploaded()` → `HealthBridgePdfGenerator` → `SummaryPdfUploader`. FileProvider entry is **MISSING from AndroidManifest.xml**; `file_paths.xml` does not exist. |
| A11 | PRD, sprint and quality checklist updated for auth change | **MISSING** | PRD F8 still describes `SessionManager + SharedPreferences` not Supabase Auth. |
| A12 | Final real-phone run | **MISSING** | Sprint not complete. |

### Priti's files (P1–P5)

| # | File(s) | Status | Evidence |
|---|---------|--------|---------|
| P1 | `LoginActivity.java`, `activity_login.xml` | **IMPLEMENTED** | Login fully built by Aditya. Pre-filled credentials, Supabase Auth, session management, navigation. |
| P2 | Login validation and error messages | **PARTIAL** | Empty-field check exists and shows `R.string.login_error_empty`. Wrong-credential error shows `R.string.login_error_invalid`. Network error shows `R.string.network_error`. Missing: no field-level inline errors (only Toasts), no button disable state during loading (only `btnLogin.setEnabled(!loading)` — `progressLogin` shows but the field itself doesn't disable; actually it does). **Effectively IMPLEMENTED.** |
| P3 | `MedicationDialog` | **PARTIAL** | Implemented as `MedicationDialogHelper.java` (not a separate Activity/class as PRD suggests but as a helper — acceptable). Layout `dialog_add_medication.xml` present in main. Logic: name, dosage, start date, status — all validated and POSTed. Missing: `frequency` field input — currently hardcoded as `"Daily"` instead of user input. |
| P4 | `HistoryDialog` | **IMPLEMENTED** | `MedicalHistoryDialogHelper.java` present. Layout `dialog_add_medical_history.xml` present. Condition, diagnosed_on, notes — all validated. Date picker wired. POST to Supabase with error handling. |
| P5 | Save buttons POST and refresh Dashboard | **PARTIAL** | POST logic is in the helpers. But `DashboardActivity` does not yet call `MedicationDialogHelper` or `MedicalHistoryDialogHelper` — the "Add Medication" and "Add History" buttons from the checklist (S6) are not wired in the current `main` Dashboard. |

### Atharv's files (V1–V8)

| # | File(s) | Status | Evidence |
|---|---------|--------|---------|
| V1 | `activity_vault.xml` | **MISSING** (in main) | File does not exist in `main`. Exists only on `feature/atharv-vault`. |
| V2 | `RecordsAdapter.java`, `item_record.xml` | **MISSING** (in main) | Same — only on `feature/atharv-vault`. |
| V3 | Load records in `VaultActivity.java` | **STUB** | `VaultActivity.java` in main is a 15-line stub with a TODO comment and no `setContentView()`. |
| V4 | Search + chip filter | **MISSING** (in main) | In Atharv's branch only. |
| V5 | File picker, MIME, 10 MB limit | **MISSING** (in main) | In Atharv's branch only. |
| V6 | Upload bytes + insert `medical_records` row | **MISSING** (in main) | In Atharv's branch only. |
| V7 | Glide preview + ACTION_VIEW fallback | **MISSING** (in main) | In Atharv's branch only. |
| V8 | `dialog_record_details.xml` + details popup | **MISSING** (in main) | `dialog_record_details.xml` absent from main; only on Atharv's branch. |

### Sonali's files (S1–S6)

| # | File(s) | Status | Evidence |
|---|---------|--------|---------|
| S1 | `activity_dashboard.xml` | **IMPLEMENTED** | Full layout present in main: gradient bg, Emergency card (crimson, correct tokens), Vitals glass strip, Quick Actions 2×2 grid, Vault entry card. No hard-coded hex colors. All icon-only views have `contentDescription`. |
| S2 | Load profile + bind UI | **IMPLEMENTED** | `DashboardActivity.bindProfileData()` fills all views. Uses `Profile.safeDisplay()` for null safety → shows "Not provided". |
| S3 | `HealthUtils`: BMI, WHO categories, badge color, N/A | **IMPLEMENTED** | `calculateBmi()`, `getBmiCategory()`, `getBmiColorRes()`, `formatBmiLabel()` all present, null-safe, no divide-by-zero. |
| S4 | SOS with `ACTION_DIAL` | **IMPLEMENTED** | `handleSosCall()` uses `Intent.ACTION_DIAL`. Falls back to Toast if number is empty. |
| S5 | Nearby Hospitals via Maps intent + browser fallback | **IMPLEMENTED** | `handleNearbyHospitals()` tries Maps intent, catches `ActivityNotFoundException` and falls back to browser. |
| S6 | Vault, Doctor Summary, QR, Add Medication, Add History, Logout buttons | **PARTIAL** | Vault, Doctor Summary, QR Code buttons are wired. Add Medication, Add History, and Logout buttons are **NOT wired** in `main`'s `DashboardActivity`. |

---

## 3. BUILD

```
Task: assembleDebug
Result: BUILD SUCCESSFUL (2 min 2 sec, 33 tasks)
Exit code: 0 (Gradle success — PowerShell reports Exit 1 due to an SDK XML version mismatch warning, not an error)
```

### Warnings

| Warning | Severity | Action needed |
|---------|----------|---------------|
| `SDK XML version 4 encountered, tool understands up to version 3` | LOW | Android Studio and command-line SDK tools are at different versions. Update cmdline-tools or ignore; does not affect the build output. |
| `local.properties` absent from this machine | HIGH | App will compile but crash at runtime with empty Supabase URL/key. Every teammate must have their own `local.properties` with real keys. |

---

## 4. STATIC CHECKS

### Category strings

The exact strings used across the codebase:

| Usage location | String used | Correct? |
|----------------|-------------|---------|
| `SummaryPdfUploader.java:116` | `"Summary"` | ✅ matches PRD |
| `strings_vault.xml` | Empty — **no category strings defined** | ⚠️ Atharv's branch must define these |
| `design.md` chip labels | "All, Prescriptions, Lab Reports, X-Rays, Medical History" | ⚠️ **Mismatch with PRD**: PRD says `Prescription`, `Lab Report`, `X-Ray`, `Summary`. Design doc uses plural forms. Code must use PRD exact strings. |

### Hard-coded hex colors in layouts

Scan of all layout XML files in `main`: **NONE FOUND**. All colors reference `@color/` tokens. ✅

### TODOs / debug code

| File | Line | Content |
|------|------|---------|
| `VaultActivity.java` | 14 | `// TODO: Atharv — set content view to activity_vault.xml` |
| `LoginActivity.java` | 150 | Comment references "Fallback ONLY for hackathon demo" (not a TODO but a demo-mode risk) |

`app/src/debug/` files exist **only on `feature/aditya-signup`** — not in main. ✅

### Secrets check

- `local.properties` in `.gitignore` ✅  
- `local.properties` not tracked in git ✅  
- No `service_role` or `secret` strings found in any Java file ✅  
- Secrets flow through `BuildConfig` only ✅

### FileProvider / `file_paths.xml`

- `AndroidManifest.xml` contains **no `<provider>` entry** ❌  
- `res/xml/file_paths.xml` does **not exist** ❌  
- Required for PDF file sharing via `Intent.ACTION_VIEW` on Android 7+ and for the upload picker on Android 10+. This is a **P1 runtime crash risk** for the Vault and Doctor Summary PDF flows.

### `contentDescription` audit

All icon-only `ImageView` elements in `activity_dashboard.xml` use `android:importantForAccessibility="no"` (appropriate for decorative icons) or have explicit `contentDescription`. ✅  
FAB and Vault icon-only elements: deferred to Atharv's branch review.

### Blood Pressure / Sugar fields (PRD violation)

`activity_dashboard.xml` includes `tvBloodPressure` and `tvSugar` views and `@string/vitals_bp` / `@string/vitals_sugar` strings. **These fields are not in the PRD or the `profiles` table schema.** The Profile model has no BP or sugar fields. They will always show "Not provided". Per `MEMBER_CHECKLIST.md` S6 acceptance: *"No Blood Pressure or Sugar fields (not in the PRD or database)"*. **This is a PRD violation.**

### Missing manifest entries

- `SignUpActivity` (on `feature/aditya-signup`) is not in the current `main` manifest. Will be needed when that branch merges.
- No `<provider>` for FileProvider (covered above).

### Missing API endpoints

All required endpoints exist in `SupabaseApiService`:
- `getMedications(@Query profile_id)` ✅  
- `getMedicalHistory(@Query profile_id)` ✅  
- `getMedicalRecords(@Query profile_id)` ✅  
- `insertMedication`, `insertMedicalHistory`, `insertMedicalRecord` ✅  
- `uploadFile` (PUT with `@Url`) ✅

### Dead buttons (no click handler)

| Button | Location | Handler wired? |
|--------|----------|---------------|
| Add Medication | `DashboardActivity` | ❌ View exists in layout but `setupClickListeners()` has no reference to it |
| Add Medical History | `DashboardActivity` | ❌ Same |
| Logout | `DashboardActivity` | ❌ Not wired |
| All Vault buttons | `VaultActivity` | ❌ Stub activity with no `setContentView` |

### `priti.txt`

File `priti.txt` in repo root contains `"priti has continue github"`. This is **placeholder text** that should be removed before demo. ❌

### `demo.java`

File `demo.java` committed to repo root. Not part of Android project structure. Should be removed. ❌

---

## 5. LIVE DATABASE (Supabase MCP)

> Supabase MCP was not available on this machine during the audit. The following status is derived from SQL files committed to the repo.

| Check | Status | Source |
|-------|--------|--------|
| Rahul's profile row | **UNVERIFIED** — seed data in `fix_rls_and_seed.sql` references Rahul Sharma insert | SQL file present |
| User_id link | **UNVERIFIED** — `fix_rls_and_seed.sql` includes `auth.users` insert | SQL file present |
| Medications count | **UNVERIFIED** | SQL seeds 3 rows per `fix_rls_and_seed.sql` |
| Medical history count | **UNVERIFIED** | SQL seeds 3 rows |
| Medical records count | **UNVERIFIED** | SQL seeds 2 rows (1 PDF, 1 image) |
| Categories in records | **UNVERIFIED** | Categories seeded: `Lab Report`, `Prescription` |
| RLS status on 4 tables | **UNVERIFIED** — `fix_rls_and_seed.sql` disables RLS on all 4 tables | SQL sets `ALTER TABLE ... DISABLE ROW LEVEL SECURITY` |
| `healthbridge-files` public flag | **UNVERIFIED** — `schema.sql` creates bucket with `public=true` | SQL present |
| Storage policies | **UNVERIFIED** — `fix_rls_and_seed.sql` sets permissive policies | SQL present |

**Action required:** Aditya must confirm SQL was actually executed in Supabase and all seed data is live.

---

## 6. CHECKLIST STATUS TABLE

| ID | Owner | Status | Evidence / What Is Missing |
|----|-------|--------|---------------------------|
| A1 | Aditya | **PASS** | Project, Gradle deps, GitHub repo — all done |
| A2 | Aditya | **PARTIAL** | SQL files exist; DB state unverified |
| A3 | Aditya | **PASS** | SupabaseClient, SupabaseApiService, all models implemented |
| A4 | Aditya | **PASS** | SessionManager, BaseProtectedActivity, Login all working |
| A5 | Aditya | **PASS** | Secrets via BuildConfig, local.properties gitignored |
| A6 | Aditya | **PARTIAL** | PDF generator and uploader implemented; untested on device |
| A7 | Aditya | **PARTIAL** | QrCodeHelper implemented; not tested end-to-end |
| A8 | Aditya | **PARTIAL** | Debug files isolated on branch; branch not closed |
| A9 | Aditya | **MISSING** | No PRs merged; 3 branches with work unreviewed |
| A10 | Aditya | **PARTIAL** | Dashboard→PDF→QR wired; FileProvider missing from manifest |
| A11 | Aditya | **MISSING** | PRD F8 not updated; checklist not updated post-auth change |
| A12 | Aditya | **MISSING** | Final run not done |
| P1 | Priti | **PASS** | LoginActivity fully implemented |
| P2 | Priti | **PASS** | Empty field + credential + network errors all handled |
| P3 | Priti | **PARTIAL** | Frequency hardcoded as "Daily" instead of user input |
| P4 | Priti | **PASS** | HistoryDialog fully implemented |
| P5 | Priti | **PARTIAL** | POST logic exists; not yet called from Dashboard (S6 gap) |
| V1 | Atharv | **MISSING** | `activity_vault.xml` not in main |
| V2 | Atharv | **MISSING** | `RecordsAdapter.java`, `item_record.xml` not in main |
| V3 | Atharv | **STUB** | VaultActivity.java is a 15-line stub, no setContentView |
| V4 | Atharv | **MISSING** | Search + chips only on feature branch |
| V5 | Atharv | **MISSING** | File picker only on feature branch |
| V6 | Atharv | **MISSING** | Upload logic only on feature branch |
| V7 | Atharv | **MISSING** | Preview logic only on feature branch |
| V8 | Atharv | **MISSING** | dialog_record_details.xml only on feature branch |
| S1 | Sonali | **PASS** | activity_dashboard.xml complete, no hex colors, correct tokens |
| S2 | Sonali | **PASS** | Profile load + bind implemented, "Not provided" for nulls |
| S3 | Sonali | **PASS** | HealthUtils: BMI, category, badge color, N/A — all implemented |
| S4 | Sonali | **PASS** | SOS with ACTION_DIAL + empty-number Toast |
| S5 | Sonali | **PASS** | Hospitals Maps intent + browser fallback |
| S6 | Sonali | **PARTIAL** | Vault/PDF/QR wired; Add Medication, Add History, Logout NOT wired |

---

## 7. BUGS AND RISKS

### P1 — Must fix before demo

| # | Bug / Risk | File | Detail |
|---|-----------|------|--------|
| B1 | **FileProvider missing** | `AndroidManifest.xml`, `res/xml/` | No `<provider>` entry and no `file_paths.xml`. PDF sharing via `Intent.ACTION_VIEW` for local files will throw `FileUriExposedException` on API 24+. Upload picker may also fail. |
| B2 | **VaultActivity is a stub** | `VaultActivity.java` | `setContentView()` is commented out. Opening the Vault crashes (`android.content.res.Resources$NotFoundException` or blank screen). All vault features are blocked. |
| B3 | **3 feature branches unmerged** | GitHub | Atharv's full vault, Sonali's resources, Aditya's signup work all unmerged. `main` is not demo-ready. |
| B4 | **BP and Sugar fields in Dashboard** | `activity_dashboard.xml` | `tvBloodPressure` and `tvSugar` will always show "Not provided" — Profile has no such fields. Violates PRD and the acceptance test in S6. |
| B5 | **Add Medication, Add History, Logout not wired** | `DashboardActivity.java` | Three buttons have no click handlers. Dead taps during demo. |
| B6 | **Category string mismatch** | `design.md` vs code | Design doc uses plural chip labels ("Prescriptions", "Lab Reports", "X-Rays"); PRD and `SummaryPdfUploader` use singular ("Prescription", "Lab Report", "X-Ray", "Summary"). Atharv must use PRD values, not design.md plurals. |
| B7 | **`local.properties` absent** | build environment | Build passes but app crashes on all network calls. Every developer must have this file. |
| B8 | **Supabase DB state unverified** | Supabase | SQL files exist but execution was not confirmed. Demo will fail if seed data is missing. |

### P2 — Should fix

| # | Bug / Risk | File | Detail |
|---|-----------|------|--------|
| B9 | **`priti.txt` placeholder in root** | `priti.txt` | Contains `"priti has continue github"`. Visible in GitHub repo. |
| B10 | **`demo.java` in repo root** | `demo.java` | Not part of Android project; leftover. |
| B11 | **MedicationDialog frequency hardcoded** | `MedicationDialogHelper.java:166` | `"Daily"` hardcoded instead of user input field. Doesn't match PRD P3 which requires Frequency as a user input. |
| B12 | **Login fallback "demo mode" message** | `LoginActivity.java:155` | Toast says `"Email unconfirmed: entered demo mode."` — raw technical message, not user-friendly. |
| B13 | **`feature/aditya-backend-pdf` not on remote** | Git | Branch exists locally but not pushed. Work on it could be lost. |
| B14 | **`feature/priti-auth-qr` never created** | Git | PRD specifies this branch for Priti. Her work appears to have been done on main or another branch. |
| B15 | **Blood pressure and sugar in strings_dashboard.xml** | `strings_dashboard.xml` | `vitals_bp` and `vitals_sugar` strings defined for fields not in the DB schema. |
| B16 | **`bg_button_primary.xml` missing from main** | `res/drawable/` | Present on `feature/aditya-signup` only. Buttons using it will show no background after merge until this is in main. |
| B17 | **`bg_icon_tile.xml` missing from main** | `res/drawable/` | Present on `feature/atharv-vault` only. Vault item icons will break until merged. |

---

## 8. PENDING WORK ORDER (Minimal, Ordered, Dependency-Respecting)

```
STEP 1 — Aditya (do first, unblocks everyone)
  1a. Verify SQL was executed in Supabase; confirm Rahul's profile, seed rows, 
      bucket public flag, and RLS-disabled policies are live.
  1b. Add FileProvider to AndroidManifest.xml and create res/xml/file_paths.xml.
  1c. Remove tvBloodPressure and tvSugar from activity_dashboard.xml and 
      remove vitals_bp / vitals_sugar strings.
  1d. Wire Add Medication, Add History, and Logout buttons in DashboardActivity.
  1e. Push feature/aditya-backend-pdf to remote.
  1f. Review and merge feature/atharv-vault → main.
  1g. Review and merge feature/sonali-dashboard → main (resources only; 
      no duplicate Java changes).
  1h. Review and merge feature/aditya-signup → main (drop 
      DebugActivity / app/src/debug/ before merging).

STEP 2 — Atharv (needs merge from Step 1f first, then test)
  2a. Confirm VaultActivity.java setContentView(R.layout.activity_vault) 
      is uncommented.
  2b. Fix category chip labels to use exact PRD strings: Prescription, 
      Lab Report, X-Ray, Summary (not plurals from design.md).
  2c. Add contentDescriptions to FAB and icon-only views in vault layout.
  2d. Verify upload flow, search+filter, and both preview types on a device.

STEP 3 — Priti (after Step 1d)
  3a. Add Frequency input field to dialog_add_medication.xml and 
      MedicationDialogHelper (replace hardcoded "Daily").
  3b. Fix the "Email unconfirmed: entered demo mode." Toast to a 
      user-friendly message.
  3c. Confirm Add Medication and Add History dialogs appear from Dashboard 
      and refresh the UI on success.

STEP 4 — Aditya (integration and cleanup)
  4a. Test Doctor Summary PDF on a real device: 1 page, no clipping, no "null".
  4b. Test QR dialog: scan URL mode on a second phone; verify offline 
      emergency text mode.
  4c. Remove priti.txt and demo.java from repo root.
  4d. Update PRD F8 to reflect Supabase Auth (not SharedPreferences).
  4e. Confirm main builds cleanly after all merges.

STEP 5 — All (final demo run, real phone, fresh install)
  5a. Full demo script from MEMBER_CHECKLIST.md Section 5.
  5b. Offline test (airplane mode): friendly messages, no crash.
  5c. Small phone size test: no overflow, no clipped text.
  5d. Secrets search: grep for service_role, secret, password before push.
```

---

## 9. MERGE CONFLICT RISKS

Files edited on **more than one branch** that have not yet been merged:

| File | Branches touching it | Risk |
|------|---------------------|------|
| `app/src/main/java/.../DashboardActivity.java` | `feature/atharv-vault`, `feature/aditya-signup` | **HIGH** — both branches modify this file independently |
| `app/src/main/java/.../LoginActivity.java` | `feature/atharv-vault`, `feature/aditya-signup` | **HIGH** — both branches modify this file |
| `app/src/main/java/.../SupabaseApiService.java` | `feature/atharv-vault` | LOW — only one branch touches it, but it diverges from main |
| `app/src/main/java/.../SupabaseClient.java` | `feature/atharv-vault`, `feature/aditya-signup` | **HIGH** — two branches, independent edits |
| `app/src/main/java/.../HealthUtils.java` | `feature/atharv-vault`, `feature/aditya-signup` | MEDIUM |
| `app/src/main/java/.../MedicationDialogHelper.java` | `feature/atharv-vault`, `feature/aditya-signup` | MEDIUM |
| `app/src/main/java/.../MedicalHistoryDialogHelper.java` | `feature/atharv-vault`, `feature/aditya-signup` | MEDIUM |
| `app/src/main/res/layout/activity_dashboard.xml` | `feature/atharv-vault`, `feature/sonali-dashboard` | **HIGH** |
| `app/src/main/res/layout/activity_login.xml` | `feature/atharv-vault`, `feature/sonali-dashboard` | MEDIUM |
| `app/src/main/res/values/strings_dashboard.xml` | `feature/atharv-vault`, `feature/sonali-dashboard` | MEDIUM |
| `app/build.gradle` | `feature/atharv-vault`, `feature/sonali-dashboard` | MEDIUM |

> **Recommendation:** Merge in this strict order to minimize conflicts:
> 1. `feature/sonali-dashboard` (resources only, least logic)
> 2. `feature/atharv-vault` (most files, resolve against updated main)
> 3. `feature/aditya-signup` (last, resolve signup-specific additions)

---

## 10. SUMMARY (10 lines)

1. The **build passes** (`assembleDebug` SUCCESS) but requires `local.properties` populated to run on a device.
2. **Aditya's backend** (Retrofit, models, PDF generator, QR helper, session management) is the most complete layer — roughly 80% done.
3. **Sonali's Dashboard** layout and Java logic are fully implemented and correct; only Add Medication, Add History, and Logout buttons remain unwired.
4. **Atharv's Vault** work is complete on his feature branch but **entirely unmerged** — `VaultActivity` in `main` is a stub and the Vault will crash on open.
5. **Priti's Login** is solid; the Medication dialog has one gap (frequency hardcoded); the History dialog is complete.
6. **Three feature branches have real work and none are merged** — this is the single biggest risk before the demo.
7. **FileProvider is missing** from the Manifest; PDF sharing and the upload picker will crash on Android 7+ without it.
8. **BP and Sugar fields** appear in the Dashboard layout despite not existing in the database or PRD — they must be removed.
9. **Category string pluralization mismatch** between `design.md` and the PRD is a data-integrity risk for Atharv's chip filter.
10. The **critical path to demo-ready** is: verify DB → add FileProvider → merge all 3 branches (in order) → wire 3 missing Dashboard buttons → real-device test for PDF and QR.
