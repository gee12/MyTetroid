package com.gee12.mytetroid.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import com.gee12.htmlwysiwygeditor.ActionButtonSize
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.extensions.getTitle
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.domain.AppThemeHelper
import com.gee12.mytetroid.model.enums.AppTheme
import com.gee12.mytetroid.ui.base.views.prefs.DateTimeFormatPreference
import com.gee12.mytetroid.ui.base.views.DateTimeFormatDialog
import com.gee12.mytetroid.ui.base.TetroidSettingsFragment

class SettingsDisplayFragment : TetroidSettingsFragment() {

    companion object {
        private const val DIALOG_FRAGMENT_TAG = "DateTimeFormatPreference"
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.prefs_display, rootKey)
        requireActivity().setTitle(R.string.pref_category_display)

        setHighlightPrefAvailability()

        if (baseViewModel.buildInfoProvider.isFullVersion()) {
            // добавляем поле "Дата изменения"
            (findPreference<Preference>(getString(R.string.pref_key_record_fields_in_list)) as MultiSelectListPreference?)?.also {
                val arrayId = if (baseViewModel.buildInfoProvider.isFullVersion()) {
                    R.array.record_fields_in_list_entries_pro
                } else {
                    R.array.record_fields_in_list_entries
                }
                it.setEntryValues(arrayId)
                it.setEntries(arrayId)
            }
        }

        // тема
        findPreference<ListPreference>(getString(R.string.pref_key_theme))?.also {
            it.entryValues = AppTheme.values().map { it.id }.toTypedArray()
            it.setDefaultValue(AppTheme.LIGHT.id)
            it.summary = settingsManager.getTheme().getString(resourcesProvider)
        }

        // панель со свойствами записи
        findPreference<ListPreference>(getString(R.string.pref_key_show_record_fields))?.also {
            it.setDefaultValue(getString(R.string.pref_show_record_fields_no))
            if (settingsManager.isHasShowRecordFieldsValue()) {
                it.summary = settingsManager.getShowRecordFields()
            }
        }

        // размер кнопок в toolbar в редакторе
        findPreference<ListPreference>(getString(R.string.pref_key_editor_toolbar_buttons_size))?.also {
            it.entryValues = ActionButtonSize.values().map { it.id.toString() }.toTypedArray()
            it.setDefaultValue(ActionButtonSize.MEDIUM.id.toString())
            if (settingsManager.isHasEditorButtonsSizeValue()) {
                it.summary = settingsManager.getEditorButtonsSize().getTitle(resourcesProvider)
            }
        }

        updateSummaryIfContains(R.string.pref_key_record_fields_in_list, getRecordFieldsValuesString())
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String) {
        when (key) {
            getString(R.string.pref_key_is_highlight_attach) -> {
                // включаем/выключаем выделение записей с файлами
                // TODO
                setHighlightPrefAvailability()
            }
            getString(R.string.pref_key_is_highlight_crypted_nodes) -> {
                // включаем/выключаем выделение зашифрованных веток
                // TODO
                setHighlightPrefAvailability()
            }
            getString(R.string.pref_key_highlight_attach_color) -> {
                // TODO
            }
            getString(R.string.pref_key_date_format_string) -> {
                // меняем формат даты
                // TODO
            }
            getString(R.string.pref_key_theme) -> {
                settingsManager.getTheme().also { theme ->
                    AppThemeHelper.setTheme(theme)
                    updateSummary(R.string.pref_key_theme, theme.getString(resourcesProvider))
                }
            }
            getString(R.string.pref_key_show_record_fields) -> {
                updateSummary(R.string.pref_key_show_record_fields, CommonSettings.getShowRecordFields(context))
            }
            getString(R.string.pref_key_editor_toolbar_buttons_size) -> {
                updateSummary(
                    R.string.pref_key_editor_toolbar_buttons_size,
                    settingsManager.getEditorButtonsSize().getTitle(resourcesProvider)
                )
            }
            getString(R.string.pref_key_record_fields_in_list) -> {
                // TODO
                updateSummary(
                    R.string.pref_key_record_fields_in_list, getRecordFieldsValuesString(),
                    getString(R.string.pref_record_fields_in_list_summ)
                )
            }
        }
    }

    private fun getRecordFieldsValuesString(): String {
        val arrayId = if (baseViewModel.buildInfoProvider.isFullVersion()) R.array.record_fields_in_list_entries_pro else R.array.record_fields_in_list_entries
        val recordFieldsInList = baseViewModel.settingsManager.getRecordFieldsSelector()
        return recordFieldsInList.joinToString(resources.getStringArray(arrayId), 0)
    }

    private fun setHighlightPrefAvailability() {
        findPreference<Preference>(getString(R.string.pref_key_highlight_attach_color))!!.isEnabled = (
                CommonSettings.isHighlightRecordWithAttach(context)
                        || CommonSettings.isHighlightEncryptedNodes(context))
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (parentFragmentManager.findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) return

        if (preference is DateTimeFormatPreference) {
            val f: DialogFragment = DateTimeFormatDialog.newInstance(preference.getKey())
            f.setTargetFragment(this, 0)
            f.show(parentFragmentManager, DIALOG_FRAGMENT_TAG)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }
}