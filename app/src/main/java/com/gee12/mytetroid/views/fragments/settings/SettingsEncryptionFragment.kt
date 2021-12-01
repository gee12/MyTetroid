package com.gee12.mytetroid.views.fragments.settings

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.Preference
import com.gee12.mytetroid.R
import com.gee12.mytetroid.data.CommonSettings

class SettingsEncryptionFragment : TetroidSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.prefs_encryption, rootKey)
        requireActivity().setTitle(R.string.pref_category_crypt)

        // когда запрашивать пароль
        findPreference<Preference>(getString(R.string.pref_key_when_ask_password))
            ?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            if (CommonSettings.isSaveMiddlePassHashLocalDef(context)) {
                baseViewModel.showMessage(getString(R.string.title_not_avail_when_save_pass))
            }
            true
        }
        updateSummary(
            R.string.pref_key_when_ask_password,
            if (CommonSettings.isSaveMiddlePassHashLocalDef(context)) getString(R.string.pref_when_ask_password_summ)
            else CommonSettings.getWhenAskPass(context)
        )
    }

    /**
     * Обработчик изменения настроек.
     * Чтобы работало нужно переопределить onResume() и onPause()
     * и дописать register/unregister настроек.
     * @param sharedPreferences
     * @param key
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == getString(R.string.pref_key_is_save_pass_hash_local)) {
//            setPINCodePrefAvailability();
            updateSummary(
                R.string.pref_key_when_ask_password,
                if (CommonSettings.isSaveMiddlePassHashLocalDef(context)) getString(R.string.pref_when_ask_password_summ)
                else CommonSettings.getWhenAskPass(context)
            )
        } else if (key == getString(R.string.pref_key_when_ask_password)) {
            updateSummary(R.string.pref_key_when_ask_password, CommonSettings.getWhenAskPass(context))
        }
    }

}