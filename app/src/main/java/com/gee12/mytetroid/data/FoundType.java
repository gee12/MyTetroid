package com.gee12.mytetroid.data;

import android.content.Context;

import com.gee12.mytetroid.R;

public class FoundType {

    public static final int TYPES_COUNT = 7;
    public static final int TYPE_RECORD = 0;
    public static final int TYPE_RECORD_TEXT = 1;
    public static final int TYPE_AUTHOR = 2;
    public static final int TYPE_URL = 3;
    public static final int TYPE_FILE = 4;
    public static final int TYPE_TAG = 5;
    public static final int TYPE_NODE = 6;

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
     * Формирование строки по типам объекта.
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
                sb.append(foundTypes[i]);
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
