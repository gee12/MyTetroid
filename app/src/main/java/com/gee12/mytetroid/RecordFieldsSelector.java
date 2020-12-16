package com.gee12.mytetroid;

import android.content.Context;

import java.util.Set;

/**
 * Класс для заполнения переменной-маски для быстрого получения выбранных значений
 *  из множественного списка.
 */
public class RecordFieldsSelector extends StringsIntMask {

    private static final int AUTHOR_INDEX = 0;
    private static final int TAGS_INDEX = 1;
    private static final int CREATED_INDEX = 2;
    private static final int EDITED_INDEX = 3;

    /**
     *
     * @param context
     * @param option Список выбранных значений.
     */
    public RecordFieldsSelector(Context context, Set<String> option) {
        super(option, context.getResources().getStringArray(R.array.record_fields_in_list_entries));
        /*String[] ar = context.getResources().getStringArray(R.array.record_fields_in_list_entries);
        this.mMask = 0;
        int i = 0;
        for (String field : option) {
            if (field.equals(ar[i])) {
                mMask |= (1 << i);
            }
            i++;
        }*/
    }

    public boolean checkIsAuthor() {
        return checkValue(AUTHOR_INDEX);
    }

    public boolean checkIsTags() {
        return checkValue(TAGS_INDEX);
    }

    public boolean checkIsCreatedDate() {
        return checkValue(CREATED_INDEX);
    }

    public boolean checkIsEditedDate() {
        return checkValue(EDITED_INDEX);
    }
}
