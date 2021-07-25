package com.gee12.mytetroid.data.crypt

import android.content.Context
import com.gee12.mytetroid.model.TetroidRecord

interface IRecordFileCrypter {
    suspend fun cryptRecordFiles(context: Context, record: TetroidRecord, isCrypted: Boolean, isEncrypt: Boolean): Boolean
}