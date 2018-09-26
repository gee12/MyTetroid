package com.gee12.mytetroid.data;

import java.util.ArrayList;
import java.util.List;

public class TetroidNode {
    private int id;
    private String name = "";
    private int level;
    private List<TetroidNode> subNodes;
    private List<TetroidRecord> records;

    public TetroidNode(int id, String name, int level) {
        this.id = id;
        this.name = name;
        this.level = level;
        this.subNodes = new ArrayList<>();
        this.records = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

//    public int getIcon() {
//        return R.id.node_icon;
//    }

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
