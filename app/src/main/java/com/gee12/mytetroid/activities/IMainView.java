package com.gee12.mytetroid.activities;

import android.os.Parcelable;

import com.gee12.mytetroid.data.ITetroidObject;
import com.gee12.mytetroid.data.TetroidFile;
import com.gee12.mytetroid.data.TetroidRecord;

public interface IMainView extends Parcelable {
    void openFolder(String pathUri);
    void openFile(TetroidRecord record, TetroidFile file);
    void updateMainTooltip(String title, int viewId);
    void openFoundObject(ITetroidObject found);
    void closeFoundFragment();
}
