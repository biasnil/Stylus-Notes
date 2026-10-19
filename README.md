# Stylus Notes

A handwriting notebook for Android 16 (API 36, min API 34). You write with your registered Lenovo pen (or any pens you allow), keep notes in folders, pick a paper background for each page, and write on top of imported PDFs.

Kotlin and Jetpack Compose for the screens, plus a custom `View` for the writing surface (it needs raw `MotionEvent` access).

Made by **biasnil**. Source and releases: <https://github.com/biasnil/Stylus-Notes>. Problems: Settings → About → Report a problem, or email workacckwanrong@gmail.com.

## Opening and running

1. Open the `StylusNotes` folder in Android Studio (Narwhal or newer, with SDK Platform 36 installed).
2. Let Gradle sync. If Studio offers to upgrade AGP or library versions, that's fine.
3. Run on the tablet. On first launch you land on Pen setup.

New dependencies in this version: `pdfbox-android` (PDF export with selectable text), `material-icons-extended` and `lifecycle-runtime-compose`.

## Library

- **Sidebar:** Create, Notes, Recent, Trash, then Pen setup and Settings at the bottom.
- **Create** opens a menu: New note, New folder, Import PDF, Note from pictures.
- **Notes** shows the folders and notes in the open folder. The breadcrumb at the top (Notes › School › Physics) takes you back up. Folders can go inside folders.
- **Long-press** a folder or note for Rename, Move to…, and Delete.
- Deleting a note sends it to **Trash**. From there you can Restore it or Delete forever. Deleting a folder removes it and its subfolders, and the notes inside go to Trash. If you restore a note whose folder is gone, it goes to the top level.
- The sort button switches between Name and Last modified.

## Creating a note

New note opens the **Create note from…** sheet:

- **Top buttons:** the paper you used last, PDF, and Image.
- **Paper Background** with tabs: All, Standard, Grid, Math & Engineering, Music, Sports, Planners & Lists.

There are 69 backgrounds, all drawn in code (`paper/`), so they stay sharp at any zoom and in exports:

| Category | Backgrounds |
|---|---|
| Standard (8) | Blank, College ruled, Wide ruled, Narrow ruled, Cornell basic, Cornell basic (college ruled), Cornell styled, Cornell styled (college ruled) |
| Grid (13) | Graph 4 sq/in, 4 sq/in bold, 5 sq/in, 5 sq/in bold, 1 mm bold, 5 mm, 1 cm, Cross grid 4/in and 5/in, Dot grid 4/in and 5/in, Isometric grid, Isometric dots |
| Math & Engineering (13) | Engineering, Polar, Polar (degrees), Polar (radians), Log-log, Semi-log X and Y at 4/in and 5/in, each plain and bold |
| Music (5) | Staff, Double staff, Grand staff, Treble staff, Bass staff |
| Sports (18) | Basketball court HS, Basketball half-court HS, Football field, Football field red zone, Baseball field, Baseball stat/score sheet, Cricket, Soccer, Soccer half-field, Rugby union, Rugby league, Field hockey, Hockey rink international, Hockey rink USA, Tennis, Volleyball, Volleyball half-court, Pickleball |
| Planners & Lists (12) | Daily (blank, college ruled, dot grid), Weekly (blank, college ruled, dot grid), Weekly columns (5 and 7 days), Monthly (5 and 6 weeks), Task list, Task list (2 columns) |

Sports fields use a landscape page. Everything else is portrait. Page size (Letter or A4) is in Settings → Note defaults.

## The editor

Toolbar, left to right:

- Back
- Title
- Undo, Redo
- Tool: Pen, Highlighter, Eraser, Move pictures, plus three sizes
- Colour
- Insert picture
- Page navigation `‹ 2 / 5 ›`. On the last page, `›` becomes `›+` and adds a page.
- The ⋮ menu: Add page, Pages, Change background, Fit page to screen, What can draw (Pen only, Pen + finger, Auto), Export as PDF, Export page as PNG, Clear page, Pen setup

Other things to know:

