# Health Bridge: Product Requirements Document (PRD)

**Tagline:** Bridging Patient History with Better Doctor Decisions
**Formerly:** MediVault
**Project type:** PBL / 1-day hackathon sprint
**Platform:** Android (Java + XML)
**Backend:** Supabase (PostgreSQL + Public Storage) via Retrofit2 / OkHttp REST
**Version:** 1.1 (enhanced, scope unchanged from v1.0)

> **Scope lock:** This document only details the features already agreed. No new features are introduced. Anything not listed in Section 5 is out of scope (see Section 4).

---

## 1. Purpose and Problem Statement

In an emergency or a doctor's visit, a patient's critical information (blood group, allergies, current medicines, past conditions, old reports) is usually scattered across paper files, phone galleries and memory. This causes delays and risky decisions.

**Health Bridge** is a patient-side medical vault that keeps this information in one place and makes it instantly available in three situations:

1. **Emergency:** show blood group, allergies and contact in one glance, call for help in one tap, find nearby hospitals.
2. **Doctor visit:** hand over a clean 1-page summary PDF.
3. **Everyday storage:** keep reports and prescriptions (PDF or image) organized and searchable.

## 2. Goals and Success Criteria

| # | Goal | How we measure it |
|---|------|-------------------|
| G1 | Emergency info visible within 1 screen after login | Emergency card appears on Dashboard without scrolling |
| G2 | Emergency help reachable in 1 tap | SOS opens the dialer with the emergency number pre-filled |
| G3 | Documents safely stored and retrievable | Upload a PDF and an image, see both in the list, preview both |
| G4 | Doctor-ready summary generated on-device | 1-page PDF generated with correct data, no overflow |
| G5 | Contactless sharing | QR scans correctly on a second phone |
| G6 | Reliable live demo | Full demo flow (Section 11) completes with no crash |

## 3. Target User and Demo Persona

- **Primary user:** an individual patient managing their own records (patient-side app only).
- **Demo persona:** **Rahul Sharma**, pre-loaded in the database with sample profile, medications, history and records.
- **Demo access:** login screen pre-filled with demo credentials for instant entry.
- **Evaluators:** PBL judges who will see a live demo on a device or emulator.

## 4. Out of Scope (Explicit)

To protect the 1-day timeline, the following are **not** being built:

- Doctor-side app or portal
- User registration / sign-up flow and password reset
- Row Level Security and production-grade authentication (RLS is intentionally disabled for demo speed)
- Fragments / Navigation Component (popup dialogs are used instead)
- Offline database or sync
- Push notifications or reminders
- Multi-user or family profiles
- Any feature not listed in Section 5

## 5. Feature Requirements

Each requirement has an ID, a description and acceptance criteria so the team and the AI agents can verify "done".

### F1. Emergency Red-Alert Card
**Description:** A prominent crimson card on the Dashboard showing the patient's critical emergency data.

| ID | Requirement |
|----|-------------|
| F1.1 | Display blood group, allergies and emergency contact (name and phone) |
| F1.2 | Use Crimson Red `#D32F2F` as the card color with high-contrast white text |
| F1.3 | Data is fetched from the `profiles` table on Dashboard load |
| F1.4 | If a field is empty, show "Not provided" instead of blank or "null" |

**Acceptance:** Card is visible on Dashboard without scrolling and shows Rahul Sharma's real data from Supabase.

### F2. One-Tap Emergency SOS
| ID | Requirement |
|----|-------------|
| F2.1 | SOS button opens the phone dialer using `Intent.ACTION_DIAL` |
| F2.2 | Number comes from the emergency contact stored in the profile |
| F2.3 | Uses `ACTION_DIAL` (not `ACTION_CALL`), so no CALL_PHONE permission is needed |
| F2.4 | If no number exists, show a short Toast instead of crashing |

**Acceptance:** Tapping SOS opens the dialer with the number pre-filled.

