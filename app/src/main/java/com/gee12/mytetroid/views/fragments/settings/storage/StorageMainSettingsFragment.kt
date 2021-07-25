package com.gee12.mytetroid.views.fragments.settings.storage

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import com.gee12.mytetroid.App
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.data.DataManager
import com.gee12.mytetroid.logs.ILogger
import com.gee12.mytetroid.logs.LogManager
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.viewmodels.StorageSettingsViewModel
import com.gee12.mytetroid.viewmodels.StoragesViewModelFactory
import com.gee12.mytetroid.views.DisabledCheckBoxPreference
import com.gee12.mytetroid.views.Message
import com.gee12.mytetroid.views.dialogs.AskDialogs
import com.gee12.mytetroid.views.dialogs.NodeDialogs
import com.gee12.mytetroid.views.dialogs.NodeDialogs.INodeChooserResult
import com.gee12.mytetroid.views.dialogs.StorageDialogs
import com.gee12.mytetroid.views.fragments.settings.TetroidSettingsFragment
import lib.folderpicker.FolderPicker
import org.jsoup.internal.StringUtil

class StorageMainSettingsFragment : TetroidSettingsFragment() {

    private lateinit var mViewModel: StorageSettingsViewModel
//    private val mViewModel: StorageViewModel by activityViewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        mViewModel = ViewModelProvider(activity!!, StoragesViewModelFactory(application))
            .get(StorageSettingsViewModel::class.java)
        // устанавливаем preferenceDataStore после onCreate(), но перед setPreferencesFromResource()
        preferenceManager?.preferenceDataStore = mViewModel.prefsDataStore

        setPreferencesFromResource(R.xml.storage_prefs_main, rootKey)
        setTitle(R.string.pref_category_main, mViewModel.getStorageName())

