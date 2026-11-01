# Health Bridge: Quality and Polish Checklist

**Purpose:** a final-polish checklist for the Health Bridge Android app and its GitHub repository.
**Origin:** merged from three scattered "website launch" lists (launch checklist, quick fixes, and "AI-looking design tells"). Duplicates were removed and every item was translated for a native Android app.
**Scope rule:** this file only checks quality. It adds **no new features**. Items that would need a new feature are marked **Out of scope**.

---

## 0. How to Read This File

| Tag | Meaning |
|-----|---------|
| **DO** | Applies to the app now, must be done before the demo |
| **REPO** | Applies to the GitHub repository (README, description, etc.) |
| **N/A** | Web-only, does not apply to a native Android app |
| **OUT** | Would need a new feature, so it is excluded by the PRD |

Priority: **P1** = must fix before demo, **P2** = should fix, **P3** = nice to have.

---

## 1. Security and Secrets  (P1)

The GitHub repo is **Public**, so a leaked key is visible to everyone.

- [ ] **DO** Keep the Supabase URL and anon key in one place only (`local.properties` read through `BuildConfig`, or a single constants file). Do not repeat them across files.
- [ ] **DO** Never commit the Supabase `service_role` key. Only the anon key may exist in the app.
- [ ] **DO** Confirm `local.properties` is in `.gitignore` (the Android template includes it).
- [ ] **DO** Search the whole project for "service_role", "secret" and "password" before every push to `main`.
- [ ] **DO** If a secret was ever pushed by mistake, rotate the key in Supabase. Deleting the file is not enough, because Git history keeps it.
- [ ] **DO** Use only `https://` URLs. Do not set `usesCleartextTraffic="true"` in the manifest.
- [ ] **DO** Request only the `INTERNET` permission.
- [ ] **REPO** Add a one-line note in the README: "Prototype with fictional demo data. RLS is intentionally disabled for demo speed. Not for real medical data."

*Source rules: "Get your secrets off the front end", "Force HTTPS".*

---

## 2. Legal and Compliance Items

| Original rule | Decision |
|---------------|----------|
| Privacy policy page | **OUT** (no new screen). Use the README disclaimer above instead. |
| Terms and conditions | **OUT**. Same disclaimer covers the prototype. |
| Cookie consent banner | **N/A** (native app, no cookies) |
| Spam / bot protection | **N/A** (no sign-up or public forms; demo login only) |
| Analytics | **OUT** (not in PRD, adds a third-party SDK) |

---

## 3. Forms, Messages and Feedback  (P1)

Applies to Login and the Vault upload. These improve existing features only.

- [ ] **DO** Login: show a clear error for empty fields and for wrong credentials.
- [ ] **DO** Upload: check file type (image or PDF only) and size (suggested max 10 MB) before uploading.
- [ ] **DO** Show a **success message** after login, upload and PDF generation (short Toast or Snackbar).
- [ ] **DO** Show an **error message** for every failed network call. Messages are plain and helpful, for example "Upload failed. Check your internet and try again."
- [ ] **DO** Never show raw exceptions, "null", or stack traces to the user.
- [ ] **DO** Empty values display "Not provided" (see PRD F1.4).
- [ ] **DO** Disable or show progress on the Login and Upload buttons while a request is running, to avoid double taps.

*Source rules: "Add form validation", "Add success messages", "Add error messages".*

---

## 4. Links, Buttons and Navigation  (P1)

- [ ] **DO** Every button and tile does something. No dead taps. Test each one.
- [ ] **DO** SOS opens the dialer with the right number (`Intent.ACTION_DIAL`).
- [ ] **DO** Nearby Hospitals opens Maps, with the browser fallback if Maps is missing.
- [ ] **DO** Wrap every `startActivity` for outside apps in a safe check so a missing app never crashes the app.
- [ ] **DO** Remove unused navigation: no leftover default `MainActivity`, empty menus, or unused buttons.
- [ ] **DO** Back button behavior is correct on every screen (Vault returns to Dashboard, not Login).
- [ ] **DO** Make sure the QR link and the PDF link open correctly on a second phone ("broken link" check).
- [ ] **N/A** Mobile menu, footer links, clickable logo, clickable email (no such UI in this app)
- [ ] **Note** The phone number is already actionable through the SOS button. Do not add extra call or email features.

