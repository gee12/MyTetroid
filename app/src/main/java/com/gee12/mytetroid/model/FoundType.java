package com.gee12.mytetroid.model;

import android.content.Context;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.StringsIntMask;

public class FoundType extends StringsIntMask {

    public static final int TYPES_COUNT = 11;
    public static final int TYPE_NONE = 0;
    public static final int TYPE_RECORD = 1;
    public static final int TYPE_RECORD_TEXT = 2;
    public static final int TYPE_AUTHOR = 3;
    public static final int TYPE_URL = 4;
    public static final int TYPE_FILE = 5;
    public static final int TYPE_TAG = 6;
    public static final int TYPE_NODE = 7;
    public static final int TYPE_NODE_ID = 8;
    public static final int TYPE_RECORD_ID = 9;
    public static final int TYPE_FILE_ID = 10;
    // не хранятся в mytetra.xml
    public static final int TYPE_IMAGE = 11;

    public FoundType() {
        super();
    }

    public FoundType(int type) {
        super();
        addValue(type);
    }

    /**
     * Формирование строки в виде перечисления типов объекта, хранящихся в битах переменной type.
     * Формат: "тип1, тип2, .., типN"
     * @param context
     * @return
     */
    public String getFoundTypeString(Context context) {
        // уменьшаем на 1, т.к. пропускаем тип NONE
        return joinToString(context.getResources().getStringArray(R.array.found_types), 1);
    }
}
