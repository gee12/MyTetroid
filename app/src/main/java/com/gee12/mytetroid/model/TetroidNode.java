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

    private String decryptedIconName;

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

    public Drawable getIcon() {
        return icon;
    }

    public void loadIcon(String iconsFolderPath) {
        if (iconsFolderPath == null || TextUtils.isEmpty(getIconName())) {
            return;
        }
        try {
            this.icon = FileUtils.loadSVGFromFile(iconsFolderPath + getIconName());
        } catch (Exception e) {
            LogManager.log(e);
        }
    }



    public String getIconName() {
        return (isCrypted && isDecrypted) ? decryptedIconName : iconName;
    }

    public String getIconName(boolean cryptedValue) {
        return (cryptedValue) ? iconName : decryptedIconName;
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

    public void setDecryptedIconName(String value) {
        this.decryptedIconName = value;
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

    public boolean addRecord(TetroidRecord record) {
        record.setNode(this);
        return records.add(record);
    }

    public boolean deleteRecord(TetroidRecord record) {
        record.setNode(null);
        return records.remove(record);
    }

    public void setParentNode(TetroidNode parentNode) {
        this.parentNode = parentNode;
    }

    public TetroidNode getParentNode() {
        return parentNode;
    }

    public boolean isExpandable() {
        return (subNodes != null && !subNodes.isEmpty());
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }
}