### F3. Nearby Hospitals Locator
| ID | Requirement |
|----|-------------|
| F3.1 | Button launches a Google Maps intent searching for "hospitals near me" |
| F3.2 | If Google Maps is not installed, fall back to a browser maps URL |
| F3.3 | No Maps SDK or API key is required (intent only) |

**Acceptance:** Tapping the button opens Maps (or browser) showing nearby hospitals.

### F4. BMI Snapshot
| ID | Requirement |
|----|-------------|
| F4.1 | BMI = weight (kg) / height (m)² using height and weight from `profiles` |
| F4.2 | Result rounded to 1 decimal place |
| F4.3 | Color-coded badge using standard WHO categories (below) |
| F4.4 | Logic lives in `HealthUtils.java` as pure, testable methods |
| F4.5 | If height or weight is missing or zero, show "N/A" with a neutral grey badge (no divide-by-zero) |

| BMI | Category | Badge color |
|-----|----------|-------------|
| < 18.5 | Underweight | Blue |
| 18.5 to 24.9 | Normal | Green |
| 25.0 to 29.9 | Overweight | Orange |
| ≥ 30.0 | Obese | Red |

**Acceptance:** Changing weight in the database changes the badge value and color after refresh.

### F5. Document Vault (PDFs and Images)
| ID | Requirement |
|----|-------------|
| F5.1 | Floating Action Button opens the file picker (`Intent.ACTION_GET_CONTENT`) accepting `image/*` and `application/pdf` |
| F5.2 | MIME type detected using `ContentResolver` and `MimeTypeMap` |
| F5.3 | File converted to `byte[]` and uploaded to the public bucket `healthbridge-files` via `/storage/v1/object/` |
| F5.4 | After upload, a row is saved in `medical_records` (title, category, file URL, MIME type, timestamp) |
| F5.5 | Records shown in a `RecyclerView` list |
| F5.6 | Search bar filters the list by title |
| F5.7 | Horizontal category filter chips filter the list by category |
| F5.8 | Images preview with Glide; PDFs open with `Intent.ACTION_VIEW` |
| F5.9 | Show a progress indicator during upload and a clear success/failure message |
| F5.10 | Reject or warn on files over a set size (suggested limit: 10 MB) |

**Acceptance:** One PDF and one image can be uploaded, appear in the list, be found via search and chip filter, and be opened.

### F6. Doctor Summary PDF (1 page)
| ID | Requirement |
|----|-------------|
| F6.1 | Generated on-device with native Android `PdfDocument` (no external PDF library) |
| F6.2 | Rule-based layout: fixed sections, no AI generation at runtime |
| F6.3 | Contains: patient details, blood group, allergies, BMI, current medications, medical history |
| F6.4 | Strictly 1 page (A4: 595 x 842 points); long lists are truncated with "and N more" |
| F6.5 | Layout coordinates hand-coded by Aditya in `HealthBridgePdfGenerator.java` |
| F6.6 | Generated file is uploaded to `healthbridge-files` so it has a shareable cloud URL |

**Acceptance:** PDF opens correctly, fits on one page, and all text is readable and not clipped.

### F7. QR Code Sharing (ZXing)
| ID | Requirement |
|----|-------------|
| F7.1 | Generate a QR code with ZXing inside a popup `AlertDialog` |
| F7.2 | Mode A (online): QR encodes the cloud URL of the Doctor Summary PDF |
| F7.3 | Mode B (offline): QR encodes plain-text emergency card details |
| F7.4 | Implemented in `QrCodeHelper.java` |
| F7.5 | QR rendered at a size scannable from a second device (suggested 512 x 512 px) |

**Acceptance:** Scanning the QR with another phone opens the PDF link (Mode A) or shows emergency text (Mode B).

### F8. Demo Login
| ID | Requirement |
|----|-------------|
| F8.1 | `LoginActivity` pre-fills demo credentials for instant access |
| F8.2 | On success, navigates to `DashboardActivity` |
| F8.3 | Shows an error message on failed login |

