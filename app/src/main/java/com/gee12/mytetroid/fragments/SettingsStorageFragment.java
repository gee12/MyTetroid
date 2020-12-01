package com.gee12.mytetroid.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.Preference;

import com.gee12.mytetroid.App;
import com.gee12.mytetroid.ILogger;
import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.data.NodesManager;
import com.gee12.mytetroid.data.SettingsManager;
import com.gee12.mytetroid.dialogs.AskDialogs;
import com.gee12.mytetroid.dialogs.NodeDialogs;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.utils.UriUtils;
import com.gee12.mytetroid.views.Message;
import com.gee12.mytetroid.views.StorageChooserDialog;

import static android.app.Activity.RESULT_OK;

public class SettingsStorageFragment extends TetroidSettingsFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        setPreferencesFromResource(R.xml.prefs_storage, rootKey);

        getActivity().setTitle(R.string.pref_category_storage);

        Preference storageFolderPicker = findPreference(getString(R.string.pref_key_storage_path));
        storageFolderPicker.setOnPreferenceClickListener(preference -> {
            if (!checkPermission(SettingsFragment.REQUEST_CODE_OPEN_STORAGE_PATH))
                return true;
            selectStorageFolder();
            return true;
        });

        Preference tempFolderPicker = findPreference(getString(R.string.pref_key_temp_path));
        tempFolderPicker.setOnPreferenceClickListener(preference -> {
            if (!checkPermission(SettingsFragment.REQUEST_CODE_OPEN_TEMP_PATH))
                return true;
            selectTrashFolder();
            return true;
        });

        findPreference(getString(R.string.pref_key_clear_trash))
                .setOnPreferenceClickListener(preference -> {
                    AskDialogs.showYesDialog(getContext(), () -> {
                        if (DataManager.clearTrashFolder(getContext())) {
                            LogManager.log(mContext, R.string.title_trash_cleared, ILogger.Types.INFO, Toast.LENGTH_SHORT);
                        } else {
                            LogManager.log(mContext, R.string.title_trash_clear_error, ILogger.Types.ERROR, Toast.LENGTH_LONG);
                        }
                    }, R.string.ask_clear_trash);
                    return true;
                });

        findPreference(getString(R.string.pref_key_quickly_node_id))
                .setOnPreferenceClickListener(preference -> {
                    // диалог выбора ветки
                    NodeDialogs.createNodeChooserDialog(getContext(), NodesManager.getQuicklyNode(),
                            false, false, true, new NodeDialogs.INodeChooserResult() {
                                @Override
                                public void onApply(TetroidNode node) {
                                    // устанавливаем ветку, если все хорошо
                                    SettingsManager.setQuicklyNode(mContext, node);
                                    NodesManager.setQuicklyNode(node);
                                    updateSummary(R.string.pref_key_quickly_node_id, SettingsManager.getQuicklyNodeName(mContext));
                                }
                                @Override
                                public void onProblem(int code) {
                                    // если хранилище недозагружено, спрашиваем о действиях
                                    int mesId = (code == NodeDialogs.INodeChooserResult.LOAD_STORAGE)
                                            ? R.string.ask_load_storage : R.string.ask_load_all_nodes;
                                    AskDialogs.showYesDialog(getContext(), () -> {
                                        // возвращаемся в MainActivity
                                        Intent intent = new Intent();
                                        switch (code) {
                                            case NodeDialogs.INodeChooserResult.LOAD_STORAGE:
                                                intent.putExtra(SettingsFragment.EXTRA_IS_LOAD_STORAGE, true);
                                                break;
                                            case NodeDialogs.INodeChooserResult.LOAD_ALL_NODES:
                                                intent.putExtra(SettingsFragment.EXTRA_IS_LOAD_ALL_NODES, true);
                                                break;
                                        }
                                        getActivity().setResult(RESULT_OK, intent);
                                        getActivity().finish();
                                    }, mesId);
                                }
                            });
                    return true;
                });
        NodesManager.updateQuicklyNode(getContext());

        Preference loadFavorPref = findPreference(getString(R.string.pref_key_is_load_favorites));
        disableIfFree(loadFavorPref);

        Preference keepNodePref = findPreference(getString(R.string.pref_key_is_keep_selected_node));
        keepNodePref.setOnPreferenceClickListener(pref -> {
            if (SettingsManager.isLoadFavoritesOnly(mContext)) {
                Message.show(getContext(), getString(R.string.title_not_avail_when_favor), Toast.LENGTH_SHORT);
            }
            return true;
        });
        if (App.isFullVersion()) {
            keepNodePref.setDependency(getString(R.string.pref_key_is_load_favorites));
        }

        updateSummary(R.string.pref_key_storage_path, SettingsManager.getStoragePath(mContext));
        updateSummary(R.string.pref_key_temp_path, SettingsManager.getTrashPath(mContext));
        updateSummary(R.string.pref_key_quickly_node_id, SettingsManager.getQuicklyNodeName(mContext));
    }

    public void onRequestPermissionsResult(boolean permGranted, int requestCode) {
        if (permGranted) {
            LogManager.log(mContext, R.string.log_write_ext_storage_perm_granted, ILogger.Types.INFO);
            switch (requestCode) {
                case SettingsFragment.REQUEST_CODE_OPEN_STORAGE_PATH:
                    selectStorageFolder();
                    break;
                case SettingsFragment.REQUEST_CODE_OPEN_TEMP_PATH:
                    selectTrashFolder();
                    break;
            }
        } else {
            LogManager.log(mContext, R.string.log_missing_write_ext_storage_permissions, ILogger.Types.WARNING, Toast.LENGTH_SHORT);
        }
    }

    public void onResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK)
            return;