*Source rules: "Fix broken links", "Fix broken buttons", "Remove unused navigation", "Make the phone number clickable".*

---

## 5. Mobile Layout  (P1)

The app is mobile only, so these rules matter on every screen.

- [ ] **DO** No clipped or overflowing text on a **small phone** (about 360 x 640dp) and a **large phone**.
- [ ] **DO** Use `ScrollView` or `NestedScrollView` where content can exceed the screen height (Dashboard, Login with keyboard open).
- [ ] **DO** No unintended horizontal scrolling. The only horizontal scroll allowed is the category chip row.
- [ ] **DO** Touch targets at least 48dp.
- [ ] **DO** Test both portrait and rotation. Either lock to portrait in the manifest or confirm layouts survive rotation. Portrait lock is the simpler choice for the demo.
- [ ] **DO** Keyboard does not cover the Login button.
- [ ] **DO** Text sizes in `sp`, spacing in `dp`, no fixed pixel sizes.

*Source rules: "Make it mobile friendly", "Remove horizontal scrolling", "Fix mobile overflow", "Make every page mobile optimized".*

---

## 6. Performance  (P2)

- [ ] **DO** Dashboard and Vault data loads in under 3 seconds on normal Wi-Fi (PRD target).
- [ ] **DO** All network work is asynchronous. Nothing blocks the UI thread.
- [ ] **DO** Use Glide for images so large images are downscaled for display and cached.
- [ ] **DO** Compress **bundled** assets: prefer vector icons (Material), use WebP instead of large PNG for any raster image, and keep the launcher icon small.
- [ ] **DO** Show a loading indicator for anything that takes more than about half a second.
- [ ] **N/A** "Check your page load speed" (web term). The equivalent is the screen load target above.
- [ ] **Note** Do not add new image-processing features for uploads. The size limit in Section 3 is enough.

*Source rules: "Compress your images", "Check your page load speed".*

---

## 7. Accessibility and Color Contrast  (P2)

- [ ] **DO** Text contrast is readable: dark text on glass cards, white text on teal and red (see `design.md` Section 10).
- [ ] **DO** If text looks weak on any device, switch that card to the solid fallback color `hb_card_solid`.
- [ ] **DO** Add `contentDescription` to every icon-only view. This is the Android version of **alt text** for images.
- [ ] **DO** Never rely on color alone. BMI badge and category tags always include text.
- [ ] **DO** Force light theme (design is light only). Without this, a phone in dark mode can produce unreadable low-contrast screens. Set a Light parent theme or `AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)`.

*Source rules: "Fix your color contrast", "Add alt text to your images", "low-contrast dark mode".*

---

## 8. App Identity and Placeholder Cleanup  (P1)

Android equivalents of favicon, page titles, meta tags and placeholder text.

| Web rule | Android equivalent | Done |
|----------|--------------------|------|
| Favicon | Custom **launcher icon** (adaptive icon), not the default green Android robot | [ ] |
| Page titles | Correct **app name** `Health Bridge` in `strings.xml`, and a proper title on each screen | [ ] |
| Meta description | One-line description in the **GitHub repo description** | [ ] |
| Social preview image | **GitHub repo social preview** image (Settings, Social preview). Use a clean screenshot or banner | [ ] |
| Sitemap / robots.txt | **N/A** | n/a |
| Custom 404 page | A friendly **empty or error state** (for example "No documents yet" and a retry message) | [ ] |
| Remove placeholder text | No "Hello World", "Lorem ipsum", "TODO", "test", "button" labels or default template text anywhere | [ ] |
| Fix copyright year | If any About or footer text exists, use **2026**. Otherwise skip. | [ ] |

- [ ] **DO** Check `strings.xml` and every layout for leftover placeholder strings.
- [ ] **DO** Remove unused files and resources (default `ic_launcher` leftovers, sample layouts).

---

## 9. "Does It Look AI-Generated?" Design Audit  (P2)

The third list describes common signs of a generic, AI-built UI. Below is each sign checked against our `design.md`. **Important:** our design inspiration image is a glassmorphism UI, so glass cards are a **deliberate, documented choice** here. They stay, but they must be used consistently and kept readable.