        // выбор каталога хранилища
        findPreference<Preference>(getString(R.string.pref_key_storage_path))?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            if (checkPermission(Constants.REQUEST_CODE_OPEN_STORAGE_PATH)) {
                selectStorageFolder()
            }
            true
        }

        val prefIsReadOnly = findPreference<DisabledCheckBoxPreference>(getString(R.string.pref_key_is_read_only))
        // TODO: принудительно отключаем (пока)
        prefIsReadOnly?.isEnabled = false
        prefIsReadOnly?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            Message.show(context, getString(R.string.title_not_implemented_yet), Toast.LENGTH_SHORT)
            true
        }

        // выбор каталога корзины
        findPreference<Preference>(getString(R.string.pref_key_temp_path))?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            if (checkPermission(Constants.REQUEST_CODE_OPEN_TEMP_PATH)) {
                selectTrashFolder()
            }
            true
        }

        // диалог очистки каталога корзины
        findPreference<Preference>(getString(R.string.pref_key_clear_trash))?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            AskDialogs.showYesDialog(context, {
                if (mViewModel.clearTrashFolder()) {
                    LogManager.log(mContext, R.string.title_trash_cleared, ILogger.Types.INFO, Toast.LENGTH_SHORT)
                } else {
                    LogManager.log(mContext, R.string.title_trash_clear_error, ILogger.Types.ERROR, Toast.LENGTH_LONG)
                }
            }, R.string.ask_clear_trash)
            true
        }

        // ветка для быстрых записей
        findPreference<Preference>(getString(R.string.pref_key_quickly_node_id))?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            // диалог выбора ветки
            NodeDialogs.createNodeChooserDialog(
                context, mViewModel.getQuicklyNode(),
                false, false, true, object : INodeChooserResult {
                    override fun onApply(node: TetroidNode) {
                        // устанавливаем ветку, если все хорошо
                        mViewModel.setQuicklyNode(node)
                    }

                    override fun onProblem(code: Int) {
                        // если хранилище недозагружено, спрашиваем о действиях
                        val mesId = if (code == INodeChooserResult.LOAD_STORAGE) R.string.ask_load_storage else R.string.ask_load_all_nodes
                        AskDialogs.showYesDialog(context, {

                            // возвращаемся в MainActivity
                            val intent = Intent()
                            when (code) {
                                INodeChooserResult.LOAD_STORAGE -> intent.putExtra(Constants.EXTRA_IS_LOAD_STORAGE, true)
                                INodeChooserResult.LOAD_ALL_NODES -> intent.putExtra(Constants.EXTRA_IS_LOAD_ALL_NODES, true)
                            }
                            activity!!.setResult(Activity.RESULT_OK, intent)
                            activity!!.finish()
                        }, mesId)
                    }
                })
            true
        }
        mViewModel.updateQuicklyNode()

        // загрузка только избранного (отключаем для Free)
        val prefIsLoadFavorites = findPreference<CheckBoxPreference>(getString(R.string.pref_key_is_load_favorites))
        disableIfFree(prefIsLoadFavorites)

        // открывать прошлую ветку
        val prefIsKeepLastNode = findPreference<CheckBoxPreference>(getString(R.string.pref_key_is_keep_selected_node))
        prefIsKeepLastNode?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            if (mViewModel.isLoadFavoritesOnly()) {
                Message.show(context, getString(R.string.title_not_avail_when_favor), Toast.LENGTH_SHORT)
            }
            true
        }
        if (App.isFullVersion()) {
            prefIsKeepLastNode?.dependency = getString(R.string.pref_key_is_load_favorites)
        }

        // добавляем подписи, если значения установлены
        updateSummary(R.string.pref_key_storage_path, mViewModel.getStoragePath())
        updateSummary(R.string.pref_key_storage_name, mViewModel.getStorageName())
        updateSummary(R.string.pref_key_temp_path, mViewModel.getTrashPath())
        updateSummary(R.string.pref_key_quickly_node_id, mViewModel.getQuicklyNodeName())

        mViewModel.updateStorageField.observe(this, { pair ->
            val key = pair.first
            val value = pair.second.toString()
            when (key) {
                // основное
                getString(R.string.pref_key_storage_path) -> updateSummary(key, value)
                getString(R.string.pref_key_storage_name) -> {
                    updateSummary(key, value)
                    setTitle(R.string.pref_category_main, value)
                }
                getString(R.string.pref_key_temp_path) -> updateSummary(key, value)
                // синхронизация
                getString(R.string.pref_key_app_for_sync) -> updateSummary(key, value)
                getString(R.string.pref_key_sync_command) -> updateSummary(key, value, getString(R.string.pref_sync_command_summ))
            }
        })
    }

    fun onRequestPermissionsResult(permGranted: Boolean, requestCode: Int) {
        if (permGranted) {
            LogManager.log(mContext, R.string.log_write_ext_storage_perm_granted, ILogger.Types.INFO)
            when (requestCode) {
                Constants.REQUEST_CODE_OPEN_STORAGE_PATH -> selectStorageFolder()
                Constants.REQUEST_CODE_OPEN_TEMP_PATH -> selectTrashFolder()
            }
        } else {
            LogManager.log(mContext, R.string.log_missing_write_ext_storage_permissions, ILogger.Types.WARNING, Toast.LENGTH_SHORT)
        }
    }

    private fun selectStorageFolder() {
        // спрашиваем: создать или выбрать хранилище ?
        StorageDialogs.createStorageSelectionDialog(context) { isNew: Boolean ->
            openFolderPicker(
                getString(R.string.title_storage_folder),
                mViewModel.getStoragePath(),
                isNew
            )
        }
    }

    protected fun openFolderPicker(title: String?, location: String, isNew: Boolean) {
        val path = if (!StringUtil.isBlank(location)) location else DataManager.getLastFolderPathOrDefault(context, true)
        val intent = Intent(context, FolderPicker::class.java)
        intent.putExtra(FolderPicker.EXTRA_TITLE, title)
        intent.putExtra(FolderPicker.EXTRA_LOCATION, path)
        if (isNew) {
            intent.putExtra(FolderPicker.EXTRA_EMPTY_FOLDER, true)
        } else {
            intent.putExtra(FolderPicker.EXTRA_DESCRIPTION, getString(R.string.title_storage_path_desc))
        }
        activity!!.startActivityForResult(
            intent,
            if (isNew) Constants.REQUEST_CODE_CREATE_STORAGE_PATH else Constants.REQUEST_CODE_OPEN_STORAGE_PATH
        )
    }

    private fun selectTrashFolder() {
        openFolderPicker(
            getString(R.string.pref_trash_path),
            mViewModel.getTrashPath(),
            Constants.REQUEST_CODE_OPEN_TEMP_PATH
        )
    }
}