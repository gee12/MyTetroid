package com.gee12.mytetroid.data;

import android.content.Context;
import android.widget.Toast;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.logs.ILogger;
import com.gee12.mytetroid.logs.LogManager;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.model.TetroidTag;

public class TagsManager extends DataManager {

    /**
     * Переименование метки в записях.
     * @param tag
     * @param newName
     */
    public static boolean renameTag(Context context, TetroidTag tag, String newName) {
        if (tag == null) {
            LogManager.log(context, R.string.log_tag_is_null, ILogger.Types.ERROR, Toast.LENGTH_LONG);
            return false;
        }

        String oldName = tag.getName();
        for (TetroidRecord record : tag.getRecords()) {
            for (TetroidTag recordTag : record.getTags()) {
                if (recordTag == tag) {
                    recordTag.setName(newName);
                }
            }
            // сформируем заново список меток
            record.updateTagsString();
        }

        if (Instance.saveStorage(context)) {
            DataManager.getTags().remove(oldName);
            DataManager.getTags().put(newName, tag);
            return true;
        } else {
            return false;
        }
    }
}
