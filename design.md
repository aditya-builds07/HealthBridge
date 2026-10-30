# Health Bridge: Design Guide (design.md)

**Source:** design inspiration image (MediSphere-style glassmorphism UI)
**Platform:** Android (Java + XML)
**Rule:** this file defines **look and feel only**. No new features are added. Anything in the inspiration image that is not in the PRD is listed in Section 11 as **not to be built**.

---

## 1. Design Direction

A calm, clean, "frosted glass" medical interface:

- Soft teal-grey gradient background
- Translucent white cards with rounded corners
- Pastel-tinted category tiles
- Dark teal rounded buttons
- White text on the teal header area, dark text on cards
- **Crimson red is reserved for the Emergency card only**, so it always stands out

**Feeling:** trustworthy, calm, easy to read in a stressful moment.

## 2. Design Principles

1. **Emergency first:** the Red-Alert Card is the most prominent element on the Dashboard.
2. **One idea per card:** each card shows one thing (emergency info, BMI, actions).
3. **Big touch targets:** at least 48dp for anything tappable.
4. **Consistent shapes:** same corner radius family everywhere.
5. **Color means something:** teal = app/brand, crimson = emergency, pastel tints = document categories, BMI colors = health status.
6. **Keep it buildable:** the team has beginners using AI agents, so every style must be doable with plain XML (no custom libraries).

## 3. Color Palette

### Brand and surfaces
| Token | Hex | Use |
|-------|-----|-----|
| `hb_teal_primary` | `#00897B` | Primary buttons, FAB, active chip, links (locked in PRD) |
| `hb_teal_dark` | `#00695C` | Pressed state, dark action bars, header text accents |
| `hb_teal_deep` | `#004D40` | Status bar, strongest emphasis |
| `hb_bg_top` | `#7FA9A5` | Background gradient start |
| `hb_bg_bottom` | `#3F7F7B` | Background gradient end |
| `hb_glass_fill` | `#B3FFFFFF` | Frosted card fill (white at about 70%) |
| `hb_glass_stroke` | `#80FFFFFF` | 1dp card border (white at about 50%) |
| `hb_card_solid` | `#F4FAF9` | Fallback card color if translucency looks bad |
| `hb_text_on_dark` | `#FFFFFF` | Text on teal header and gradient |
| `hb_text_primary` | `#1F2D2B` | Main text on cards |
| `hb_text_secondary` | `#5C6F6C` | Dates, subtitles, hints |

### Emergency
| Token | Hex | Use |
|-------|-----|-----|
| `hb_emergency_red` | `#D32F2F` | Emergency Red-Alert Card (locked in PRD) |
| `hb_emergency_red_dark` | `#B71C1C` | SOS button pressed state |
| `hb_on_emergency` | `#FFFFFF` | Text and icons on the red card |

### Document category tints (tile background / tag background / icon color)
| Category | Tile / tag | Icon / tag text |
|----------|-----------|-----------------|
| Prescriptions | `#DDF3E8` | `#2E9E6B` |
| Lab Reports | `#DCEBFA` | `#2F7BD1` |
| X-Rays | `#E6E1F7` | `#6A4FD0` |
| Medical History | `#FBE0DE` | `#D9534F` |
| PDF file icon accent | `#FDE3E1` | `#E53935` |

### BMI badge colors (locked behavior from PRD F4)
| Category | Badge | Text |
|----------|-------|------|
| Underweight | `#1E88E5` | White |
| Normal | `#43A047` | White |
| Overweight | `#FB8C00` | White |
| Obese | `#E53935` | White |
| N/A | `#9E9E9E` | White |

## 4. Typography

Use the default Android font family (Roboto) or the Google font **Poppins** if the team wants a softer look. Poppins is optional; do not add it if it slows setup.

| Style | Size | Weight | Use |
|-------|------|--------|-----|
| Display | 28sp | Bold | Greeting name on Dashboard |
| Title | 20sp | Bold | Screen titles, section headings |
| Subtitle | 16sp | Medium | Card titles, list item titles |
| Body | 14sp | Regular | General text |
| Caption | 12sp | Regular | Dates, tags, helper text |
| Button | 14sp | Medium | All buttons (no all-caps) |

Always use `sp` for text and `dp` for everything else.

## 5. Shape, Spacing and Elevation

| Item | Value |
|------|-------|
| Spacing grid | 8dp (use 4, 8, 12, 16, 24) |
| Screen side padding | 16dp |
| Large card radius | 24dp |
| Standard card / tile radius | 20dp |
| Button radius | 16dp (primary buttons may be fully pill-shaped) |
| Chip radius | 20dp (pill) |
| Tag radius | 8dp |
| Card elevation | 2dp to 4dp, soft shadow only |
| Card border | 1dp `hb_glass_stroke` |
| Icon size | 24dp standard, 40dp inside a tinted icon tile |

## 6. Glass Effect: How to Build It Safely on Android

