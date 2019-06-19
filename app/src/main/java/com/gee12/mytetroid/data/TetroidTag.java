package com.gee12.mytetroid.data;

import java.util.List;
import java.util.Map;

import androidx.annotation.Nullable;

public class TetroidTag extends FoundObject { //implements Map.Entry<String, List<TetroidRecord>> {
    private String name;
    private List<TetroidRecord> records;

    public TetroidTag(String name, List<TetroidRecord> records) {
        this.name = name;
        this.records = records;
    }

    public void addRecord(TetroidRecord record) {
        records.add(record);
    }

    public String getName() {
        return name;
    }

    public List<TetroidRecord> getRecords() {
        return records;
    }

    public TetroidRecord getRecord(int index) {
        return records.get(index);
    }

    //    @Override
//    public String getKey() {
//        return name;
//    }
//
//    @Override
//    public List<TetroidRecord> getValue() {
//        return records;
//    }
//
//    @Override
//    public List<TetroidRecord> setValue(List<TetroidRecord> value) {
//        return records = value;
//    }
//
//    @Override
//    public boolean equals(@Nullable Object obj) {
//        TetroidTag e2 = (TetroidTag) obj;
//        return (getKey()==null ?
//                e2.getKey()==null
//                : getKey().equals(e2.getKey()))
//                    && (getValue()==null ?
//                        e2.getValue()==null : getValue().equals(e2.getValue()));
//    }
//
//    @Override
//    public int hashCode() {
//        return (getKey()==null ? 0 : getKey().hashCode()) ^
//                (getValue()==null ? 0 : getValue().hashCode());
//    }

    @Override
    public String getDisplayName() {
        return name;
    }
}
