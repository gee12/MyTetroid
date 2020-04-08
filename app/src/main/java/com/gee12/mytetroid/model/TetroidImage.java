package com.gee12.mytetroid.model;

import org.jetbrains.annotations.NotNull;

public class TetroidImage extends TetroidObject {

    private TetroidRecord record;
    private int width;
    private int height;

    public TetroidImage(String nameId, @NotNull TetroidRecord record) {
        super(FoundType.TYPE_IMAGE, record.isCrypted(), nameId, nameId);
        this.record = record;
    }

    public TetroidRecord getRecord() {
        return record;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public String getPrefix() {
        return "";
    }
}