//        String folderPath = data.getStringExtra(FolderPicker.EXTRA_DATA);
        String folderPath = new UriUtils(getContext()).getPath(data.getData());
        boolean isCreate = requestCode == SettingsFragment.REQUEST_CODE_CREATE_STORAGE_PATH;
        if (requestCode == SettingsFragment.REQUEST_CODE_OPEN_STORAGE_PATH || isCreate) {
            // уведомляем об изменении каталога, если он действительно изменился, либо если создаем
            boolean pathChanged = !folderPath.equals(SettingsManager.getStoragePath(mContext)) || isCreate;
            if (pathChanged) {
                Intent intent = new Intent();
                intent.putExtra(SettingsFragment.EXTRA_IS_REINIT_STORAGE, true);
                if (isCreate) {
                    intent.putExtra(SettingsFragment.EXTRA_IS_CREATE_STORAGE, true);
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
        else if (requestCode == SettingsFragment.REQUEST_CODE_OPEN_TEMP_PATH) {
            SettingsManager.setTrashPath(mContext, folderPath);
            SettingsManager.setLastChoosedFolder(mContext, folderPath);
            updateSummary(R.string.pref_key_temp_path, folderPath);
        }
        else if (requestCode == SettingsFragment.REQUEST_CODE_OPEN_LOG_PATH) {
            SettingsManager.setLogPath(mContext, folderPath);
            SettingsManager.setLastChoosedFolder(mContext, folderPath);
            LogManager.setLogPath(mContext, folderPath);
            updateSummary(R.string.pref_key_log_path, folderPath);
        }
    }

    private void selectStorageFolder() {
        // спрашиваем: создать или выбрать хранилище ?
        StorageChooserDialog.createDialog(getContext(), isNew -> {
            openFolderPicker(getString(R.string.title_storage_folder),
                    SettingsManager.getStoragePath(mContext),
                    (isNew) ? SettingsFragment.REQUEST_CODE_CREATE_STORAGE_PATH
                            : SettingsFragment.REQUEST_CODE_OPEN_STORAGE_PATH);
        });
    }

    private void selectTrashFolder() {
        openFolderPicker(getString(R.string.pref_trash_path),
                SettingsManager.getTrashPath(mContext),
                SettingsFragment.REQUEST_CODE_OPEN_TEMP_PATH);
    }
}
