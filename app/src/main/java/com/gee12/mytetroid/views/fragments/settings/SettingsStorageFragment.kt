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

//        Preference storageFolderPicker = findPreference(getString(R.string.pref_key_storage_path));
//        storageFolderPicker.setOnPreferenceClickListener(preference -> {
//            if (!checkPermission(SettingsFragment.REQUEST_CODE_OPEN_STORAGE_PATH))
//                return true;
//            selectStorageFolder();
//            return true;
//        });

        findPreference<Preference>(getString(R.string.pref_key_temp_path))
            ?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            if (!checkPermission(Constants.REQUEST_CODE_OPEN_TEMP_PATH)) return@OnPreferenceClickListener true
            selectTrashFolder()
            true
        }

//        findPreference(getString(R.string.pref_key_clear_trash))
//                .setOnPreferenceClickListener(preference -> {
//                    AskDialogs.showYesDialog(getContext(), () -> {
//                        if (DataManager.clearTrashFolder(getContext())) {
//                            LogManager.log(mContext, R.string.title_trash_cleared, ILogger.Types.INFO, Toast.LENGTH_SHORT);
//                        } else {
//                            LogManager.log(mContext, R.string.title_trash_clear_error, ILogger.Types.ERROR, Toast.LENGTH_LONG);
//                        }
//                    }, R.string.ask_clear_trash);
//                    return true;
//                });

//        findPreference(getString(R.string.pref_key_quickly_node_id))
//                .setOnPreferenceClickListener(preference -> {
//                    // диалог выбора ветки
//                    NodeDialogs.createNodeChooserDialog(getContext(), NodesManager.getQuicklyNode(),
//                            false, false, true, new NodeDialogs.INodeChooserResult() {
//                                @Override
//                                public void onApply(TetroidNode node) {
//                                    // устанавливаем ветку, если все хорошо
//                                    SettingsManager.setQuicklyNode(mContext, node);
//                                    NodesManager.setQuicklyNode(node);
//                                    updateSummary(R.string.pref_key_quickly_node_id, SettingsManager.getQuicklyNodeName(mContext));
//                                }
//                                @Override
//                                public void onProblem(int code) {
//                                    // если хранилище недозагружено, спрашиваем о действиях
//                                    int mesId = (code == NodeDialogs.INodeChooserResult.LOAD_STORAGE)
//                                            ? R.string.ask_load_storage : R.string.ask_load_all_nodes;
//                                    AskDialogs.showYesDialog(getContext(), () -> {
//                                        // возвращаемся в MainActivity
//                                        Intent intent = new Intent();
//                                        switch (code) {
//                                            case NodeDialogs.INodeChooserResult.LOAD_STORAGE:
//                                                intent.putExtra(SettingsFragment.EXTRA_IS_LOAD_STORAGE, true);
//                                                break;
//                                            case NodeDialogs.INodeChooserResult.LOAD_ALL_NODES:
//                                                intent.putExtra(SettingsFragment.EXTRA_IS_LOAD_ALL_NODES, true);
//                                                break;
//                                        }
//                                        getActivity().setResult(RESULT_OK, intent);
//                                        getActivity().finish();
//                                    }, mesId);
//                                }
//                            });
//                    return true;
//                });
//        NodesManager.updateQuicklyNode(getContext());
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

//        updateSummary(R.string.pref_key_storage_path, SettingsManager.getStoragePath(mContext));
        updateSummary(R.string.pref_key_temp_path, CommonSettings.getTrashPath(context))
//        updateSummary(R.string.pref_key_quickly_node_id, SettingsManager.getQuicklyNodeName(context))
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
//        String folderPath = new UriUtils(getContext()).getPath(data.getData());
        /*boolean isCreate = requestCode == Constants.REQUEST_CODE_CREATE_STORAGE_PATH;
        if (requestCode == Constants.REQUEST_CODE_OPEN_STORAGE_PATH || isCreate) {
            // уведомляем об изменении каталога, если он действительно изменился, либо если создаем
            boolean pathChanged = !folderPath.equals(SettingsManager.getStoragePath(mContext)) || isCreate;
            if (pathChanged) {
                Intent intent = new Intent();
                intent.putExtra(Constants.EXTRA_IS_REINIT_STORAGE, true);
                if (isCreate) {
                    intent.putExtra(Constants.EXTRA_IS_CREATE_STORAGE, true);
                }
                getActivity().setResult(RESULT_OK, intent);
            }
            SettingsManager.setStoragePath(mContext, folderPath);
            SettingsManager.setLastChoosedFolder(mContext, folderPath);
            updateSummary(R.string.pref_key_storage_path, folderPath);
            if (pathChanged) {
                // закрываем настройки для немедленной перезагрузки хранилища
                getActivity().finish();
            }
        }
        else*/
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

//    private fun selectStorageFolder() {
//        // спрашиваем: создать или выбрать хранилище ?
//        StorageDialogs.createStorageSelectionDialog(context, object : StorageDialogs.IItemClickListener {
//            override fun onItemClick(isNew: Boolean) {
//                openFolderPicker(
//                    getString(R.string.title_storage_folder),
//                    SettingsManager.getStoragePath(context), isNew
//                )
//            }
//        })
//    }

//    protected fun openFolderPicker(title: String?, location: String, isNew: Boolean) {
//        val path = if (!StringUtil.isBlank(location)) location
//            else StorageInteractor.getLastFolderPathOrDefault(requireContext(), true)
//        val intent = Intent(context, FolderPicker::class.java)
//        intent.putExtra(FolderPicker.EXTRA_TITLE, title)
//        intent.putExtra(FolderPicker.EXTRA_LOCATION, path)
//        if (isNew) {
//            intent.putExtra(FolderPicker.EXTRA_EMPTY_FOLDER, true)
//        } else {
//            intent.putExtra(FolderPicker.EXTRA_DESCRIPTION, getString(R.string.title_storage_path_desc))
//        }
//        requireActivity().startActivityForResult(
//            intent,
//            if (isNew) Constants.REQUEST_CODE_CREATE_STORAGE_PATH else Constants.REQUEST_CODE_OPEN_STORAGE_PATH
//        )
//    }

    private fun selectTrashFolder() {
        openFolderPicker(
            getString(R.string.pref_trash_path),
            CommonSettings.getTrashPath(context),
            Constants.REQUEST_CODE_OPEN_TEMP_PATH
        )
    }
}