True background blur is hard and only works on newer Android versions, so we **fake it** with a translucent fill and a light border. This looks very close and works on every device.

**Rules**
- Do **not** use blur libraries or `RenderEffect`.
- Use `hb_glass_fill` plus a 1dp `hb_glass_stroke` border and a rounded shape.
- Put the cards on top of the gradient background.
- If text contrast looks weak on a device, switch the card to `hb_card_solid`.

**Gradient background** (`bg_gradient.xml` in `res/drawable`)
```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <gradient
        android:angle="270"
        android:startColor="#7FA9A5"
        android:endColor="#3F7F7B" />
</shape>
```

**Glass card** (`bg_glass_card.xml`)
```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#B3FFFFFF" />
    <stroke android:width="1dp" android:color="#80FFFFFF" />
    <corners android:radius="20dp" />
</shape>
```

**Emergency card** (`bg_emergency_card.xml`)
```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#D32F2F" />
    <corners android:radius="24dp" />
</shape>
```

**Primary button** (`bg_button_primary.xml`)
```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#00897B" />
    <corners android:radius="16dp" />
</shape>
```

## 7. Component Library

### 7.1 Header (all screens)
- Sits on the gradient background, white text.
- Title (Title style) plus a one-line subtitle (Caption, white at 80%).
- Optional back arrow on the left for inner screens.

### 7.2 Glass Card
- Background `bg_glass_card`, 16dp inner padding, 12dp gap between cards.
- Used for BMI snapshot, action group, list rows, and the details dialog.

### 7.3 Emergency Red-Alert Card
- Background `bg_emergency_card`, white text, 24dp radius, 16dp padding.
- Top row: small alert icon plus the title "Emergency Info".
- Three labeled rows: **Blood Group**, **Allergies**, **Emergency Contact**.
- Blood group is shown large (Display or Title style) so it is readable at a glance.
- Empty values show "Not provided".

### 7.4 Quick Action Tile
- Square-ish glass tile (about 96dp tall), rounded 20dp.
- Centered 40dp icon in a tinted circle or rounded square, label below (Caption, medium weight).
- Used for the existing actions only: SOS, Nearby Hospitals, Doctor Summary, QR Code.
- The **SOS tile uses the crimson accent** (red icon tile) so it is distinguishable.

### 7.5 BMI Badge
- Pill (`8dp` to `16dp` radius) with BMI number and category word, for example `22.4 · Normal`.
- Background color from the BMI table in Section 3; white text.

### 7.6 Category Filter Chips (Vault)
- Horizontal scroll row (`HorizontalScrollView` plus Material `Chip` or `ChipGroup`).
- Selected chip: teal `#00897B` fill with white text.
- Unselected chip: glass fill with dark teal text.
- Chip labels should match the `category` values stored in `medical_records`. Suggested set: **All, Prescriptions, Lab Reports, X-Rays, Medical History**. Confirm these against the database.

### 7.7 Search Bar (Vault only)
- Rounded pill (28dp radius), glass fill, search icon on the left, hint text "Search documents...".
- No filter icon on the right (not in scope).

### 7.8 Document List Row
- Glass card, 72dp to 80dp tall.
- Left: 48dp rounded icon tile tinted by category (or red PDF icon for PDFs, small image thumbnail for images via Glide).
- Middle: **Title** (Subtitle style), then a caption line with date, then a small colored **category tag**.
- Right: chevron icon.
- Row tap opens the preview (image with Glide, PDF with `Intent.ACTION_VIEW`).

### 7.9 Floating Action Button (Upload)
- Circular, 56dp, teal `#00897B`, white "+" icon.
- Bottom-right of the Vault screen, 16dp from the edges.
- Opens the file picker (PDF and images).

### 7.10 Document Details Popup (AlertDialog / custom dialog)
Keeps the "popup dialogs instead of Fragments" rule from the PRD.
- Glass-style rounded dialog.
- Top: preview (Glide thumbnail for images, PDF icon for PDFs), title, category tag.
- Info rows, each with a small icon, a caption label and a value: **File Name, File Type, Category, Uploaded On**.
- One primary teal button: **View** (opens the file).

### 7.11 QR Popup
- Rounded dialog with the QR (about 512 x 512 px source, shown at about 240dp) centered on a white square so scanners read it easily.
- Short caption under the QR (for example "Scan to open Doctor Summary").
- Close button in teal.

### 7.12 Progress and Messages
- Upload shows a teal `ProgressBar` or progress dialog.
- Success and error use a `Snackbar` or `Toast` with short plain text.

## 8. Screen Designs

### 8.1 LoginActivity
- Full gradient background.
- Centered logo circle (teal, white cross/heart icon), app name **Health Bridge**, tagline "Bridging Patient History with Better Doctor Decisions".
- One glass card containing: email field, password field (both pre-filled with demo credentials), and a full-width primary **Login** button.
- Fields: rounded 16dp, white fill, dark text.

