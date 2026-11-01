# Health Bridge: 24-Hour Sprint Task Assignments

**Project Rules for All Members (and AI Agents):**
* Strict Android Java + XML layouts only. No Kotlin, no Jetpack Compose.
* UI Theme: Teal (`#00897B`) and White, with a Crimson Red (`#D32F2F`) Emergency Card.
* Use Retrofit2 for all backend API calls (no Supabase Kotlin SDK).

---

## 1. Aditya (Team Lead, Backend & Core APIs)
*Handles Git, Database Infrastructure, and complex custom implementations.*

* [ ] **Phase 1: Setup**
  * [ ] Initialize empty Android Studio project (Java).
  * [ ] Configure `build.gradle` (Retrofit, Gson, Glide, ZXing dependencies).
  * [ ] Setup GitHub repository and invite team.
* [ ] **Phase 2: Database (Supabase MCP)**
  * [ ] Run SQL script via MCP to create tables: `profiles`, `medical_records`, `medications`, `medical_history`.
  * [ ] Set storage bucket `healthbridge-files` to Public.
  * [ ] Seed the database with demo user (Rahul Sharma).
* [ ] **Phase 3: Core API & Utilities**
  * [ ] Build `SupabaseClient.java` (inject API Keys) and `SupabaseApiService.java` (Retrofit endpoints).
  * [ ] Implement `HealthBridgePdfGenerator.java` using native `PdfDocument` to draw the 1-page summary.
  * [ ] Implement `QrCodeHelper.java` using ZXing to generate the QR code popup.
* [ ] **Phase 4: Integration**
  * [ ] Review PRs, resolve merge conflicts, and link Activities together.

---

## 2. Priti (Authentication & Data Entry)
*Handles session management and user input dialogs.*

* [ ] **Phase 1: Authentication**
  * [ ] Create `LoginActivity.java` and `activity_login.xml`.
  * [ ] Build UI with Email and Password inputs, and a Login Button.
  * [ ] Pre-fill credentials with `rahul@healthbridge.com` and `123456` for fast demo access.
  * [ ] Create `SessionManager.java` (using `SharedPreferences`) to save and retrieve `user_id`.
  * [ ] Route successful login to `DashboardActivity`.
* [ ] **Phase 2: Input Dialogs (Dashboard Popups)**
  * [ ] Build an `AlertDialog` builder method in Java to "Add New Medication" (Inputs: Name, Dosage, Start Date, Status).
  * [ ] Build an `AlertDialog` builder method in Java to "Add Medical History" (Inputs: Condition, Date, Notes).
  * [ ] Wire the "Save" buttons on these dialogs to construct JSON objects and trigger the respective Retrofit POST API calls.

---

## 3. Atharv (Document Vault & File Uploads)
*Handles the Medical Records list, UI filtering, and local-to-cloud file handling.*

* [ ] **Phase 1: Vault Layout**
  * [ ] Create `VaultActivity.java` and `activity_vault.xml`.
  * [ ] Implement a top Search Bar for querying document names.
  * [ ] Implement a horizontal `ScrollView` with Filter Chips (All, Prescription, Lab Report, X-Ray, Summary).
  * [ ] Add a `RecyclerView` to the layout and create `RecordsAdapter.java` to display fetched records.
  * [ ] Add a Floating Action Button (FAB) to the bottom right for uploading new documents.
* [ ] **Phase 2: File Picker & Upload Logic**
  * [ ] Wire the FAB to launch an `Intent.ACTION_GET_CONTENT` file picker.
  * [ ] Restrict the file picker strictly to `image/jpeg`, `image/png`, and `application/pdf` using `EXTRA_MIME_TYPES`.
  * [ ] Write a Java helper using `ContentResolver` to convert the selected file `Uri` into a `byte[]` array.
  * [ ] Pass the byte array to Aditya's Retrofit Storage API to upload the file to Supabase.

---

## 4. Sonali (Dashboard UI & Native Integrations)
*Handles the main landing screen layout, health calculations, and OS-level intent actions.*

* [ ] **Phase 1: Dashboard UI Construction**
  * [ ] Create `DashboardActivity.java` and `activity_dashboard.xml`.
  * [ ] Build the Top Emergency Card in Crimson Red (`#D32F2F`) displaying Patient Name, Blood Group, and Allergies.
  * [ ] Build the Vitals Card displaying Blood Pressure, Sugar, Height, Weight, and BMI.
  * [ ] Add primary action buttons: "My Medical Vault", "Generate Doctor PDF", and "Share via QR Code".
* [ ] **Phase 2: Vitals Math & System Intents**
  * [ ] Create `HealthUtils.java`.
  * [ ] Add BMI calculation logic in `HealthUtils` (Weight / Height in meters squared) and return a status string with an associated color badge (Green for Normal, Red for Obese, etc.).
  * [ ] Implement logic for an "SOS Call" button using `Intent.ACTION_DIAL`.
  * [ ] Implement logic for a "Nearby Hospitals" button using `Intent.ACTION_VIEW` configured for Google Maps search.