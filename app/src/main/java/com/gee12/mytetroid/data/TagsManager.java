package com.gee12.mytetroid.data;

import android.content.Context;
import android.text.TextUtils;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.logs.ILogger;
import com.gee12.mytetroid.logs.LogManager;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.model.TetroidTag;

import java.util.HashMap;

@Deprecated
public class TagsManager extends DataManager {

//    /**
//     * Переименование метки в записях.
//     * @param tag
//     * @param newName
//     */
//    public static boolean renameTag(Context context, TetroidTag tag, String newName) {
//        if (tag == null || TextUtils.isEmpty(newName)) {
//            LogManager.log(context, R.string.log_tag_is_null, ILogger.Types.ERROR, Toast.LENGTH_LONG);
//            return false;
//        }
//        // если новое имя метки совпадает со старым (в т.ч. и по регистру), ничего не делаем
//        if (newName.contentEquals(tag.getName())) {
//            return true;
//        }
//
//        HashMap<String, TetroidTag> tagsMap = Instance.mXml.mTagsMap;
//        String lowerCaseNewName = newName.toLowerCase();
//        String lowerCaseOldName = tag.getName().toLowerCase();
//        // смотрим, если есть метка с таким же названием в списке после переименования
//        if (tagsMap.containsKey(lowerCaseNewName)) {
//            TetroidTag existsTag = tagsMap.get(lowerCaseNewName);
//            // если новая и существующая метки отличаются только регистром
//            // (т.е. по сути, в глобальном списке меток это одна и та же запись),
//            //  то обновляем название метки
//            if (tag == existsTag) {
//                existsTag.setName(newName);
//            }
//            // если есть, то сливаем 2 метки в одну уже имеющуюся в списке:
//            //  1) уже имеющуюся используем вместо старой (только что переименованной)
//            //  2) старую удаляем из общего списка
//            for (TetroidRecord record : tag.getRecords()) {
//                if (tag != existsTag) {
//                    int index = record.getTags().indexOf(tag);
//                    // удаляем старую метку-дубликат из записей
//                    record.getTags().remove(tag);
//
//                    // добавляем записи из старой метки в существующую, только если записи еще нет
//                    // (исправление дублирования записей по метке, если одна и та же метка
//                    // добавлена в запись несколько раз)
//                    if (!existsTag.getRecords().contains(record)) {
//                        existsTag.addRecord(record);
//                        // вставляем на ту же позицию, где была старая метка
//                        record.getTags().add(index, existsTag);
//                    }
//                }
//                // формируем заново tagsString у записей метки
//                record.updateTagsString();
//            }
//            // удаляем старую метку-дубликат из общего списка,
//            //  но только, если это полностью разные метки, а не отличающиеся только регистром
//            if (tag != existsTag) {
//                tagsMap.remove(lowerCaseOldName);
//                tag = null;
//            }
//        } else {
//            // если название новой метки - уникально, то
//            //  1) удаляем запись из общего списка по старому ключу
//            tagsMap.remove(lowerCaseOldName);
//            //  2) добавляем ее по-новой в общий список (по новому ключу)
//            tagsMap.put(lowerCaseNewName, tag);
//
//            // обновим название метки
//            tag.setName(newName);
//            // сформируем заново tagsString у записей метки
//            for (TetroidRecord record : tag.getRecords()) {
//                record.updateTagsString();
//            }
//        }
//
//        return (Instance.saveStorage(context));
//    }
}
