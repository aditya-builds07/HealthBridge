# Health Bridge: Member Working Checklist

**Use:** tick items only after testing them on a real phone. Update the **Status** column from the audit report (`AUDIT_REPORT.md`).
**Scope:** only features in `PRD.md`. Style from `design.md`. Quality rules from `QUALITY_CHECKLIST.md`.
**Status key:** `[x]` done and tested, `[~]` partly done or untested, `[ ]` not done.

> Known status at the time of writing comes from the build logs: Aditya's foundation is done. Everything else is **unverified** until the audit runs.

---

## Shared rules (everyone)

- [ ] Pull `main` before starting (`git pull origin main`)
- [ ] Edit only files you own (see each section)
- [ ] Copy `local.properties.example` to `local.properties` and paste the keys (never commit it)
- [ ] Category names are exactly: `Prescription`, `Lab Report`, `X-Ray`, `Summary`
- [ ] No hard-coded hex colors in layouts, use tokens from `colors.xml`
- [ ] Every network call is async with a friendly error message
- [ ] Small PRs, one feature each, to `main`
- [ ] `git status` clean of `local.properties`, keys and debug code before every push

---

## 1. Aditya (Tech Lead, Backend, PDF, QR, Integration)

| # | Task | Files | Status |
|---|------|-------|--------|
| A1 | Android project, Gradle dependencies, GitHub repo | `build.gradle`, repo | [x] |
| A2 | Supabase tables, Auth link, seed data, storage bucket and policies | `database/schema.sql`, `cleanup.sql` | [~] SQL must be run and verified |
| A3 | `SupabaseClient`, `SupabaseApiService`, models | `api/`, `models/` | [x] |
| A4 | `SessionManager`, `BaseProtectedActivity`, login with Supabase Auth | `utils/`, `ui/` | [x] (login failure still being debugged) |
| A5 | Secrets moved to `local.properties` / `BuildConfig` | `build.gradle`, `SupabaseClient` | [x] |
| A6 | `HealthBridgePdfGenerator` and `SummaryPdfUploader` | `utils/` | [~] built, PDF not yet viewed |
| A7 | `QrCodeHelper` (QR dialog + emergency text) | `utils/QrCodeHelper.java` | [~] verified in code/build, needs phone test |
| A8 | Debug screen kept on its own branch, never merged | `app/src/debug/` | [~] |
| A9 | Review and merge all PRs, resolve conflicts | GitHub | [x] merged into feature/finish-all |
| A10 | Wire Dashboard buttons to PDF and QR, FileProvider | integration | [x] |
| A11 | Update `PRD.md` F8, sprint file and quality checklist for the auth change | docs | [ ] |
| A12 | Final real-phone run and release of demo build | all | [ ] |

**Acceptance tests**
- [ ] Login with `rahul@healthbridge.com` reaches the Dashboard, and a reopened app skips login
- [ ] Doctor Summary PDF is 1 page, nothing clipped, no "null"
- [ ] Uploaded PDF URL opens in a browser
- [ ] QR scans on a second phone (URL mode and offline emergency text mode)

---

## 2. Priti (Login polish, Input Dialogs)

| # | Task | Files | Status |
|---|------|-------|--------|
| P1 | `LoginActivity` + `activity_login.xml` (Supabase Auth, pre-filled demo credentials) | `ui/LoginActivity.java`, `res/layout/activity_login.xml` | [x] built by Aditya, polish only |
| P2 | Login validation and friendly error messages | same files | [x] |
| P3 | `MedicationDialog`: Name, Dosage, Frequency, Start Date, Status | `ui/MedicationDialog.java`, `dialog_add_medication.xml` | [x] |
| P4 | `HistoryDialog`: Condition, Diagnosed On, Notes | `ui/HistoryDialog.java`, `dialog_add_history.xml` | [x] |
| P5 | Save buttons POST to Supabase and refresh the Dashboard | uses existing Retrofit endpoints | [x] |

