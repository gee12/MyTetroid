package com.gee12.mytetroid.helpers

import android.content.Context
import java.util.*

interface ILocaleHelper {
    fun isRusLanguage(): Boolean
}

class LocaleHelper(context: Context) : ILocaleHelper {

    override fun isRusLanguage() = Locale.getDefault().language == "ru"

}