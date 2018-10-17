package com.gee12.mytetroid.data;

import android.graphics.drawable.Drawable;
import android.os.Environment;

import com.gee12.mytetroid.Utils;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TetroidNode {
    private String id;
    private String name;
    private int level;
    private List<TetroidNode> subNodes;
    private List<TetroidRecord> records;
    private Drawable icon;
    private String iconName;
    private boolean isCrypted;

    private boolean isDecrypted;

    public TetroidNode(boolean crypt, String id, String name, String iconName,
                       List<TetroidNode> subNodes, List<TetroidRecord> records, int level) {
        this.id = id;
        this.name = name;
//        setIcon(iconFullName);
        this.iconName = iconName;
        this.isCrypted = crypt;
        this.subNodes = subNodes;
        this.records = records;
        this.level = level;
    }

    public TetroidNode(String id, String name, int level) {
        this.id = id;
        this.name = name;
        this.level = level;
        this.subNodes = new ArrayList<>();
        this.records = new ArrayList<>();
        //
//        if (new Random().nextInt(10) % 2 == 0)
//            setIcon(Environment.getExternalStorageDirectory() + "/KateDownloads/test.svg");
    }

    public String getId() {
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

    public void loadIcon(String fullFileName) {
        if (fullFileName == null)
            return;
        try {
//            this.icon = Utils.loadSVGFromFile(Environment.getExternalStorageDirectory() + "/KateDownloads/test.svg");
            this.icon = Utils.loadSVGFromFile(fullFileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadIconFromStorage(String iconsStoragePath) {
        if (Utils.isNullOrEmpty(iconName))
            return;
         loadIcon(iconsStoragePath + iconName);
    }

    public String getName() {
        return name;
    }

    public String getIconName() {
        return iconName;
    }

    public String getCryptedName() {
        return (!isCrypted || isDecrypted) ? name : "Зашифровано";
    }

    public int getLevel() {
        return level;
    }

    public List<TetroidNode> getSubNodes() {
        return subNodes;
    }

    public int getSubNodesCount() {
        return subNodes.size();
    }

    public List<TetroidRecord> getRecords() {
        return records;
    }

    public int getRecordsCount() {
        return records.size();
    }

    public boolean isCrypted() {
        return isCrypted;
    }

    public boolean isDecrypted() {
        return (!isCrypted || isDecrypted);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    public void setDecrypted(boolean decrypted) {
        isDecrypted = decrypted;
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

    public boolean isExpandable() {
        return !subNodes.isEmpty() && isDecrypted();
    }
}
