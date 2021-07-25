package com.gee12.mytetroid.data;

import com.gee12.mytetroid.model.TetroidRecord;

public interface ITagsParser {

    /**
     * Разбираем строку с метками записи и добавляем метки в запись и в дерево.
     * @param record
     * @param tagsString Строка с метками (не зашифрована).
     * Передается отдельно, т.к. поле в записи может быть зашифровано.
     */
    void parseRecordTags(TetroidRecord record, String tagsString);

    /**
     * Удаление меток записи из списка.
     * @param record
     */
    void deleteRecordTags(TetroidRecord record);

}