**Acceptance:** One tap on Login reaches the Dashboard.

## 6. Architecture

### 6.1 Screens
| Activity | Role |
|----------|------|
| `LoginActivity` | Demo login |
| `DashboardActivity` | Emergency card, SOS, hospitals, BMI, summary PDF and QR actions |
| `VaultActivity` | Document list, search, chips, upload, preview |

Popup dialogs (QR, confirmations) replace Fragments.

### 6.2 Package Structure (`com.example.healthbridge`)
```
api/       SupabaseClient.java, SupabaseApiService.java
models/    Profile, MedicalRecord, Medication, MedicalHistory
adapters/  RecordsAdapter
utils/     HealthUtils, HealthBridgePdfGenerator, QrCodeHelper
ui/        LoginActivity, DashboardActivity, VaultActivity
```

### 6.3 Tech Stack
| Layer | Choice |
|-------|--------|
| Language / UI | Java + XML layouts |
| Networking | Retrofit2 + OkHttp + Gson (direct REST, no Supabase Kotlin SDK) |
| Images | Glide |
| QR | ZXing |
| PDF | Native `PdfDocument` |
| Backend | Supabase PostgreSQL + public Storage bucket |

### 6.4 Supabase REST Endpoints Used
| Purpose | Endpoint |
|---------|----------|
| Read / write table rows | `/rest/v1/<table>` |
| Upload file | `/storage/v1/object/healthbridge-files/<path>` |
| Public file URL | `/storage/v1/object/public/healthbridge-files/<path>` |

All requests carry the project `apikey` header. Keys are kept in one place (`SupabaseClient.java` or `local.properties`) and never hard-coded across files.

## 7. Data Model

4 tables, with RLS disabled / permissive for demo. *Columns below are the proposed minimum needed by the features above; finalize them in the SQL migration.*

| Table | Proposed columns |
|-------|------------------|
| `profiles` | id, full_name, age, gender, blood_group, height_cm, weight_kg, allergies, emergency_contact_name, emergency_contact_phone |
| `medical_records` | id, profile_id, title, category, file_url, mime_type, created_at |
| `medications` | id, profile_id, name, dosage, frequency |
| `medical_history` | id, profile_id, condition, notes, diagnosed_on |

**Storage:** one public bucket, `healthbridge-files`.

**Seed data:** Rahul Sharma with a complete profile, 2 to 3 medications, 2 to 3 history entries, and at least 2 sample records (one PDF, one image).

## 8. Non-Functional Requirements

| Area | Requirement |
|------|-------------|
| Performance | Dashboard data loads in under 3 seconds on normal Wi-Fi |
| Threading | All network calls are asynchronous (Retrofit `enqueue`); never block the UI thread |
| Reliability | Every network call handles failure with a user-friendly message; no crashes on no-internet |
| Usability | Teal `#00897B` primary color, Crimson `#D32F2F` for emergency only; touch targets at least 48dp; readable text sizes |
| Compatibility | Runs on Android 8.0 (API 26) and above (suggested minimum) |
| Permissions | Minimal: INTERNET only (picker and intents need no extra runtime permissions) |
| Security (demo scope) | Public bucket and open RLS are accepted for the demo only; demo data is fictional, **no real patient data** |
| Code quality | Each class has a single clear responsibility; no duplicated Supabase config |

## 9. Team Ownership and File Isolation

Strict ownership means no two people edit the same file, which avoids GitHub merge conflicts.

| Member | Owned files | Responsibility |
|--------|-------------|----------------|
| **Aditya** (Tech Lead) | `SupabaseClient.java`, `SupabaseApiService.java`, `HealthBridgePdfGenerator.java`, `QrCodeHelper.java`, Gradle, models, Supabase MCP setup | Project setup, backend wiring, database and seed data, PDF engine, QR, PR merging |
| **Atharv** | `VaultActivity.java`, `activity_vault.xml`, `RecordsAdapter.java` (+ item layout) | Vault UI, picker, upload, list, search, chips, preview |
| **Sonali** | `DashboardActivity.java`, `activity_dashboard.xml`, `HealthUtils.java` | Dashboard, Emergency card, SOS, hospitals, BMI |
| **Priti** | `LoginActivity.java`, `activity_login.xml` | Demo login screen *(additional tasks to be confirmed by Aditya)* |

