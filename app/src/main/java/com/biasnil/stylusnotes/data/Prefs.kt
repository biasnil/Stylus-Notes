package com.biasnil.stylusnotes.data

import com.biasnil.stylusnotes.paper.PaperCatalog
import com.biasnil.stylusnotes.paper.PaperSize

enum class ThemeChoice { SYSTEM, LIGHT, DARK, SAKURA, OCEAN, FOREST, MIDNIGHT }

/** How a page is sized when it's opened. */
enum class ZoomMode { FIT_WIDTH, FIT_PAGE, ACTUAL }

enum class DoubleTapAction { RESET_ZOOM, UNDO, NOTHING }

/** What a pen side button does. */
enum class PenAction { NOTHING, TOGGLE_ERASER, HOLD_ERASER, TOGGLE_HIGHLIGHTER, UNDO, REDO }

/** What the back (eraser) end of the pen does. */
enum class EraserEnd { ERASE, PEN, NOTHING }

data class PenButtons(
    val primary: PenAction = PenAction.TOGGLE_ERASER,
    val secondary: PenAction = PenAction.NOTHING,
    val eraserEnd: EraserEnd = EraserEnd.ERASE,
)

data class AppSettings(
    // Note editor
    val resumeLastPage: Boolean = true,
    val firstPageThumbnail: Boolean = false,
    val disableBack: Boolean = false,
    val keepScreenOn: Boolean = false,
    val followOrientation: Boolean = false,
    // Input
    val buttons: PenButtons = PenButtons(),
    /** Key code assigned to the secondary side button (-1 = none). */
    val secondaryKey: Int = -1,
    // Note defaults
    val defaultTemplate: String = PaperCatalog.DEFAULT_ID,
    val paperSize: PaperSize = PaperSize.LETTER,
    val savePagePositions: Boolean = true,
    val defaultZoom: ZoomMode = ZoomMode.FIT_WIDTH,
    val twoFingerDoubleTap: DoubleTapAction = DoubleTapAction.RESET_ZOOM,
    val pdfKeepText: Boolean = false,
    // Casting
    val presentOnExternal: Boolean = false,
    // General
    val theme: ThemeChoice = ThemeChoice.SYSTEM,
    /** Ask GitHub for a newer release when the app starts. */
    val checkUpdates: Boolean = true,
    /** Release tag the user chose to skip. */
    val skippedVersion: String = "",
    // Library
    val sort: SortMode = SortMode.NAME,
)
