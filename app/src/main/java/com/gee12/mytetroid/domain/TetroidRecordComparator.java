package com.gee12.mytetroid.domain;

import com.gee12.mytetroid.model.obj.TetroidRecord;

import java.util.Comparator;

/**
 * Структура для сравнения записей по определенному полю.
 */
public class TetroidRecordComparator implements Comparator<TetroidRecord> {

    public static final int FIELD_ID = 1;
    public static final int FIELD_DIR_NAME = 2;

    /**
     * Id
     */
    private class IdComparator extends TetroidRecordComparator {
        @Override
        public int compare(TetroidRecord o1, TetroidRecord o2) {
            return o1.getId().compareTo(o2.getId());
        }
        @Override
        public boolean compare(String fieldValue, TetroidRecord obj) {
            return fieldValue.equals(obj.getId());
        }
    }

    /**
     * DirName
     */
    private class DirNameComparator extends TetroidRecordComparator {
        @Override
        public int compare(TetroidRecord o1, TetroidRecord o2) {
            return o1.getFolderName().compareTo(o2.getFolderName());
        }
        @Override
        public boolean compare(String fieldValue, TetroidRecord obj) {
            return fieldValue.equals(obj.getFolderName());
        }
    }

    private TetroidRecordComparator mComparator;
    private int mFieldType;

    public TetroidRecordComparator() {}

    public TetroidRecordComparator(int fieldType) {
        this.mFieldType = fieldType;
        this.mComparator = (fieldType == FIELD_ID)
                ? new IdComparator()
                : (fieldType == FIELD_DIR_NAME)
                ? new DirNameComparator()
                : null;
    }

    @Override
    public int compare(TetroidRecord o1, TetroidRecord o2) {
        return mComparator.compare(o1, o2);
    }

    public boolean compare(String fieldValue, TetroidRecord obj) {
        return mComparator.compare(fieldValue, obj);
    }

    public int getFieldType() {
        return mFieldType;
    }
}
