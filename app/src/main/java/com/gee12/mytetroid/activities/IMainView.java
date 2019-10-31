package com.gee12.mytetroid.activities;

import android.os.Parcelable;

import com.gee12.mytetroid.data.ITetroidObject;
import com.gee12.mytetroid.data.TetroidFile;
import com.gee12.mytetroid.data.TetroidRecord;

public interface IMainView extends Parcelable {
    void onMainPageCreated();
    void openFolder(String pathUri);
    void openFile(TetroidRecord record, TetroidFile file);
    void updateMainToolbar(int viewId, String title);
    void openFoundObject(ITetroidObject found);
    void openMainPage();
    void closeFoundFragment();
    void toggleFullscreen();
    void openTag(String tag);
    void checkKeepScreenOn(int curViewId);
}
