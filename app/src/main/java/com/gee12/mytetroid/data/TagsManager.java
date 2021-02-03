package com.gee12.mytetroid.data;

import android.content.Context;
import android.widget.Toast;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.logs.ILogger;
import com.gee12.mytetroid.logs.LogManager;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.model.TetroidTag;

import java.util.HashMap;

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

        HashMap<String, TetroidTag> tagsMap = Instance.mXml.mTagsMap;
        // смотрим, если есть метка с таким же названием в списке после переименования
        if (tagsMap.containsKey(newName)) {
            TetroidTag existsTag = tagsMap.get(newName);
            // если есть, то сливаем 2 метки в одну уже имеющуюся в списке:
            //  1) уже имеющуюся используем вместо старой (только что переименованной)
            //  2) старую удаляем из общего списка
            for (TetroidRecord record : tag.getRecords()) {
                // добавляем записи из старой метки в существующую, только если записи еще нет
                // (исправление дублирования записей по метке, если одна и та же метка
                // добавлена в запись несколько раз)
                if (!existsTag.getRecords().contains(record)) {
                    existsTag.addRecord(record);
                    record.addTag(existsTag);
                }
                // удаляем старую метку-дубликат из записей
                record.getTags().remove(tag);
                // формируем заново tagsString у записей метки
                record.updateTagsString();
            }
            // удаляем старую метку-дубликат из общего списка
            tagsMap.remove(tag.getName());
            tag = null;
        } else {
            // если название новой метки - уникально, то
            //  1) удаляем запись из общего списка по старому ключу
            tagsMap.remove(tag.getName());
            //  2) добавляем ее по-новой в общий список (по новому ключу)
            tagsMap.put(newName, tag);

            // обновим название метки
            tag.setName(newName);
            // сформируем заново tagsString у записей метки
            for (TetroidRecord record : tag.getRecords()) {
                record.updateTagsString();
            }
        }

        return (Instance.saveStorage(context));
    }
}
