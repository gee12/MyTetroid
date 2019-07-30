package com.gee12.mytetroid.activities;

import android.os.Parcelable;

import com.gee12.mytetroid.data.FoundObject;
import com.gee12.mytetroid.data.TetroidFile;
import com.gee12.mytetroid.data.TetroidRecord;

public interface IMainView extends Parcelable {
    void openFolder(String pathUri);
    void openFile(TetroidRecord record, TetroidFile file);
    void setMainTitle(String title, int viewId);
    void openFoundObject(FoundObject found);
    void closeFoundFragment();
}
