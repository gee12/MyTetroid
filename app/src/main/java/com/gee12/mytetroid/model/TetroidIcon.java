package com.gee12.mytetroid.model;

import static com.gee12.mytetroid.common.extensions.FileExtensionsKt.makePath;

import android.graphics.drawable.Drawable;

import java.io.File;


public class TetroidIcon {

    private String folder;
    private String name;
    private Drawable icon;

    public TetroidIcon(String folder, String name) {
        this.folder = folder;
        this.name = name;
        this.icon = null;
    }

    public String getPath() {
        return File.separator + makePath(folder, name);
    }

    public String getFolder() {
        return folder;
    }

    public String getName() {
        return name;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

}