- **Add page** opens the same sheet with "Same as this page", PDF (insert its pages after this one) and Image (one page per picture).
- **Pages** lists every page. Tap one to jump there, or move it up or down, duplicate it, or delete it.
- **Scrolling and zoom:** in Pen only mode, one finger scrolls and two fingers pinch-zoom. In the finger-drawing modes, use two fingers. Fingers never scroll while the pen is hovering or touching, or when the touch is palm-sized, so resting your hand doesn't move the page.
- **Pictures:** a new picture starts selected, with a dashed outline. Drag inside it to move it, drag the corner dot to resize it (keeps its shape), and drag the round handle above it to rotate (snaps to 45° steps). A bar at the bottom has Rotate left, Rotate right, Bring to front, Delete and Done. To pick a picture again later, use the Move pictures tool and tap it.
- **Colour:** the colour button shows the preset colours, your recent picks, and **More colours…**, which opens a honeycomb picker with a brightness slider, greys and a hex code box.
- **Highlighter** ink is see-through and always sits under pen ink.
- Undo history is kept per page while the note is open.

## Phones

Under 600 dp wide (phones, split screen) the layout changes:

- **Library:** the sidebar becomes a drawer behind the ☰ button, and Create moves to a round + button at the bottom right.
- **Editor:** the top bar keeps back, title, undo/redo and ⋮. The pen, colour, picture and page controls (`‹ 2 / 5 ›`) move to a bar at the bottom.
- **Pages:** each page in the Pages list has its own ⋮ menu, so nothing gets squashed.
- **Settings:** shows the category list first, then the page you tap.

## Settings

Laid out like Squid: categories on the left, options on the right (one at a time on phones).

**Notes**
- **Note editor:** resume last page, show the first page as the thumbnail (otherwise the last page viewed), disable the back button, keep screen on, follow device orientation (ignores the rotation lock).
- **Input methods:**
  - Primary input method: registered pens or any stylus
  - Finger action: scroll and zoom, draw, or draw until a pen is used
  - Manage pens
  - Primary and secondary side button: nothing, switch to eraser, eraser while held, switch to highlighter, undo, redo
  - Secondary button key: tap it, then press the pen button you want as secondary. Every other pen key counts as primary.
  - Eraser end: stroke eraser, same as the pen tip, or nothing
- **Note defaults:** default paper (used by the quick Paper button), page size, save page positions (scroll and zoom per page), default zoom (fit width, fit whole page, actual size), two-finger double tap (reset zoom, undo, nothing), and the PDF "keep text selectable" default.
- **Casting and presenting:** show the open page on a connected display (HDMI, USB-C or wireless). The second screen shows the page without the toolbar and updates after each stroke.

**General**
- **Theme:** Follow system, Light, Dark, Sakura, Ocean, Forest, Midnight (pure black).
- **Language:** Follow system, English, Bahasa Melayu, 简体中文, 한국어.
- **About:** version, changelog, open-source licences.

## Themes (XML)

All theme colours are in `app/src/main/res/values/colors.xml`, one block per theme (`dark_*`, `light_*`, `sakura_*`, and so on). Change a hex value there to restyle a theme. `ui/ThemeColors.kt` maps each block to the app's colour slots. To add a theme:

1. Copy a block in `colors.xml` with a new prefix.
2. Add a value to `ThemeChoice` in `data/Prefs.kt`.
3. Add a loader in `ThemeColors.kt`.
4. Add a label in `SettingsScreen.kt` and the strings files.

The launch window colours are in `res/values/themes.xml` and `res/values-night/themes.xml`.

## Languages (XML)

All text is in string resources:

| Language | File |
|---|---|
| English | `res/values/strings.xml` |
| Malay | `res/values-ms/strings.xml` |
| Simplified Chinese | `res/values-zh-rCN/strings.xml` |
| Korean | `res/values-ko/strings.xml` |

To add a language:

1. Copy `values/strings.xml` to `values-<code>/strings.xml` and translate it.
2. Add the code to `res/xml/locales_config.xml`.
3. Add it to `LANGUAGES` in `SettingsScreen.kt`.

The in-app picker uses Android's per-app language setting, so the choice also shows in Android Settings → Apps → Stylus Notes → Language.

## PDFs

Importing a PDF asks one question: **Keep text selectable?**

- **On:** when you export, the original PDF pages are copied as they are (PdfBox) and your ink and pictures are drawn on top as vectors. Text stays selectable and searchable, and the file stays close to the original size.
- **Off (flatten):** exported pages are images of the PDF with your ink on top. Simpler and smaller for scanned documents.

The default is in Settings, and you can still choose per import. Rotated PDF pages are handled.

In the app, PDF pages are shown with Android's built-in `PdfRenderer` and re-rendered sharper when you zoom in. Password-protected PDFs can't be opened.

## Export

- **Export as PDF** saves the whole note to `Download/StylusNotes`. Paper backgrounds and ink are vector.
- **Export page as PNG** saves the current page.

