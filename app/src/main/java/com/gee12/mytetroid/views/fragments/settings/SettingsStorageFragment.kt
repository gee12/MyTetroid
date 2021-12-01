package com.gee12.mytetroid.views.fragments.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import com.gee12.mytetroid.App
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.data.CommonSettings
import lib.folderpicker.FolderPicker

class SettingsStorageFragment : TetroidSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.prefs_storage, rootKey)
        requireActivity().setTitle(R.string.pref_category_storage)

        findPreference<Preference>(getString(R.string.pref_key_temp_path))
            ?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            if (!checkPermission(Constants.REQUEST_CODE_OPEN_TEMP_PATH)) return@OnPreferenceClickListener true
            selectTrashFolder()
            true
        }

        val loadFavorPref = findPreference<Preference>(getString(R.string.pref_key_is_load_favorites))
        disableIfFree(loadFavorPref!!)

        val keepNodePref = findPreference<Preference>(getString(R.string.pref_key_is_keep_selected_node))
        keepNodePref!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            if (CommonSettings.isLoadFavoritesOnlyDef(context)) {
                baseViewModel.showMessage(getString(R.string.title_not_avail_when_favor))
            }
            true
        }
        if (App.isFullVersion()) {
            keepNodePref.dependency = getString(R.string.pref_key_is_load_favorites)
        }

        updateSummary(R.string.pref_key_temp_path, CommonSettings.getTrashPath(context))
    }

    fun onRequestPermissionsResult(permGranted: Boolean, requestCode: Int) {
        if (permGranted) {
            baseViewModel.log(R.string.log_write_ext_storage_perm_granted)
            when (requestCode) {
//                Constants.REQUEST_CODE_OPEN_STORAGE_PATH -> selectStorageFolder()
                Constants.REQUEST_CODE_OPEN_TEMP_PATH -> selectTrashFolder()
            }
        } else {
            baseViewModel.logWarning(R.string.log_missing_write_ext_storage_permissions, true)
        }
    }

    fun onResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (resultCode != Activity.RESULT_OK) return

        val folderPath = data.getStringExtra(FolderPicker.EXTRA_DATA)

        if (requestCode == Constants.REQUEST_CODE_OPEN_TEMP_PATH) {
            CommonSettings.setTrashPath(context, folderPath)
            CommonSettings.setLastChoosedFolder(context, folderPath)
            updateSummary(R.string.pref_key_temp_path, folderPath)
        } else if (requestCode == Constants.REQUEST_CODE_OPEN_LOG_PATH) {
            CommonSettings.setLogPath(context, folderPath)
            CommonSettings.setLastChoosedFolder(context, folderPath)
            baseViewModel.logger.setLogPath(folderPath)
            updateSummary(R.string.pref_key_log_path, folderPath)
        }
    }

    private fun selectTrashFolder() {
        openFolderPicker(
            getString(R.string.pref_trash_path),
            CommonSettings.getTrashPath(context),
            Constants.REQUEST_CODE_OPEN_TEMP_PATH
        )
    }
}