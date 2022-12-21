package com.gee12.mytetroid.domain.provider

import android.content.Context
import java.util.*

interface ILocaleProvider {
    fun isRusLanguage(): Boolean
}

class LocaleProvider(context: Context) : ILocaleProvider {

    override fun isRusLanguage() = Locale.getDefault().language == "ru"

}