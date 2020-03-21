package com.gee12.mytetroid.model;

import java.util.Comparator;
import java.util.List;

public class TetroidTag extends TetroidObject {

    public static final String TAG_LINKS_PREF = "tag:"; //implements Map.Entry<String, List<TetroidRecord>> {

//    private String name;
    private List<TetroidRecord> records;

    public TetroidTag(String name, List<TetroidRecord> records) {
        super(FoundType.TYPE_TAG, false, "", name);
//        this.name = name;
        this.records = records;
    }

//    @Override
//    public String getId() {
//        return "";
//    }
//
//    @Override
//    public int getType() {
//        return FoundType.TYPE_TAG;
//    }
//
//    @Override
//    public String getName() {
//        return name;
//    }

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

    /**
     * Формирование списка меток в виде html-кода.
     * @return
     */
    public static String createTagsLinksString(TetroidRecord record) {
        if (record == null)
            return null;
        StringBuilder sb = new StringBuilder();
        int size = record.getTags().size();
        if (size == 0) {
            return null;
        }
        // #a4a4e4 - это colorLightLabelText
        sb.append("<body style=\"font-family:'DejaVu Sans';color:#a4a4e4;margin:0px;\">");
        for (TetroidTag tag : record.getTags()) {
            // #303F9F - это colorBlueDark
            sb.append("<a href=\"").append(TAG_LINKS_PREF).append(tag.getName()).append("\" style=\"color:#303F9F;\">").
                    append(tag.getName()).append("</a>");
            if (--size > 0)
                sb.append(", ");
        }
        sb.append("</body>");
        return sb.toString();
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

    /**
     * Функция сравнения меток.
     */
    public static class TagsComparator implements Comparator<String> {

        @Override
        public int compare(String o1, String o2) {
            if (o1 == o2) {
                return 0;
            }
            if (o1 == null) {
                return -1;
            }
            if (o2 == null) {
                return 1;
            }
            return o1.toLowerCase().compareTo(o2.toLowerCase());
        }
    }

}