No storage permission is needed.

## Pen setup and input modes

Unchanged from before:

- Register one or more pens.
- Choose what can draw: Pen only, Pen + finger, or Auto (fingers until a pen touches).
- Choose which pens: registered pens only, or any stylus.
- Lenovo side buttons (key codes 600–604) toggle the eraser. Holding the primary stylus button while touching down also erases.
- Palm rejection and `ACTION_CANCEL` / `FLAG_CANCELED` handling work as before, now in `canvas/PageCanvasView.kt`.

## Storage

App-private files:

```
files/library.json      folders, note list, which folder each note is in, trash
files/notes/<id>.json   one file per note: pages, strokes (x, y, pressure, time), pictures
files/assets/           imported PDFs and pictures
files/thumbs/<id>.png   first-page thumbnails for the library
```

Writes go to a temp file and get renamed, so a crash mid-save can't corrupt anything. Notes save 0.7 s after a change, when you leave the editor, and when the app goes to the background. Deleting a note forever also deletes PDFs and pictures that no other note uses.

**Upgrading:** notes from the first version (one canvas, no pages) open as a single blank page with all their ink, at the top level of the library.

## Updates and releases

On start (Settings → About → "Check when the app opens", on by default) the app asks GitHub for the latest release of `biasnil/Stylus-Notes` and compares its tag with `versionName` in `app/build.gradle.kts`. If the tag is newer it shows a dialog with the release notes and **Download** (opens the release page), **Later** or **Skip this version**. "Check for updates" in the same screen does it by hand. Only the version request goes out; the `INTERNET` permission is used for nothing else.

To publish an update:

1. Raise `versionCode` and `versionName` in `app/build.gradle.kts` (for example `versionCode = 2`, `versionName = "1.1"`).
2. Build the APK (Build → Generate Signed App Bundle / APK → APK, release). Sign every release with the same key, or Android won't install it over the old one.
3. On GitHub, Releases → Draft a new release, tag `v1.1` (the `v` is optional), write the notes, attach the APK, Publish.

Drafts and pre-releases are ignored, since GitHub's "latest" endpoint skips them. Tags compare by number, so `1.10` is newer than `1.9`, and `1.1-beta` is older than `1.1`.

"Report a problem" opens your email app addressed to workacckwanrong@gmail.com, with the app version, device model and Android version filled in.

## Tests

- `./gradlew test` runs JVM unit tests:
  - pen filter and input modes
  - stroke geometry
  - library logic (folders, trash, restore, moving folders, asset clean-up)
  - paper catalogue (69 entries, ids, categories), and that every background has a renderer
  - PDF coordinate mapping for all four rotations
  - JSON round trips and the v1 upgrade
  - tool switching
- `./gradlew connectedAndroidTest` (device or emulator, API 34+) drives the page canvas with synthetic touches:
  - finger, mouse and second-stylus rejection
  - finger scrolling
  - cancelled strokes, and cancels from other devices
  - palm before the pen, and palm during a stroke
  - finger modes, and the pen taking over from a finger
  - eraser and undo
  - highlighter
  - per-page undo
  - moving, resizing and rotating a picture

## Limits

- On some tablets the pen and fingers come from the same touchscreen input device. Fingers are still controlled by the input mode, but two pens of the same kind can't be told apart.
- Clef symbols on the Treble, Bass and Grand staff papers come from the system's music font. If the tablet doesn't have one, the staves are drawn without clefs.
- Handwriting-to-text isn't included.

## Project layout

```
app/src/main/java/com/biasnil/stylusnotes/
  MainActivity.kt        pen-button keys, edge-to-edge
  MainViewModel.kt       navigation, library actions, pen settings
  EditorViewModel.kt     the open note: pages, saving, import, export
  StylusNotesApp.kt      shared repositories, pen filter, tools, messages
  canvas/                PageCanvasView, PageRenderer, StrokeRenderer, StrokeGeometry, Tools
  data/                  models, library index + LibraryOps, LibraryRepository, settings
  paper/                 PaperCatalog + renderers (standard/grid/math/music, sports, planners)
  pdf/                   AssetLoader (images + PdfRenderer), PageFactory, Thumbnails, PdfCoords
  update/                UpdateChecker (GitHub latest release), Versions (tag comparison), ProjectLinks
  export/                PDF (Android PdfDocument + PdfBox) and PNG export
  input/                 StylusFilter, pens, side-button keys
  ui/                    library, create sheet, editor, dialogs, settings, pen setup, theme
```
