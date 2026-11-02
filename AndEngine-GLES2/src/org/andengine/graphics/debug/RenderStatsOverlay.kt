package org.andengine.graphics.debug

import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView

/**
 * Shows [RenderStats] as a plain Android [TextView] on top of the GL surface.
 *
 * It is deliberately not drawn with GL, so the overlay does not show up in its own numbers.
 * The view is not clickable, touches pass through to the game.
 */
object RenderStatsOverlay {

    /**
     * Adds the overlay as the top-most child of [parent] and enables [RenderStats].
     */
    @JvmStatic
    fun attach(parent: ViewGroup): TextView {
        val context = parent.context
        val density = context.resources.displayMetrics.density

        val view = TextView(context).apply {
            typeface = Typeface.MONOSPACE
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            setTextColor(Color.WHITE)
            setBackgroundColor(0x99000000.toInt())
            val padding = (4 * density).toInt()
            setPadding(padding, padding, padding, padding)
            isClickable = false
            isFocusable = false
        }

        parent.addView(view, ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            val margin = (48 * density).toInt()
            setMargins(margin, margin, 0, 0)
        })

        RenderStats.listener = RenderStats.Listener { summary ->
            view.post { view.text = summary }
        }
        RenderStats.reset()
        RenderStats.enabled = true

        return view
    }

    /** Disables [RenderStats] and removes the overlay. */
    @JvmStatic
    fun detach(view: TextView) {
        RenderStats.enabled = false
        RenderStats.listener = null
        (view.parent as? ViewGroup)?.removeView(view)
    }
}
