package com.gee12.mytetroid.ui.settings.storage

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.documentfile.provider.DocumentFile
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.extensions.uriToAbsolutePath
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.model.permission.PermissionRequestCode
import com.gee12.mytetroid.ui.base.ITetroidFileStorage
import com.gee12.mytetroid.ui.base.TetroidStorageSettingsFragment
import com.gee12.mytetroid.ui.base.views.prefs.DisabledCheckBoxPreference
import com.gee12.mytetroid.ui.base.views.prefs.DisabledPreference
import com.gee12.mytetroid.ui.dialogs.AskDialogs
import com.gee12.mytetroid.ui.dialogs.node.NodeChooserDialog
import com.gee12.mytetroid.ui.main.MainActivity

class StorageMainSettingsFragment : TetroidStorageSettingsFragment(), ITetroidFileStorage {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
    }

    // region Storage

    override fun onStorageFoundInBase(storage: TetroidStorage) {
        setTitle(R.string.pref_category_main, storage.name)

        // устанавливаем preferenceDataStore после onCreate(), но перед setPreferencesFromResource()
        preferenceManager?.preferenceDataStore = viewModel.prefsDataStore

        setPreferencesFromResource(R.xml.storage_prefs_main, null)

        // выбор каталога хранилища
        findPreference<Preference>(getString(R.string.pref_key_storage_path))?.also {
            it.isCopyingEnabled = true
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                selectStorageFolder()
                true
            }
        }

        findPreference<DisabledCheckBoxPreference>(getString(R.string.pref_key_is_read_only))?.also {
            // TODO: принудительно отключаем (пока)
            it.isEnabled = false
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                viewModel.showMessage(R.string.title_not_implemented_yet)
                true
            }
        }

        // каталог корзины
        findPreference<Preference>(getString(R.string.pref_key_storage_path))?.also {
            it.isCopyingEnabled = true
        }

        // диалог очистки каталога корзины
        findPreference<Preference>(getString(R.string.pref_key_clear_trash))?.also {
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                AskDialogs.showYesDialog(
                    context = requireContext(),
                    messageResId = R.string.ask_clear_trash,
                    onApply = {
                        viewModel.clearTrashFolder()
                    },
                )
                true
            }
        }

        // ветка для быстрых записей
        findPreference<DisabledPreference>(getString(R.string.pref_key_quickly_node_id))?.also {
            it.isEnabled = false
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                when {
                    !viewModel.checkStorageIsReady(
                        checkIsFavorMode = true,
                        showMessage = true,
                    ) -> Unit
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
        findPreference<CheckBoxPreference>(getString(R.string.pref_key_is_load_favorites))?.also {
            it.disableIfFree()
        }

        // открывать прошлую ветку
        findPreference<CheckBoxPreference>(getString(R.string.pref_key_is_keep_selected_node))?.also {
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                if (viewModel.isLoadFavoritesOnly()) {
                    viewModel.showMessage(R.string.title_not_avail_when_favor)
                }
                true
            }
            if (baseViewModel.buildInfoProvider.isFullVersion()) {
                it.dependency = getString(R.string.pref_key_is_load_favorites)
            }
        }

        // добавляем подписи, если значения установлены
        val path = viewModel.getStorageUri()?.uriToAbsolutePath(requireContext())
        updateSummary(R.string.pref_key_storage_path, path)
        updateSummary(R.string.pref_key_storage_name, viewModel.getStorageName())
        updateSummary(R.string.pref_key_temp_path, viewModel.getStorageTrashFolderPath().fullPath)
        updateSummary(R.string.pref_key_quickly_node_id, viewModel.getQuicklyNodeNameOrMessage(), getString(R.string.pref_quickly_node_summ))
    }

    override fun onStorageInited(storage: TetroidStorage) {
        // ветка для быстрых записей
        findPreference<DisabledPreference>(getString(R.string.pref_key_quickly_node_id))?.also {
            it.isEnabled = viewModel.checkStorageIsReady(
                checkIsFavorMode = true,
                showMessage = false
            )
        }

        updateSummary(R.string.pref_key_quickly_node_id, viewModel.getQuicklyNodeNameOrMessage(), getString(R.string.pref_quickly_node_summ))
    }

    // endregion Storage

    // region File

    private fun selectStorageFolder() {
        settingsActivity?.openFolderPicker(
            requestCode = PermissionRequestCode.CHANGE_STORAGE_FOLDER,
            initialPath = viewModel.storageFolder?.uri?.toString(),
        )
    }

    override fun isUseFileStorage() = true

    override fun onStorageAccessGranted(requestCode: Int, root: DocumentFile) {}

    override fun onFolderSelected(requestCode: Int, folder: DocumentFile) {
        when (PermissionRequestCode.fromCode(requestCode)) {
            PermissionRequestCode.CHANGE_STORAGE_FOLDER -> {
                viewModel.updateStorageFolder(folder)
            }
            else -> Unit
        }
    }

    override fun onFileSelected(requestCode: Int, files: List<DocumentFile>) {}

    // endregion File

    private fun showNodeChooserDialog() {
        NodeChooserDialog(
            node = viewModel.quicklyNode,
            canCrypted = false,
            canDecrypted = false,
            rootOnly = true,
            storageId = viewModel.getStorageId(),
            onApply = { node ->
                // устанавливаем ветку, если все хорошо
                viewModel.quicklyNode = node
            },
            onProblem = { code ->
                // если хранилище недозагружено, спрашиваем о действиях
                AskDialogs.showYesDialog(
                    context = requireContext(),
                    messageResId = if (code == NodeChooserDialog.ProblemType.LOAD_STORAGE) R.string.ask_load_storage else R.string.ask_load_all_nodes,
                    onApply = {
                        // возвращаемся в MainActivity
                        val intent = Intent(requireContext(), MainActivity::class.java)
                        intent.action = Constants.ACTION_STORAGE_SETTINGS
                        when (code) {
                            NodeChooserDialog.ProblemType.LOAD_STORAGE -> {
                                intent.putExtra(Constants.EXTRA_IS_LOAD_STORAGE, true)
                                intent.putExtra(Constants.EXTRA_IS_LOAD_ALL_NODES, true)
                            }
                            NodeChooserDialog.ProblemType.LOAD_ALL_NODES -> {
                                intent.putExtra(Constants.EXTRA_IS_LOAD_ALL_NODES, true)
                            }
                        }
                        intent.putExtra(Constants.EXTRA_STORAGE_ID, viewModel.getStorageId())
                        requireActivity().setResult(Activity.RESULT_OK, intent)
                        requireActivity().startActivity(intent)
                        requireActivity().finish()
                    },
                )
            },
        ).showIfPossible(parentFragmentManager)
    }

    override fun onUpdateStorageFieldEvent(key: String, value: String) {
        when (key) {
            // основное
            getString(R.string.pref_key_storage_name) -> {
                updateSummary(key, value)
                setTitle(R.string.pref_category_main, value)
            }
            getString(R.string.pref_key_quickly_node_id) -> {
                updateSummary(key, viewModel.getQuicklyNodeName(), getString(R.string.pref_quickly_node_summ))
            }
        }
    }

}