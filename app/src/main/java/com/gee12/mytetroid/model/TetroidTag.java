package com.gee12.mytetroid.model;

import java.util.List;

public class TetroidTag extends TetroidObject {

    public static final String PREFIX = "tag";
    public static final String LINKS_PREFIX = PREFIX + ":"; //implements Map.Entry<String, List<TetroidRecord>> {

    private List<TetroidRecord> records;
    private Boolean isEmpty;

    public TetroidTag(String name, List<TetroidRecord> records, Boolean isEmpty) {
        super(FoundType.TYPE_TAG, false, "", name);
        this.records = records;
        this.isEmpty = isEmpty;
    }

    public void addRecord(TetroidRecord record) {
        if (records != null)
            records.add(record);
    }

    public List<TetroidRecord> getRecords() {
        return records;
    }

    public TetroidRecord getRecord(int index) {
        return (records != null) ? records.get(index) : null;
    }

    public Boolean isEmpty() {
        return isEmpty;
    }

    /**
     * Формирование ссылки на объект хранилища.
     * @return
     */
    @Override
    public String createUrl() {
        return createUrl(name);
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }
}
