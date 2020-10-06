package com.gee12.mytetroid.data;

import android.content.Context;

import com.gee12.mytetroid.model.TetroidRecord;

public interface IRecordFileCrypter {
    boolean cryptRecordFiles(Context context, TetroidRecord record, boolean isCrypted, boolean isEncrypt);
}
