package com.gee12.mytetroid.views.fragments.settings

import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import com.gee12.mytetroid.App
import com.gee12.mytetroid.R
import com.gee12.mytetroid.RecordFieldsSelector
import com.gee12.mytetroid.data.CommonSettings
import com.gee12.mytetroid.views.DateTimeFormatPreference
import com.gee12.mytetroid.views.DateTimeFormatDialog

class SettingsDisplayFragment : TetroidSettingsFragment() {

    companion object {
        private const val DIALOG_FRAGMENT_TAG = "DateTimeFormatPreference"
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.prefs_display, rootKey)
        requireActivity().setTitle(R.string.pref_category_display)

        setHighlightPrefAvailability()

        if (App.isFullVersion()) {
            // добавляем поле "Дата изменения"
            val prefFields = findPreference<Preference>(getString(R.string.pref_key_record_fields_in_list)) as MultiSelectListPreference?
            val arrayId = if (App.isFullVersion()) R.array.record_fields_in_list_entries_pro else R.array.record_fields_in_list_entries
            prefFields?.setEntryValues(arrayId)
            prefFields?.setEntries(arrayId)
        }
        updateSummary(R.string.pref_key_show_record_fields, CommonSettings.getShowRecordFields(context))
        updateSummaryIfContains(R.string.pref_key_record_fields_in_list, getRecordFieldsValuesString())
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            getString(R.string.pref_key_is_highlight_attach) -> {
                // включаем/выключаем выделение записей с файлами
                App.IsHighlightAttach = CommonSettings.isHighlightRecordWithAttach(context)
                setHighlightPrefAvailability()
            }
            getString(R.string.pref_key_is_highlight_crypted_nodes) -> {
                // включаем/выключаем выделение зашифрованных веток
                App.IsHighlightCryptedNodes = CommonSettings.isHighlightEncryptedNodes(context)
                setHighlightPrefAvailability()
            }
            getString(R.string.pref_key_highlight_attach_color) -> {
                // меняем цвет выделения записей с файлами
                App.HighlightAttachColor = CommonSettings.getHighlightColor(context)
            }
            getString(R.string.pref_key_date_format_string) -> {
                // меняем формат даты
                App.DateFormatString = CommonSettings.getDateFormatString(context)
            }
            getString(R.string.pref_key_show_record_fields) -> {
                updateSummary(R.string.pref_key_show_record_fields, CommonSettings.getShowRecordFields(context))
            }
            getString(R.string.pref_key_record_fields_in_list) -> {
                App.RecordFieldsInList = RecordFieldsSelector(context, CommonSettings.getRecordFieldsInList(context))
                updateSummary(
                    R.string.pref_key_record_fields_in_list, getRecordFieldsValuesString(),
                    getString(R.string.pref_record_fields_in_list_summ)
                )
            }
        }
    }

    private fun getRecordFieldsValuesString(): String {
        val arrayId = if (App.isFullVersion()) R.array.record_fields_in_list_entries_pro else R.array.record_fields_in_list_entries
        return App.RecordFieldsInList.joinToString(resources.getStringArray(arrayId), 0)
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