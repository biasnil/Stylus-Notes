package com.biasnil.stylusnotes.canvas

import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import com.biasnil.stylusnotes.data.Page

/**
 * Shows the open page, without any toolbar, on a second screen (HDMI, USB-C or a
 * wireless display). Nothing can be drawn on it; it mirrors what's committed on the tablet.
 */
class PagePresentation(context: Context, display: Display, private val assets: PageCanvasView.Assets) :
    Presentation(context, display) {

    private var view: PageCanvasView? = null
    private var pending: Page? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val v = PageCanvasView(context).apply {
            assets = this@PagePresentation.assets
            filter = null // read-only
            restoreViews = false
            zoomMode = com.biasnil.stylusnotes.data.ZoomMode.FIT_PAGE
            deskColor = 0xFF000000.toInt()
        }
        view = v
        setContentView(v)
        pending?.let { v.mirror(it) }
    }

    fun showPage(page: Page) {
        pending = page
        view?.mirror(page)
    }

    fun onAssetsLoaded() {
        view?.onAssetsLoaded()
    }

    companion object {
        /** First display that can show a presentation, or null. */
        fun findDisplay(context: Context): Display? =
            context.getSystemService(DisplayManager::class.java)
                .getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
                .firstOrNull()
    }
}