### 8.2 DashboardActivity (inspired by the first phone in the image)
Top to bottom:
1. **Header:** app name and tagline on the left, patient avatar circle on the right (a static placeholder, not a feature). Greeting below: "Good Morning," and the patient name (Display style, white).
2. **Emergency Red-Alert Card** (most prominent element).
3. **Vitals glass strip:** BMI snapshot with the colored badge, plus height and weight values.
4. **Quick Actions** (section title, then a row/grid of tiles): **SOS**, **Nearby Hospitals**, **Doctor Summary**, **QR Code**.
5. Entry to the Vault: a glass card or tile labeled **Document Vault** that opens `VaultActivity`.

Notes:
- The greeting text can follow the time of day using simple local logic; it is cosmetic only.
- Wrap the content in a `ScrollView` so small phones are covered.

### 8.3 VaultActivity (inspired by the second phone in the image)
Top to bottom:
1. **Header:** title "Medical Documents", subtitle "All your health records in one place", back arrow.
2. **Search bar.**
3. **Category chips** (horizontal).
4. **RecyclerView** of Document List Rows.
5. **FAB (+)** for upload at the bottom-right.

### 8.4 Document Details (inspired by the third phone in the image)
Shown as a **popup dialog** over the Vault (see 7.10), not a new screen.

## 9. States and Feedback

| State | Design |
|-------|--------|
| Loading | Centered teal progress indicator on the gradient |
| Empty vault | Glass card with an icon and the text "No documents yet. Tap + to upload." |
| No internet / error | Short Snackbar message, never a crash |
| Pressed | Buttons and tiles darken to `hb_teal_dark` (or emergency dark red for SOS) |
| Disabled | 40% opacity |

## 10. Accessibility

- Text contrast: dark text on glass cards, white text on teal and red. If it looks weak, use the solid card fallback.
- Minimum touch target 48dp.
- Add `contentDescription` to every icon-only view (FAB, chevrons, icon tiles).
- Never use color alone to convey meaning: BMI badge always includes the category word, and the category tag always includes text.
- Keep text at 12sp or larger.

## 11. Elements in the Inspiration Image That We Do NOT Build

The image shows more than our PRD allows. Take only the **visual style** from it. Do not build these:

| Element in image | Decision |
|------------------|----------|
| Notification bell | Not built |
| Bottom navigation bar (Home, Documents, Health, Profile) | Not built; the app uses 3 Activities and simple buttons |
| Document count stats (Total Docs, per-category counts) and category count tiles | Not built |
| Scan Prescription (camera) | Not built |
| Share with Doctor flow, Share and Download buttons | Not built; sharing is only the existing QR feature |
| Filter icon in search bar | Not built |
| Overflow "..." menus | Not built |
| Document Notes field | Not built |
| Separate Document Details screen | Replaced by a popup dialog (info shown only from existing data) |

## 12. File Ownership for Design Work

Design resources are split so teammates do not edit the same file.

| Owner | Design files |
|-------|--------------|
| **Aditya** | Shared resources: `colors.xml`, `themes.xml`, `styles.xml`, `dimens.xml`, drawables in Section 6 (`bg_gradient`, `bg_glass_card`, `bg_emergency_card`, `bg_button_primary`) |
| **Sonali** | `activity_dashboard.xml` and its own item layouts |
| **Atharv** | `activity_vault.xml`, `item_record.xml`, details dialog layout |
| **Priti** | `activity_login.xml` |

**Rule:** teammates only **reference** shared tokens (for example `@color/hb_teal_primary`, `@drawable/bg_glass_card`). They never create their own hard-coded colors. If a token is missing, ask Aditya to add it.

## 13. Implementation Checklist

- [ ] Aditya adds all color tokens to `colors.xml` and the four drawables, then merges to `main` first
- [ ] Teammates pull `main` before starting their screens
- [ ] Every screen uses `bg_gradient` as the window background
- [ ] Every card uses `bg_glass_card` (or `bg_emergency_card` for emergency)
- [ ] All buttons use the primary teal style except SOS
- [ ] BMI badge colors match Section 3
- [ ] No hard-coded hex colors inside layout files
- [ ] Icons use Android Studio Vector Asset (Material icons), no extra icon libraries
- [ ] All icon-only views have `contentDescription`
- [ ] Tested on a small phone size and a large phone size

## 14. Instruction Block for Antigravity Agents

Paste this at the start of any AI-agent task so every teammate gets the same look:

> Follow `design.md` for all styling. Use only the color tokens, drawables and dimensions defined there; do not hard-code colors. Use a teal-to-grey gradient window background, translucent white glass cards (20dp radius, 1dp light border), pill-shaped chips, a teal circular FAB, and the crimson card only for emergency info. Do not add features, screens, bottom navigation, or libraries that are not listed in `PRD.md`. Only edit the files assigned to you.