**Shared files** (`AndroidManifest.xml`, `strings.xml`, `colors.xml`, `build.gradle`) are edited **only by Aditya**. Others request changes through him.

## 10. Git Workflow

- Repo: `HealthBridge` (Android `.gitignore`, no license)
- `main` = stable, always buildable. Nobody pushes directly to `main`.
- Branches: `feature/aditya-backend-pdf`, `feature/atharv-vault`, `feature/sonali-dashboard`, `feature/priti-auth-qr`
- Flow: commit small and often, push to your branch, open a Pull Request, Aditya reviews and merges.
- Before starting, run `git pull origin main` to stay in sync.
- Commit messages follow `feat: ...` / `fix: ...` style.
- AI agent rule: agents may only modify files owned by that member.

## 11. Milestones (1-Day Sprint)

| Phase | Work | Owner |
|-------|------|-------|
| 1. Foundation | Project, dependencies, packages, GitHub, branches | Aditya |
| 2. Backend | Supabase tables, seed data, bucket, Retrofit client and service | Aditya |
| 3. Parallel build | Login, Dashboard, Vault (all isolated) | Priti, Sonali, Atharv |
| 4. Advanced | PDF generator, QR helper | Aditya |
| 5. Integration | Merge PRs, wire Dashboard buttons to PDF and QR, fix conflicts | Aditya |
| 6. Testing and demo prep | Full test pass, rehearsal | All |

### Demo Script
1. Open app, tap Login (pre-filled).
2. Show the Emergency Red-Alert Card, BMI badge, SOS dialer, Nearby Hospitals.
3. Open Vault, upload a PDF and an image, search, filter, preview.
4. Generate the Doctor Summary PDF.
5. Show the QR code and scan it with a second phone.

## 12. Testing Checklist

- [ ] Login works with demo credentials
- [ ] Dashboard shows correct Rahul Sharma data
- [ ] SOS opens the dialer with the right number
- [ ] Hospitals button opens Maps (and fallback works)
- [ ] BMI shows the right value and badge color for each category and for missing data
- [ ] PDF upload works and opens
- [ ] Image upload works and previews
- [ ] Search and chip filters work together
- [ ] Doctor Summary stays on 1 page and looks clean
- [ ] QR scans on a second device (both modes)
- [ ] App survives no-internet without crashing
- [ ] Build passes after all PRs are merged into `main`

## 13. Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Merge conflicts | Lost time | Strict file ownership, shared files owned by Aditya |
| AI-generated code does not compile | Blocked teammates | Aditya reviews each PR; small, frequent merges |
| PDF coordinates wrong | Ugly output | Hand-coded by Aditya, tested on a real device |
| Network or Supabase issues during demo | Demo failure | Test beforehand, keep a pre-uploaded sample file and screenshots as backup |
| Large file upload fails | Vault demo breaks | Size check (F5.10), use small demo files |
| Teammates with no Android experience | Slow progress | Clear task checklists, one file per person, Aditya available for unblocking |
| Teammates cannot access the original Gemini chat | Lost context | This PRD and the task list act as the single source of truth |

## 14. Definition of Done

A feature is done when:
1. It meets its acceptance criteria in Section 5.
2. It builds and runs with no crash.
3. Its PR is reviewed and merged into `main`.
4. It works in the full demo flow.

## 15. Assumptions

- Internet is available during the demo.
- Android Studio, an emulator or a physical device, and Antigravity (with Supabase MCP for Aditya) are available.
- All data is fictional and the app is a prototype, not for real medical use.
