package com.gee12.mytetroid.data;

import android.graphics.drawable.Drawable;
import android.os.Environment;

import com.gee12.mytetroid.Utils;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TetroidNode {
    private int id;
    private String name = "";
    private int level;
    private List<TetroidNode> subNodes;
    private List<TetroidRecord> records;
    private Drawable icon;

    public TetroidNode(int id, String name, int level) {
        this.id = id;
        this.name = name;
        this.level = level;
        this.subNodes = new ArrayList<>();
        this.records = new ArrayList<>();
        //
        if (new Random().nextInt(10) % 2 == 0)
            setIcon(Environment.getExternalStorageDirectory() + "/KateDownloads/test.svg");
    }

    public int getId() {
        return id;
    }

//    public Uri getIconUri() {
//        return iconUri;
//    }

    public Drawable getIcon() {
        return icon;
    }

//    public void setIconUri(String fullFileName) {
//        File file = new File(fullFileName);
//        if (file.exists()) {
//            this.iconUri = Uri.fromFile(file);
//        }
//    }

    public void setIcon(String fullFileName) {
        try {
            this.icon = Utils.loadSVGFromFile(fullFileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    public List<TetroidNode> getSubNodes() {
        return subNodes;
    }

    public List<TetroidRecord> getRecords() {
        return records;
    }

    public int getRecordsCount() {
        return records.size();
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addSubNode(TetroidNode subNode)
    {
        subNodes.add(subNode);
    }

    public void addRecord(TetroidRecord record)
    {
        record.setNode(this);
        records.add(record);
    }

    public boolean isHaveSubNodes() {
        return !subNodes.isEmpty();
    }
}