| Common "tell" | Our rule |
|---------------|----------|
| Purple-to-blue gradients | **Avoid.** We use the teal-grey gradient only. |
| Gradient headline text | **Avoid.** Headlines are solid white or dark text. |
| Small badge above the headline | **Avoid** on every screen. |
| Three icon boxes in a row | **Watch out.** The Dashboard quick actions must not look like a generic icon-box row. Keep SOS visually distinct (red accent) and labels specific. |
| Emojis in headings | **Avoid.** Use Material icons only. |
| Colored left-border cards | **Avoid.** |
| Glassmorphism | **Allowed, intentionally.** Use the single `bg_glass_card` style everywhere, with the solid fallback if contrast is weak. |
| Untouched default components | Restyle Material buttons, chips and dialogs with our tokens. Do not ship default-looking widgets. |
| Lucide icons everywhere / mixed icon sets | Use **one icon set** (Material icons via Vector Asset), same size and stroke style. |
| Inter font everywhere | Roboto is fine on Android. Optional Poppins for headings. Pick one scheme and keep it. |
| Serif italic accent words | **Avoid.** |
| Em dashes everywhere in copy | Keep UI text short and plain. Avoid em dashes and filler. |
| Generic buzzword copy | Use specific, honest labels ("Emergency Info", "Doctor Summary"). No "revolutionary" or "seamless". |
| Sections fading in on scroll | **Avoid.** No scroll-triggered animation. |
| Cursor-following line, hover fades | **N/A** on mobile. Use simple pressed states only. |
| Low-contrast dark mode | No dark mode. Force light theme (Section 7). |
| Inconsistent spacing | Follow the **8dp grid** from `design.md`. No random margins. |
| Grain texture | **Avoid.** |

---

## 10. One Clear Primary Action per Screen  (P2)

Source rule: "Add one clear call to action". Applied per screen without adding features:

| Screen | Primary action | Style |
|--------|----------------|-------|
| LoginActivity | **Login** | Full-width teal button |
| DashboardActivity | **SOS** (emergency) with the Emergency card as the focus | Red-accent tile, largest |
| VaultActivity | **Upload (+)** | Teal FAB |

Secondary actions (Hospitals, Doctor Summary, QR) are visually quieter than the primary one.

---

## 11. GitHub Repository Polish  (P2)

- [ ] **REPO** Repo description set (tagline: "Bridging Patient History with Better Doctor Decisions. An Android Java + Supabase PBL Sprint project.")
- [ ] **REPO** README includes: app name, tagline, feature list (from PRD only), tech stack, team members, setup steps, demo credentials note, and the demo-data disclaimer
- [ ] **REPO** Screenshots of Login, Dashboard, Vault and the Doctor Summary PDF
- [ ] **REPO** `PRD.md`, `design.md` and this checklist are in the repo
- [ ] **REPO** Social preview image uploaded
- [ ] **REPO** Topics added (for example `android`, `java`, `supabase`, `healthcare`)
- [ ] **REPO** No build files, `.idea` folders, or keys committed
- [ ] **REPO** `main` builds cleanly after all merges

---

## 12. Final Pre-Demo Run  (P1)

Do this on a **real phone**, from a fresh install, after all PRs are merged into `main`.

- [ ] Fresh install works, correct app name and launcher icon
- [ ] Login works, errors show for bad input
- [ ] Dashboard: Emergency card, BMI badge, SOS, Hospitals, Doctor Summary, QR all work
- [ ] Vault: upload PDF, upload image, search, filter, preview
- [ ] Doctor Summary PDF is 1 page and readable
- [ ] QR scans correctly on a second phone
- [ ] Turn off internet: app shows friendly errors, no crash
- [ ] Small and large screen check, no overflow or clipped text
- [ ] Phone set to dark mode: app still looks correct
- [ ] No placeholder text, no leftover default screens
- [ ] Section 1 secrets search done, nothing sensitive in the repo

---

## 13. Ownership

| Area | Owner |
|------|-------|
| Secrets, manifest, themes, launcher icon, repo polish, final merge check | **Aditya** |
| Vault validation, messages, empty state, contentDescriptions | **Atharv** |
| Dashboard buttons, intents safety, BMI N/A state, layout overflow | **Sonali** |
| Login validation, messages, keyboard behavior | **Priti** |
| Final demo run | **Everyone** |

Each person checks only the items in their own files, to keep Git conflicts away.
