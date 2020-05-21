package com.gee12.mytetroid.data;

import com.gee12.mytetroid.model.TetroidRecord;

public interface IRecordFileCrypter {
    void cryptRecordFile(TetroidRecord record, boolean isEncrypt);
}
