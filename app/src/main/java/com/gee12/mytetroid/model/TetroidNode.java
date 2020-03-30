package com.gee12.mytetroid.model;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;

public class TetroidNode extends TetroidObject {

    public static final String PREFIX = "branch";

    private int level;
    private List<TetroidNode> subNodes;
    private List<TetroidRecord> records;
    private Drawable icon;
    private String iconName;
    private TetroidNode parentNode;

    public TetroidNode(boolean isCrypted, String id, String name, String iconName, int level) {
        super(FoundType.TYPE_NODE, isCrypted, id, name);
//        setIcon(iconFullName);
        this.iconName = iconName;
//        this.subNodes = subNodes;
//        this.records = records;
        this.level = level;
    }

    public TetroidNode(String id, String name, int level) {
        super(FoundType.TYPE_NODE, false, id, name);
        this.id = id;
        this.name = name;
        this.iconName = null;
        this.level = level;
        this.subNodes = new ArrayList<>();
        this.records = new ArrayList<>();
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
            this.icon = FileUtils.loadSVGFromFile(fullFileName);
        } catch (Exception e) {
            LogManager.addLog(e);
        }
    }

    public void loadIconFromStorage(String iconsStoragePath) {
        if (TextUtils.isEmpty(iconName))
            return;
        loadIcon(iconsStoragePath + iconName);
    }

    public String getIconName() {
        return iconName;
    }

    public String getCryptedName(String cryptedName) {
        return (!isCrypted || isDecrypted) ? name : cryptedName;
    }

    public int getLevel() {
        return level;
    }

    public List<TetroidNode> getSubNodes() {
        return subNodes;
    }

    public int getSubNodesCount() {
        return (subNodes != null) ? subNodes.size() : 0;
    }

    public List<TetroidRecord> getRecords() {
        return records;
    }

    public int getRecordsCount() {
        return (records != null) ? records.size() : 0;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    public void setSubNodes(List<TetroidNode> subNodes) {
        this.subNodes = subNodes;
    }

    public void setRecords(List<TetroidRecord> records) {
        this.records = records;
    }

    public void addSubNode(TetroidNode subNode) {
        if (subNodes != null)
            subNodes.add(subNode);
    }

    public void addRecord(TetroidRecord record) {
        record.setNode(this);
        records.add(record);
    }

    public void setParentNode(TetroidNode parentNode) {
        this.parentNode = parentNode;
    }

    public TetroidNode getParentNode() {
        return parentNode;
    }

    public boolean isExpandable() {
        return (subNodes != null && !subNodes.isEmpty()) /*&& isNonCryptedOrDecrypted()*/;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }
}
