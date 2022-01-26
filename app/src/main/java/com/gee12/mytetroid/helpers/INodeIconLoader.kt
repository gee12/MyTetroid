package com.gee12.mytetroid.helpers

import android.content.Context
import com.gee12.mytetroid.model.TetroidNode

interface INodeIconLoader {
    fun loadIcon(context: Context, node: TetroidNode)
}