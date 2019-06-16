package com.gee12.mytetroid.activities;

import com.gee12.mytetroid.data.TetroidFile;
import com.gee12.mytetroid.data.TetroidRecord;

public interface IMainView {
    void openFolder(String pathUri);
    void openFile(TetroidRecord record, TetroidFile file);
    void setMainTitle(String title, int viewId);
}
