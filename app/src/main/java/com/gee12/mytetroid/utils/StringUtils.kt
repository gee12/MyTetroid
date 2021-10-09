package com.gee12.mytetroid.utils

import android.content.Context
import com.gee12.mytetroid.R
import com.gee12.mytetroid.model.TetroidObject
import java.util.*

object StringUtils {

    fun getStringFormat(format: String, vararg args: Any?): String {
        return String.format(Locale.getDefault(), format, *args)
    }

    fun getStringFormat(context: Context, format: Int, vararg args: Any?): String {
        return String.format(Locale.getDefault(), context.getString(format), *args)
    }

    fun getStringFromTo(context: Context, from: String?, to: String?): String {
        return getStringFormat(context, R.string.log_from_to_mask, from, to)
    }

    fun getStringTo(context: Context, to: String?): String {
        return getStringFormat(context, R.string.log_to_mask, to)
    }

    fun getIdString(context: Context, obj: TetroidObject?): String {
        return obj?.let { getStringFormat(context, R.string.log_obj_id_mask, obj.id) } ?: ""
    }

    fun getIdNameString(context: Context, obj: TetroidObject?): String {
        return obj?.let { getStringFormat(context, R.string.log_obj_id_name_mask, obj.id, obj.name) } ?: ""
    }
}