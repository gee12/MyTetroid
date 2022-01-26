package com.gee12.mytetroid.repo

import android.content.Context
import com.gee12.mytetroid.common.utils.FileUtils
import com.gee12.mytetroid.data.settings.CommonSettings
import org.jsoup.internal.StringUtil
import java.io.File

class CommonSettingsRepo(
    private val context: Context
) {

    fun getLastFolderPathOrDefault(forWrite: Boolean): String? {
        val lastFolder = CommonSettings.getLastChoosedFolderPath(context)
        return if (!StringUtil.isBlank(lastFolder) && File(lastFolder).exists()) lastFolder
        else FileUtils.getExternalPublicDocsOrAppDir(context, forWrite)
    }

}