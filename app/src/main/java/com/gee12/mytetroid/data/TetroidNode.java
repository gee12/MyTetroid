package com.gee12.mytetroid.data;

import java.util.ArrayList;
import java.util.List;

public class TetroidNode {
    private int id;
    private String name = "";
    private List<TetroidNode> subNodes;
    private int level;

    public TetroidNode(int id, String name, int level) {
        this.id = id;
        this.name = name;
        this.level = level;
        this.subNodes = new ArrayList<>();
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


    public void setName(String name) {
        this.name = name;
    }

    public void addSubNode(TetroidNode subNode)
    {
        subNodes.add(subNode);
    }

    public boolean isHaveSubNodes() {
        return !subNodes.isEmpty();
    }
}
