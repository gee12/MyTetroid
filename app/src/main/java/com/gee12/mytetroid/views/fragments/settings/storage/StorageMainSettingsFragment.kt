package com.gee12.mytetroid.views.fragments.settings.storage

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import com.gee12.mytetroid.App
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.views.DisabledCheckBoxPreference
import com.gee12.mytetroid.views.activities.MainActivity
import com.gee12.mytetroid.views.dialogs.AskDialogs
import com.gee12.mytetroid.views.dialogs.node.NodeChooserDialog
import com.gee12.mytetroid.views.dialogs.node.NodeDialogs.INodeChooserResult
import com.gee12.mytetroid.views.dialogs.storage.StorageDialogs
import lib.folderpicker.FolderPicker
import org.jsoup.internal.StringUtil

class StorageMainSettingsFragment : TetroidStorageSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
    }

    override fun onStorageFoundInBase(storage: TetroidStorage) {
        setTitle(R.string.pref_category_main, storage.name)

        // устанавливаем preferenceDataStore после onCreate(), но перед setPreferencesFromResource()
        preferenceManager?.preferenceDataStore = viewModel.prefsDataStore

        setPreferencesFromResource(R.xml.storage_prefs_main, null)

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
            viewModel.showMessage(R.string.title_not_implemented_yet)
            true
        }

        // выбор каталога корзины
        findPreference<Preference>(getString(R.string.pref_key_temp_path))
            ?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            if (checkPermission(Constants.REQUEST_CODE_OPEN_TEMP_PATH)) {
                selectTrashFolder()
            }
            true
        }

        // диалог очистки каталога корзины
        findPreference<Preference>(getString(R.string.pref_key_clear_trash))
            ?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            AskDialogs.showYesDialog(context, {
                viewModel.clearTrashFolder()
            }, R.string.ask_clear_trash)
            true
        }

        // ветка для быстрых записей
        findPreference<Preference>(getString(R.string.pref_key_quickly_node_id))?.apply {
            isEnabled = viewModel.checkStorageIsReady(
                checkIsFavorMode = true,
                showMessage = false
            )
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                when {
                    !viewModel.checkStorageIsReady(
                        checkIsFavorMode = true,
                        showMessage = true
                    ) -> {}
                    !viewModel.isStorageDefault() -> {
                        viewModel.showMessage(R.string.pref_quickly_node_not_available)
                    }
                    else -> {
                        showNodeChooserDialog()
                    }
                }
                true
            }
        }
        viewModel.updateQuicklyNode()

        // загрузка только избранного (отключаем для Free)
        findPreference<CheckBoxPreference>(getString(R.string.pref_key_is_load_favorites))?.let {
            disableIfFree(it)
        }

        // открывать прошлую ветку
        val prefIsKeepLastNode = findPreference<CheckBoxPreference>(getString(R.string.pref_key_is_keep_selected_node))
        prefIsKeepLastNode?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            if (viewModel.isLoadFavoritesOnly()) {
                viewModel.showMessage(R.string.title_not_avail_when_favor)
            }
            true
        }
        if (baseViewModel.appBuildHelper.isFullVersion()) {
            prefIsKeepLastNode?.dependency = getString(R.string.pref_key_is_load_favorites)
        }

        // добавляем подписи, если значения установлены
        updateSummary(R.string.pref_key_storage_path, viewModel.getStoragePath())
        updateSummary(R.string.pref_key_storage_name, viewModel.getStorageName())
        updateSummary(R.string.pref_key_temp_path, viewModel.getTrashPath())
        updateSummary(R.string.pref_key_quickly_node_id, viewModel.getQuicklyNodeNameOrMessage(), getString(R.string.pref_quickly_node_summ))
    }

    private fun showNodeChooserDialog() {
        val nodeCallback = object : NodeChooserDialog.Result() {
            override fun onApply(node: TetroidNode?) {
                // устанавливаем ветку, если все хорошо
                viewModel.quicklyNode = node
            }

            override fun onProblem(code: Int) {
                // если хранилище недозагружено, спрашиваем о действиях
                val mesId = if (code == INodeChooserResult.LOAD_STORAGE) R.string.ask_load_storage else R.string.ask_load_all_nodes
                AskDialogs.showYesDialog(context, {

                    // возвращаемся в MainActivity
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    intent.setAction(Constants.ACTION_STORAGE_SETTINGS)
                    when (code) {
                        INodeChooserResult.LOAD_STORAGE -> {
                            intent.putExtra(Constants.EXTRA_IS_LOAD_STORAGE, true)
                            intent.putExtra(Constants.EXTRA_IS_LOAD_ALL_NODES, true)
                        }
                        INodeChooserResult.LOAD_ALL_NODES -> intent.putExtra(Constants.EXTRA_IS_LOAD_ALL_NODES, true)
                    }
                    intent.putExtra(Constants.EXTRA_STORAGE_ID, viewModel.getStorageId())
                    requireActivity().setResult(Activity.RESULT_OK, intent)
                    requireActivity().startActivity(intent)
                    requireActivity().finish()
                }, mesId)
            }
        }
        NodeChooserDialog(
            node = viewModel.quicklyNode,
            canCrypted = false,
            canDecrypted = false,
            rootOnly = true,
            storageId = viewModel.getStorageId(),
            callback = nodeCallback
        ).showIfPossible(parentFragmentManager)
    }

    override fun onUpdateStorageFieldEvent(key: String, value: String) {
        when (key) {
            // основное
            getString(R.string.pref_key_storage_path) -> {
                updateSummary(key, value)
                viewModel.onStoragePathChanged()
            }
            getString(R.string.pref_key_storage_name) -> {
                updateSummary(key, value)
                setTitle(R.string.pref_category_main, value)
            }
            getString(R.string.pref_key_temp_path) -> updateSummary(key, value)
            getString(R.string.pref_key_quickly_node_id) -> updateSummary(key, viewModel.getQuicklyNodeName(), getString(R.string.pref_quickly_node_summ))
        }
    }

    fun onRequestPermissionsResult(permGranted: Boolean, requestCode: Int) {
        if (permGranted) {
            baseViewModel.log(R.string.log_write_ext_storage_perm_granted, true)
            when (requestCode) {
                Constants.REQUEST_CODE_OPEN_STORAGE_PATH -> selectStorageFolder()
                Constants.REQUEST_CODE_OPEN_TEMP_PATH -> selectTrashFolder()
            }
        } else {
            baseViewModel.logWarning(R.string.log_missing_write_ext_storage_permissions, true)
        }
    }

    fun onResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (resultCode != Activity.RESULT_OK) return

        val path = data.getStringExtra(FolderPicker.EXTRA_DATA).orEmpty()
        when (requestCode) {
            Constants.REQUEST_CODE_CREATE_STORAGE_PATH,
            Constants.REQUEST_CODE_OPEN_STORAGE_PATH -> {
                val isCreate = (requestCode == Constants.REQUEST_CODE_CREATE_STORAGE_PATH)
                viewModel.updateStorageOption(getString(R.string.pref_key_storage_path), path)
                CommonSettings.setLastChoosedFolder(context, path)
                onStoragePathChanged(path, isCreate)
            }
            Constants.REQUEST_CODE_OPEN_TEMP_PATH -> {
                viewModel.updateStorageOption(getString(R.string.pref_key_temp_path), path)
                CommonSettings.setLastChoosedFolder(context, path)
            }
        }
    }

    private fun onStoragePathChanged(path: String, isCreateStorageFiles: Boolean) {
        if (isCreateStorageFiles) {
            AskDialogs.showCreateStorageFilesDialog(requireContext(), path) {
                viewModel.storage?.let {
                    it.isNew = true
                    viewModel.startInitStorage(it)
                }
            }
        }
    }

    private fun selectStorageFolder() {
        // спрашиваем: создать или выбрать хранилище ?
        StorageDialogs.createStorageSelectionDialog(context, object : StorageDialogs.IItemClickListener {
            override fun onItemClick(isNew: Boolean) {
                openFolderPicker(
                    getString(R.string.title_storage_folder),
                    viewModel.getStoragePath(),
                    isNew
                )
            }
        })
    }

    protected fun openFolderPicker(title: String?, location: String, isNew: Boolean) {
        val path = if (!StringUtil.isBlank(location)) location else viewModel.getLastFolderPathOrDefault(true)
        val intent = Intent(context, FolderPicker::class.java)
        intent.putExtra(FolderPicker.EXTRA_TITLE, title)
        intent.putExtra(FolderPicker.EXTRA_LOCATION, path)
        if (isNew) {
            intent.putExtra(FolderPicker.EXTRA_EMPTY_FOLDER, true)
        } else {
            intent.putExtra(FolderPicker.EXTRA_DESCRIPTION, getString(R.string.title_storage_path_desc))
        }
        requireActivity().startActivityForResult(
            intent,
            if (isNew) Constants.REQUEST_CODE_CREATE_STORAGE_PATH else Constants.REQUEST_CODE_OPEN_STORAGE_PATH
        )
    }

    private fun selectTrashFolder() {
        openFolderPicker(
            getString(R.string.pref_trash_path),
            viewModel.getTrashPath(),
            Constants.REQUEST_CODE_OPEN_TEMP_PATH
        )
    }

}