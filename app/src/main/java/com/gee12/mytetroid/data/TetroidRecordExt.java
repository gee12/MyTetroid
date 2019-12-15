package com.gee12.mytetroid.data;

public class TetroidRecordExt {

    protected TetroidRecord record;
    protected String textHtml;
    protected String tagsHtml;

    public TetroidRecordExt(TetroidRecord record) {
        this.record = record;
    }

    public void setTextHtml(String textHtml) {
        this.textHtml = textHtml;
    }

    public void setTagsHtml(String tagsHtml) {
        this.tagsHtml = tagsHtml;
    }

    public TetroidRecord getRecord() {
        return record;
    }

    public String getTextHtml() {
        return textHtml;
    }

    public String getTagsHtml() {
        return tagsHtml;
    }
}