**Acceptance tests**
- [ ] Empty required fields show field errors, nothing is sent
- [ ] Saved medication and history rows appear in Supabase (`start_date`, `status`, `diagnosed_on` filled)
- [ ] Dialog stays open with a message if saving fails
- [ ] Dates are saved as `yyyy-MM-dd`

---

## 3. Atharv (Document Vault)

| # | Task | Files | Status |
|---|------|-------|--------|
| V1 | Vault layout: gradient, header, search bar, category chips, RecyclerView, FAB, empty state | `activity_vault.xml` | [x] |
| V2 | `RecordsAdapter` and row layout (icon tile, title, date, category tag, chevron) | `adapters/RecordsAdapter.java`, `item_record.xml` | [x] |
| V3 | Load records for the logged-in profile, newest first | `ui/VaultActivity.java` | [x] |
| V4 | Search by title plus chip filter, combined | `VaultActivity` | [x] |
| V5 | File picker (PDF, JPEG, PNG only), 10 MB limit, MIME detection | `VaultActivity` | [~] needs phone test |
| V6 | Upload bytes to `healthbridge-files`, then insert the `medical_records` row | `VaultActivity` | [~] needs phone test |
| V7 | Preview: Glide for images, `ACTION_VIEW` for PDFs with fallback | `VaultActivity` | [~] needs phone test |
| V8 | Details popup dialog (preview, title, category tag, type, date, View button) | `dialog_record_details.xml` | [x] |

**Acceptance tests**
- [ ] One PDF and one image upload, appear in the list, and open
- [ ] Search and each chip work, also together
- [ ] Files over 10 MB and other types are rejected with a message
- [ ] The Dashboard's Summary PDF shows under the Summary chip
- [ ] No internet gives a friendly message, no crash

---

## 4. Sonali (Dashboard, BMI, Native Actions)

| # | Task | Files | Status |
|---|------|-------|--------|
| S1 | `activity_dashboard.xml`: header, Emergency Card, Vitals card, action tiles | `res/layout/activity_dashboard.xml` | [x] |
| S2 | Load the profile and fill the UI ("Not provided" for empty values) | `ui/DashboardActivity.java` | [x] |
| S3 | `HealthUtils`: BMI, WHO category, badge color, N/A case | `utils/HealthUtils.java` | [x] |
| S4 | SOS with `Intent.ACTION_DIAL` | `DashboardActivity` | [~] needs phone test |
| S5 | Nearby Hospitals via Maps intent with browser fallback | `DashboardActivity` | [~] needs phone test |
| S6 | Buttons: Vault, Doctor Summary, Share via QR, Add Medication, Add History, Logout | `DashboardActivity` | [x] |

**Acceptance tests**
- [ ] Emergency Card shows Rahul's blood group, allergies and contact without scrolling
- [ ] BMI value and colored badge match the table in `design.md`, missing data shows "N/A"
- [ ] SOS opens the dialer with the number, hospitals opens Maps
- [ ] No Blood Pressure or Sugar fields (not in the PRD or database)

---

## 5. Final Team Run (Everyone, real phone, fresh install)

- [ ] Correct app name and launcher icon
- [ ] Login, Dashboard, Vault, Dialogs, PDF, QR all work in one demo flow
- [ ] Offline test: friendly messages, no crash
- [ ] Small and large screen: no clipped text, no horizontal scroll (except chips)
- [ ] Phone in dark mode: app still readable (light theme forced)
- [ ] No placeholder text, no leftover debug code, no secrets in the repo
- [ ] `main` builds cleanly after all merges

## Demo script
1. Open the app, tap Login (pre-filled)
2. Show the Emergency Card, BMI badge, SOS and Nearby Hospitals
3. Open the Vault: upload a PDF and an image, search, filter, preview
4. Generate the Doctor Summary PDF
5. Show the QR and scan it with a second phone
