package com.gee12.mytetroid.activities;

import android.os.Parcelable;

import com.gee12.mytetroid.model.ITetroidObject;
import com.gee12.mytetroid.model.TetroidFile;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.model.TetroidRecord;

public interface IMainView extends Parcelable {

    void onMainPageCreated();
    void openNode(TetroidNode node);
    void openRecordFolder(TetroidRecord record);
    void openRecordAttaches(TetroidRecord record);
    void openAttach(TetroidFile file);
    void updateMainToolbar(int viewId, String title);
    void openFoundObject(ITetroidObject found);
    void openMainPage();
    void research();
    void closeFoundFragment();
    void openRecord(TetroidRecord record);
    void updateTags();
    void updateNodeList();
    void openFilePicker();
    void openFolderPicker();
    void updateFavorites();
    void showGlobalSearchWithQuery();
    void updateOptionsMenu();
    void downloadFileToCache(String url, TetroidActivity.IDownloadFileResult callback);
    void attachFile(String fullFileName, boolean deleteSrcFile);
}
