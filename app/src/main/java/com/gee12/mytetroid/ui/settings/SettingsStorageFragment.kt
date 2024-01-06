package com.gee12.mytetroid.ui.settings

import android.os.Bundle
import androidx.preference.Preference
import com.gee12.mytetroid.R
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.ui.base.TetroidSettingsFragment
import com.gee12.mytetroid.ui.dialogs.AskDialogs

class SettingsStorageFragment : TetroidSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.prefs_storage, rootKey)
        requireActivity().setTitle(R.string.pref_category_storage)

        findPreference<Preference>(getString(R.string.pref_key_temp_path))?.also {
            it.isCopyingEnabled = true
        }

        // диалог очистки каталога корзины у всех хранилищ
        findPreference<Preference>(getString(R.string.pref_key_clear_trash))?.also {
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                AskDialogs.showYesDialog(
                    context = requireContext(),
                    messageResId = R.string.ask_clear_trash_all_storages,
                    onApply = {
                        baseViewModel.clearTrashFolders()
                    },
                )
                true
            }
        }

        findPreference<Preference>(getString(R.string.pref_key_is_load_favorites))?.also {
            it.disableIfFree()
        }

        findPreference<Preference>(getString(R.string.pref_key_is_keep_selected_node))?.also {
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                if (CommonSettings.isLoadFavoritesOnlyDef(context)) {
                    baseViewModel.showMessage(R.string.title_not_avail_when_favor)
                }
                true
            }
            if (baseViewModel.buildInfoProvider.isFullVersion()) {
                it.dependency = getString(R.string.pref_key_is_load_favorites)
            }
        }

        updateSummary(R.string.pref_key_temp_path, baseViewModel.appPathProvider.getPathToTrashFolder().fullPath)
    }

}