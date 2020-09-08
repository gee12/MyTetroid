package com.gee12.mytetroid.model;

import android.content.Context;

import com.gee12.mytetroid.R;

public class FoundType {

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

    private int type;

    public FoundType() {
        this.type = 0;
    }

    public FoundType(int type) {
        this.type = 0;
        addType(type);
    }

    /**
     * Добавление типа объекта.
     * @param type
     */
    public void addType(int type) {
        this.type |= (1 << type);
    }

    /**
     * Формирование строки в виде перечисления типов объекта, хранящихся в битах переменной type.
     * Формат: "тип1, тип2, .., типN"
     * @param context
     * @return
     */
    public String getTypeString(Context context) {
        StringBuilder sb = new StringBuilder();
        String[] foundTypes = context.getResources().getStringArray(R.array.found_types);
        boolean isFirst = true;
        for (int i = 0; i < TYPES_COUNT; i++)
            if ((type & (1 << i)) > 0) {
                if (!isFirst)
                    sb.append(", ");
                // уменьшаем на 1, т.к. пропускаем тип NONE
                sb.append(foundTypes[i - 1]);
                isFirst = false;
            }
        return sb.toString();
    }

    /**
     * Проверка имеет ли объект данный тип.
     * @param type
     * @return
     */
    public boolean checkType(int type) {
        return ((type & (1 << type)) > 0);
    }
}
