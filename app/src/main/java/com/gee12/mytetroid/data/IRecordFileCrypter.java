package com.gee12.mytetroid.data;

import com.gee12.mytetroid.model.TetroidRecord;

public interface IRecordFileCrypter {
    boolean cryptRecordFile(TetroidRecord record, boolean isEncrypt);
}
