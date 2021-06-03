package com.gee12.mytetroid.model;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import com.gee12.mytetroid.logs.LogManager;
import com.gee12.mytetroid.utils.FileUtils;

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
        return File.separator + folder + File.separator + name;
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

    public void loadIcon(Context context, String iconsFolderPath) {
        if (iconsFolderPath == null || icon != null) {
            return;
        }
        try {
            this.icon = FileUtils.loadSVGFromFile(iconsFolderPath + File.separator + getPath());
        } catch (Exception e) {
            LogManager.log(context, e);
        }
    }
}
