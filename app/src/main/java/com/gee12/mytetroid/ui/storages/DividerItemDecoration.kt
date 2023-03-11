package com.gee12.mytetroid.ui.storages

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.gee12.mytetroid.R
import kotlin.math.roundToInt

class DividerItemDecoration(
    context: Context,
    private val orientation: Int,
    private val spaceBetweenRes: Int,
    private var dividerDrawable: Drawable = ColorDrawable(
        ContextCompat.getColor(context, R.color.recycler_view_divider_color)
    )
) : RecyclerView.ItemDecoration() {

    companion object {
        private const val HORIZONTAL = LinearLayout.HORIZONTAL
        private const val VERTICAL = LinearLayout.VERTICAL
    }

    private val bounds = Rect()

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDraw(c, parent, state)
        if (parent.layoutManager == null) {
            return
        }
        if (orientation == VERTICAL) {
            drawVertical(c, parent)
        } else {
            drawHorizontal(c, parent)
        }
    }

    private fun drawVertical(canvas: Canvas, parent: RecyclerView) {
        canvas.save()
        val left: Int
        val right: Int
        if (parent.clipToPadding) {
            left = parent.paddingLeft
            right = parent.width - parent.paddingRight
            canvas.clipRect(
                left, parent.paddingTop, right,
                parent.height - parent.paddingBottom
            )
        } else {
            left = 0
            right = parent.width
        }
        val childCount = parent.childCount
        if (childCount > 0) {
            for (i in 0 until childCount - 1) {
                val child = parent.getChildAt(i)
                parent.getDecoratedBoundsWithMargins(child, bounds)
                val bottom = bounds.bottom + child.translationY.roundToInt()

                val top = bottom - dividerDrawable.intrinsicHeight
                dividerDrawable.setBounds(left, top, right, bottom)
                dividerDrawable.draw(canvas)
            }
        }
        canvas.restore()
    }

    private fun drawHorizontal(canvas: Canvas, parent: RecyclerView) {
        canvas.save()
        val top: Int
        val bottom: Int
        if (parent.clipToPadding) {
            top = parent.paddingTop
            bottom = parent.height - parent.paddingBottom
            canvas.clipRect(
                parent.paddingLeft, top,
                parent.width - parent.paddingRight, bottom
            )
        } else {
            top = 0
            bottom = parent.height
        }
        val childCount = parent.childCount
        if (childCount > 0) {
            for (i in 0 until childCount - 1) {
                val child = parent.getChildAt(i)
                parent.layoutManager!!.getDecoratedBoundsWithMargins(child, bounds)
                val right = bounds.right + Math.round(child.translationX)
                val left = right - dividerDrawable.intrinsicWidth
                dividerDrawable.setBounds(left, top, right, bottom)
                dividerDrawable.draw(canvas)
            }
        }
        canvas.restore()
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        val space = view.resources.getDimensionPixelSize(spaceBetweenRes)

        when (orientation) {
            RecyclerView.VERTICAL -> {
                outRect.top = space
            }
            RecyclerView.HORIZONTAL -> {
                outRect.left = space
            }
        }
    }

}
